package com.sperance.exileforge.ui.screens.expedition.world

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.RoadState
import com.sperance.exileforge.core.campaign.TokenState
import com.sperance.exileforge.core.campaign.WorldMap
import com.sperance.exileforge.core.campaign.WorldToken
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.rules.content.CampaignFile
import com.sperance.exileforge.rules.content.WorldGraph
import com.sperance.exileforge.ui.components.motionPhase
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.ModBlue
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.hypot

/**
 * The world map itself: the parchment, the fog above the frontier, the roads between the
 * tokens and the tokens — bronze medallions with the zone's level, the passed ones ticked, the open
 * ones glowing, the «???» behind them dark and locked. A drag moves it, a pinch zooms it; a tap on a
 * token picks the zone, a tap on the land lets it go. Everything but the words is drawn in world
 * units under one transform; the words are laid out once and only scaled.
 *
 * [story] - зона шага сюжета, вокруг жетона кольцо; null - сюжета на карте нет. Без анимаций жетоны и дороги стоят; общие часы декора, а не
 * свой бесконечный переход. Пергамент записан один раз в свой слой ([LocalWorldArtLayer], живёт с оболочкой); пергамент, туман и дороги
 * записаны в неподвижный слой карты ([StillLayer]), камера его не перерисовывает, а лишь сдвигает и масштабирует. Жетоны, ореолы, кольцо
 * сюжета и надписи - свои холсты: их меняют выбор, метки тайника и часы декора; часы будят только дышащее и только на экране. Пока арт
 * не собран - голый пергамент с дорогами и жетонами.
 */
@Composable
fun WorldCanvas(
    chart: WorldChart,
    art: WorldArt?,
    camera: WorldCamera,
    selected: String?,
    stash: Map<String, Int>,
    modifier: Modifier = Modifier,
    story: String? = null,
    onTap: (String?) -> Unit,
) {
    val world = chart.world
    val pulse by motionPhase(PULSE_MS, still = .5f)
    val marchPhase by motionPhase(MARCH_MS)
    val spin by motionPhase(STORY_SPIN_MS)
    val height = camera.world.height.toFloat()
    val roads = chart.roads
    val ahead = remember(chart) { roads.filter { it.state == RoadState.AHEAD } }
    val open = remember(chart) { world.tokens.filter { it.state == TokenState.OPEN } }
    val fog = chart.fog
    val words = chart.words
    val artLayer = LocalWorldArtLayer.current ?: rememberWorldArtLayer()
    val stillLayer = rememberStillLayer()
    val tap by rememberUpdatedState(onTap)
    val description = ui("expedition.world_map")
    Box(
        modifier
            .clipToBounds()
            .semantics { contentDescription = description }
            .onSizeChanged { camera.viewport = it }
            .pointerInput(camera) { detectTransformGestures { centroid, pan, zoom, _ -> camera.transform(centroid, pan, zoom) } }
            .pointerInput(camera, world) { detectTapGestures { point -> tap(hit(world, camera, point)) } },
    ) {
        Canvas(
            Modifier.matchParentSize().graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = camera.offset.x
                translationY = camera.offset.y
                scaleX = camera.unit
                scaleY = camera.unit
            },
        ) { drawLayer(with(stillLayer) { of(chart, art, artLayer) }) }
        Canvas(Modifier.matchParentSize()) {
            if (ahead.isEmpty() && open.isEmpty()) return@Canvas
            val view = camera.view(size)
            onWorld(camera) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(7f, 6f), -DASH_PERIOD * marchPhase)
                val march = Stroke(2.2f, cap = StrokeCap.Round, pathEffect = dash)
                ahead.forEach { road -> if (road.bounds.overlaps(view)) drawPath(road.path, GoldBright, style = march) }
                val beat = pulse
                open.forEach { token -> if (token.seen(view, height)) openPulse(token, height, beat) }
            }
        }
        Canvas(Modifier.matchParentSize()) {
            val view = camera.view(size)
            onWorld(camera) {
                world.tokens.forEach { if (it.seen(view, height)) token(it, height, it.zone.code.value == selected, stash[it.zone.code.value] ?: 0) }
            }
        }
        if (story != null) {
            val token = remember(world, story) { world.tokens.firstOrNull { it.zone.code.value == story } }
            if (token != null) Canvas(Modifier.matchParentSize()) { onWorld(camera) { storyRing(token, height, spin) } }
        }
        Canvas(Modifier.matchParentSize()) { words.draw(this, camera, stash) }
    }
}

