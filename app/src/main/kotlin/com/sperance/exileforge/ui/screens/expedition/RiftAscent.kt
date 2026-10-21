package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.expedition.RiftChart
import com.sperance.exileforge.core.expedition.RiftSpot
import com.sperance.exileforge.core.expedition.RiftTrail
import com.sperance.exileforge.core.expedition.RiftWalk
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.rules.content.RiftMetal
import com.sperance.exileforge.rules.content.RiftNodeKind
import com.sperance.exileforge.rules.content.RiftReward
import com.sperance.exileforge.rules.content.RiftRules
import com.sperance.exileforge.rules.rift.RiftEngine
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.motionPhase
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Карта Разлома «Восхождение» по карте забега [chart]: извилистый остров-тропа над пустотой в изометрии Похода ([RiftIsle]), все акты
 * снизу вверх, страж - ворота поперёк тропы между актами. Узлы - объекты вдоль тропы с развилками, над узлом впереди - значок его
 * главной награды; тропы - жилы: пройденная горит и течёт, доступная течёт мягче. Дальше обзора клубится туман, в нём видны только
 * силуэты стражей, значков нет. Фигурка героя [hero] стоит на своём узле и идёт по тропе к выбранному ([walk]); лента сама держит героя
 * в нижней трети, свободной камеры нет. Декор стоит при выключенном `LocalMotion`.
 *
 * Раскладка собирается раз на схему и ширину, земля ([IsleArt]) - вне главного потока и пишется в неподвижный слой ([IsleGround]);
 * жилы, пути и перья - раз на свои входы. Фазы декора и шаг героя читаются в рисовании и в слоях - кадр не перекомпонует экран и не
 * перерисовывает землю.
 */
@Composable internal fun RiftAscent(
    rules: RiftRules,
    engine: RiftEngine,
    chart: RiftChart,
    walk: RiftWalk?,
    idle: Boolean,
    modifier: Modifier = Modifier,
    top: Dp = 0.dp,
    bottom: Dp = 0.dp,
    hero: Boolean = true,
    onNode: (RiftSpot) -> Unit,
) {
    val plan = engine.plan
    val depths = rules.acts * (rules.layout.rows + 1)
    val flow = motionPhase(FLOW_MS)
    val breath = motionPhase(BREATH_MS)
    val drift = motionPhase(DRIFT_MS)
    val scroll = rememberScrollState()
    val motion = LocalMotion.current
    val step = remember { Animatable(1f) }
    LaunchedEffect(walk, motion) {
        if (walk == null || !motion) return@LaunchedEffect step.snapTo(1f)
        step.snapTo(0f)
        step.animateTo(1f, tween(RiftWalk.STEP_MS, easing = LinearEasing))
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width = maxWidth
        val viewport = maxHeight
        val isle = remember(plan, width, top) { RiftIsle.of(plan, engine::depth, depths, rules.layout.width.max(), width.value, top.value) }
        val scale = LocalDensity.current.density
        val art by produceState<IsleArt?>(null, isle, scale) {
            value = null
            value = withContext(Dispatchers.Default) { IsleArt.of(isle, scale) }
        }
        val veins = remember(isle, chart.trails, scale) { chart.trails.map { Vein(isle.trail(it.from, it.to).path(scale), it) } }
        val route = remember(isle, walk) { walk?.let { isle.trail(it.from, it.to) } }
        val stride = remember(route, scale) { route?.let { Stride(it.path(scale)) } }
        val here = chart.here?.id
        val anchor = walk?.to ?: here
        LaunchedEffect(anchor, viewport, isle) {
            snapshotFlow { scroll.maxValue }.first { it > 0 }
            val target = (isle.at(anchor).y - viewport.value * .66f) * scale
            scroll.animateScrollTo(target.toInt().coerceIn(0, scroll.maxValue))
        }
        val order = remember(chart, isle) { chart.spots.filter { it.shown }.sortedBy { isle.at(it.node.id).y } }
        Box(Modifier.fillMaxWidth().verticalScroll(scroll)) {
            Box(Modifier.width(width).height(isle.height.dp + bottom)) {
                IsleGround(art, Modifier.fillMaxSize())
                Spacer(
                    Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                        val pen = VeinPen(scale)
                        val reach = HALO * scale
                        val halo = Brush.radialGradient(listOf(RiftColors.Rift.copy(alpha = .16f), Color.Transparent), Offset.Zero, reach)
                        val entry = isle.entry * scale
                        val fog = if (chart.edge >= depths - 1) null else Fog((isle.rowY(chart.edge) - FOG_LIFT) * scale, size.width, scale)
                        onDrawBehind {
                            val at = (route?.at(step.value) ?: isle.at(here)) * scale
                            if (hero) translate(at.x, at.y) { drawCircle(halo, reach, Offset.Zero) }
                            portal(entry, breath.value)
                            val current = pen.flow(flow.value)
                            veins.forEach { vein(it, pen, current) }
                            stride?.let { pace(it, step.value, pen) }
                            fog?.draw(this, drift.value)
                        }
                    },
                )
                order.forEach { spot ->
                    key(spot.node.id) {
                        NodeMark(
                            spot,
                            open = spot.open && idle,
                            breath = breath,
                            name = if (spot.silhouette) rules.guardians.monsters.getOrNull(spot.node.act)?.let { loc("monster.${it.value}.name") } else null,
                            at = isle.at(spot.node.id),
                        ) { onNode(spot) }
                    }
                }
                if (hero) {
                    HeroFigure(
                        Modifier.graphicsLayer {
                            val t = step.value
                            val at = route?.at(t) ?: isle.at(here)
                            translationX = (at.x - HERO_W / 2) * scale
                            translationY = (at.y - HERO_H + HERO_FOOT - if (route != null) abs(sin(t * PI * 3)).toFloat() * 2f else 0f) * scale
                        },
                    )
                }
            }
        }
    }
}

