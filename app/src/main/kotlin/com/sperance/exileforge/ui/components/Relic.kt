package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.R
import com.sperance.exileforge.core.display.AffixKind
import com.sperance.exileforge.core.display.AffixMarks
import com.sperance.exileforge.core.display.ItemLine
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Надписи «Реликвария» (заголовки, номера, места): Cinzel, начертание задаётся осью веса вариативного шрифта. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val RelicSerif = FontFamily(
    Font(R.font.cinzel, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.cinzel, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** Надпись серифами Cinzel размера [size]: заголовки и номера, не имена предметов. */
fun relicName(size: Int = 23) = TextStyle(fontFamily = RelicSerif, fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = (size * 1.18f).sp, letterSpacing = (size * .02f).sp)

/** Имена предметов: Philosopher Bold с кириллицей - файл шрифта в ресурсах приложения (лицензия SIL OFL 1.1), без сети. */
val ItemNameFont = FontFamily(Font(R.font.philosopher_bold, FontWeight.Bold))

/** Имя предмета размера [size] - карточка, плита, плитка, снаряжение, витрина: Philosopher Bold. */
fun itemName(size: Int = 22) = TextStyle(fontFamily = ItemNameFont, fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = (size * 1.2f).sp, letterSpacing = (size * .01f).sp)

/**
 * Облик карточки по редкости: рамка, фон, свечение и цвета текста. Обычные, волшебные и редкие вещи - басальт в тон редкости;
 * уникальные - тёплый уголь с искрами, мифические - звёздное небо. Подписи [muted] - с контрастом не ниже AA на своей подложке.
 *
 * «Реликварий»: подробная карточка предмета, её строки модификаторов со значком тира, итог героя «сейчас → станет» и «Астролябия»
 * уникальных и мифических вещей.
 */
@Immutable
data class RelicLook(
    val rarity: Color,
    val name: Color,
    val top: Color,
    val bottom: Color,
    val glow: Color,
    val gold: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val legend: Legend?,
) {
    /** Рисунок «Астролябии»: искры уникальной вещи или звёзды мифической. */
    enum class Legend { EMBER, STARS }

    val primary: Brush get() = if (legend != null) Brush.verticalGradient(listOf(gold.lighten(), gold.darken())) else PrimaryBrush
    val onPrimary: Color get() = if (legend != null) Color(0xFF2A1802) else Color(0xFF04210F)
}

private fun Color.lighten() = Color(red + (1 - red) * .35f, green + (1 - green) * .35f, blue + (1 - blue) * .35f, alpha)
private fun Color.darken() = Color(red * .82f, green * .66f, blue * .4f, alpha)

/** Облик карточки вещи редкости [rarity]. */
fun relicLook(rarity: Rarity): RelicLook {
    val color = rarityColor(rarity.name)
    return when (rarity) {
        Rarity.UNIQUE -> RelicLook(
            color, Color(0xFFFFB774), Color(0xFF24150C), Color(0xFF120B07), Color(0xFFF0965A), Color(0xFFF2A65A),
            Color(0xFFC0A894), Color(0xFF6E5A4A), Color(0xFFFFC98A), RelicLook.Legend.EMBER,
        )

        Rarity.MYTHICAL -> RelicLook(
            color, Color(0xFFFFD99A), Color(0xFF2B1A48), Color(0xFF0C0A16), Color(0xFFF0C76A), Color(0xFFF0C76A),
            Color(0xFFB5A9CC), Color(0xFF6A5E80), Color(0xFF8EC5FF), RelicLook.Legend.STARS,
        )

        else -> RelicLook(color, color, DepthTop, DepthBottom, color, color, Quiet, Color(0xFF56636D), Rune, null)
    }
}

/** Имя вещи ([itemName]) размера [size] в цвете редкости; у легенды - с ореолом её свечения. */
fun RelicLook.nameStyle(size: Int): TextStyle = itemName(size).copy(color = name, shadow = halo)

/** Надпись серифами ([relicName]) в облике редкости - места и имена героев доски славы; у легенды - с ореолом. */
fun RelicLook.titleStyle(size: Int): TextStyle = relicName(size).copy(color = name, shadow = halo)

/** Ореол надписи легенды - её свечение; у прочих редкостей ореола нет. */
private val RelicLook.halo: Shadow? get() = if (legend != null) Shadow(glow.copy(alpha = .6f), blurRadius = 18f) else null

/**
 * Подложка вещи в облике редкости - общая для полной карточки и плитки: градиент [RelicLook.top] → [RelicLook.bottom],
 * свечение редкости от верхнего края, завитки по углам у легенды и рамка - нить [RelicLook.gold], у выбранной - золото.
 * Небо легенды ([relicSky]) - только при [sky]: его зажигает полная карточка, в списках подложка без анимации.
 */
@Composable fun Modifier.relicGround(look: RelicLook, shape: Shape, selected: Boolean = false, sky: Boolean = false): Modifier {
    val frame = Brush.verticalGradient(
        if (selected) {
            listOf(GoldBright, GoldBright)
        } else {
            listOf(look.gold.copy(alpha = .6f), look.gold.copy(alpha = .1f), look.gold.copy(alpha = .3f))
        },
    )
    return clip(shape)
        .background(Brush.verticalGradient(listOf(look.top, look.bottom)))
        .drawBehind { drawRect(Brush.radialGradient(listOf(look.glow.copy(alpha = .16f), Color.Transparent), Offset(size.width / 2, 0f), size.width * .8f)) }
        .then(if (sky) Modifier.relicSky(look) else Modifier)
        .then(if (look.legend != null) Modifier.drawBehind { relicCorners(look.gold) } else Modifier)
        .border(if (selected) 2.dp else 1.dp, frame, shape)
}

/**
 * Гнездо иконки размера [size]: у обычной, волшебной и редкой - скруглённый квадрат басальта в нити редкости,
 * у легенды - круг её неба в золотой нити с ореолом, как гнездо «Астролябии».
 */
@Composable fun RelicSocket(item: ItemView, look: RelicLook, size: Dp) {
    val legend = look.legend != null
    val shape = if (legend) CircleShape else RoundedCornerShape(size * .3f)
    Box(
        Modifier.size(size).glow(look.glow.copy(alpha = if (legend) .55f else .25f), radius = 8.dp, shape = shape)
            .background(Brush.radialGradient(if (legend) listOf(look.top, look.bottom) else listOf(Color(0xFF232C2A), Color(0xFF0B1013))), shape)
            .border(if (legend) 1.5.dp else 1.dp, if (legend) look.gold else look.rarity.copy(alpha = .45f), shape),
        contentAlignment = Alignment.Center,
    ) { ItemIcon(item, look.rarity, Modifier.size(size * .66f)) }
}

/** Цвета значков тира: первый тир - янтарь, второй - мята, третий - лёд, ниже - серый. */
private val TierAmber = Color(0xFFF5B547)
private val TierMint = Color(0xFF7BE0A6)
private val TierIce = Color(0xFF8EC5FF)
private val TierAsh = Color(0xFF9FB3C0)
private val UniqueGold = Color(0xFFF0C76A)
private val BenchLilac = Color(0xFFB4B4FF)

/** Заливка значка строки: по тиру для роллов, свой цвет у уникальной, ремесла, порчи и прочих источников. */
fun tierColor(marks: AffixMarks): Color = when (marks.kind) {
    AffixKind.UNIQUE -> UniqueGold

    AffixKind.CRAFTED -> BenchLilac

    AffixKind.IMPLICIT -> TierAsh

    AffixKind.CORRUPTION -> LifeRed

    AffixKind.FRACTURED -> Fractured

    AffixKind.HANDCRAFTED -> Handcrafted

    AffixKind.ALCHEMY -> Vital

    AffixKind.ESSENCE -> Color(0xFFC8A0FF)

    AffixKind.INFLUENCE -> marks.influence?.let(::influenceColor) ?: TierAsh

    AffixKind.PREFIX, AffixKind.SUFFIX, null -> when (marks.tier) {
        1 -> TierAmber
        2 -> TierMint
        3 -> TierIce
        else -> TierAsh
    }
}

/** Цвет текста строки: тёплый у уникальной, сиреневый у ремесла, серый у врождённой, розовый у порчи, голубой у прочих. */
fun modColor(marks: AffixMarks): Color = when (marks.kind) {
    AffixKind.UNIQUE -> Color(0xFFFFDDB0)
    AffixKind.CRAFTED -> Color(0xFFC9C6FF)
    AffixKind.IMPLICIT -> TierAsh
    AffixKind.CORRUPTION -> Color(0xFFE98C86)
    else -> Color(0xFFA9CFFF)
}

/** Шестигранник [size] с вершиной вверх. */
private fun DrawScope.hexPath(): Path {
    val w = size.width
    val h = size.height
    return Path().apply {
        moveTo(w * .5f, h * .075f)
        lineTo(w * .87f, h * .2875f)
        lineTo(w * .87f, h * .7125f)
        lineTo(w * .5f, h * .925f)
        lineTo(w * .13f, h * .7125f)
        lineTo(w * .13f, h * .2875f)
        close()
    }
}

/**
 * Значок строки: шестигранник в цвете тира с его номером, ✦ у уникальной строки, молот у ремесла, пустой контур у врождённой.
 * Долгое нажатие называет вид строки.
 */
@Composable fun TierHex(marks: AffixMarks, size: Dp = 20.dp) {
    val kind = marks.kind
    val fill = tierColor(marks)
    val glyph = when (kind) {
        AffixKind.UNIQUE -> "✦"
        AffixKind.CRAFTED, AffixKind.IMPLICIT -> ""
        AffixKind.PREFIX, AffixKind.SUFFIX, null -> marks.tier.takeIf { it > 0 }?.toString().orEmpty()
        else -> if (marks.tier > 0) "${marks.letter}${marks.tier}" else marks.letter
    }
    val content: @Composable () -> Unit = {
        Box(
            Modifier.size(size).drawBehind {
                val hex = hexPath()
                if (kind == AffixKind.IMPLICIT) {
                    drawPath(hex, fill, style = Stroke(1.5.dp.toPx()))
                } else {
                    drawPath(hex, fill)
                }
                if (kind == AffixKind.CRAFTED) {
                    val ink = Color(0xFF14123A)
                    val s = this.size.width / 20f
                    val stroke = 1.6f * s
                    drawLine(ink, Offset(6.5f * s, 12.5f * s), Offset(10.5f * s, 8.5f * s), stroke, StrokeCap.Round)
                    drawLine(ink, Offset(9f * s, 7.5f * s), Offset(11.5f * s, 10f * s), stroke, StrokeCap.Round)
                    drawLine(ink, Offset(11.8f * s, 6.2f * s), Offset(13.7f * s, 8.1f * s), stroke, StrokeCap.Round)
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            if (glyph.isNotEmpty()) {
                Text(glyph, color = Color(0xFF16100A), fontSize = (if (glyph.length > 1) 7 else 10).sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
    if (kind == null) content() else Tipped({ Tip(ui("mod.kind.${kind.name}"), tint = fill) }) { content() }
}

/**
 * Строка модификатора в карточке: значок тира, текст в цвете вида и справа «ремесло» у строки верстака. Вилку тира
 * карточка не печатает: тир, вилку и место ролла открывает нажатие на строку.
 *
 * Усиленная строка: ▲ у значка, текст - уже итог; ролл и процент - в разборе по нажатию.
 */
@Composable fun RelicModLine(line: ItemLine, onClick: (() -> Unit)?, big: Boolean = true) {
    val tap = if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClickLabel = line.text, onClick = onClick)
    Row(
        Modifier.fillMaxWidth().then(tap).padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TierHex(line.marks, if (big) 20.dp else 16.dp)
        if (line.amplified != null) Text(ui("relic.amplified"), color = Vital, style = if (big) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall)
        Text(
            line.text,
            color = modColor(line.marks),
            style = if (big) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
        )
        if (big && line.marks.kind == AffixKind.CRAFTED) Text(ui("relic.crafted"), color = Color(0xFF56636D), style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** Разделитель карточки: две нити света к ромбу, у легенд - к звезде. */
@Composable fun RelicDivider(look: RelicLook, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(look.gold.copy(alpha = 0f), look.gold.copy(alpha = .5f)))))
        if (look.legend != null) {
            Canvas(Modifier.size(14.dp)) { drawPath(starPath(size.width / 2, center), look.gold) }
        } else {
            Canvas(Modifier.size(7.dp)) { rotate(45f) { drawRect(look.rarity) } }
        }
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(look.gold.copy(alpha = .5f), look.gold.copy(alpha = 0f)))))
    }
}

/** Четырёхлучевая звезда радиуса [r] вокруг [c]. */
private fun starPath(r: Float, c: Offset): Path = Path().apply {
    (0 until 8).forEach { k ->
        val a = PI / 4 * k - PI / 2
        val rr = if (k % 2 == 0) r else r * .3f
        val x = c.x + (rr * cos(a)).toFloat()
        val y = c.y + (rr * sin(a)).toFloat()
        if (k == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** «◆ № 41 из всех миров ◆»: сквозной по всем мирам номер экземпляра уникальной или мифической вещи, между двух нитей золота. */
@Composable fun SerialSeal(serial: Long, look: RelicLook) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(30.dp).height(1.dp).background(Brush.horizontalGradient(listOf(look.gold.copy(alpha = 0f), look.gold))))
        Text(
            "◆ ${ui("relic.serial", serial)} ◆",
            color = look.gold,
            style = TextStyle(fontFamily = RelicSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.4.sp),
        )
        Box(Modifier.width(30.dp).height(1.dp).background(Brush.horizontalGradient(listOf(look.gold, look.gold.copy(alpha = 0f)))))
    }
}

/** Мерцание звёзд и подъём искр легендарной карточки: по одному числу на точку, без перестройки при каждом кадре. */
private val SPARKS = List(26) { i ->
    val r = java.util.Random(7L + i * 31)
    floatArrayOf(r.nextFloat() * .94f + .03f, r.nextFloat() * .96f + .02f, if (r.nextInt(4) == 0) 2f else 1.2f, r.nextFloat())
}

/**
 * Фон легенды: у мифической - мерцающие звёзды, у уникальной - искры, медленно поднимающиеся к имени.
 * Рисуется за содержимым и не трогает раскладку; только в полной карточке ([relicGround] с `sky`).
 *
 * Фаза читается в рисовании: небо перерисовывается без перекомпоновки карточки.
 */
@Composable private fun Modifier.relicSky(look: RelicLook): Modifier {
    val legend = look.legend ?: return this
    val sky = motionPhase(SKY_MS)
    return drawBehind {
        val time = sky.value
        SPARKS.forEach { (x, y, r, phase) ->
            if (legend == RelicLook.Legend.STARS) {
                val glow = .25f + .75f * ((sin(((time * 6 + phase) * 2 * PI)).toFloat() + 1) / 2)
                drawCircle(Color(0xFFFFF0D2).copy(alpha = glow), r.dp.toPx(), Offset(size.width * x, size.height * y))
            } else {
                val life = (time * 5 + phase) % 1f
                val alpha = if (life < .2f) life / .2f else 1f - (life - .2f) / .8f
                val rise = life * size.height * .3f
                drawCircle(Color(0xFFFFB061).copy(alpha = alpha * .9f), (r + .4f).dp.toPx(), Offset(size.width * x, size.height * (.45f + y * .5f) - rise))
            }
        }
    }
}

/** Внешняя дуга завитка в единицах угла (40 на сторону): строится раз, кадр её только масштабирует. */
private val CornerOuter = Path().apply {
    moveTo(0f, 38f)
    lineTo(0f, 14f)
    quadraticTo(0f, 0f, 14f, 0f)
    lineTo(38f, 0f)
}

/** Внутренняя дуга завитка в тех же единицах. */
private val CornerInner = Path().apply {
    moveTo(5f, 38f)
    lineTo(5f, 18f)
    quadraticTo(5f, 5f, 18f, 5f)
    lineTo(38f, 5f)
}

/** Завитки по углам рамки легенды: двойная дуга и капля золота; без аллокаций в кадре. */
fun DrawScope.relicCorners(gold: Color) {
    val k = 38.dp.toPx() / 40f
    val inset = 8.dp.toPx()
    val outer = Stroke(1.2.dp.toPx() / k)
    val inner = Stroke(.8.dp.toPx() / k)
    val drop = 2.dp.toPx() / k
    for (corner in 0 until 4) {
        val x = if (corner == 1 || corner == 2) size.width - inset else inset
        val y = if (corner >= 2) size.height - inset else inset
        withTransform({
            translate(x, y)
            rotate(corner * 90f, Offset.Zero)
            scale(k, k, Offset.Zero)
        }) {
            drawPath(CornerOuter, gold.copy(alpha = .85f), style = outer)
            drawPath(CornerInner, gold.copy(alpha = .35f), style = inner)
            drawCircle(gold, drop, Offset(10f, 10f))
        }
    }
}

/**
 * «Астролябия»: вокруг вещи - кольцо с делениями, медленно идущее по кругу, встречная пунктирная орбита со
 * спутниками и дышащее свечение; в центре - круглое гнездо с иконкой. Размер блока [size].
 *
 * Фазы читаются в рисовании: кольцо и свечение перерисовываются без перекомпоновки.
 */
@Composable fun Astrolabe(look: RelicLook, size: Dp = 200.dp, socket: Dp = 108.dp, content: @Composable BoxScope.() -> Unit) {
    val ring = motionPhase(RING_MS)
    val breathing = motionPhase(BREATH_MS)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val turn = ring.value
            val breath = breathing.value
            val c = center
            val unit = this.size.minDimension / 200f
            val pulse = .55f + .45f * ((sin(breath * 2 * PI).toFloat() + 1) / 2)
            drawCircle(Brush.radialGradient(listOf(look.glow.copy(alpha = .38f * pulse), Color.Transparent), c, 78 * unit), 78 * unit, c)
            rotate(turn * 360f, c) {
                drawCircle(look.gold.copy(alpha = .35f), 94 * unit, c, style = Stroke(1.dp.toPx()))
                repeat(24) { i ->
                    rotate(i * 15f, c) {
                        drawLine(look.gold.copy(alpha = .55f), Offset(c.x, c.y - 94 * unit), Offset(c.x, c.y - (if (i % 3 == 0) 84 else 88) * unit), 1.dp.toPx())
                    }
                }
                drawCircle(look.gold, 3 * unit, Offset(c.x, c.y - 94 * unit))
            }
            rotate(-turn * 540f, c) {
                drawCircle(look.accent.copy(alpha = .35f), 76 * unit, c, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2 * unit, 6 * unit))))
                rotate(-24f, c) {
                    drawOval(look.gold.copy(alpha = .25f), Offset(c.x - 88 * unit, c.y - 30 * unit), Size(176 * unit, 60 * unit), style = Stroke(1.dp.toPx()))
                }
                drawCircle(look.accent, 2.5f * unit, Offset(c.x + 76 * unit, c.y))
                drawCircle(look.gold, 2f * unit, Offset(c.x - 62 * unit, c.y + 40 * unit))
            }
        }
        Box(
            Modifier.size(socket).glow(look.glow.copy(alpha = .55f), radius = 14.dp, shape = CircleShape)
                .background(Brush.radialGradient(listOf(look.top, look.bottom)), CircleShape)
                .drawBehind {
                    drawCircle(look.gold, this.size.minDimension / 2, style = Stroke(1.5.dp.toPx()))
                    drawCircle(look.gold.copy(alpha = .12f), this.size.minDimension / 2 + 4.dp.toPx(), style = Stroke(5.dp.toPx()))
                },
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** Оборот кольца «Астролябии», мс. */
private const val RING_MS = 60_000

/** Вдох свечения за вещью, мс. */
private const val BREATH_MS = 5_000

/** Цикл неба легенды: звёзды мерцают, искры всплывают, мс. */
private const val SKY_MS = 24_000
