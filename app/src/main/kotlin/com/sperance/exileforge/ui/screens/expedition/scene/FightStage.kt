package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.Action
import com.sperance.exileforge.core.campaign.Ailment
import com.sperance.exileforge.core.campaign.AilmentView
import com.sperance.exileforge.core.campaign.FightHud
import com.sperance.exileforge.core.campaign.HitKind
import com.sperance.exileforge.core.campaign.LungeView
import com.sperance.exileforge.core.campaign.Outcome
import com.sperance.exileforge.core.campaign.RolledMonster
import com.sperance.exileforge.core.campaign.Side
import com.sperance.exileforge.ui.screens.expedition.ailmentTint
import com.sperance.exileforge.ui.screens.expedition.rarityTint
import com.sperance.exileforge.ui.screens.expedition.washAmount
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Where the fighters stand (2.57.0): the scene draws them there and the overlay writes their numbers
 * over the same spots, so the two agree without talking. Shares of the screen's width and height.
 */
internal object FightStage {
    const val FLOOR = .66f
    const val HERO_X = .28f
    const val MONSTER_X = .70f
    /** How far a lunge carries a fighter toward the other, as a share of the width. */
    const val REACH = .12f

    /** A fighter's height: a quarter of the screen, never wider than the screen leaves room for. */
    fun figure(width: Float, height: Float) = min(height * .26f, width * .40f)

    /** How far [side] is out of its place, toward the other: lunging on its own blow, shuddering when the other's lands, leaning away from a miss. */
    fun shift(lunge: LungeView?, side: Side): Float {
        lunge ?: return 0f
        val t = lunge.progress
        val toward = if (side == Side.HERO) 1f else -1f
        val out = when {
            // A short draw back, then the strike.
            lunge.actor == side && lunge.action == Action.ATTACK -> if (t < .12f) -sin(t / .12f * PI).toFloat() * .35f else sin((t - .12f) / .88f * PI).toFloat()
            lunge.actor == side && lunge.action == Action.RETREAT -> -.5f * sin(t * PI).toFloat()
            lunge.actor != side && lunge.landed && lunge.action == Action.ATTACK && t > .5f -> -.2f * (1 - t) * sin(t * 60)
            lunge.actor != side && lunge.kind == HitKind.EVADED -> -.35f * sin(t * PI).toFloat()
            else -> 0f
        }
        return out * toward
    }

    /** How white [side]'s portrait flashes: a blow that just landed on it. */
    fun flash(lunge: LungeView?, side: Side): Float {
        lunge ?: return 0f
        if (lunge.actor == side || !lunge.landed || lunge.action != Action.ATTACK || lunge.progress <= .5f) return 0f
        return (1 - lunge.progress) * 2f * (if (lunge.kind == HitKind.CRIT) 1f else .6f)
    }
}

/**
 * The fight's ground since 2.57.0 — the cave of the owner's mockup VI under the «HUD как в PoE»:
 * a vault, two ridges and the torches between them drifting at their own depths, the biome's floor
 * with its seams running to the horizon, fog along it. On it stand the fighters as framed portraits,
 * the hero in gold, the foe in its rarity's colour, and behind the foe the rest of its pack, waiting
 * dim. Each carries what is on it as the fight shows it — embers, frost, ice, sparks, bubbles,
 * drops of blood, stars over a stunned head — and a blow is the fighter itself lunging across.
 * No words and no numbers: those are the overlay's.
 */
