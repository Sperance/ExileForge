package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.run.ProvokeView
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import kotlin.math.sin

internal fun tauntTip(provoke: ProvokeView?) = Tip(
    ui("fight.taunt"),
    provoke?.let {
        ui("fight.taunt_hero", fineNumber(it.chance), fineNumber(it.seconds), fineNumber(it.cut), fineNumber(it.bossCut), fineNumber(it.immunity), fineNumber(it.bossImmunity))
    } ?: ui("fight.taunt_hint"),
    TauntCrimson,
)

/** The taunt's colours: dried blood and old gold. */
private val TauntCrimson = Color(0xFFB8373A)
private val TauntGold = Color(0xFFE8B06A)

/**
 * A taunter's aura: a soft crimson glow breathing round its card, under the border, so a
 * taunter is found at a glance without a word on it.
 */
internal fun DrawScope.tauntAura(outline: Outline, time: Float) {
    val breath = .35f + .25f * sin(time * 2.4f)
    for (step in 3 downTo 1) drawOutline(outline, TauntCrimson.copy(alpha = breath / step), style = Stroke((step * 2.5f).dp.toPx()))
    drawOutline(outline, Brush.verticalGradient(listOf(TauntGold, TauntCrimson)), style = Stroke(1.5.dp.toPx()))
}

/**
 * The taunt's seal: a heraldic shield in crimson with a gold rim and a gold chevron,
 * glowing faintly — the mark a taunter carries on its portrait; a tap opens its window.
 */
@Composable internal fun TauntSeal(frame: FightFrame, modifier: Modifier, tip: (() -> Tip)?) = Tipped(tip, modifier) {
    Canvas(Modifier.fillMaxSize().semantics { contentDescription = ui("fight.taunt") }) {
        val time = frame.time
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
