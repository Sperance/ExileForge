package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.run.HeroCastView
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.ui.components.NeonHue
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Вид жилы по стихии (4.4.0, «Жилы энергии»): толщина [width] и размах изгиба [jag] в dp, сколько жил [veins] и боковых
 * ответвлений [branches], перерисовываются ли изломы каждый шаг ([reseed]), извиваются ли волной ([writhe]), и тяга частиц
 * [gravity] (dp/с², минус - вверх).
 */
private enum class VeinStyle(val width: Float, val jag: Float, val veins: Int, val branches: Int, val reseed: Boolean, val writhe: Boolean, val gravity: Float) {
    /** Огонь: толстые тёплые жилы с боковыми ответвлениями, искры поднимаются. */
    EMBER(6f, 26f, 1, 3, false, false, -160f),

    /** Молния: изломы и развилки, перерисовываются на каждом шаге. */
    FORK(3f, 70f, 2, 2, true, false, 0f),

    /** Холод: прямые тонкие иглы, осколки падают. */
    NEEDLE(2.2f, 4f, 3, 0, false, false, 120f),

    /** Хаос: извивающиеся фиолетовые жилы, дым едва поднимается. */
    WRITHE(3.5f, 30f, 2, 1, false, true, -30f),

    /** Физика: бледный прямой росчерк, щепки падают. */
    STREAK(3f, 8f, 1, 0, false, false, 380f),
    ;

    companion object {
        fun of(hue: NeonHue): VeinStyle = when (hue) {
            NeonHue.FIRE -> EMBER
            NeonHue.LIGHTNING -> FORK
            NeonHue.COLD -> NEEDLE
            NeonHue.CHAOS -> WRITHE
            else -> STREAK
        }
    }
}

/** Умения на себя: вместо жил - вращающиеся пунктирные орбиты. */
private val SELF_TYPES = setOf(SkillType.AURA, SkillType.WARCRY, SkillType.HEAL, SkillType.GUARD)

/** Доля сцены, за которую жила доходит до цели; дальше - удар и угасание (макет: 0,45 с из 0,9). */
private const val TRAVEL = .55f

/** Точек на жиле и на ответвлении. */
private const val POINTS = 14
private const val BRANCH_POINTS = 5

/** Искр удара у тира I-II и у тира III. */
private const val EMBERS = 26
private const val EMBERS_RICH = 46

/** Эхо применения рисуется бледнее. */
private const val ECHO_ALPHA = .45f

/** Пунктир орбиты: штрихов на круг. */
private const val DASHES = 18

/**
 * «Жилы энергии» (4.4.0, утверждён владельцем, макет 2): сцена последнего применения умения героя [cast] одним холстом поверх
 * боя. От героя к каждой цели бегут ветвящиеся жилы цвета умения ([NeonHue]) - огонь, молния, холод, хаос, физика по-своему
 * ([VeinStyle]), - гаснут от начала к концу; у цели - ударная волна и искры. Умение на себя - встречные пунктирные орбиты. Тир III
 * - больше искр и вспышка по краям, эхо - бледнее. Всё считается из прогресса сцены и номера строки лога детерминированным
 * шумом: ни частиц в куче, ни выделений на кадр. Бой анимируется всегда - `LocalMotion` сцену не гасит.
 */
@Composable internal fun SkillFx(cast: HeroCastView?, hero: Rect?, foe: (Int) -> Rect?, origin: Offset) {
    if (cast == null || hero == null) return
    val hue = cast.element?.let { element -> NeonHue.entries.firstOrNull { it.name == element } } ?: NeonHue.of(cast.type)
    val style = VeinStyle.of(hue)
    val density = LocalDensity.current
    val strokes = remember(density) { with(density) { FxStrokes(Stroke(2.dp.toPx(), cap = StrokeCap.Round), Stroke(3.dp.toPx()), Stroke(6.dp.toPx())) } }
    Canvas(Modifier.fillMaxSize()) {
        val alpha = if (cast.echo) ECHO_ALPHA else 1f
        val from = hero.center - origin
        if (cast.self || cast.type in SELF_TYPES || cast.foes.isEmpty()) {
            orbits(cast, hue.color, from, alpha, strokes)
            return@Canvas
        }
        cast.foes.forEachIndexed { n, index ->
            val to = (foe(index) ?: return@forEachIndexed).center - origin
            val start = Offset(from.x, from.y - 12.dp.toPx())
            veins(cast, style, hue.color, start, to, alpha, n)
            impact(cast, style, hue.color, to, alpha, n, strokes)
        }
        if (cast.tier >= 3 && cast.progress > TRAVEL) edgeFlash(hue.color, (1 - (cast.progress - TRAVEL) / (1 - TRAVEL)) * .5f * alpha, strokes)
    }
}

