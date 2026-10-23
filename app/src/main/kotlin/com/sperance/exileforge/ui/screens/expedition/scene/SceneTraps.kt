package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.campaign.run.TrapSpot
import com.sperance.exileforge.rules.content.TrapShape
import com.sperance.exileforge.rules.run.TrapCell
import com.sperance.exileforge.rules.run.TrapLayout
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ловушки «Механизмы»: зона - материальная плита своего вида, многоклеточная - одной фигурой: клетки без зазора, шов и свет стихии
 * только по наружному краю видимых клеток, вспышка перед ударом - по общему контуру; узор вида - в каждой клетке (дыры шипов, решётка с
 * жаром, вентиляция яда, иней, медная шина между столбами, ромб пульсирующей зоны). Клетки в тумане не рисуются. Жернов - каменное колесо в жёлобе, маятник - лезвие на столбе. Плиты лежат
 * на полу под всем стоящим ([drawTrapFloor]); что встаёт из пола при ударе - шипы, огонь, ледяные иглы, - столбы, колесо и лезвие
 * стоят в очереди глубины ([standTrap], [drawTrapPart]). Ритм - часы карты ([TrapSpot.rhythm]): плита вспыхивает перед ударом.
 */
internal fun ScenePainter.drawTrapFloor(spot: TrapSpot, t: Double) {
    val beat = TrapBeat(spot, t)
    val layout = spot.layout
    when (layout.kind.shape) {
        TrapShape.ROLLER -> groove(layout)
        TrapShape.PENDULUM -> arcGroove(layout)
        else -> {
            val joints = trapJoints.of(layout)
            layout.cells.forEachIndexed { i, cell -> if (explored(cell.x, cell.y)) plate(layout, cell, visibleJoints(cell, joints[i]), beat) }
        }
    }
    if (layout.kind.shape == TrapShape.CLOUD) cloud(layout, beat)
}

/** Стоящие части ловушки [spot] - в очередь глубины: по клетке у встающих из пола, столбы, колесо, лезвие, кристалл ромба. */
internal fun ScenePainter.standTrap(queue: Standing, spot: TrapSpot, t: Double) {
    val layout = spot.layout
    when (layout.kind.shape) {
        TrapShape.LINE, TrapShape.PATCH, TrapShape.CROSS -> layout.cells.forEachIndexed { i, c -> queue.add(c.x + c.y + 1.0, Standing.FEATURE, spot, i) }

        TrapShape.CHAIN -> layout.posts.forEachIndexed { i, c -> queue.add(c.x + c.y + 1.0, Standing.FEATURE, spot, POST + i) }

        TrapShape.PENDULUM -> {
            queue.add(layout.x + layout.y + 1.0, Standing.FEATURE, spot, POST)
            spot.body(t)?.let { (bx, by) -> queue.add(bx + by, Standing.FEATURE, spot, BODY) }
        }

        TrapShape.ROLLER -> spot.body(t)?.let { (bx, by) -> queue.add(bx + by, Standing.FEATURE, spot, BODY) }

        TrapShape.PULSE -> queue.add(layout.x + layout.y + 1.0, Standing.FEATURE, spot, BODY)

        TrapShape.CLOUD -> Unit
    }
}

/** Часть [part] ловушки [spot], что поставил [standTrap]. */
internal fun ScenePainter.drawTrapPart(spot: TrapSpot, part: Int, t: Double) {
    val beat = TrapBeat(spot, t)
    val layout = spot.layout
    when (layout.kind.shape) {
        TrapShape.LINE -> layout.cells.getOrNull(part)?.takeIf { explored(it.x, it.y) }?.let { spikes(it, beat) }

        TrapShape.PATCH -> layout.cells.getOrNull(part)?.takeIf { explored(it.x, it.y) }?.let { flames(it, part, beat) }

        TrapShape.CROSS -> layout.cells.getOrNull(part)?.takeIf { explored(it.x, it.y) }?.let { needles(it, beat) }

        TrapShape.CHAIN -> {
            layout.posts.getOrNull(part - POST)?.let { stormPost(it, beat) }
            if (part - POST == layout.posts.lastIndex) bolt(layout, beat)
        }

        TrapShape.ROLLER -> spot.body(t)?.let { (bx, by) -> wheel(layout, bx, by) }

        TrapShape.PENDULUM -> if (part == POST) pendulumPost(layout) else pendulumBlade(spot, t)

        TrapShape.PULSE -> pulseCrystal(layout, beat)

        TrapShape.CLOUD -> Unit
    }
}

