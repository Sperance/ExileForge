package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.sperance.exileforge.core.campaign.Cell
import com.sperance.exileforge.core.campaign.run.BlightSpot
import com.sperance.exileforge.core.campaign.run.ExpeditionWorld
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.theme.rarityColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// ==================== Скверна на карте (4.0.0, макет A «Гнойное пятно») ====================

/** Палитра Скверны (RULES.md, «Скверна»): только она - у очага, следа, сундука, полосы карты и полосы Матери. */
internal object BlightTint {
    val void = Color(0xFF12040A)
    val flesh = Color(0xFF5A0E2A)
    val vein = Color(0xFFD81B60)
    val hot = Color(0xFFFF5C9A)
    val bile = Color(0xFFE8D27A)
    val bone = Color(0xFFE8D7C0)
}

/**
 * Очаг Скверны по частям, каждая своей глубиной: шлейф следа между раскрытыми точками и лужи лежат на земле (под всем, что
 * стоит), сундук зачищенной точки стоит своей клеткой. Живая лужа - у активной точки, у пройденных - остывшее пятно.
 */
internal fun ScenePainter.blightParts(spot: BlightSpot, world: ExpeditionWorld, glow: (Int, Int) -> Float): List<Pair<Double, () -> Unit>> = buildList {
    val shown = spot.revealed
    val active = spot.active
    for (point in 0 until shown) {
        val cell = spot.at(point)
        if (!world.explored(cell.x, cell.y)) continue
        if (point + 1 < shown) add(GROUND to { drawBlightTrail(cell, spot.at(point + 1), point, glow(cell.x, cell.y)) })
        val live = point == active
        add(GROUND + .1 to { drawBlightPool(cell, live, point, glow(cell.x, cell.y)) })
        // Сундук: взятый стоит открытым, ждущий - на зачищенной активной точке; на клетке Матери стоит её сундук
        val looted = point in spot.taken && !(point == spot.blight.points - 1 && spot.raised(spot.blight.motherChoice))
        val waiting = live && spot.cleared(world, point)
        if (looted || waiting) add(cell.x + cell.y + 1.0 to { drawBlightChest(cell, spot.blight.quality(point) ?: Rarity.COMMON, looted, glow(cell.x, cell.y)) })
    }
    // Сундук Матери - на последней точке, после её смерти
    val mother = spot.blight.motherChoice
    if (spot.blight.mother && spot.raised(mother)) {
        val cell = spot.at(mother)
        val looted = mother in spot.taken
        if (world.explored(cell.x, cell.y) && (looted || spot.cleared(world, mother))) {
            add(cell.x + cell.y + 1.1 to { drawBlightChest(cell, spot.blight.quality(mother) ?: Rarity.RARE, looted, glow(cell.x, cell.y)) })
        }
    }
}

/**
 * Живая лужа очага: рваный текучий край (несколько бегущих волн по контуру), от центра к краю пульсируют жилы, ядро бьётся.
 * Пройденная точка - тусклое пятно поменьше, без жил. Декор стоит при выключенных анимациях ([ScenePainter.decor]).
 */
