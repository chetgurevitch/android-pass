package proton.android.pass.featurecreateitem.impl.note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import proton.android.pass.common.api.onError
import proton.android.pass.common.api.onSuccess
import proton.android.pass.commonui.api.toUiModel
import proton.android.pass.composecomponents.impl.uievents.IsLoadingState
import proton.android.pass.crypto.api.context.EncryptionContextProvider
import proton.android.pass.data.api.repositories.ItemRepository
import proton.android.pass.data.api.usecases.GetShareById
import proton.android.pass.featurecreateitem.impl.ItemSavedState
import proton.android.pass.featurecreateitem.impl.note.NoteSnackbarMessage.ItemCreationError
import proton.android.pass.log.api.PassLogger
import proton.android.pass.notifications.api.SnackbarMessageRepository
import proton.pass.domain.ShareId
import javax.inject.Inject

@HiltViewModel
class CreateNoteViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val getShare: GetShareById,
    private val itemRepository: ItemRepository,
    private val snackbarMessageRepository: SnackbarMessageRepository,
    private val encryptionContextProvider: EncryptionContextProvider,
    savedStateHandle: SavedStateHandle
) : BaseNoteViewModel(snackbarMessageRepository, savedStateHandle) {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        PassLogger.e(TAG, throwable)
    }

    fun createNote(shareId: ShareId) = viewModelScope.launch(coroutineExceptionHandler) {
        val noteItem = noteItemState.value
        val noteItemValidationErrors = noteItem.validate()
        if (noteItemValidationErrors.isNotEmpty()) {
            noteItemValidationErrorsState.update { noteItemValidationErrors }
        } else {
            isLoadingState.update { IsLoadingState.Loading }
            val userId = accountManager.getPrimaryUserId()
                .first { userId -> userId != null }
            if (userId != null) {
                getShare(userId, shareId)
                    .onSuccess { share ->
                        requireNotNull(share)
                        val itemContents = noteItem.toItemContents()
                        itemRepository.createItem(userId, share, itemContents)
                            .onSuccess { item ->
                                isItemSavedState.update {
                                    encryptionContextProvider.withEncryptionContext {
                                        ItemSavedState.Success(
                                            item.id,
                                            item.toUiModel(this@withEncryptionContext)
                                        )
                                    }
                                }
                            }
                            .onError {
                                val defaultMessage = "Create item error"
                                PassLogger.e(
                                    TAG,
                                    it ?: Exception(defaultMessage),
                                    defaultMessage
                                )
                                snackbarMessageRepository.emitSnackbarMessage(ItemCreationError)
                            }
                    }
                    .onError {
                        val defaultMessage = "Get share error"
                        PassLogger.e(TAG, it ?: Exception(defaultMessage), defaultMessage)
                        snackbarMessageRepository.emitSnackbarMessage(ItemCreationError)
                    }
            } else {
                PassLogger.i(TAG, "Empty User Id")
                snackbarMessageRepository.emitSnackbarMessage(ItemCreationError)
            }
            isLoadingState.update { IsLoadingState.NotLoading }
        }
    }

    companion object {
        private const val TAG = "CreateNoteViewModel"
    }
}
