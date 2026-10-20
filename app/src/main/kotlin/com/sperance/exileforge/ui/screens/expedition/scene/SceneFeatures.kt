package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import com.sperance.exileforge.core.campaign.Tile
import com.sperance.exileforge.core.campaign.run.AltarSpot
import com.sperance.exileforge.core.campaign.run.BlightSpot
import com.sperance.exileforge.core.campaign.run.ExpeditionWorld
import com.sperance.exileforge.core.campaign.run.FeatureSpot
import com.sperance.exileforge.core.campaign.run.MerchantSpot
import com.sperance.exileforge.core.campaign.run.NodeSpot
import com.sperance.exileforge.core.campaign.run.RoomSpot
import com.sperance.exileforge.core.campaign.run.TrapSpot
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Job
import com.sperance.exileforge.rules.content.NodeLook
import com.sperance.exileforge.rules.run.FeatureKind
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Rune
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Объект карты в стиле сцены: каждый вид - свой знак. Глубина сортировки - его клетка; комната рисует трещину или дверь на
 * стене, рычаг и сундук внутри - каждый своей клеткой: в очередь стоящего ложится часть ([part]), рисует её [drawFeature].
 *
 * Открытый вход рисуется проёмом с распахнутыми створками; тайная трещина открытой не видна.
 */
internal fun ScenePainter.standFeature(queue: Standing, spot: FeatureSpot, world: ExpeditionWorld) {
    when (spot) {
        is AltarSpot, is MerchantSpot, is NodeSpot -> queue.add(depth(spot.cell.x, spot.cell.y), Standing.FEATURE, spot)

        is TrapSpot -> standTrap(queue, spot, world.clock)

        is RoomSpot -> {
            val (ex, ey) = spot.entrance
            if (!spot.opened || spot.kind == FeatureKind.VAULT) queue.add(depth(ex, ey) + .1, Standing.FEATURE, spot, ROOM_ENTRANCE)
            spot.lever?.takeIf { world.explored(it.x, it.y) }?.let { queue.add(depth(it.x, it.y), Standing.FEATURE, spot, ROOM_LEVER) }
            spot.chest.takeIf { spot.opened && world.explored(it.x, it.y) }?.let { queue.add(depth(it.x, it.y), Standing.FEATURE, spot, ROOM_CHEST) }
        }

        is BlightSpot -> standBlight(queue, spot, world)
    }
}

/** Часть [part] объекта [spot], что поставил в очередь [standFeature]. */
internal fun ScenePainter.drawFeature(spot: FeatureSpot, part: Int, world: ExpeditionWorld) {
    when (spot) {
        is AltarSpot -> drawAltar(spot.cell.x + .5, spot.cell.y + .5, spot.spent, glow(spot.cell.x, spot.cell.y))

        is MerchantSpot -> drawMerchant(spot.cell.x + .5, spot.cell.y + .5, spot.spent, glow(spot.cell.x, spot.cell.y))

        is NodeSpot -> {
            drawNode(spot.cell.x + .5, spot.cell.y + .5, spot.node.job.look, spot.spent, glow(spot.cell.x, spot.cell.y))
            if (!spot.spent) materials[spot.node.job.output]?.let { drawMaterial(spot.cell.x + .5, spot.cell.y + .5, it, glow(spot.cell.x, spot.cell.y)) }
        }

        is TrapSpot -> drawTrapPart(spot, part, world.clock)

        is RoomSpot -> when (part) {
            ROOM_ENTRANCE -> {
                val (ex, ey) = spot.entrance
                if (spot.kind == FeatureKind.VAULT) drawDoor(doorFace(world, ex, ey), wallRise(ex, ey), spot.opened, glow(ex, ey)) else drawCrack(ex + .5, ey + .5, spot.progress.toFloat(), glow(ex, ey))
            }

            ROOM_LEVER -> spot.lever?.let { lever ->
                val face = leverFace(world, lever.x, lever.y)
                if (face != null) drawWallLever(face, spot.opened, glow(lever.x, lever.y)) else drawLever(lever.x + .5, lever.y + .5, spot.opened, glow(lever.x, lever.y))
            }

            else -> spot.chest.let { drawChest(it.x + .5, it.y + .5, spot.looted, glow(it.x, it.y)) }
        }

        is BlightSpot -> drawBlight(spot, part, world)
    }
}

