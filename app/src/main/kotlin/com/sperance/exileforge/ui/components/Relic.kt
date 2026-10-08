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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.R
import com.sperance.exileforge.core.character.FlaskVerdict
import com.sperance.exileforge.core.character.GearVerdict.Shift
import com.sperance.exileforge.core.character.StatDelta
import com.sperance.exileforge.core.display.AffixKind
import com.sperance.exileforge.core.display.AffixMarks
import com.sperance.exileforge.core.display.ItemLine
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.flaskRowTitle
import com.sperance.exileforge.core.display.flaskRowValue
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statValue
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * «Реликварий» (3.88.6, выбор владельца): подробная карточка предмета, её строки модификаторов со значком тира, итог героя
 * «сейчас → станет» и «Астролябия» уникальных и мифических вещей - кольца, искры и звёзды вокруг предмета.
 */

/** Имена предметов: Cinzel, начертание задаётся осью веса вариативного шрифта. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val RelicSerif = FontFamily(
    Font(R.font.cinzel, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.cinzel, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** Имя предмета в карточке: серифы Cinzel, размер [size]. */
fun relicName(size: Int = 23) = TextStyle(fontFamily = RelicSerif, fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = (size * 1.18f).sp, letterSpacing = (size * .02f).sp)

/**
 * Облик карточки по редкости: рамка, фон, свечение и цвета текста. Обычные, волшебные и редкие вещи - басальт в тон редкости;
 * уникальные - тёплый уголь с искрами, мифические - звёздное небо.
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
            Color(0xFFA88F7A), Color(0xFF6E5A4A), Color(0xFFFFC98A), RelicLook.Legend.EMBER,
        )

        Rarity.MYTHICAL -> RelicLook(
            color, Color(0xFFFFD99A), Color(0xFF2B1A48), Color(0xFF0C0A16), Color(0xFFF0C76A), Color(0xFFF0C76A),
            Color(0xFF9C8FB5), Color(0xFF6A5E80), Color(0xFF8EC5FF), RelicLook.Legend.STARS,
        )

        else -> RelicLook(color, color, DepthTop, DepthBottom, color, color, Color(0xFF8FA0AB), Color(0xFF56636D), Rune, null)
    }
}

/** Имя вещи серифами размера [size] в цвете редкости; у легенды - с ореолом её свечения (3.90.0). */
fun RelicLook.nameStyle(size: Int): TextStyle = relicName(size).copy(color = name, shadow = if (legend != null) Shadow(glow.copy(alpha = .6f), blurRadius = 18f) else null)

/**
 * Подложка вещи в облике редкости (3.90.0) - общая для полной карточки и плитки: градиент [RelicLook.top] → [RelicLook.bottom],
 * свечение редкости от верхнего края, небо легенды, завитки по углам у легенды и рамка - нить [RelicLook.gold], у выбранной - золото.
 */
@Composable fun Modifier.relicGround(look: RelicLook, shape: Shape, selected: Boolean = false): Modifier {
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
        .relicSky(look)
        .then(if (look.legend != null) Modifier.drawBehind { relicCorners(look.gold) } else Modifier)
        .border(if (selected) 2.dp else 1.dp, frame, shape)
}

/**
 * Гнездо иконки размера [size] (3.90.0): у обычной, волшебной и редкой - скруглённый квадрат басальта в нити редкости,
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

    AffixKind.PREFIX, AffixKind.SUFFIX, null -> when (marks.tier) {
        1 -> TierAmber
        2 -> TierMint
        3 -> TierIce
        else -> TierAsh
    }
}

/** Цвет текста строки: тёплый у уникальной, сиреневый у ремесла, серый у врождённой, голубой у прочих. */
fun modColor(marks: AffixMarks): Color = when (marks.kind) {
    AffixKind.UNIQUE -> Color(0xFFFFDDB0)
    AffixKind.CRAFTED -> Color(0xFFC9C6FF)
    AffixKind.IMPLICIT -> TierAsh
    else -> Color(0xFFA9CFFF)
}

