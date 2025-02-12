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

package proton.android.pass.data.impl.usecases.capabilities

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import proton.android.pass.data.api.usecases.GetVaultById
import proton.android.pass.data.api.usecases.capabilities.CanShareVault
import proton.android.pass.log.api.PassLogger
import proton.android.pass.preferences.FeatureFlag
import proton.android.pass.preferences.FeatureFlagsPreferencesRepository
import proton.pass.domain.ShareId
import proton.pass.domain.ShareRole
import proton.pass.domain.Vault
import javax.inject.Inject

class CanShareVaultImpl @Inject constructor(
    private val featureFlagsPreferencesRepository: FeatureFlagsPreferencesRepository,
    private val getVaultById: GetVaultById,
) : CanShareVault {

    override suspend fun invoke(shareId: ShareId): Boolean {
        val vault = runCatching { getVaultById(shareId = shareId).first() }.getOrElse {
            PassLogger.w(TAG, it, "canShare vault not found")
            return false
        }

        return invoke(vault)
    }

    override suspend fun invoke(vault: Vault): Boolean {
        val isSharingEnabled = getSharingEnabledFlag()
        if (!isSharingEnabled) return false

        return when {
            vault.members >= vault.maxMembers -> false
            vault.isPrimary -> false
            vault.isOwned -> true
            vault.role == ShareRole.Admin -> true
            else -> false
        }
    }

    private suspend fun getSharingEnabledFlag() = featureFlagsPreferencesRepository
        .get<Boolean>(FeatureFlag.SHARING_V1)
        .firstOrNull()
        ?: false

    companion object {
        private const val TAG = "CanShareVaultImpl"
    }
}
