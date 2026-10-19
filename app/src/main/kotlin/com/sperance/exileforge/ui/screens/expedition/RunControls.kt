package com.sperance.exileforge.ui.screens.expedition
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.FlowLine
import com.sperance.exileforge.core.display.FlowSubject
import com.sperance.exileforge.core.display.FlowTone
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.flowLine
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.modNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statPercent
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.arena.ArenaOverlay
import com.sperance.exileforge.ui.screens.expedition.arena.key
import com.sperance.exileforge.ui.screens.expedition.arena.rarityTint
import com.sperance.exileforge.ui.screens.expedition.scene.ExpeditionScene
import com.sperance.exileforge.ui.screens.expedition.scene.SCENE_UNIT
import com.sperance.exileforge.ui.screens.expedition.scene.SceneCamera
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Управление походом: полосы жизни и маны, стик движения. */
/**
 * Литые полосы: объёмная полоса жизни с числами внутри, щит - светящаяся кромка по её верху со своим числом,
 * мана - тонкая полоса под ней с числами внутри; удержанная аурами часть ([reserved] сверх [maxMana]) - штриховкой.
 *
 * Цифры полос - одной ширины: тик их не дёргает. [onFlow] не null (карта) - цифры полосы жизни прижаты влево, а у правого края
 * её же - итог [flow] плашкой: «−14/с» цветом убыли или прибыли, щитовым цветом, когда недуги едят только щит ([flowLine]); покой -
 * плашки нет, высота полос не меняется; итог подчёркнут пунктиром, касание всей полосы жизни - разбор итога. Запас, что ауры держат
 * целиком, рисуется полной штрихованной полосой; [lifeReserved] - здоровье, что держат ауры сверх [maxLife], - штрихованный хвост
 * полосы здоровья.
 */
@Composable internal fun Vitals(
    life: Int,
    maxLife: Int,
    shield: Int,
    maxShield: Int,
    modifier: Modifier = Modifier,
    mana: Int = 0,
    maxMana: Int = 0,
    reserved: Int = 0,
    flow: LifeFlow? = null,
    lifeReserved: Int = 0,
    onFlow: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(5.dp)
    val lifeWhole = maxLife + lifeReserved.coerceAtLeast(0)
    val lifeShare by animateFloatAsState(if (lifeWhole > 0) life / lifeWhole.toFloat() else 0f, label = "life")
    val shieldShare by animateFloatAsState(if (maxShield > 0) shield / maxShield.toFloat() else 0f, label = "shield")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.fillMaxWidth().height(if (onFlow != null) MAP_LIFE_BAR else 18.dp).clip(shape).background(VitalsWell, shape)
                .reservedTail(reservedShare(maxLife, lifeReserved), LifeRed).border(1.dp, LifeRim, shape)
                .then(onFlow?.let { Modifier.clickable(onClickLabel = ui("fight.flow.title"), onClick = it) } ?: Modifier),
        ) {
            Box(Modifier.fillMaxWidth(lifeShare.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.verticalGradient(listOf(LifeTop, LifeRed, LifeDeep))))
            Box(Modifier.fillMaxWidth(lifeShare.coerceIn(0f, 1f)).fillMaxHeight(.4f).background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .22f), Color.Transparent))))
            if (maxShield > 0) {
                Box(
                    Modifier.fillMaxWidth(shieldShare.coerceIn(0f, 1f)).height(5.dp).align(Alignment.TopStart)
                        .background(Brush.horizontalGradient(listOf(ShieldBright, ShieldCyan))),
                )
            }
            VitalsFigure(
                buildAnnotatedString {
                    append(ui("expedition.vitals_figure", number(life.toDouble()), number(maxLife.toDouble())))
                    if (maxShield > 0) withStyle(SpanStyle(color = ShieldBright, fontWeight = FontWeight.SemiBold)) { append("  ◈ " + number(shield.toDouble())) }
                },
                11.sp,
                if (onFlow != null) Alignment.CenterStart else Alignment.Center,
            )
            if (onFlow != null) flow?.let { flowLine(it, life.toDouble(), maxLife.toDouble()) }?.let { FlowPlate(it) }
        }
        val pooled = maxMana + reserved.coerceAtLeast(0) > 0
        if (pooled) {
            Box(
                Modifier.fillMaxWidth().height(11.dp).clip(shape).background(VitalsWell, shape)
                    .reservedTail(reservedShare(maxMana, reserved), ManaBlue).border(1.dp, ManaRim, shape),
            ) {
                Box(
                    Modifier.fillMaxWidth((mana / (maxMana + reserved.coerceAtLeast(0)).toFloat()).coerceIn(0f, 1f)).fillMaxHeight()
                        .background(Brush.verticalGradient(listOf(ManaTop, ManaDeep))),
                )
                VitalsFigure(
                    AnnotatedString(ui("expedition.vitals_figure", number(mana.toDouble()), number(maxMana.toDouble()))),
                    9.sp,
                    if (onFlow != null) Alignment.CenterStart else Alignment.Center,
                )
            }
        }
    }
}

