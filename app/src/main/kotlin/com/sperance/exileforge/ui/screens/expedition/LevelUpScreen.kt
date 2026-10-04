package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.run.LevelUp
import com.sperance.exileforge.core.display.modNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/** What a rise brings: the class's growth for every level, the skill slots and the tree's points that open on the way. */
internal data class LevelGains(val stats: List<Pair<String, Double>>, val active: Int, val passive: Int, val points: Int)

internal fun levelGains(rise: LevelUp, heroClass: HeroClass?, index: ContentIndex?): LevelGains {
    val stats = heroClass?.perLevel.orEmpty().filterValues { it != 0.0 }.map { (stat, value) -> stat to value * rise.levels }
    val within = { levels: List<Int> -> levels.count { it in (rise.from + 1)..rise.to } }
    val rules = index?.skills?.rules
    return LevelGains(
        stats,
        active = rules?.let { within(it.activeSlots) } ?: 0,
        passive = rules?.let { within(it.passiveSlots) } ?: 0,
        points = index?.classes?.let { it.pointsTotal(rise.to) - it.pointsTotal(rise.from) } ?: 0,
    )
}

/**
 * «Вознесение» (3.81.0, mockup A): a won fight that raised the hero's level stops on a screen of its own before the report —
 * rays behind, the new level falling into place over the old, then what it brought one line after another: the class's growth
 * (with the attributes every level adds), the skill slots and the tree's points it opened. Several levels at once are one screen.
 */
@Composable internal fun LevelUpScreen(rise: LevelUp, heroClass: HeroClass?, index: ContentIndex?, onDone: () -> Unit) {
    val gains = remember(rise, heroClass, index) { levelGains(rise, heroClass, index) }
    val motion = LocalMotion.current
    val drop = remember { Animatable(if (motion) 0f else 1f) }
    var shown by remember { mutableIntStateOf(if (motion) 0 else Int.MAX_VALUE) }
    LaunchedEffect(rise) {
        if (!motion) return@LaunchedEffect
        drop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        while (shown < gains.stats.size + 1) {
            delay(STEP_MS)
            shown++
        }
    }
    BackHandler(onBack = onDone)
    Box(
        Modifier.fillMaxSize().background(Ink.copy(alpha = .97f))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Rays(motion, Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(.6f))
            Text(ui("levelup.title").uppercase(), color = Gold, style = MaterialTheme.typography.titleMedium, letterSpacing = 4.sp)
            Text(
                rise.to.toString(),
                style = TextStyle(fontSize = 112.sp, fontWeight = FontWeight.Black, color = GoldBright, shadow = Shadow(Gold, blurRadius = 32f)),
                modifier = Modifier.graphicsLayer {
                    val t = drop.value
                    scaleX = 2.2f - 1.2f * t
                    scaleY = 2.2f - 1.2f * t
                    alpha = t.coerceIn(0f, 1f)
                },
            )
            Text(ui("levelup.was", rise.from), color = Muted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(22.dp))
            Column(Modifier.widthIn(max = 320.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                gains.stats.forEachIndexed { i, (stat, value) ->
                    Gain(statTitle(stat), "+${modNumber(stat, value)}", visible = i < shown)
                }
                val unlocks = listOfNotNull(
                    ui("levelup.active_slot", gains.active).takeIf { gains.active > 0 },
                    ui("levelup.passive_slot", gains.passive).takeIf { gains.passive > 0 },
                    ui("levelup.points", gains.points).takeIf { gains.points > 0 },
                )
                if (unlocks.isNotEmpty() && shown > gains.stats.size) {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 8.dp).background(Gold.copy(alpha = .10f), RoundedCornerShape(8.dp))
                            .border(1.dp, Gold.copy(alpha = .5f), RoundedCornerShape(8.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        unlocks.forEach { Text("✦ $it", color = GoldBright, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            ForgeButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
            ) {
                Text(ui("expedition.continue"), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** One line of the growth: its name and its figure, fading in when its turn comes. */
@Composable private fun Gain(title: String, figure: String, visible: Boolean) {
    Row(
        Modifier.fillMaxWidth().graphicsLayer { alpha = if (visible) 1f else 0f }
            .background(Panel.copy(alpha = .8f), RoundedCornerShape(6.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(figure, color = Vital, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

/** The rays behind the number: a slow golden wheel, still when the animations are off. */
@Composable private fun Rays(motion: Boolean, modifier: Modifier) {
    val turn = if (motion) {
        rememberInfiniteTransition(label = "rays").animateFloat(0f, 360f, infiniteRepeatable(tween(RAYS_MS, easing = LinearEasing)), label = "turn").value
    } else {
        0f
    }
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height * .3f)
        val r = size.maxDimension
        drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = .28f), Color.Transparent), c, size.minDimension * .6f), size.minDimension * .6f, c)
        rotate(turn, c) {
            (0 until RAYS).forEach { k ->
                val a = 2 * Math.PI * k / RAYS
                val w = Math.PI / RAYS / 2
                val path = Path().apply {
                    moveTo(c.x, c.y)
                    lineTo(c.x + (r * cos(a - w)).toFloat(), c.y + (r * sin(a - w)).toFloat())
                    lineTo(c.x + (r * cos(a + w)).toFloat(), c.y + (r * sin(a + w)).toFloat())
                    close()
                }
                drawPath(path, Brush.radialGradient(listOf(Gold.copy(alpha = .16f), Color.Transparent), c, r * .7f))
            }
        }
    }
}

private const val RAYS = 14
private const val RAYS_MS = 40_000
private const val STEP_MS = 180L