/**
 * Миг ритма ловушки на кадре: вспышка перед ударом [warn] (мерцает - [blink]), удар (первые [HIT] секунд) и опадание
 * ([rise]: встающее из пола), горит ли зона ([lit], пульсирующая гаснет), цвет стихии этого мига ([hue]) и свет контура ([edge]).
 */
private class TrapBeat(spot: TrapSpot, t: Double) {
    val warn: Float = spot.rhythm.warning(t).toFloat()
    val blink: Float = if (warn > 0) (.35f + .65f * warn) * (.75f + .25f * cos(warn * PI.toFloat() * 6)) else 0f
    private val since = t - if (spot.trap.trap.shape.moving) spot.struckAt else spot.rhythm.last(t)
    val hit: Boolean = since in 0.0..HIT
    val rise: Float = when {
        since < 0 -> 0f
        since < HIT -> 1f
        since < HIT + SINK -> (1 - (since - HIT) / SINK).toFloat()
        else -> 0f
    }
    val lit: Boolean = spot.rhythm.lit(t)
    val hue: Color = hueOf(spot.trap.trap.shape, spot.kindAt(t).element)
    val edge: Float = when {
        hit -> .85f
        warn > 0 -> .15f + .6f * blink
        !lit -> 0f
        rise > 0 -> .06f
        else -> .14f
    }
}

/** Цвет стихии ловушки: жернов - латунь, маятник - кровь, прочие - своя стихия. */
private fun hueOf(shape: TrapShape, element: String): Color = when {
    shape == TrapShape.ROLLER -> Brass
    shape == TrapShape.PENDULUM -> Blood
    element == "FIRE" -> Color(0xFFFF7A30)
    element == "CHAOS" -> Color(0xFF7CD050)
    element == "COLD" -> Color(0xFF8FD3FF)
    element == "LIGHTNING" -> Color(0xFFF2C94C)
    else -> Color(0xFFE0E6EE)
}

/**
 * Плита клетки [cell] формы [layout]: основа своего вида впритык к соседним клеткам той же ловушки, шов и контур в цвет стихии - только
 * по наружным рёбрам ([joints] - видимые соседи-клетки той же ловушки, биты [TrapJoints]), знак плиты - свой в каждой клетке.
 */
