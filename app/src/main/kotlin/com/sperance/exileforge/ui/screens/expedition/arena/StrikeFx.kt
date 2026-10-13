package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.combat.HitKind
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.combat.StrikeView
import com.sperance.exileforge.core.campaign.progressAt
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// ==================== «Кто кого бьёт» (4.9.3, макет «Стороны», вариант C) ====================
// Сцена удара - доля 0..1 окна [StrikeView.progress]: карта бьющего в выпаде первую треть, чары летят [FLIGHT], у цели - вспышка
// и тряска [IMPACT]. Всё - отклик боя: идёт всегда, без LocalMotion, и без выделений, кроме кистей в миг удара.

/** Доля сцены, что карта бьющего вынесена к цели: выпад правил - треть сцены из трёх выпадов. */
private const val LUNGE = 1f / 3

/** Доля сцены, что летит снаряд чар от бьющего к цели. */
private const val FLIGHT = .5f

/** Доля сцены после попадания, что цель вспыхивает, трясётся и на ней виден разрез. */
private const val IMPACT = .45f

/** С какой доли замаха карта наливается цветом (последние 30%), и с какой - подаётся вперёд. */
private const val GLOW_FROM = .7f
private const val WINDUP_FROM = .9f

/** Кровь удара по герою: разрез, числа, виньетка. */
internal val HeroHurt = Color(0xFFFF5A52)

/** Свечение замаха [swing] (0..1): последние 30% - рамка карты наливается цветом. */
internal fun swingGlow(swing: Float, held: Boolean): Float = if (held) 0f else ((swing - GLOW_FROM) / (1 - GLOW_FROM)).coerceIn(0f, 1f)

/** Удар [strike] - карты [actor] (у врага - [foe]). */
private fun StrikeView.by(actor: Side, foe: Int?): Boolean = this.actor == actor && (foe == null || this.foe == foe)

/** Удар [strike] - по карте [target] (у врага - [foe]). */
private fun StrikeView.at(target: Side, foe: Int?): Boolean = this.target == target && (foe == null || this.foe == foe)

/** Сколько прошло от попадания (0..1 окна вспышки) при доле сцены [p]; null - снаряд ещё летит или вспышка прошла. */
private fun StrikeView.impact(p: Float): Float? {
    val since = if (spell) p - FLIGHT else p
    return if (since < 0f || since >= IMPACT) null else since / IMPACT
}

/**
 * Насколько карта вынесена к цели (0..1): разгон в конце замаха [swing], в миг удара - вся, назад за выпад. Чары карту не
 * выносят - летит снаряд.
 */
internal fun strikeReach(fight: FightHud, now: Double, actor: Side, foe: Int?, swing: Float = 0f, held: Boolean = false): Float {
    var reach = if (held) 0f else ((swing - WINDUP_FROM) / (1 - WINDUP_FROM)).coerceIn(0f, 1f) * .7f
    val strikes = fight.strikes
    for (i in strikes.indices) {
        val strike = strikes[i]
        val p = strike.progressAt(fight, now)
        if (strike.spell || p >= LUNGE || !strike.by(actor, foe)) continue
        reach = max(reach, cos(p / LUNGE * PI / 2).toFloat())
    }
    return reach
}

/** Вспышка карты от попадания (0..1, крит - ярче): белым по портрету, тряска пока она есть. */
internal fun strikeFlash(fight: FightHud, now: Double, target: Side, foe: Int?): Float {
    var flash = 0f
    val strikes = fight.strikes
    for (i in strikes.indices) {
        val strike = strikes[i]
        if (!strike.landed || !strike.at(target, foe)) continue
        val t = strike.impact(strike.progressAt(fight, now)) ?: continue
        flash = max(flash, (1 - t) * (if (strike.kind == HitKind.CRIT) 1f else .6f))
    }
    return flash
}

/** Тряска карты, по которой попали: затухает вместе со вспышкой. */
internal fun shake(flash: Float, time: Float): Float = if (flash > 0f) sin(time * 60f) * 2.5f * flash else 0f

/** Рамка карты наливается [tint] в замахе: мягкое свечение вокруг контура [outline] силой [glow]. */
internal fun DrawScope.swingAura(outline: Outline, tint: Color, glow: Float) {
    if (glow <= 0f) return
    for (step in 3 downTo 1) drawOutline(outline, tint.copy(alpha = .45f * glow / step), style = Stroke((step * 2.5f).dp.toPx()))
}

/** Цвет удара: по герою - кровь, по врагу - стихия; промах - пепел. */
private fun strikeTint(strike: StrikeView): Color = when {
    !strike.landed -> Muted
    strike.target == Side.HERO -> HeroHurt
    else -> damageTint(strike.type)
}

/**
 * Удары поверх боя (4.9.3): атака - вспышка-разрез на цели, чары - снаряд цвета стихии от бьющего к цели и вспышка; все
 * одновременные удары, каждый своим местом на цели. [card] - рамка карты стороны (герой - `null`, враг - его номер).
 */
