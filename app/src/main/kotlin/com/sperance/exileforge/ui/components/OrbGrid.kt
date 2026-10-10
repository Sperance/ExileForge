package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.theme.*

/** Код пустого выбора сетки сфер: плитка «любая» ([OrbGrid] с `any`). */
const val ANY_ORB = ""

/**
 * Выбор сферы сеткой витражей (4.6.3): плитки [OrbGlyph] от дешёвой к дорогой, выбранная подсвечена золотом,
 * её имя - под сеткой. Одна на лист продажи аукциона и фильтр витрины.
 *
 * [selected] и ответ [onPick] - код предмета сферы. С подписью [any] первой встаёт плитка пустого выбора
 * ([ANY_ORB]) - «любая сфера» фильтра; без неё выбрать можно только сферу из [orbs].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrbGrid(
    label: String,
    orbs: List<Item>,
    selected: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    any: String? = null,
) {
    val byPrice = remember(orbs) { orbs.sortedBy { it.price } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            any?.let { OrbTile(selected == ANY_ORB, enabled, { onPick(ANY_ORB) }) { Icon(ForgeGlyphs.Orb, it, Modifier.size(22.dp), tint = Muted) } }
            byPrice.forEach { item ->
                val code = item.code.value
                OrbTile(selected == code, enabled, { onPick(code) }) { OrbGlyph(Orb.of(code), Modifier.size(30.dp), itemTitle(code)) }
            }
        }
        Text(
            if (selected == ANY_ORB) any.orEmpty() else itemTitle(selected),
            color = GoldBright,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Плитка сетки: выбранная - в золотой рамке на золотой подложке, остальные - на приподнятой панели. */
@Composable
private fun OrbTile(on: Boolean, enabled: Boolean, onClick: () -> Unit, art: @Composable () -> Unit) {
    val tile = RoundedCornerShape(10.dp)
    Box(
        Modifier.size(44.dp).clip(tile).background(if (on) Gold.copy(alpha = .18f) else PanelRaised, tile)
            .border(if (on) 2.dp else 1.dp, if (on) Gold else Color.Transparent, tile)
            .selectable(selected = on, enabled = enabled, role = Role.RadioButton, onClick = onClick).alpha(if (enabled) 1f else .55f),
        contentAlignment = Alignment.Center,
    ) { art() }
}
