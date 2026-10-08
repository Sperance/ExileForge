package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Живые портреты Скверны (4.0.0): поверх статичного портрета сервера (scripts/gen_blight_portraits.py) - слои по утверждённым
 * макетам (RULES.md, «Скверна»). Матерь: сердце бьётся двойным ударом (1,4 с), желчные огни глаз мерцают ступенью (3,2 с),
 * фигура дышит (5,5 с), четыре пуповины качаются и гонят свет к сердцу. Пожиратель очагов: пасть дышит, четыре глаза моргают.
 * Желчный пророк: шар желчи пульсирует, ядовитые искры всплывают. Гнилостайник: глаза мерцают, по когтям бегут блики.
 * Слои рисуются в поле 300 на 400; при выключенных анимациях вызывающий держит [time] неподвижным, и портрет стоит в покое.
 */
internal object BlightPortraits : FieldPortraitMotion() {
    const val MOTHER = "BLIGHT_MOTHER"
    private const val DEVOURER = "BLIGHT_DEVOURER"
    private const val PROPHET = "BLIGHT_PROPHET"
    private const val ROTSTALKER = "BLIGHT_ROTSTALKER"

    /** Пуповин у Матери: по одной рвётся на каждой четверти здоровья. */
    const val CORDS = 4

    private val flesh = Color(0xFF5A0E2A)
    private val vein = Color(0xFFD81B60)
    private val hot = Color(0xFFFF5C9A)
    private val bile = Color(0xFFE8D27A)
    private val bone = Color(0xFFE8D7C0)
    private val pit = Color(0xFF050203)
    private val dark = Color(0xFF2A0814)
    private val clot = Color(0xFF3A0616)

    override val layers: Map<String, DrawScope.(Float, Int) -> Unit> = mapOf(
        MOTHER to { t, torn -> mother(t, torn) },
        DEVOURER to { t, _ -> devourer(t) },
        PROPHET to { t, _ -> prophet(t) },
        ROTSTALKER to { t, _ -> rotstalker(t) },
    )

    override fun breath(code: String, time: Float): Float = when (code) {
        MOTHER, DEVOURER -> portraitWave(time, BREATH, -.25f)
        else -> 0f
    }

    /**
     * Слои Матери Скверны в рамке [scope] (поверх её портрета, со сдвигом [lift]): [torn] пуповин из [CORDS] уже порвано
     * (0..4, по одной на каждые 25% потерянного здоровья; приходит из боя), они висят обрывками, тусклые и без света;
     * остальные живы. По умолчанию 0 - все целы. В арене то же даёт `Portraits.monster(..., phase = torn)`.
     */
    fun mother(scope: DrawScope, time: Float, lift: Float, torn: Int = 0) = draw(scope, MOTHER, time, lift, torn)

    private fun wave(t: Float, period: Float, phase: Float = 0f) = portraitWave(t, period, phase)

    private fun DrawScope.glow(center: Offset, radius: Float, color: Color, alpha: Float) = portraitGlow(center, radius, color, alpha)

    /** Точка квадратичной кривой [p0] - [c] - [p2] при параметре [s]. */
    private fun quad(p0: Offset, c: Offset, p2: Offset, s: Float): Offset {
        val r = 1 - s
        return p0 * (r * r) + c * (2 * r * s) + p2 * (s * s)
    }

    /** Поворот [p] вокруг [pivot] на [degrees]. */
    private fun turn(p: Offset, pivot: Offset, degrees: Float): Offset {
        val a = degrees * PI.toFloat() / 180
        val d = p - pivot
        return pivot + Offset(d.x * cos(a) - d.y * sin(a), d.x * sin(a) + d.y * cos(a))
    }

