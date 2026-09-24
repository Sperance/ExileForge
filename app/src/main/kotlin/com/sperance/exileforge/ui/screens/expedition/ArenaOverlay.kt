package com.sperance.exileforge.ui.screens.expedition

import com.sperance.exileforge.presentation.state.sellPrice
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.unit.min
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
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.theme.*
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

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
    DamageType.MAGICAL -> Rune
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
 * The fight — «Арена», as heraldry since 2.28.0 (the owner's pick of five): two framed portraits
 * side by side, the hero's in gold and the monster's in its rarity's colour, cut-cornered as an
 * item's frame is. The frame carries everything about its fighter: the name and what it is on top,
 * the bust in the middle, and under it life with the shield over it, the hero's mana, a bar that
 * fills toward the next swing at its own attack speed, one toward the next spell, and the ailments
 * on it as chips that drain as they wear off. The portrait is three by four on every phone (since
 * 2.29.0) and the frame is as tall as it and its lines need. What the monster rolled sits under its
 * frame, apart from it, in the rune blue the modifiers are written in everywhere.
 *
 * A blow is the frame itself: it draws back and strikes the other frame, a flash bursts where they
 * meet, the number rises off the one that was hit and its frame shudders. A spell is a bolt from
 * frame to frame; a flask is a green glow on the hero's; a stunned or frozen fighter's frame dims.
 * Under the frames, the log, newest line first, which unfolds over the scene on demand, and the
 * player's two hands: the flask and the way out.
 */
@Composable internal fun ArenaOverlay(s: ForgeState, hud: RunHud, fight: FightHud, level: Int, onCommand: (RunCommand) -> Unit) {
    var logOpen by rememberSaveable { mutableStateOf(false) }
    val monster = fight.monster
    val hero = s.play.hero?.character
    val lunge = fight.lunge
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val height = maxHeight
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp)) {
            // The frames take whatever the log and the buttons leave, so those never leave the screen;
            // everything that moves is clipped to the frames' own row.
            Column(Modifier.weight(1f).fillMaxWidth()) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds(), contentAlignment = Alignment.TopCenter) {
                    val gap = 12.dp
                    val cardWidth = (maxWidth - gap) / 2
                    // A frame is its portrait (three by four) and its lines, and no taller than the room it has.
                    val cardHeight = min(maxHeight, cardWidth * 4f / 3f + FRAME_LINES)
                    val reach = cardWidth * .3f
                    val heroShift = shift(lunge, Side.HERO, reach)
                    val monsterShift = shift(lunge, Side.MONSTER, reach)
                    Box(Modifier.fillMaxWidth().height(cardHeight)) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            FighterFrame(Modifier.weight(1f).fillMaxHeight().offset { IntOffset(heroShift.roundToPx(), 0) },
                                side = Side.HERO, accent = GoldBright, name = hero?.name.orEmpty(),
                                line = ui("expedition.hero_line", s.heroClass?.title.orEmpty(), hero?.level ?: 1),
                                life = fight.heroLife, maxLife = hud.heroMaxLife, shield = fight.heroShield, maxShield = hud.heroMaxShield,
                                mana = fight.heroMana, maxMana = fight.heroMaxMana, swing = fight.heroSwing, cast = fight.heroCast.takeIf { fight.heroCasts },
                                ailments = fight.heroAilments, held = fight.heroHeld, flash = flash(lunge, Side.HERO), glow = if (fight.flaskActive) Vital else null,
                                hits = fight.hits.filter { it.target == Side.HERO },
                                portrait = { time, wash, amount, flash -> Portraits.hero(this, s.heroClass?.code, time, wash, amount, flash) })
                            FighterFrame(Modifier.weight(1f).fillMaxHeight().offset { IntOffset(monsterShift.roundToPx(), 0) },
                                side = Side.MONSTER, accent = rarityTint(monster.rarity), name = monsterTitle(monster.code),
                                line = ui("expedition.monster_line", ui(monster.rarity.key()), level),
                                life = fight.monsterLife, maxLife = fight.monsterMaxLife, shield = fight.monsterShield, maxShield = fight.monsterMaxShield,
                                mana = 0, maxMana = 0, swing = fight.monsterSwing, cast = fight.monsterCast.takeIf { fight.monsterCasts },
                                ailments = fight.monsterAilments, held = fight.monsterHeld, flash = flash(lunge, Side.MONSTER), glow = null,
                                hits = fight.hits.filter { it.target == Side.MONSTER },
                                portrait = { time, wash, amount, flash -> Portraits.monster(this, monster.code, monster.form, rarityTint(monster.rarity), time, wash, amount, flash) })
                        }
                        Strikes(lunge, cardWidth, cardHeight, gap)
                        fight.outcome?.let {
                            Text(ui("expedition.outcome_${it.name.lowercase()}"), color = outcomeColour(it), style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.align(Alignment.Center).background(Ink.copy(alpha = .7f), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 6.dp))
                        }
                    }
                }
                // What the monster rolled: outside its frame, under it, apart from the fighter's own numbers.
                // Since 2.45.0 one list: its own modifiers and its map's buffs, summed per characteristic,
                // a line the map is in marked so — the sum the fight already folds.
                val lines = monsterLines(monster)
                if (lines.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Spacer(Modifier.weight(1f))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        lines.forEach { line ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Rhombus(if (line.fromMap) LifeRed else Rune, 4.dp)
                                Text(monsterLineText(line), color = Rune, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                                if (line.fromMap) Text(ui("fight.line_map"), color = LifeRed, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 8.dp).background(Panel.copy(alpha = .92f), RoundedCornerShape(10.dp))
                .border(1.dp, Bronze.copy(alpha = .5f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp)
                .height(if (logOpen) height * .35f else 96.dp)) {
                FightLog(fight.events, monster.code)
            }
            val live = fight.outcome == null
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // The flask: what is left of it is the label, and it cannot be drunk twice at once.
                Button(enabled = live && fight.flasks > 0 && !fight.flaskActive, onClick = { onCommand(RunCommand.Flask) }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = GoldBright, disabledContainerColor = Panel, disabledContentColor = Muted)) {
                    Icon(ForgeGlyphs.Flask, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(ui(if (fight.flaskActive) "expedition.flask_drinking" else "expedition.flask", fight.flasks, hud.maxFlasks))
                }
                OutlinedButton(enabled = live && !fight.retreating, onClick = { onCommand(RunCommand.Retreat) }, modifier = Modifier.weight(1f)) {
                    Text(ui(if (fight.retreating) "expedition.retreating" else "expedition.retreat"))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onCommand(RunCommand.Speed) }, modifier = Modifier.weight(1f)) { Text(ui("expedition.speed", fight.speed)) }
                OutlinedButton(onClick = { logOpen = !logOpen }, modifier = Modifier.weight(1f)) {
                    Text(ui(if (logOpen) "expedition.log_less" else "expedition.log"))
                }
            }
        }
    }
}