/** Шестигранник [size] с вершиной вверх, как в макете. */
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
 * Значок строки (3.88.6): шестигранник в цвете тира с его номером, ✦ у уникальной строки, молот у ремесла, пустой контур у врождённой.
 * Долгое нажатие называет вид строки.
 */
@Composable fun TierHex(marks: AffixMarks, size: Dp = 20.dp) {
    val kind = marks.kind
    val fill = tierColor(marks)
    val glyph = when (kind) {
        AffixKind.UNIQUE -> "✦"
        AffixKind.CRAFTED, AffixKind.IMPLICIT -> ""
        AffixKind.PREFIX, AffixKind.SUFFIX, null -> marks.tier.takeIf { it > 0 }?.toString().orEmpty()
        else -> if (marks.tier > 0) "${kind.letter}${marks.tier}" else "${kind.letter}"
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
                    // Молот ремесла: рукоять наискось и боёк поперёк.
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
 * карточка не печатает (3.88.7): тир, вилку и место ролла открывает нажатие на строку.
 */
@Composable fun RelicModLine(line: ItemLine, onClick: (() -> Unit)?, big: Boolean = true) {
    val tap = if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClickLabel = line.text, onClick = onClick)
    Row(
        Modifier.fillMaxWidth().then(tap).padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TierHex(line.marks, if (big) 20.dp else 16.dp)
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

/** «◆ № 41 в мире ◆»: номер экземпляра уникальной или мифической вещи, между двух нитей золота. */
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

/**
 * «ИТОГ ГЕРОЯ» (3.88.6): что станет с листом героя, если надеть вещь, - таблица «сейчас → станет», рост зелёным, потеря красным.
 */
@Composable fun HeroTotals(delta: List<StatDelta>, look: RelicLook, modifier: Modifier = Modifier) = TotalsTable(
    ui("relic.totals"),
    delta.map { TotalsLine(statTitle(it.stat), statValue(it.stat, it.before), statValue(it.stat, it.after), it.change > 0) },
    look,
    modifier,
)

/**
 * «ИТОГ ФЛЯГИ» (4.2.0): строки листа фляги против фляги в выбранном гнезде - лечение, длительность, заряды, эффект и что она
 * кладёт на героя; лучше - зелёным, хуже - красным (меньший расход заряда - лучше).
 */
@Composable fun FlaskTotals(verdict: FlaskVerdict, look: RelicLook, modifier: Modifier = Modifier) = TotalsTable(
    ui("relic.flask_totals"),
    verdict.rows.map { TotalsLine(flaskRowTitle(it), flaskRowValue(it, it.before), flaskRowValue(it, it.after), it.shift.takeIf { s -> s != Shift.EVEN }?.let { s -> s == Shift.UP }) },
    look,
    modifier,
)

/** Строка итога: имя, «сейчас», «станет» и сдвиг - к лучшему, к худшему или без цвета (null). */
private class TotalsLine(val title: String, val before: String, val after: String, val better: Boolean?)

/** Таблица «сейчас → станет» итога карточки под заголовком [title]; пустая говорит, что ничего не изменится. */
@Composable private fun TotalsTable(title: String, lines: List<TotalsLine>, look: RelicLook, modifier: Modifier) {
    val label = MaterialTheme.typography.labelSmall
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = look.muted, style = label, letterSpacing = 1.8.sp, modifier = Modifier.weight(1f))
            Text(ui("relic.now"), color = look.faint, style = label, textAlign = TextAlign.End, modifier = Modifier.width(64.dp))
            Spacer(Modifier.width(22.dp))
            Text(ui("relic.after"), color = look.faint, style = label, modifier = Modifier.width(64.dp))
        }
        if (lines.isEmpty()) Text(ui("wear.nothing"), color = look.muted, style = MaterialTheme.typography.bodySmall)
        lines.forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(line.title, color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
                Text(line.before, color = look.muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.width(64.dp), maxLines = 1)
                Text("→", color = look.faint, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                Text(
                    line.after,
                    color = when (line.better) {
                        true -> Vital
                        false -> Color(0xFFFF8F88)
                        null -> Parchment
                    },
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(64.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Мерцание звёзд и подъём искр легендарной карточки: по одному числу на точку, без перестройки при каждом кадре. */
private val SPARKS = List(26) { i ->
    val r = java.util.Random(7L + i * 31)
    floatArrayOf(r.nextFloat() * .94f + .03f, r.nextFloat() * .96f + .02f, if (r.nextInt(4) == 0) 2f else 1.2f, r.nextFloat())
}

/**
 * Фон легенды: у мифической - мерцающие звёзды, у уникальной - искры, медленно поднимающиеся к имени.
 * Рисуется за содержимым и не трогает раскладку.
 */
@Composable fun Modifier.relicSky(look: RelicLook): Modifier {
    val legend = look.legend ?: return this
    val time = motionClock(SKY_MS, "relic-sky")
    return drawBehind {
        SPARKS.forEach { (x, y, r, phase) ->
            if (legend == RelicLook.Legend.STARS) {
                val glow = .25f + .75f * ((sin(((time * 6 + phase) * 2 * PI)).toFloat() + 1) / 2)
                drawCircle(Color(0xFFFFF0D2).copy(alpha = glow), r.dp.toPx(), Offset(size.width * x, size.height * y))
            } else {
                // Искра живёт свой отрезок пути: всплывает на треть карточки и гаснет.
                val life = (time * 5 + phase) % 1f
                val alpha = if (life < .2f) life / .2f else 1f - (life - .2f) / .8f
                val rise = life * size.height * .3f
                drawCircle(Color(0xFFFFB061).copy(alpha = alpha * .9f), (r + .4f).dp.toPx(), Offset(size.width * x, size.height * (.45f + y * .5f) - rise))
            }
        }
    }
}

/** Завитки по углам рамки легенды: двойная дуга и капля золота. */
fun DrawScope.relicCorners(gold: Color) {
    val s = 38.dp.toPx()
    val inset = 8.dp.toPx()
    listOf(0f to Offset(inset, inset), 90f to Offset(size.width - inset, inset), 180f to Offset(size.width - inset, size.height - inset), 270f to Offset(inset, size.height - inset))
        .forEach { (angle, at) ->
            withTransform({
                translate(at.x, at.y)
                rotate(angle, Offset.Zero)
            }) {
                val k = s / 40f
                val outer = Path().apply {
                    moveTo(0f, 38 * k)
                    lineTo(0f, 14 * k)
                    quadraticTo(0f, 0f, 14 * k, 0f)
                    lineTo(38 * k, 0f)
                }
                val inner = Path().apply {
                    moveTo(5 * k, 38 * k)
                    lineTo(5 * k, 18 * k)
                    quadraticTo(5 * k, 5 * k, 18 * k, 5 * k)
                    lineTo(38 * k, 5 * k)
                }
                drawPath(outer, gold.copy(alpha = .85f), style = Stroke(1.2.dp.toPx()))
                drawPath(inner, gold.copy(alpha = .35f), style = Stroke(.8.dp.toPx()))
                drawCircle(gold, 2.dp.toPx(), Offset(10 * k, 10 * k))
            }
        }
}

/**
 * «Астролябия» (3.88.6): вокруг вещи - кольцо с делениями, медленно идущее по кругу, встречная пунктирная орбита со
 * спутниками и дышащее свечение; в центре - круглое гнездо с иконкой. Размер блока [size].
 */
@Composable fun Astrolabe(look: RelicLook, size: Dp = 200.dp, socket: Dp = 108.dp, content: @Composable BoxScope.() -> Unit) {
    val turn = motionClock(RING_MS, "relic-ring")
    val breath = motionClock(BREATH_MS, "relic-breath")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
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