private fun ScenePainter.plate(layout: TrapLayout, cell: TrapCell, joints: Int, beat: TrapBeat) {
    val (x, y) = cell
    val light = glow(x, y)
    val shape = layout.kind.shape
    pen.color = when (shape) {
        TrapShape.CROSS -> Color(0xFF1A2430)
        TrapShape.CHAIN -> Color(0xFF241C14)
        else -> Color(0xFF1C1F23)
    }.copy(alpha = light)
    cellQuad(x, y, 0.0)
    pen.color = Seam.copy(alpha = light)
    outerRim(x, y, joints, RIM_INSET, unit * .03f)
    if (beat.edge > 0) {
        pen.color = beat.hue.copy(alpha = beat.edge * light)
        outerRim(x, y, joints, RIM_INSET, unit * .05f)
    }
    when (shape) {
        TrapShape.LINE -> HOLES.forEach { (a, b) ->
            pen.color = if (beat.warn > 0) beat.hue.copy(alpha = (.3f + .7f * beat.blink) * light) else Color(0xFF050607).copy(alpha = light)
            dot(x + a, y + b, .07f, .035f)
        }

        TrapShape.PATCH -> {
            pen.color = beat.hue.copy(alpha = beat.edge * light)
            outerQuad(x, y, joints, .14)
            pen.color = Color(0xFF4E4741).copy(alpha = light)
            for (b in GRATE) segment(x + .14, y + b, x + .86, y + b, unit * .06f)
        }

        TrapShape.CLOUD -> {
            pen.color = Color(0xFF050607).copy(alpha = light)
            dot(x + .5, y + .5, .3f, .15f)
            pen.color = beat.hue.copy(alpha = (.25f + beat.edge * .7f) * light)
            dot(x + .5, y + .5, .18f, .09f)
        }

        TrapShape.CROSS -> {
            pen.color = beat.hue.copy(alpha = (.35f + beat.edge * .65f) * light)
            val w = unit * .03f
            segment(x + .2, y + .3, x + .5, y + .5, w)
            segment(x + .5, y + .5, x + .75, y + .2, w)
            segment(x + .5, y + .5, x + .45, y + .85, w)
            segment(x + .5, y + .5, x + .85, y + .65, w)
        }

        TrapShape.CHAIN -> {
            val ax = layout.axis.dx * .5
            val ay = layout.axis.dy * .5
            pen.color = Copper.copy(alpha = light)
            segment(x + .5 - ax, y + .5 - ay, x + .5 + ax, y + .5 + ay, unit * .12f)
            pen.color = Color(0xFF7A4A22).copy(alpha = light)
            for (k in RUNGS) segment(x + .5 - ax + layout.axis.dx * k - ay * .4, y + .5 - ay + layout.axis.dy * k + ax * .4, x + .5 - ax + layout.axis.dx * k + ay * .4, y + .5 - ay + layout.axis.dy * k - ax * .4, unit * .04f)
            if (beat.edge > .2f) {
                pen.color = beat.hue.copy(alpha = beat.edge * light)
                segment(x + .5 - ax, y + .5 - ay, x + .5 + ax, y + .5 + ay, unit * .06f)
            }
        }

        TrapShape.PULSE -> {
            val on = beat.lit || beat.warn > 0
            pen.color = if (on) beat.hue.copy(alpha = maxOf(.4f, beat.edge) * light) else Seam.copy(alpha = light)
            cellRim(x, y, .22, unit * .04f)
            if (beat.hit || beat.warn > 0) {
                pen.color = beat.hue.copy(alpha = beat.edge * .45f * light)
                cellQuad(x, y, 0.0)
            }
        }

        else -> Unit
    }
}

/** Облако яда над вентиляцией: мягкий круг радиуса облака, гуще при ударе. */
private fun ScenePainter.cloud(layout: TrapLayout, beat: TrapBeat) {
    val alpha = when {
        beat.hit -> .55f
        beat.warn > 0 -> .18f + .3f * beat.blink
        beat.rise > 0 -> .06f
        else -> .13f
    }
    val r = (layout.size * TrapLayout.CLOUD_SPREAD).toFloat()
    val cx = isoX(layout.centreX, layout.centreY)
    val cy = isoY(layout.centreX, layout.centreY) + unit * .15f
    val w = r * unit * SQRT2
    for (k in 0 until CLOUD_RINGS) {
        val s = 1f - k * .22f
        pen.color = beat.hue.copy(alpha = alpha * (.45f + .25f * k) * glow(layout.x, layout.y))
        pen.ellipse(cx - w * s, cy - w * s / 2, w * s * 2, w * s)
    }
}

