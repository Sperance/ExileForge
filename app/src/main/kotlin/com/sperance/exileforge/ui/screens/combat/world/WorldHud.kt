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
import com.sperance.exileforge.core.model.combat.Zone
import com.sperance.exileforge.core.model.combat.world.GroundLoot
import com.sperance.exileforge.ui.components.StatGlobe
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ForgeIcon
import com.sperance.exileforge.ui.icons.LocalForgeIcons
import com.sperance.exileforge.ui.screens.combat.BattleActions
import com.sperance.exileforge.ui.theme.*

/**
 * The plate over the top of the floor: who the server put in front of the hero and how far the zone is.
 *
 * Every figure is read straight off the server's [Battle] — the world keeps no counters of its own.
 */
@Composable internal fun WorldTopPlate(battle: Battle, zone: Zone?, kills: Int, modifier: Modifier = Modifier) {
    val icons = LocalForgeIcons.current
    val accent = if(battle.monster.boss) Blood else elementTint(battle.monster.element)
    Column(modifier.fillMaxWidth().background(Ink.copy(alpha = .72f)).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeIcon(icons.forMonster(battle.monster), Modifier.size(26.dp), description = battle.monster.name) {
                Icon(if(battle.monster.boss) ForgeGlyphs.Sigil else ForgeGlyphs.Skull, null, tint = accent,
                    modifier = Modifier.size(22.dp))
            }
            Text(battle.monster.name, style = MaterialTheme.typography.titleMedium, color = accent,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("${battle.enemy.life.toInt()}/${battle.enemy.maxLife.toInt()}", color = Parchment,
                style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { (battle.enemy.life / battle.enemy.maxLife.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp), color = LifeRed, trackColor = Abyss)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("Ход ${battle.turn} · ${battle.monster.element}", "Turn ${battle.turn} · ${battle.monster.element}"),
                color = Muted, style = MaterialTheme.typography.labelSmall)
            zone?.let {
                Text(tr("Зачистка ${minOf(kills, it.killsForBoss)}/${it.killsForBoss}",
                    "Cleared ${minOf(kills, it.killsForBoss)}/${it.killsForBoss}"),
                    color = Gold, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * The plate along the bottom: the hero's globes, the flasks left, and who is driving.
 *
 * The auto-pilot is the normal way to fight — the thumb is busy walking. Switching it off reveals the
 * same manual rail the screen has always had, and both send the identical durable command.
 */
@Composable internal fun WorldBottomPlate(battle: Battle, controls: Boolean, auto: Boolean, manual: Boolean,
    onAuto: (Boolean) -> Unit, onAction: (BattleAction) -> Unit, onFlee: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Ink.copy(alpha = .80f)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            StatGlobe(tr("Здоровье", "Life"), "${battle.hero.life.toInt()}/${battle.hero.maxLife.toInt()}",
                (battle.hero.life / battle.hero.maxLife.coerceAtLeast(1.0)).toFloat(), LifeRed)
            if(battle.hero.maxMana > 0.0) StatGlobe(tr("Мана", "Mana"),
                "${battle.hero.mana.toInt()}/${battle.hero.maxMana.toInt()}",
                (battle.hero.mana / battle.hero.maxMana.coerceAtLeast(1.0)).toFloat(), ManaBlue)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FlaskRow(battle.potions)
                AutoSwitch(auto, onAuto)
                Text(if(auto) tr("Свайп — шаг. Удары — сами.", "Swipe to walk. The blows land themselves.")
                else tr("Свайп — шаг. Удары — по кнопкам.", "Swipe to walk. The blows are yours to press."),
                    color = Muted, style = MaterialTheme.typography.labelSmall)
            }
        }
        if(manual) BattleActions(battle, controls, onAction, onFlee)
    }
}

/** Charges the server still grants; a spent one greys out rather than disappearing. */
@Composable private fun FlaskRow(flasks: Int, total: Int = 2) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            Icon(ForgeGlyphs.Flask, null, tint = if(index < flasks) LifeRed else Bronze.copy(alpha = .5f),
                modifier = Modifier.size(16.dp))
        }
        Text(tr("флаконы", "flasks"), color = Muted, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp))
    }
}

/**
 * Who swings: the exile's own hands, or the player's.
 *
 * Always live. It changes nothing on the server, so making it wait for a request in flight would leave
 * the player unable to take over in the middle of a fight, which is exactly when they want to.
 */
@Composable private fun AutoSwitch(auto: Boolean, onAuto: (Boolean) -> Unit) {
    val shape = CutCornerShape(6.dp)
    Row(Modifier.clip(shape).border(1.dp, (if(auto) Gold else Bronze).copy(alpha = .55f), shape)
        .clickable { onAuto(!auto) }.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(ForgeGlyphs.Swords, null, tint = if(auto) GoldBright else Muted, modifier = Modifier.size(15.dp))
        Text(if(auto) tr("Авто-бой", "Auto attack") else tr("Вручную", "Manual"),
            color = if(auto) GoldBright else Muted, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * What the hero has walked over, newest first.
 *
 * The rewards were granted by the server the moment it answered; picking them up off the floor is the
 * picture of that, so the feed never adds to what the stash already holds.
 */
@Composable internal fun LootFeed(collected: List<GroundLoot>, onOpen: (GroundLoot) -> Unit,
    modifier: Modifier = Modifier) {
    if(collected.isEmpty()) return
    Column(modifier.width(168.dp), horizontalAlignment = Alignment.End,
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

/** A caption that floats over the floor without stealing the swipe underneath it. */
@Composable internal fun WorldNotice(text: String, accent: Color = Gold, modifier: Modifier = Modifier) {
    Text(text, color = accent, style = MaterialTheme.typography.labelMedium,
        modifier = modifier.background(Ink.copy(alpha = .78f)).padding(horizontal = 10.dp, vertical = 6.dp))
}