/**
 * Земля острова: пустота и неподвижный слой рисунка [art], записанный раз на рисунок - кадры жил и тумана его только воспроизводят;
 * до готовности рисунка - одна пустота.
 */
@Composable private fun IsleGround(art: IsleArt?, modifier: Modifier) {
    Spacer(
        modifier.drawWithCache {
            val void = Brush.verticalGradient(listOf(RiftColors.Ground, RiftColors.Ground2, RiftColors.Ground))
            val layer = art?.let { ground -> obtainGraphicsLayer().apply { record { ground.draw(this) } } }
            onDrawBehind {
                drawRect(void)
                layer?.let { drawLayer(it) }
            }
        },
    )
}

/** Жила тропы [trail], собранная раз на раскладку: путь [path] в пикселях. */
private class Vein(val path: Path, val trail: RiftTrail)

/** Путь шага героя [path] в пикселях, собранный раз на шаг: мера пути и пройденный кусок, что кадр переписывает на месте. */
private class Stride(val path: Path) {
    private val measure = PathMeasure().apply { setPath(path, false) }
    private val done = Path()

    /** Пройденная доля [t] пути. */
    fun walked(t: Float): Path = done.apply {
        reset()
        measure.getSegment(0f, measure.length * t, this, true)
    }
}

/** Перья жил и шага героя при [scale] пикселей на dp, собранные раз на кэш рисования; течение - одно на кадр ([flow]). */
private class VeinPen(scale: Float) {
    val rut = Stroke(9 * scale, cap = StrokeCap.Round)
    val walked = Stroke(3.2f * scale, cap = StrokeCap.Round)
    val open = Stroke(2 * scale, cap = StrokeCap.Round)
    val behind = Stroke(1.4f * scale, cap = StrokeCap.Round)
    val dim = Stroke(1.8f * scale, cap = StrokeCap.Round)
    val route = Stroke(2.4f * scale, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5 * scale, 5 * scale)))
    val stride = Stroke(3 * scale, cap = StrokeCap.Round)
    private val flowWidth = 1.6f * scale
    private val dash = floatArrayOf(4 * scale, 8 * scale)
    private val travel = 24 * scale

    /** Течение жил на фазе [phase]. */
    fun flow(phase: Float): Stroke = Stroke(flowWidth, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(dash, -phase * travel))
}