/** What a frame holds besides its portrait: the name and the kind above, the bars and the ailments below. */
private val FRAME_LINES = 112.dp

/** How far [side]'s frame is out of its place, toward the other: lunging on its own blow, shuddering when the other's lands, leaning away from a miss. */
private fun shift(lunge: LungeView?, side: Side, reach: Dp): Dp {
    lunge ?: return 0.dp
    val t = lunge.progress
    val toward = if (side == Side.HERO) 1f else -1f
    val out = when {
        // A short draw back, then the strike.
        lunge.actor == side && lunge.action == Action.ATTACK -> if (t < .12f) -sin(t / .12f * PI).toFloat() * .35f else sin((t - .12f) / .88f * PI).toFloat()
        lunge.actor == side && lunge.action == Action.RETREAT -> -.5f * sin(t * PI).toFloat()
        lunge.actor != side && lunge.landed && lunge.action != Action.TICK && lunge.action != Action.FLASK && t > .5f -> -.2f * (1 - t) * sin(t * 60).toFloat()
        lunge.actor != side && lunge.kind == HitKind.EVADED -> -.35f * sin(t * PI).toFloat()
        else -> 0f
    }
    return reach * out * toward
}

/** How white [side]'s portrait flashes: a blow that just landed on it. */
private fun flash(lunge: LungeView?, side: Side): Float {
    lunge ?: return 0f
    if (lunge.actor == side || !lunge.landed || lunge.action == Action.TICK || lunge.action == Action.FLASK || lunge.progress <= .5f) return 0f
    return (1 - lunge.progress) * 2f * (if (lunge.kind == HitKind.CRIT) 1f else .6f)
}

