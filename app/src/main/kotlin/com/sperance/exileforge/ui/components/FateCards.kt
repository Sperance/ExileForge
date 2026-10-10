package com.sperance.exileforge.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.rules.content.FateTheme
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos

/*
 * Предначертание (4.6.0, макет «Пепел предков», утверждён владельцем): палитра уголь и золото, как у уникальных вещей. Части
 * экрана выбора - угли, алтарь, карта-таро, кнопка удержания - и плашка дара для карточки игрока и шапки героя. Декор
 * (угли, дыхание алтаря, кольцо печати) стоит при выключенном `LocalMotion`.
 */

/** Палитра «Пепла предков». */
object FateInk {
    val Gold = Color(0xFFF0C76A)
    val GoldLight = Color(0xFFFFD99A)
    val Ember = Color(0xFFFFB35C)
    val EmberGlow = Color(0xFFFF7A2C)
    val Fire = Color(0xFFFF8C3C)
    val Face = listOf(Color(0xFF2A1810), Color(0xFF120A07))
    val Rim = Color(0xFF6B4422)
    val Back = listOf(Color(0xFF3A2110), Color(0xFF2A170B))
    val Sigil = listOf(Color(0xFF5A3418), Color(0xFF1C0F08))
    val Ground = listOf(Color(0xFF5A2310), Color(0xFF24100A), Color(0xFF0B0605))
    val Text = Color(0xFFD7DBE4)
    val Tag = Color(0xFF9AA1B2)
    val Warn = Color(0xFFE8A35A)
    val Accepted = Color(0xFF3FB67A)
}

/** Знак темы дара в печати карты: символ, не слово. */
fun FateTheme.sigil(): String = when (this) {
    FateTheme.OFFENCE -> "⚔"
    FateTheme.DEFENCE -> "⛨"
    FateTheme.SKILLS -> "✧"
    FateTheme.LOOT -> "◈"
    FateTheme.ATLAS -> "✵"
    FateTheme.TRIALS -> "♜"
    FateTheme.CRAFT -> "⚒"
    FateTheme.PROGRESS -> "➶"
}

/** Подложка «Пепла предков»: жар снизу, уголь к верху. */
fun Modifier.ashGround(): Modifier = drawBehind {
    drawRect(Brush.radialGradient(FateInk.Ground, Offset(size.width / 2, size.height * 1.1f), size.maxDimension * .9f))
}

/** Одна искра: место по ширине, снос вбок, доля периода на подъём и сдвиг фазы. */
private data class Spark(val x: Float, val drift: Float, val pace: Float, val phase: Float)

private val SPARKS = List(26) { i ->
    // Детерминированный разброс без кости: искры одни и те же при каждом показе
    val h = (i * 7919 + 13) % 997 / 997f
    val g = (i * 104729 + 7) % 991 / 991f
    Spark(.05f + .9f * h, (g - .5f) * 80f, .45f + .55f * ((i * 31) % 17 / 17f), (i * 37 % 100) / 100f)
}

/** Угли поднимаются со дна (6-9 с на подъём); при выключенных анимациях стоят, где застал их кадр. */
@Composable fun EmberField(modifier: Modifier = Modifier) {
    val time = motionClock(EMBER_MS, "fate-embers")
    Canvas(modifier) {
        SPARKS.forEach { spark ->
            val life = (time / spark.pace + spark.phase) % 1f
            val alpha = if (life < .1f) life / .1f else 1f - (life - .1f) / .9f
            val at = Offset(size.width * spark.x + spark.drift.dp.toPx() * life, size.height + 10.dp.toPx() - life * (size.height + 40.dp.toPx()))
            drawCircle(FateInk.EmberGlow.copy(alpha = alpha * .35f), 5.dp.toPx(), at)
            drawCircle(FateInk.Ember.copy(alpha = alpha), 2.dp.toPx(), at)
        }
    }
}

/** Алтарь: тёплый овал жара дышит (3 с); без анимаций - ровный. */
@Composable fun AltarGlow(modifier: Modifier = Modifier) {
    val time = motionClock(ALTAR_MS, "fate-altar")
    val breath = (1 - cos(time * 2 * PI).toFloat()) / 2
    Canvas(modifier) {
        val scale = 1f + .08f * breath
        val alpha = .55f - .25f * breath
        drawOval(
            Brush.radialGradient(listOf(FateInk.Fire.copy(alpha = alpha), Color.Transparent), center, size.width / 2 * scale),
            topLeft = Offset(size.width * (1 - scale) / 2, size.height * (1 - scale) / 2),
            size = Size(size.width * scale, size.height * scale),
        )
    }
}