/** Жила тропы: под ней тёмная колея; пройденная горит и течёт ([flow]), доступная течёт мягче, позади - тусклая, прочие - нить. */
private fun DrawScope.vein(vein: Vein, pen: VeinPen, flow: Stroke) {
    val path = vein.path
    val trail = vein.trail
    drawPath(path, Rut, style = pen.rut)
    if (trail.walked) drawPath(path, VeinGlow, style = pen.rut)
    when {
        trail.walked -> drawPath(path, RiftColors.Rift, style = pen.walked)
        trail.open -> drawPath(path, RiftColors.Soft, style = pen.open)
        trail.behind -> drawPath(path, VeinBehind, style = pen.behind)
        else -> drawPath(path, VeinDim, style = pen.dim)
    }
    if (trail.walked || trail.open) drawPath(path, if (trail.walked) RiftColors.Hot else RiftColors.Rift, style = flow)
}

/** Шаг героя [stride] на доле [t]: весь путь - пунктир золотом, пройденное - сплошным. */
private fun DrawScope.pace(stride: Stride, t: Float, pen: VeinPen) {
    drawPath(stride.path, RiftColors.Pick.copy(alpha = .85f), style = pen.route)
    drawPath(stride.walked(t), RiftColors.Pick, style = pen.stride)
}

/** Вход в Разлом под первым рядом: кольцо портала на земле, ядро дышит. */
private fun DrawScope.portal(at: Offset, breath: Float) {
    val rx = 17.dp.toPx()
    val ry = 8.5.dp.toPx()
    val glow = .2f + .15f * (1 - cos(breath * 2 * PI).toFloat()) / 2
    oval(at, Offset(rx * .6f, ry * .6f), RiftColors.Rift.copy(alpha = glow))
    oval(at, Offset(rx, ry), RiftColors.Rift, Stroke(2.dp.toPx()))
}

private val VeinDim = RiftColors.LineHi.copy(alpha = .75f)
private val VeinBehind = RiftColors.Line.copy(alpha = .6f)
private val VeinGlow = RiftColors.Rift.copy(alpha = .25f)
private val Rut = RiftColors.Ground.copy(alpha = .75f)

/**
 * Туман над краем [edge] (px) холста шириной [width] при [scale] пикселей на dp: почти чёрная толща, в ней дрейфуют зелёные клубы,
 * нижняя кромка тает. Клубы и кромка собраны раз на кэш рисования, кадр только двигает клубы.
 */
private class Fog(private val edge: Float, width: Float, scale: Float) {
    private val fade = 56 * scale
    private val sway = 30 * scale
    private val bob = 12 * scale
    private val rim = Brush.verticalGradient(listOf(Color.Transparent, RiftColors.Ground.copy(alpha = .9f), Color.Transparent), edge - fade, edge + fade / 2)
    private val clubs = List(MIST) { k ->
        val r = (90 + k * 23 % 70) * scale
        val y = edge - (k * 61 % 100) / 100f * edge.coerceAtMost(900 * scale)
        Club(width * (.15f + .7f * ((k * 37 % 100) / 100f)), y, r, Brush.radialGradient(MistTones, Offset.Zero, r))
    }

    /** Туман на фазе дрейфа [drift]. */
    fun draw(scope: DrawScope, drift: Float) = with(scope) {
        drawRect(FogBody, size = size.copy(height = edge))
        val turn = drift * 2 * PI
        clubs.forEachIndexed { k, club ->
            val phase = turn + k * 1.7
            translate(club.x + (sin(phase) * sway).toFloat(), club.y + (cos(phase * .7) * bob).toFloat()) { drawCircle(club.brush, club.r, Offset.Zero) }
        }
        drawRect(rim, Offset(0f, edge - fade), size.copy(height = fade * 1.5f))
    }

