/*
 * Copyright (c) 2023 Proton AG
 * This file is part of Proton AG and Proton Pass.
 *
 * Proton Pass is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Pass is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Pass.  If not, see <https://www.gnu.org/licenses/>.
 */

package proton.android.pass.featuresharing.impl.manage.bottomsheet.inviteoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import proton.android.pass.commonui.api.SavedStateHandleProvider
import proton.android.pass.commonui.api.require
import proton.android.pass.data.api.errors.CannotSendMoreInvitesError
import proton.android.pass.data.api.usecases.CancelInvite
import proton.android.pass.data.api.usecases.ResendInvite
import proton.android.pass.featuresharing.impl.SharingSnackbarMessage
import proton.android.pass.featuresharing.impl.manage.bottomsheet.InviteIdArg
import proton.android.pass.log.api.PassLogger
import proton.android.pass.navigation.api.CommonNavArgId
import proton.android.pass.notifications.api.SnackbarDispatcher
import proton.pass.domain.InviteId
import proton.pass.domain.ShareId
import javax.inject.Inject

@HiltViewModel
class InviteOptionsViewModel @Inject constructor(
    private val snackbarDispatcher: SnackbarDispatcher,
    private val resendInvite: ResendInvite,
    private val cancelInvite: CancelInvite,
    savedState: SavedStateHandleProvider,
) : ViewModel() {

    private val shareId = ShareId(savedState.get().require(CommonNavArgId.ShareId.key))
    private val inviteId = InviteId(savedState.get().require(InviteIdArg.key))

    private val loadingOptionFlow: MutableStateFlow<LoadingOption?> = MutableStateFlow(null)
    private val eventFlow: MutableStateFlow<InviteOptionsEvent> =
        MutableStateFlow(InviteOptionsEvent.Unknown)

    val state: StateFlow<InviteOptionsUiState> = combine(
        loadingOptionFlow,
        eventFlow,
        ::InviteOptionsUiState
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = InviteOptionsUiState.Initial
    )

    fun cancelInvite() = viewModelScope.launch {
        loadingOptionFlow.update { LoadingOption.CancelInvite }
        runCatching {
            cancelInvite.invoke(shareId, inviteId)
        }.onSuccess {
            PassLogger.i(TAG, "Invite canceled")
            eventFlow.update { InviteOptionsEvent.Close(refresh = true) }
            snackbarDispatcher(SharingSnackbarMessage.CancelInviteSuccess)
        }.onFailure {
            PassLogger.w(TAG, it, "Error canceling invite")
            snackbarDispatcher(SharingSnackbarMessage.CancelInviteError)
        }
        loadingOptionFlow.update { null }
    }

    fun resendInvite() = viewModelScope.launch {
        loadingOptionFlow.update { LoadingOption.ResendInvite }
        runCatching {
            resendInvite.invoke(shareId, inviteId)
        }.onSuccess {
            PassLogger.i(TAG, "Invite resent")
            eventFlow.update { InviteOptionsEvent.Close(refresh = true) }
            snackbarDispatcher(SharingSnackbarMessage.ResendInviteSuccess)
        }.onFailure {
            PassLogger.w(TAG, it, "Error resending invite")
            val message = if (it is CannotSendMoreInvitesError) {
                SharingSnackbarMessage.TooManyInvitesSentError
            } else {
                SharingSnackbarMessage.ResendInviteError
            }
            snackbarDispatcher(message)
        }
        loadingOptionFlow.update { null }
    }

    fun clearEvent() = viewModelScope.launch {
        eventFlow.update { InviteOptionsEvent.Unknown }
    }

    companion object {
        private const val TAG = "InviteOptionsViewModel"
    }
}
