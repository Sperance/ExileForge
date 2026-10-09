package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * Иконка умения «Неон рун» (4.4.0, утверждена владельцем, макет C): тёмная скруглённая плитка в нити цвета умения, на ней
 * светящийся контур-глиф - моно-спрайт сервера ([SkillDefinition.icon]) только обводкой - и римская цифра тира в углу. Тир II - рамка цвета
 * и мягкое свечение, тир III - двойная рамка и дышащее свечение (стоит при выключенном `LocalMotion`). [dim] - умение не
 * изучено: всё приглушено.
 */
@Composable fun NeonSkillIcon(skill: SkillDefinition, tier: Int, size: Dp, modifier: Modifier = Modifier, dim: Boolean = false) {
    val hue = NeonHue.of(skill).color.let { if (dim) lerp(it, NeonTile, .55f) else it }
    val breath = if (tier >= 3) .5f + .5f * sin(motionClock(BREATH_MS, "neon-breath") * 2 * PI).toFloat() else 0f
    Box(
        modifier.size(size).drawBehind { neonTile(hue, tier, breath) },
        contentAlignment = Alignment.Center,
    ) {
        val glyph = size * .58f
        // Ореол под глифом - неоновое свечение контура
        Box(Modifier.size(glyph).drawBehind { drawCircle(Brush.radialGradient(listOf(hue.copy(alpha = .32f), Color.Transparent)), radius = this.size.minDimension * .75f) })
        if (!NeonSprite(skillIcon(skill.icon), hue, Modifier.size(glyph))) {
            Icon(ForgeGlyphs.Grimoire, null, tint = hue, modifier = Modifier.size(glyph))
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
    }
}

/** Плитка, нить и рамки тира; свечение рамки ложится за край плитки слоями убывающей прозрачности. */
private fun DrawScope.neonTile(hue: Color, tier: Int, breath: Float) {
    val radius = CornerRadius(size.minDimension * .25f)
    val line = 1.dp.toPx()
    if (tier >= 2) {
        val reach = (if (tier >= 3) 8.dp.toPx() + 6.dp.toPx() * breath else 5.dp.toPx())
        val strength = if (tier >= 3) .22f + .12f * breath else .16f
        GLOW_LAYERS.forEach { layer ->
            val grow = reach * layer
            drawRoundRect(
                hue.copy(alpha = strength * (1 - layer)),
                topLeft = Offset(-grow / 2, -grow / 2),
                size = Size(size.width + grow, size.height + grow),
                cornerRadius = CornerRadius(radius.x + grow / 2),
                style = Stroke(grow.coerceAtLeast(line)),
            )
        }
    }
    drawRoundRect(NeonTile, cornerRadius = radius)
    drawRoundRect(if (tier >= 2) hue else lerp(hue, Color.Black, .55f), cornerRadius = radius, style = Stroke(line))
    if (tier >= 3) {
        val inset = 4.dp.toPx()
        drawRoundRect(
            hue,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            cornerRadius = CornerRadius((radius.x - inset).coerceAtLeast(line)),
            style = Stroke(line),
        )
    }
}

/** Доли размаха свечения рамки: три слоя, от плотного к прозрачному. */
private val GLOW_LAYERS = listOf(.25f, .55f, .9f)