    /** Клуб тумана: покой в ([x], [y]), радиус [r], заливка [brush] вокруг начала. */
    private class Club(val x: Float, val y: Float, val r: Float, val brush: Brush)
}

private val FogBody = RiftColors.Ground.copy(alpha = .97f)
private val MistTones = listOf(RiftColors.Deep.copy(alpha = .55f), RiftColors.Rift.copy(alpha = .05f), Color.Transparent)

/**
 * Узел на земле в точке [at] (dp холста): плита цвета вида и объект вида - постамент со знаком; страж - ворота поперёк тропы. Доступный
 * ([open]) дышит и пускает кольцо, узел героя подсвечен, пройденный и оставшийся позади притушены; над узлом впереди - значок главной
 * награды ([RiftSpot.reward]); страж в тумане - тёмный силуэт ворот с именем.
 *
 * Узел - свой слой: дыхание читается в его рисовании и слое, без перекомпоновки и без перерисовки соседей.
 */
@Composable private fun NodeMark(spot: RiftSpot, open: Boolean, breath: State<Float>, name: String?, at: Offset, onClick: () -> Unit) {
    val gate = spot.guardian
    val silhouette = spot.silhouette
    val box = if (gate) GateBox else NodeBox
    val tint = if (silhouette) RiftColors.Warn else spot.node.kind.color()
    val fade = when {
        spot.hero || silhouette -> 1f
        spot.walked -> .55f
        spot.passed -> .32f
        else -> 1f
    }

    fun pulse() = (1 - cos(breath.value * 2 * PI).toFloat()) / 2
    Box(
        Modifier.offset((at.x - box.width / 2).dp, (at.y - box.height + box.foot).dp).size(box.width.dp, box.height.dp).graphicsLayer {
            alpha = if (silhouette) .45f + .2f * pulse() else fade
        },
    ) {
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val ground = Offset(size.width / 2, size.height - box.foot.dp.toPx())
                val ring = Offset((if (gate) 50 else 23).dp.toPx(), (if (gate) 22f else 11.5f).dp.toPx())
                val shape = if (gate) gateShape(ground, silhouette) else plinthShape(spot.node.kind, ground, tint)
                onDrawBehind {
                    val phase = breath.value
                    if (!silhouette) {
                        oval(ground, ring, tint.copy(alpha = .13f))
                        oval(ground, ring, tint.copy(alpha = .7f), Stroke(1.4.dp.toPx()))
                    }
                    when {
                        open -> {
                            oval(ground, ring * (.85f + .5f * phase), RiftColors.Rift.copy(alpha = .9f * (1 - phase)), Stroke(2.dp.toPx()))
                            oval(ground, ring, RiftColors.Soft.copy(alpha = .6f), Stroke(2.dp.toPx()))
                        }

                        spot.hero -> oval(ground, ring, RiftColors.Rift.copy(alpha = .15f))

                        spot.walked -> oval(ground, ring * .82f, RiftColors.Rift.copy(alpha = .6f), Stroke(1.2.dp.toPx()))
                    }
                    shape.draw(this)
                }
            },
        )
        Icon(
            spot.node.kind.glyph(),
            null,
            tint = if (silhouette) RiftColors.Dim else tint,
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = -(box.foot + box.lift).dp).size(box.glyph.dp).graphicsLayer {
                val grow = if (open) 1f + .08f * pulse() else 1f
                scaleX = grow
                scaleY = grow
            },
        )
        spot.reward?.let { reward ->
            RewardBadge(reward, spot.key, Modifier.align(Alignment.BottomCenter).offset(x = box.badge.x.dp, y = -(box.foot + box.badge.y).dp))
        }
        name?.let {
            Text(
                it.uppercase(),
                color = RiftColors.Dim,
                style = relicName(11),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomCenter).width((box.width * 1.4f).dp),
            )
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = (box.foot / 2).dp).size(box.hit.x.dp, box.hit.y.dp).clickable(onClick = onClick))
    }
}

