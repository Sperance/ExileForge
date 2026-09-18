package com.sperance.exileforge.ui.screens.combat.world

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.combat.Battle
import com.sperance.exileforge.core.model.combat.BattleStatus
import com.sperance.exileforge.core.model.combat.world.GroundLoot
import com.sperance.exileforge.core.model.combat.world.Vec2
import com.sperance.exileforge.core.model.combat.world.WorldArt
import com.sperance.exileforge.core.model.combat.world.WorldIntent
import com.sperance.exileforge.core.model.combat.world.WorldMode
import com.sperance.exileforge.core.model.combat.world.WorldRules
import com.sperance.exileforge.core.model.combat.world.WorldSimulation
import com.sperance.exileforge.core.model.combat.world.groundDirection
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.LocalForgeIcons
import com.sperance.exileforge.ui.theme.*

/** Longest frame the world will integrate at once, so a stall replays instead of teleporting. */
private const val FRAME_CAP = 64L
/** How much of the map fits across the screen. Nine tiles is a phone held upright. */
private const val TILES_ACROSS = 9f
/** The set's own character picture when it ships one; otherwise the bundled exile glyph draws. */
private const val HERO_ICON = "ui-character"

/**
 * The playable expedition: a generated map, a thumb that walks the exile and an exile that fights.
 *
 * The loop here only *draws and walks*. Every blow and every new encounter still goes through
 * [ForgeViewModel] as the same durable, idempotent command a tap always sent — the world decides when
 * to ask, never what the answer is — and a replayed command produces the same snapshot, which
 * [WorldSimulation.observe] refuses to play twice.
 */
