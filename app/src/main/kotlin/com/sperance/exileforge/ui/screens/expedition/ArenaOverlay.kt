package com.sperance.exileforge.ui.screens.expedition

import com.sperance.exileforge.presentation.state.sellPrice
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sperance.exileforge.ui.screens.expedition.scene.FightStage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.CampaignReward
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.presentation.features.key
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.GoldBright

internal fun rarityTint(rarity: MonsterRarity) = when (rarity) {
    MonsterRarity.NORMAL -> Parchment
    MonsterRarity.MAGIC -> Color(0xFF8888FF)
    MonsterRarity.RARE -> Color(0xFFFFFF77)
    MonsterRarity.UNIQUE -> Color(0xFFAF6025)
}

/** Each damage type's colour, on a number and in the log alike. */
internal fun damageTint(type: DamageType?, onHero: Boolean = false): Color = when (type) {
    DamageType.FIRE -> Ember
    DamageType.COLD -> ShieldCyan
    DamageType.LIGHTNING -> Color(0xFFFFD34A)
    DamageType.CHAOS -> Elder
    else -> if (onHero) LifeRed else Parchment
}

/** Each ailment's colour: the damage that brings it, or the state it leaves. */
internal fun ailmentTint(ailment: Ailment): Color = when (ailment) {
    Ailment.BURNING -> Ember
    Ailment.CHILLED -> ShieldCyan
    Ailment.FROZEN -> Shaper
    Ailment.SHOCKED -> Color(0xFFFFD34A)
    Ailment.POISONED -> Vital
    Ailment.BLEEDING -> LifeRed
}

internal fun Ailment.key() = "enum.ailment.$name"
internal fun DamageType.key() = "enum.damage.$name"

/**
 * The fight — since 2.57.0 the owner's «HUD как в PoE» over the scene's cave (mockup VI): the
 * fighters stand on the scene's floor as framed portraits and everything with words or numbers in it
 * is here. Across the top, the foe as PoE draws a boss: its name in its rarity's colour, what it is,
 * life with the shield over it, its swing, every state on it, the pack with a bar per foe, and —
 * folded until asked — its modifiers and its map's buffs summed per characteristic. At the foot, the
 * hero: life as a globe ringed by the shield, every state on them as a tile that drains, their swing,
 * the newest line of the log (a tap unfolds the rest), the speed and the way out. Nothing moves until
 * «Начать» — for the first foe of a pack and for every next one.
 */
