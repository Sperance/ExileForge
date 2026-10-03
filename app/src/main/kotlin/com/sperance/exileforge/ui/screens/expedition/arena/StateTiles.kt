package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/** Every state on the hero as a tile of its colour whose dark fill rises as it wears off; a stun is a gold star. */
@Composable internal fun StateTiles(ailments: List<AilmentView>, held: Boolean, effects: List<EffectView> = emptyList(), charges: List<ChargeView> = emptyList()) {
    val stunned = held && ailments.none { it.ailment == Ailment.FROZEN }
    Row(Modifier.height(30.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!stunned && ailments.isEmpty() && effects.isEmpty() && charges.isEmpty()) MutedText(ui("fight.no_states"), style = MaterialTheme.typography.labelSmall)
        if (stunned) StateTile(null, GoldBright, 1f, 1, ui("expedition.stunned")) { stunTip() }
        ailments.forEach { view -> StateTile(view.ailment, ailmentTint(view.ailment), view.left, view.stacks, ailmentLabel(view)) { ailmentTip(view) } }
        charges.forEach { ChargeTile(it) }
        effects.take(4).forEach { EffectTile(it) }
    }
}

/** A kind of the hero's charges (3.33.0): the drawing of its maximum and the count held. */
@Composable private fun ChargeTile(view: ChargeView, side: Dp = 30.dp) {
    val shape = RoundedCornerShape(4.dp)
    val title = ui("fight.charge.${view.kind.name}")
    Tipped(
        { Tip(title, ui("fight.charge_tip", view.count, view.max, fineNumber(view.seconds))) },
        Modifier.size(side).clip(shape).background(Color(0xFF0B0E13)).background(Gold.copy(alpha = .16f)).border(1.dp, Gold, shape)
            .semantics { contentDescription = "$title ${view.count}" },
    ) {
        com.sperance.exileforge.ui.icons.StatIcon(view.stat, Gold, Modifier.fillMaxSize().padding(side / 6))
        Text(
            "${view.count}",
            color = Parchment,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp),
        )
    }
}

/** A buff or a curse (2.78.0) as a tile: the skill's mark in gold for a buff, in blood for a curse, darkening as it wears off. */
@Composable internal fun EffectTile(view: EffectView, side: Dp = 30.dp) {
    val tint = if (view.kind == EffectKind.CURSE) LifeRed else Gold
    val shape = RoundedCornerShape(4.dp)
    // A buff of the rules (3.35.0) — Onslaught, Fortify — is named and drawn by its kind, a skill's by the skill.
    val buff = view.buff
    val title = buff?.let { ui("fight.buff.${it.name}") } ?: SkillText.title(view.source)
    Tipped(
        { Tip(title, ui(if (view.kind == EffectKind.CURSE) "fight.effect_curse" else "fight.effect_buff", fineNumber(view.seconds))) },
        Modifier.size(side).clip(shape).background(Color(0xFF0B0E13)).background(tint.copy(alpha = .16f)).border(1.dp, tint, shape)
            .semantics { contentDescription = title },
    ) {
        if (buff != null) {
            com.sperance.exileforge.ui.icons.StatIcon(buff.icon, tint, Modifier.fillMaxSize().padding(side / 6))
        } else {
            SkillGlyph(view.icon, Modifier.fillMaxSize().padding(side / 6), tint)
        }
        Box(Modifier.fillMaxWidth().fillMaxHeight((1 - view.left).coerceIn(0f, 1f)).background(Color.Black.copy(alpha = .55f)))
    }
}

@Composable internal fun StateTile(ailment: Ailment?, tint: Color, left: Float, stacks: Int, label: String, side: Dp = 30.dp, tip: () -> Tip) {
    val shape = RoundedCornerShape(4.dp)
    Tipped(
        tip,
        Modifier.size(side).clip(shape).background(Color(0xFF0B0E13)).background(tint.copy(alpha = .16f)).border(1.dp, tint, shape)
            .semantics { contentDescription = label },
    ) {
        Canvas(Modifier.fillMaxSize().padding(side / 5)) { stateGlyph(ailment, tint) }
        Box(Modifier.fillMaxWidth().fillMaxHeight((1 - left).coerceIn(0f, 1f)).background(Color.Black.copy(alpha = .55f)))
        if (stacks > 1) {
            Text(
                "$stacks",
                color = Parchment,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp),
            )
        }
    }
}

/** An ailment's mark drawn from shapes: a flame, a snowflake, a crystal, a bolt, a drop, two drops; a star for a stun. */
private fun DrawScope.stateGlyph(ailment: Ailment?, tint: Color) {
    val u = size.minDimension / 16f
    fun path(vararg points: Pair<Float, Float>) = Path().apply {
        points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * u, y * u) else lineTo(x * u, y * u) }
        close()
    }
    when (ailment) {
        Ailment.BURNING -> drawPath(path(8f to 1f, 12f to 7f, 12f to 11f, 10f to 15f, 6f to 15f, 4f to 11f, 5f to 7f, 7f to 9f), tint)

        Ailment.CHILLED -> listOf(0f, 60f, 120f).forEach { angle ->
            val a = Math.toRadians(angle.toDouble())
            val dx = (kotlin.math.cos(a) * 7 * u).toFloat()
            val dy = (kotlin.math.sin(a) * 7 * u).toFloat()
            drawLine(tint, Offset(8 * u - dx, 8 * u - dy), Offset(8 * u + dx, 8 * u + dy), 1.6f * u)
        }

        Ailment.FROZEN -> drawPath(path(8f to 1f, 14f to 5f, 14f to 11f, 8f to 15f, 2f to 11f, 2f to 5f), tint)

        Ailment.SHOCKED -> drawPath(path(10f to 1f, 3f to 9f, 7f to 9f, 6f to 15f, 13f to 7f, 9f to 7f), tint)

        Ailment.POISONED -> {
            drawPath(path(8f to 2f, 12f to 9f, 11f to 13f, 8f to 14f, 5f to 13f, 4f to 9f), tint)
            drawCircle(Color(0xFF0B0E13), 1.4f * u, Offset(8 * u, 10 * u))
        }

        Ailment.BLEEDING -> {
            drawPath(path(6f to 2f, 9f to 8f, 8f to 11f, 6f to 12f, 4f to 11f, 3f to 8f), tint)
            drawPath(path(12f to 7f, 14f to 11f, 13f to 13f, 12f to 14f, 11f to 13f, 10f to 11f), tint)
        }

        null -> drawPath(path(8f to 1f, 9.8f to 5.2f, 14.3f to 5.6f, 10.9f to 8.6f, 11.9f to 13f, 8f to 10.7f, 4.1f to 13f, 5.1f to 8.6f, 1.7f to 5.6f, 6.2f to 5.2f), tint)
    }
}

internal fun ailmentLabel(view: AilmentView) = if (view.stacks > 1) ui("expedition.ailment_stacks", ui(view.ailment.key()), view.stacks) else ui(view.ailment.key())