/** Жёлоб жернова: тёмные плиты пути, две латунные направляющие и поперечины по клеткам. */
private fun ScenePainter.groove(layout: TrapLayout) {
    val ax = layout.axis.dx.toDouble()
    val ay = layout.axis.dy.toDouble()
    layout.cells.forEach { (x, y) ->
        val light = glow(x, y)
        pen.color = Color(0xFF0B0D0F).copy(alpha = light)
        cellQuad(x, y, 0.0)
        pen.color = Color(0xFF2E3338).copy(alpha = light)
        segment(x + .5 - ax * .5 - ay * .5, y + .5 - ay * .5 - ax * .5, x + .5 - ax * .5 + ay * .5, y + .5 - ay * .5 + ax * .5, unit * .03f)
    }
    val first = layout.cells.first()
    val last = layout.cells.last()
    pen.color = Color(0xFF8A6E45).copy(alpha = glow(layout.x, layout.y))
    for (side in RAILS) {
        val ox = -ay * side
        val oy = ax * side
        segment(first.x + .5 - ax * .5 + ox, first.y + .5 - ay * .5 + oy, last.x + .5 + ax * .5 + ox, last.y + .5 + ay * .5 + oy, unit * .05f)
    }
}

/** Канавка маятника: тёмная дуга, что режет лезвие, по клеткам дуги - пунктир опасности. */
private fun ScenePainter.arcGroove(layout: TrapLayout) {
    val light = glow(layout.x, layout.y)
    val base = layout.baseAngle
    val r = layout.size.toDouble()
    for (i in 0 until ARC_STEPS) {
        val a0 = base - TrapLayout.SWING + 2 * TrapLayout.SWING * i / ARC_STEPS
        val a1 = base - TrapLayout.SWING + 2 * TrapLayout.SWING * (i + 1) / ARC_STEPS
        pen.color = Color(0xFF07080A).copy(alpha = light)
        segment(layout.centreX + r * cos(a0), layout.centreY + r * sin(a0), layout.centreX + r * cos(a1), layout.centreY + r * sin(a1), unit * .16f)
        if (i % 2 == 0) {
            pen.color = Color(0xFF3A2A2A).copy(alpha = light)
            segment(layout.centreX + (r + .6) * cos(a0), layout.centreY + (r + .6) * sin(a0), layout.centreX + (r + .6) * cos(a1), layout.centreY + (r + .6) * sin(a1), unit * .03f)
        }
    }
}

/** Шипы клетки: встают из дыр при ударе и опадают к перезарядке. */
private fun ScenePainter.spikes(cell: TrapCell, beat: TrapBeat) {
    if (beat.rise <= 0) return
    val h = unit * .62f * (if (beat.hit) 1f else beat.rise * .5f)
    val light = glow(cell.x, cell.y)
    HOLES.forEach { (a, b) ->
        val px = isoX(cell.x + a, cell.y + b)
        val py = isoY(cell.x + a, cell.y + b)
        pen.color = Color(0xFFD8DEE6).copy(alpha = light)
        pen.triangle(px - unit * .07f, py, px + unit * .07f, py, px, py + h)
        pen.color = Color(0xFF5E6770).copy(alpha = light)
        pen.line(px, py, px, py + h, unit * .02f)
    }
}

/** Языки огня решётки: тлеют во вспышке, встают при ударе; высота у каждой клетки своя. */
private fun ScenePainter.flames(cell: TrapCell, n: Int, beat: TrapBeat) {
    val scale = if (beat.hit) {
        1f
    } else if (beat.warn > 0) {
        .25f * beat.blink
    } else {
        0f
    }
    val hh = unit * (.55f + .25f * ((n * 37) % 5) / 5f) * scale
    if (hh < 2f) return
    val light = glow(cell.x, cell.y)
    val px = isoX(cell.x + .5, cell.y + .5)
    val py = isoY(cell.x + .5, cell.y + .5)
    pen.color = beat.hue.copy(alpha = .8f * light)
    pen.quad(px - unit * .28f, py, px - unit * .2f, py + hh * .6f, px, py + hh, px + unit * .2f, py + hh * .6f)
    pen.triangle(px - unit * .28f, py, px + unit * .2f, py + hh * .6f, px + unit * .28f, py)
    pen.color = Color(0xFFFFE2A0).copy(alpha = light)
    pen.triangle(px - unit * .13f, py, px + unit * .13f, py, px, py + hh * .6f)
}