@Composable internal fun ArenaOverlay(s: ForgeState, hud: RunHud, fight: FightHud, level: Int, onCommand: (RunCommand) -> Unit) {
    var logOpen by rememberSaveable { mutableStateOf(false) }
    var modifiersOpen by rememberSaveable { mutableStateOf(false) }
    val hero = s.play.hero?.character
    val live = fight.outcome == null
    val panel = RoundedCornerShape(10.dp)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val logHeight = maxHeight * .3f
        FloatingHits(fight.hits, maxWidth, maxHeight)
        fight.outcome?.let {
            Text(ui("expedition.outcome_${it.name.lowercase()}"), color = outcomeColour(it), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center).background(Ink.copy(alpha = .7f), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EnemyBar(fight, level)
            MonsterModifiers(fight.monster, modifiersOpen) { modifiersOpen = !modifiersOpen }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = .92f))))
            .navigationBarsPadding().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (logOpen) Box(Modifier.fillMaxWidth().height(logHeight).background(Panel.copy(alpha = .94f), panel)
                .border(1.dp, Bronze.copy(alpha = .5f), panel).padding(horizontal = 10.dp, vertical = 8.dp)) {
                FightLog(fight.events, fight.monster.code)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LifeGlobe(fight.heroLife, hud.heroMaxLife, fight.heroShield, hud.heroMaxShield)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(listOfNotNull(hero?.name, ui("expedition.hero_line", s.heroClass?.title.orEmpty(), hero?.level ?: 1)).joinToString(" · "),
                        color = GoldBright, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    StateTiles(fight.heroAilments, fight.heroHeld)
                    SwingBar(fight.heroSwing, fight.heroHeld, Modifier.fillMaxWidth())
                    LogTicker(fight.events.firstOrNull(), fight.monster.code) { logOpen = !logOpen }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onCommand(RunCommand.Speed) }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text(ui("expedition.speed", fight.speed), style = MaterialTheme.typography.labelMedium)
                    }
                    // Before «Начать» it simply walks away; once begun it costs the foe's free swings.
                    OutlinedButton(enabled = live && !fight.retreating, onClick = { onCommand(RunCommand.Retreat) }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text(ui(if (fight.retreating) "expedition.retreating" else "expedition.retreat"), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (!fight.started) Button(onClick = { onCommand(RunCommand.Begin) }, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = GoldBright)) {
                Icon(ForgeGlyphs.Swords, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(ui("expedition.begin"), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** The foe across the top, as PoE draws a boss: name, what it is, life and shield, swing, what is on it, and its pack. */
@Composable private fun EnemyBar(fight: FightHud, level: Int) {
    val monster = fight.monster
    Text(monsterTitle(monster.code), color = rarityTint(monster.rarity), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(if (fight.packTotal > 1) ui("expedition.monster_line_pack", ui(monster.rarity.key()), level, fight.packIndex, fight.packTotal)
        else ui("expedition.monster_line", ui(monster.rarity.key()), level), color = Muted, style = MaterialTheme.typography.labelSmall)
    LifeBar(fight.monsterLife, fight.monsterMaxLife, fight.monsterShield, fight.monsterMaxShield, Modifier.fillMaxWidth().height(16.dp))
    SwingBar(fight.monsterSwing, fight.monsterHeld, Modifier.fillMaxWidth(.7f))
    // The row keeps its place when empty, so the bar never jumps as states come and go.
    Row(Modifier.height(18.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (fight.monsterHeld && fight.monsterAilments.none { it.ailment == Ailment.FROZEN }) AilmentChip(ui("expedition.stunned"), GoldBright, 1f)
        fight.monsterAilments.forEach { AilmentChip(ailmentLabel(it), ailmentTint(it.ailment), it.left) }
    }
    if (fight.packTotal > 1) PackBars(fight)
}

/**
 * The pack (2.56.1, with bars since 2.57.0): a mark and a bar per foe in fighting order — the fallen
 * empty and dimmed, the one at hand as its life stands, the ones waiting whole — and how many are left.
 */
@Composable private fun PackBars(fight: FightHud) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        fight.pack.forEachIndexed { index, rarity ->
            val fallen = index < fight.packIndex - 1
            val current = index == fight.packIndex - 1
            val share = when { fallen -> 0f; current -> fight.monsterLife / fight.monsterMaxLife.coerceAtLeast(1).toFloat(); else -> 1f }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(if (current) 10.dp else 8.dp).background(rarityTint(rarity).copy(alpha = if (fallen) .3f else 1f), CircleShape)
                    .then(if (current) Modifier.border(1.5.dp, GoldBright, CircleShape) else Modifier))
                Box(Modifier.width(40.dp).height(4.dp).background(Color(0xE60A0D12), RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(LifeRed, RoundedCornerShape(2.dp)))
                }
            }
        }
        Text(ui("expedition.pack_left", fight.packTotal - fight.packIndex + 1, fight.packTotal), color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * What the foe rolled and what its map adds, summed per characteristic (since 2.45.0) — folded into
 * one line under the bar until a tap opens it, so the cave stays in view (2.57.0).
 */
@Composable private fun MonsterModifiers(monster: RolledMonster, open: Boolean, onToggle: () -> Unit) {
    val lines = monsterLines(monster)
    if (lines.isEmpty()) return
    val shape = RoundedCornerShape(8.dp)
    Column(Modifier.widthIn(max = 360.dp).background(Panel.copy(alpha = .92f), shape).border(1.dp, Bronze.copy(alpha = .55f), shape)
        .clip(shape).clickable(onClick = onToggle).animateContentSize().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("fight.modifiers", lines.size), color = Rune, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(if (open) "▴" else "▾", color = Gold, style = MaterialTheme.typography.labelMedium)
        }
        if (open) lines.forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Rhombus(if (line.fromMap) LifeRed else Rune, 4.dp)
                Text(monsterLineText(line), color = Rune, style = MaterialTheme.typography.labelSmall)
                if (line.fromMap) Text(ui("fight.line_map"), color = LifeRed, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** A life bar with the shield laid over its top edge and the figures written across it. */
@Composable private fun LifeBar(life: Int, maxLife: Int, shield: Int, maxShield: Int, modifier: Modifier) {
    val shape = CutCornerShape(3.dp)
    val share by animateFloatAsState(if (maxLife > 0) life / maxLife.toFloat() else 0f, label = "life")
    Box(modifier.background(Color(0xCC0A0D12), shape).border(1.dp, Gold.copy(alpha = .7f), shape)) {
        Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(LifeRed, LifeRed.copy(alpha = .55f))), shape))
        if (maxShield > 0) Box(Modifier.fillMaxWidth((shield / maxShield.toFloat()).coerceIn(0f, 1f)).height(4.dp).background(ShieldCyan.copy(alpha = .85f)))
        Text(if (maxShield > 0) ui("expedition.vitals_shield", life, maxLife, shield) else ui("expedition.vitals", life, maxLife),
            color = GoldBright, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
    }
}

/** The swing: full the instant the next blow lands; dimmed while nothing can land. */
@Composable private fun SwingBar(swing: Float, held: Boolean, modifier: Modifier) {
    Box(modifier.height(3.dp).background(Color(0x14FFFFFF), RoundedCornerShape(2.dp))) {
        Box(Modifier.fillMaxWidth(swing.coerceIn(0f, 1f)).fillMaxHeight().background(if (held) Muted else Gold, RoundedCornerShape(2.dp)))
    }
}

/**
 * The hero's life as PoE keeps it: a globe of blood that sinks as it is lost, with a ripple on top,
 * and the energy shield as a cyan ring round it.
 */
@Composable private fun LifeGlobe(life: Int, maxLife: Int, shield: Int, maxShield: Int) {
    val share by animateFloatAsState(if (maxLife > 0) life / maxLife.toFloat() else 0f, label = "globe")
    val time by rememberClock()
    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val ring = 4.dp.toPx()
            val r = size.minDimension / 2 - ring * 2
            val c = center
            drawCircle(Color(0xFF0A0C10), r, c)
            clipPath(Path().apply { addOval(Rect(c, r)) }) {
                val top = c.y + r - 2 * r * share.coerceIn(0f, 1f)
                val liquid = Path().apply {
                    moveTo(c.x - r, top)
                    for (i in 0..12) lineTo(c.x - r + 2 * r * i / 12, top + sin(time * 3f + i * .6f) * r * .03f)
                    lineTo(c.x + r, c.y + r); lineTo(c.x - r, c.y + r); close()
                }
                drawPath(liquid, Brush.verticalGradient(listOf(Color(0xFFD65252), Color(0xFF5A1414)), c.y - r, c.y + r))
                drawOval(Color.White.copy(alpha = .12f), Offset(c.x - r * .55f, c.y - r * .62f), Size(r * .62f, r * .32f))
            }
            drawCircle(Bronze, r + 1.dp.toPx(), c, style = Stroke(2.dp.toPx()))
            if (maxShield > 0) {
                val outer = r + ring * 1.6f
                drawCircle(Color.Black.copy(alpha = .6f), outer, c, style = Stroke(ring))
                drawArc(ShieldCyan, -90f, 360f * (shield / maxShield.toFloat()).coerceIn(0f, 1f), false, Offset(c.x - outer, c.y - outer), Size(outer * 2, outer * 2), style = Stroke(ring))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$life", color = GoldBright, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            if (maxShield > 0) Text(ui("fight.globe_shield", shield), color = ShieldCyan, fontSize = 10.sp)
        }
    }
}

/** Every state on the hero as a tile of its colour whose dark fill rises as it wears off; a stun is a gold star. */
@Composable private fun StateTiles(ailments: List<AilmentView>, held: Boolean) {
    val stunned = held && ailments.none { it.ailment == Ailment.FROZEN }
    Row(Modifier.height(30.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!stunned && ailments.isEmpty()) Text(ui("fight.no_states"), color = Muted, style = MaterialTheme.typography.labelSmall)
        if (stunned) StateTile(null, GoldBright, 1f, 1, ui("expedition.stunned"))
        ailments.forEach { StateTile(it.ailment, ailmentTint(it.ailment), it.left, it.stacks, ailmentLabel(it)) }
    }
}

@Composable private fun StateTile(ailment: Ailment?, tint: Color, left: Float, stacks: Int, label: String) {
    val shape = RoundedCornerShape(4.dp)
    Box(Modifier.size(30.dp).clip(shape).background(Color(0xFF0B0E13)).background(tint.copy(alpha = .16f)).border(1.dp, tint, shape)
        .semantics { contentDescription = label }) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) { stateGlyph(ailment, tint) }
        Box(Modifier.fillMaxWidth().fillMaxHeight((1 - left).coerceIn(0f, 1f)).background(Color.Black.copy(alpha = .55f)))
        if (stacks > 1) Text("$stacks", color = Parchment, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp))
    }
}

/** An ailment's mark drawn from shapes: a flame, a snowflake, a crystal, a bolt, a drop, two drops; a star for a stun. */
private fun DrawScope.stateGlyph(ailment: Ailment?, tint: Color) {
    val u = size.minDimension / 16f
    fun path(vararg points: Pair<Float, Float>) = Path().apply {
        points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * u, y * u) else lineTo(x * u, y * u) }
        close()
    }
    when (ailment) {
        Ailment.BURNING -> drawPath(path(8f to 1f, 12f to 7f, 12f to 11f, 10f to 15f, 6f to 15f, 4f to 11f, 5f to 7f, 7f to 9f), tint)
        Ailment.CHILLED -> listOf(0f, 60f, 120f).forEach { angle ->
            val a = Math.toRadians(angle.toDouble())
            val dx = (kotlin.math.cos(a) * 7 * u).toFloat()
            val dy = (kotlin.math.sin(a) * 7 * u).toFloat()
            drawLine(tint, Offset(8 * u - dx, 8 * u - dy), Offset(8 * u + dx, 8 * u + dy), 1.6f * u)
        }
        Ailment.FROZEN -> drawPath(path(8f to 1f, 14f to 5f, 14f to 11f, 8f to 15f, 2f to 11f, 2f to 5f), tint)
        Ailment.SHOCKED -> drawPath(path(10f to 1f, 3f to 9f, 7f to 9f, 6f to 15f, 13f to 7f, 9f to 7f), tint)
        Ailment.POISONED -> { drawPath(path(8f to 2f, 12f to 9f, 11f to 13f, 8f to 14f, 5f to 13f, 4f to 9f), tint); drawCircle(Color(0xFF0B0E13), 1.4f * u, Offset(8 * u, 10 * u)) }
        Ailment.BLEEDING -> { drawPath(path(6f to 2f, 9f to 8f, 8f to 11f, 6f to 12f, 4f to 11f, 3f to 8f), tint); drawPath(path(12f to 7f, 14f to 11f, 13f to 13f, 12f to 14f, 11f to 13f, 10f to 11f), tint) }
        null -> drawPath(path(8f to 1f, 9.8f to 5.2f, 14.3f to 5.6f, 10.9f to 8.6f, 11.9f to 13f, 8f to 10.7f, 4.1f to 13f, 5.1f to 8.6f, 1.7f to 5.6f, 6.2f to 5.2f), tint)
    }
}