    /** Сужающийся отросток вдоль кривой [p0] - [c] - [p2]: толщина [w0] у корня и [w1] у конца (как taper_q генератора). */
    private fun taper(p0: Offset, c: Offset, p2: Offset, w0: Float, w1: Float): Path {
        fun normal(a: Offset, b: Offset): Offset {
            val l = hypot(b.x - a.x, b.y - a.y).takeIf { it > 0f } ?: 1f
            return Offset(-(b.y - a.y) / l, (b.x - a.x) / l)
        }
        val n0 = normal(p0, c)
        val n2 = normal(c, p2)
        val nc = (n0 + n2) / 2f
        val wc = (w0 + w1) / 2
        return Path().apply {
            moveTo(p0.x + n0.x * w0 / 2, p0.y + n0.y * w0 / 2)
            quadraticTo(c.x + nc.x * wc / 2, c.y + nc.y * wc / 2, p2.x + n2.x * w1 / 2, p2.y + n2.y * w1 / 2)
            lineTo(p2.x - n2.x * w1 / 2, p2.y - n2.y * w1 / 2)
            quadraticTo(c.x - nc.x * wc / 2, c.y - nc.y * wc / 2, p0.x - n0.x * w0 / 2, p0.y - n0.y * w0 / 2)
            close()
        }
    }

    private fun curve(p0: Offset, c: Offset, p2: Offset) = Path().apply {
        moveTo(p0.x, p0.y)
        quadraticTo(c.x, c.y, p2.x, p2.y)
    }

    /** Хвост кривой [p0] - [c] - [p2] от параметра [s]: начало, вершина изгиба и тот же конец. */
    private fun tail(p0: Offset, c: Offset, p2: Offset, s: Float) = Triple(quad(p0, c, p2, s), c + (p2 - c) * s, p2)

    /** Капля, что срывается с [from] и падает на [fall] за [period] секунд, тая. */
    private fun DrawScope.drip(t: Float, from: Offset, fall: Float, period: Float, phase: Float, color: Color) {
        val u = (t / period + phase) % 1f
        val p = from + Offset(0f, u * u * fall)
        drawCircle(color.copy(alpha = 1 - u), 1.6f + (1 - u), p)
    }

    // ---------------------------------------------------------------------------------- Матерь Скверны

    private const val BREATH = 5.5f
    private const val BEAT = 1.4f
    private const val FLICKER = 3.2f
    private const val SWAY = 4f

    /** Точка макета Матери (квадрат 300) в поле портрета: так её перенёс генератор портретов сервера. */
    private fun motherAt(x: Float, y: Float) = Offset(1.15f * (x - 150) + 150, 1.15f * y + 46)

    /** Пуповины: корень за спиной, вершина изгиба, конец за краем; порядок - порядок разрыва. */
    private val cords = listOf(
        Triple(138f to 140f, 70f to 50f, 0f to 64f),
        Triple(162f to 140f, 230f to 50f, 300f to 64f),
        Triple(140f to 165f, 50f to 210f, -2f to 150f),
        Triple(160f to 165f, 250f to 210f, 302f to 150f),
    ).map { (r, c, t) -> Triple(motherAt(r.first, r.second), motherAt(c.first, c.second), motherAt(t.first, t.second)) }

    /** Где пуповина выходит из-за савана (параметр кривой): ближе к корню её закрывает фигура. */
    private val cordExit = listOf(.3f, .3f, .36f, .36f)

    /** Где рвётся пуповина (параметр кривой). */
    private const val CORD_BREAK = .46f
    private const val CORD_ROOT_W = 17f
    private const val CORD_TIP_W = 6f

    private val heart = motherAt(150f, 168f)
    private val heartSize = Size(16f * 1.15f * 2, 19f * 1.15f * 2)
    private val motherEyes = listOf(motherAt(138f, 82f), motherAt(162f, 82f))

    /** Двойной удар сердца: масштаб по ключам макета (0 - 1, 12% - 1.07, 24% - .98, 36% - 1.04, дальше покой). */
    private fun beat(t: Float): Float {
        val k = (t / BEAT) % 1f
        val keys = floatArrayOf(0f, .12f, .24f, .36f, 1f)
        val values = floatArrayOf(1f, 1.07f, .98f, 1.04f, 1f)
        val i = (1 until keys.size).first { k <= keys[it] }
        val f = (k - keys[i - 1]) / (keys[i] - keys[i - 1])
        val e = f * f * (3 - 2 * f)
        return values[i - 1] + (values[i] - values[i - 1]) * e
    }