/** The burst where the frames meet, the bolt of a spell, the arc of a block — drawn over the frames. */
@Composable private fun Strikes(lunge: LungeView?, cardWidth: Dp, cardHeight: Dp, gap: Dp) {
    lunge ?: return
    if (lunge.action == Action.TICK || lunge.action == Action.RETREAT) return
    Canvas(Modifier.fillMaxSize()) {
        val t = lunge.progress
        val byHero = lunge.actor == Side.HERO
        val cw = cardWidth.toPx()
        val g = gap.toPx()
        val seam = cw + g / 2
        val y = cardHeight.toPx() * .45f
        val sourceX = if (byHero) cw / 2 else cw * 1.5f + g
        val targetX = if (byHero) cw * 1.5f + g else cw / 2
        fun block() = drawArc(Color(0xFFC8C8C8).copy(alpha = .8f * (1 - t)), if (byHero) 110f else -70f, 140f, false,
            Offset(targetX - cw * .3f, y - cw * .3f), Size(cw * .6f, cw * .6f), style = Stroke(6f))
        when (lunge.action) {
            Action.ATTACK -> {
                if (lunge.landed && t > .5f) {
                    val burst = (t - .5f) * 2
                    val tint = if (lunge.kind == HitKind.CRIT) Color(0xFFFFD34A) else GoldBright
                    val radius = cw * (.25f + .45f * burst)
                    drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = .9f * (1 - burst)), tint.copy(alpha = .3f * (1 - burst)), Color.Transparent), Offset(seam, y), radius), radius, Offset(seam, y))
                }
                if (lunge.kind == HitKind.BLOCKED && t > .4f) block()
            }
            Action.SPELL -> {
                val flight = (t * 2).coerceAtMost(1f)
                val bx = sourceX + (targetX - sourceX) * flight
                val by = y - sin(flight * PI).toFloat() * cw * .3f
                if (t < .5f) {
                    for (i in 3 downTo 1) drawCircle(Rune.copy(alpha = .15f * i), cw * .05f * i, Offset(bx, by))
                    drawCircle(Color.White.copy(alpha = .85f), cw * .035f, Offset(bx, by))
                } else if (lunge.landed) {
                    val burst = (t - .5f) * 2
                    drawCircle(Rune.copy(alpha = .55f * (1 - burst)), cw * (.12f + .4f * burst), Offset(targetX, y))
                } else if (lunge.kind == HitKind.BLOCKED) block()
            }
            Action.FLASK -> {
                val radius = cw * (.4f + .4f * t)
                drawCircle(Brush.radialGradient(listOf(Vital.copy(alpha = .5f * (1 - t)), Color.Transparent), Offset(cw / 2, y), radius), radius, Offset(cw / 2, y))
            }
            Action.TICK, Action.RETREAT -> Unit
        }
    }
}

/**
 * One fighter's frame: the name and what it is, the bust, the pools, the swing and cast bars, and
 * what is on it. The frame is gold for the hero and the rarity's colour for a monster, with a bronze
 * line inside as an item's frame has; a flask glows it green, a held fighter dims it.
 */
