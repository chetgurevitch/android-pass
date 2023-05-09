package proton.android.pass.featureitemcreate.impl.alias.mailboxes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import proton.android.pass.composecomponents.impl.dialogs.NoPaddingDialog
import proton.android.pass.featureitemcreate.impl.alias.SelectedAliasMailboxUiModel

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun SelectMailboxesDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    mailboxes: List<SelectedAliasMailboxUiModel>,
    onMailboxesChanged: (List<SelectedAliasMailboxUiModel>) -> Unit,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
) {
    if (!show) return
    val viewModel: SelectMailboxesDialogViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.setMailboxes(mailboxes)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NoPaddingDialog(modifier = modifier, onDismissRequest = onDismiss) {
        SelectMailboxesDialogContent(
            state = uiState,
            onConfirm = { onMailboxesChanged(uiState.mailboxes) },
            onDismiss = onDismiss,
            onUpgrade = onUpgrade,
            onMailboxToggled = { viewModel.onMailboxChanged(it) }
        )
    }
}