@Composable internal fun WorldBoard(s: ForgeState, battle: Battle, vm: ForgeViewModel, onCamp: () -> Unit,
    onFlee: () -> Unit) {
    val icons = LocalForgeIcons.current
    // A new character gets a new world: corpses, drops and bars must not survive the switch.
    val world = remember(s.battleCharacterId) { WorldSimulation() }
    val clock = remember(world) { mutableLongStateOf(0L) }
    val feed = remember(world) { mutableStateOf(emptyList<GroundLoot>()) }
    val onFloor = remember(world) { mutableIntStateOf(0) }
    val roaming = remember(world) { mutableIntStateOf(0) }
    var stick by remember(world) { mutableStateOf<StickState?>(null) }
    var inspect by remember(world) { mutableStateOf<GroundLoot?>(null) }
    // Saved as an ordinal: what survives a rotation has to be something a Bundle certainly holds.
    var driving by rememberSaveable(s.battleCharacterId) { mutableIntStateOf(WorldMode.AUTO_STRIKE.ordinal) }
    val mode = WorldMode.entries[driving.coerceIn(0, WorldMode.entries.lastIndex)]
    val art = remember(icons.version, s.battleLoot) {
        WorldArt(forMonster = icons::forMonster,
            // An instance carries the base the server snapshotted with it, which is what names its picture.
            forReward = { reward ->
                s.battleLoot[reward.equipmentUuid]?.let { icons.forDocument(inventoryDocument(it, null)) }
                    ?: icons.forCurrency(reward.itemId).orEmpty()
            },
            rarityOf = { reward -> s.battleLoot[reward.equipmentUuid]?.poe?.rarity.orEmpty() },
            hero = if(icons.descriptor(HERO_ICON) != null) HERO_ICON else "")
    }
    val zone = s.combatCatalog?.zones?.firstOrNull { it.id == battle.zoneId }
    val kills = s.battleView?.zoneKills?.get(battle.zoneId) ?: 0
    val killsForBoss = zone?.killsForBoss ?: 0
    val controls = s.battleCharacterId == s.characterId && s.signedIn && !s.busy && s.battlePending == null
    // What the camp would accept, mirrored from the server's own counters rather than invented here.
    val allowed = controls && (s.hero?.level?.toInt() ?: 0) >= battle.level
    val rules = WorldRules(engage = allowed, boss = allowed && killsForBoss > 0 && kills >= killsForBoss)
    val bossReady = killsForBoss > 0 && kills >= killsForBoss && battle.status != BattleStatus.ACTIVE
    // The simulation is not Compose state, so the switches are pushed to it once composition settles.
    SideEffect { world.mode = mode; world.rules = rules }
    // A defeat hands the reins back: an auto-pilot that walks straight into the next fight is not one.
    LaunchedEffect(battle.id, battle.status) {
        if(battle.status == BattleStatus.DEFEAT) driving = WorldMode.MANUAL.ordinal
    }
    LaunchedEffect(world, battle.id, battle.turn, battle.status, s.battleAction) {
        world.observe(battle, zone?.monsters.orEmpty(), art, s.battleAction)
    }
    // The world runs off the display's clock, and the pump reads the freshest permission each frame.
    val live = rememberUpdatedState(controls)
    val here = rememberUpdatedState(battle.zoneId)
    LaunchedEffect(world) {
        var previous = 0L
        while(true) withFrameNanos { now ->
            val elapsed = if(previous == 0L) 0L else (now - previous) / 1_000_000L
            previous = now
            world.advance(elapsed.coerceIn(0L, FRAME_CAP))
            clock.longValue = world.clock
            if(onFloor.intValue != world.loot.size) onFloor.intValue = world.loot.size
            if(roaming.intValue != world.roaming) roaming.intValue = world.roaming
            if(feed.value.size != world.collected.size || feed.value.lastOrNull() !== world.collected.lastOrNull())
                feed.value = world.collected.toList()
            if(live.value) when(val intent = world.takeIntent()) {
                is WorldIntent.Strike -> vm.battleAction(intent.action)
                is WorldIntent.Engage -> vm.startBattle(here.value, intent.boss)
                null -> Unit
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val tile = remember(maxWidth, density) {
            val across = with(density) { maxWidth.toPx() } / TILES_ACROSS
            Vec2(across, across / 2f)
        }
        WorldView(world, { clock.longValue }, tile, { stick }, Modifier.fillMaxSize()
            .pointerInput(world, tile) {
                val reachAtFullSpeed = 44.dp.toPx()
                detectDragGestures(
                    onDragStart = { start -> stick = StickState(start, start) },
                    onDragEnd = { stick = null; world.steer(Vec2.ZERO) },
                    onDragCancel = { stick = null; world.steer(Vec2.ZERO) },
                    onDrag = { change, _ ->
                        change.consume()
                        val origin = stick?.origin ?: change.position
                        stick = StickState(origin, change.position)
                        val delta = change.position - origin
                        val pace = (delta.getDistance() / reachAtFullSpeed).coerceIn(0f, 1f)
                        world.steer(groundDirection(Vec2(delta.x, delta.y), tile) * pace)
                    })
            })
        WorldTopPlate(battle, zone, kills, roaming.intValue, bossReady, Modifier.align(Alignment.TopCenter))
        LootFeed(feed.value, { inspect = it }, Modifier.align(Alignment.TopEnd).padding(top = 70.dp, end = 10.dp))
        Column(Modifier.align(Alignment.BottomCenter), verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            if(s.battlePending != null) WorldNotice(tr("Ответ не подтверждён — повторите тот же запрос",
                "The answer was lost — resend the identical request"), Gold)
            // Walking over a pile is the picture of a reward the server granted when it answered, so
            // leaving one behind costs nothing and never blocks the next fight.
            else if(onFloor.intValue > 0) WorldNotice(tr("Добыча на земле: ${onFloor.intValue} · уже в инвентаре",
                "Loot on the floor: ${onFloor.intValue} · already in your stash"), GoldBright)
            // The rail below is for a settled battle nobody is driving: while the exile runs the
            // expedition itself, or while a command is in flight, there is nothing to press.
            if(battle.status != BattleStatus.ACTIVE && mode != WorldMode.AUTO_RUN && controls)
                Aftermath(s, battle, kills, killsForBoss, vm, onCamp)
            WorldBottomPlate(battle, controls, mode, onMode = { driving = it.ordinal },
                onAction = vm::battleAction, onFlee = onFlee)
        }
    }
    inspect?.let { pile -> LootSheet(s, pile) { inspect = null } }
}

/** What is on offer once the dust settles: the next mob, the boss, the stash, or the camp. */
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun Aftermath(s: ForgeState, battle: Battle, kills: Int, killsForBoss: Int,
    vm: ForgeViewModel, onCamp: () -> Unit) {
    val allowed = (s.hero?.level?.toInt() ?: 0) >= battle.level
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(when(battle.status) {
            BattleStatus.VICTORY -> tr("Победа — награды уже в инвентаре", "Victory — the rewards are already in your stash")
            BattleStatus.DEFEAT -> tr("Поражение — возвращение в лагерь", "Defeat — back to camp")
            else -> tr("Отступление без наград", "Retreat without rewards")
        }, color = Gold, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Button(enabled = allowed, onClick = { vm.startBattle(battle.zoneId, false) }) {
                Icon(ForgeGlyphs.Swords, null, Modifier.size(15.dp)); Spacer(Modifier.width(5.dp))
                Text(tr("Следующий", "Next"))
            }
            OutlinedButton(enabled = allowed && killsForBoss > 0 && kills >= killsForBoss,
                onClick = { vm.startBattle(battle.zoneId, true) }) {
                Icon(ForgeGlyphs.Skull, null, Modifier.size(15.dp)); Spacer(Modifier.width(5.dp))
                Text(tr("Босс", "Boss"))
            }
            OutlinedButton(enabled = !s.busy, onClick = { vm.tab(4); vm.loadInventory() }) {
                Text(tr("Арсенал", "Stash"))
            }
            TextButton(onClick = onCamp) { Text(tr("В лагерь", "To camp")) }
        }
    }
}

/** The item as the stash will show it: the rarity, the base and the modifiers the server rolled. */
@Composable private fun LootSheet(s: ForgeState, pile: GroundLoot, onClose: () -> Unit) {
    val instance = s.battleLoot[pile.equipmentUuid]
    AlertDialog(onDismissRequest = onClose, containerColor = Panel, titleContentColor = Gold,
        title = { Text(tr("Добыча", "Loot")) },
        text = {
            if(instance == null) Text(tr("${pile.title} — откройте арсенал, чтобы увидеть модификаторы",
                "${pile.title} — open the stash to see its modifiers"))
            else ItemCard(inventoryDocument(instance, s.inventoryBases[instance.equipmentId]), enabled = false,
                detailed = true, definitions = s.inventoryDefinitions)
        },
        confirmButton = { TextButton(onClick = onClose) { Text(tr("Закрыть", "Close")) } })
}
