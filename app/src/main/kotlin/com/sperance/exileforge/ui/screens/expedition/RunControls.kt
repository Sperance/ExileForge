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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.modNumber
import com.sperance.exileforge.core.display.statDescription
import com.sperance.exileforge.core.display.statPercent
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.arena.ArenaOverlay
import com.sperance.exileforge.ui.screens.expedition.arena.key
import com.sperance.exileforge.ui.screens.expedition.arena.rarityTint
import com.sperance.exileforge.ui.screens.expedition.scene.ExpeditionScene
import com.sperance.exileforge.ui.screens.expedition.scene.SCENE_UNIT
import com.sperance.exileforge.ui.screens.expedition.scene.sceneToWorld
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Управление походом (3.80.24): полосы жизни и маны, стик движения. */
/**
 * A life bar with the shield laid over it, and the figure in words; the mana under it since 2.78.0,
 * its [reserved] part past [maxMana] a hatched tail.
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
) {
    val shape = RoundedCornerShape(3.dp)
    val lifeShare by animateFloatAsState(if (maxLife > 0) life / maxLife.toFloat() else 0f, label = "life")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0xCC0A0D12), shape).border(1.dp, LifeRed.copy(alpha = .8f), shape)) {
            Box(Modifier.fillMaxWidth(lifeShare.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(LifeRed, LifeRed.copy(alpha = .55f))), shape))
            if (maxShield > 0) Box(Modifier.fillMaxWidth((shield / maxShield.toFloat()).coerceIn(0f, 1f)).height(4.dp).align(Alignment.TopStart).background(ShieldCyan.copy(alpha = .85f)))
        }
        // A pool the auras hold whole is still drawn: a full hatched bar.
        val pooled = maxMana + reserved.coerceAtLeast(0) > 0
        if (pooled) {
            Box(
                Modifier.fillMaxWidth().height(6.dp).clip(shape).background(Color(0xCC0A0D12), shape)
                    .reservedTail(reservedShare(maxMana, reserved), ManaBlue).border(1.dp, ManaBlue.copy(alpha = .8f), shape),
            ) {
                Box(Modifier.fillMaxWidth((mana / (maxMana + reserved.coerceAtLeast(0)).toFloat()).coerceIn(0f, 1f)).fillMaxHeight().background(ManaBlue, shape))
            }
        }
        Text(
            (if (maxShield > 0) ui("expedition.vitals_shield", life, maxLife, shield) else ui("expedition.vitals", life, maxLife)) +
                (if (pooled) " · " + ui("expedition.vitals_mana", mana, maxMana) else ""),
            color = Parchment,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/**
 * The stick: wherever the thumb lands in the lower part of the screen, dragging from there walks.
 * The direction is sent in screen axes; the run turns it into the map's. A tap anywhere that does not
 * drag (3.70.0) lands on the map: on a fountain still full, it names it to [onFountain].
 */
@Composable internal fun Stick(run: ExpeditionRun, onFountain: (Int) -> Unit) {
    var centre by remember { mutableStateOf<Offset?>(null) }
    var knob by remember { mutableStateOf(Offset.Zero) }
    var area by remember { mutableStateOf(IntSize.Zero) }
    val radius = with(LocalDensity.current) { 56.dp.toPx() }
    val unit = with(LocalDensity.current) { SCENE_UNIT.toPx() }
    val tap by rememberUpdatedState { at: Offset ->
        val (x, y) = sceneToWorld(run, at, area, unit)
        run.world.fountains.firstOrNull { !it.used && hypot(it.cell.x + .5 - x, it.cell.y + .5 - y) < FOUNTAIN_TAP }?.let { onFountain(it.id) }
    }
    // A fight can start under a thumb still on the glass; the hero must not walk off after it.
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
                val walks = down.position.y >= area.height * .35f
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
        if (centre == null) {
            Text(
                ui("expedition.stick_hint"),
                color = Muted.copy(alpha = .8f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp),
            )
        }
    }
}
