package com.sperance.exileforge.ui.screens.combat.arena

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.combat.Battle
import com.sperance.exileforge.core.model.combat.BattleStatus
import com.sperance.exileforge.core.model.combat.arena.ArenaArt
import com.sperance.exileforge.core.model.combat.arena.ArenaSimulation
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.LocalForgeIcons
import com.sperance.exileforge.ui.screens.combat.BattleActions
import com.sperance.exileforge.ui.theme.*

/** Longest frame the stage will integrate at once, so a stall replays instead of teleporting. */
private const val FRAME_CAP = 64L

/**
 * The playable part of the expedition: the stage, the hero's plate and the action rail.
 *
 * The loop here only *draws*. Each action still goes through [ForgeViewModel] as the same durable,
 * idempotent command it always was, and the stage animates what the server answered — a replayed
 * command produces the same snapshot and [ArenaSimulation.observe] refuses to play it twice.
 */
@Composable fun ArenaBoard(s: ForgeState, battle: Battle, vm: ForgeViewModel, onFlee: () -> Unit) {
    val icons = LocalForgeIcons.current
    // A new character gets a new stage: corpses, loot motes and bars must not survive the switch.
    val arena = remember(s.battleCharacterId) { ArenaSimulation() }
    val clock = remember(arena) { mutableLongStateOf(0L) }
    val art = remember(icons.version) {
        ArenaArt(forMonster = icons::forMonster, forReward = { icons.forCurrency(it.itemId).orEmpty() },
            hero = if(icons.descriptor(HERO_ICON) != null) HERO_ICON else "")
    }
    val zone = s.combatCatalog?.zones?.firstOrNull { it.id == battle.zoneId }
    val kills = s.battleView?.zoneKills?.get(battle.zoneId) ?: 0
    val controls = s.battleCharacterId == s.characterId && s.signedIn && !s.busy && s.battlePending == null
    LaunchedEffect(arena, battle.id, battle.turn, battle.status, s.battleAction) {
        arena.observe(battle, zone?.monsters.orEmpty(), art, s.battleAction)
    }
    LaunchedEffect(arena) {
        var previous = 0L
        while(true) withFrameNanos { now ->
            val elapsed = if(previous == 0L) 0L else (now - previous) / 1_000_000L
            previous = now
            arena.advance(elapsed.coerceIn(0L, FRAME_CAP))
            clock.longValue = arena.clock
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val frame = CutCornerShape(10.dp)
        ArenaStage(arena, { clock.longValue }, battle.zoneId, Modifier.fillMaxWidth().height(250.dp)
            .clip(frame).border(1.dp, (if(battle.monster.boss) Blood else Gold).copy(alpha = .42f), frame))
        ForgePanel(accent = if(battle.monster.boss) Blood else Gold) {
            ArenaHud(battle, zone, kills, battle.potions)
            if(battle.status == BattleStatus.ACTIVE) {
                BattleActions(battle, controls, vm::battleAction, onFlee)
                Text(tr("Все числа на арене — ответ сервера: урон, броня, сопротивления и шанс добычи считает он.",
                    "Every number on the stage is the server's answer: it rolls damage, armour, resistances and loot."),
                    color = Muted, style = MaterialTheme.typography.bodySmall)
            } else Aftermath(s, battle, kills, zone?.killsForBoss ?: 0, controls, vm)
            if(battle.unsupportedStats.isNotEmpty()) Text(
                tr("Не участвуют в расчёте: ", "Not used in the calculation: ") + battle.unsupportedStats.joinToString(),
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** What is on offer once the stage settles: the next mob, the boss, or the stash. */
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun Aftermath(s: ForgeState, battle: Battle, kills: Int, killsForBoss: Int,
    controls: Boolean, vm: ForgeViewModel) {
    val level = s.hero?.level?.toInt() ?: 0
    val allowed = controls && level >= battle.level
    Text(when(battle.status) {
        BattleStatus.VICTORY -> tr("Победа — награды уже в инвентаре", "Victory — the rewards are already in your stash")
        BattleStatus.DEFEAT -> tr("Поражение — возвращение в лагерь", "Defeat — back to camp")
        else -> tr("Отступление без наград", "Retreat without rewards")
    }, color = Gold, style = MaterialTheme.typography.titleMedium)
    battle.rewards.forEach { reward ->
        Text("· ${reward.name} +${reward.amount}", color = GoldBright, style = MaterialTheme.typography.labelMedium)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(enabled = allowed, onClick = { vm.startBattle(battle.zoneId, false) }) {
            Icon(ForgeGlyphs.Swords, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
            Text(tr("Следующий противник", "Next enemy"))
        }
        OutlinedButton(enabled = allowed && killsForBoss > 0 && kills >= killsForBoss,
            onClick = { vm.startBattle(battle.zoneId, true) }) {
            Icon(ForgeGlyphs.Skull, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
            Text(tr("Вызвать босса", "Summon the boss"))
        }
        OutlinedButton(enabled = !s.busy, onClick = { vm.tab(4); vm.loadInventory() }) {
            Text(tr("Открыть арсенал", "Open the stash"))
        }
    }
}

/** The set's own character picture when it ships one; otherwise the bundled exile glyph draws. */
private const val HERO_ICON = "ui-character"
