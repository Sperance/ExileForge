package com.sperance.exileforge.ui.screens.expedition.world

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.RoadState
import com.sperance.exileforge.core.campaign.TokenState
import com.sperance.exileforge.core.campaign.WorldMap
import com.sperance.exileforge.core.campaign.WorldToken
import com.sperance.exileforge.core.campaign.mapTitle
import com.sperance.exileforge.core.campaign.regionTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.ModBlue
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment
import kotlin.math.hypot

/**
 * The world map itself (2.76.0): the parchment, the fog above the frontier, the roads between the
 * tokens and the tokens — bronze medallions with the zone's level, the passed ones ticked, the open
 * ones glowing, the «???» behind them dark and locked. A drag moves it, a pinch zooms it; a tap on a
 * token picks the zone, a tap on the land lets it go. Everything but the words is drawn in world
 * units under one transform; the words are laid out once and only scaled.
 */
@Composable
fun WorldCanvas(world: WorldMap, art: WorldArt, camera: WorldCamera, selected: String?, stash: Map<String, Int>,
                modifier: Modifier = Modifier, onTap: (String?) -> Unit) {
    val measurer = rememberTextMeasurer()
    val motion = rememberInfiniteTransition(label = "world")
    val pulse by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(PULSE_MS, easing = LinearEasing)), label = "pulse")
    val march by motion.animateFloat(0f, -DASH_PERIOD, infiniteRepeatable(tween(MARCH_MS, easing = LinearEasing)), label = "march")
    val height = camera.world.height.toFloat()
    val roads = remember(world) { world.roads.map { it.state to road(it.from.zone.x, height - it.from.zone.y, it.to.zone.x, height - it.to.zone.y, it.from.zone.code + it.to.zone.code) } }
    val words = remember(world, measurer) { Words(world, measurer) }
    val tap by rememberUpdatedState(onTap)
    val description = ui("expedition.world_map")
    Canvas(modifier
        .clipToBounds()
        .semantics { contentDescription = description }
        .onSizeChanged { camera.viewport = it }
        .pointerInput(camera) { detectTransformGestures { centroid, pan, zoom, _ -> camera.transform(centroid, pan, zoom) } }
        .pointerInput(camera, world) { detectTapGestures { point -> tap(hit(world, camera, point)) } }) {
        withTransform({ translate(camera.offset.x, camera.offset.y); scale(camera.unit, camera.unit, Offset.Zero) }) {
            art.draw(this)
            fog(world, camera.world.width.toFloat(), height)
            val dash = PathEffect.dashPathEffect(floatArrayOf(7f, 6f), march)
            val dots = PathEffect.dashPathEffect(floatArrayOf(1f, 7f))
            roads.forEach { (state, path) ->
                when (state) {
                    RoadState.WALKED -> { drawPath(path, ROAD_INK, style = Stroke(7f, cap = StrokeCap.Round)); drawPath(path, ROAD_GOLD, style = Stroke(2.4f, cap = StrokeCap.Round)) }
                    RoadState.AHEAD -> { drawPath(path, ROAD_INK, style = Stroke(7f, cap = StrokeCap.Round)); drawPath(path, GoldBright, style = Stroke(2.2f, cap = StrokeCap.Round, pathEffect = dash)) }
                    RoadState.UNTRODDEN -> drawPath(path, Muted.copy(alpha = .85f), style = Stroke(1.8f, cap = StrokeCap.Round, pathEffect = dots))
                }
            }
            world.tokens.forEach { token(it, height, pulse, it.zone.code == selected, stash[it.zone.code] ?: 0) }
        }
        words.draw(this, camera, stash)
    }
}

/** The token under a tap, generous enough for a finger at any zoom. */
private fun hit(world: WorldMap, camera: WorldCamera, point: Offset): String? {
    val at = camera.toWorld(point)
    val reach = maxOf(TOKEN_REACH, FINGER / camera.scale)
    return world.tokens.map { it to hypot(it.zone.x - at.x, it.zone.y - at.y) }.filter { it.second <= reach }.minByOrNull { it.second }?.first?.zone?.code
}

/** A road between two tokens: a quadratic bow, bent the same way every time for the same two zones. */
private fun road(x1: Int, y1: Float, x2: Int, y2: Float, key: String): Path {
    val dx = x2 - x1.toFloat()
    val dy = y2 - y1
    val length = hypot(dx, dy).coerceAtLeast(1f)
    val bend = (Math.floorMod(key.hashCode(), 1000) / 1000f - .5f) * 50f
    return Path().apply {
        moveTo(x1.toFloat(), y1)
        quadraticTo((x1 + x2) / 2f - dy / length * bend, (y1 + y2) / 2 + dx / length * bend, x2.toFloat(), y2)
    }
}

