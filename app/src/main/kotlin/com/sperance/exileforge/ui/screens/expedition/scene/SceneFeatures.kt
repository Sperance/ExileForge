package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.campaign.run.AltarSpot
import com.sperance.exileforge.core.campaign.run.FeatureSpot
import com.sperance.exileforge.core.campaign.run.MerchantSpot
import com.sperance.exileforge.core.campaign.run.NodeSpot
import com.sperance.exileforge.core.campaign.run.RoomSpot
import com.sperance.exileforge.core.campaign.run.TrapSpot
import com.sperance.exileforge.rules.run.FeatureKind
import kotlin.math.abs
import kotlin.math.sin

// ==================== Объекты карты (3.90.0, сервер 1.81.3) ====================

/**
 * Объект карты в стиле сцены: каждый вид - свой знак. Глубина сортировки - его клетка; комната рисует трещину или дверь на
 * стене, рычаг и сундук внутри - каждый своей клеткой ([featureParts]).
 */
internal fun ScenePainter.featureParts(spot: FeatureSpot, glow: (Int, Int) -> Float, explored: (Int, Int) -> Boolean): List<Pair<Double, () -> Unit>> = when (spot) {
    is AltarSpot -> listOf(depth(spot.cell.x, spot.cell.y) to { drawAltar(spot.cell.x + .5, spot.cell.y + .5, spot.spent, glow(spot.cell.x, spot.cell.y)) })

    is MerchantSpot -> listOf(depth(spot.cell.x, spot.cell.y) to { drawMerchant(spot.cell.x + .5, spot.cell.y + .5, spot.spent, glow(spot.cell.x, spot.cell.y)) })

    is NodeSpot -> listOf(depth(spot.cell.x, spot.cell.y) to { drawNode(spot.cell.x + .5, spot.cell.y + .5, spot.node.profession, spot.spent, glow(spot.cell.x, spot.cell.y)) })

    is TrapSpot -> listOf(depth(spot.cell.x, spot.cell.y) - .4 to { drawTrap(spot, glow(spot.cell.x, spot.cell.y)) })

    is RoomSpot -> buildList {
        val (ex, ey) = spot.entrance
        if (!spot.opened) add(depth(ex, ey) + .1 to { drawEntrance(ex + .5, ey + .5, spot.kind == FeatureKind.VAULT, spot.progress.toFloat(), glow(ex, ey)) })
        spot.lever?.takeIf { explored(it.x, it.y) }?.let { lever -> add(depth(lever.x, lever.y) to { drawLever(lever.x + .5, lever.y + .5, spot.opened, glow(lever.x, lever.y)) }) }
        spot.chest.takeIf { spot.opened && explored(it.x, it.y) }?.let { chest -> add(depth(chest.x, chest.y) to { drawChest(chest.x + .5, chest.y + .5, spot.looted, glow(chest.x, chest.y)) }) }
    }
}

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

/** Узел ремесла по профессии: рудная глыба, кусты трав или поваленное бревно; собранный - пустой пень и крошка. */
internal fun ScenePainter.drawNode(x: Double, y: Double, profession: String, spent: Boolean, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    val alpha = if (spent) .45f else 1f
    pen.color = Color.Black.copy(alpha = .3f)
    pen.ellipse(cx - unit * .45f, cy - unit * .12f, unit * .9f, unit * .24f)
    when (profession) {
        HERBALISM -> {
            val leaf = tone(Color(0xFF4FA048), light).copy(alpha = alpha)
            for (i in -1..1) {
                pen.color = leaf
                pen.triangle(cx + i * unit * .2f - unit * .08f, cy, cx + i * unit * .2f + unit * .08f, cy, cx + i * unit * .2f, cy + unit * (.35f + .08f * i * i))
            }
            if (!spent) {
                pen.color = Color(0xFFE070C0).copy(alpha = light)
                pen.circle(cx, cy + unit * .38f, unit * .05f)
            }
        }

        WOODCUTTING -> {
            pen.color = tone(Color(0xFF6B4423), light).copy(alpha = alpha)
            pen.quad(cx - unit * .4f, cy + unit * .05f, cx + unit * .3f, cy - unit * .1f, cx + unit * .3f, cy + unit * .12f, cx - unit * .4f, cy + unit * .27f)
            pen.color = tone(Color(0xFFC09060), light).copy(alpha = alpha)
            pen.ellipse(cx + unit * .22f, cy - unit * .1f, unit * .16f, unit * .22f)
        }

        else -> {
            pen.color = tone(Color(0xFF6E6A62), light).copy(alpha = alpha)
            pen.quad(cx - unit * .35f, cy, cx, cy - unit * .12f, cx + unit * .35f, cy, cx, cy + unit * .45f)
            if (!spent) {
                pen.color = Color(0xFFE0B050).copy(alpha = (.6f + .3f * sin(time * 2.5f + x.toFloat())) * light)
                pen.circle(cx - unit * .08f, cy + unit * .15f, unit * .045f)
                pen.circle(cx + unit * .1f, cy + unit * .25f, unit * .035f)
            }
        }
    }
}