    private fun DrawScope.mother(t: Float, torn: Int) {
        val broken = torn.coerceIn(0, CORDS)
        cords.forEachIndexed { i, (root, c, tip) ->
            if (i < broken) tornCord(t, i, root, c, tip) else livingCord(t, i, root, c, tip)
        }
        // сердце: двойной удар, на пике вспышка
        val k = beat(t)
        val surge = ((k - 1f) / .07f).coerceIn(0f, 1f)
        glow(heart, 44f + 10f * surge, hot, .2f + .35f * surge)
        val size = heartSize * k
        val topLeft = heart - Offset(size.width / 2, size.height / 2)
        drawOval(
            Brush.radialGradient(listOf(hot, vein, clot), heart + Offset(-8f, -9f) * k, 25f * k, TileMode.Clamp),
            topLeft,
            size,
        )
        drawPath(curve(heart + Offset(-7f, -12f) * k, heart + Offset(0f, -3f) * k, heart + Offset(-5f, 11f) * k), clot.copy(alpha = .8f), style = Stroke(1.6f))
        drawPath(curve(heart + Offset(7f, -9f) * k, heart + Offset(2f, 2f) * k, heart + Offset(9f, 14f) * k), clot.copy(alpha = .8f), style = Stroke(1.6f))
        // желчные огни: ровный жар и короткий провал каждые 3,2 с
        val dim = (t / FLICKER) % 1f in .72f..<.74f
        motherEyes.forEachIndexed { i, eye ->
            if (dim) {
                drawCircle(pit.copy(alpha = .75f), 6f, eye)
            } else {
                glow(eye, 16f, bile, .22f + .12f * wave(t, 1.1f, i * .3f))
            }
        }
        portraitSparks(t, 12, hot)
    }

    /** Живая пуповина: качается от корня, по жиле к сердцу бежит свет. */
    private fun DrawScope.livingCord(t: Float, i: Int, root: Offset, c: Offset, tip: Offset) {
        val swing = SWAY * sin((t / SWAY + i * .25f) * 2 * PI.toFloat())
        val bend = turn(c, root, swing * .6f)
        val end = turn(tip, root, swing)
        val (from, mid, to) = tail(root, bend, end, cordExit[i])
        val w0 = CORD_ROOT_W + (CORD_TIP_W - CORD_ROOT_W) * cordExit[i]
        val body = taper(from, mid, to, w0, CORD_TIP_W)
        drawPath(curve(from, mid, to), vein.copy(alpha = .12f), style = Stroke(w0 + 8f, cap = StrokeCap.Round))
        drawPath(body, flesh)
        drawPath(body, pit, style = Stroke(1.4f))
        val pulse = wave(t, BEAT, i * .1f)
        drawPath(curve(from, mid, to), vein.copy(alpha = .45f + .35f * pulse), style = Stroke(1.8f, cap = StrokeCap.Round))
        // узлы на пуповине
        listOf(.35f, .7f).forEach { s ->
            val p = quad(from, mid, to, s)
            drawCircle(dark, (w0 + (CORD_TIP_W - w0) * s) * .62f, p)
            drawCircle(vein.copy(alpha = .5f), (w0 + (CORD_TIP_W - w0) * s) * .3f, p)
        }
        // свет течёт от края к сердцу
        repeat(3) { k ->
            val u = (t / 1.2f + k / 3f + i * .17f) % 1f
            val p = quad(from, mid, to, 1 - u)
            val a = sin(u * PI.toFloat())
            glow(p, 7f, hot, .5f * a)
            drawCircle(hot.copy(alpha = .9f * a), 1.6f, p)
        }
    }

