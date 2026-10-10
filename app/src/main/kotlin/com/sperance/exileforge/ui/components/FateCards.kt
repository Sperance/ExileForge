package com.sperance.exileforge.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

/*
 * Предначертание (4.6.0, макет «Пепел предков», утверждён владельцем): палитра уголь и золото, как у уникальных вещей. Части
 * экрана выбора - угли, алтарь, карта-таро, кнопка удержания - и лента дара (4.6.3) для карточек героя, выбора героя и досье. Декор
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
    val Parchment = listOf(Color(0xFF2E1C10), Color(0xFF1E120A), Color(0xFF2A190D))
    val Warn = Color(0xFFE8A35A)
    val Accepted = Color(0xFF3FB67A)

    /** Осквернение (4.6.2, макеты «Руническая печать» и «Тлеющие письмена»): тлеющий уголь, сургуч печати, пепел спящей строки. */
    val Cinder = Color(0xFFE0743A)
    val CinderInk = Color(0xFFB48A70)
    val Wax = listOf(Color(0xFFFFCF7A), Color(0xFFA5531D), Color(0xFF4A1F0A))
    val WaxInk = Color(0xFF2A1206)
    val Ash = Color(0xFF44515A)
    val AshInk = Color(0xFF6F7D86)

    /** Лента дара (4.6.3, макет «Лента и свиток»): описание на свитке - тёплая бумага. */
    val Vellum = Color(0xFFE9D9BF)
}

/** Знак Предначертания в печати (4.6.1 - без тем): один на все дары. */
internal const val SIGIL = "✦"

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

/** Печать дара: круг уголь-золото со знаком Предначертания и медленно вращающимся пунктирным кольцом (14 с). */
@Composable fun FateSigil(size: Dp = 54.dp) {
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
        ) { Text(SIGIL, color = FateInk.GoldLight, fontSize = (size.value * .44f).sp) }
    }
}

/**
 * Карта-таро дара [card] (4.6.1, решение владельца): на лице - только имя, крупно Cinzel между золотыми узорами, чтобы
 * помещалось всегда; описание - свиток под веером ([FateScroll]). Рубашка - узор и звезда. [turn] - поворот вокруг вертикали
 * в градусах (0 - лицом, 180 - рубашкой); лицо и рубашка не просвечивают друг сквозь друга.
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
                    .padding(3.dp).border(1.dp, FateInk.Gold.copy(alpha = .35f), RoundedCornerShape(11.dp)).padding(horizontal = 6.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            ) {
                FateFlourish(Modifier.fillMaxWidth(.7f).height(10.dp))
                Text(
                    card.title.uppercase(),
                    style = relicName(nameSize(card.title)).copy(color = FateInk.GoldLight, shadow = Shadow(FateInk.Gold.copy(alpha = .45f), blurRadius = 14f)),
                    textAlign = TextAlign.Center,
                    lineHeight = (nameSize(card.title) + 5).sp,
                )
                FateFlourish(Modifier.fillMaxWidth(.7f).height(10.dp))
            }
        }
    }
}

/**
 * Кегль имени на лице карты (4.6.1): по самому длинному слову - Cinzel заглавными, чтобы слово не рвалось на узкой карте.
 */
private fun nameSize(title: String): Int = when (title.split(' ', '-').maxOfOrNull { it.length } ?: 0) {
    in 0..7 -> 17
    in 8..9 -> 15
    in 10..11 -> 13
    else -> 11
}

/** Золотой узор: тонкая черта, к краям гаснет, в середине - ромб. */
@Composable fun FateFlourish(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val mid = size.height / 2
        val fade = Brush.horizontalGradient(listOf(Color.Transparent, FateInk.Gold.copy(alpha = .7f), Color.Transparent))
        drawLine(fade, Offset(0f, mid), Offset(size.width, mid), 1.dp.toPx())
        val r = size.height / 2
        val diamond = Path().apply {
            moveTo(center.x, mid - r)
            lineTo(center.x + r, mid)
            lineTo(center.x, mid + r)
            lineTo(center.x - r, mid)
            close()
        }
        drawPath(diamond, FateInk.Face.last())
        drawPath(diamond, FateInk.Gold, style = Stroke(1.dp.toPx()))
    }
}

/**
 * Свиток судьбы (4.6.1, решение владельца): описание выбранного дара [card] - выезжает снизу и разворачивается между двумя
 * золотыми валиками (подъём, затем развёртка, ~0,9 с); новая карта - свиток разворачивается заново. Без анимаций - сразу
 * развёрнут. Имя - Cinzel, текст - словами сервера, длинный листается.
 */
