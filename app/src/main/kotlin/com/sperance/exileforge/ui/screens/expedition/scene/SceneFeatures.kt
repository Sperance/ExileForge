package com.sperance.exileforge.ui.screens.expedition.scene

import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.rules.content.Job
import com.sperance.exileforge.rules.content.ContentIndex
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.campaign.run.AltarSpot
import com.sperance.exileforge.core.campaign.run.BlightSpot
import com.sperance.exileforge.core.campaign.run.ExpeditionWorld
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
internal fun ScenePainter.featureParts(spot: FeatureSpot, world: ExpeditionWorld, glow: (Int, Int) -> Float, explored: (Int, Int) -> Boolean): List<Pair<Double, () -> Unit>> = when (spot) {
    is AltarSpot -> listOf(depth(spot.cell.x, spot.cell.y) to { drawAltar(spot.cell.x + .5, spot.cell.y + .5, spot.spent, glow(spot.cell.x, spot.cell.y)) })

    is MerchantSpot -> listOf(depth(spot.cell.x, spot.cell.y) to { drawMerchant(spot.cell.x + .5, spot.cell.y + .5, spot.spent, glow(spot.cell.x, spot.cell.y)) })

    is NodeSpot -> listOf(
        depth(spot.cell.x, spot.cell.y) to {
            drawNode(spot.cell.x + .5, spot.cell.y + .5, spot.node.profession, spot.spent, glow(spot.cell.x, spot.cell.y))
            if (!spot.spent) materials[spot.node.job.output]?.let { drawMaterial(spot.cell.x + .5, spot.cell.y + .5, it, glow(spot.cell.x, spot.cell.y)) }
        },
    )

    is TrapSpot -> listOf(depth(spot.cell.x, spot.cell.y) - .4 to { drawTrap(spot, glow(spot.cell.x, spot.cell.y)) })

    is RoomSpot -> buildList {
        val (ex, ey) = spot.entrance
        if (!spot.opened) add(depth(ex, ey) + .1 to { drawEntrance(ex + .5, ey + .5, spot.kind == FeatureKind.VAULT, spot.progress.toFloat(), glow(ex, ey)) })
        spot.lever?.takeIf { explored(it.x, it.y) }?.let { lever -> add(depth(lever.x, lever.y) to { drawLever(lever.x + .5, lever.y + .5, spot.opened, glow(lever.x, lever.y)) }) }
        spot.chest.takeIf { spot.opened && explored(it.x, it.y) }?.let { chest -> add(depth(chest.x, chest.y) to { drawChest(chest.x + .5, chest.y + .5, spot.looted, glow(chest.x, chest.y)) }) }
    }

    is BlightSpot -> blightParts(spot, world, glow)
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

/**
 * Вход в комнату на стене: тайная - светлая трещина, что ярче, пока герой стоит рядом; запертая (4.6.6, макет «Рычаги и
 * двери», вариант A) - арка в камне, створка в кованых полосах и замок-ромб, что тлеет, пока дверь заперта.
 */
internal fun ScenePainter.drawEntrance(x: Double, y: Double, door: Boolean, progress: Float, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    if (door) return drawDoor(cx, cy, light)
    pen.color = Color(0xFFE8E0C8).copy(alpha = (.35f + .55f * progress) * light)
    pen.polyline(cx - unit * .05f, cy + unit * .95f, cx + unit * .08f, cy + unit * .7f, cx - unit * .06f, cy + unit * .5f, cx + unit * .05f, cy + unit * .25f, width = unit * .04f)
}

/** Запертая дверь: каменный проём с аркой, тёмное дерево, две кованые полосы, шов створок и тлеющий замок. */
private fun ScenePainter.drawDoor(cx: Float, cy: Float, light: Float) {
    val half = unit * .32f
    val top = cy + unit * 1.05f
    // Проём: камень вокруг арки
    pen.color = tone(Color(0xFF4A4540), light)
    pen.rect(cx - half - unit * .07f, cy + unit * .05f, (half + unit * .07f) * 2, unit * .82f)
    pen.circle(cx, cy + unit * .87f, half + unit * .07f)
    // Створка: тёмное дерево до арки
    pen.color = tone(Color(0xFF4A2E18), light)
    pen.rect(cx - half, cy + unit * .08f, half * 2, unit * .79f)
    pen.circle(cx, cy + unit * .87f, half)
    // Кованые полосы и шов
    pen.color = tone(Color(0xFF5A5B62), light)
    pen.line(cx - half, cy + unit * .32f, cx + half, cy + unit * .32f, unit * .055f)
    pen.line(cx - half, cy + unit * .66f, cx + half, cy + unit * .66f, unit * .055f)
    pen.color = tone(Color(0xFF2A1A0E), light)
    pen.line(cx, cy + unit * .08f, cx, top - unit * .1f, unit * .025f)
    // Замок: ромб, что тлеет, пока дверь заперта
    val glow = .65f + .35f * sin(time * 2.4f)
    pen.color = Palettes.torch.copy(alpha = .3f * glow * light)
    pen.circle(cx, cy + unit * .5f, unit * .16f)
    pen.color = Color(0xFFF2B45A).copy(alpha = glow * light)
    pen.quad(cx, cy + unit * .6f, cx + unit * .07f, cy + unit * .5f, cx, cy + unit * .4f, cx - unit * .07f, cy + unit * .5f)
}

/**
 * Рычаг (4.6.6, макет «Рычаги и двери», вариант A): каменный постамент с шестернёй и рукоять с латунным набалдашником, вдвое
 * крупнее прежнего; невзведённый - с тёплым ореолом-подсказкой, потянутый - рукоять откинута, набалдашник погас.
 */
internal fun ScenePainter.drawLever(x: Double, y: Double, pulled: Boolean, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    if (!pulled) {
        pen.color = Palettes.torch.copy(alpha = (.18f + .12f * sin(time * 2.4f)) * light)
        pen.ellipse(cx - unit * .55f, cy - unit * .2f, unit * 1.1f, unit * .44f)
    }
    // Постамент
    pen.color = tone(Color(0xFF4A4C52), light)
    pen.rect(cx - unit * .26f, cy - unit * .02f, unit * .52f, unit * .3f)
    pen.color = tone(Color(0xFF62656C), light)
    pen.rect(cx - unit * .26f, cy + unit * .26f, unit * .52f, unit * .08f)
    // Шестерня
    val hub = cy + unit * .16f
    pen.color = tone(Color(0xFF3A3C42), light)
    pen.circle(cx, hub, unit * .13f)
    pen.color = tone(Color(0xFF8A6A3A), light)
    pen.ring(cx - unit * .13f, hub - unit * .13f, unit * .26f, unit * .26f, unit * .035f)
    // Рукоять и набалдашник
    val (tipX, tipY) = if (pulled) cx + unit * .42f to hub + unit * .22f else cx - unit * .3f to hub + unit * .62f
    pen.color = tone(Color(0xFF6B4A2A), light)
    pen.line(cx, hub, tipX, tipY, unit * .08f)
    pen.color = if (pulled) tone(Color(0xFF7A6A4A), light) else Color(0xFFE0A64A).copy(alpha = light)
    pen.circle(tipX, tipY, unit * .1f)
}

/**
 * Значок материала над узлом (4.6.6, макет «Растения», вариант C): спрайт того, что даст узел, в тёмном кольце цвета ступени
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
 * Цвет ступени материала [code] (4.6.6): по месту работы-источника в лестнице её профессии - нижняя треть пепельная, середина
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

private const val HERBALISM = "HERBALISM"
private const val WOODCUTTING = "WOODCUTTING"