@Composable private fun FighterFrame(modifier: Modifier, side: Side, accent: Color, name: String, line: String, life: Int, maxLife: Int, shield: Int, maxShield: Int,
    mana: Int, maxMana: Int, swing: Float, cast: Float?, ailments: List<AilmentView>, held: Boolean, flash: Float, glow: Color?, hits: List<FloatingHit>,
    portrait: DrawScope.(Float, Color?, Float, Float) -> Unit) {
    val shape = CutCornerShape(12.dp)
    val inner = CutCornerShape(9.dp)
    val bar = CutCornerShape(3.dp)
    val share by animateFloatAsState(if (maxLife > 0) life / maxLife.toFloat() else 0f, label = "life")
    val time by rememberClock()
    val wash = ailments.map { it.ailment }.maxByOrNull { washAmount(it) }
    Column(modifier
        .background(Brush.verticalGradient(listOf(PanelRaised, Abyss)), shape)
        .border(2.dp, glow ?: accent, shape).padding(3.dp).border(1.dp, Bronze.copy(alpha = .85f), inner).clip(inner)) {
        Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Abyss, Color.Transparent))).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(name, color = accent, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(line, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // The portrait is always three by four, whatever the phone: the frame grows round it.
        // The portrait is three by four, as large as the frame's room allows; the numbers rise inside it.
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).border(1.dp, Bronze.copy(alpha = .5f)).background(Color.Black).clipToBounds(),
            contentAlignment = Alignment.Center) {
            Box(Modifier.aspectRatio(3f / 4f)) {
                Canvas(Modifier.fillMaxSize()) {
                    portrait(this, time, wash?.let(::ailmentTint), wash?.let(::washAmount) ?: 0f, flash)
                    if (held) drawRect(Ink.copy(alpha = .45f))
                }
                if (side == Side.MONSTER && accent != Parchment) Canvas(Modifier.fillMaxSize()) { Portraits.ring(this, accent, time) }
            }
            val density = LocalDensity.current
            val boxWidth = with(density) { maxWidth.toPx() }
            val boxHeight = with(density) { maxHeight.toPx() }
            hits.forEach { hit ->
                val rise = (hit.age / ExpeditionRun.HIT_LIFETIME).toFloat()
                val x = boxWidth / 2 + ((hit.id % 3) - 1) * 26f
                val y = boxHeight * .45f - rise * boxHeight * .35f
                val alpha = (1 - rise).coerceIn(0f, 1f)
                val half = with(density) { 70.dp.toPx() }
                Column(Modifier.align(Alignment.TopStart).offset { IntOffset((x - half).roundToInt(), y.roundToInt()) }.width(140.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
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
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.fillMaxWidth().height(14.dp).background(Color(0xCC0A0D12), bar).border(1.dp, LifeRed.copy(alpha = .8f), bar)) {
                Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(LifeRed, LifeRed.copy(alpha = .55f))), bar))
                if (maxShield > 0) Box(Modifier.fillMaxWidth((shield / maxShield.toFloat()).coerceIn(0f, 1f)).height(3.dp).background(ShieldCyan.copy(alpha = .85f)))
                Text(if (maxShield > 0) "$life / $maxLife · $shield" else "$life / $maxLife", color = Parchment, fontSize = 9.sp, modifier = Modifier.align(Alignment.Center))
            }
            if (maxMana > 0) Box(Modifier.fillMaxWidth().height(5.dp).background(Color(0xCC0A0D12), bar).border(1.dp, ManaBlue.copy(alpha = .7f), bar)) {
                Box(Modifier.fillMaxWidth((mana / maxMana.toFloat()).coerceIn(0f, 1f)).fillMaxHeight().background(ManaBlue, bar))
            }
            // The swing bar: full the instant the next blow lands; dimmed while nothing can land.
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0x14FFFFFF), RoundedCornerShape(2.dp))) {
                Box(Modifier.fillMaxWidth(swing.coerceIn(0f, 1f)).fillMaxHeight().background(if (held) Muted else Gold, RoundedCornerShape(2.dp)))
            }
            cast?.let {
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0x14FFFFFF), RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth(it.coerceIn(0f, 1f)).fillMaxHeight().background(if (held) Muted else Rune, RoundedCornerShape(2.dp)))
                }
            }
            // The ailments' row keeps its place empty, so a frame never changes height mid-fight.
            Row(Modifier.padding(top = 2.dp).height(16.dp).clipToBounds(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (held && ailments.none { it.ailment == Ailment.FROZEN }) AilmentChip(ui("expedition.stunned"), GoldBright, 1f)
                ailments.take(3).forEach { AilmentChip(if (it.stacks > 1) ui("expedition.ailment_stacks", ui(it.ailment.key()), it.stacks) else ui(it.ailment.key()), ailmentTint(it.ailment), it.left) }
            }
        }
    }
}

