package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.roman
import com.sperance.exileforge.core.display.skillIcon
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.NeonSprite
import kotlin.math.PI
import kotlin.math.sin

/**
 * Цвет умения «Неон рун» (4.4.0): стихия - огонь, холод, молния, хаос, физика, - а без стихии вид умения. Палитра одна на
 * иконку гримуара и «Жилы энергии» боя.
 */
enum class NeonHue(val color: Color) {
    PHYSICAL(Color(0xFFD8CDB0)),
    FIRE(Color(0xFFFF7A3C)),
    COLD(Color(0xFF7FD4FF)),
    LIGHTNING(Color(0xFFFFE45C)),
    CHAOS(Color(0xFFB77CFF)),
    LIFE(Color(0xFFFF4D62)),
    HOLY(Color(0xFFFFF1B8)),
    ARMOUR(Color(0xFFA9B4BF)),
    GOLD(Color(0xFFF2D27A)),
    ;

    companion object {
        /** Цвет умения [skill]: стихия ([SkillDefinition.element]) первой, иначе - вид. */
        fun of(skill: SkillDefinition): NeonHue = skill.element?.let { element -> entries.firstOrNull { it.name == element } } ?: of(skill.type)

        fun of(type: SkillType): NeonHue = when (type) {
            SkillType.ATTACK -> PHYSICAL
            SkillType.SPELL, SkillType.AURA -> GOLD
            SkillType.WARCRY, SkillType.BONUS -> ARMOUR
            SkillType.CURSE -> CHAOS
            SkillType.HEAL -> HOLY
            SkillType.GUARD -> LIFE
            SkillType.TRIGGER -> LIGHTNING
        }
    }
}

/** Плитка «Неон рун»: почти чёрная, у всех иконок одна. */
private val NeonTile = Color(0xFF0A0F12)

/** Дыхание свечения тира III, мс (макет: 2,6 с). */
private const val BREATH_MS = 2600

/**
 * Силуэт плитки умения по его виду (0.14.4, макет «Иконки умений», вариант B): атака - скруглённый квадрат, чары и лечение - круг,
 * аура - восьмиугольник, клич и защита - щит, проклятие - ромб, бонус - шестигранник, срабатывание - треугольник. Вид читается
 * силуэтом и на малой иконке боя; [glyph] - доля стороны под контур, [lift] - сдвиг контура вниз (доля стороны), чтобы он лёг в
 * широкую часть силуэта; [points] - вершины в долях стороны (пусто - скруглённый квадрат или круг).
 */
enum class NeonFrame(val glyph: Float, val lift: Float = 0f, private val points: List<Pair<Float, Float>> = emptyList()) {
    SQUARE(.58f),
    CIRCLE(.54f),
    OCTAGON(.54f, points = listOf(.29f to 0f, .71f to 0f, 1f to .29f, 1f to .71f, .71f to 1f, .29f to 1f, 0f to .71f, 0f to .29f)),
    SHIELD(.5f, -.03f, listOf(.5f to 0f, 1f to .14f, .95f to .6f, .78f to .86f, .5f to 1f, .22f to .86f, .05f to .6f, 0f to .14f)),
    DIAMOND(.46f, points = listOf(.5f to 0f, 1f to .5f, .5f to 1f, 0f to .5f)),
    HEXAGON(.52f, points = listOf(.5f to 0f, .94f to .25f, .94f to .75f, .5f to 1f, .06f to .75f, .06f to .25f)),
    TRIANGLE(.44f, .14f, listOf(.5f to .02f, 1f to .96f, 0f to .96f)),
    ;

    /** Контур силуэта на холсте [size], отступив [inset] пикселей внутрь со всех сторон. */
    fun path(size: Size, inset: Float = 0f): Path = Path().apply {
        val w = size.width - inset * 2
        val h = size.height - inset * 2
        when {
            this@NeonFrame == SQUARE -> addRoundRect(RoundRect(inset, inset, inset + w, inset + h, CornerRadius(minOf(w, h) * .25f)))

            this@NeonFrame == CIRCLE -> addOval(Rect(inset, inset, inset + w, inset + h))

            else -> {
                points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(inset + x * w, inset + y * h) else lineTo(inset + x * w, inset + y * h) }
                close()
            }
        }
    }

    companion object {
        fun of(type: SkillType): NeonFrame = when (type) {
            SkillType.ATTACK -> SQUARE
            SkillType.SPELL, SkillType.HEAL -> CIRCLE
            SkillType.AURA -> OCTAGON
            SkillType.WARCRY, SkillType.GUARD -> SHIELD
            SkillType.CURSE -> DIAMOND
            SkillType.BONUS -> HEXAGON
            SkillType.TRIGGER -> TRIANGLE
        }
    }
}

