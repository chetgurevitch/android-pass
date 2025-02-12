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

import proton.android.pass.common.api.Option
import proton.android.pass.commonuimodels.api.ItemUiModel
import proton.android.pass.featureitemcreate.impl.bottomsheets.customfield.CustomFieldType
import proton.pass.domain.ItemId
import proton.pass.domain.ShareId

sealed interface CreateLoginNavigation {
    data class LoginCreated(val itemUiModel: ItemUiModel) : CreateLoginNavigation
    data class SelectVault(val shareId: ShareId) : CreateLoginNavigation
}

sealed interface UpdateLoginNavigation {
    data class LoginUpdated(val shareId: ShareId, val itemId: ItemId) : UpdateLoginNavigation
}

sealed interface BaseLoginNavigation {
    data class OnCreateLoginEvent(val event: CreateLoginNavigation) : BaseLoginNavigation
    data class OnUpdateLoginEvent(val event: UpdateLoginNavigation) : BaseLoginNavigation

    data class CreateAlias(
        val shareId: ShareId,
        val showUpgrade: Boolean,
        val title: Option<String>
    ) : BaseLoginNavigation

    object GeneratePassword : BaseLoginNavigation
    object Upgrade : BaseLoginNavigation
    data class ScanTotp(
        val index: Option<Int>
    ) : BaseLoginNavigation
    object Close : BaseLoginNavigation

    data class AliasOptions(
        val shareId: ShareId,
        val showUpgrade: Boolean,
    ) : BaseLoginNavigation
    object DeleteAlias : BaseLoginNavigation
    data class EditAlias(
        val shareId: ShareId,
        val showUpgrade: Boolean
    ) : BaseLoginNavigation

    object AddCustomField : BaseLoginNavigation
    data class CustomFieldTypeSelected(
        val type: CustomFieldType
    ) : BaseLoginNavigation

    data class CustomFieldOptions(val currentValue: String, val index: Int) : BaseLoginNavigation
    data class EditCustomField(val currentValue: String, val index: Int) : BaseLoginNavigation
    object RemovedCustomField : BaseLoginNavigation

    @JvmInline
    value class TotpSuccess(val results: Map<String, Any>) : BaseLoginNavigation
    object TotpCancel : BaseLoginNavigation

    @JvmInline
    value class OpenImagePicker(val index: Option<Int>) : BaseLoginNavigation
}
