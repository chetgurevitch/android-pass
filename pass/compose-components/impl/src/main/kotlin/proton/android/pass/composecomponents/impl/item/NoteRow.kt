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

package proton.android.pass.composecomponents.impl.item

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import kotlinx.collections.immutable.persistentListOf
import proton.android.pass.common.api.removeAccents
import proton.android.pass.commonui.api.PassTheme
import proton.android.pass.commonui.api.ThemePairPreviewProvider
import proton.android.pass.commonuimodels.api.ItemUiModel
import proton.android.pass.composecomponents.impl.item.icon.NoteIcon
import proton.pass.domain.ItemContents

private const val MAX_LINES_NOTE_DETAIL = 1
private const val MAX_NOTE_CHARS_PREVIEW = 128

@Composable
fun NoteRow(
    modifier: Modifier = Modifier,
    item: ItemUiModel,
    highlight: String = "",
    vaultIcon: Int? = null
) {
    val content = item.contents as ItemContents.Note

    val highlightColor = PassTheme.colors.interactionNorm
    val (title, note) = remember(content.title, content.note, highlight) {
        getHighlightedFields(content.title, content.note, highlight, highlightColor)
    }

    ItemRow(
        modifier = modifier,
        icon = { NoteIcon() },
        title = title,
        subtitles = persistentListOf(note),
        vaultIcon = vaultIcon
    )
}

private fun getHighlightedFields(
    title: String,
    note: String,
    highlight: String,
    highlightColor: Color,
): NoteHighlightFields {
    var annotatedTitle = AnnotatedString(title.take(MAX_NOTE_CHARS_PREVIEW))
    val annotatedNote = if (highlight.isNotBlank()) {
        val regex = highlight.toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.LITERAL))
        val cleanTitle = title.removeAccents()
        val titleMatches = regex.findAll(cleanTitle)
        if (titleMatches.any()) {
            annotatedTitle = title.highlight(titleMatches, highlightColor)
        }
        val noteWithoutNewLines = note.replace("\n", " ")
        val cleanNote = noteWithoutNewLines.removeAccents()
        val noteMatches = regex.findAll(cleanNote)
        if (noteMatches.any()) {
            noteWithoutNewLines.highlight(noteMatches, highlightColor)
        } else {
            AnnotatedString(noteWithoutNewLines.take(MAX_NOTE_CHARS_PREVIEW))
        }
    } else {
        val firstLines = note.lines().take(MAX_LINES_NOTE_DETAIL).joinToString(" ")
        AnnotatedString(firstLines.take(MAX_NOTE_CHARS_PREVIEW))
    }

    return NoteHighlightFields(annotatedTitle, annotatedNote)
}

@Stable
private data class NoteHighlightFields(
    val title: AnnotatedString,
    val note: AnnotatedString
)

class ThemedNoteItemPreviewProvider :
    ThemePairPreviewProvider<NoteRowParameter>(NoteRowPreviewProvider())

@Preview
@Composable
fun NoteRowPreview(
    @PreviewParameter(ThemedNoteItemPreviewProvider::class) input: Pair<Boolean, NoteRowParameter>
) {
    PassTheme(isDark = input.first) {
        Surface {
            NoteRow(
                item = input.second.model,
                highlight = input.second.highlight
            )
        }
    }
}
