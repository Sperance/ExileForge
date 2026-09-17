package com.sperance.exileforge.ui.screens.combat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.combat.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ForgeIcon
import com.sperance.exileforge.ui.icons.LocalForgeIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun BattleActions(battle: Battle, enabled: Boolean, onAction: (BattleAction) -> Unit, onFlee: () -> Unit) {
    val controls = enabled && battle.status == BattleStatus.ACTIVE
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(enabled = controls, onClick = { onAction(BattleAction.ATTACK) }) { ActionRune(BattleAction.ATTACK, ForgeGlyphs.Swords); Spacer(Modifier.width(6.dp)); Text(tr("Удар", "Strike")) }
        Button(enabled = controls && battle.hero.mana >= 8, onClick = { onAction(BattleAction.POWER) }) { ActionRune(BattleAction.POWER, ForgeGlyphs.Sigil); Spacer(Modifier.width(6.dp)); Text(tr("Мощный · 8 MP", "Heavy · 8 MP")) }
        OutlinedButton(enabled = controls, onClick = { onAction(BattleAction.GUARD) }) { ActionRune(BattleAction.GUARD, ForgeGlyphs.Kite); Spacer(Modifier.width(6.dp)); Text(tr("Защита · +6 MP", "Guard · +6 MP")) }
        OutlinedButton(enabled = controls && battle.potions > 0, onClick = { onAction(BattleAction.POTION) }) { ActionRune(BattleAction.POTION, ForgeGlyphs.Flask); Spacer(Modifier.width(6.dp)); Text(tr("Флакон · +40% HP", "Flask · +40% HP")) }
        TextButton(enabled = controls, onClick = { onFlee() }) { Text(tr("Отступить", "Retreat")) }
    }
}

/** The set draws every battle action; the bundled glyph stands in when it is not loaded. */
@Composable private fun ActionRune(action: BattleAction, bundled: androidx.compose.ui.graphics.vector.ImageVector) {
    ForgeIcon(LocalForgeIcons.current.forBattleAction(action.name), Modifier.size(18.dp), framed = false) { Icon(bundled, null, Modifier.size(16.dp)) }
}
