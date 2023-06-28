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

package proton.android.pass.autofill.entities

import android.text.InputType
import android.view.autofill.AutofillValue
import kotlinx.serialization.Serializable
import proton.android.pass.common.api.Option

@JvmInline
@Serializable
value class InputTypeValue(val value: Int) {

    fun hasVariations(vararg variations: Int): Boolean =
        variations.any { this.value and InputType.TYPE_MASK_VARIATION == it }
}

data class AutofillNode(
    val id: AutofillFieldId?,
    val className: String?,
    val isImportantForAutofill: Boolean,
    val text: String?,
    val autofillValue: AutofillValue?,
    val inputType: InputTypeValue,
    val autofillHints: List<String>,
    val htmlAttributes: List<Pair<String, String>>,
    val children: List<AutofillNode>,
    val url: Option<String>,
    val hintKeywordList: List<CharSequence>
)
