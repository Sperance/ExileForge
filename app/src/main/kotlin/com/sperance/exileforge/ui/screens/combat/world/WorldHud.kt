package com.sperance.exileforge.ui.screens.combat.world

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.combat.Battle
import com.sperance.exileforge.core.model.combat.BattleAction
import com.sperance.exileforge.core.model.combat.BattleStatus
import com.sperance.exileforge.core.model.combat.Zone
import com.sperance.exileforge.core.model.combat.world.GroundLoot
import com.sperance.exileforge.core.model.combat.world.WorldMode
import com.sperance.exileforge.ui.components.StatGlobe
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ForgeIcon
import com.sperance.exileforge.ui.icons.LocalForgeIcons
import com.sperance.exileforge.ui.screens.combat.BattleActions
import com.sperance.exileforge.ui.theme.*

/**
 * The strip across the top: who the server put in front of the exile, or what the zone still holds.
 *
 * Every figure is read straight off the server's [Battle] and its zone counters — the world keeps no
 * counters of its own. It is one line tall on purpose: the map underneath is the thing to look at.
 */
@Composable internal fun WorldTopPlate(battle: Battle, zone: Zone?, kills: Int, roaming: Int, bossReady: Boolean,
    modifier: Modifier = Modifier) {
    val icons = LocalForgeIcons.current
    val fighting = battle.status == BattleStatus.ACTIVE
    val accent = if(battle.monster.boss) Blood else elementTint(battle.monster.element)
    Column(modifier.fillMaxWidth().background(Ink.copy(alpha = .70f)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if(fighting) {
                ForgeIcon(icons.forMonster(battle.monster), Modifier.size(22.dp), description = battle.monster.name) {
                    Icon(if(battle.monster.boss) ForgeGlyphs.Sigil else ForgeGlyphs.Skull, null, tint = accent,
                        modifier = Modifier.size(19.dp))
                }
                Text(battle.monster.name, style = MaterialTheme.typography.labelLarge, color = accent,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${battle.enemy.life.toInt()}/${battle.enemy.maxLife.toInt()}", color = Parchment,
                    style = MaterialTheme.typography.labelSmall)
            } else {
                Icon(ForgeGlyphs.Skull, null, tint = Muted, modifier = Modifier.size(19.dp))
                Text(tr("Врагов на карте: $roaming", "Monsters roaming: $roaming"),
                    style = MaterialTheme.typography.labelLarge, color = Parchment, modifier = Modifier.weight(1f))
            }
            zone?.let {
                Text(tr("${minOf(kills, it.killsForBoss)}/${it.killsForBoss}",
                    "${minOf(kills, it.killsForBoss)}/${it.killsForBoss}"),
                    color = Gold, style = MaterialTheme.typography.labelSmall)
            }
        }
        if(fighting) LinearProgressIndicator(
            progress = { (battle.enemy.life / battle.enemy.maxLife.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp), color = LifeRed, trackColor = Abyss)
        else if(bossReady) Text(tr("Круг призыва открыт — встаньте в него", "The summoning circle is open — stand in it"),
            color = GoldBright, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * The plate along the bottom: the exile's globes, the flasks left, and who is driving.
 *
 * The rail of buttons only exists in [WorldMode.MANUAL]; in every other mode it is gone and the map
 * has the screen to itself. Both rail and auto-pilot send the identical durable command.
 */
@Composable internal fun WorldBottomPlate(battle: Battle, controls: Boolean, mode: WorldMode,
    onMode: (WorldMode) -> Unit, onAction: (BattleAction) -> Unit, onFlee: () -> Unit,
    modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Ink.copy(alpha = .78f)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StatGlobe(tr("Здоровье", "Life"), "${battle.hero.life.toInt()}/${battle.hero.maxLife.toInt()}",
                (battle.hero.life / battle.hero.maxLife.coerceAtLeast(1.0)).toFloat(), LifeRed, size = 46.dp)
            if(battle.hero.maxMana > 0.0) StatGlobe(tr("Мана", "Mana"),
                "${battle.hero.mana.toInt()}/${battle.hero.maxMana.toInt()}",
                (battle.hero.mana / battle.hero.maxMana.coerceAtLeast(1.0)).toFloat(), ManaBlue, size = 46.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                FlaskRow(battle.potions)
                ModeSwitch(mode, onMode)
            }
        }
        if(mode == WorldMode.MANUAL) BattleActions(battle, controls, onAction, onFlee)
    }
}

/** Charges the server still grants; a spent one greys out rather than disappearing. */
@Composable private fun FlaskRow(flasks: Int, total: Int = 2) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            Icon(ForgeGlyphs.Flask, null, tint = if(index < flasks) LifeRed else Bronze.copy(alpha = .5f),
                modifier = Modifier.size(15.dp))
        }
        Text(tr("флаконы", "flasks"), color = Muted, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp))
    }
}

/**
 * How much of the expedition the exile runs itself, cycled by tapping one chip.
 *
 * Always live. It changes nothing on the server, so making it wait for a request in flight would leave
 * the player unable to take over in the middle of a fight, which is exactly when they want to.
 */
@Composable private fun ModeSwitch(mode: WorldMode, onMode: (WorldMode) -> Unit) {
    val shape = CutCornerShape(6.dp)
    val accent = when(mode) {
        WorldMode.MANUAL -> Bronze
        WorldMode.AUTO_STRIKE -> Gold
        WorldMode.AUTO_RUN -> GoldBright
    }
    Row(Modifier.clip(shape).border(1.dp, accent.copy(alpha = .60f), shape)
        .clickable { onMode(mode.next()) }.padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(if(mode == WorldMode.AUTO_RUN) ForgeGlyphs.Sigil else ForgeGlyphs.Swords, null,
            tint = accent, modifier = Modifier.size(14.dp))
        Text(mode.title(), color = accent, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * What the exile has walked over, newest first.
 *
 * The rewards were granted by the server the moment it answered; picking them up off the floor is the
 * picture of that, so the feed never adds to what the stash already holds.
 */
@Composable internal fun LootFeed(collected: List<GroundLoot>, onOpen: (GroundLoot) -> Unit,
    modifier: Modifier = Modifier) {
    if(collected.isEmpty()) return
    Column(modifier.width(164.dp), horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        collected.takeLast(4).asReversed().forEach { pile ->
            val tint = lootTint(pile)
            Row(Modifier.background(Ink.copy(alpha = .74f))
                .border(1.dp, tint.copy(alpha = .45f), CutCornerShape(4.dp))
                .clickable(enabled = pile.equipmentUuid.isNotBlank()) { onOpen(pile) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ForgeIcon(pile.icon, Modifier.size(16.dp), framed = false, description = pile.title) {
                    Icon(ForgeGlyphs.Gem, null, tint = tint, modifier = Modifier.size(14.dp))
                }
                Text(pile.title, color = tint, style = MaterialTheme.typography.labelSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if(pile.amount > 1L) Text("×${pile.amount}", color = Muted,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** A caption that floats over the map without stealing the swipe underneath it. */
@Composable internal fun WorldNotice(text: String, accent: Color = Gold, modifier: Modifier = Modifier) {
    Text(text, color = accent, style = MaterialTheme.typography.labelSmall,
        modifier = modifier.background(Ink.copy(alpha = .78f)).padding(horizontal = 10.dp, vertical = 5.dp))
}