internal fun DrawScope.fightStage(fight: FightHud, waiting: List<RolledMonster>, palette: Palette, classCode: String?, time: Float) {
    val w = size.width
    val h = size.height
    val floor = h * FightStage.FLOOR
    val sway = sin(time * .23f) * w * .02f
    drawRect(Brush.verticalGradient(listOf(palette.void, tone(palette.wallSide, .55f), tone(palette.floor, .6f)), 0f, floor), size = Size(w, floor))
    val vault = Offset(w / 2, -h * .05f)
    drawCircle(Brush.radialGradient(listOf(palette.accent.copy(alpha = .10f), Color.Transparent), vault, w * .8f), w * .8f, vault)
    stalactites(tone(palette.wallSide, .45f), sway * .2f)
    ridge(3, floor - h * .20f, h * .10f, tone(palette.wallSide, .75f), sway * .3f)
    listOf(.1f, .9f).forEachIndexed { i, share -> torch(w * share + sway * .55f, floor - h * .15f, time, i) }
    ridge(11, floor - h * .05f, h * .05f, tone(palette.wallTop, .55f), sway * .55f)
    ground(floor, palette, sway)
    val figure = FightStage.figure(w, h)
    // The rest of the pack, waiting its turn behind the foe: smaller, further, in the dark.
    waiting.forEachIndexed { i, monster ->
        val ring = rarityTint(monster.rarity)
        standee(w * (.86f + i * .09f) + sway * .8f, floor - h * .02f * (i + 1), figure * (.62f - i * .06f), ring, fade = .5f, palette = palette) {
            Portraits.monster(this, monster.code, monster.form, ring, time)
        }
    }
    val monster = fight.monster
    val ring = rarityTint(monster.rarity)
    val monsterX = w * (FightStage.MONSTER_X + FightStage.REACH * FightStage.shift(fight.lunge, Side.MONSTER)) + sway * .9f
    val heroX = w * (FightStage.HERO_X + FightStage.REACH * FightStage.shift(fight.lunge, Side.HERO)) + sway * .9f
    fighter(monsterX, floor, figure, ring, fight.monsterAilments, fight.monsterHeld, fight.monsterShield > 0, time,
        fade = if (fight.outcome == Outcome.WIN) .3f else 0f, palette = palette) { wash, amount ->
        Portraits.monster(this, monster.code, monster.form, ring, time, wash, amount, FightStage.flash(fight.lunge, Side.MONSTER))
        if (monster.rarity.ordinal > 0) Portraits.ring(this, ring, time)
    }
    fighter(heroX, floor, figure, Palettes.hero, fight.heroAilments, fight.heroHeld, fight.heroShield > 0, time,
        fade = if (fight.outcome == Outcome.LOSS) .3f else 0f, palette = palette) { wash, amount ->
        Portraits.hero(this, classCode, time, wash, amount, FightStage.flash(fight.lunge, Side.HERO))
    }
    burst(fight.lunge, (heroX + monsterX) / 2, floor - figure * .55f, figure)
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .6f)), Offset(w / 2, h / 2), maxOf(w, h) * .75f))
}

/** A fighter on the floor: its framed portrait, washed by its strongest ailment, and what is on it drawn around. */
private fun DrawScope.fighter(x: Float, feet: Float, height: Float, ring: Color, ailments: List<AilmentView>, held: Boolean, shielded: Boolean,
    time: Float, fade: Float, palette: Palette, portrait: DrawScope.(Color?, Float) -> Unit) {
    val wash = ailments.map { it.ailment }.maxByOrNull(::washAmount)
    if (shielded) drawOval(Color(0xFF63B7C4).copy(alpha = .35f), Offset(x - height * .5f, feet - height * 1.08f), Size(height, height * 1.12f), style = Stroke(1.5.dp.toPx()))
    standee(x, feet, height, ring, fade, palette, dim = held) { portrait(wash?.let(::ailmentTint), wash?.let(::washAmount) ?: 0f) }
    ailments.forEach { effect(it.ailment, x, feet, height, time) }
    if (held && ailments.none { it.ailment == Ailment.FROZEN }) stars(x, feet - height * 1.06f, height, time)
}

/**
 * A portrait standing on the ground in a cut-cornered frame, three by four as everywhere else:
 * its shadow, the frame in [ring] with a bronze line inside, [dim] while it can do nothing and
 * sunk into the dark by [fade].
 */
private fun DrawScope.standee(x: Float, feet: Float, height: Float, ring: Color, fade: Float, palette: Palette, dim: Boolean = false, portrait: DrawScope.() -> Unit) {
    val width = height * .75f
    val left = x - width / 2
    val top = feet - height
    drawOval(Color.Black.copy(alpha = .5f), Offset(x - width * .62f, feet - height * .035f), Size(width * 1.24f, height * .09f))
    val frame = frame(left, top, width, height, width * .12f)
    clipPath(frame) {
        drawRect(Color(0xFF0B0E13), Offset(left, top), Size(width, height))
        inset(left, top, size.width - left - width, size.height - top - height) { portrait() }
        if (dim) drawRect(Color(0xFF07090C).copy(alpha = .45f), Offset(left, top), Size(width, height))
        if (fade > 0f) drawRect(palette.void.copy(alpha = fade), Offset(left, top), Size(width, height))
    }
    drawPath(frame, ring.copy(alpha = 1f - fade * .6f), style = Stroke(2.dp.toPx()))
    val gap = 3.dp.toPx()
    drawPath(frame(left + gap, top + gap, width - gap * 2, height - gap * 2, width * .09f), Palettes.bronze.copy(alpha = .8f - fade * .5f), style = Stroke(1.dp.toPx()))
}