internal fun ScenePainter.drawBlightPool(cell: Cell, live: Boolean, seed: Int, light: Float) {
    val cx = isoX(cell.x + .5, cell.y + .5)
    val cy = -isoY(cell.x + .5, cell.y + .5)
    val t = decor
    val radius = unit * POOL_CELLS * SQRT2 * (if (live) 1f else .6f)
    val breathe = if (live) .78f + .22f * sin(t * 2 * PI.toFloat() / BREATHE_SECONDS + seed) else 1f
    val scope = pen.scope
    scope.withTransform({ scale(1f, .5f, Offset(cx, cy)) }) {
        val edge = Path()
        for (i in 0..EDGE_POINTS) {
            val a = i * 2 * PI.toFloat() / EDGE_POINTS
            val wobble = 1f + .12f * sin(3 * a + t * .9f + seed) + .08f * sin(5 * a - t * 1.3f) + .05f * sin(9 * a + t * 2f + seed * 2)
            val x = cx + cos(a) * radius * wobble
            val y = cy + sin(a) * radius * wobble
            if (i == 0) edge.moveTo(x, y) else edge.lineTo(x, y)
        }
        edge.close()
        val stops = if (live) {
            listOf(BlightTint.hot, BlightTint.vein, BlightTint.flesh, BlightTint.void.copy(alpha = 0f))
        } else {
            listOf(BlightTint.flesh, BlightTint.flesh.copy(alpha = .7f), BlightTint.void.copy(alpha = .4f), BlightTint.void.copy(alpha = 0f))
        }
        drawPath(edge, Brush.radialGradient(stops.map { tone(it, light, alpha = it.alpha * (if (live) breathe else .55f)) }, Offset(cx, cy), radius * 1.15f))
        if (!live) return@withTransform
        // Жилы: свечение и тонкая нить, от центра к краю, ломаной
        VEINS.forEachIndexed { i, (angle, bend) ->
            val a = angle + seed * .7f
            val mid = Offset(cx + cos(a + bend) * radius * .5f, cy + sin(a + bend) * radius * .5f)
            val tip = Offset(cx + cos(a) * radius * .9f, cy + sin(a) * radius * .9f)
            val pulse = .55f + .45f * sin(t * 2 * PI.toFloat() / BREATHE_SECONDS + i * .9f)
            drawLine(BlightTint.hot.copy(alpha = .25f * pulse * light), Offset(cx, cy), mid, unit * .16f, StrokeCap.Round)
            drawLine(BlightTint.hot.copy(alpha = .25f * pulse * light), mid, tip, unit * .12f, StrokeCap.Round)
            drawLine(BlightTint.hot.copy(alpha = .9f * pulse * light), Offset(cx, cy), mid, unit * .05f, StrokeCap.Round)
            drawLine(BlightTint.hot.copy(alpha = .8f * pulse * light), mid, tip, unit * .035f, StrokeCap.Round)
        }
        // Ядро бьётся: 2,2 с, на пике чуть шире и тусклее
        val beat = .5f + .5f * sin(t * 2 * PI.toFloat() / CORE_SECONDS)
        val core = unit * .3f * (1f + .08f * beat)
        drawCircle(Brush.radialGradient(listOf(BlightTint.hot.copy(alpha = .55f * light), Color.Transparent), Offset(cx, cy), core * 2.4f), core * 2.4f, Offset(cx, cy))
        drawCircle(BlightTint.hot.copy(alpha = (1f - .15f * beat) * light), core, Offset(cx, cy))
        drawCircle(BlightTint.bile.copy(alpha = .7f * light), core * .35f, Offset(cx, cy))
    }
}

/**
 * След Скверны от точки [from] к [to]: багровый шлейф плоти (два смещённых мазка - рваный край) и по нему ползущая пунктирная
 * струя ихора. Изгиб - свой у каждого отрезка [seed].
 */
internal fun ScenePainter.drawBlightTrail(from: Cell, to: Cell, seed: Int, light: Float) {
    val a = Offset(isoX(from.x + .5, from.y + .5), -isoY(from.x + .5, from.y + .5))
    val b = Offset(isoX(to.x + .5, to.y + .5), -isoY(to.x + .5, to.y + .5))
    val length = hypot(b.x - a.x, b.y - a.y).coerceAtLeast(1f)
    val normal = Offset(-(b.y - a.y) / length, (b.x - a.x) / length)
    val bend = (if (seed % 2 == 0) 1f else -1f) * length * .18f
    val control = Offset((a.x + b.x) / 2 + normal.x * bend, (a.y + b.y) / 2 + normal.y * bend)
    fun curve(shift: Float) = Path().apply {
        moveTo(a.x + normal.x * shift, a.y + normal.y * shift)
        quadraticTo(control.x + normal.x * shift, control.y + normal.y * shift, b.x + normal.x * shift, b.y + normal.y * shift)
    }
    val scope = pen.scope
    val width = unit * .42f
    scope.drawPath(curve(0f), BlightTint.flesh.copy(alpha = .55f * light), style = Stroke(width, cap = StrokeCap.Round))
    scope.drawPath(curve(width * .22f), BlightTint.flesh.copy(alpha = .35f * light), style = Stroke(width * .7f, cap = StrokeCap.Round))
    scope.drawPath(curve(-width * .25f), BlightTint.void.copy(alpha = .4f * light), style = Stroke(width * .4f, cap = StrokeCap.Round))
    val dash = unit * .2f
    scope.drawPath(
        curve(0f),
        BlightTint.vein.copy(alpha = .9f * light),
        style = Stroke(unit * .08f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash * 1.6f), -decor * dash * 2.6f / ICHOR_SECONDS)),
    )
}