@Composable fun FateScroll(card: FateCard, modifier: Modifier = Modifier) {
    val motion = LocalMotion.current
    val open = remember(card.code) { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(card.code) { open.animateTo(1f, tween(SCROLL_MS, easing = LinearEasing)) }
    val rise = FastOutSlowInEasing.transform((open.value / SCROLL_RISE).coerceIn(0f, 1f))
    val unroll = FastOutSlowInEasing.transform(((open.value - SCROLL_RISE / 2) / (1 - SCROLL_RISE / 2)).coerceIn(0f, 1f))
    Column(
        modifier.graphicsLayer {
            alpha = rise
            translationY = (1 - rise) * 48.dp.toPx()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScrollRod()
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp).clipToBounds().layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, (placeable.height * unroll).roundToInt()) { placeable.place(0, 0) }
            },
        ) {
            Column(
                Modifier.fillMaxWidth().background(Brush.verticalGradient(FateInk.Parchment)).drawBehind {
                    // Края свитка темнее - бумага загибается к валикам
                    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .35f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = .35f))))
                }.heightIn(max = SCROLL_MAX).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(card.title, style = relicName(15).copy(color = FateInk.GoldLight), textAlign = TextAlign.Center)
                FateFlourish(Modifier.padding(vertical = 6.dp).fillMaxWidth(.5f).height(8.dp))
                Text(card.text, color = FateInk.Text, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
            }
        }
        ScrollRod()
    }
}

/** Валик свитка: золотой стержень с круглыми навершиями по краям. */
@Composable private fun ScrollRod() {
    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
        val knob = size.height / 2
        val rod = Brush.verticalGradient(listOf(FateInk.GoldLight, FateInk.Rim, Color(0xFF3A2110)))
        drawRoundRect(rod, Offset(knob, size.height * .2f), Size(size.width - 2 * knob, size.height * .6f), CornerRadius(size.height * .3f))
        listOf(knob, size.width - knob).forEach { x ->
            drawCircle(FateInk.Rim, knob, Offset(x, knob))
            drawCircle(FateInk.Gold, knob * .55f, Offset(x - knob * .15f, knob * .85f))
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
 * Лента Предначертания (4.6.3, утверждена владельцем по макету «Лента и свиток», вариант A): угольная лента во всю ширину -
 * печать [FateSigil], имя дара [card] Cinzel заглавными и подпись [caption] (что это за дар или что он копит). Касание
 * разворачивает под лентой свиток: узор, описание серифом курсивом и «Изменить нельзя никогда». Одна на карточку игрока, шапку
 * выбора героя, лист героя и досье модерации; без `LocalMotion` свиток раскрывается сразу, кольцо печати стоит.
 */
@Composable fun FateRibbon(card: FateCard, caption: String, modifier: Modifier = Modifier) {
    var open by rememberSaveable(card.code) { mutableStateOf(false) }
    val motion = LocalMotion.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.clip(shape).border(1.dp, FateInk.Rim, shape).then(if (motion) Modifier.animateContentSize() else Modifier),
    ) {
        Row(
            Modifier.fillMaxWidth().background(Brush.horizontalGradient(RIBBON)).clickable { open = !open }.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FateSigil(RIBBON_SIGIL)
            Column(Modifier.weight(1f)) {
                Text(
                    card.title.uppercase(),
                    style = relicName(14).copy(color = FateInk.GoldLight, shadow = Shadow(FateInk.Gold.copy(alpha = .4f), blurRadius = 10f)),
                    letterSpacing = 1.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(caption, color = FateInk.CinderInk, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (open) "▴" else "▾", color = FateInk.Gold, style = MaterialTheme.typography.labelMedium)
        }
        if (open) FateUnrolled(card)
    }
}

/** Метка дара в строке (4.6.3): знак Предначертания и имя дара [card] золотом - рядом с классом в списках. */
@Composable fun FateMark(card: FateCard, modifier: Modifier = Modifier) {
    Text("$SIGIL ${card.title}", modifier, color = FateInk.GoldLight, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Свиток под лентой: узор ❧✦❧, описание дара серифом курсивом и предупреждение, что дар навсегда. */
@Composable private fun FateUnrolled(card: FateCard) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(FateInk.Rim))
    Column(
        Modifier.fillMaxWidth().background(Brush.verticalGradient(FateInk.Parchment.drop(1))).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("❧ $SIGIL ❧", color = FateInk.Gold, fontSize = 11.sp, letterSpacing = 6.sp)
        card.text.takeIf { it.isNotBlank() }?.let { text ->
            Text(
                ui("fate.ribbon.quote", text),
                color = FateInk.Vellum,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
        Text(ui("fate.ribbon.never"), color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

/** Периоды декора, мс: подъём углей, дыхание алтаря, оборот кольца печати. */
private const val EMBER_MS = 9_000
private const val ALTAR_MS = 3_000
private const val SIGIL_MS = 14_000

/** Лента (4.6.3): уголь к краям, жар в середине; размер печати. */
private val RIBBON = listOf(FateInk.Face.first(), FateInk.Back.first(), FateInk.Face.first())
private val RIBBON_SIGIL = 20.dp

/** Свиток: вся анимация, доля подъёма в ней и предел высоты текста. */
private const val SCROLL_MS = 900
private const val SCROLL_RISE = .35f
private val SCROLL_MAX = 190.dp

/** Сколько держать кнопку принятия, мс. */
const val HOLD_MS = 1_200