private fun frame(left: Float, top: Float, width: Float, height: Float, cut: Float) = Path().apply {
    moveTo(left + cut, top); lineTo(left + width - cut, top); lineTo(left + width, top + cut)
    lineTo(left + width, top + height - cut); lineTo(left + width - cut, top + height); lineTo(left + cut, top + height)
    lineTo(left, top + height - cut); lineTo(left, top + cut); close()
}

/** A stable pseudo-random share in 0..1 for particle [i], so a spark keeps its lane from frame to frame. */
private fun lane(i: Int): Float = ((sin(i * 127.1f + 311.7f) * 43758.547f) % 1f).let { abs(it) }

/** What an ailment looks like on its bearer: the particles and the crust its colour belongs to. */
private fun DrawScope.effect(ailment: Ailment, x: Float, feet: Float, height: Float, time: Float) {
    val tint = ailmentTint(ailment)
    val width = height * .75f
    when (ailment) {
        Ailment.BURNING -> repeat(12) { i ->
            val p = (time * 1.3f + lane(i)) % 1f
            drawCircle((if (i % 3 == 0) Color(0xFFFFD34A) else tint).copy(alpha = 1 - p), 2.4.dp.toPx() * (1 - p) + 1f,
                Offset(x + (lane(i + 3) - .5f) * width + sin(time * 5 + i) * 3f, feet - height * (.1f + p * .95f)))
        }
        Ailment.POISONED -> repeat(7) { i ->
            val p = (time * .7f + lane(i + 9)) % 1f
            drawCircle(tint.copy(alpha = 1 - p), 2.dp.toPx() + p * 3.dp.toPx(), Offset(x + (lane(i + 5) - .5f) * width * .8f, feet - height * (.2f + p * .7f)), style = Stroke(1.2.dp.toPx()))
        }
        Ailment.BLEEDING -> repeat(6) { i ->
            val p = (time * 1.1f + lane(i + 20)) % 1f
            drawOval(tint.copy(alpha = 1 - p * .6f), Offset(x + (lane(i + 21) - .5f) * width * .8f, feet - height * (.7f - p * .7f)), Size(3.dp.toPx(), 5.dp.toPx()))
        }
        Ailment.CHILLED -> repeat(10) { i ->
            val p = (time * .5f + lane(i + 30)) % 1f
            drawRect(Color(0xFFDFF4FF).copy(alpha = .9f * (1 - p)), Offset(x + (lane(i + 31) - .5f) * width * 1.3f, feet - height * (1.05f - p)), Size(2.dp.toPx(), 2.dp.toPx()))
        }
        Ailment.SHOCKED -> if (sin(time * 23) > -.2f) repeat(2) { k ->
            val bolt = Path().apply {
                var px = x - width * .4f + k * width * .5f
                var py = feet - height
                moveTo(px, py)
                repeat(5) { j -> px += (lane((time * 12).toInt() + j + k * 7) - .5f) * width * .4f; py += height * .18f; lineTo(px, py) }
            }
            drawPath(bolt, tint.copy(alpha = .9f), style = Stroke(1.5.dp.toPx()))
        }
        Ailment.FROZEN -> {
            val crust = Path().apply {
                moveTo(x - width * .62f, feet); lineTo(x - width * .7f, feet - height * .6f); lineTo(x - width * .2f, feet - height * 1.06f)
                lineTo(x + width * .5f, feet - height * .94f); lineTo(x + width * .66f, feet - height * .3f); lineTo(x + width * .5f, feet); close()
            }
            drawPath(crust, tint.copy(alpha = .28f))
            drawPath(crust, Color(0xFFE8F7FF).copy(alpha = .8f), style = Stroke(1.2.dp.toPx()))
        }
    }
}

/** Three stars round a stunned head. */
private fun DrawScope.stars(x: Float, y: Float, height: Float, time: Float) = repeat(3) { i ->
    val angle = time * 5 + i * 2.1f
    val centre = Offset(x + cos(angle) * height * .28f, y + sin(angle) * height * .06f)
    val r = 5.dp.toPx()
    val star = Path().apply {
        repeat(10) { k ->
            val a = -PI.toFloat() / 2 + k * PI.toFloat() / 5
            val rr = if (k % 2 == 0) r else r * .42f
            if (k == 0) moveTo(centre.x + cos(a) * rr, centre.y + sin(a) * rr) else lineTo(centre.x + cos(a) * rr, centre.y + sin(a) * rr)
        }
        close()
    }
    drawPath(star, Color(0xFFFFD34A))
}