/** Ледяные иглы креста: встают при ударе и тают к перезарядке. */
private fun ScenePainter.needles(cell: TrapCell, beat: TrapBeat) {
    if (beat.rise <= 0) return
    val h = unit * .75f * (if (beat.hit) 1f else beat.rise * .6f)
    val light = glow(cell.x, cell.y)
    NEEDLES.forEach { (a, b, s) ->
        val px = isoX(cell.x + a, cell.y + b)
        val py = isoY(cell.x + a, cell.y + b)
        pen.color = beat.hue.copy(alpha = .85f * light)
        pen.triangle(px - unit * .09f, py, px + unit * .09f, py, px - unit * .02f, py + h * s)
        pen.color = Color(0xFFEAF6FF).copy(alpha = light)
        pen.line(px - unit * .02f, py + h * s, px + unit * .09f, py, unit * .015f)
    }
}

/** Столб молнии на конце цепи: тёмный ствол с медными поясами и шар стихии, ярче во вспышке. */
private fun ScenePainter.stormPost(cell: TrapCell, beat: TrapBeat) {
    val light = glow(cell.x, cell.y)
    val px = isoX(cell.x + .5, cell.y + .5)
    val py = isoY(cell.x + .5, cell.y + .5)
    val h = unit * POST_HEIGHT
    val w = unit * .2f
    pen.color = Color.Black.copy(alpha = .5f * light)
    pen.ellipse(px - w * 1.6f, py - w * .8f, w * 3.2f, w * 1.6f)
    pen.color = Color(0xFF2A2E33).copy(alpha = light)
    pen.rect(px - w, py, w * 2, h)
    pen.color = Copper.copy(alpha = light)
    pen.rect(px - w, py + h * .3f, w * 2, unit * .08f)
    pen.rect(px - w, py + h * .6f, w * 2, unit * .08f)
    val orb = if (beat.hit) {
        1f
    } else if (beat.warn > 0) {
        .4f + .6f * beat.blink
    } else {
        .45f
    }
    pen.color = beat.hue.copy(alpha = orb * .35f * light)
    pen.circle(px, py + h + w * .6f, w * 2f)
    pen.color = beat.hue.copy(alpha = orb * light)
    pen.circle(px, py + h + w * .6f, w * 1.1f)
}

/** Молния меж столбов цепи: ломаная по верхам столбов при ударе, тонкая - в конце вспышки. */
private fun ScenePainter.bolt(layout: TrapLayout, beat: TrapBeat) {
    if (!beat.hit && beat.warn <= .6f) return
    val (a, b) = layout.posts
    val ax = isoX(a.x + .5, a.y + .5)
    val ay = isoY(a.x + .5, a.y + .5) + unit * (POST_HEIGHT + .12f)
    val bx = isoX(b.x + .5, b.y + .5)
    val by = isoY(b.x + .5, b.y + .5) + unit * (POST_HEIGHT + .12f)
    pen.color = (if (beat.hit) Color(0xFFFFFBEA) else beat.hue).copy(alpha = glow(layout.x, layout.y))
    var px = ax
    var py = ay
    for (i in 1..BOLT_STEPS) {
        val q = i.toFloat() / BOLT_STEPS
        val inner = i < BOLT_STEPS
        val nx = ax + (bx - ax) * q + (if (inner) (if (i % 2 == 1) 6f else -6f) else 0f)
        val ny = ay + (by - ay) * q + (if (inner) (if (i % 3 == 0) -4f else 3f) else 0f)
        pen.line(px, py, nx, ny, if (beat.hit) unit * .08f else unit * .04f)
        px = nx
        py = ny
    }
}