/** Штрихи сцены, собранные раз на плотность экрана: тонкий пунктир орбит и белое кольцо, ударная волна, вспышка краёв. */
private class FxStrokes(val thin: Stroke, val wave: Stroke, val edge: Stroke)

/** Жилы к цели [to]: голова бежит, хвост гаснет следом; у каждой - свой излом и ответвления. */
private fun DrawScope.veins(cast: HeroCastView, style: VeinStyle, color: Color, from: Offset, to: Offset, alpha: Float, target: Int) {
    val p = cast.progress
    val head = ease(min(1f, p / TRAVEL * 1.6f))
    val tail = max(0f, (p - TRAVEL * .7f) / (1 - TRAVEL * .7f))
    if (head <= tail) return
    val veins = style.veins + if (cast.tier >= 2) 1 else 0
    // Молния перерисовывает изломы на каждом шаге сцены
    val step = if (style.reseed) (p * 20).toInt() else 0
    repeat(veins) { k ->
        val seed = cast.serial * 31 + target * 7 + k * 131 + step * 977
        val width = (style.width - k).coerceAtLeast(1f).dp.toPx()
        vein(style, color, from, to, seed, p, head, tail, width, alpha * if (k == 0) 1f else .7f)
        repeat(style.branches) { b ->
            val at = .25f + .5f * noise(seed, 50 + b)
            if (at in tail..head) branch(color, point(style, from, to, seed, at, p), from, to, seed + b * 17, width * .6f, alpha * .7f)
        }
    }
}

/** Одна жила: [POINTS] отрезков от [tail] до [head] доли пути, свечение - широкий бледный штрих, сердцевина - белая нить. */
private fun DrawScope.vein(style: VeinStyle, color: Color, from: Offset, to: Offset, seed: Int, p: Float, head: Float, tail: Float, width: Float, alpha: Float) {
    val first = (tail * POINTS).toInt()
    val last = ceil(head * POINTS).toInt().coerceAtMost(POINTS)
    for (i in first until last) {
        val a = point(style, from, to, seed, i / POINTS.toFloat(), p)
        val b = point(style, from, to, seed, (i + 1) / POINTS.toFloat(), p)
        drawLine(color.copy(alpha = .25f * alpha), a, b, width * 3, StrokeCap.Round)
        drawLine(color.copy(alpha = alpha), a, b, width, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = .8f * alpha), a, b, max(1f, width * .3f), StrokeCap.Round)
    }
}

/** Точка жилы на доле [t] пути: отступ поперёк пути, ноль на концах; хаос извивается волной во времени. */
private fun DrawScope.point(style: VeinStyle, from: Offset, to: Offset, seed: Int, t: Float, p: Float): Offset {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = hypot(dx, dy).coerceAtLeast(1f)
    val nx = -dy / length
    val ny = dx / length
    val swing = if (style.writhe) sin(t * 6f + p * 12f + noise(seed, 0) * 6f) * .5f else noise(seed, (t * POINTS).toInt() + 1) - .5f
    val off = swing * style.jag.dp.toPx() * sin(t * PI.toFloat())
    return Offset(from.x + dx * t + nx * off, from.y + dy * t + ny * off)
}

/** Боковое ответвление из точки [at]: короткая ломаная в сторону от пути. */
private fun DrawScope.branch(color: Color, at: Offset, from: Offset, to: Offset, seed: Int, width: Float, alpha: Float) {
    val angle = atan2(to.y - from.y, to.x - from.x) + (if (noise(seed, 3) > .5f) 1 else -1) * (.6f + noise(seed, 4) * .6f)
    val reach = (18 + noise(seed, 5) * 22).dp.toPx()
    var last = at
    for (i in 1..BRANCH_POINTS) {
        val t = i / BRANCH_POINTS.toFloat()
        val jitter = (noise(seed, 10 + i) - .5f) * 8.dp.toPx()
        val next = Offset(at.x + cos(angle) * reach * t - sin(angle) * jitter, at.y + sin(angle) * reach * t + cos(angle) * jitter)
        drawLine(color.copy(alpha = alpha * (1 - t * .6f)), last, next, width, StrokeCap.Round)
        last = next
    }
}