/** Итог здоровья в секунду у правого края полосы жизни: тёмная плашка, число его цвета, подчёркнутое пунктиром. */
@Composable private fun BoxScope.FlowPlate(line: FlowLine) {
    val tint = when {
        line.subject == FlowSubject.SHIELD -> ShieldBright
        line.tone == FlowTone.GAIN -> Vital
        else -> FlowLoss
    }
    val dash = with(LocalDensity.current) { floatArrayOf(3.dp.toPx(), 2.dp.toPx()) }
    Box(
        Modifier.align(Alignment.CenterEnd).padding(3.dp).fillMaxHeight().background(FlowWell, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            line.text,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall.copy(shadow = Shadow(Color.Black, Offset(0f, 1f), 3f), fontFeatureSettings = "tnum"),
            maxLines = 1,
            modifier = Modifier.drawBehind {
                drawLine(tint.copy(alpha = .7f), Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(dash))
            },
        )
    }
}

/** Высота полосы жизни на карте: в ней же стоит плашка итога. */
private val MAP_LIFE_BAR = 20.dp

/** Подложка плашки итога на полосе жизни. */
private val FlowWell = Color(0xD105080B)

/** Числа поверх литой полосы: по центру или у края [at], с тенью - читаются и на пустой, и на полной. */
@Composable private fun BoxScope.VitalsFigure(text: AnnotatedString, size: TextUnit, at: Alignment = Alignment.Center) {
    Text(
        text,
        color = Color.White,
        fontSize = size,
        fontWeight = FontWeight.Bold,
        letterSpacing = .4.sp,
        style = MaterialTheme.typography.labelSmall.copy(shadow = Shadow(Color.Black, Offset(0f, 1f), 3f), fontFeatureSettings = "tnum"),
        modifier = Modifier.align(at).padding(horizontal = if (at == Alignment.Center) 0.dp else 8.dp),
        maxLines = 1,
    )
}

/** Убыль здоровья в секунду в плашке итога: светлый красный, чтобы читался на тёмной карте. */
private val FlowLoss = Color(0xFFFFB3AD)

private val VitalsWell = Color(0xCC0A0D12)
private val LifeRim = Color(0xFF3A1A18)
private val LifeTop = Color(0xFFE8645C)
private val LifeDeep = Color(0xFF7C2420)
private val ShieldBright = Color(0xFF8FE4EF)
private val ManaRim = Color(0xFF1A2C44)
private val ManaTop = Color(0xFF9CCFFF)
private val ManaDeep = Color(0xFF3F86D6)

/**
 * The stick: wherever the thumb lands in the lower part of the screen (below [STICK_FROM] of its height), dragging from there walks.
 * The direction is sent in screen axes; the run turns it into the map's. A tap anywhere that does not
 * drag lands on the map: on a fountain still full, it names it to [onFountain].
 *
 * Бой может начаться под пальцем на стекле: герой не уходит после него.
 */
@Composable internal fun Stick(run: ExpeditionRun, onFountain: (Int) -> Unit) {
    var centre by remember { mutableStateOf<Offset?>(null) }
    var knob by remember { mutableStateOf(Offset.Zero) }
    var area by remember { mutableStateOf(IntSize.Zero) }
    val radius = with(LocalDensity.current) { 56.dp.toPx() }
    val unit = with(LocalDensity.current) { SCENE_UNIT.toPx() }
    val tap by rememberUpdatedState { at: Offset ->
        val (x, y) = SceneCamera.toWorld(run.world.heroX, run.world.heroY, at, area.toSize(), unit)
        run.world.fountains.firstOrNull { !it.used && hypot(it.cell.x + .5 - x, it.cell.y + .5 - y) < FOUNTAIN_TAP }?.let { onFountain(it.id) }
    }
    DisposableEffect(run) {
        onDispose {
            run.stickX = 0.0
            run.stickY = 0.0
        }
    }
    Box(
        Modifier.fillMaxSize().onSizeChanged { area = it }.pointerInput(run) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val walks = down.position.y >= area.height * STICK_FROM
                if (walks) {
                    centre = down.position
                    knob = down.position
                }
                var dragged = false
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val delta = change.position - down.position
                    val length = hypot(delta.x, delta.y)
                    if (length > viewConfiguration.touchSlop) dragged = true
                    if (!walks) continue
                    val clamped = if (length > radius) delta * (radius / length) else delta
                    knob = down.position + clamped
                    run.stickX = (clamped.x / radius).toDouble()
                    run.stickY = (clamped.y / radius).toDouble()
                    change.consume()
                } while (change.pressed)
                if (walks) {
                    run.stickX = 0.0
                    run.stickY = 0.0
                    centre = null
                }
                if (!dragged) tap(down.position)
            }
        },
    ) {
        centre?.let { c ->
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Gold.copy(alpha = .18f), radius, c)
                drawCircle(Gold.copy(alpha = .5f), radius, c, style = Stroke(2.dp.toPx()))
                drawCircle(GoldBright.copy(alpha = .75f), radius * .4f, knob)
            }
        }
    }
}

/** Доля высоты экрана, ниже которой касание ведёт стик: нижние 55% - под палец, верх - шапка карты. */
private const val STICK_FROM = .45f