/** The newest line of the log on one row; a tap unfolds the whole of it above the hero. */
@Composable private fun LogTicker(latest: CombatEvent?, monsterCode: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(Modifier.fillMaxWidth().clip(shape).background(Panel.copy(alpha = .9f)).border(1.dp, Bronze.copy(alpha = .5f), shape)
        .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 5.dp)) {
        Text(latest?.let { logLine(it, monsterTitle(monsterCode)) } ?: ui("expedition.log"), color = latest?.let(::logColour) ?: Muted,
            style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The numbers rising off the fighters, over the spots the scene stands them on ([FightStage]). */
@Composable private fun FloatingHits(hits: List<FloatingHit>, width: Dp, height: Dp) {
    val density = LocalDensity.current
    val w = with(density) { width.toPx() }
    val h = with(density) { height.toPx() }
    val figure = FightStage.figure(w, h)
    val half = with(density) { 70.dp.toPx() }
    hits.forEach { hit ->
        val rise = (hit.age / ExpeditionRun.HIT_LIFETIME).toFloat()
        val x = w * (if (hit.target == Side.HERO) FightStage.HERO_X else FightStage.MONSTER_X) + ((hit.id % 3) - 1) * 22f
        val y = h * FightStage.FLOOR - figure * (1.25f + .6f * rise)
        val alpha = (1 - rise).coerceIn(0f, 1f)
        Column(Modifier.offset { IntOffset((x - half).roundToInt(), y.roundToInt()) }.width(140.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(hitText(hit), color = hitColour(hit).copy(alpha = alpha), textAlign = TextAlign.Center,
                fontSize = when { hit.kind == HitKind.CRIT -> 30.sp; hit.action == Action.TICK -> 16.sp; else -> 22.sp },
                fontWeight = if (hit.action == Action.TICK) FontWeight.Normal else FontWeight.Bold,
                fontStyle = if (hit.action == Action.TICK) FontStyle.Italic else FontStyle.Normal)
            val marks = (if (hit.stunned) listOf(ui("expedition.stunned")) else emptyList()) + hit.inflicted.map { ui(it.key()) }
            if (marks.isNotEmpty()) Text(marks.joinToString(" · "), color = (hit.inflicted.firstOrNull()?.let(::ailmentTint) ?: GoldBright).copy(alpha = alpha),
                style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

private fun ailmentLabel(view: AilmentView) =
    if (view.stacks > 1) ui("expedition.ailment_stacks", ui(view.ailment.key()), view.stacks) else ui(view.ailment.key())

/** How strongly an ailment washes a portrait: a freeze is the whole face, a bleed a tint. */
internal fun washAmount(ailment: Ailment): Float = when (ailment) {
    Ailment.FROZEN -> .55f
    Ailment.BURNING -> .35f
    Ailment.CHILLED, Ailment.POISONED, Ailment.SHOCKED -> .3f
    Ailment.BLEEDING -> .25f
}

/** A clock in seconds for the portraits' idle motion, ticking once a frame while the frame is on screen. */
@Composable internal fun rememberClock(): State<Float> {
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) withFrameNanos { now -> if (last != 0L) clock.floatValue += ((now - last) / 1e9f).coerceAtMost(.05f); last = now }
    }
    return clock
}

/** A small coloured chip whose fill drains as the ailment wears off. */
@Composable private fun AilmentChip(label: String, tint: Color, left: Float) {
    val shape = RoundedCornerShape(4.dp)
    val fill = tint.copy(alpha = .3f)
    Box(Modifier.clip(shape).border(1.dp, tint.copy(alpha = .8f), shape).drawBehind {
        drawRect(Color(0xAA0A0D12))
        drawRect(fill, size = Size(size.width * left.coerceIn(0f, 1f), size.height))
    }) {
        Text(label, color = tint, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp), maxLines = 1)
    }
}

/** The blows so far, newest first: when, who, and what came of it, coloured by what it was. */
@Composable internal fun FightLog(events: List<CombatEvent>, monsterCode: String, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        items(events) { EventRow(it, monsterTitle(monsterCode)) }
    }
}

/**
 * The log of a whole pack (since 2.54.0): one list, each foe's blows under its own name — a mixed
 * pack's «they hit» lines would otherwise all say the wrong name. A caption between them names which
 * of the pack it was, only when there was more than one.
 */
@Composable internal fun FightLog(pack: List<PackHit>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        pack.forEachIndexed { index, hit ->
            val name = monsterTitle(hit.monster.code)
            if (pack.size > 1) item { Caption(ui("expedition.report_pack_enemy", index + 1, pack.size, name)) }
            items(hit.events) { EventRow(it, name) }
        }
    }
}

@Composable private fun EventRow(event: CombatEvent, monster: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(ui("expedition.log_time", String.format(Locale.ROOT, "%.1f", event.time)), color = Muted,
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
        Text(logLine(event, monster), color = logColour(event), style = MaterialTheme.typography.bodySmall,
            fontWeight = if (event.kind == HitKind.CRIT) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (event.action == Action.TICK) FontStyle.Italic else FontStyle.Normal)
    }
}

private fun logLine(event: CombatEvent, monster: String): String {
    val damage = event.damage.roundToInt()
    val hero = event.actor == Side.HERO
    val line = when (event.action) {
        Action.RETREAT -> ui("expedition.log_retreat")
        Action.TICK -> {
            val ailment = event.ailment?.let { ui(it.key()) }.orEmpty()
            if (hero) ui("expedition.log_tick_they", monster, damage, ailment) else ui("expedition.log_tick_you", damage, ailment)
        }
        Action.ATTACK -> when (event.kind) {
            HitKind.HIT -> if (hero) ui("expedition.log_you_hit", damage) else ui("expedition.log_they_hit", monster, damage)
            HitKind.CRIT -> if (hero) ui("expedition.log_you_crit", damage) else ui("expedition.log_they_crit", monster, damage)
            // An evasion or a block belongs to the one who was struck at.
            HitKind.EVADED -> if (hero) ui("expedition.log_they_evade", monster) else ui("expedition.log_you_evade")
            HitKind.BLOCKED -> if (hero) ui("expedition.log_they_block", monster) else ui("expedition.log_you_block")
        }
    }
    // The blow's leading element and what it left behind, as words after the sentence.
    val marks = buildList {
        event.type?.takeIf { event.action == Action.ATTACK && event.landed && it != DamageType.PHYSICAL }?.let { add(ui(it.key())) }
        if (event.stunned) add(ui("expedition.stunned"))
        event.inflicted.forEach { add(ui(it.key())) }
    }
    return if (marks.isEmpty()) line else "$line · ${marks.joinToString(" · ")}"
}

private fun logColour(event: CombatEvent): Color = when {
    event.action == Action.RETREAT -> Muted
    event.action == Action.TICK -> damageTint(event.type).copy(alpha = .85f)
    event.kind == HitKind.CRIT -> Color(0xFFFFD34A)
    event.kind == HitKind.HIT -> if (event.actor == Side.HERO) Parchment else Color(0xFFE9A0A0)
    else -> Muted
}

private fun hitText(hit: FloatingHit): String = when {
    hit.kind == HitKind.EVADED -> ui("expedition.evaded")
    hit.kind == HitKind.BLOCKED -> ui("expedition.blocked")
    hit.kind == HitKind.CRIT -> ui("expedition.crit", hit.amount)
    else -> hit.amount.toString()
}

private fun hitColour(hit: FloatingHit): Color = when {
    hit.kind == HitKind.EVADED || hit.kind == HitKind.BLOCKED -> Muted
    hit.kind == HitKind.CRIT -> Color(0xFFFFD34A)
    else -> damageTint(hit.type, onHero = hit.target == Side.HERO)
}

internal fun outcomeColour(outcome: Outcome) = when (outcome) { Outcome.WIN -> Vital; Outcome.LOSS -> LifeRed; Outcome.RETREAT -> Muted }

/** What the server rolled — experience, gold, orbs and items — for a kill and a chest alike. */
@Composable internal fun RewardLines(s: ForgeState, reward: CampaignReward) {
    Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (reward.experience > 0) Text(ui("expedition.loot_experience", number(reward.experience)), color = Rune)
            if (reward.gold > 0) Text(ui("expedition.loot_gold", reward.gold), color = GoldBright)
        }
        reward.items.forEach { stack ->
            val orb = s.world.orbs.firstOrNull { it.id == stack.itemId }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(ForgeGlyphs.Orb, null, tint = Gold, modifier = Modifier.size(18.dp))
                Text(ui("expedition.loot_stack", orb?.title(s.lang) ?: ui("common.item"), stack.amount), color = Parchment)
            }
        }
        reward.equipment.forEach { instance ->
            ItemRow(inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId]), s.world.definitions, price = s.sellPrice(instance)) {}
        }
        if (reward.items.isEmpty() && reward.equipment.isEmpty()) Text(ui("expedition.loot_nothing"), color = Muted)
    }
}
