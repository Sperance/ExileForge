package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.LoneWolfRule
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import kotlin.math.sin

internal fun tauntTip(hero: Boolean) = Tip(ui("fight.taunt"), ui(if (hero) "fight.taunt_hero" else "fight.taunt_hint"), TauntCrimson)

internal fun loneWolfTip(rule: LoneWolfRule) = Tip(
    ui("fight.lone_wolf_title"),
    ui("fight.lone_wolf_body", number(rule.dealt), number(rule.taken)),
    GoldBright,
    listOf(ui("fight.fact_dealt") to "+${number(rule.dealt)}%", ui("fight.fact_taken") to "−${number(rule.taken)}%"),
)

/** The taunt's colours: dried blood and old gold. */
private val TauntCrimson = Color(0xFFB8373A)
private val TauntGold = Color(0xFFE8B06A)

/**
 * A taunter's aura (2.72.0): a soft crimson glow breathing round its card, under the border, so a
 * taunter is found at a glance without a word on it.
 */
internal fun Modifier.tauntAura(shape: Shape, time: Float) = drawBehind {
    val breath = .35f + .25f * sin(time * 2.4f)
    val outline = shape.createOutline(size, layoutDirection, this)
    for (step in 3 downTo 1) drawOutline(outline, TauntCrimson.copy(alpha = breath / step), style = Stroke((step * 2.5f).dp.toPx()))
    drawOutline(outline, Brush.verticalGradient(listOf(TauntGold, TauntCrimson)), style = Stroke(1.5.dp.toPx()))
}

/**
 * The taunt's seal (2.72.0): a heraldic shield in crimson with a gold rim and a gold chevron,
 * glowing faintly — the mark a taunter carries on its portrait; a tap opens its window.
 */
@Composable internal fun TauntSeal(time: Float, modifier: Modifier, tip: (() -> Tip)?) = Tipped(tip, modifier) {
    Canvas(Modifier.fillMaxSize().semantics { contentDescription = ui("fight.taunt") }) {
        val w = size.width
        val h = size.height
        drawCircle(TauntCrimson.copy(alpha = .25f + .15f * sin(time * 2.4f)), w * .62f, center)
        val shield = Path().apply {
            moveTo(w * .5f, h * .06f)
            lineTo(w * .9f, h * .2f)
            lineTo(w * .86f, h * .56f)
            quadraticTo(w * .78f, h * .84f, w * .5f, h * .96f)
            quadraticTo(w * .22f, h * .84f, w * .14f, h * .56f)
            lineTo(w * .1f, h * .2f)
            close()
        }
        drawPath(shield, Brush.verticalGradient(listOf(Color(0xFF7A1C22), TauntCrimson, Color(0xFF4A0E12))))
        drawPath(shield, TauntGold, style = Stroke(1.4.dp.toPx()))
        val chevron = Path().apply {
            moveTo(w * .3f, h * .42f)
            lineTo(w * .5f, h * .6f)
            lineTo(w * .7f, h * .42f)
        }
        drawPath(chevron, TauntGold, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * The lone wolf's medallion (2.72.0): a wolf's head in gold on a dark coin with a bronze rim; a tap
 * opens what the bonus gives.
 */
@Composable internal fun LoneWolfMedal(modifier: Modifier, tip: () -> Tip) = Tipped(tip, modifier.clip(CircleShape)) {
    Canvas(Modifier.fillMaxSize().semantics { contentDescription = ui("fight.lone_wolf_title") }) {
        val w = size.width
        val h = size.height
        drawCircle(Brush.radialGradient(listOf(PanelRaised, Color(0xFF0B0D11)), center, w / 2), w / 2, center)
        drawCircle(Bronze, w / 2 - 1.dp.toPx(), center, style = Stroke(1.5.dp.toPx()))
        val head = Path().apply {
            moveTo(w * .24f, h * .22f)
            lineTo(w * .38f, h * .38f)
            lineTo(w * .62f, h * .38f)
            lineTo(w * .76f, h * .22f)
            lineTo(w * .74f, h * .52f)
            lineTo(w * .58f, h * .8f)
            lineTo(w * .5f, h * .84f)
            lineTo(w * .42f, h * .8f)
            lineTo(w * .26f, h * .52f)
            close()
        }
        drawPath(head, Brush.verticalGradient(listOf(GoldBright, Gold)))
        drawCircle(Color(0xFF0B0D11), w * .045f, Offset(w * .41f, h * .5f))
        drawCircle(Color(0xFF0B0D11), w * .045f, Offset(w * .59f, h * .5f))
    }
}
