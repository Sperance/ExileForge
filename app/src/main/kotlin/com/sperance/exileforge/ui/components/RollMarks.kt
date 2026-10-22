package com.sperance.exileforge.ui.components

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.LineScale
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Quiet
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.Vital
import com.sperance.exileforge.ui.theme.glow

/**
 * Ступень качества роллов шагом в двадцать: цвет дуги метки. Ступени различаются длиной дуги и цифрой, цвет лишь
 * подсказывает; идеальная сотня - расплавленное золото (не радуга: она выбивалась бы из тёмного фэнтези).
 */
private enum class RollTier(val key: String, val tint: Color) {
    POOR("card.roll_poor", Muted),
    MODEST("card.roll_modest", Parchment),
    FAIR("card.roll_fair", Rune),
    GOOD("card.roll_good", Vital),
    SUPERB("card.roll_superb", GoldBright),
    PERFECT("card.roll_perfect", GoldBright),
    ;

    companion object {
        fun of(quality: Int): RollTier = if (quality >= 100) PERFECT else entries[(quality / 20).coerceIn(0, SUPERB.ordinal)]
    }
}

/** Золото жара идеального ролла: цвет его цифры и ореола. */
private val MoltenGold = Color(0xFFF2D27A)

/** Белый жар посередине течения. */
private val MoltenHeat = Color(0xFFFFF1C2)

/** Расплавленное золото дуги идеального ролла: тёмная бронза, золото и белый жар посередине. */
private val Molten = listOf(Color(0xFF8A5A12), MoltenGold, MoltenHeat, MoltenGold, Color(0xFF8A5A12))

/** Жар цифры идеального ролла: без тёмной бронзы, чтобы мелкая цифра не гасла. */
private val MoltenFigure = listOf(MoltenGold, MoltenHeat, MoltenGold, Color(0xFFC99A3E))

/**
 * Течение жара идеального ролла: доля круга [MOLTEN_MS] состоянием - читается в рисовании, так что ролл
 * перерисовывается шагом часов декора без перекомпоновки; без анимаций (`LocalMotion`) - стоит на середине.
 */
@Composable private fun moltenPhase(): State<Float> = motionPhase(MOLTEN_MS, still = MOLTEN_STILL)

/**
 * Свет жара цветами [colors]: один шейдер на элемент, собирается в кэше рисования; течение сдвигает его матрицу
 * ([at]), так что кадр не создаёт ни кисти, ни шейдера.
 */
private class MoltenFlow(colors: List<Color>) {
    private val shader = LinearGradientShader(Offset(-MOLTEN_SPAN, 0f), Offset(0f, MOLTEN_SPAN / 3), colors, tileMode = TileMode.Mirror)
    private val shift = Matrix()
    private val brush = ShaderBrush(shader)

    /** Кисть на доле течения [phase]: жар медленно течёт по золоту. */
    fun at(phase: Float): Brush {
        shift.setTranslate(phase * MOLTEN_SPAN * 2, 0f)
        shader.setLocalMatrix(shift)
        return brush
    }
}

/** Перекрашивает нарисованный текст жаром на фазе [phase]: фаза читается только в рисовании, без перекомпоновки. */
private fun Modifier.moltenText(phase: State<Float>): Modifier = graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithCache {
        val flow = MoltenFlow(MoltenFigure)
        onDrawWithContent {
            drawContent()
            drawRect(flow.at(phase.value), blendMode = BlendMode.SrcIn)
        }
    }

/** Сколько течёт жар по идеальному роллу, мс. */
private const val MOLTEN_MS = 3600

/** Доля течения, на которой жар стоит без анимаций. */
private const val MOLTEN_STILL = .5f

/** Длина полосы градиента жара, px. */
private const val MOLTEN_SPAN = 240f

/** Непрозрачность дуги обычной ступени: плоская метка не спорит с иконкой и именем. */
private const val ARC_ALPHA = .75f

/**
 * Размеры метки: кольцо [ring] со штрихом [stroke] над дорожкой непрозрачности [track], цифра кеглем [figure] через [gap],
 * ореол сотни силой [glow].
 */
private class RollMarkSpec(val ring: Dp, val stroke: Dp, val track: Float, val gap: Dp, val figure: TextUnit, val glow: Dp)

/** Шапка полной карточки. */
private val RingSpec = RollMarkSpec(ring = 26.dp, stroke = 1.5.dp, track = .10f, gap = 6.dp, figure = 12.sp, glow = 8.dp)

/** Строка списка. */
private val PillSpec = RollMarkSpec(ring = 12.dp, stroke = 1.4.dp, track = .12f, gap = 5.dp, figure = 11.sp, glow = 5.dp)

/**
 * Плоская дуга доли [quality] цветом [tint] поверх тусклой дорожки; при [molten] - полное кольцо жаром с ореолом. Штрихи и
 * цвета собираются в кэше рисования, жар читает фазу только в рисовании.
 */
