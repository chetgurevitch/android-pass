package proton.android.pass.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import proton.android.pass.composecomponents.impl.bottomsheet.PassModalBottomSheetLayout
import proton.android.pass.composecomponents.impl.messages.OfflineIndicator
import proton.android.pass.composecomponents.impl.messages.PassSnackbarHost
import proton.android.pass.composecomponents.impl.messages.PassSnackbarHostState
import proton.android.pass.composecomponents.impl.messages.rememberPassSnackbarHostState
import proton.android.pass.featurevault.impl.VaultList
import proton.android.pass.navigation.api.rememberAppNavigator
import proton.android.pass.network.api.NetworkStatus
import proton.android.pass.notifications.api.SnackbarMessage
import proton.android.pass.ui.internal.InternalDrawerState
import proton.android.pass.ui.internal.InternalDrawerValue
import proton.android.pass.ui.internal.rememberInternalDrawerState

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalMaterialApi::class)
@Composable
fun PassAppContent(
    modifier: Modifier = Modifier,
    appUiState: AppUiState,
    coreNavigation: CoreNavigation,
    onSnackbarMessageDelivered: () -> Unit,
    finishActivity: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val appNavigator = rememberAppNavigator(
        bottomSheetNavigator = rememberBottomSheetNavigator(bottomSheetState),
    )
    val scaffoldState = rememberScaffoldState()
    val passSnackbarHostState = rememberPassSnackbarHostState(scaffoldState.snackbarHostState)

    SnackBarLaunchedEffect(
        appUiState.snackbarMessage.value(),
        passSnackbarHostState,
        onSnackbarMessageDelivered
    )
    val internalDrawerState: InternalDrawerState =
        rememberInternalDrawerState(InternalDrawerValue.Closed)
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    Scaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        snackbarHost = { PassSnackbarHost(snackbarHostState = passSnackbarHostState) }
    ) { contentPadding ->
        InternalDrawer(
            drawerState = internalDrawerState,
            onOpenVault = {
                coroutineScope.launch {
                    internalDrawerState.close()
                }
                appNavigator.navigate(VaultList)
            },
            content = {
                Column(modifier = Modifier.padding(contentPadding)) {
                    PassModalBottomSheetLayout(appNavigator.bottomSheetNavigator) {
                        PassNavHost(
                            modifier = Modifier.weight(1f),
                            appNavigator = appNavigator,
                            finishActivity = finishActivity,
                            onReportProblemClick = { coreNavigation.onReport() },
                            dismissBottomSheet = {
                                coroutineScope.launch {
                                    bottomSheetState.hide()
                                }
                            },
                            onLogout = { coreNavigation.onRemove(null) }
                        )
                    }

                    AnimatedVisibility(visible = appUiState.networkStatus == NetworkStatus.Offline) {
                        OfflineIndicator()
                    }
                }
            }
        )
    }
}

@Composable
private fun SnackBarLaunchedEffect(
    snackBarMessage: SnackbarMessage?,
    passSnackBarHostState: PassSnackbarHostState,
    onSnackBarMessageDelivered: () -> Unit
) {
    snackBarMessage ?: return
    val snackBarMessageLocale = stringResource(id = snackBarMessage.id)
    LaunchedEffect(snackBarMessage) {
        passSnackBarHostState.showSnackbar(
            snackBarMessage.type,
            snackBarMessageLocale
        )
        onSnackBarMessageDelivered()
    }
}

@ExperimentalMaterialNavigationApi
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun rememberBottomSheetNavigator(
    sheetState: ModalBottomSheetState,
): BottomSheetNavigator {
    return remember(sheetState) {
        BottomSheetNavigator(sheetState = sheetState)
    }
}
