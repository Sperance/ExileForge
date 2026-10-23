package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.*

/** «✦ +20%» строки списка: качество копии жетоном, без качества - ничего. Касание - подсказка [qualityTip]: что качество усиливает. */
@Composable fun QualityBadge(item: ItemView, modifier: Modifier = Modifier) {
    val quality = item.quality.takeIf { it > 0 } ?: return
    val shape = RoundedCornerShape(50)
    Tipped({ qualityTip(item, quality) }, modifier) {
        Text(
            ui("item.quality_badge", quality),
            color = GoldBright,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.background(Gold.copy(alpha = .12f), shape).border(1.dp, Gold.copy(alpha = .5f), shape).padding(horizontal = 5.dp),
        )
    }
}

/**
 * Качество на левом верхнем углу гнезда или «Астролябии» карточки - зеркально уровню ([ItemLevelMark]) на левом нижнем: «✦ +20%» на
 * плотной тёмной подложке (у «Астролябии» угол лежит над небом, число читается и там), касание в зоне [MarkTouchZone] - подсказка
 * «Качество +20%» с тем, что оно усиливает; тот же заголовок - для чтения с экрана. Без качества значка нет.
 */
@Composable fun ItemQualityMark(item: ItemView, modifier: Modifier = Modifier) {
    val quality = item.quality.takeIf { it > 0 } ?: return
    val shape = RoundedCornerShape(9.dp)
    val label = ui("card.quality", quality)
    Tipped({ qualityTip(item, quality) }, modifier.touchOverhang(MarkTouchZone), contentAlignment = Alignment.Center) {
        Text(
            ui("item.quality_badge", quality),
            color = GoldBright,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 22.dp).heightIn(min = 18.dp).background(BadgeGround, shape).border(1.dp, Gold.copy(alpha = .75f), shape)
                .padding(horizontal = 5.dp).clearAndSetSemantics { contentDescription = label },
        )
    }
}

/** Подсказка качества [quality]: «Качество +N%» и что оно усиливает - числа базы или вид модификаторов катализатора. */
private fun qualityTip(item: ItemView, quality: Int): Tip = Tip(
    ui("card.quality", quality),
    item.catalyst?.let { ui("item.quality_catalyst_hint", ui("enum.catalyst.${it.name}")) } ?: ui("item.quality_hint", quality),
    tint = GoldBright,
)
