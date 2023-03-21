package proton.android.pass.featuresettings.impl

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.overline
import proton.android.pass.commonui.api.PassTheme
import proton.android.pass.commonui.api.PassTypography
import proton.android.pass.composecomponents.impl.container.Circle
import proton.android.pass.composecomponents.impl.topbar.iconbutton.BackArrowCircleIconButton
import me.proton.core.presentation.R as CoreR

@Composable
fun LogViewContent(
    modifier: Modifier = Modifier,
    content: String,
    onUpClick: () -> Unit,
    onShareLogsClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.view_logs_title),
                        style = PassTypography.hero
                    )
                },
                navigationIcon = {
                    BackArrowCircleIconButton(
                        modifier = Modifier.padding(12.dp, 4.dp),
                        color = PassTheme.colors.accentBrandOpaque,
                        onUpClick = onUpClick
                    )
                },
                actions = {
                    Circle(
                        modifier = Modifier.padding(12.dp, 4.dp),
                        backgroundColor = PassTheme.colors.accentBrandOpaque,
                        onClick = onShareLogsClick
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_proton_arrow_up_from_square),
                            contentDescription = "",
                            tint = PassTheme.colors.accentBrandOpaque
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        Text(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(16.dp),
            text = content,
            style = ProtonTheme.typography.overline
        )
    }
}