@Composable private fun RollArc(quality: Int, tint: Color, molten: State<Float>?, spec: RollMarkSpec) {
    Spacer(
        Modifier.size(spec.ring)
            .glow(MoltenGold, on = molten != null, radius = spec.glow, shape = CircleShape)
            .drawWithCache {
                val width = spec.stroke.toPx()
                val stroke = Stroke(width, cap = StrokeCap.Round)
                val track = Stroke(width)
                val trackColor = Parchment.copy(alpha = spec.track)
                val arcColor = tint.copy(alpha = ARC_ALPHA)
                val corner = Offset(width / 2, width / 2)
                val arc = Size(size.width - width, size.height - width)
                val sweep = 360f * quality.coerceIn(0, 100) / 100f
                val flow = molten?.let { MoltenFlow(Molten) }
                onDrawBehind {
                    drawArc(trackColor, -90f, 360f, false, corner, arc, style = track)
                    when {
                        molten != null && flow != null -> drawArc(flow.at(molten.value), -90f, 360f, false, corner, arc, style = stroke)
                        sweep > 0f -> drawArc(arcColor, -90f, sweep, false, corner, arc, style = stroke)
                    }
                }
            },
    )
}

/** Метка качества роллов: кольцо-значок и «N%» рядом тихим цветом; у сотни кольцо и цифра в жару. */
@Composable private fun RollMark(quality: Int, tier: RollTier, spec: RollMarkSpec, modifier: Modifier = Modifier) {
    val molten = if (tier == RollTier.PERFECT) moltenPhase() else null
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(spec.gap), verticalAlignment = Alignment.CenterVertically) {
        RollArc(quality, tier.tint, molten, spec)
        Text(
            ui("row.rolls", quality),
            color = if (molten != null) MoltenGold else Quiet,
            fontSize = spec.figure,
            fontWeight = if (molten != null) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium,
            modifier = if (molten != null) Modifier.moltenText(molten) else Modifier,
        )
    }
}

/** Подсказка и описание метки: «Модификаторы предмета · Хорошие · 72%». */
private fun rollLabel(quality: Int, tier: RollTier): String = ui("card.roll_quality", ui(tier.key), quality)

/** Качество роллов в шапке карточки: кольцо 26dp с дугой до доли и «N%» рядом; касание - «Модификаторы предмета · Хорошие · 72%». */
@Composable fun RollRing(quality: Int, modifier: Modifier = Modifier) {
    val tier = RollTier.of(quality)
    Tipped({ Tip(rollLabel(quality, tier), tint = tier.tint) }, modifier) {
        RollMark(quality, tier, RingSpec)
    }
}

/** Качество роллов в строке списка: значок-кольцо и «N%»; чтение экрана слышит «Модификаторы предмета · ступень · N%». */
@Composable fun RollPill(quality: Int) {
    val tier = RollTier.of(quality)
    val label = rollLabel(quality, tier)
    RollMark(quality, tier, PillSpec, Modifier.clearAndSetSemantics { contentDescription = label })
}

/**
 * Число строки уникалки на шкале своей вилки: края вилки числами, тусклая дорожка, ромб на месте ролла цветом его ступени и «N%» справа;
 * чтение экрана слышит «вилка a–b · N%». Касание строки над шкалой открывает её разбор, как у всех строк.
 */
@Composable fun RollScale(scale: LineScale, modifier: Modifier = Modifier) {
    val tier = RollTier.of(scale.percent)
    val label = ui("card.roll_scale", scale.low, scale.high, scale.percent)
    Row(
        modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(scale.low, color = Quiet, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Canvas(Modifier.weight(1f).height(SCALE_HEIGHT)) {
            val mid = size.height / 2
            drawLine(Parchment.copy(alpha = SCALE_TRACK), Offset(0f, mid), Offset(size.width, mid), strokeWidth = 2.dp.toPx())
            val r = SCALE_MARK.toPx() / 2
            val x = r + (size.width - 2 * r) * scale.share.toFloat()
            val diamond = Path().apply {
                moveTo(x, mid - r)
                lineTo(x + r, mid)
                lineTo(x, mid + r)
                lineTo(x - r, mid)
                close()
            }
            drawPath(diamond, tier.tint)
        }
        Text(scale.high, color = Quiet, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Text(ui("row.rolls", scale.percent), color = tier.tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/**
 * Качество строк уникалки над ними: «Качество строк» и «N% · ступень» её цветом, справа ⓘ - уровень вещи не влияет на строки уникального
 * предмета (вместо серой сноски).
 */
@Composable fun UniqueQuality(quality: Int, modifier: Modifier = Modifier) {
    val tier = RollTier.of(quality)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ui("card.unique_quality"), color = Quiet, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text(ui("card.unique_quality_value", quality, ui(tier.key)), color = tier.tint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
        InfoMark(ui("card.unique_level_label"), { Tip(body = ui("card.unique_level_note")) })
    }
}

/** Высота шкалы вилки. */
private val SCALE_HEIGHT = 14.dp

/** Размер ромба места ролла на шкале. */
private val SCALE_MARK = 10.dp

/** Прозрачность дорожки шкалы. */
private const val SCALE_TRACK = .18f
