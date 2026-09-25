package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.AffixKind
import com.sperance.exileforge.core.display.AffixMarks
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.*

/** Each kind of line in the colour Path of Exile writes it in; the game's own kinds take the tone they already had. */
fun affixTint(kind: AffixKind?): Color = when (kind) {
    AffixKind.PREFIX, AffixKind.SUFFIX, null -> Rune
    AffixKind.IMPLICIT -> Parchment
    AffixKind.ENCHANTMENT -> Color(0xFFB8DAF2)
    AffixKind.CRAFTED -> Crafted
    AffixKind.HANDCRAFTED -> Handcrafted
    AffixKind.FRACTURED -> Fractured
    AffixKind.CORRUPTION -> Color(0xFFD20000)
    AffixKind.ALCHEMY -> Vital
    AffixKind.UNIQUE -> Color(0xFFAF6025)
}

/**
 * The letter in front of a line (2.58.0), as PoE's trade site prints it — «P1», «S3», «I», «F2» —
 * in the kind's colour. A long press spells it out beside the badge: letters are learnt, not guessed.
 * A line with no kind (a passive's) keeps the plain rhombus.
 */
@Composable fun AffixBadge(marks: AffixMarks) {
    val kind = marks.kind
    if (kind == null) { Rhombus(); return }
    val badge = marks.badge.orEmpty()
    val tint = affixTint(kind)
    val title = ui("mod.kind.${kind.name}")
    var explained by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(3.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = title }
            .pointerInput(kind) { detectTapGestures(onLongPress = { explained = !explained }) }) {
        Text(badge, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(min = 22.dp).background(tint.copy(alpha = .14f), shape).border(1.dp, tint.copy(alpha = .7f), shape)
                .padding(horizontal = 4.dp, vertical = 1.dp))
        if (explained) Text(title, color = tint, style = MaterialTheme.typography.labelSmall)
    }
}
