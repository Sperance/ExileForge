package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.ItemLine
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.influenceSlotText
import com.sperance.exileforge.core.display.influenceTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Influence
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Vital

/**
 * Печать влияния - знак его состояния ([stateGlyph]) на круге его цвета ([influenceColor]): на гнезде полной карточки, на строке влияния
 * и в чипе места влияния. Одна печать на все места, где карточка называет влияние вещи.
 */
@Composable fun InfluenceSealMark(influence: Influence, size: Dp, modifier: Modifier = Modifier) {
    val tint = influenceColor(influence)
    Box(
        modifier.size(size).background(Brush.radialGradient(listOf(tint.copy(alpha = .55f), SealGround)), CircleShape).border(1.dp, tint.copy(alpha = .8f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(stateGlyph(influence.name.lowercase()), influenceTitle(influence), tint = SealInk, modifier = Modifier.size(size * .62f)) }
}

/**
 * Строка влияния печатью - первой над всеми строками полной карточки: печать влияния, над текстом мелко «Бездна · тир 1» цветом
 * влияния, текст строки; касание - [onClick] (тир и место ролла).
 */
@Composable fun InfluenceSealCard(line: ItemLine, onClick: () -> Unit) {
    val influence = line.marks.influence ?: return
    val tint = influenceColor(influence)
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape)
            .background(Brush.radialGradient(listOf(tint.copy(alpha = .26f), tint.copy(alpha = .06f)), Offset.Zero, SEAL_GLOW_RADIUS), shape)
            .border(1.dp, tint.copy(alpha = .5f), shape)
            .clickable(role = Role.Button, onClickLabel = line.text, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfluenceSealMark(influence, 36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                ui("card.influence_seal", influenceTitle(influence), line.roll.tier).uppercase(),
                color = tint,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (line.amplified != null) Text(ui("relic.amplified"), color = Vital, style = MaterialTheme.typography.labelMedium)
                Text(line.text, color = Parchment, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Места вещи чипами под именем полной карточки: «Преф. a/b», «Суф. c/d» и, когда строка влияния стоит, «Влияние 1/1» с печатью;
 * пустое место влияния не печатается. У редкости без мест (обычная, уникальная) чипов нет.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SideChips(item: ItemView) {
    val sides = item.sides ?: return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SideChip(ui("card.prefixes", sides.prefixes, sides.prefixLimit), null)
        SideChip(ui("card.suffixes", sides.suffixes, sides.suffixLimit), null)
        item.influenceSlots?.let { slots -> SideChip(influenceSlotText(slots), item.influenceLine?.marks?.influence) }
    }
}

/** Чип места: [text], у места влияния - его печать [influence] и цвет. */
@Composable private fun SideChip(text: String, influence: Influence?) {
    val tint = influence?.let(::influenceColor)
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.height(SIDE_CHIP_HEIGHT).background(tint?.copy(alpha = .16f) ?: Color.White.copy(alpha = .05f), shape)
            .border(1.dp, tint?.copy(alpha = .6f) ?: Color.White.copy(alpha = .14f), shape).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        influence?.let { InfluenceSealMark(it, 16.dp) }
        Text(text, color = Parchment, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Высота чипа места. */
private val SIDE_CHIP_HEIGHT = 28.dp

/** Радиус свечения печати строки влияния от её левого края, px. */
private const val SEAL_GLOW_RADIUS = 900f

/** Тёмный край круга печати. */
private val SealGround = Color(0xFF140F1C)

/** Знак на печати: светлый, чтобы читался на любом цвете влияния. */
private val SealInk = Color(0xFFF4ECFF)
