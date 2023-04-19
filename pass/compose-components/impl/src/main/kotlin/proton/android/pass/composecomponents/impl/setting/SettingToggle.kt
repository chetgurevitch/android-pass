package proton.android.pass.composecomponents.impl.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak
import proton.android.pass.commonui.api.PassTheme

@Composable
fun SettingToggle(
    modifier: Modifier = Modifier,
    text: String,
    isChecked: Boolean,
    onClick: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onClick(!isChecked) }
            .fillMaxWidth()
            .padding(16.dp, 26.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = ProtonTheme.typography.defaultWeak,
            color = PassTheme.colors.textNorm
        )
        Switch(
            checked = isChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PassTheme.colors.interactionNormMajor1,
            ),
            onCheckedChange = null
        )
    }
}
