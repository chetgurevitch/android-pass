package me.proton.pass.presentation.home

import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.android.pass.clipboard.api.ClipboardManager
import me.proton.android.pass.data.api.crypto.EncryptionContextProvider
import me.proton.android.pass.data.api.usecases.ApplyPendingEvents
import me.proton.android.pass.data.api.usecases.ObserveActiveItems
import me.proton.android.pass.data.api.usecases.ObserveActiveShare
import me.proton.android.pass.data.api.usecases.ObserveCurrentUser
import me.proton.android.pass.data.api.usecases.TrashItem
import me.proton.android.pass.log.PassLogger
import me.proton.android.pass.notifications.api.SnackbarMessageRepository
import me.proton.pass.common.api.None
import me.proton.pass.common.api.Option
import me.proton.pass.common.api.Result
import me.proton.pass.common.api.Some
import me.proton.pass.common.api.map
import me.proton.pass.common.api.onSuccess
import me.proton.pass.domain.ItemType
import me.proton.pass.presentation.components.model.ItemUiModel
import me.proton.pass.presentation.extension.toUiModel
import me.proton.pass.presentation.home.HomeSnackbarMessage.AliasCopied
import me.proton.pass.presentation.home.HomeSnackbarMessage.AliasMovedToTrash
import me.proton.pass.presentation.home.HomeSnackbarMessage.LoginMovedToTrash
import me.proton.pass.presentation.home.HomeSnackbarMessage.NoteCopied
import me.proton.pass.presentation.home.HomeSnackbarMessage.NoteMovedToTrash
import me.proton.pass.presentation.home.HomeSnackbarMessage.ObserveItemsError
import me.proton.pass.presentation.home.HomeSnackbarMessage.ObserveShareError
import me.proton.pass.presentation.home.HomeSnackbarMessage.PasswordCopied
import me.proton.pass.presentation.home.HomeSnackbarMessage.RefreshError
import me.proton.pass.presentation.home.HomeSnackbarMessage.UsernameCopied
import me.proton.pass.presentation.uievents.IsLoadingState
import me.proton.pass.presentation.uievents.IsProcessingSearchState
import me.proton.pass.presentation.uievents.IsRefreshingState
import me.proton.pass.presentation.utils.ItemUiFilter
import javax.inject.Inject