/** Сундук Скверны: сундук карты в рамке качества [quality] - окованный её цветом, с её отсветом на земле; открытый - тусклее. */
internal fun ScenePainter.drawBlightChest(cell: Cell, quality: Rarity, opened: Boolean, light: Float) {
    val x = cell.x + .5
    val y = cell.y + .5
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    val frame = rarityColor(quality.name)
    val w = unit * .45f
    val d = unit * .22f
    val h = unit * .38f
    pen.color = frame.copy(alpha = (if (opened) .12f else .22f + .08f * sin(decor * 2f + x.toFloat())) * light)
    pen.ellipse(cx - w * 1.6f, cy - d * 1.3f, w * 3.2f, d * 2.6f)
    drawChest(x, y, opened, light)
    // Рамка по качеству: рёбра крышки и передние грани
    pen.color = frame.copy(alpha = (if (opened) .45f else .95f) * light)
    val top = cy + h
    pen.polyline(cx - w, top, cx, top + d, cx + w, top, cx, top - d, cx - w, top, width = unit * .045f)
    pen.polyline(cx - w, cy, cx - w, cy + h, width = unit * .04f)
    pen.polyline(cx + w, cy, cx + w, cy + h, width = unit * .04f)
    pen.polyline(cx, cy - d, cx, cy - d + h, width = unit * .04f)
    if (!opened) {
        pen.color = BlightTint.bile.copy(alpha = .85f * light)
        pen.circle(cx, cy + h * .55f, unit * .05f)
    }
}

/**
 * Жетон кольца очага «выпрыгивает» (0,6 с, по очереди): масштаб жетона, что стоит [age] секунд, [n]-й в кольце. Появление -
 * переход, а не декор: идёт и при выключенных анимациях.
 */
internal fun blightPop(age: Double, n: Int): Float {
    val t = ((age - n * POP_STAGGER) / POP_SECONDS).toFloat()
    if (t <= 0f) return 0f
    if (t >= 1f) return 1f
    // Отскок с перелётом (как cubic-bezier(.3, 1.6, .5, 1) макета)
    val u = t - 1f
    return 1f + BACK_OVERSHOOT_3 * u * u * u + BACK_OVERSHOOT * u * u
}

/** Земля под всем стоящим: шлейф и лужи ложатся раньше любой скалы и фигуры. */
private const val GROUND = -1e9

private const val SQRT2 = 1.4142135f

/** Радиус лужи очага в клетках. */
private const val POOL_CELLS = 1.15f

/** Точек на рваном крае лужи. */
private const val EDGE_POINTS = 36

/** Дыхание лужи и жил и удар ядра, секунды (макет: 3,4 с и 2,2 с). */
private const val BREATHE_SECONDS = 3.4f
private const val CORE_SECONDS = 2.2f

/** За сколько секунд струя ихора проползает свой шаг пунктира (макет: 1,6 с). */
private const val ICHOR_SECONDS = 1.6f

/** Жилы лужи: угол от центра и излом середины, радианы. */
private val VEINS = listOf(-2.6f to .35f, -0.7f to -.2f, .45f to .3f, 1.75f to -.25f, 2.85f to .2f)

/** Появление жетона кольца: длительность и шаг очереди, секунды (макет: 0,6 с и 0,12 с). */
private const val POP_SECONDS = .6
private const val POP_STAGGER = .12

/** Перелёт отскока при появлении. */
private const val BACK_OVERSHOOT = 1.70158f
private const val BACK_OVERSHOOT_3 = BACK_OVERSHOOT + 1f