/** Рамка узла на карте (dp): [width]×[height], земля на [foot] выше низа, знак [glyph] на [lift] над землёй, значок награды [badge], касание [hit]. */
private class MarkBox(val width: Float, val height: Float, val foot: Float, val lift: Float, val glyph: Float, val badge: Offset, val hit: Offset)

private val NodeBox = MarkBox(68f, 86f, 18f, 16f, 22f, Offset(22f, 48f), Offset(56f, 62f))
private val GateBox = MarkBox(136f, 122f, 24f, 18f, 28f, Offset(60f, 78f), Offset(108f, 92f))

/** Значок главной награды узла: кружок с каймой её цвета и знаком; ключ - знак металла [key]. */
@Composable private fun RewardBadge(reward: RiftReward, key: RiftMetal?, modifier: Modifier) {
    val tint = when (reward) {
        RiftReward.KEY -> key?.color() ?: RiftColors.Hot
        RiftReward.ECHO -> RiftColors.Rift
        RiftReward.BOON -> RiftColors.Chaos
        RiftReward.HONOR -> RiftColors.Honor
    }
    Box(modifier.size(BADGE).background(RiftColors.Ground, CircleShape).border(1.5.dp, tint, CircleShape), contentAlignment = Alignment.Center) {
        when (reward) {
            RiftReward.KEY -> key?.let { KeyMark(it, Modifier.size(9.dp)) }
            RiftReward.ECHO -> Icon(ForgeGlyphs.Rift, null, tint = tint, modifier = Modifier.size(12.dp))
            RiftReward.BOON -> Icon(ForgeGlyphs.Gem, null, tint = tint, modifier = Modifier.size(12.dp))
            RiftReward.HONOR -> Icon(ForgeGlyphs.Kite, null, tint = tint, modifier = Modifier.size(12.dp))
        }
    }
}

/** Фигурка героя: тень, золотой ореол Предначертания, плащ, голова и клинок; ступни - на [HERO_FOOT] выше низа рамки. */
@Composable private fun HeroFigure(modifier: Modifier) {
    Spacer(
        modifier.size(HERO_W.dp, HERO_H.dp).drawWithCache {
            val u = size.width / HERO_W
            val feet = Offset(size.width / 2, size.height - HERO_FOOT * u)
            fun p(x: Float, y: Float) = Offset(feet.x + x * u, feet.y + y * u)
            val cloak = Path().apply {
                p(-7f, 0f).let { moveTo(it.x, it.y) }
                p(-5f, -16f).let { lineTo(it.x, it.y) }
                p(0f, -21f).let { a -> p(5f, -16f).let { b -> quadraticTo(a.x, a.y, b.x, b.y) } }
                p(7f, 0f).let { lineTo(it.x, it.y) }
                p(0f, 2.5f).let { a -> p(-7f, 0f).let { b -> quadraticTo(a.x, a.y, b.x, b.y) } }
                close()
            }
            onDrawBehind {
                oval(feet, Offset(14 * u, 7 * u), RiftColors.Pick.copy(alpha = .28f))
                oval(feet, Offset(8 * u, 3.6f * u), Color.Black.copy(alpha = .55f))
                drawPath(cloak, HeroCloak)
                drawPath(cloak, RiftColors.Pick, style = Stroke(1.3f * u))
                drawLine(HeroSteel, p(6f, -14f), p(13f, -30f), 2f * u, StrokeCap.Round)
                drawLine(RiftColors.Pick, p(4f, -15f), p(9f, -12f), 2f * u)
                drawCircle(HeroSkin, 4.6f * u, p(0f, -21f))
                drawCircle(HeroCloak, 4.6f * u, p(0f, -21f), style = Stroke(1f * u))
            }
        },
    )
}

