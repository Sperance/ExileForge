package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Живые портреты стражей Разлома (3.96.1): поверх статичного портрета сервера - слои анимации по утверждённому макету
 * (RULES.md, «Стражи Разлома»): печати Стража пульсируют и раскалываются, эхо затягивает в пасть Поглотителя и его глаза
 * моргают, нимбы Владыки вращаются и скрижаль сбоит перед Законом, тень мерцает. Слои рисуются в поле портрета 300 на 400;
 * [time] - секунды; при выключенных анимациях вызывающий держит его неподвижным, и портрет стоит в покое.
 */
internal object RiftPortraits : FieldPortraitMotion() {
    private val rift = Color(0xFF39FF88)
    private val soft = Color(0xFF9DFFB8)
    private val hot = Color(0xFFD4FF6A)
    private val deep = Color(0xFF0B3A21)

    override val layers: Map<String, DrawScope.(Float, Int) -> Unit> = mapOf(
        "RIFT_GATE_WARDEN" to { t, _ -> warden(t) },
        "RIFT_ECHO_DEVOURER" to { t, _ -> devourer(t) },
        "RIFT_LORD" to { t, _ -> lord(t) },
        "RIFT_SHADE" to { t, _ -> shade(t) },
    )

    /** Точка макета (поле 400 в ширину) в поле портрета: так её перенёс генератор портретов сервера. */
    private fun mock(x: Float, y: Float, dy: Float) = Offset(.8f * (x - 200) + 150, .8f * y + dy)

    private fun wave(t: Float, period: Float, phase: Float = 0f) = portraitWave(t, period, phase)

    private fun DrawScope.glow(center: Offset, radius: Float, color: Color, alpha: Float) = portraitGlow(center, radius, color, alpha)

    private fun DrawScope.sparks(t: Float, count: Int, color: Color) = portraitSparks(t, count, color)

    // ---------------------------------------------------------------------------------- Страж Врат

    private const val WARDEN_DY = 56f
    private val seals = listOf(118f to 204f, 282f to 204f, 200f to 262f, 100f to 340f, 300f to 340f).map { (x, y) -> mock(x, y, WARDEN_DY) }
    private val wardenEyes = listOf(mock(180f, 124f, WARDEN_DY), mock(220f, 124f, WARDEN_DY))
    private val portal = mock(200f, 120f, WARDEN_DY)
    private val idols = listOf(mock(40f, 251f, WARDEN_DY), mock(360f, 251f, WARDEN_DY))

    /** Каждые [SEAL_CYCLE] секунд одна печать трескается, разлетается и отрастает. */
    private const val SEAL_CYCLE = 3f

    private fun DrawScope.warden(t: Float) {
        // портал за аркой кружит
        repeat(3) { i ->
            val r = 46f + i * 14f
            drawArc(
                rift.copy(alpha = .22f - i * .05f),
                (t * (30f + i * 14f) * if (i % 2 == 0) 1 else -1) % 360f,
                110f,
                false,
                Offset(portal.x - r, portal.y - r),
                Size(r * 2, r * 2),
                style = Stroke(2.2f),
            )
        }
        // лучи идолов к печатям
        idols.forEachIndexed { side, idol ->
            val target = seals[side]
            val a = .15f + .2f * wave(t, 1.1f, side * .5f)
            drawLine(rift.copy(alpha = a), idol, target, 1.4f)
            glow(idol, 18f, rift, .25f + .25f * wave(t, 1.1f, side * .5f))
        }
        val cycle = floor(t / SEAL_CYCLE).toInt()
        val broken = cycle.mod(seals.size)
        val f = (t % SEAL_CYCLE) / SEAL_CYCLE
        seals.forEachIndexed { i, seal ->
            val pulse = wave(t, 2.4f, i * .2f)
            if (i == broken && f < .55f) {
                // треснувшая печать: вспышка, осколки и пустое место, пока не отрастёт
                val k = f / .55f
                if (k < .3f) glow(seal, 26f, Color.White, (1 - k / .3f) * .8f)
                repeat(6) { s ->
                    val angle = s / 6f * 2 * PI.toFloat() + i
                    val d = 6f + k * 22f
                    drawLine(hot.copy(alpha = (1 - k)), Offset(seal.x + cos(angle) * d, seal.y + sin(angle) * d), Offset(seal.x + cos(angle) * (d + 4), seal.y + sin(angle) * (d + 4)), 1.6f)
                }
            } else {
                val regrow = if (i == broken) ((f - .55f) / .45f).coerceIn(0f, 1f) else 1f
                glow(seal, 16f + 6f * pulse, rift, (.25f + .35f * pulse) * regrow)
                drawCircle(soft.copy(alpha = .35f * regrow), 6f + 2f * pulse, seal, style = Stroke(1.2f))
            }
        }
        // глаза за забралом вспыхивают
        val flicker = .55f + .45f * wave(t, .7f) * wave(t, 2.9f, .3f)
        wardenEyes.forEach { glow(it, 9f, hot, flicker * .7f) }
        sparks(t, 14, soft)
    }

    // ---------------------------------------------------------------------------------- Поглотитель эха

