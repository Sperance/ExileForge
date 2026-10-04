package com.sperance.exileforge.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.Term
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.*

/**
 * «Термины» (3.81.0): the game's own words the lines above use — a buff, charges, an ailment, a condition — each a chip whose tap
 * opens its rule. Nothing is drawn when the lines name none.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TermsBlock(terms: List<Term>, modifier: Modifier = Modifier) {
    if (terms.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(ui("term.title"), color = Muted, style = MaterialTheme.typography.labelSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            terms.forEach { term ->
                Tipped({ Tip(term.title, term.text, Rune) }) {
                    Text(
                        "ⓘ ${term.title}",
                        color = Rune,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.border(1.dp, Rune.copy(alpha = .5f), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}
