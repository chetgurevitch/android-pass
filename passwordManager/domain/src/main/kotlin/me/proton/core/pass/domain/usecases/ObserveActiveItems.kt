package me.proton.core.pass.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.pass.common.api.Result
import me.proton.core.pass.domain.Item
import me.proton.core.pass.domain.ItemState
import me.proton.core.pass.domain.ShareId
import me.proton.core.pass.domain.ShareSelection
import me.proton.core.pass.domain.repositories.ItemRepository
import me.proton.core.user.domain.UserManager
import javax.inject.Inject

interface ObserveActiveItems {
    operator fun invoke(): Flow<Result<List<Item>>>
}

class ObserveActiveItemsImpl @Inject constructor(
    accountManager: AccountManager,
    private val userManager: UserManager,
    private val observeActiveShare: ObserveActiveShare,
    private val itemRepository: ItemRepository
) : ObserveActiveItems {

    private val getCurrentUserIdFlow = accountManager.getPrimaryUserId()
        .filterNotNull()
        .flatMapLatest { userManager.observeUser(it) }
        .distinctUntilChanged()

    override operator fun invoke(): Flow<Result<List<Item>>> = observeActiveShare()
        .flatMapLatest { result: Result<ShareId?> ->
            when (result) {
                is Result.Error -> return@flatMapLatest flowOf(Result.Error(result.exception))
                Result.Loading -> return@flatMapLatest flowOf(Result.Loading)
                is Result.Success -> {
                    flowOf(result.data)
                        .filterNotNull()
                        .combine(getCurrentUserIdFlow.filterNotNull()) { share, user -> share to user }
                        .flatMapLatest { v ->
                            itemRepository.observeItems(
                                v.second.userId,
                                ShareSelection.Share(v.first),
                                ItemState.Active
                            )
                        }
                }
            }
        }
}
