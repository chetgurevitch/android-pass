package proton.android.pass.crypto.impl.usecases

import com.proton.gopenpgp.armor.Armor.unarmor
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.key.domain.encryptAndSignData
import me.proton.core.key.domain.useKeys
import me.proton.core.user.domain.entity.User
import me.proton.core.user.domain.entity.UserAddress
import proton.android.pass.crypto.api.Base64
import proton.android.pass.crypto.api.EncryptionKey
import proton.android.pass.crypto.api.context.EncryptionContextProvider
import proton.android.pass.crypto.api.context.EncryptionTag
import proton.android.pass.crypto.api.usecases.CreateVault
import proton.android.pass.crypto.api.usecases.CreateVaultOutput
import proton.android.pass.crypto.api.usecases.EncryptedCreateVault
import proton_pass_vault_v1.VaultV1
import javax.inject.Inject

class CreateVaultImpl @Inject constructor(
    private val cryptoContext: CryptoContext,
    private val encryptionContextProvider: EncryptionContextProvider
) : CreateVault {

    override fun createVaultRequest(
        user: User,
        userAddress: UserAddress,
        vaultMetadata: VaultV1.Vault
    ): CreateVaultOutput {
        val vaultKey = EncryptionKey.generate()
        val vaultContents = vaultMetadata.toByteArray()

        val encryptedVaultKey = user.useKeys(cryptoContext) { encryptAndSignData(vaultKey.value()) }
        val encryptedVaultContents = encryptionContextProvider.withEncryptionContext(vaultKey.clone()) {
            encrypt(vaultContents, EncryptionTag.VaultContent)
        }

        return CreateVaultOutput(
            request = EncryptedCreateVault(
                addressId = userAddress.addressId.id,
                content = Base64.encodeBase64String(encryptedVaultContents.array),
                contentFormatVersion = CONTENT_FORMAT_VERSION,
                encryptedVaultKey = Base64.encodeBase64String(unarmor(encryptedVaultKey))

            ),
            shareKey = vaultKey
        )
    }

    companion object {
        const val CONTENT_FORMAT_VERSION = 1
    }
}