/** Каменное колесо жернова над точкой [bx], [by]: тень, обод латунью и спицы, что крутятся с ходом по жёлобу. */
private fun ScenePainter.wheel(layout: TrapLayout, bx: Double, by: Double) {
    val light = glow(bx.toInt(), by.toInt())
    val px = isoX(bx, by)
    val py = isoY(bx, by)
    val r = unit * WHEEL
    pen.color = Color.Black.copy(alpha = .55f * light)
    pen.ellipse(px - unit * .45f, py - unit * .2f, unit * .9f, unit * .4f)
    val cy = py + r
    pen.color = Color(0xFF6E6862).copy(alpha = light)
    pen.circle(px, cy, r)
    pen.color = Brass.copy(alpha = light)
    pen.ring(px - r, cy - r, r * 2, r * 2, unit * .06f)
    val roll = ((bx - layout.centreX) * layout.axis.dx + (by - layout.centreY) * layout.axis.dy) / WHEEL
    pen.color = Color(0xFF3E3A36).copy(alpha = light)
    for (k in 0 until SPOKES) {
        val a = roll + k * PI / 2
        pen.line(px, cy, px + (r * .8f * cos(a)).toFloat(), cy + (r * .8f * sin(a)).toFloat(), unit * .06f)
    }
    pen.color = Brass.copy(alpha = light)
    pen.circle(px, cy, unit * .09f)
}

/** Столб маятника с латунной шапкой. */
private fun ScenePainter.pendulumPost(layout: TrapLayout) {
    val light = glow(layout.x, layout.y)
    val px = isoX(layout.centreX, layout.centreY)
    val py = isoY(layout.centreX, layout.centreY)
    pen.color = Color.Black.copy(alpha = .5f * light)
    pen.ellipse(px - unit * .3f, py - unit * .15f, unit * .6f, unit * .3f)
    pen.color = Color(0xFF2A2E33).copy(alpha = light)
    pen.rect(px - unit * .12f, py, unit * .24f, unit * PENDULUM_HEIGHT)
    pen.color = Brass.copy(alpha = light)
    pen.rect(px - unit * .2f, py + unit * PENDULUM_HEIGHT, unit * .4f, unit * .14f)
}

/** Лезвие маятника на штанге от верха столба: тень, след прежнего мига и само лезвие с кровавой кромкой. */
private fun ScenePainter.pendulumBlade(spot: TrapSpot, t: Double) {
    val layout = spot.layout
    val (bx, by) = spot.body(t) ?: return
    val light = glow(bx.toInt(), by.toInt())
    val topX = isoX(layout.centreX, layout.centreY)
    val topY = isoY(layout.centreX, layout.centreY) + unit * PENDULUM_HEIGHT
    pen.color = Color.Black.copy(alpha = .45f * light)
    pen.ellipse(isoX(bx, by) - unit * .4f, isoY(bx, by) - unit * .16f, unit * .8f, unit * .32f)
    spot.body(t - TRAIL)?.let { (gx, gy) -> blade(topX, topY, isoX(gx, gy), isoY(gx, gy) + unit * .25f, .3f * light) }
    pen.color = Color(0xFF8A939C).copy(alpha = light)
    pen.line(topX, topY, isoX(bx, by), isoY(bx, by) + unit * .25f, unit * .05f)
    blade(topX, topY, isoX(bx, by), isoY(bx, by) + unit * .25f, light)
}

/** Лезвие-полумесяц в точке [x], [y] экрана поперёк штанги от [topX], [topY]. */
private fun ScenePainter.blade(topX: Float, topY: Float, x: Float, y: Float, alpha: Float) {
    val angle = atan2(y - topY, x - topX)
    val nx = -sin(angle)
    val ny = cos(angle)
    val l = unit * .72f
    val bellyX = x + cos(angle) * unit * .5f
    val bellyY = y + sin(angle) * unit * .5f
    pen.color = Color(0xFFC7CED6).copy(alpha = alpha)
    pen.triangle(x + nx * l, y + ny * l, bellyX, bellyY, x - nx * l, y - ny * l)
    pen.color = Color(0xFFD24A43).copy(alpha = alpha)
    pen.line(x + nx * l, y + ny * l, bellyX, bellyY, unit * .05f)
    pen.line(bellyX, bellyY, x - nx * l, y - ny * l, unit * .05f)
}