/** Части комнаты в очереди стоящего. */
private const val ROOM_ENTRANCE = 0
private const val ROOM_LEVER = 1
private const val ROOM_CHEST = 2

private fun depth(x: Int, y: Int) = x + y + 1.0

/** Алтарь сделки: чёрный камень с алой и золотой чашами; заключённый - остывший. */
internal fun ScenePainter.drawAltar(x: Double, y: Double, spent: Boolean, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    val w = unit * .4f
    val d = unit * .2f
    val h = unit * .45f
    val stone = Color(0xFF3B3540)
    pen.color = Color.Black.copy(alpha = .35f)
    pen.ellipse(cx - w * 1.2f, cy - d * .8f, w * 2.4f, d * 1.6f)
    pen.color = tone(stone, .8f * light)
    pen.quad(cx - w, cy, cx, cy - d, cx, cy - d + h, cx - w, cy + h)
    pen.color = tone(stone, .6f * light)
    pen.quad(cx, cy - d, cx + w, cy, cx + w, cy + h, cx, cy - d + h)
    pen.color = tone(stone, light)
    diamond(cx, cy + h, w, d)
    val boon = Color(0xFFE8C060)
    val curse = Color(0xFFD03040)
    val beat = if (spent) 0f else .5f + .5f * sin(time * 2.2f + x.toFloat())
    pen.color = (if (spent) Color(0xFF4A4038) else boon).copy(alpha = (.7f + .3f * beat) * light)
    pen.circle(cx - w * .45f, cy + h + d * .1f, unit * .08f)
    pen.color = (if (spent) Color(0xFF402A2C) else curse).copy(alpha = (.7f + .3f * (1 - beat)) * light)
    pen.circle(cx + w * .45f, cy + h + d * .1f, unit * .08f)
    if (!spent) {
        pen.color = curse.copy(alpha = .12f * light)
        pen.circle(cx, cy + h * .8f, unit * .55f)
    }
}

/** Странствующий торговец: полосатый навес на шестах и мешок с золотом; распроданный - без блеска. */
internal fun ScenePainter.drawMerchant(x: Double, y: Double, spent: Boolean, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    val w = unit * .45f
    val h = unit * .8f
    pen.color = Color.Black.copy(alpha = .35f)
    pen.ellipse(cx - w * 1.2f, cy - unit * .15f, w * 2.4f, unit * .3f)
    pen.color = tone(Color(0xFF5A3A20), light)
    pen.line(cx - w, cy, cx - w, cy + h, unit * .05f)
    pen.line(cx + w, cy, cx + w, cy + h, unit * .05f)
    pen.color = tone(Color(0xFFB03A2E), light)
    pen.triangle(cx - w * 1.15f, cy + h, cx + w * 1.15f, cy + h, cx, cy + h + unit * .35f)
    pen.color = tone(Color(0xFFE8D8B0), light)
    pen.triangle(cx - w * .4f, cy + h, cx + w * .4f, cy + h, cx, cy + h + unit * .35f)
    pen.color = tone(Color(0xFF8A6A3A), light)
    pen.circle(cx, cy + unit * .14f, unit * .14f)
    if (!spent) {
        pen.color = Palettes.torch.copy(alpha = (.55f + .35f * sin(time * 3f + y.toFloat())) * light)
        pen.circle(cx, cy + unit * .3f, unit * .05f)
    }
}

/** Узел ремесла: облик его работы-сбора ([NodeGlyphs]), целый или собранный. */
internal fun ScenePainter.drawNode(x: Double, y: Double, look: NodeLook?, spent: Boolean, light: Float) {
    look ?: return
    nodes.draw(pen.scope, look, spent, isoX(x, y), isoY(x, y), unit, light)
}

/** Кольцо идущего сбора по полу вокруг узла: тёмный след и золотая доля [progress] времени сбора - от верха по часовой. */
internal fun ScenePainter.drawGather(x: Double, y: Double, progress: Float, light: Float) {
    val w = unit * GATHER_RING
    val h = w * .5f
    val left = isoX(x, y) - w / 2
    val bottom = isoY(x, y) - h / 2
    pen.color = Color.Black.copy(alpha = .35f * light)
    pen.ring(left, bottom, w, h, unit * .1f)
    pen.color = GoldBright.copy(alpha = light)
    pen.ringArc(left, bottom, w, h, 90f - 360f * progress, 360f * progress, unit * .07f)
}