/** The fog over what the hero has not reached: clear under the frontier, thick a few rows above it. */
private fun DrawScope.fog(world: WorldMap, width: Float, height: Float) {
    val clearTo = height - (world.fogLine + FOG_CLEAR)
    if (clearTo <= 0f) return
    val thickFrom = (height - (world.fogLine + FOG_THICK)).coerceAtLeast(0f)
    drawRect(Brush.verticalGradient(0f to FOG, (thickFrom / clearTo) to FOG.copy(alpha = .93f), 1f to FOG.copy(alpha = 0f), startY = 0f, endY = clearTo),
        size = androidx.compose.ui.geometry.Size(width, clearTo))
    val seed = world.fogLine
    repeat(PUFFS) { i ->
        val x = (Math.floorMod(seed * 31 + i * 977, width.toInt().coerceAtLeast(1))).toFloat()
        val y = thickFrom + (Math.floorMod(seed * 17 + i * 613, 1000) / 1000f) * (clearTo - thickFrom)
        val r = 50f + Math.floorMod(i * 389 + seed, 90)
        drawCircle(Brush.radialGradient(listOf(FOG.copy(alpha = .85f), FOG.copy(alpha = 0f)), Offset(x, y), r), r, Offset(x, y))
    }
}

/** One token: its medallion by state, the finale's crown, its tick or lock, the stash's maps for it, and the pick's ring. */
private fun DrawScope.token(token: WorldToken, height: Float, pulse: Float, selected: Boolean, maps: Int) {
    val c = Offset(token.zone.x.toFloat(), height - token.zone.y)
    val r = radius(token)
    val (light, mid, dark, ring) = when (token.state) {
        TokenState.PASSED -> listOf(Color(0xFF8D7447), Color(0xFF54432A), Color(0xFF2B2216), Color(0xFFB89A62))
        TokenState.OPEN -> listOf(Color(0xFFC9A868), Color(0xFF7B6038), Color(0xFF3B2D19), GoldBright)
        TokenState.LOCKED -> listOf(Color(0xFF41444A), Color(0xFF202227), Color(0xFF141518), Color(0xFF5A5F66))
    }
    if (token.state == TokenState.OPEN) {
        drawCircle(Brush.radialGradient(listOf(GoldBright.copy(alpha = .45f), GoldBright.copy(alpha = 0f)), c, r * 2f), r * 2f, c)
        drawCircle(GoldBright.copy(alpha = (1 - pulse) * .7f), r + 5 + pulse * 10, c, style = Stroke(1.5f))
    }
    drawCircle(Color.Black.copy(alpha = .55f), r + 2, c + Offset(0f, 3f))
    drawCircle(Brush.radialGradient(listOf(light, mid, dark), c + Offset(-r * .28f, -r * .4f), r * 1.3f), r, c)
    drawCircle(ring, r - 1, c, style = Stroke(2f))
    drawCircle(dark, r - 3, c, style = Stroke(2f))
    if (token.state != TokenState.LOCKED) drawCircle(Bronze, r - 4.5f, c, style = Stroke(1f))
    if (token.zone.finale) {
        drawCircle(Gold.copy(alpha = .35f), r + 2.5f, c, style = Stroke(2f))
        val top = c.y - r - 13
        val crown = Path().apply {
            moveTo(c.x - 9, top + 6); lineTo(c.x - 10, top - 3); lineTo(c.x - 4.5f, top + 1); lineTo(c.x, top - 6)
            lineTo(c.x + 4.5f, top + 1); lineTo(c.x + 10, top - 3); lineTo(c.x + 9, top + 6); close()
        }
        drawPath(crown, GoldBright)
        drawPath(crown, Color(0xFF2B2216), style = Stroke(1.2f, join = StrokeJoin.Round))
    }
    val badge = c + Offset(r * .72f, r * .72f)
    when (token.state) {
        TokenState.PASSED -> {
            drawCircle(Color(0xFF1D170E), 8.5f, badge)
            drawCircle(Gold, 8.5f, badge, style = Stroke(1f))
            drawPath(Path().apply { moveTo(badge.x - 4, badge.y + .5f); lineTo(badge.x - 1, badge.y + 3.5f); lineTo(badge.x + 4.5f, badge.y - 3) },
                GoldBright, style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        TokenState.LOCKED -> {
            drawCircle(Color(0xFF15171B), 8.5f, badge)
            drawCircle(Color(0xFF5A5F66), 8.5f, badge, style = Stroke(1f))
            drawRect(Color(0xFFA4A9AE), badge + Offset(-3.5f, -.5f), androidx.compose.ui.geometry.Size(7f, 5f))
            drawArc(Color(0xFFA4A9AE), 180f, 180f, false, badge + Offset(-2.3f, -4.5f), androidx.compose.ui.geometry.Size(4.6f, 8f), style = Stroke(1.4f))
        }
        TokenState.OPEN -> Unit
    }
    if (maps > 0) {
        val box = Rect(c.x + r * .55f - 1, c.y - r - 5, c.x + r * .55f + 26, c.y - r + 11)
        drawRoundRect(Color(0xFF141B2E), box.topLeft, box.size, CornerRadius(4f))
        drawRoundRect(ModBlue.copy(alpha = .7f), box.topLeft, box.size, CornerRadius(4f), style = Stroke(1f))
    }
    if (selected) drawCircle(GoldBright, r + 7, c, style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))))
}

