package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import com.sperance.exileforge.ui.components.MutedText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.wearDelta
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.*

/** Whether an item goes on the body at all: a map, a tool and a jewel are placed elsewhere. */
fun wearable(s: ForgeState, instance: EquipmentInstance): Boolean =
    s.world.inventoryBases[instance.equipmentId]?.text("slot")?.let { it != "MAP" && it != "JEWEL" && !it.startsWith("TOOL_") } == true

/**
 * «Если надеть» (2.46.0): what the sheet would become with this item on, added up here by the
 * server's formula, one line per characteristic that moves. An item out of reach says instead, in
 * red, what it needs — and its button stays off.
 */
@Composable fun WearPreview(s: ForgeState, instance: EquipmentInstance) {
    if (!wearable(s, instance) || instance.equipped || instance.socketed) return
    val unmet = s.unmetFor(instance.equipmentId)
    val delta = remember(instance, s.play.hero, s.world.statTables) { s.wearDelta(instance) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (unmet.isNotEmpty()) {
            Text(ui("wear.blocked"), color = LifeRed, style = MaterialTheme.typography.labelLarge)
            unmet.forEach { Text(requirementReason(it, s.lang), color = LifeRed, style = MaterialTheme.typography.bodySmall) }
            return@Column
        }
        Text(ui("wear.title"), color = Gold, style = MaterialTheme.typography.labelLarge)
        if (delta.isEmpty()) MutedText(ui("wear.nothing"))
        delta.forEach { line ->
            val tone = if (line.change > 0) Vital else LifeRed
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatIcon(line.stat, Muted, Modifier.size(14.dp))
                Text(statTitle(line.stat), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                MutedText(ui("wear.from_to", statNumber(line.stat, line.before), statNumber(line.stat, line.after)), style = MaterialTheme.typography.labelSmall)
                Text((if (line.change > 0) "+" else "−") + statNumber(line.stat, kotlin.math.abs(line.change)), color = tone,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
