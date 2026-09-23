package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.features.key
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.scene.ExpeditionScene
import com.sperance.exileforge.ui.screens.expedition.scene.FightLayout
import com.sperance.exileforge.ui.theme.*
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * A run of the campaign, over the whole screen: the scene underneath, the overlay above.
 *
 * The scene draws and steps the world; everything with words or numbers in it — the bars, the
 * monster's name and modifiers, the hits, the loot — is Compose, in the app's dictionary and
 * theme, laid over it. The stick is the overlay's too: a thumb anywhere in the lower part of the
 * screen sets it, and letting go stops the hero.
 */
@Composable fun ExpeditionPlay(s: ForgeState, vm: ForgeViewModel, run: ExpeditionRun) {
    val hud by run.hud.collectAsState()
    BackHandler { vm.runCommand(RunCommand.Leave) }
    LaunchedEffect(hud.phase) { if (hud.phase == RunPhase.LEFT) vm.closeRun() }

    Box(Modifier.fillMaxSize().background(Ink)) {
        ExpeditionScene(run, Modifier.fillMaxSize())
        when (hud.phase) {
            RunPhase.MAP -> {
                Stick(run)
                MapBar(hud, onLeave = { vm.runCommand(RunCommand.Leave) })
            }
            RunPhase.FIGHT -> hud.fight?.let { FightOverlay(hud, it) { vm.runCommand(RunCommand.Speed) } }
            RunPhase.LOOT -> {
                MapBar(hud, onLeave = null)
                LootPanel(s, hud) { vm.runCommand(RunCommand.Continue) }
            }
            RunPhase.DEAD -> Ending(ui("expedition.dead"), ui("expedition.dead_hint"), LifeRed, hud) { vm.runCommand(RunCommand.Continue) }
            RunPhase.CLEARED -> Ending(ui("expedition.map_done"), ui("expedition.map_done_hint"), Vital, hud) { vm.runCommand(RunCommand.Continue) }
            RunPhase.LEFT -> Unit
        }
    }
}

// ==================== Walking ====================

/** Life, shield and what is left on the map, with the way out. */
@Composable private fun MapBar(hud: RunHud, onLeave: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(mapTitle(hud.mapCode), color = GoldBright, style = MaterialTheme.typography.titleMedium)
                Text(ui("expedition.monsters_left", hud.alive, hud.total), color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            onLeave?.let { OutlinedButton(onClick = it) { Text(ui("expedition.leave")) } }
        }
        Vitals(hud.heroLife, hud.heroMaxLife, hud.heroShield, hud.heroMaxShield, Modifier.fillMaxWidth(.6f))
    }
}

/** A life bar with the shield laid over it, and the figure in words. */
@Composable private fun Vitals(life: Int, maxLife: Int, shield: Int, maxShield: Int, modifier: Modifier = Modifier) {
    val shape = CutCornerShape(3.dp)
    val lifeShare by animateFloatAsState(if (maxLife > 0) life / maxLife.toFloat() else 0f, label = "life")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0xCC0A0D12), shape).border(1.dp, LifeRed.copy(alpha = .8f), shape)) {
            Box(Modifier.fillMaxWidth(lifeShare.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(LifeRed, LifeRed.copy(alpha = .55f))), shape))
            if (maxShield > 0) Box(Modifier.fillMaxWidth((shield / maxShield.toFloat()).coerceIn(0f, 1f)).height(4.dp).align(Alignment.TopStart).background(ShieldCyan.copy(alpha = .85f)))
        }
        Text(if (maxShield > 0) ui("expedition.vitals_shield", life, maxLife, shield) else ui("expedition.vitals", life, maxLife),
            color = Parchment, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * The stick: wherever the thumb lands in the lower part of the screen, dragging from there walks.
 * The direction is sent in screen axes; the run turns it into the map's.
 */
@Composable private fun Stick(run: ExpeditionRun) {
    var centre by remember { mutableStateOf<Offset?>(null) }
    var knob by remember { mutableStateOf(Offset.Zero) }
    var area by remember { mutableStateOf(IntSize.Zero) }
    val radius = with(LocalDensity.current) { 56.dp.toPx() }
    // A fight can start under a thumb still on the glass; the hero must not walk off after it.
    DisposableEffect(run) { onDispose { run.stickX = 0.0; run.stickY = 0.0 } }
    Box(Modifier.fillMaxSize().onSizeChanged { area = it }.pointerInput(run) {
        awaitEachGesture {
            val down = awaitFirstDown()
            if (down.position.y < area.height * .35f) return@awaitEachGesture
            centre = down.position
            knob = down.position
            do {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                val delta = change.position - down.position
                val length = hypot(delta.x, delta.y)
                val clamped = if (length > radius) delta * (radius / length) else delta
                knob = down.position + clamped
                run.stickX = (clamped.x / radius).toDouble()
                run.stickY = (clamped.y / radius).toDouble()
                change.consume()
            } while (change.pressed)
            run.stickX = 0.0
            run.stickY = 0.0
            centre = null
        }
    }) {
        centre?.let { c ->
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Gold.copy(alpha = .18f), radius, c)
                drawCircle(Gold.copy(alpha = .5f), radius, c, style = Stroke(2.dp.toPx()))
                drawCircle(GoldBright.copy(alpha = .75f), radius * .4f, knob)
            }
        }
        if (centre == null) Text(ui("expedition.stick_hint"), color = Muted.copy(alpha = .8f), style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp))
    }
}

// ==================== Fighting ====================

private fun rarityTint(rarity: MonsterRarity) = when (rarity) {
    MonsterRarity.NORMAL -> Parchment
    MonsterRarity.MAGIC -> Color(0xFF8888FF)
    MonsterRarity.RARE -> Color(0xFFFFFF77)
}

