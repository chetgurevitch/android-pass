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

package proton.android.pass.featureitemcreate.impl.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import proton.android.pass.common.api.toOption
import proton.android.pass.composecomponents.impl.form.SimpleNoteSection
import proton.android.pass.composecomponents.impl.item.LinkedAppsListSection
import proton.android.pass.featureitemcreate.impl.login.LoginStickyFormOptionsContentType.AddTotp
import proton.android.pass.featureitemcreate.impl.login.LoginStickyFormOptionsContentType.AliasOptions
import proton.android.pass.featureitemcreate.impl.login.LoginStickyFormOptionsContentType.GeneratePassword
import proton.android.pass.featureitemcreate.impl.login.LoginStickyFormOptionsContentType.None
import proton.android.pass.featureitemcreate.impl.login.customfields.CustomFieldsContent

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("UnusedPrivateMember")
@Composable
internal fun LoginItemForm(
    modifier: Modifier = Modifier,
    loginItemFormState: LoginItemFormState,
    canUseCustomFields: Boolean,
    isEditAllowed: Boolean,
    totpUiState: TotpUiState,
    focusedField: LoginField?,
    customFieldValidationErrors: ImmutableList<LoginItemValidationErrors.CustomFieldValidationError>,
    showCreateAliasButton: Boolean,
    primaryEmail: String?,
    isUpdate: Boolean,
    isTotpError: Boolean,
    focusLastWebsite: Boolean,
    canUpdateUsername: Boolean,
    websitesWithErrors: ImmutableList<Int>,
    onEvent: (LoginContentEvent) -> Unit,
    onGeneratePasswordClick: () -> Unit,
    onCreateAliasClick: () -> Unit,
    onAliasOptionsClick: () -> Unit,
    onNavigate: (BaseLoginNavigation) -> Unit,
    titleSection: @Composable (ColumnScope.() -> Unit),
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(modifier = modifier) {
        val currentStickyFormOption = when (focusedField) {
            LoginField.Username -> AliasOptions
            LoginField.Password -> GeneratePassword
            LoginField.PrimaryTotp,
            is LoginCustomField.CustomFieldTOTP -> AddTotp

            is LoginCustomField.CustomFieldHidden,
            is LoginCustomField.CustomFieldText,
            LoginField.Title,
            null -> None
        }

        val isCurrentStickyVisible = currentStickyFormOption != None

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            titleSection()
            MainLoginSection(
                loginItemFormState = loginItemFormState,
                canUpdateUsername = canUpdateUsername,
                totpUiState = totpUiState,
                isEditAllowed = isEditAllowed,
                isTotpError = isTotpError,
                onEvent = onEvent,
                onAliasOptionsClick = onAliasOptionsClick,
                onFocusChange = { field, isFocused ->
                    onEvent(LoginContentEvent.OnFocusChange(field, isFocused))
                },
                onUpgrade = { onNavigate(BaseLoginNavigation.Upgrade) }
            )
            WebsitesSection(
                websites = loginItemFormState.urls.toImmutableList(),
                isEditAllowed = isEditAllowed,
                websitesWithErrors = websitesWithErrors,
                focusLastWebsite = focusLastWebsite,
                onWebsiteSectionEvent = { onEvent(LoginContentEvent.OnWebsiteEvent(it)) }
            )
            SimpleNoteSection(
                value = loginItemFormState.note,
                enabled = isEditAllowed,
                onChange = { onEvent(LoginContentEvent.OnNoteChange(it)) }
            )
            CustomFieldsContent(
                customFields = loginItemFormState.customFields.toImmutableList(),
                focusedField = focusedField as? LoginCustomField,
                canEdit = isEditAllowed,
                canUseCustomFields = canUseCustomFields,
                validationErrors = customFieldValidationErrors,
                onEvent = { onEvent(LoginContentEvent.OnCustomFieldEvent(it)) }
            )
            if (isUpdate) {
                LinkedAppsListSection(
                    packageInfoUiSet = loginItemFormState.packageInfoSet.toImmutableSet(),
                    isEditable = true,
                    onLinkedAppDelete = { onEvent(LoginContentEvent.OnLinkedAppDelete(it)) }
                )
            }
            if (isCurrentStickyVisible) {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding(),
            visible = isCurrentStickyVisible,
            enter = expandVertically()
        ) {
            when (currentStickyFormOption) {
                GeneratePassword ->
                    StickyGeneratePassword(
                        onClick = {
                            onGeneratePasswordClick()
                            keyboardController?.hide()
                        }
                    )

                AliasOptions -> StickyUsernameOptions(
                    showCreateAliasButton = showCreateAliasButton,
                    primaryEmail = primaryEmail,
                    onCreateAliasClick = {
                        onCreateAliasClick()
                        keyboardController?.hide()
                    },
                    onPrefillCurrentEmailClick = {
                        onEvent(LoginContentEvent.OnUsernameChange(it))
                        keyboardController?.hide()
                    }
                )

                AddTotp -> StickyTotpOptions(
                    onPasteCode = {
                        onEvent(LoginContentEvent.PasteTotp)
                        keyboardController?.hide()
                    },
                    onScanCode = {
                        val index = (focusedField as? LoginCustomField)?.index
                        onNavigate(BaseLoginNavigation.ScanTotp(index.toOption()))
                        keyboardController?.hide()
                    }
                )

                None -> {}
            }
        }
    }
}