/** Удар у цели: ударная волна, белое кольцо у тира II+, мягкое пятно цвета и искры по тяге стихии. */
private fun DrawScope.impact(cast: HeroCastView, style: VeinStyle, color: Color, at: Offset, alpha: Float, target: Int, strokes: FxStrokes) {
    val local = (cast.progress - TRAVEL * .8f) / (1 - TRAVEL * .8f)
    if (local <= 0f) return
    val e = ease(local)
    val fade = (1 - local) * alpha
    drawCircle(color.copy(alpha = .3f * fade), (30 + e * 110).dp.toPx(), at)
    drawCircle(color.copy(alpha = fade), (10 + e * 70).dp.toPx(), at, style = strokes.wave)
    if (cast.tier >= 2) drawCircle(Color.White.copy(alpha = .7f * fade), (6 + e * 40).dp.toPx(), at, style = strokes.thin)
    val count = if (cast.tier >= 3) EMBERS_RICH else EMBERS
    val seed = cast.serial * 53 + target * 11
    val seconds = local * SCENE_SECONDS
    for (i in 0 until count) {
        val life = .35f + noise(seed, i * 3) * .55f
        val age = local / life
        if (age >= 1f) continue
        val angle = noise(seed, i * 3 + 1) * 2 * PI.toFloat()
        val speed = (40 + noise(seed, i * 3 + 2) * (if (cast.tier >= 3) 280 else 180)).dp.toPx()
        val drift = ease(min(1f, local * 2.2f))
        val x = at.x + cos(angle) * speed * drift * .5f
        val y = at.y + sin(angle) * speed * drift * .5f + style.gravity.dp.toPx() * seconds * seconds * .5f
        drawCircle(color.copy(alpha = (1 - age) * alpha), (1.2f + noise(seed, i + 97) * 2f).dp.toPx() * (.6f + (1 - age) * .6f), Offset(x, y))
    }
}

/** Сколько секунд длится фаза удара на глаз - для тяги искр. */
private const val SCENE_SECONDS = .6f

/** Умение на себя: две-три встречные пунктирные орбиты вокруг героя, искры поднимаются. */
private fun DrawScope.orbits(cast: HeroCastView, color: Color, at: Offset, alpha: Float, strokes: FxStrokes) {
    val p = cast.progress
    val strength = sin(p * PI.toFloat()) * alpha
    val rings = if (cast.tier >= 3) 3 else 2
    val sweep = 360f / DASHES * .5f
    repeat(rings) { k ->
        val radius = (40 + k * 22 + sin(p * 8 + k) * 3).dp.toPx()
        val spin = p * 240f * if (k % 2 == 0) 1 else -1
        val corner = Offset(at.x - radius, at.y - radius)
        val box = Size(radius * 2, radius * 2)
        for (d in 0 until DASHES) {
            drawArc(color = color.copy(alpha = strength), startAngle = spin + d * 360f / DASHES, sweepAngle = sweep, useCenter = false, topLeft = corner, size = box, style = strokes.thin)
        }
    }
    val seed = cast.serial * 29
    for (i in 0 until if (cast.tier >= 3) EMBERS else EMBERS / 2) {
        val start = noise(seed, i) * .6f
        val age = (p - start) / .4f
        if (age !in 0f..1f) continue
        val x = at.x + (noise(seed, i + 40) - .5f) * 80.dp.toPx()
        val y = at.y + 20.dp.toPx() - age * 70.dp.toPx()
        drawCircle(color.copy(alpha = (1 - age) * alpha), 2.dp.toPx(), Offset(x, y))
    }
}

/** Вспышка тира III по краям: рамка цвета, тающая к центру. */
private fun DrawScope.edgeFlash(color: Color, alpha: Float, strokes: FxStrokes) {
    if (alpha <= 0f) return
    val step = strokes.edge.width
    for (i in 0 until EDGE_LAYERS) {
        val inset = step * (i + .5f)
        drawRect(color.copy(alpha = alpha * (1 - i / EDGE_LAYERS.toFloat())), Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2), style = strokes.edge)
    }
}

/** Слоёв вспышки краёв: каждый - штрих [FxStrokes.edge], всё бледнее к центру. */
private const val EDGE_LAYERS = 4

private fun ease(t: Float): Float = 1 - (1 - t) * (1 - t) * (1 - t)

/** Детерминированный шум 0..1 по зерну и номеру - без генератора и без выделений. */
private fun noise(seed: Int, i: Int): Float {
    var h = seed * 374761393 + i * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFFFF) / 16777215f
}