    private const val DEVOURER_DY = -7f
    private val maw = mock(200f, 215f, DEVOURER_DY)
    private val devourerEyes = listOf(
        96f to 120f, 304f to 120f, 70f to 200f, 330f to 200f, 100f to 286f, 300f to 286f, 150f to 62f, 250f to 62f, 200f to 366f,
        118f to 176f, 282f to 176f, 60f to 260f, 340f to 260f,
    ).map { (x, y) -> mock(x, y, DEVOURER_DY) }

    private fun DrawScope.devourer(t: Float) {
        // огоньки эха по спирали в пасть
        repeat(14) { i ->
            val u = (t * .22f + i / 14f) % 1f
            val angle = i * 2.4f + u * 4 * PI.toFloat()
            val r = 150f * (1 - u)
            val p = Offset(maw.x + cos(angle) * r, maw.y + sin(angle) * r * 1.2f)
            val a = 4 * u * (1 - u)
            glow(p, 7f, soft, a * .6f)
            drawCircle(Color.White.copy(alpha = a * .8f), 1.3f, p)
        }
        // ядро дышит
        val pulse = wave(t, 1.8f)
        glow(maw, 30f + 10f * pulse, hot, .25f + .3f * pulse)
        // глаза моргают по очереди: веко - тёмный овал на миг
        devourerEyes.forEachIndexed { i, eye ->
            val phase = (t / (3.5f + i % 4) + i * .37f) % 1f
            if (phase < .06f) {
                val shut = 1 - kotlin.math.abs(phase / .03f - 1)
                drawOval(deep.copy(alpha = .95f), Offset(eye.x - 13f, eye.y - 9f * shut), Size(26f, 18f * shut))
            } else {
                glow(eye, 9f, rift, .15f + .2f * wave(t, 2.2f, i * .13f))
            }
        }
        sparks(t, 10, rift)
    }

    // ---------------------------------------------------------------------------------- Владыка Разлома

    private const val LORD_DY = 45f
    private val halo = mock(200f, 135f, LORD_DY)
    private val lordEyes = listOf(mock(200f, 122f, LORD_DY), mock(200f, 152f, LORD_DY), mock(200f, 182f, LORD_DY))
    private val tabletTop = mock(122f, 270f, LORD_DY)
    private val tabletBottom = mock(278f, 360f, LORD_DY)

    /** Закон держится [LAW_HOLD] секунд, затем [LAW_OMEN] секунд знамения: скрижаль дрожит и сбоит. */
    private const val LAW_HOLD = 5f
    private const val LAW_OMEN = 3f

    private fun DrawScope.lord(t: Float) {
        // нимбы рун кружат в разные стороны
        listOf(105.6f to .12f, 83.2f to -.18f).forEachIndexed { ring, (r, speed) ->
            val n = if (ring == 0) 24 else 16
            repeat(n) { i ->
                val angle = i / n.toFloat() * 2 * PI.toFloat() + t * speed
                val p = Offset(halo.x + cos(angle) * r, halo.y + sin(angle) * r)
                drawCircle((if (ring == 0) rift else hot).copy(alpha = .35f + .25f * wave(t, 3f, i / n.toFloat())), if (ring == 0) 1.6f else 2.2f, p)
            }
        }
        // три глаза разлома горят по очереди
        lordEyes.forEachIndexed { i, eye -> glow(eye, 12f, hot, .25f + .5f * wave(t, 2.1f, i / 3f)) }
        // скрижаль: в знамение дрожит и вспыхивает полосами
        val inCycle = t % (LAW_HOLD + LAW_OMEN)
        val omen = inCycle >= LAW_HOLD
        val width = tabletBottom.x - tabletTop.x
        val height = tabletBottom.y - tabletTop.y
        if (omen) {
            val k = (inCycle - LAW_HOLD) / LAW_OMEN
            val jitter = sin(t * 60f) * 1.5f
            repeat(4) { band ->
                val y = tabletTop.y + ((band * .27f + t * 3.1f) % 1f) * height
                drawRect(rift.copy(alpha = .18f + .25f * k), Offset(tabletTop.x + jitter, y), Size(width, 3f))
            }
            glow(Offset(tabletTop.x + width / 2, tabletTop.y + height / 2), width * .7f, rift, .15f + .3f * wave(t, .25f))
        } else {
            glow(Offset(tabletTop.x + width / 2, tabletTop.y + height / 2), width * .6f, rift, .12f * wave(t, 2.5f))
        }
        sparks(t, 12, soft)
    }

    // ---------------------------------------------------------------------------------- Тень украденного

    private val shadeEyes = listOf(Offset(141f, 128f), Offset(159f, 128f))

    private fun DrawScope.shade(t: Float) {
        val flicker = .5f + .5f * wave(t, .9f) * wave(t, 2.3f, .4f)
        shadeEyes.forEach { glow(it, 8f, hot, .3f + .5f * flicker) }
        // туман клубится по подолу
        repeat(5) { i ->
            val x = 60f + i * 45f + sin(t * .6f + i) * 10f
            glow(Offset(x, 360f + sin(t * .8f + i * 1.3f) * 8f), 40f, rift, .08f + .06f * wave(t, 3f, i * .2f))
        }
        sparks(t, 8, rift)
    }
}
