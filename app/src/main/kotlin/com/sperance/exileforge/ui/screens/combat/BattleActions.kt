package com.sperance.exileforge.ui.screens.combat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.model.combat.*

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun BattleActions(battle: Battle, enabled: Boolean, onAction: (BattleAction) -> Unit, onFlee: () -> Unit) {
    val controls = enabled && battle.status == BattleStatus.ACTIVE
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(enabled = controls, onClick = { onAction(BattleAction.ATTACK) }) { Text("Удар") }
                    Button(enabled = controls && battle.hero.mana >= 8, onClick = { onAction(BattleAction.POWER) }) { Text("Мощный · 8 MP") }
                    OutlinedButton(enabled = controls, onClick = { onAction(BattleAction.GUARD) }) { Text("Защита · +6 MP") }
                    OutlinedButton(enabled = controls && battle.potions > 0, onClick = { onAction(BattleAction.POTION) }) { Text("Флакон · +40% HP") }
                    TextButton(enabled = controls, onClick = { onFlee() }) { Text("Отступить") }
                }

}