/**
 * Карта мира, готовая к рисованию: жетоны и дороги ([world]), пути дорог, туман и надписи. Строится вне главного потока ([of]) - мир,
 * туман и мерка надписей держали вкладку на открытии; вход с тем же прогрессом, языком и плотностью экрана берёт последнюю ([cached]).
 * Надписи жетонов меряются при первом появлении на экране.
 */
class WorldChart private constructor(val world: WorldMap, internal val roads: List<Road>, internal val fog: Fog, internal val words: Words) {
    /** Из чего собрана карта: её же собирать заново не нужно. */
    private class Source(val campaign: CampaignFile, val graph: WorldGraph, val progress: CampaignProgress, val look: List<Any>) {
        fun fits(other: Source) = campaign === other.campaign && graph === other.graph && progress == other.progress && look == other.look
    }

    companion object {
        @Volatile private var last: Pair<Source, WorldChart>? = null
        private val building = Mutex()

        private fun source(campaign: CampaignFile, graph: WorldGraph, progress: CampaignProgress, density: Density) = Source(campaign, graph, progress, listOf(density.density, density.fontScale, uiLanguage))

        /** Карта, если она уже собрана, - для первого кадра. */
        fun cached(campaign: CampaignFile, graph: WorldGraph, progress: CampaignProgress, density: Density): WorldChart? {
            val source = source(campaign, graph, progress, density)
            return last?.takeIf { it.first.fits(source) }?.second
        }

        /** Карта, собранная вне главного потока; надписи меряет [measurer]. */
        suspend fun of(campaign: CampaignFile, graph: WorldGraph, progress: CampaignProgress, measurer: TextMeasurer, density: Density): WorldChart {
            val source = source(campaign, graph, progress, density)
            return cached(campaign, graph, progress, density) ?: building.withLock {
                last?.takeIf { it.first.fits(source) }?.second
                    ?: withContext(Dispatchers.Default) { build(source, measurer) }.also { last = source to it }
            }
        }

        private fun build(source: Source, measurer: TextMeasurer): WorldChart {
            val world = WorldMap(source.campaign, source.graph, source.progress)
            val width = source.campaign.world.width.toFloat()
            val height = source.campaign.world.height.toFloat()
            val roads = world.roads.map {
                Road(it.state, road(it.from.zone.x, height - it.from.zone.y, it.to.zone.x, height - it.to.zone.y, it.from.zone.code.value + it.to.zone.code.value))
            }
            return WorldChart(world, roads, Fog.of(world, width, height), Words(world, measurer))
        }
    }
}

/** Рисунок в мере мира: перенос камеры и её масштаб. */
private inline fun DrawScope.onWorld(camera: WorldCamera, block: DrawScope.() -> Unit) = withTransform({
    translate(camera.offset.x, camera.offset.y)
    scale(camera.unit, camera.unit, Offset.Zero)
}, block)

/** Что видно на холсте [size] в мере мира (y сверху, как рисует холст), с запасом на ореолы и подписи. */
private fun WorldCamera.view(size: Size): Rect {
    val unit = unit
    val margin = VIEW_MARGIN
    return Rect(-offset.x / unit - margin, -offset.y / unit - margin, (size.width - offset.x) / unit + margin, (size.height - offset.y) / unit + margin)
}

/** Жетон хоть краем в [view]. */
private fun WorldToken.seen(view: Rect, height: Float): Boolean = view.contains(Offset(zone.x.toFloat(), height - zone.y))

/** Дорога: её состояние, путь в мере мира и рамка пути. */
internal class Road(val state: RoadState, val path: Path) {
    val bounds: Rect = path.getBounds()
}

/**
 * Туман над тем, куда герой ещё не дошёл: чистый под фронтиром, густой на несколько рядов выше. Кисти - раз на карту: полоса
 * и одно пятно единичного радиуса, растянутое на каждый клуб.
 */
internal class Fog(private val width: Float, private val clearTo: Float, private val band: Brush?, private val puffs: FloatArray) {
    fun draw(scope: DrawScope) = with(scope) {
        band ?: return@with
        drawRect(band, size = Size(width, clearTo))
        for (i in 0 until puffs.size / 3) {
            val x = puffs[i * 3]
            val y = puffs[i * 3 + 1]
            val r = puffs[i * 3 + 2]
            withTransform({
                translate(x, y)
                scale(r, r, Offset.Zero)
            }) { drawCircle(PUFF, 1f, Offset.Zero) }
        }
    }