@Composable internal fun StrikeFx(fight: FightHud, frame: FightFrame, card: (Int?) -> Rect?, origin: Offset) {
    val strikes = fight.strikes
    if (strikes.isEmpty()) return
    Canvas(Modifier.fillMaxSize()) {
        val now = frame.now(fight)
        for (i in strikes.indices) {
            val strike = strikes[i]
            val from = card(if (strike.actor == Side.HERO) null else strike.foe) ?: continue
            val to = card(if (strike.target == Side.HERO) null else strike.foe) ?: continue
            // Одновременные удары по одной цели (тот же миг) - бок о бок, а не друг на друге
            var place = 0
            var together = 0
            for (j in strikes.indices) {
                val other = strikes[j]
                if (other.target != strike.target || other.foe != strike.foe || abs(other.progress - strike.progress) > SAME_MOMENT) continue
                if (j < i) place++
                together++
            }
            val shift = Offset((place - (together - 1) / 2f) * 18.dp.toPx(), 0f)
            val tint = strikeTint(strike)
            val p = strike.progressAt(fight, now)
            if (strike.spell) bolt(strike, p, from.center - origin, to.center - origin + shift, tint) else slash(strike, p, to.center - origin + shift, to.height, tint)
        }
    }
}

/** Удары одного мига: доля сцены, ближе которой удары по одной цели ставятся рядом. */
private const val SAME_MOMENT = .05f

/** Разрез на цели: две косые полосы, вторая короче и бледнее, растут за первую треть вспышки и гаснут. */
private fun DrawScope.slash(strike: StrikeView, p: Float, at: Offset, height: Float, tint: Color) {
    val t = strike.impact(p) ?: return
    val grow = (t / .35f).coerceAtMost(1f)
    val fade = (1 - t) * (if (strike.landed) 1f else .5f)
    val half = height * .3f
    // −35°: из правого верха в левый низ
    val dir = Offset(SLASH_X, SLASH_Y)
    val start = at - dir * half
    val end = start + dir * (2 * half * grow)
    drawLine(tint.copy(alpha = .3f * fade), start, end, 8.dp.toPx(), StrokeCap.Round)
    drawLine(tint.copy(alpha = .95f * fade), start, end, 3.dp.toPx(), StrokeCap.Round)
    drawLine(Color.White.copy(alpha = .7f * fade), start + (end - start) * .35f, start + (end - start) * .65f, 1.5.dp.toPx(), StrokeCap.Round)
    val side = Offset(10.dp.toPx(), 0f)
    drawLine(tint.copy(alpha = .55f * fade), start + side + dir * (half * .25f), start + side + (end - start) * .75f, 2.dp.toPx(), StrokeCap.Round)
}

private val SLASH_X = sin(35 * PI / 180).toFloat()
private val SLASH_Y = cos(35 * PI / 180).toFloat()

/** Снаряд чар: шар с хвостом летит [FLIGHT] сцены от [from] к [to], на цели - расходящаяся вспышка. */
private fun DrawScope.bolt(strike: StrikeView, p: Float, from: Offset, to: Offset, tint: Color) {
    if (p < FLIGHT) {
        val k = p / FLIGHT
        val eased = k * k * (3 - 2 * k)
        val head = from + (to - from) * eased
        val tail = from + (to - from) * (eased - .12f).coerceAtLeast(0f)
        drawLine(tint.copy(alpha = .35f), tail, head, 4.dp.toPx(), StrokeCap.Round)
        drawCircle(tint.copy(alpha = .25f), 10.dp.toPx(), head)
        drawCircle(tint, 5.dp.toPx(), head)
        drawCircle(Color.White.copy(alpha = .9f), 2.dp.toPx(), head)
        drawCircle(tint.copy(alpha = .45f), 3.dp.toPx(), tail)
        return
    }
    val t = strike.impact(p) ?: return
    val radius = (8 + 26 * t) * density
    drawCircle(tint.copy(alpha = .35f * (1 - t)), radius, to)
    drawCircle(tint.copy(alpha = .8f * (1 - t)), radius, to, style = Stroke(2.dp.toPx()))
}

/**
 * Красная виньетка по краю экрана (4.9.3), пока попадание по герою свежо: угроза читается боковым зрением. Отклик боя -
 * всегда, но мягко.
 */
@Composable internal fun HitVignette(fight: FightHud, frame: FightFrame) {
    val strikes = fight.strikes
    if (strikes.none { it.landed && it.damage > 0 && it.at(Side.HERO, null) }) return
    Canvas(Modifier.fillMaxSize()) {
        val now = frame.now(fight)
        var strength = 0f
        for (i in strikes.indices) {
            val strike = strikes[i]
            if (!strike.landed || strike.damage <= 0 || !strike.at(Side.HERO, null)) continue
            val t = strike.impact(strike.progressAt(fight, now)) ?: continue
            strength = max(strength, 1 - t)
        }
        if (strength <= 0f) return@Canvas
        val glow = Brush.radialGradient(
            0f to Color.Transparent,
            .62f to Color.Transparent,
            1f to LifeRed.copy(alpha = .42f * strength),
            center = center,
            radius = size.maxDimension * .72f,
        )
        drawRect(glow)
    }
}

/** Тон сторон арены (4.9.3): верх - красноватый (враги), низ - зелёно-золотой (герой), середина чиста. Кисть одна на приложение. */
private val SideTint = Brush.verticalGradient(
    0f to LifeRed.copy(alpha = .09f),
    .42f to Color.Transparent,
    .58f to Color.Transparent,
    1f to Gold.copy(alpha = .1f),
)

/** Арена поделена тоном сторон (4.9.3): отклик боя, мягкий и всегда. */
internal fun Modifier.sideTint(): Modifier = drawBehind { drawRect(SideTint) }