/** Кристалл в середине ромба: цветом стихии цикла, пока зона горит; погасшая - серый камень. */
private fun ScenePainter.pulseCrystal(layout: TrapLayout, beat: TrapBeat) {
    val light = glow(layout.x, layout.y)
    val px = isoX(layout.centreX, layout.centreY)
    val py = isoY(layout.centreX, layout.centreY)
    val h = unit * .75f
    val w = unit * .24f
    val on = beat.lit || beat.warn > 0
    val a = if (beat.hit) {
        1f
    } else if (beat.warn > 0) {
        .5f + .5f * beat.blink
    } else if (on) {
        .6f
    } else {
        0f
    }
    if (on && a > .6f) {
        pen.color = beat.hue.copy(alpha = .3f * a * light)
        pen.circle(px, py + h * .5f, w * 2.4f)
    }
    pen.color = (if (on) beat.hue else Seam).copy(alpha = (if (on) maxOf(.35f, a) else 1f) * light)
    pen.quad(px, py + h, px + w, py + h * .5f, px, py, px - w, py + h * .5f)
}

/** Клетка [x], [y] с отступом [ins] от краёв - залитой плитой. */
private fun ScenePainter.cellQuad(x: Int, y: Int, ins: Double) {
    val x0 = x + ins
    val y0 = y + ins
    val x1 = x + 1 - ins
    val y1 = y + 1 - ins
    pen.quad(isoX(x0, y0), isoY(x0, y0), isoX(x1, y0), isoY(x1, y0), isoX(x1, y1), isoY(x1, y1), isoX(x0, y1), isoY(x0, y1))
}

/** Контур клетки [x], [y] с отступом [ins] толщиной [width]. */
private fun ScenePainter.cellRim(x: Int, y: Int, ins: Double, width: Float) {
    val x0 = x + ins
    val y0 = y + ins
    val x1 = x + 1 - ins
    val y1 = y + 1 - ins
    segment(x0, y0, x1, y0, width)
    segment(x1, y0, x1, y1, width)
    segment(x1, y1, x0, y1, width)
    segment(x0, y1, x0, y0, width)
}

/**
 * Плита клетки [x], [y] с отступом [ins] только от наружных рёбер ([joints] - соседи той же ловушки): у соседних клеток заливки
 * смыкаются в одну фигуру.
 */
private fun ScenePainter.outerQuad(x: Int, y: Int, joints: Int, ins: Double) {
    val x0 = x + if (joints and TrapJoints.WEST == 0) ins else 0.0
    val y0 = y + if (joints and TrapJoints.NORTH == 0) ins else 0.0
    val x1 = x + 1 - if (joints and TrapJoints.EAST == 0) ins else 0.0
    val y1 = y + 1 - if (joints and TrapJoints.SOUTH == 0) ins else 0.0
    pen.quad(isoX(x0, y0), isoY(x0, y0), isoX(x1, y0), isoY(x1, y0), isoX(x1, y1), isoY(x1, y1), isoX(x0, y1), isoY(x0, y1))
}

/**
 * Наружный контур клетки [x], [y] толщиной [width] с отступом [ins]: рёбра без соседа той же ловушки ([joints]); у соседа ребро продолжается
 * до края клетки, во внутреннем углу формы - на [ins] дальше, к ребру соседа. Так контуры клеток сходятся в один контур фигуры.
 */
private fun ScenePainter.outerRim(x: Int, y: Int, joints: Int, ins: Double, width: Float) {
    if (joints and TrapJoints.NORTH == 0) {
        segment(x + rimEnd(joints, TrapJoints.WEST, TrapJoints.NORTH_WEST, ins), y + ins, x + 1 - rimEnd(joints, TrapJoints.EAST, TrapJoints.NORTH_EAST, ins), y + ins, width)
    }
    if (joints and TrapJoints.EAST == 0) {
        segment(x + 1 - ins, y + rimEnd(joints, TrapJoints.NORTH, TrapJoints.NORTH_EAST, ins), x + 1 - ins, y + 1 - rimEnd(joints, TrapJoints.SOUTH, TrapJoints.SOUTH_EAST, ins), width)
    }
    if (joints and TrapJoints.SOUTH == 0) {
        segment(x + 1 - rimEnd(joints, TrapJoints.EAST, TrapJoints.SOUTH_EAST, ins), y + 1 - ins, x + rimEnd(joints, TrapJoints.WEST, TrapJoints.SOUTH_WEST, ins), y + 1 - ins, width)
    }
    if (joints and TrapJoints.WEST == 0) {
        segment(x + ins, y + 1 - rimEnd(joints, TrapJoints.SOUTH, TrapJoints.SOUTH_WEST, ins), x + ins, y + rimEnd(joints, TrapJoints.NORTH, TrapJoints.NORTH_WEST, ins), width)
    }
}