    companion object {
        private val PUFF = Brush.radialGradient(listOf(FOG.copy(alpha = .85f), FOG.copy(alpha = 0f)), Offset.Zero, 1f)

        fun of(world: WorldMap, width: Float, height: Float): Fog {
            val clearTo = height - (world.fogLine + FOG_CLEAR)
            if (clearTo <= 0f) return Fog(width, clearTo, null, FloatArray(0))
            val thickFrom = (height - (world.fogLine + FOG_THICK)).coerceAtLeast(0f)
            val band = Brush.verticalGradient(0f to FOG, (thickFrom / clearTo) to FOG.copy(alpha = .93f), 1f to FOG.copy(alpha = 0f), startY = 0f, endY = clearTo)
            val seed = world.fogLine
            val puffs = FloatArray(PUFFS * 3)
            repeat(PUFFS) { i ->
                puffs[i * 3] = (Math.floorMod(seed * 31 + i * 977, width.toInt().coerceAtLeast(1))).toFloat()
                puffs[i * 3 + 1] = thickFrom + (Math.floorMod(seed * 17 + i * 613, 1000) / 1000f) * (clearTo - thickFrom)
                puffs[i * 3 + 2] = 50f + Math.floorMod(i * 389 + seed, 90)
            }
            return Fog(width, clearTo, band, puffs)
        }
    }
}

/** Ореол и пульс открытого жетона: под его медальоном, на часах декора. */
private fun DrawScope.openPulse(token: WorldToken, height: Float, pulse: Float) {
    val c = Offset(token.zone.x.toFloat(), height - token.zone.y)
    val r = radius(token)
    withTransform({
        translate(c.x, c.y)
        scale(r * 2f, r * 2f, Offset.Zero)
    }) { drawCircle(OPEN_GLOW, 1f, Offset.Zero) }
    drawCircle(GoldBright.copy(alpha = (1 - pulse) * .7f), r + 5 + pulse * 10, c, style = PULSE_RING)
}

