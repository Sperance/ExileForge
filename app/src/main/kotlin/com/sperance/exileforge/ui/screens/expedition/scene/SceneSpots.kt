package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.Tile
import com.sperance.exileforge.core.campaign.run.AgentMode
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.ui.icons.drawToken
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

// ==================== What stands on the map ====================

/** A fountain: a stone basin on the ground, its water glowing while it can still heal, dark once drunk. */
internal fun ScenePainter.drawFountain(x: Double, y: Double, used: Boolean, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    val w = unit * .42f
    val d = unit * .21f
    val h = unit * .2f
    val stone = Color(0xFF6E6A62)
    pen.color = Color.Black.copy(alpha = .35f)
    pen.ellipse(cx - w * 1.2f, cy - d * .8f, w * 2.4f, d * 1.6f)
    pen.color = tone(stone, .7f * light)
    pen.quad(cx - w, cy, cx, cy - d, cx, cy - d + h, cx - w, cy + h)
    pen.color = tone(stone, .55f * light)
    pen.quad(cx, cy - d, cx + w, cy, cx + w, cy + h, cx, cy - d + h)
    pen.color = tone(stone, .95f * light)
    diamond(cx, cy + h, w, d)
    val water = if (used) Color(0xFF1B2226) else Color(0xFF4FB8D8)
    pen.color = tone(water, light)
    diamond(cx, cy + h, w * .72f, d * .72f)
    if (!used) {
        pen.color = water.copy(alpha = (.35f + .25f * sin(time * 2.5f + x.toFloat())) * light)
        pen.circle(cx, cy + h, unit * .45f)
        pen.color = Color.White.copy(alpha = .5f * light)
        pen.circle(cx, cy + h - unit * .05f, unit * .05f + unit * .03f * sin(time * 4f))
    }
}

/**
 * A crystal of essences (2.78.0): a tall prism of violet glass on the ground, humming while its guardian
 * waits — red once a Vaal orb made the guardian stronger — and dull glass once it is freed.
 */
internal fun ScenePainter.drawCrystal(x: Double, y: Double, freed: Boolean, stronger: Boolean, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    val w = unit * .26f
    val d = unit * .13f
    val h = unit * (if (freed) .45f else .95f)
    val glass = when {
        freed -> Color(0xFF3A3346)
        stronger -> Color(0xFFD04A5A)
        else -> Color(0xFFB07FE0)
    }
    pen.color = Color.Black.copy(alpha = .35f)
    pen.ellipse(cx - w * 1.4f, cy - d, w * 2.8f, d * 2f)
    pen.color = tone(glass, .78f * light)
    pen.quad(cx - w, cy, cx, cy - d, cx, cy - d + h * .8f, cx - w, cy + h * .7f)
    pen.color = tone(glass, .55f * light)
    pen.quad(cx, cy - d, cx + w, cy, cx + w, cy + h * .7f, cx, cy - d + h * .8f)
    pen.color = tone(glass, light)
    pen.quad(cx - w, cy + h * .7f, cx, cy - d + h * .8f, cx + w, cy + h * .7f, cx, cy + h)
    if (!freed) {
        pen.color = glass.copy(alpha = (.18f + .12f * sin(time * 2f + x.toFloat())) * light)
        pen.circle(cx, cy + h * .5f, unit * .6f)
        pen.color = Color.White.copy(alpha = .6f * light)
        pen.circle(cx - w * .35f, cy + h * .6f, unit * .035f)
    }
}

/**
 * A crack of the Abyss (2.82.0): a black rift torn in the ground, its lips lit violet and breathing, a
 * haze rising from it — until it was opened, when it is only a dark scar.
 */
internal fun ScenePainter.drawCrack(x: Double, y: Double, opened: Boolean, seed: Int, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    val w = unit * .62f
    val d = unit * .3f
    val violet = Color(0xFFA26BFF)
    val breath = .5f + .5f * sin(time * 1.6f + seed)
    if (!opened) {
        pen.color = violet.copy(alpha = (.12f + .1f * breath) * light)
        pen.ellipse(cx - w * 1.5f, cy - d * 1.5f, w * 3f, d * 3f)
    }
    pen.color = tone(if (opened) Color(0xFF2A2433) else Color(0xFF0B0612), light)
    pen.quad(cx - w, cy, cx - w * .2f, cy + d * .55f, cx + w, cy, cx + w * .15f, cy - d * .6f)
    pen.color = (if (opened) Color(0xFF4A3A5C) else violet).copy(alpha = (if (opened) .5f else .75f + .25f * breath) * light)
    pen.polyline(cx - w, cy, cx - w * .45f, cy + d * .2f, cx - w * .1f, cy - d * .15f, cx + w * .35f, cy + d * .25f, cx + w, cy, width = unit * .05f)
    if (opened) return
    pen.color = violet.copy(alpha = .9f * light)
    pen.polyline(cx - w * .7f, cy + d * .05f, cx - w * .2f, cy - d * .25f, cx + w * .25f, cy + d * .1f, cx + w * .7f, cy - d * .05f, width = unit * .025f)
    // A haze of the deep rising: three motes drifting up and fading.
    for (i in 0 until 3) {
        val t = ((time * .35f + i / 3f + seed * .13f) % 1f)
        pen.color = violet.copy(alpha = (1 - t) * .55f * light)
        pen.circle(cx + (i - 1) * w * .35f + sin(time * 2f + i) * unit * .05f, cy + t * unit * .9f, unit * (.05f + .03f * (1 - t)))
    }
}