/**
 * Где кончается наружное ребро у бока [side] клетки: бок наружный - угол фигуры, ребро короче на [ins]; бок к соседу и угол [corner] тоже
 * сосед - внутренний угол, ребро длиннее на [ins]; иначе ребро идёт до края клетки.
 */
private fun rimEnd(joints: Int, side: Int, corner: Int, ins: Double): Double = when {
    joints and side == 0 -> ins
    joints and corner != 0 -> -ins
    else -> 0.0
}

/** Видимые соседи клетки [cell] той же ловушки: из соседей формы [joints] - лишь исследованные клетки. */
private fun ScenePainter.visibleJoints(cell: TrapCell, joints: Int): Int {
    var seen = 0
    for (bit in 0 until TrapJoints.SIDES) {
        val mask = 1 shl bit
        if (joints and mask != 0 && explored(cell.x + TrapJoints.dx(bit), cell.y + TrapJoints.dy(bit))) seen = seen or mask
    }
    return seen
}

/** Отрезок по полу между точками карты. */
private fun ScenePainter.segment(x0: Double, y0: Double, x1: Double, y1: Double, width: Float) = pen.line(isoX(x0, y0), isoY(x0, y0), isoX(x1, y1), isoY(x1, y1), width)

/** Пятно на полу в точке карты: полуоси [rx], [ry] в долях полуклетки. */
private fun ScenePainter.dot(x: Double, y: Double, rx: Float, ry: Float) {
    val px = isoX(x, y)
    val py = isoY(x, y)
    pen.ellipse(px - unit * rx, py - unit * ry, unit * rx * 2, unit * ry * 2)
}

/** Отступ шва и контура плиты от наружного края фигуры, клеток. */
private const val RIM_INSET = .03

/** Части стоящего: клетки - своим номером, столбы и тело - отдельно. */
private const val POST = 1000
private const val BODY = 2000

/** Удар держится столько секунд, затем встающее опадает за [SINK]. */
private const val HIT = .2
private const val SINK = .35

/** Дыры шипов в клетке, доли клетки. */
private val HOLES = listOf(.3 to .5, .5 to .5, .7 to .5, .4 to .28, .6 to .72)

/** Прутья решётки огня поперёк клетки. */
private val GRATE = listOf(.26, .42, .58, .74)

/** Ледяные иглы клетки: место и доля высоты. */
private val NEEDLES = listOf(Triple(.35, .4, 1f), Triple(.6, .55, .7f), Triple(.45, .72, .85f))

/** Поперечины медной шины по клетке. */
private val RUNGS = listOf(.25, .5, .75)

/** Направляющие жёлоба по обе стороны оси, клеток. */
private val RAILS = listOf(-.38, .38)

private val Brass = Color(0xFFC9A26B)
private val Copper = Color(0xFFB87333)
private val Blood = Color(0xFFF08580)
private val Seam = Color(0xFF3A4046)

private const val POST_HEIGHT = 1.6f
private const val PENDULUM_HEIGHT = 2.1f
private const val WHEEL = .48f
private const val SPOKES = 4
private const val BOLT_STEPS = 8
private const val ARC_STEPS = 16
private const val CLOUD_RINGS = 3

/** На сколько секунд назад рисуется след лезвия. */
private const val TRAIL = .08

private val SQRT2 = sqrt(2f)