/** Поперечник кольца сбора у узла в клетках сцены. */
private const val GATHER_RING = 1.3f

/** Тайный вход - светлая трещина в скале, что ярче, пока герой стоит рядом. */
internal fun ScenePainter.drawCrack(x: Double, y: Double, progress: Float, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    pen.color = Color(0xFFE8E0C8).copy(alpha = (.35f + .55f * progress) * light)
    pen.polyline(cx - unit * .05f, cy + unit * .95f, cx + unit * .08f, cy + unit * .7f, cx - unit * .06f, cy + unit * .5f, cx + unit * .05f, cy + unit * .25f, width = unit * .04f)
}

/**
 * Грань стены в изометрии: от угла [x0], [y0] вдоль ([ax], [ay]) на одну
 * клетку, к камере - по нормали ([nx], [ny]). Точка грани - доля [s] вдоль, высота [t] в клетках и шаг [n] от грани к камере.
 */
internal class WallFace(private val x0: Double, private val y0: Double, private val ax: Double, private val ay: Double, private val nx: Double, private val ny: Double) {
    fun x(painter: ScenePainter, s: Double, n: Double = 0.0): Float = painter.isoX(x0 + ax * s + nx * n, y0 + ay * s + ny * n)

    fun y(painter: ScenePainter, s: Double, t: Double, n: Double = 0.0): Float = painter.isoY(x0 + ax * s + nx * n, y0 + ay * s + ny * n) + (t * painter.unit).toFloat()

    companion object {
        /** Ближняя к камере грань клетки [x], [y], что смотрит вдоль +y (вниз-влево) или +x (вниз-вправо). */
        fun facingY(x: Int, y: Int) = WallFace(x.toDouble(), y + 1.0, 1.0, 0.0, 0.0, 1.0)

        fun facingX(x: Int, y: Int) = WallFace(x + 1.0, y.toDouble(), 0.0, 1.0, 1.0, 0.0)
    }
}

/**
 * Грань двери: вход - стена толщиной в клетку между полом перед ним и полом комнаты, обе её грани на оси прохода - дверные;
 * рисуется та, что смотрит в камеру.
 */
private fun doorFace(world: ExpeditionWorld, x: Int, y: Int): WallFace {
    val alongY = world.map.tile(x, y + 1) != Tile.WALL || world.map.tile(x, y - 1) != Tile.WALL
    return if (alongY) WallFace.facingY(x, y) else WallFace.facingX(x, y)
}

/** Грань стены за рычагом [x], [y], видная камере; нет такой стены - null (рычаг на постаменте). */
private fun leverFace(world: ExpeditionWorld, x: Int, y: Int): WallFace? = when {
    world.map.tile(x, y - 1) == Tile.WALL -> WallFace.facingY(x, y - 1)
    world.map.tile(x - 1, y) == Tile.WALL -> WallFace.facingX(x - 1, y)
    else -> null
}

/** Точки многоугольников двери: одна на сцену, без массива на кадр. */
private val doorPoints = FloatArray(64)

/**
 * Арка на грани [face] в [doorPoints]: прямые косяки от [t0] до [t1] между [s0] и [s1], сверху полукруг; число точек.
 * Глубина [n] - шаг от грани к камере.
 */
private fun ScenePainter.arch(face: WallFace, s0: Double, s1: Double, t0: Double, t1: Double, n: Double = 0.0): Int {
    var k = 0
    fun put(s: Double, t: Double) {
        doorPoints[k * 2] = face.x(this, s, n)
        doorPoints[k * 2 + 1] = face.y(this, s, t, n)
        k++
    }
    put(s0, t0)
    put(s0, t1)
    val r = (s1 - s0) / 2
    val mid = (s0 + s1) / 2
    for (i in 1 until ARCH_STEPS) {
        val a = PI - PI * i / ARCH_STEPS
        put(mid + r * cos(a), t1 + r * sin(a))
    }
    put(s1, t1)
    put(s1, t0)
    return k
}

/**
 * Дверь в плоскости стены: каменные косяки и арка с замковым камнем,
 * две створки из досок в кованых полосах с петлями и кольцами; закрытая - из щели между створками бьёт тёплый свет и ложится
 * полосой на пол; открытая - створки распахнуты внутрь, проём светится. Высота - в рост стены [rise].
 */