    /** Порванная пуповина: культя до разрыва, рваный край и тусклый обрывок, висящий вниз; с края капает. */
    private fun DrawScope.tornCord(t: Float, i: Int, root: Offset, c: Offset, tip: Offset) {
        val (from, _, _) = tail(root, c, tip, cordExit[i])
        val cut = quad(root, c, tip, CORD_BREAK)
        val stubC = quad(root, c, tip, (cordExit[i] + CORD_BREAK) / 2)
        val w0 = CORD_ROOT_W + (CORD_TIP_W - CORD_ROOT_W) * cordExit[i]
        val wCut = CORD_ROOT_W + (CORD_TIP_W - CORD_ROOT_W) * CORD_BREAK
        drawPath(taper(from, stubC, cut, w0, wCut), flesh.copy(alpha = .7f))
        // обрывок висит и чуть покачивается
        val side = if (cut.x < FIELD_W / 2) -1f else 1f
        val sway = sin(t * 1.3f + i) * 4f
        val limpC = cut + Offset(side * 8f, 24f)
        val limpEnd = cut + Offset(side * 3f + sway, 52f)
        val limp = taper(cut, limpC, limpEnd, wCut * .85f, 2.5f)
        drawPath(limp, flesh.copy(alpha = .55f))
        drawPath(limp, vein.copy(alpha = .3f), style = Stroke(1.2f))
        // рана на месте разрыва
        glow(cut, wCut * 1.4f, vein, .35f)
        drawCircle(pit, wCut * .4f, cut)
        drawCircle(hot.copy(alpha = .45f), wCut * .45f, cut, style = Stroke(1.2f))
        drip(t, limpEnd, 26f, 2.2f, i * .37f, vein)
    }

    // ---------------------------------------------------------------------------------- Пожиратель очагов

    /** Точка макета Пожирателя (квадрат 300) в поле портрета. */
    private fun devourerAt(x: Float, y: Float) = Offset(1.2f * (x - 150) + 150, 1.2f * y - 50)

    private val maw = devourerAt(150f, 206f)

    /** Глаза: центр и полуоси века (в поле). */
    private val devourerEyes = listOf(
        Triple(100f to 140f, 12f, 8f),
        Triple(200f to 140f, 12f, 8f),
        Triple(90f to 200f, 10f, 7f),
        Triple(210f to 200f, 10f, 7f),
    ).map { (p, rx, ry) -> Triple(devourerAt(p.first, p.second), rx * 1.2f, ry * 1.2f) }

    private fun DrawScope.devourer(t: Float) {
        // пасть дышит: жар в глотке раздувается и опадает
        val breath = wave(t, 2.6f)
        glow(maw, 40f + 16f * breath, hot, .18f + .22f * breath)
        val rx = (5f + 4f * breath) * 1.2f
        val ry = (30f + 8f * breath) * 1.2f
        drawOval(Brush.radialGradient(listOf(hot.copy(alpha = .55f), vein.copy(alpha = .2f), Color.Transparent), maw, ry), maw - Offset(rx, ry), Size(rx * 2, ry * 2))
        // глаза моргают каждый в свой срок: веко плоти на миг
        devourerEyes.forEachIndexed { i, (eye, rx, ry) ->
            val phase = (t / (3.4f + i * .7f) + i * .31f) % 1f
            if (phase < .07f) {
                val shut = 1 - abs(phase / .035f - 1)
                val h = (ry + 1.5f) * 2 * shut
                drawOval(dark, Offset(eye.x - rx - 1.5f, eye.y - ry - 1.5f), Size((rx + 1.5f) * 2, h))
                drawLine(pit, Offset(eye.x - rx, eye.y - ry - 1.5f + h), Offset(eye.x + rx, eye.y - ry - 1.5f + h), 1.4f)
            } else {
                glow(eye, rx * 2f, bile, .12f + .14f * wave(t, 2.2f, i * .2f))
            }
        }
        drip(t, devourerAt(150f, 276f) + Offset(0f, 30f), 30f, 2.8f, 0f, bile)
        portraitSparks(t, 10, hot)
    }

    // ---------------------------------------------------------------------------------- Желчный пророк

    private val orb = Offset(238f, 96f)
    private val curse = Offset(62f, 122f)
    private val prophetEyes = listOf(Offset(136f, 134f), Offset(164f, 134f))