/** Печать дара: круг уголь-золото со знаком темы и медленно вращающимся пунктирным кольцом (14 с). */
@Composable fun FateSigil(theme: FateTheme, size: Dp = 54.dp) {
    val turn = motionClock(SIGIL_MS, "fate-sigil")
    Box(
        Modifier.size(size + 12.dp).drawBehind {
            rotate(turn * 360f) {
                drawCircle(
                    FateInk.Gold.copy(alpha = .45f),
                    this.size.minDimension / 2 - 1.dp.toPx(),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(size).background(Brush.radialGradient(FateInk.Sigil), CircleShape).border(1.dp, FateInk.Gold.copy(alpha = .6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(theme.sigil(), color = FateInk.GoldLight, fontSize = (size.value * .44f).sp) }
    }
}

/**
 * Карта-таро дара [card]: лицо - тема, печать, имя Cinzel и описание словами сервера; рубашка - узор и звезда. [turn] - поворот
 * вокруг вертикали в градусах (0 - лицом, 180 - рубашкой); лицо и рубашка не просвечивают друг сквозь друга.
 */
@Composable fun FateTarot(card: FateCard, turn: Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val back = (turn % 360 + 360) % 360 in 90f..270f
    Box(
        modifier.graphicsLayer {
            rotationY = turn
            cameraDistance = 12f * density
        },
    ) {
        if (back) {
            Box(
                Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.background(Brush.linearGradient(FateInk.Back), shape).border(1.dp, FateInk.Rim, shape),
                contentAlignment = Alignment.Center,
            ) { Text("✦", color = FateInk.Gold, fontSize = 34.sp) }
        } else {
            Column(
                Modifier.fillMaxSize().background(Brush.linearGradient(FateInk.Face), shape).border(1.dp, FateInk.Rim, shape)
                    .padding(3.dp).border(1.dp, FateInk.Gold.copy(alpha = .35f), RoundedCornerShape(11.dp)).padding(horizontal = 7.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(card.themeTitle.uppercase(), color = FateInk.Tag, fontSize = 8.5.sp, letterSpacing = 1.4.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                FateSigil(card.theme, 44.dp)
                Text(card.title, style = relicName(13).copy(color = FateInk.GoldLight), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(card.text, color = FateInk.Text, fontSize = 10.5.sp, lineHeight = 14.sp, textAlign = TextAlign.Center, maxLines = 7, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * Кнопка «удержи, чтобы принять» (4.6.0): полоса наполняется, пока палец держит кнопку, [holdMs]; отпущенная раньше - пустеет.
 * Полное удержание - [onHeld] один раз. [enabled] false - касание лишь зовёт [onRefused] (например, «сначала выбери карту»).
 */
@Composable fun HoldToAccept(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    done: Boolean = false,
    holdMs: Int = HOLD_MS,
    onRefused: () -> Unit = {},
    onHeld: () -> Unit,
) {
    val fill = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val held by rememberUpdatedState(onHeld)
    val refused by rememberUpdatedState(onRefused)
    val live by rememberUpdatedState(enabled && !done)
    val shape = RoundedCornerShape(16.dp)
    val ground = if (done) {
        Brush.linearGradient(listOf(FateInk.Accepted, FateInk.Accepted))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF7A5A20), Color(0xFFC99A3C), FateInk.Gold, Color(0xFFC99A3C)))
    }
    Box(
        modifier.height(54.dp).background(ground, shape).drawWithContent {
            drawContent()
            if (!done) drawRect(Color.White.copy(alpha = .35f), size = size.copy(width = size.width * fill.value))
        }.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown()
                if (!live) {
                    refused()
                    waitForUpOrCancellation()
                    return@awaitEachGesture
                }
                val run = scope.launch {
                    fill.animateTo(1f, tween(((1f - fill.value) * holdMs).toInt().coerceAtLeast(1), easing = LinearEasing))
                    held()
                }
                waitForUpOrCancellation()
                if (fill.value < 1f) {
                    run.cancel()
                    scope.launch { fill.snapTo(0f) }
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = relicName(13), color = if (done) Color(0xFF04120A) else Color(0xFF1B1306), letterSpacing = 1.8.sp)
    }
}

/**
 * Плашка Предначертания (4.6.0) для карточки игрока и шапки героя: печать темы, тема и имя; касание раскрывает описание.
 */
@Composable fun FateBadge(card: FateCard, modifier: Modifier = Modifier, compact: Boolean = false) {
    var open by rememberSaveable(card.code) { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.background(Brush.verticalGradient(FateInk.Face), shape).border(1.dp, FateInk.Rim, shape).clickable { open = !open }
            .padding(horizontal = 10.dp, vertical = 8.dp).animateContentSize(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FateSigil(card.theme, if (compact) 22.dp else 28.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(card.themeTitle.uppercase(), color = FateInk.Tag, fontSize = 9.sp, letterSpacing = 1.2.sp, maxLines = 1)
                Text(card.title, style = relicName(if (compact) 13 else 15).copy(color = FateInk.GoldLight, shadow = Shadow(FateInk.Gold.copy(alpha = .4f), blurRadius = 10f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (open) "▴" else "▾", color = Muted, style = MaterialTheme.typography.labelMedium)
        }
        if (open) {
            Spacer(Modifier.height(6.dp))
            Text(card.text, color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
            Text(ui("fate.forever"), color = FateInk.Warn, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Периоды декора, мс: подъём углей, дыхание алтаря, оборот кольца печати. */
private const val EMBER_MS = 9_000
private const val ALTAR_MS = 3_000
private const val SIGIL_MS = 14_000

/** Сколько держать кнопку принятия, мс. */
const val HOLD_MS = 1_200