/** The token under a tap, generous enough for a finger at any zoom. */
private fun hit(world: WorldMap, camera: WorldCamera, point: Offset): String? {
    val at = camera.toWorld(point)
    val reach = maxOf(TOKEN_REACH, FINGER / camera.scale)
    return world.tokens.map { it to hypot(it.zone.x - at.x, it.zone.y - at.y) }.filter { it.second <= reach }.minByOrNull { it.second }?.first?.zone?.code?.value
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

/** One token: its medallion by state, the finale's crown, its tick or lock, the stash's maps for it, and the pick's ring. */
private fun DrawScope.token(token: WorldToken, height: Float, selected: Boolean, maps: Int) {
    val c = Offset(token.zone.x.toFloat(), height - token.zone.y)
    val r = radius(token)
    val (light, mid, dark, ring) = when (token.state) {
        TokenState.PASSED -> listOf(Color(0xFF8D7447), Color(0xFF54432A), Color(0xFF2B2216), Color(0xFFB89A62))
        TokenState.OPEN -> listOf(Color(0xFFC9A868), Color(0xFF7B6038), Color(0xFF3B2D19), GoldBright)
        TokenState.LOCKED -> listOf(Color(0xFF41444A), Color(0xFF202227), Color(0xFF141518), Color(0xFF5A5F66))
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
            moveTo(c.x - 9, top + 6)
            lineTo(c.x - 10, top - 3)
            lineTo(c.x - 4.5f, top + 1)
            lineTo(c.x, top - 6)
            lineTo(c.x + 4.5f, top + 1)
            lineTo(c.x + 10, top - 3)
            lineTo(c.x + 9, top + 6)
            close()
        }
        drawPath(crown, GoldBright)
        drawPath(crown, Color(0xFF2B2216), style = Stroke(1.2f, join = StrokeJoin.Round))
    }
    val badge = c + Offset(r * .72f, r * .72f)
    when (token.state) {
        TokenState.PASSED -> {
            drawCircle(Color(0xFF1D170E), 8.5f, badge)
            drawCircle(Gold, 8.5f, badge, style = Stroke(1f))
            drawPath(
                Path().apply {
                    moveTo(badge.x - 4, badge.y + .5f)
                    lineTo(badge.x - 1, badge.y + 3.5f)
                    lineTo(badge.x + 4.5f, badge.y - 3)
                },
                GoldBright,
                style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
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

/**
 * Руническое кольцо зоны сюжета: засечки по кругу вокруг жетона медленно вращаются
 * (доля круга [spin]); без анимаций стоят.
 */
private fun DrawScope.storyRing(token: WorldToken, height: Float, spin: Float) {
    val c = Offset(token.zone.x.toFloat(), height - token.zone.y)
    val r = radius(token) + STORY_RING_GAP
    drawCircle(GoldBright.copy(alpha = .25f), r, c, style = Stroke(1f))
    rotate(spin * 360f, c) {
        repeat(STORY_RUNES) { i ->
            val a = (i * 2 * Math.PI / STORY_RUNES).toFloat()
            val dir = Offset(kotlin.math.cos(a), kotlin.math.sin(a))
            val long = i % 3 == 0
            drawLine(GoldBright, c + dir * (r - if (long) 5f else 3f), c + dir * (r + if (long) 5f else 3f), strokeWidth = if (long) 2f else 1.2f, cap = StrokeCap.Round)
        }
    }
}

/** Засечек в кольце сюжета, отступ кольца от жетона и полный оборот, мс. */
private const val STORY_RUNES = 24
private const val STORY_RING_GAP = 12f
private const val STORY_SPIN_MS = 14_000

private fun radius(token: WorldToken): Float = if (token.zone.finale) FINALE_RADIUS else TOKEN_RADIUS

/**
 * The map's words, laid out once per world and only scaled with the zoom: each token's level and
 * name, the regions the hero knows with their levels, and «Неизведанное» in the fog. A token's words are
 * laid out the first time it comes on screen, and one off screen is not drawn: the map opens without measuring the world.
 */
internal class Words(val world: WorldMap, private val measurer: TextMeasurer) {
    private val shade = Shadow(Color.Black, Offset(0f, 1f), 6f)
    private val levels = HashMap<String, TextLayoutResult>()
    private val names = HashMap<String, TextLayoutResult>()
    private val counts = HashMap<Int, TextLayoutResult>()
    private val regions: List<Triple<Offset, TextLayoutResult, TextLayoutResult>> = world.knownRegions.map { region ->
        val levels = region.zones.map { it.level }
        val name = measurer.measure(
            regionTitle(region.code).uppercase(),
            TextStyle(
                color = Parchment.copy(alpha = .45f),
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 21.sp,
                letterSpacing = 5.sp,
            ),
        )
        val span = measurer.measure(
            ui("expedition.region_levels", levels.min(), levels.max()).uppercase(),
            TextStyle(
                color = Gold.copy(alpha = .6f),
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
            ),
        )
        Triple(Offset(region.label.x.toFloat(), region.label.y.toFloat()), name, span)
    }
    private val unknown: TextLayoutResult = measurer.measure(
        ui("expedition.fog").uppercase(),
        TextStyle(
            color = Muted.copy(alpha = .7f),
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            letterSpacing = 6.sp,
        ),
    )

    private fun level(token: WorldToken) = levels.getOrPut(token.zone.code.value) {
        measurer.measure(
            token.zone.level.toString(),
            TextStyle(
                color = if (token.state == TokenState.LOCKED) Color(0xFFA4A9AE) else GoldBright,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = if (token.zone.finale) 21.sp else 17.sp,
                shadow = Shadow(Color.Black.copy(alpha = .9f), Offset(0f, 1f), 2f),
            ),
        )
    }

    private fun name(token: WorldToken) = names.getOrPut(token.zone.code.value) {
        measurer.measure(
            if (token.state == TokenState.LOCKED) ui("expedition.hidden") else mapTitle(token.zone.code),
            TextStyle(
                color = if (token.state == TokenState.LOCKED) Color(0xFFA4A9AE) else Parchment,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                shadow = shade,
            ),
        )
    }

    private fun count(maps: Int) = counts.getOrPut(maps) { measurer.measure("×$maps", TextStyle(color = ModBlue, fontWeight = FontWeight.Bold, fontSize = 10.sp)) }

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
        val margin = OFFSCREEN_MARGIN * camera.unit
        world.tokens.forEach { token ->
            val zone = token.zone
            val centre = camera.toScreen(zone.x.toFloat(), zone.y.toFloat())
            if (centre.x < -margin || centre.x > size.width + margin || centre.y < -margin || centre.y > size.height + margin) return@forEach
            val r = radius(token)
            place(level(token), centre, zoom, 1f, centreY = true)
            if (nameAlpha > 0f) place(name(token), camera.toScreen(zone.x.toFloat(), zone.y - r - 5), zoom, nameAlpha)
            val maps = stash[zone.code.value] ?: 0
            if (maps > 0) place(count(maps), camera.toScreen(zone.x + r * .55f + 12.5f, zone.y + r - 3), zoom, 1f, centreY = true)
        }
    }

    /** Draws [text] centred across [anchor] — below it, or centred on it too — grown by [factor] about it. */
    private fun DrawScope.place(text: TextLayoutResult, anchor: Offset, factor: Float, alpha: Float, centreY: Boolean = false) {
        val topLeft = Offset(anchor.x - text.size.width / 2f, if (centreY) anchor.y - text.size.height / 2f else anchor.y)
        if (factor == 1f) {
            drawText(text, topLeft = topLeft, alpha = alpha)
        } else {
            withTransform({ scale(factor, factor, anchor) }) { drawText(text, topLeft = topLeft, alpha = alpha) }
        }
    }
}

private val BARE = Color(0xFF1B150D)
private val ROAD_INK = Color(0xD9120D07)
private val ROAD_GOLD = Color(0xFFB89A62)
private val FOG = Color(0xF7080A0D)
private const val TOKEN_RADIUS = 23f

/** How far past the screen's edge, in world units, a token's words are still drawn: a name is wider than its medallion. */
private const val OFFSCREEN_MARGIN = 120f
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

/** Запас видимого в мере мира: ореолы, кольца и подписи шире жетона. */
private const val VIEW_MARGIN = 140f

private val OPEN_GLOW = Brush.radialGradient(listOf(GoldBright.copy(alpha = .45f), GoldBright.copy(alpha = 0f)), Offset.Zero, 1f)
private val PULSE_RING = Stroke(1.5f)
private val WALKED_INK = Stroke(7f, cap = StrokeCap.Round)
private val WALKED_GOLD = Stroke(2.4f, cap = StrokeCap.Round)
private val UNTRODDEN_INK = Muted.copy(alpha = .85f)
private val UNTRODDEN = Stroke(1.8f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 7f)))

/**
 * Слой пергамента карты мира: записывается один раз на рисунок [WorldArt] и переживает уход с вкладки - его держит оболочка
 * игры ([LocalWorldArtLayer]). Запись - при первом рисовании, дальше только воспроизведение.
 */
class WorldArtLayer internal constructor(private val layer: GraphicsLayer) {
    private var recorded: WorldArt? = null

    /** Слой с рисунком [art] размера [size]: другой рисунок пишется заново, тот же - как есть. */
    fun DrawScope.of(art: WorldArt, size: IntSize): GraphicsLayer {
        if (recorded !== art) {
            layer.record(size) { art.draw(this) }
            recorded = art
        }
        return layer
    }
}

/**
 * Неподвижный слой карты мира в мере мира: пергамент ([WorldArtLayer]), туман и дороги. Пишется при первом рисовании новой карты
 * ([WorldChart]) или пришедшего пергамента, дальше камера только воспроизводит его своим переносом и масштабом. Слой - запись команд
 * рисования, а не растр: под масштабом камеры дороги и туман растеризуются в разрешении экрана, на любом приближении без мыла, и памяти
 * под пиксели мира слой не держит. Живёт с холстом карты ([rememberStillLayer]): уход с вкладки его освобождает.
 */
internal class StillLayer(private val layer: GraphicsLayer) {
    private var chart: WorldChart? = null
    private var art: WorldArt? = null

    /** Слой карты [chart] на пергаменте [art] (null - голый пергамент): та же пара - как есть, другая пишется заново. */
    fun DrawScope.of(chart: WorldChart, art: WorldArt?, artLayer: WorldArtLayer): GraphicsLayer {
        if (this@StillLayer.chart === chart && this@StillLayer.art === art) return layer
        val world = chart.world.campaign.world
        val size = IntSize(world.width, world.height)
        val parchment = art?.let { with(artLayer) { of(it, size) } }
        layer.record(size) {
            if (parchment == null) drawRect(BARE) else drawLayer(parchment)
            chart.fog.draw(this)
            chart.roads.forEach { road ->
                when (road.state) {
                    RoadState.WALKED -> {
                        drawPath(road.path, ROAD_INK, style = WALKED_INK)
                        drawPath(road.path, ROAD_GOLD, style = WALKED_GOLD)
                    }

                    RoadState.AHEAD -> drawPath(road.path, ROAD_INK, style = WALKED_INK)

                    RoadState.UNTRODDEN -> drawPath(road.path, UNTRODDEN_INK, style = UNTRODDEN)
                }
            }
        }
        this@StillLayer.chart = chart
        this@StillLayer.art = art
        return layer
    }
}

/** Неподвижный слой карты, что живёт, пока жив холст. */
@Composable private fun rememberStillLayer(): StillLayer {
    val layer = rememberGraphicsLayer()
    return remember(layer) { StillLayer(layer) }
}

/** Слой пергамента оболочки игры; null - карта рисуется вне неё (превью) и держит свой. */
val LocalWorldArtLayer = staticCompositionLocalOf<WorldArtLayer?> { null }

/** Слой пергамента, что живёт, пока жив вызвавший. */
@Composable fun rememberWorldArtLayer(): WorldArtLayer {
    val layer = rememberGraphicsLayer()
    return remember(layer) { WorldArtLayer(layer) }
}