/** The flash where a landed blow meets its target, gold, brighter on a critical strike. */
private fun DrawScope.burst(lunge: LungeView?, x: Float, y: Float, figure: Float) {
    lunge ?: return
    if (lunge.action != Action.ATTACK || lunge.progress <= .5f) return
    val t = (lunge.progress - .5f) * 2
    if (lunge.landed) {
        val tint = if (lunge.kind == HitKind.CRIT) Color(0xFFFFD34A) else Color(0xFFF0E2C0)
        val radius = figure * (.2f + .4f * t)
        drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = .9f * (1 - t)), tint.copy(alpha = .3f * (1 - t)), Color.Transparent), Offset(x, y), radius), radius, Offset(x, y))
    } else if (lunge.kind == HitKind.BLOCKED) {
        val toward = if (lunge.actor == Side.HERO) 1f else -1f
        drawArc(Color(0xFFC8C8C8).copy(alpha = .8f * (1 - t)), if (toward > 0) -70f else 110f, 140f, false,
            Offset(x + toward * figure * .2f - figure * .3f, y - figure * .3f), Size(figure * .6f, figure * .6f), style = Stroke(5.dp.toPx()))
    }
}

/** Icicles of rock along the vault. */
private fun DrawScope.stalactites(colour: Color, shift: Float) {
    val w = size.width
    val path = Path().apply {
        moveTo(-40f, 0f)
        for (i in 0..14) {
            val x = -40f + (w + 80f) * i / 14 + shift
            lineTo(x - 7.dp.toPx(), 0f); lineTo(x, size.height * (.03f + lane(i + 50) * .14f)); lineTo(x + 7.dp.toPx(), 0f)
        }
        lineTo(w + 40f, 0f); close()
    }
    drawPath(path, colour)
}

/** A jagged skyline from [seed], so a biome's cave keeps its shape all fight long. */
private fun DrawScope.ridge(seed: Int, top: Float, amplitude: Float, colour: Color, shift: Float) {
    val w = size.width
    val path = Path().apply {
        moveTo(-60f, size.height)
        for (i in 0..16) lineTo(-60f + (w + 120f) * i / 16 + shift, top - lane(seed + i) * amplitude)
        lineTo(w + 60f, size.height); close()
    }
    drawPath(path, colour)
}

/** A torch on its bracket, breathing, and the warm light it throws. */
private fun DrawScope.torch(x: Float, y: Float, time: Float, seed: Int) {
    val flicker = .82f + .12f * sin(time * 17 + seed) + .08f * sin(time * 29 + seed * 3)
    val glow = 40.dp.toPx() * flicker
    drawCircle(Brush.radialGradient(listOf(Palettes.torch.copy(alpha = .18f), Color.Transparent), Offset(x, y), glow), glow, Offset(x, y))
    drawRect(Color(0xFF2A2418), Offset(x - 2.dp.toPx(), y), Size(4.dp.toPx(), 18.dp.toPx()))
    drawRect(Palettes.bronze, Offset(x - 5.dp.toPx(), y - 2.dp.toPx()), Size(10.dp.toPx(), 4.dp.toPx()))
    val s = 6.dp.toPx()
    val flame = Path().apply {
        moveTo(x - s * .5f, y); quadraticTo(x - s * .6f, y - s * 1.1f * flicker, x + sin(time * 9 + seed) * s * .2f, y - s * 2.1f * flicker)
        quadraticTo(x + s * .6f, y - s * 1.1f * flicker, x + s * .5f, y); close()
    }
    drawPath(flame, Brush.verticalGradient(listOf(Color(0x00F0E2C0), Color(0xFFF0E2C0), Palettes.torch), y - s * 2.1f, y))
}

/** The floor: the biome's ground falling into the dark, its seams running to the horizon, fog drifting along it. */
private fun DrawScope.ground(floor: Float, palette: Palette, sway: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(tone(palette.floor, .9f), palette.void), floor, h), Offset(0f, floor), Size(w, h - floor))
    val seam = Palettes.bronze.copy(alpha = .16f)
    for (i in -7..7) drawLine(seam, Offset(w / 2 + i * w * .06f + sway * .8f, floor), Offset(w / 2 + i * w * .2f + sway, h), 1f)
    for (k in 1..4) { val y = floor + (h - floor) * (k / 5f).let { it * it }; drawLine(seam, Offset(0f, y), Offset(w, y), 1f) }
    drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Palettes.bronze.copy(alpha = .6f), Color.Transparent)), Offset(0f, floor), Offset(w, floor), 1.4f)
    withTransform({ scale(3f, 1f, Offset(w / 2, floor + (h - floor) * .25f)) }) {
        drawCircle(Brush.radialGradient(listOf(palette.accent.copy(alpha = .05f), Color.Transparent), Offset(w / 2 + sway, floor + (h - floor) * .25f), w * .2f),
            w * .2f, Offset(w / 2 + sway, floor + (h - floor) * .25f))
    }
}