    private fun DrawScope.prophet(t: Float) {
        // шар желчи пульсирует в клети
        val pulse = wave(t, 1.8f)
        glow(orb, 40f + 14f * pulse, bile, .16f + .24f * pulse)
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .55f * pulse), bile.copy(alpha = .35f), Color.Transparent), orb - Offset(3f, 4f), 14f + 3f * pulse), 14f + 3f * pulse, orb)
        // ядовитые искры всплывают от шара, виляя
        repeat(10) { i ->
            val u = (t * .22f + i / 10f) % 1f
            val x = orb.x + sin(t * 1.7f + i * 2f) * 10f * u + (i % 3 - 1) * 9f * u
            val y = orb.y - 14f - u * 130f
            val a = 4 * u * (1 - u)
            glow(Offset(x, y), 6f, bile, .45f * a)
            drawCircle(bile.copy(alpha = .9f * a), 1.3f, Offset(x, y))
        }
        // печать проклятия над рукой тлеет
        glow(curse, 26f + 6f * wave(t, 2.4f), hot, .12f + .18f * wave(t, 2.4f))
        drawCircle(hot.copy(alpha = .25f + .35f * wave(t, 2.4f)), 20f + 2f * wave(t, 2.4f), curse, style = Stroke(1f))
        prophetEyes.forEachIndexed { i, eye -> glow(eye, 10f, hot, .2f + .25f * wave(t, 1.6f, i * .5f)) }
        portraitSparks(t, 6, vein)
    }

    // ---------------------------------------------------------------------------------- Гнилостайник

    private val stalkerEyes = listOf(
        Offset(136f, 130f) to 3.2f,
        Offset(164f, 130f) to 3.2f,
        Offset(128f, 118f) to 2.4f,
        Offset(172f, 118f) to 2.4f,
        Offset(142f, 110f) to 1.8f,
        Offset(158f, 110f) to 1.8f,
    )

    /** Когти левой руки: корень, изгиб, остриё; правые - зеркало поля. */
    private val claws = listOf(
        Triple(Offset(70f, 318f), Offset(64f, 352f), Offset(96f, 384f)),
        Triple(Offset(78f, 316f), Offset(84f, 346f), Offset(118f, 368f)),
        Triple(Offset(62f, 314f), Offset(44f, 346f), Offset(66f, 390f)),
    )

    private fun mirror(p: Offset) = Offset(FIELD_W - p.x, p.y)

    /** Каждые [GLINT_CYCLE] секунд по когтям руки пробегает блик; правая рука - в противофазе. */
    private const val GLINT_CYCLE = 2.4f
    private const val GLINT_RUN = .5f

    private fun DrawScope.rotstalker(t: Float) {
        // глаза мерцают вразнобой: у каждого свой короткий провал
        stalkerEyes.forEachIndexed { i, (eye, r) ->
            val dim = (t * (.55f + i * .07f) + i * .37f) % 1f < .06f
            if (dim) {
                drawOval(pit.copy(alpha = .8f), eye - Offset(r * 1.5f, r), Size(r * 3f, r * 2f))
            } else {
                glow(eye, r * 4f, hot, .2f + .25f * wave(t, .9f + i * .13f, i * .2f))
            }
        }
        // блики бегут по когтям от корня к острию
        listOf(0f, .5f).forEachIndexed { hand, shift ->
            val k = ((t / GLINT_CYCLE + shift) % 1f) * GLINT_CYCLE / GLINT_RUN
            claws.forEachIndexed { j, (r0, c0, p0) ->
                val (root, c, tip) = if (hand == 0) Triple(r0, c0, p0) else Triple(mirror(r0), mirror(c0), mirror(p0))
                val u = k - j * .12f
                if (u in 0f..1f) {
                    val a = sin(u * PI.toFloat())
                    val p = quad(root, c, tip, u)
                    val q = quad(root, c, tip, (u + .08f).coerceAtMost(1f))
                    glow(p, 8f, bone, .5f * a)
                    drawLine(Color.White.copy(alpha = .9f * a), p, q, 1.4f, StrokeCap.Round)
                }
                if (j < 2) drip(t, tip + Offset(0f, 4f), 18f, 1.6f + j * .3f, hand * .5f + j * .21f, vein)
            }
        }
        portraitSparks(t, 8, vein)
    }
}