private val HeroCloak = Color(0xFF1B2620)
private val HeroSteel = Color(0xFFE6EDF6)
private val HeroSkin = Color(0xFFE2BD92)

/** Овал с серединой [c] и полуосями [r]; [style] - заливка или обводка. */
private fun DrawScope.oval(c: Offset, r: Offset, color: Color, style: DrawStyle = Fill) = drawOval(color, Offset(c.x - r.x, c.y - r.y), Size(r.x * 2, r.y * 2), style = style)

/** Объект узла, собранный раз на рамку: грани и обводки по порядку рисования. */
private class MarkShape(private val faces: List<Pair<Path, Color>>, private val lines: List<Pair<Path, Color>>, private val width: Float) {
    fun draw(scope: DrawScope) = with(scope) {
        faces.forEach { (path, color) -> drawPath(path, color) }
        lines.forEach { (path, color) -> drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round)) }
    }
}

/**
 * Изометрический брусок шириной [w] и высотой [h] (px) на точке земли [c]: левая, правая грани и крышка - как плитка земли Похода, вдвое
 * шире, чем выше.
 */
private fun isoBox(c: Offset, w: Float, h: Float, top: Color, left: Color, right: Color): List<Pair<Path, Color>> {
    val a = w / 2
    val b = w / 4
    fun face(p0: Offset, p1: Offset, p2: Offset, p3: Offset) = Path().apply {
        moveTo(p0.x, p0.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        close()
    }
    return listOf(
        face(Offset(c.x - a, c.y - h), Offset(c.x, c.y - h + b), Offset(c.x, c.y + b), Offset(c.x - a, c.y)) to left,
        face(Offset(c.x, c.y - h + b), Offset(c.x + a, c.y - h), Offset(c.x + a, c.y), Offset(c.x, c.y + b)) to right,
        face(Offset(c.x - a, c.y - h), Offset(c.x, c.y - h - b), Offset(c.x + a, c.y - h), Offset(c.x, c.y - h + b)) to top,
    )
}

/** Постамент узла вида [kind] на земле [ground] в цвете вида [tint]: у каждого вида своя ширина и высота, у Кузни - наковальня в два бруска. */
private fun Density.plinthShape(kind: RiftNodeKind, ground: Offset, tint: Color): MarkShape {
    val (w, h) = when (kind) {
        RiftNodeKind.FIGHT -> 22f to 5f
        RiftNodeKind.ELITE -> 22f to 9f
        RiftNodeKind.CHAMPION -> 20f to 12f
        RiftNodeKind.ALTAR -> 24f to 8f
        RiftNodeKind.REST -> 26f to 3f
        RiftNodeKind.TREASURE -> 26f to 10f
        RiftNodeKind.FORGE -> 14f to 6f
        RiftNodeKind.GUARDIAN -> 30f to 12f
    }
    val top = lerp(RiftColors.Panel, tint, .3f)
    val left = lerp(RiftColors.Ground2, tint, .14f)
    val faces = isoBox(ground, w.dp.toPx(), h.dp.toPx(), top, left, RiftColors.Ground) +
        if (kind == RiftNodeKind.FORGE) isoBox(Offset(ground.x, ground.y - h.dp.toPx()), 26.dp.toPx(), 7.dp.toPx(), top, left, RiftColors.Ground) else emptyList()
    return MarkShape(faces, listOf(faces.last().first to tint.copy(alpha = .6f)), 1.dp.toPx())
}

/** Ворота стража на земле [ground]: два столба поперёк тропы, перекладина и дуга угрозы; силуэт [silhouette] - тёмный, без дуги. */
private fun Density.gateShape(ground: Offset, silhouette: Boolean): MarkShape {
    val top = if (silhouette) RiftColors.Ground2 else GateTop
    val left = if (silhouette) RiftColors.Ground else GateLeft
    val right = if (silhouette) RiftColors.Ground else GateRight
    val rim = if (silhouette) RiftColors.Line else RiftColors.Warn.copy(alpha = .6f)
    val pillar = 44.dp.toPx()
    val faces = isoBox(Offset(ground.x - pillar, ground.y), 16.dp.toPx(), 58.dp.toPx(), top, left, right) +
        isoBox(Offset(ground.x + pillar, ground.y), 16.dp.toPx(), 58.dp.toPx(), top, left, right)
    fun p(x: Int, y: Int) = Offset(ground.x + x.dp.toPx(), ground.y + y.dp.toPx())
    val lintel = Path().apply {
        listOf(p(-52, -62), p(0, -80), p(52, -62), p(52, -56), p(0, -74), p(-52, -56)).forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
        close()
    }
    val arch = Path().apply {
        p(-44, -58).let { moveTo(it.x, it.y) }
        p(0, -96).let { a -> p(44, -58).let { b -> quadraticTo(a.x, a.y, b.x, b.y) } }
    }
    val lines = buildList {
        add(lintel to rim)
        if (!silhouette) add(arch to RiftColors.Warn.copy(alpha = .75f))
    }
    return MarkShape(faces + (lintel to top), lines, 1.4.dp.toPx())
}

private val GateTop = Color(0xFF1A2A21)
private val GateLeft = Color(0xFF101C15)
private val GateRight = Color(0xFF0A130E)

/** Знак вида узла вне карты (лист узла): шестигранник-руна цвета вида со знаком внутри, без дыхания. */
@Composable internal fun NodeSeal(kind: RiftNodeKind, modifier: Modifier = Modifier) {
    val tint = kind.color()
    Box(
        modifier.drawWithCache {
            val hex = hexagon(size.center, size.minDimension * .45f)
            onDrawBehind {
                drawPath(hex, RiftColors.Ground2)
                drawPath(hex, tint, style = Stroke(1.5.dp.toPx()))
            }
        },
        contentAlignment = Alignment.Center,
    ) { Icon(kind.glyph(), null, tint = tint, modifier = Modifier.fillMaxSize(.5f)) }
}

/** Шестигранник радиуса [r] вокруг [c], вершиной вбок. */
private fun hexagon(c: Offset, r: Float): Path = Path().apply {
    repeat(6) { k ->
        val a = PI / 3 * k + PI / 6
        val p = Offset(c.x + (cos(a) * r).toFloat(), c.y + (sin(a) * r).toFloat())
        if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/** Знак вида узла. */
internal fun RiftNodeKind.glyph(): ImageVector = when (this) {
    RiftNodeKind.FIGHT -> ForgeGlyphs.Swords
    RiftNodeKind.ELITE -> ForgeGlyphs.Skull
    RiftNodeKind.CHAMPION -> ForgeGlyphs.Banner
    RiftNodeKind.ALTAR -> ForgeGlyphs.Chain
    RiftNodeKind.REST -> ForgeGlyphs.Flask
    RiftNodeKind.TREASURE -> ForgeGlyphs.Coins
    RiftNodeKind.FORGE -> ForgeGlyphs.Anvil
    RiftNodeKind.GUARDIAN -> ForgeGlyphs.Helm
}

/** Размер значка награды над узлом. */
private val BADGE = 20.dp

/** Рамка фигурки героя (dp): ширина, высота и подъём ступней над низом. */
private const val HERO_W = 30f
private const val HERO_H = 44f
private const val HERO_FOOT = 6f

/** Радиус ореола у героя и подъём края тумана над рядом последнего видимого узла (dp). */
private const val HALO = 220f
private const val FOG_LIFT = 52f
private const val MIST = 9
private const val FLOW_MS = 1200
private const val BREATH_MS = 2400
private const val DRIFT_MS = 16000