private fun ScenePainter.drawDoor(face: WallFace, rise: Double, open: Boolean, light: Float) {
    val height = minOf(DOOR_MAX, rise - DOOR_HEADROOM)
    val s0 = .16
    val s1 = .84
    val radius = (s1 - s0) / 2
    val spring = (height - radius).coerceAtLeast(radius / 2)
    val flicker = if (motion) .7f + .3f * sin(time * 3.1f) else 1f
    pen.color = Palettes.torch.copy(alpha = (if (open) .3f else .12f * flicker) * light)
    val far = if (open) 1.4 else 1.0
    pen.quad(face.x(this, .42), face.y(this, .42, 0.0), face.x(this, .58), face.y(this, .58, 0.0), face.x(this, if (open) .95 else .66, far), face.y(this, if (open) .95 else .66, 0.0, far), face.x(this, if (open) .05 else .34, far), face.y(this, if (open) .05 else .34, 0.0, far))
    pen.color = tone(DOOR_STONE, 1.05f * light)
    pen.polygon(doorPoints, arch(face, s0 - .07, s1 + .07, 0.0, spring))
    pen.color = tone(DOOR_STONE, .6f * light)
    for (i in 0 until VOUSSOIRS) {
        val a = PI - PI * (i + .5) / VOUSSOIRS
        pen.line(face.x(this, .5 + radius * cos(a)), face.y(this, .5 + radius * cos(a), spring + radius * sin(a)), face.x(this, .5 + (radius + .07) * cos(a)), face.y(this, .5 + (radius + .07) * cos(a), spring + (radius + .07) * sin(a)), 1f)
    }
    val hole = arch(face, s0, s1, 0.0, spring)
    if (!open) closedLeaves(face, hole, s0, s1, height, flicker, light) else openLeaves(face, hole, s0, s1, height, light)
    pen.color = tone(DOOR_STONE_HI, light)
    val top = spring + radius
    pen.quad(face.x(this, .44), face.y(this, .44, top + .06), face.x(this, .56), face.y(this, .56, top + .06), face.x(this, .54), face.y(this, .54, top - .04), face.x(this, .46), face.y(this, .46, top - .04))
}

/** Закрытые створки: доски, кованые полосы, петли, кольца у шва и свет из щели. */
private fun ScenePainter.closedLeaves(face: WallFace, hole: Int, s0: Double, s1: Double, height: Double, flicker: Float, light: Float) {
    pen.color = tone(DOOR_WOOD, light)
    pen.polygon(doorPoints, hole)
    val bands = doubleArrayOf(height * .22, height * .56)
    pen.clipped(doorPoints, hole) {
        pen.color = tone(DOOR_WOOD_DARK, light)
        for (i in 1 until PLANKS) {
            val s = s0 + (s1 - s0) * i / PLANKS
            pen.line(face.x(this, s), face.y(this, s, 0.0), face.x(this, s), face.y(this, s, height + .1), maxOf(1f, unit * .018f))
        }
        bands.forEach { t ->
            pen.color = tone(DOOR_METAL, light)
            pen.line(face.x(this, s0), face.y(this, s0, t), face.x(this, s1), face.y(this, s1, t), unit * .06f)
        }
    }
    pen.color = tone(DOOR_METAL_HI, .9f * light)
    bands.forEach { t ->
        pen.circle(face.x(this, s0 + .02), face.y(this, s0 + .02, t), unit * .045f)
        pen.circle(face.x(this, s1 - .02), face.y(this, s1 - .02, t), unit * .045f)
    }
    pen.color = tone(DOOR_METAL_HI, light)
    val ring = unit * .1f
    for (s in doubleArrayOf(.44, .56)) pen.ring(face.x(this, s) - ring / 2, face.y(this, s, height * .4) - ring / 2, ring, ring, maxOf(1f, unit * .02f))
    pen.color = Palettes.torch.copy(alpha = .25f * flicker * light)
    pen.line(face.x(this, .5), face.y(this, .5, .02), face.x(this, .5), face.y(this, .5, height - .02), unit * .12f)
    pen.color = SLIT_LIGHT.copy(alpha = .95f * flicker * light)
    pen.line(face.x(this, .5), face.y(this, .5, .02), face.x(this, .5), face.y(this, .5, height - .02), maxOf(1.2f, unit * .025f))
}

