package com.sperance.exileforge.ui.screens.passives

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.model.passives.*
import com.sperance.exileforge.ui.theme.Gold

@Composable internal fun PassiveNodeDetails(node: PassiveNode, state: PassiveState, busy: Boolean, onAction: (PassiveAction, String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(node.name, style = MaterialTheme.typography.titleLarge, color = Gold)
            Text("${passiveKind(node.kind)} · ${node.cost} очк.")
            Text(node.description)
            node.effects.forEach { Text(passiveEffectText(it)) }
            val learned = node.id in state.allocated
            val available = node.id in state.allocatable
            val refundable = node.id in state.refundable
            if(learned) OutlinedButton(enabled = !busy && refundable && state.lockedReason == null, onClick = { onAction(PassiveAction.REFUND, node.id) }) { Text("Снять узел") }
            else Button(enabled = !busy && available && state.lockedReason == null, onClick = { onAction(PassiveAction.ALLOCATE, node.id) }) { Text("Изучить узел") }
            Text(when {
                state.lockedReason != null -> state.lockedReason.orEmpty()
                learned && !refundable -> "Нельзя разорвать связь изученных узлов со стартом."
                learned -> "Очко вернётся. Требования надетых предметов проверит сервер."
                state.availablePoints < node.cost -> "Не хватает очков. Новое очко даётся за каждый уровень после первого."
                !available -> "Сначала проложите путь от начального узла."
                else -> "Узел доступен для изучения."
            }, style = MaterialTheme.typography.bodySmall)
        }
    }
}
