package com.sperance.exileforge.ui.screens.combat.arena

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.combat.Battle
import com.sperance.exileforge.core.model.combat.Zone
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.StatGlobe
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ForgeIcon
import com.sperance.exileforge.ui.icons.LocalForgeIcons
import com.sperance.exileforge.ui.theme.*

/**
 * The plate under the stage: the hero's globes, the flasks left, the turn, and the zone's push.
 *
 * Every figure is read straight off the server's [Battle] — the arena keeps no counters of its own.
 */
@Composable fun ArenaHud(battle: Battle, zone: Zone?, kills: Int, flasks: Int, modifier: Modifier = Modifier) {
    val icons = LocalForgeIcons.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        StatGlobe(tr("Здоровье", "Life"), "${battle.hero.life.toInt()}/${battle.hero.maxLife.toInt()}",
            (battle.hero.life / battle.hero.maxLife.coerceAtLeast(1.0)).toFloat(), LifeRed)
        if(battle.hero.maxMana > 0.0) StatGlobe(tr("Мана", "Mana"),
            "${battle.hero.mana.toInt()}/${battle.hero.maxMana.toInt()}",
            (battle.hero.mana / battle.hero.maxMana.coerceAtLeast(1.0)).toFloat(), ManaBlue)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ForgeIcon(icons.forMonster(battle.monster), Modifier.size(22.dp), description = battle.monster.name) {
                    Icon(if(battle.monster.boss) ForgeGlyphs.Sigil else ForgeGlyphs.Skull, null,
                        tint = elementTint(battle.monster.element), modifier = Modifier.size(20.dp))
                }
                Text(battle.monster.name, style = MaterialTheme.typography.labelLarge,
                    color = if(battle.monster.boss) Blood else elementTint(battle.monster.element))
            }
            Text(tr("Ход ${battle.turn} · стихия ${battle.monster.element}",
                "Turn ${battle.turn} · element ${battle.monster.element}"),
                color = Muted, style = MaterialTheme.typography.labelMedium)
            FlaskRow(flasks)
            zone?.let {
                Engraved(tr("Зачистка ${minOf(kills, it.killsForBoss)}/${it.killsForBoss}",
                    "Cleared ${minOf(kills, it.killsForBoss)}/${it.killsForBoss}"))
                LinearProgressIndicator(
                    progress = { (kills.toFloat() / it.killsForBoss.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp), color = Gold, trackColor = Abyss)
            }
        }
    }
}

/** Charges the server still grants; a spent one greys out rather than disappearing. */
@Composable private fun FlaskRow(flasks: Int, total: Int = 2) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { index ->
            Icon(ForgeGlyphs.Flask, null, tint = if(index < flasks) LifeRed else Bronze.copy(alpha = .5f),
                modifier = Modifier.size(16.dp))
        }
        Text(tr("флаконы", "flasks"), color = Muted, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp))
    }
}