@ExperimentalMaterialApi
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trashItem: TrashItem,
    private val snackbarMessageRepository: SnackbarMessageRepository,
    private val clipboardManager: ClipboardManager,
    private val applyPendingEvents: ApplyPendingEvents,
    private val encryptionContextProvider: EncryptionContextProvider,
    observeCurrentUser: ObserveCurrentUser,
    observeActiveShare: ObserveActiveShare,
    observeActiveItems: ObserveActiveItems
) : ViewModel() {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        PassLogger.e(TAG, throwable)
    }

    private val filterModeFlow: MutableStateFlow<HomeFilterMode> =
        MutableStateFlow(HomeFilterMode.AllItems)

    private val currentUserFlow = observeCurrentUser().filterNotNull()

    private val searchQueryState: MutableStateFlow<String> = MutableStateFlow("")
    private val isInSearchModeState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val isProcessingSearchState: MutableStateFlow<IsProcessingSearchState> =
        MutableStateFlow(IsProcessingSearchState.NotLoading)

    private val searchWrapperWrapper = combine(
        searchQueryState,
        isInSearchModeState,
        isProcessingSearchState
    ) { searchQuery, isInSearchMode, isProcessingSearch ->
        SearchWrapper(searchQuery, isInSearchMode, isProcessingSearch)
    }

    private val isRefreshing: MutableStateFlow<IsRefreshingState> =
        MutableStateFlow(IsRefreshingState.NotRefreshing)

    private val sortingTypeState: MutableStateFlow<SortingType> =
        MutableStateFlow(SortingType.ByName)

    private val activeItemUIModelFlow: Flow<Result<List<ItemUiModel>>> = observeActiveItems()
        .map { itemResult ->
            itemResult.map { list ->
                encryptionContextProvider.withEncryptionContext {
                    list.map { it.toUiModel(this@withEncryptionContext) }
                }
            }
        }
        .distinctUntilChanged()

    private val sortedListItemFlow: Flow<Result<List<ItemUiModel>>> = combine(
        activeItemUIModelFlow,
        sortingTypeState
    ) { result, sortingType ->
        when (sortingType) {
            SortingType.ByName -> result.map { list -> list.sortByTitle() }
            SortingType.ByItemType -> result.map { list -> list.sortByItemType() }
        }
    }
        .distinctUntilChanged()

    @OptIn(FlowPreview::class)
    private val resultsFlow: Flow<Result<List<ItemUiModel>>> = combine(
        sortedListItemFlow,
        searchQueryState.debounce(DEBOUNCE_TIMEOUT),
        filterModeFlow
    ) { result, searchQuery, filterMode ->
        isProcessingSearchState.update { IsProcessingSearchState.NotLoading }
        if (searchQuery.isNotBlank()) {
            result.map { ItemUiFilter.filterByQuery(it, searchQuery) }
        } else {
            result.map { items ->
                items.filter {
                    when (filterMode) {
                        HomeFilterMode.AllItems -> true
                        HomeFilterMode.Aliases -> it.itemType is ItemType.Alias
                        HomeFilterMode.Logins -> it.itemType is ItemType.Login
                        HomeFilterMode.Notes -> it.itemType is ItemType.Note
                    }
                }
            }
        }
    }.flowOn(Dispatchers.Default)

    private data class SearchWrapper(
        val searchQuery: String,
        val isInSearchMode: Boolean,
        val isProcessingSearch: IsProcessingSearchState
    )

    val homeUiState: StateFlow<HomeUiState> = combine(
        observeActiveShare(),
        resultsFlow,
        searchWrapperWrapper,
        isRefreshing,
        sortingTypeState
    ) { shareIdResult, itemsResult, searchWrapper, refreshing, sortingType ->
        val isLoading = IsLoadingState.from(
            shareIdResult is Result.Loading || itemsResult is Result.Loading
        )

        val items = when (itemsResult) {
            Result.Loading -> emptyList()
            is Result.Success -> itemsResult.data
            is Result.Error -> {
                val defaultMessage = "Observe items error"
                PassLogger.e(
                    TAG,
                    itemsResult.exception ?: Exception(defaultMessage),
                    defaultMessage
                )
                snackbarMessageRepository.emitSnackbarMessage(ObserveItemsError)
                emptyList()
            }
        }

        val selectedShare = when (shareIdResult) {
            Result.Loading -> None
            is Result.Success -> Option.fromNullable(shareIdResult.data)
            is Result.Error -> {
                val defaultMessage = "Observe active share error"
                PassLogger.e(
                    TAG,
                    shareIdResult.exception ?: Exception(defaultMessage),
                    defaultMessage
                )
                snackbarMessageRepository.emitSnackbarMessage(ObserveShareError)
                None
            }
        }

        HomeUiState(
            homeListUiState = HomeListUiState(
                isLoading = isLoading,
                isRefreshing = refreshing,
                items = items,
                selectedShare = selectedShare,
                sortingType = sortingType
            ),
            searchUiState = SearchUiState(
                searchQuery = searchWrapper.searchQuery,
                inSearchMode = searchWrapper.isInSearchMode,
                isProcessingSearch = searchWrapper.isProcessingSearch
            )
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading
        )

    fun onSearchQueryChange(query: String) {
        if (query.contains("\n")) return

        searchQueryState.update { query }
        isProcessingSearchState.update { IsProcessingSearchState.Loading }
    }

    fun onStopSearching() {
        searchQueryState.update { "" }
        isInSearchModeState.update { false }
    }

    fun onEnterSearch() {
        searchQueryState.update { "" }
        isInSearchModeState.update { true }
    }

    fun onSortingTypeChanged(sortingType: SortingType) {
        sortingTypeState.update { sortingType }
    }

    fun onRefresh() = viewModelScope.launch(coroutineExceptionHandler) {
        val userId = currentUserFlow.firstOrNull()?.userId
        val share = homeUiState.value.homeListUiState.selectedShare
        if (userId != null && share is Some) {
            isRefreshing.update { IsRefreshingState.Refreshing }
            runCatching {
                applyPendingEvents(userId, share.value)
            }.onFailure {
                PassLogger.e(TAG, it, "Error in refresh")
                snackbarMessageRepository.emitSnackbarMessage(RefreshError)
            }

            isRefreshing.update { IsRefreshingState.NotRefreshing }
        }
    }

    fun sendItemToTrash(item: ItemUiModel?) = viewModelScope.launch(coroutineExceptionHandler) {
        if (item == null) return@launch

        val userId = currentUserFlow.firstOrNull()?.userId
        if (userId != null) {
            trashItem(userId, item.shareId, item.id)
                .onSuccess {
                    when (item.itemType) {
                        is ItemType.Alias ->
                            snackbarMessageRepository.emitSnackbarMessage(AliasMovedToTrash)
                        is ItemType.Login ->
                            snackbarMessageRepository.emitSnackbarMessage(LoginMovedToTrash)
                        is ItemType.Note ->
                            snackbarMessageRepository.emitSnackbarMessage(NoteMovedToTrash)
                        ItemType.Password -> {}
                    }
                }
        }
    }

    fun copyToClipboard(text: String, homeClipboardType: HomeClipboardType) {
        when (homeClipboardType) {
            HomeClipboardType.Alias -> {
                clipboardManager.copyToClipboard(text = text)
                viewModelScope.launch {
                    snackbarMessageRepository.emitSnackbarMessage(AliasCopied)
                }
            }
            HomeClipboardType.Note -> {
                clipboardManager.copyToClipboard(text = text)
                viewModelScope.launch {
                    snackbarMessageRepository.emitSnackbarMessage(NoteCopied)
                }
            }
            HomeClipboardType.Password -> {
                encryptionContextProvider.withEncryptionContext {
                    clipboardManager.copyToClipboard(
                        text = decrypt(text),
                        isSecure = true
                    )
                }
                viewModelScope.launch {
                    snackbarMessageRepository.emitSnackbarMessage(PasswordCopied)
                }
            }
            HomeClipboardType.Username -> {
                clipboardManager.copyToClipboard(text = text)
                viewModelScope.launch {
                    snackbarMessageRepository.emitSnackbarMessage(UsernameCopied)
                }
            }
        }
    }

    fun setFilterMode(mode: HomeFilterMode) {
        filterModeFlow.update { mode }
    }

    private fun List<ItemUiModel>.sortByTitle() = sortedBy { it.name.lowercase() }

    private fun List<ItemUiModel>.sortByItemType() =
        groupBy { it.itemType.toWeightedInt() }
            .toSortedMap()
            .map { it.value }
            .flatten()

    companion object {
        private const val DEBOUNCE_TIMEOUT = 300L
        private const val TAG = "HomeViewModel"
    }
}