/** A chest: an iron-bound box on the ground, its lid shut and gleaming, or thrown back on an empty one. */
internal fun ScenePainter.drawChest(x: Double, y: Double, opened: Boolean, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    val w = unit * .45f
    val d = unit * .22f
    val h = unit * .38f
    val wood = Color(0xFF6B4423)
    val iron = Color(0xFF3A3A40)
    pen.color = Color.Black.copy(alpha = .35f)
    pen.ellipse(cx - w * 1.2f, cy - d * .8f, w * 2.4f, d * 1.6f)
    // Two faces seen at a slant, and the top.
    pen.color = tone(wood, .85f * light)
    pen.quad(cx - w, cy, cx, cy - d, cx, cy - d + h, cx - w, cy + h)
    pen.color = tone(wood, .65f * light)
    pen.quad(cx, cy - d, cx + w, cy, cx + w, cy + h, cx, cy - d + h)
    pen.color = tone(iron, light)
    pen.line(cx - w, cy + h * .5f, cx, cy - d + h * .5f, unit * .04f)
    pen.line(cx, cy - d + h * .5f, cx + w, cy + h * .5f, unit * .04f)
    if (opened) {
        pen.color = tone(Color(0xFF1A120B), light)
        diamond(cx, cy + h, w, d)
        pen.color = tone(wood, .95f * light)
        pen.quad(cx - w, cy + h, cx, cy + h + d, cx, cy + h + d + h * .8f, cx - w, cy + h + h * .8f)
    } else {
        pen.color = tone(wood, 1.05f * light)
        diamond(cx, cy + h, w, d)
        pen.color = Palettes.torch.copy(alpha = (.55f + .35f * sin(time * 3f + x.toFloat())) * light)
        pen.circle(cx, cy + h * .55f, unit * .06f)
        pen.color = Palettes.torch.copy(alpha = .12f * light)
        pen.circle(cx, cy + h * .7f, unit * .5f)
    }
}

/** The Vaal portal (2.65.0): a black mouth in scarlet rings, beating like the zone behind it. */
internal fun ScenePainter.vaalPortal(x: Double, y: Double, light: Float) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    val beat = .5f + .5f * sin(time * 2.4f)
    val lit = light.coerceAtLeast(.5f)
    pen.color = Palettes.blood.copy(alpha = .25f * lit)
    pen.ellipse(cx - unit * .9f, cy - unit * .3f, unit * 1.8f, unit * .6f)
    for (i in 3 downTo 1) {
        pen.color = Color(0xFFFF3C28).copy(alpha = (.1f + .12f * i * beat) * lit)
        pen.ellipse(cx - unit * .32f * i, cy - unit * .12f * i + unit * .7f, unit * .64f * i, unit * .55f * i)
    }
    pen.color = Color(0xFF120204)
    pen.ellipse(cx - unit * .28f, cy + unit * .72f, unit * .56f, unit * .95f)
    pen.color = Color(0xFFFF5A46).copy(alpha = (.55f + .35f * beat) * lit)
    pen.ring(cx - unit * .3f, cy + unit * .7f, unit * .6f, unit * 1f, unit * .06f)
}

/** The exit: a pale gate, or — while its guardian lives (since 2.34.0) — a dim red one crossed by chains. */
internal fun ScenePainter.portal(x: Double, y: Double, sealed: Boolean) {
    val cx = isoX(x, y)
    val cy = isoY(x, y)
    val pulse = .5f + .5f * sin(time * (if (sealed) 1.5f else 3f))
    val hue = if (sealed) Palettes.blood else Palettes.portal
    for (i in 3 downTo 1) {
        pen.color = hue.copy(alpha = .12f + .1f * i * pulse)
        pen.ellipse(cx - unit * .35f * i, cy - unit * .15f * i + unit * .6f, unit * .7f * i, unit * .5f * i)
    }
    pen.color = (if (sealed) Palettes.blood else Color.White).copy(alpha = .5f + .4f * pulse)
    pen.ellipse(cx - unit * .25f, cy + unit * .75f, unit * .5f, unit * .8f)
    if (sealed) {
        pen.color = Palettes.steel.copy(alpha = .85f)
        pen.line(cx - unit * .4f, cy + unit * .7f, cx + unit * .4f, cy + unit * 1.5f, unit * .06f)
        pen.line(cx + unit * .4f, cy + unit * .7f, cx - unit * .4f, cy + unit * 1.5f, unit * .06f)
        pen.circle(cx, cy + unit * 1.1f, unit * .1f)
    }
}
