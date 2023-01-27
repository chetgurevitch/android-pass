package proton.android.pass.data.impl.extensions

import proton.pass.domain.ItemContents
import proton_pass_item_v1.ItemV1
import proton_pass_item_v1.extraField
import proton_pass_item_v1.extraTotp

fun ItemContents.serializeToProto(): ItemV1.Item {
    val builder = ItemV1.Item.newBuilder()
        .setMetadata(
            ItemV1.Metadata.newBuilder()
                .setName(title)
                .setNote(note)
                .build()
        )
    val contentBuilder = ItemV1.Content.newBuilder()
    val content = when (this) {
        is ItemContents.Login -> {
            if (packageNames.isNotEmpty()) {
                val packageNameList = packageNames.map {
                    ItemV1.AllowedAndroidApp.newBuilder()
                        .setPackageName(it)
                        .build()
                }
                builder.platformSpecific = ItemV1.PlatformSpecific.newBuilder()
                    .setAndroid(
                        ItemV1.AndroidSpecific.newBuilder()
                            .addAllAllowedApps(packageNameList)
                            .build()
                    )
                    .build()
            }
            for (entry in extraTotpSet) {
                builder.addExtraFields(
                    extraField {
                        totp = extraTotp { totpUri = entry }
                    }
                )
            }
            contentBuilder.setLogin(
                ItemV1.ItemLogin.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .setTotpUri(primaryTotp)
                    .addAllUrls(urls)
                    .build()
            )
        }
        is ItemContents.Note -> contentBuilder.setNote(
            ItemV1.ItemNote.newBuilder().build()
        )
        is ItemContents.Alias -> contentBuilder.setAlias(
            ItemV1.ItemAlias.newBuilder().build()
        )
    }.build()

    return builder
        .setContent(content)
        .build()
}