/** The monster's name and modifiers on top, both bars over the fighters, and the hits flying off them. */
@Composable private fun FightOverlay(hud: RunHud, fight: FightHud, onSpeed: () -> Unit) {
    val monster = fight.monster
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(monsterTitle(monster.code), color = rarityTint(monster.rarity), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(ui(monster.rarity.key()), color = Muted, style = MaterialTheme.typography.labelMedium)
            monster.modifiers.forEach { Text(monsterModifierText(it), color = Rune, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }
        }
        val barWidth = width * .36f
        val barTop = height * FightLayout.GROUND_Y + 12.dp
        Vitals(fight.heroLife, hud.heroMaxLife, fight.heroShield, hud.heroMaxShield,
            Modifier.width(barWidth).offset(x = width * FightLayout.HERO_X - barWidth / 2, y = barTop))
        Vitals(fight.monsterLife, fight.monsterMaxLife, fight.monsterShield, fight.monsterMaxShield,
            Modifier.width(barWidth).offset(x = width * FightLayout.MONSTER_X - barWidth / 2, y = barTop))
        val density = LocalDensity.current
        fight.hits.forEach { hit ->
            val column = if (hit.target == Side.HERO) FightLayout.HERO_X else FightLayout.MONSTER_X
            val rise = (hit.age / ExpeditionRun.HIT_LIFETIME).toFloat()
            val x = with(density) { (width * column).toPx() } + ((hit.id % 3) - 1) * 40f
            val y = with(density) { (height * (FightLayout.GROUND_Y - .3f)).toPx() } - rise * 120f
            Text(hitText(hit), color = hitColour(hit).copy(alpha = (1 - rise).coerceIn(0f, 1f)),
                fontSize = if (hit.kind == HitKind.CRIT) 30.sp else 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.offset { IntOffset((x - 40f).roundToInt(), y.roundToInt()) }.width(80.dp), textAlign = TextAlign.Center)
        }
        fight.outcome?.let {
            Text(ui("expedition.outcome_${it.name.lowercase()}"), color = when (it) { Outcome.WIN -> Vital; Outcome.LOSS -> LifeRed; Outcome.RETREAT -> Muted },
                style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.Center))
        }
        OutlinedButton(onClick = onSpeed, modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp)) {
            Text(ui("expedition.speed", fight.speed))
        }
    }
}

private fun hitText(hit: FloatingHit): String = when (hit.kind) {
    HitKind.EVADED -> ui("expedition.evaded")
    HitKind.BLOCKED -> ui("expedition.blocked")
    HitKind.CRIT -> ui("expedition.crit", hit.amount)
    HitKind.HIT -> hit.amount.toString()
}

private fun hitColour(hit: FloatingHit): Color = when (hit.kind) {
    HitKind.EVADED, HitKind.BLOCKED -> Muted
    HitKind.CRIT -> Color(0xFFFFD34A)
    HitKind.HIT -> if (hit.target == Side.HERO) LifeRed else Parchment
}

// ==================== After ====================

/** What the kill brought: the server's roll, or its absence said plainly, and the way on. */
@Composable private fun BoxScope.LootPanel(s: ForgeState, hud: RunHud, onContinue: () -> Unit) {
    RunPanel(Modifier.align(Alignment.BottomCenter)) {
        Engraved(ui("expedition.victory"))
        hud.slain?.let { Text(monsterTitle(it.code), color = rarityTint(it.rarity), style = MaterialTheme.typography.titleLarge) }
        val reward = hud.reward
        when {
            hud.rewardPending -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Gold, strokeWidth = 2.dp)
                Text(ui("expedition.loot_pending"), color = Muted)
            }
            hud.rewardFailed -> Text(ui("expedition.loot_failed"), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
            reward != null -> Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(ui("expedition.loot_experience", number(reward.experience)), color = Rune)
                if (reward.gold > 0) Text(ui("expedition.loot_gold", reward.gold), color = GoldBright)
                reward.items.forEach { stack ->
                    val orb = s.world.orbs.firstOrNull { it.id == stack.itemId }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(ForgeGlyphs.Orb, null, tint = Gold, modifier = Modifier.size(18.dp))
                        Text(ui("expedition.loot_stack", orb?.title(s.lang) ?: ui("common.item"), stack.amount), color = Parchment)
                    }
                }
                reward.equipment.forEach { instance ->
                    ItemRow(inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId]), s.world.definitions) {}
                }
                if (reward.items.isEmpty() && reward.equipment.isEmpty()) Text(ui("expedition.loot_nothing"), color = Muted)
            }
        }
        Button(enabled = !hud.rewardPending, onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text(ui("expedition.continue")) }
    }
}

/** A run that ended — by death or by the exit — and what it brought all told. */
@Composable private fun Ending(title: String, hint: String, accent: Color, hud: RunHud, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = .72f)), contentAlignment = Alignment.Center) {
        RunPanel(Modifier, accent) {
            Text(title, color = accent, style = MaterialTheme.typography.headlineSmall)
            Text(hint, color = Parchment, style = MaterialTheme.typography.bodyMedium)
            Text(ui("expedition.summary", hud.kills, hud.gold, number(hud.experience)), color = Muted, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(ui("expedition.back_to_camp")) }
        }
    }
}

@Composable private fun RunPanel(modifier: Modifier, accent: Color = Gold, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    Row(modifier.navigationBarsPadding().padding(12.dp).fillMaxWidth().height(IntrinsicSize.Min).background(Panel, shape).border(1.dp, accent.copy(alpha = .5f), shape)) {
        RaritySpine(accent, 4.dp)
        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