private fun radius(token: WorldToken): Float = if (token.zone.finale) FINALE_RADIUS else TOKEN_RADIUS

/**
 * The map's words, laid out once per world and only scaled with the zoom: each token's level and
 * name, the regions the hero knows with their levels, and «Неизведанное» in the fog.
 */
private class Words(private val world: WorldMap, private val measurer: TextMeasurer) {
    private val levels: Map<String, TextLayoutResult>
    private val names: Map<String, TextLayoutResult>
    private val regions: List<Triple<Offset, TextLayoutResult, TextLayoutResult>>
    private val unknown: TextLayoutResult

    init {
        val shade = Shadow(Color.Black, Offset(0f, 1f), 6f)
        levels = world.tokens.associate { token ->
            token.zone.code to measurer.measure(token.zone.level.toString(), TextStyle(
                color = if (token.state == TokenState.LOCKED) Color(0xFFA4A9AE) else GoldBright,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = if (token.zone.finale) 21.sp else 17.sp,
                shadow = Shadow(Color.Black.copy(alpha = .9f), Offset(0f, 1f), 2f)))
        }
        names = world.tokens.associate { token ->
            token.zone.code to measurer.measure(if (token.state == TokenState.LOCKED) ui("expedition.hidden") else mapTitle(token.zone.code), TextStyle(
                color = if (token.state == TokenState.LOCKED) Color(0xFFA4A9AE) else Parchment,
                fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, shadow = shade))
        }
        regions = world.knownRegions.map { region ->
            val levels = region.zones.map { it.level }
            val name = measurer.measure(regionTitle(region.code).uppercase(), TextStyle(color = Parchment.copy(alpha = .45f), fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, letterSpacing = 5.sp))
            val span = measurer.measure(ui("expedition.region_levels", levels.min(), levels.max()).uppercase(), TextStyle(color = Gold.copy(alpha = .6f),
                fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 3.sp))
            Triple(Offset(region.label.x.toFloat(), region.label.y.toFloat()), name, span)
        }
        unknown = measurer.measure(ui("expedition.fog").uppercase(), TextStyle(color = Muted.copy(alpha = .7f), fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = 6.sp))
    }

    fun draw(scope: DrawScope, camera: WorldCamera, stash: Map<String, Int>) = with(scope) {
        val zoom = camera.scale
        val nameAlpha = ((zoom - WorldCamera.NAMES_FROM + .15f) / .15f).coerceIn(0f, 1f)
        regions.forEach { (at, name, span) ->
            val p = camera.toScreen(at.x, at.y)
            place(name, p, zoom, 1f, centreY = true)
            place(span, p + Offset(0f, name.size.height * .5f * zoom + 2 * camera.unit), zoom, 1f)
        }
        val fogAt = world.fogLine + FOG_WORD
        if (fogAt < camera.world.height) place(unknown, camera.toScreen(camera.world.width / 2f, fogAt.toFloat()), zoom, 1f, centreY = true)
        world.tokens.forEach { token ->
            val zone = token.zone
            val r = radius(token)
            levels[zone.code]?.let { place(it, camera.toScreen(zone.x.toFloat(), zone.y.toFloat()), zoom, 1f, centreY = true) }
            if (nameAlpha > 0f) names[zone.code]?.let { place(it, camera.toScreen(zone.x.toFloat(), zone.y - r - 5), zoom, nameAlpha) }
            val maps = stash[zone.code] ?: 0
            if (maps > 0) {
                val count = measurer.measure("×$maps", TextStyle(color = ModBlue, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                place(count, camera.toScreen(zone.x + r * .55f + 12.5f, zone.y + r - 3), zoom, 1f, centreY = true)
            }
        }
    }

    /** Draws [text] centred across [anchor] — below it, or centred on it too — grown by [factor] about it. */
    private fun DrawScope.place(text: TextLayoutResult, anchor: Offset, factor: Float, alpha: Float, centreY: Boolean = false) {
        val topLeft = Offset(anchor.x - text.size.width / 2f, if (centreY) anchor.y - text.size.height / 2f else anchor.y)
        if (factor == 1f) drawText(text, topLeft = topLeft, alpha = alpha)
        else withTransform({ scale(factor, factor, anchor) }) { drawText(text, topLeft = topLeft, alpha = alpha) }
    }
}

private val ROAD_INK = Color(0xD9120D07)
private val ROAD_GOLD = Color(0xFFB89A62)
private val FOG = Color(0xF7080A0D)
private const val TOKEN_RADIUS = 23f
private const val FINALE_RADIUS = 29f
/** How far from a token's centre a tap still takes it, in world units, and at least this many dp on the screen. */
private const val TOKEN_REACH = 34f
private const val FINGER = 24f
/** The fog is clear up to so far above the highest seen token, and thick from so far. */
private const val FOG_CLEAR = 60f
private const val FOG_THICK = 280f
private const val FOG_WORD = 190
private const val PUFFS = 26
private const val PULSE_MS = 2400
private const val MARCH_MS = 1200
private const val DASH_PERIOD = 13f
