package com.sperance.exileforge.ui.screens.passives

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.passives.*
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.components.OrnateDivider
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ForgeIcon
import com.sperance.exileforge.ui.icons.LocalForgeIcons
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted

@Composable internal fun PassiveNodeDetails(node: PassiveNode, state: PassiveState, busy: Boolean, onAction: (PassiveAction, String) -> Unit) {
    val learned = node.id in state.allocated
    ForgePanel(accent = if(learned) Gold else Muted) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ForgeIcon(LocalForgeIcons.current.forNode(node), Modifier.size(34.dp), description = node.name) {
                Icon(ForgeGlyphs.Constellation, null, tint = if(learned) GoldBright else Muted, modifier = Modifier.size(22.dp))
            }
            Text(node.name, style = MaterialTheme.typography.titleLarge, color = if(learned) GoldBright else Gold)
        }
        Text(tr("${passiveKind(node.kind)} · ${node.cost} очк.", "${passiveKind(node.kind)} · ${node.cost} pts"), color = Muted, style = MaterialTheme.typography.labelMedium)
        OrnateDivider(if(learned) Gold else Muted)
        Text(node.description)
        node.effects.forEach { Text(passiveEffectText(it), color = GoldBright, style = MaterialTheme.typography.bodyMedium) }
        val available = node.id in state.allocatable
        val refundable = node.id in state.refundable
        if(learned) OutlinedButton(enabled = !busy && refundable && state.lockedReason == null, onClick = { onAction(PassiveAction.REFUND, node.id) }) { Text(tr("Снять узел", "Refund the node")) }
        else Button(enabled = !busy && available && state.lockedReason == null, onClick = { onAction(PassiveAction.ALLOCATE, node.id) }) { Text(tr("Изучить узел", "Allocate the node")) }
        Text(when {
            state.lockedReason != null -> state.lockedReason.orEmpty()
            learned && !refundable -> tr("Нельзя разорвать связь изученных узлов со стартом.", "Allocated nodes must stay connected to the start.")
            learned -> tr("Очко вернётся. Требования надетых предметов проверит сервер.", "The point is refunded. Equipped item requirements are checked by the server.")
            state.availablePoints < node.cost -> tr("Не хватает очков. Новое очко даётся за каждый уровень после первого.", "Not enough points. One point is granted per level after the first.")
            !available -> tr("Сначала проложите путь от начального узла.", "Connect a path from the starting node first.")
            else -> tr("Узел доступен для изучения.", "The node can be allocated.")
        }, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}
