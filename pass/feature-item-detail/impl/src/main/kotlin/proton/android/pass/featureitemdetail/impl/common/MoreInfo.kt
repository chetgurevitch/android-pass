package proton.android.pass.featureitemdetail.impl.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import me.proton.core.compose.theme.ProtonTheme
import proton.android.pass.common.api.Option
import proton.android.pass.common.api.Some
import proton.android.pass.commonui.api.DateFormatUtils
import proton.android.pass.commonui.api.ThemePairPreviewProvider
import proton.android.pass.featureitemdetail.impl.R

@Suppress("MagicNumber")
@Composable
fun MoreInfo(
    modifier: Modifier = Modifier,
    moreInfoUiState: MoreInfoUiState,
    shouldShowMoreInfoInitially: Boolean = false
) {
    moreInfoUiState.createdTime
    Column(modifier = modifier.fillMaxWidth()) {
        var showMoreInfo by remember { mutableStateOf(shouldShowMoreInfoInitially) }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showMoreInfo = !showMoreInfo },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(me.proton.core.presentation.R.drawable.ic_info_circle),
                contentDescription = stringResource(R.string.more_info_icon),
                tint = ProtonTheme.colors.iconWeak
            )
            MoreInfoText(
                modifier = Modifier.padding(8.dp),
                text = stringResource(R.string.more_info_title)
            )
        }
        AnimatedVisibility(visible = showMoreInfo) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier.weight(0.3f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoreInfoLastAutofilledTitle(lastAutofilled = moreInfoUiState.lastAutofilled)
                    MoreInfoModifiedTitle(numRevisions = moreInfoUiState.numRevisions)
                    // Created
                    MoreInfoText(text = stringResource(R.string.more_info_created))
                }
                Column(
                    modifier = Modifier.weight(0.7f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoreInfoLastAutofilledContent(moreInfoUiState = moreInfoUiState)
                    MoreInfoModifiedContent(moreInfoUiState = moreInfoUiState)
                    // Created
                    MoreInfoText(
                        text = DateFormatUtils.formatInstantText(
                            now = moreInfoUiState.now,
                            toFormat = moreInfoUiState.createdTime
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreInfoLastAutofilledTitle(
    modifier: Modifier = Modifier,
    lastAutofilled: Option<Instant>
) {
    if (lastAutofilled.isNotEmpty()) {
        MoreInfoText(modifier = modifier, text = stringResource(R.string.more_info_autofilled))
    }
}

@Composable
private fun MoreInfoModifiedTitle(
    modifier: Modifier = Modifier,
    numRevisions: Long
) {
    if (numRevisions > 1) {
        MoreInfoText(modifier = modifier, text = stringResource(R.string.more_info_modified))
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun MoreInfoLastAutofilledContent(
    modifier: Modifier = Modifier,
    moreInfoUiState: MoreInfoUiState
) {
    if (moreInfoUiState.lastAutofilled is Some) {
        MoreInfoText(
            modifier = modifier,
            text = DateFormatUtils.formatInstantText(
                now = moreInfoUiState.now,
                toFormat = moreInfoUiState.lastAutofilled.value
            )
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ColumnScope.MoreInfoModifiedContent(
    modifier: Modifier = Modifier,
    moreInfoUiState: MoreInfoUiState
) {
    val modifiedTimes = moreInfoUiState.numRevisions - 1
    if (modifiedTimes > 0) {
        val lastUpdateString = DateFormatUtils.formatInstantText(
            now = moreInfoUiState.now,
            toFormat = moreInfoUiState.lastModified
        )

        MoreInfoText(
            modifier = modifier,
            text = pluralStringResource(
                id = R.plurals.more_info_modified_times,
                count = modifiedTimes.toInt(),
                modifiedTimes.toInt()
            )
        )
        MoreInfoText(
            modifier = modifier,
            text = stringResource(R.string.more_info_last_time_modified, lastUpdateString)
        )
    }
}

class ThemedMoreInfoPreviewProvider :
    ThemePairPreviewProvider<MoreInfoPreview>(MoreInfoPreviewProvider())

@Preview
@Composable
fun MoreInfoPreview(
    @PreviewParameter(ThemedMoreInfoPreviewProvider::class) input: Pair<Boolean, MoreInfoPreview>
) {
    ProtonTheme(isDark = input.first) {
        Surface {
            MoreInfo(
                shouldShowMoreInfoInitially = input.second.showMoreInfo,
                moreInfoUiState = input.second.uiState
            )
        }
    }
}
