package com.sperance.exileforge.ui.screens.combat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.combat.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun BattleActions(battle: Battle, enabled: Boolean, onAction: (BattleAction) -> Unit, onFlee: () -> Unit) {
    val controls = enabled && battle.status == BattleStatus.ACTIVE
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(enabled = controls, onClick = { onAction(BattleAction.ATTACK) }) { Icon(ForgeGlyphs.Swords, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(tr("Удар", "Strike")) }
        Button(enabled = controls && battle.hero.mana >= 8, onClick = { onAction(BattleAction.POWER) }) { Text(tr("Мощный · 8 MP", "Heavy · 8 MP")) }
        OutlinedButton(enabled = controls, onClick = { onAction(BattleAction.GUARD) }) { Icon(ForgeGlyphs.Kite, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(tr("Защита · +6 MP", "Guard · +6 MP")) }
        OutlinedButton(enabled = controls && battle.potions > 0, onClick = { onAction(BattleAction.POTION) }) { Icon(ForgeGlyphs.Flask, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(tr("Флакон · +40% HP", "Flask · +40% HP")) }
        TextButton(enabled = controls, onClick = { onFlee() }) { Text(tr("Отступить", "Retreat")) }
    }
}
