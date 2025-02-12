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

package proton.android.pass.data.impl.crypto

import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.repository.PublicAddressRepository
import me.proton.core.user.domain.extension.primary
import me.proton.core.user.domain.repository.UserAddressRepository
import me.proton.core.user.domain.repository.UserRepository
import proton.android.pass.crypto.api.usecases.AcceptInvite
import proton.android.pass.crypto.api.usecases.EncryptedInviteKey
import proton.android.pass.crypto.api.usecases.InvitedUserMode
import proton.android.pass.data.impl.local.InviteAndKeysEntity
import proton.android.pass.data.impl.requests.InviteKeyRotation
import javax.inject.Inject

interface EncryptInviteKeys {
    suspend operator fun invoke(
        userId: UserId,
        invite: InviteAndKeysEntity,
        invitedUserMode: InvitedUserMode
    ): List<InviteKeyRotation>
}

class EncryptInviteKeysImpl @Inject constructor(
    private val acceptInvite: AcceptInvite,
    private val userRepository: UserRepository,
    private val addressRepository: UserAddressRepository,
    private val publicAddressRepository: PublicAddressRepository
) : EncryptInviteKeys {
    override suspend fun invoke(
        userId: UserId,
        invite: InviteAndKeysEntity,
        invitedUserMode: InvitedUserMode
    ): List<InviteKeyRotation> {
        val user = userRepository.getUser(userId)
        val address = addressRepository.getAddresses(userId).primary()
            ?: throw IllegalStateException("User has no primary address")

        val privateAddressKeys = address.keys.map { it.privateKey }
        val inviterAddressKeys = publicAddressRepository
            .getPublicAddress(userId, invite.inviteEntity.inviterEmail)
            .keys
            .map { it.publicKey }

        val inviteKeys = invite.inviteKeys.map {
            EncryptedInviteKey(
                keyRotation = it.keyRotation,
                key = it.key
            )
        }
        val encryptedKeys = acceptInvite(
            invitedUser = user,
            invitedUserAddressKeys = privateAddressKeys,
            inviterAddressKeys = inviterAddressKeys,
            keys = inviteKeys,
            invitedUserMode = invitedUserMode
        )

        return encryptedKeys.keys.map {
            InviteKeyRotation(
                keyRotation = it.keyRotation,
                key = it.key
            )
        }
    }
}
