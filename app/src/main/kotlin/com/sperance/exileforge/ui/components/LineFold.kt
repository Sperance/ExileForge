package com.sperance.exileforge.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemLine
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.i18n.ui

/**
 * Как плита [ItemRow] показывает строки вещи (4.8.0, макет «Аукцион», вариант A): все сразу или свёрнутыми под чип
 * «Свойства · N». Свёртка - только облик списка: сервер о ней не знает.
 */
enum class LineFold {
    /** Все строки вещи, как в тайнике. */
    OPEN,

    /** Строки под чипом «Свойства · N ▾», нажатие раскрывает только эту плиту. */
    FOLDED,
    ;

    companion object {
        /** Свёртка лота: свойства уникальной и мифической вещи свёрнуты, пока игрок не велел раскрывать их ([open]). */
        fun legend(item: ItemView, open: Boolean): LineFold = if (item.rarity.fixed && !open) FOLDED else OPEN
    }
}

/** Строки плиты по свёртке [fold]: малые [RelicModLine], у свёрнутой - чип раскрытия в нити [look] над ними. */
@Composable internal fun FoldedLines(lines: List<ItemLine>, fold: LineFold, look: RelicLook) {
    var open by rememberSaveable { mutableStateOf(false) }
    if (fold == LineFold.FOLDED) FoldChip(ui("row.properties", lines.size), open, look) { open = !open }
    if (fold == LineFold.OPEN || open) {
        Column(Modifier.padding(start = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            lines.forEach { RelicModLine(it, null, big = false) }
        }
    }
}

/** Чип «Свойства · N ▾/▴»: пилюля в золотой нити облика вещи. */
@Composable private fun FoldChip(label: String, open: Boolean, look: RelicLook, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        label + if (open) " ▴" else " ▾",
        color = look.gold,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(shape).border(1.dp, look.gold.copy(alpha = .55f), shape).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