/**
 * Иконка умения «Неон рун» (4.4.0, утверждена владельцем, макет C): тёмная плитка в нити цвета умения, на ней светящийся
 * контур-глиф - моно-спрайт сервера ([SkillDefinition.icon]) только обводкой - и римская цифра тира в углу. Тир II - рамка цвета
 * и мягкое свечение, тир III - двойная рамка и дышащее свечение (стоит при выключенном `LocalMotion`). [dim] - умение не
 * изучено: всё приглушено. С 0.14.4 (макет «Иконки умений», вариант B) плитка - силуэт вида умения ([NeonFrame]), а внизу -
 * его руна ([SkillDefinition.sigil]): умения одного вида и цвета различаются с первого взгляда.
 */
@Composable fun NeonSkillIcon(skill: SkillDefinition, tier: Int, size: Dp, modifier: Modifier = Modifier, dim: Boolean = false) {
    val hue = NeonHue.of(skill).color.let { if (dim) lerp(it, NeonTile, .55f) else it }
    val frame = NeonFrame.of(skill.type)
    // Фаза дыхания читается в рисовании (4.9.0): плитка перерисовывается шагом часов декора без перекомпоновки.
    val phase = if (tier >= 3) motionPhase(BREATH_MS) else null
    Box(
        // Силуэт строится раз на размер, кадр дыхания его только перерисовывает
        modifier.size(size).drawWithCache {
            val outline = frame.path(this.size)
            val inner = frame.path(this.size, 4.dp.toPx())
            onDrawBehind { neonTile(outline, inner, hue, tier, phase?.let { .5f + .5f * sin(it.value * 2 * PI).toFloat() } ?: 0f) }
        },
        contentAlignment = Alignment.Center,
    ) {
        val glyph = size * frame.glyph
        val lift = Modifier.offset(y = size * frame.lift)
        // Ореол под глифом - неоновое свечение контура
        Box(lift.size(glyph).drawBehind { drawCircle(Brush.radialGradient(listOf(hue.copy(alpha = .32f), Color.Transparent)), radius = this.size.minDimension * .75f) })
        if (!NeonSprite(skillIcon(skill.icon), hue, lift.size(glyph))) {
            Icon(ForgeGlyphs.Grimoire, null, tint = hue, modifier = lift.size(glyph))
        }
        Text(
            roman(tier),
            style = TextStyle(
                color = hue,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * .16f).coerceIn(8f, 13f).sp,
                shadow = Shadow(hue, blurRadius = 8f),
            ),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = size * .03f, end = size * .08f),
        )
        Text(
            skill.sigil,
            style = TextStyle(
                color = lerp(hue, Color.White, .6f).copy(alpha = if (dim) .55f else .9f),
                fontSize = (size.value * .17f).coerceIn(7f, 12f).sp,
                shadow = Shadow(hue, blurRadius = 6f),
            ),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = size * .04f),
        )
    }
}

/**
 * Силуэт [outline], нить и рамки тира ([inner] - вторая рамка тира III); свечение рамки - тот же силуэт широкой полупрозрачной
 * обводкой, слоями убывающей прозрачности.
 */
private fun DrawScope.neonTile(outline: Path, inner: Path, hue: Color, tier: Int, breath: Float) {
    val line = 1.dp.toPx()
    if (tier >= 2) {
        val reach = (if (tier >= 3) 8.dp.toPx() + 6.dp.toPx() * breath else 5.dp.toPx())
        val strength = if (tier >= 3) .22f + .12f * breath else .16f
        GLOW_LAYERS.forEach { layer ->
            val grow = reach * layer
            drawPath(outline, hue.copy(alpha = strength * (1 - layer)), style = Stroke(grow.coerceAtLeast(line)))
        }
    }
    drawPath(outline, NeonTile)
    drawPath(outline, if (tier >= 2) hue else lerp(hue, Color.Black, .55f), style = Stroke(line))
    if (tier >= 3) drawPath(inner, hue, style = Stroke(line))
}

/** Доли размаха свечения рамки: три слоя, от плотного к прозрачному. */
private val GLOW_LAYERS = listOf(.25f, .55f, .9f)