/** How strongly an ailment washes a portrait: a freeze is the whole face, a bleed a tint. */
private fun washAmount(ailment: Ailment): Float = when (ailment) {
    Ailment.FROZEN -> .55f
    Ailment.BURNING -> .35f
    Ailment.CHILLED, Ailment.POISONED, Ailment.SHOCKED -> .3f
    Ailment.BLEEDING -> .25f
}

/** A clock in seconds for the portraits' idle motion, ticking once a frame while the frame is on screen. */
@Composable private fun rememberClock(): State<Float> {
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
    val monster = monsterTitle(monsterCode)
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        items(events) { event ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui("expedition.log_time", String.format(Locale.ROOT, "%.1f", event.time)), color = Muted,
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
                Text(logLine(event, monster), color = logColour(event), style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (event.kind == HitKind.CRIT) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (event.action == Action.TICK) FontStyle.Italic else FontStyle.Normal)
            }
        }
    }
}

private fun logLine(event: CombatEvent, monster: String): String {
    val damage = event.damage.roundToInt()
    val hero = event.actor == Side.HERO
    val line = when (event.action) {
        Action.FLASK -> ui("expedition.log_flask", event.healed.roundToInt())
        Action.RETREAT -> ui("expedition.log_retreat")
        Action.TICK -> {
            val ailment = event.ailment?.let { ui(it.key()) }.orEmpty()
            if (hero) ui("expedition.log_tick_they", monster, damage, ailment) else ui("expedition.log_tick_you", damage, ailment)
        }
        Action.SPELL -> when (event.kind) {
            HitKind.CRIT -> if (hero) ui("expedition.log_you_spell_crit", damage) else ui("expedition.log_they_spell_crit", monster, damage)
            HitKind.BLOCKED -> if (hero) ui("expedition.log_they_block", monster) else ui("expedition.log_you_block")
            else -> if (hero) ui("expedition.log_you_spell", damage) else ui("expedition.log_they_spell", monster, damage)
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
    event.action == Action.FLASK -> Vital
    event.action == Action.RETREAT -> Muted
    event.action == Action.TICK -> damageTint(event.type).copy(alpha = .85f)
    event.kind == HitKind.CRIT -> Color(0xFFFFD34A)
    event.action == Action.SPELL && event.landed -> Rune
    event.kind == HitKind.HIT -> if (event.actor == Side.HERO) Parchment else Color(0xFFE9A0A0)
    else -> Muted
}

private fun hitText(hit: FloatingHit): String = when {
    hit.action == Action.FLASK -> ui("expedition.flask_heal", hit.healed)
    hit.kind == HitKind.EVADED -> ui("expedition.evaded")
    hit.kind == HitKind.BLOCKED -> ui("expedition.blocked")
    hit.kind == HitKind.CRIT -> ui("expedition.crit", hit.amount)
    else -> hit.amount.toString()
}

private fun hitColour(hit: FloatingHit): Color = when {
    hit.action == Action.FLASK -> Vital
    hit.kind == HitKind.EVADED || hit.kind == HitKind.BLOCKED -> Muted
    hit.kind == HitKind.CRIT -> Color(0xFFFFD34A)
    else -> damageTint(hit.type, onHero = hit.target == Side.HERO)
}

private fun outcomeColour(outcome: Outcome) = when (outcome) { Outcome.WIN -> Vital; Outcome.LOSS -> LifeRed; Outcome.RETREAT -> Muted }

/**
 * After the fight: how it ended, what it came to, the whole log to scroll back through and — for a
 * victory — what the server rolled. The same screen closes a defeat, with what the death cost
 * instead of the loot.
 */
@Composable internal fun ReportScreen(s: ForgeState, hud: RunHud, report: FightReport, onContinue: () -> Unit) {
    val won = report.outcome == Outcome.WIN
    Column(Modifier.fillMaxSize().background(Ink.copy(alpha = .9f)).statusBarsPadding().navigationBarsPadding().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(ui("expedition.outcome_${report.outcome.name.lowercase()}"), color = outcomeColour(report.outcome), style = MaterialTheme.typography.headlineSmall)
        Text(monsterTitle(report.monster.code), color = rarityTint(report.monster.rarity), style = MaterialTheme.typography.titleMedium)
        val cells = listOf(
            report.dealt.toString() to "expedition.sum_dealt", report.taken.toString() to "expedition.sum_taken",
            ui("expedition.log_time", String.format(Locale.ROOT, "%.1f", report.duration)) to "expedition.sum_time",
            report.crits.toString() to "expedition.sum_crits", report.spells.toString() to "expedition.sum_spells", report.flasks.toString() to "expedition.sum_flasks",
            report.dotDealt.toString() to "expedition.sum_dot_dealt", report.blocked.toString() to "expedition.sum_blocked", report.evaded.toString() to "expedition.sum_evaded")
        cells.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    Column(Modifier.weight(1f).background(Panel, RoundedCornerShape(8.dp)).border(1.dp, Bronze.copy(alpha = .4f), RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(ui(label), color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        // What was inflicted, each way, as coloured words.
        if (report.inflicted.isNotEmpty()) AilmentLine(ui("expedition.sum_inflicted"), report.inflicted)
        if (report.suffered.isNotEmpty()) AilmentLine(ui("expedition.sum_suffered"), report.suffered)
        Box(Modifier.weight(1f).fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).border(1.dp, Bronze.copy(alpha = .4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)) {
            // The whole fight, from the first blow down, as it happened.
            FightLog(report.events, report.monster.code, Modifier.fillMaxSize())
        }
        if (won) Loot(s, hud) else Fall(hud)
        Button(enabled = !hud.rewardPending && !hud.fallPending, onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(ui(if (won) "expedition.continue" else "expedition.back_to_camp"))
        }
    }
}

@Composable private fun AilmentLine(label: String, ailments: List<Ailment>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
        ailments.forEach { Text(ui(it.key()), color = ailmentTint(it), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
    }
}

/** What the kill brought: the server's roll, or its absence said plainly. */
@Composable private fun Loot(s: ForgeState, hud: RunHud) {
    val reward = hud.reward
    when {
        hud.rewardPending -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Gold, strokeWidth = 2.dp)
            Text(ui("expedition.loot_pending"), color = Muted)
        }
        hud.rewardFailed -> Text(ui("expedition.loot_failed"), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
        reward != null -> RewardLines(s, reward)
    }
}

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

/** What the death cost: the server's word, awaited, or its absence said plainly. */
@Composable private fun Fall(hud: RunHud) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(ui("expedition.dead_hint"), color = Parchment, style = MaterialTheme.typography.bodySmall)
        val fall = hud.fall
        when {
            hud.fallPending -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Gold, strokeWidth = 2.dp)
                Text(ui("expedition.fall_pending"), color = Muted)
            }
            fall == null -> Text(ui("expedition.fall_failed"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
            fall.lost > 0 -> Text(ui("expedition.fall_lost", number(fall.lost)), color = LifeRed, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            else -> Text(ui("expedition.fall_free"), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