/**
 * Открытый проём: отсвет снизу и створки, распахнутые внутрь от петель.
 *
 * Проём не заливается: вход стал полом, герой и монстры видны в нём.
 */
private fun ScenePainter.openLeaves(face: WallFace, hole: Int, s0: Double, s1: Double, height: Double, light: Float) {
    pen.clipped(doorPoints, hole) {
        pen.color = Palettes.torch.copy(alpha = .2f * light)
        pen.circle(face.x(this, .5), face.y(this, .5, .1), unit * .5f)
        for ((hinge, swing) in listOf(s0 to .22, s1 to -.22)) {
            pen.color = tone(DOOR_WOOD, .75f * light)
            pen.quad(face.x(this, hinge), face.y(this, hinge, 0.0), face.x(this, hinge + swing, -LEAF_DEPTH), face.y(this, hinge + swing, 0.0, -LEAF_DEPTH), face.x(this, hinge + swing, -LEAF_DEPTH), face.y(this, hinge + swing, height, -LEAF_DEPTH), face.x(this, hinge), face.y(this, hinge, height))
            pen.color = tone(DOOR_METAL, .9f * light)
            for (t in doubleArrayOf(height * .22, height * .56)) pen.line(face.x(this, hinge), face.y(this, hinge, t), face.x(this, hinge + swing, -LEAF_DEPTH), face.y(this, hinge + swing, t, -LEAF_DEPTH), unit * .05f)
        }
    }
}

/**
 * Рычаг на стене: железная пластина с прорезью и заклёпками на грани
 * стены за рычагом, рукоять выходит к камере - вверх, с тёплым набалдашником и ореолом, пока ждёт; вниз, когда потянут, и над
 * пластиной зажигается лампа.
 */
private fun ScenePainter.drawWallLever(face: WallFace, pulled: Boolean, light: Float) {
    pen.color = tone(DOOR_METAL, .8f * light)
    pen.quad(face.x(this, .3), face.y(this, .3, .32), face.x(this, .7), face.y(this, .7, .32), face.x(this, .7), face.y(this, .7, .95), face.x(this, .3), face.y(this, .3, .95))
    pen.color = tone(DOOR_METAL_HI, light)
    for ((s, t) in listOf(.34 to .36, .66 to .36, .34 to .91, .66 to .91)) pen.circle(face.x(this, s), face.y(this, s, t), unit * .022f)
    pen.color = Color(0xFF0C0806).copy(alpha = light)
    pen.line(face.x(this, .5), face.y(this, .5, .4), face.x(this, .5), face.y(this, .5, .86), unit * .05f)
    val lampX = face.x(this, .5)
    val lampY = face.y(this, .5, 1.04)
    if (pulled) {
        pen.color = LEVER_LAMP.copy(alpha = .3f * light)
        pen.circle(lampX, lampY, unit * .2f)
    }
    pen.color = if (pulled) LEVER_LAMP.copy(alpha = light) else tone(Color(0xFF2A2A2A), light)
    pen.circle(lampX, lampY, unit * .05f)
    val pivotX = face.x(this, .5)
    val pivotY = face.y(this, .5, .62)
    val tipX = face.x(this, .5, if (pulled) .3 else .24)
    val tipY = face.y(this, .5, if (pulled) .32 else 1.0, if (pulled) .3 else .24)
    pen.color = tone(DOOR_METAL, .6f * light)
    pen.circle(pivotX, pivotY, unit * .06f)
    pen.color = tone(Color(0xFF6B4A2A), 1.1f * light)
    pen.line(pivotX, pivotY, tipX, tipY, unit * .065f)
    if (!pulled) {
        pen.color = Palettes.torch.copy(alpha = (.2f + .15f * sin(decor * 2.4f)) * light)
        pen.circle(tipX, tipY, unit * .35f)
    }
    pen.color = if (pulled) tone(Color(0xFF7A6A4A), light) else Color(0xFFE0A64A).copy(alpha = light)
    pen.circle(tipX, tipY, unit * .085f)
}

/** Дверь не выше стольких клеток и ниже стены на столько. */
private const val DOOR_MAX = 1.12
private const val DOOR_HEADROOM = .2

/** Створки распахнуты вглубь комнаты на столько клеток. */
private const val LEAF_DEPTH = .62