/**
 * Ловушка, замеченная светом (3.95.2 - крупнее и опаснее с виду): тёмная плита, знак стихии - шипы стали, решётка огня, лужа
 * яда - и ореол угрозы, что дышит, пока она взведена. Сработавшая вспыхивает, затем тускнеет и разгорается к новому взводу.
 */
internal fun ScenePainter.drawTrap(spot: TrapSpot, light: Float) {
    val x = spot.cell.x + .5
    val y = spot.cell.y + .5
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    val hue = when (spot.trap.element) {
        "FIRE" -> Color(0xFFFF7A30)
        "CHAOS" -> Color(0xFF7CD050)
        else -> Color(0xFFE0E6EE)
    }
    // Доля взвода: 1 - взведена, к нулю - только что сработала
    val charge = when {
        spot.rearm > 0 -> (1 - spot.rearming / spot.rearm).toFloat().coerceIn(0f, 1f)
        spot.armed -> 1f
        else -> 0f
    }
    val pulse = .5f + .5f * sin(time * 3f + x.toFloat())
    if (spot.armed) {
        pen.color = hue.copy(alpha = (.18f + .22f * pulse) * light)
        diamond(cx, cy, unit * (.62f + .06f * pulse), unit * (.31f + .03f * pulse))
    }
    pen.color = Color(0xFF15120F).copy(alpha = .75f * light)
    diamond(cx, cy, unit * .5f, unit * .25f)
    pen.color = Color(0xFF6A2A20).copy(alpha = (.35f + .4f * charge) * light)
    pen.ring(cx - unit * .5f, cy - unit * .25f, unit, unit * .5f, unit * .03f)
    val alpha = (.3f + .65f * charge) * light
    pen.color = hue.copy(alpha = alpha)
    if (spot.trap.element == "PHYSICAL") {
        for (i in -2..2) {
            val at = cx + i * unit * .14f
            val tall = unit * (.3f - abs(i) * .04f) * (.4f + .6f * charge)
            pen.triangle(at - unit * .055f, cy, at + unit * .055f, cy, at, cy + tall)
        }
    } else {
        pen.ring(cx - unit * .36f, cy - unit * .13f, unit * .72f, unit * .26f, unit * .05f)
        pen.color = hue.copy(alpha = (.2f + .3f * pulse) * charge * light)
        pen.circle(cx, cy + unit * .12f, unit * .3f)
    }
    // Вспышка удара: первые доли секунды после срабатывания
    val since = if (spot.rearm > 0) spot.rearm - spot.rearming else Double.MAX_VALUE
    if (since < FLASH_SECONDS) {
        val fade = (1 - since / FLASH_SECONDS).toFloat()
        pen.color = hue.copy(alpha = .55f * fade * light)
        pen.circle(cx, cy + unit * .2f, unit * (.4f + .5f * (1 - fade)))
    }
}

/** Сколько секунд горит вспышка сработавшей ловушки. */
private const val FLASH_SECONDS = .45

/** Вход в комнату на стене: тайная - светлая трещина, что ярче, пока герой стоит рядом; запертая - окованная дверь. */
internal fun ScenePainter.drawEntrance(x: Double, y: Double, door: Boolean, progress: Float, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    if (door) {
        pen.color = tone(Color(0xFF5A3A20), light)
        pen.rect(cx - unit * .25f, cy + unit * .1f, unit * .5f, unit * .8f)
        pen.color = tone(Color(0xFF3A3A40), light)
        pen.line(cx - unit * .25f, cy + unit * .5f, cx + unit * .25f, cy + unit * .5f, unit * .05f)
        pen.circle(cx + unit * .15f, cy + unit * .45f, unit * .05f)
        return
    }
    pen.color = Color(0xFFE8E0C8).copy(alpha = (.35f + .55f * progress) * light)
    pen.polyline(cx - unit * .05f, cy + unit * .95f, cx + unit * .08f, cy + unit * .7f, cx - unit * .06f, cy + unit * .5f, cx + unit * .05f, cy + unit * .25f, width = unit * .04f)
}

/** Рычаг: столб и рукоять, опущенная, когда дверь открыта. */
internal fun ScenePainter.drawLever(x: Double, y: Double, pulled: Boolean, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    pen.color = tone(Color(0xFF3A3A40), light)
    pen.rect(cx - unit * .08f, cy, unit * .16f, unit * .3f)
    pen.color = tone(Color(0xFF8A6A3A), light)
    if (pulled) pen.line(cx, cy + unit * .25f, cx + unit * .35f, cy + unit * .1f, unit * .05f) else pen.line(cx, cy + unit * .25f, cx - unit * .2f, cy + unit * .6f, unit * .05f)
    pen.color = Palettes.torch.copy(alpha = (if (pulled) .3f else .8f) * light)
    pen.circle(if (pulled) cx + unit * .35f else cx - unit * .2f, if (pulled) cy + unit * .1f else cy + unit * .6f, unit * .06f)
}

private const val HERBALISM = "HERBALISM"
private const val WOODCUTTING = "WOODCUTTING"
