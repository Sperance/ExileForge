package com.sperance.exileforge.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.AffixKind
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.weaponTitle
import com.sperance.exileforge.ui.theme.Parchment

/**
 * Плитка вещи (3.90.0) - сжатая карточка «Реликвария» в том же облике редкости ([relicGround], [RelicSocket], [RelicLook.nameStyle]):
 * гнездо и имя в шапке, под ним «что это · редкость», база одной строкой, затем каждая строка вещи мелким шрифтом в цвете её вида -
 * без значков и тиров. [footer] - что добавляет список под строками (вердикт, «Надеть»).
 * Строка тайника (3.90.3): [trailing] - напротив имени (цена, вердикт, отметки), [selected] - золотая рамка, [onClick] - вся плитка.
 */
@Composable
fun ItemTile(
    item: ItemView,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable ColumnScope.() -> Unit)? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val look = relicLook(item.rarity)
    Column(
        modifier.fillMaxWidth().relicGround(look, RoundedCornerShape(16.dp), selected).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RelicSocket(item, look, 40.dp)
            Column(Modifier.weight(1f)) {
                Text(item.title, style = look.nameStyle(15), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(item.weaponType?.let { weaponTitle(it) } ?: slotTitle(item.slot), rarityTitle(item.rarity)).joinToString(" · "),
                    color = look.muted,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = .4.sp,
                    maxLines = 1,
                )
            }
            trailing?.let { Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp), content = it) }
        }
        if (item.base.isNotEmpty()) {
            Text(
                buildAnnotatedString {
                    item.base.forEachIndexed { i, property ->
                        if (i > 0) append(" · ")
                        append(basePropertyText(property, withBase = false))
                    }
                },
                color = Parchment,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (item.lines.isNotEmpty()) {
            RelicDivider(look)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item.lines.forEachIndexed { i, line ->
                    Text(line.text, color = modColor(line.marks), style = MaterialTheme.typography.labelSmall)
                    // Врождённые строки - над чертой, как в полной карточке.
                    val next = item.lines.getOrNull(i + 1)
                    if (line.marks.kind == AffixKind.IMPLICIT && next != null && next.marks.kind != AffixKind.IMPLICIT) {
                        HorizontalDivider(Modifier.padding(vertical = 2.dp), thickness = .5.dp, color = Color.White.copy(alpha = .08f))
                    }
                }
            }
        }
        footer()
    }
}