/** Точек на полукруг арки, клиньев арки и досок створок. */
private const val ARCH_STEPS = 12
private const val VOUSSOIRS = 5
private const val PLANKS = 6

private val DOOR_STONE = Color(0xFF5A544C)
private val DOOR_STONE_HI = Color(0xFF7A7268)
private val DOOR_WOOD = Color(0xFF5A3A20)
private val DOOR_WOOD_DARK = Color(0xFF2E1C0E)
private val DOOR_METAL = Color(0xFF5A5B62)
private val DOOR_METAL_HI = Color(0xFF8A8C94)
private val SLIT_LIGHT = Color(0xFFFFE2A8)
private val LEVER_LAMP = Color(0xFF7BE0A6)

/**
 * Рычаг: каменный постамент с шестернёй и рукоять с латунным набалдашником;
 * невзведённый - с тёплым ореолом-подсказкой, потянутый - рукоять откинута, набалдашник погас.
 */
internal fun ScenePainter.drawLever(x: Double, y: Double, pulled: Boolean, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    if (!pulled) {
        pen.color = Palettes.torch.copy(alpha = (.18f + .12f * sin(time * 2.4f)) * light)
        pen.ellipse(cx - unit * .55f, cy - unit * .2f, unit * 1.1f, unit * .44f)
    }
    pen.color = tone(Color(0xFF4A4C52), light)
    pen.rect(cx - unit * .26f, cy - unit * .02f, unit * .52f, unit * .3f)
    pen.color = tone(Color(0xFF62656C), light)
    pen.rect(cx - unit * .26f, cy + unit * .26f, unit * .52f, unit * .08f)
    val hub = cy + unit * .16f
    pen.color = tone(Color(0xFF3A3C42), light)
    pen.circle(cx, hub, unit * .13f)
    pen.color = tone(Color(0xFF8A6A3A), light)
    pen.ring(cx - unit * .13f, hub - unit * .13f, unit * .26f, unit * .26f, unit * .035f)
    val (tipX, tipY) = if (pulled) cx + unit * .42f to hub + unit * .22f else cx - unit * .3f to hub + unit * .62f
    pen.color = tone(Color(0xFF6B4A2A), light)
    pen.line(cx, hub, tipX, tipY, unit * .08f)
    pen.color = if (pulled) tone(Color(0xFF7A6A4A), light) else Color(0xFFE0A64A).copy(alpha = light)
    pen.circle(tipX, tipY, unit * .1f)
}

/**
 * Значок материала над узлом: спрайт того, что даст узел, в тёмном кольце цвета ступени
 * материала; парит над узлом и чуть покачивается (стоит без анимаций). Собранный узел значка не несёт.
 */
internal fun ScenePainter.drawMaterial(x: Double, y: Double, mark: MaterialMark, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y) + unit * (1.05f + if (motion) .05f * sin(time * 2f + x.toFloat()) else 0f)
    val r = unit * .3f
    pen.color = Color(0xE60A0D12).copy(alpha = .9f * light)
    pen.circle(cx, cy, r)
    pen.color = mark.tier.copy(alpha = light)
    pen.ring(cx - r, cy - r, r * 2, r * 2, unit * .045f)
    val side = r * 1.3f
    pen.scope.translate(cx - side / 2, -cy - side / 2) {
        with(mark.icon) { draw(Size(side, side), alpha = light, colorFilter = ColorFilter.tint(mark.tier)) }
    }
}

/** Значок материала узла: рисунок и цвет ступени. */
internal class MaterialMark(val icon: Painter, val tier: Color)

/**
 * Цвет ступени материала [code]: по месту работы-источника в лестнице её профессии - нижняя треть пепельная, середина
 * ледяная, верх золотой; материал без источника - золотой.
 */
internal fun materialTier(index: ContentIndex, code: String): Color {
    val source = index.professions.professions.firstOrNull { p -> p.jobs.any { it is Job.Item && it.output == code } } ?: return GoldBright
    val ladder = source.jobs.filterIsInstance<Job.Item>().sortedBy { it.level }
    val share = ladder.indexOfFirst { it.output == code }.toFloat() / (ladder.size - 1).coerceAtLeast(1)
    return when {
        share < 1f / 3 -> Parchment
        share < 2f / 3 -> Rune
        else -> GoldBright
    }
}
