package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.run.DropChip
import com.sperance.exileforge.core.campaign.run.MapDrop
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonSize
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.screens.expedition.scene.SceneCamera
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Brass
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.OnPrimary
import com.sperance.exileforge.ui.theme.rarityColor
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Полёт добычи в сумку на карте: ответ сервера на сундук выпускает веер фишек редкостей от героя в кнопку сумки ([BagFan]), кнопка
 * пульсирует, и на ней на миг встаёт «+N» ([MapBag]). Веер и пульс - декор: без `LocalMotion` их нет, «+N» встаёт сразу.
 * Добыча, что легла до этого экрана, не вылетает снова: веер - только для ответов при нём; новый ответ обрывает веер.
 */
@Stable internal class BagFlight {
    /** Ответ, чей веер летит сейчас. */
    var flying by mutableStateOf<MapDrop?>(null)
        private set

    /** Последний принятый ответ: его «+N» висит на кнопке [NEWS_MS]. */
    var news by mutableStateOf<MapDrop?>(null)
        private set

    /** Середина кнопки сумки в мере корня: туда летят фишки; пока кнопки нет - некуда. */
    var target by mutableStateOf<Offset?>(null)
        internal set

    internal val fan = Animatable(0f)
    internal val pulse = Animatable(1f)

    /** Проводит принятый ответ [drop]: веер, пульс и «+N»; [moving] - движение разрешено. */
    internal suspend fun land(drop: MapDrop, moving: Boolean) {
        if (drop.chips.isEmpty()) return
        try {
            if (moving) {
                flying = drop
                fan.snapTo(0f)
                fan.animateTo(1f, tween(FAN_MS, easing = LinearEasing))
                flying = null
                pulse.snapTo(PULSE_SCALE)
                pulse.animateTo(1f, tween(PULSE_MS))
            }
        } finally {
            flying = null
            news = drop
        }
    }

    /** Снимает «+N», если на кнопке всё ещё [drop]. */
    internal fun settle(drop: MapDrop) {
        if (news == drop) news = null
    }
}

/** Полёт добычи экрана карты по ответам [drops]: живёт, пока стоит экран, - ответы до него не летят. */
@Composable internal fun rememberBagFlight(drops: List<MapDrop>): BagFlight {
    val moving = LocalMotion.current
    val flight = remember { BagFlight() }
    val told = remember { drops.filterNot { it.waiting }.mapTo(HashSet()) { it.n } }
    LaunchedEffect(drops) {
        drops.filter { !it.waiting && it.n !in told }.forEach { drop ->
            told += drop.n
            flight.land(drop, moving)
        }
    }
    val news = flight.news
    LaunchedEffect(news) {
        if (news == null) return@LaunchedEffect
        delay(NEWS_MS)
        flight.settle(news)
    }
    return flight
}

/** Веер добычи над всем экраном: от героя ([SceneCamera.HERO_DOWN]) к кнопке сумки. */
@Composable internal fun BagFan(flight: BagFlight) {
    val drop = flight.flying ?: return
    val density = LocalDensity.current
    var origin by remember { mutableStateOf<Offset?>(null) }
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        val from = origin ?: return@BoxWithConstraints
        val to = flight.target ?: return@BoxWithConstraints
        val target = with(density) { (to.x - from.x).toDp() to (to.y - from.y).toDp() }
        Fan(drop, from = maxWidth / 2 to maxHeight * SceneCamera.HERO_DOWN, to = target, progress = flight.fan.value)
    }
}

/**
 * Кнопка сумки под мини-картой: «Сумка · N» - сколько добычи легло за карту ([gathered]), «+N» последнего ответа в углу ([BagFlight]);
 * ждущие ответа сундуки - строкой «ждёт N сундук» под ней. Нажатие открывает «Сумку захода» ([onOpen]) - всю добычу захода;
 * кнопка встаёт с первой добычей ([looted]) или первым сундуком. Ширина - не уже мини-карты [width].
 */
@Composable internal fun MapBag(flight: BagFlight, drops: List<MapDrop>, looted: Boolean, gathered: Long, width: Dp, onOpen: () -> Unit) {
    if (drops.isEmpty() && !looted) return
    val waiting = drops.count { it.waiting }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            ForgeButton(
                onClick = onOpen,
                modifier = Modifier.widthIn(min = width).onGloballyPositioned { flight.target = it.boundsInRoot().center }.scale(flight.pulse.value),
                kind = ButtonKind.Secondary,
                size = ButtonSize.Small,
            ) { Text(ui("run.bag_count", gathered), maxLines = 1) }
            flight.news?.let { drop -> NewsBadge(drop.pieces, Modifier.align(Alignment.TopStart).offset(x = (-8).dp, y = (-9).dp)) }
        }
        if (waiting > 0) {
            Text(
                ui("run.bag_wait", waiting, plural("run.chest", waiting)),
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(width),
            )
        }
    }
}

/** «+N» принятого ответа в углу кнопки сумки: золотая плашка; число - в «Сумке захода», поэтому для чтения с экрана её нет. */
@Composable private fun NewsBadge(pieces: Long, modifier: Modifier) {
    val shape = RoundedCornerShape(9.dp)
    Text(
        "+$pieces",
        color = OnPrimary,
        fontSize = 11.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier.clearAndSetSemantics {}.defaultMinSize(minWidth = 18.dp).height(18.dp)
            .border(2.dp, Abyss, shape).background(Gold, shape).padding(horizontal = 5.dp),
    )
}

/**
 * Веер добычи [drop] в мере экрана: фишки раскрываются дугой над [from] (углы −150…−30°), висят миг и летят в [to], тая.
 * [progress] - 0..1 всего полёта.
 *
 * Раскрытие, вис и полёт в сумку - доли [progress].
 */
@Composable private fun Fan(drop: MapDrop, from: Pair<Dp, Dp>, to: Pair<Dp, Dp>, progress: Float) {
    val chips = drop.chips.take(FAN_CHIPS)
    chips.forEachIndexed { i, chip ->
        val angle = Math.toRadians(if (chips.size == 1) -90.0 else -150.0 + 120.0 * i / (chips.size - 1))
        val fanX = from.first + FAN_RADIUS * cos(angle).toFloat()
        val fanY = from.second + FAN_RADIUS * sin(angle).toFloat()
        val open = (progress / FAN_OPEN).coerceIn(0f, 1f)
        val fly = ((progress - FAN_HOLD) / (1f - FAN_HOLD)).coerceIn(0f, 1f)
        val x = lerp(lerp(from.first, fanX, open), to.first, fly)
        val y = lerp(lerp(from.second, fanY, open), to.second, fly)
        val size = 1f - .65f * fly
        val (glyph, tint, label) = chipLook(chip)
        Column(
            Modifier.offset { IntOffset((x - CHIP / 2).roundToPx(), (y - CHIP / 2).roundToPx()) }
                .graphicsLayer {
                    alpha = open * (1f - fly * fly)
                    scaleX = (.3f + .7f * open) * size
                    scaleY = (.3f + .7f * open) * size
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(CHIP).background(Abyss, CircleShape).border(1.dp, tint, CircleShape), contentAlignment = Alignment.Center) {
                Text(glyph, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            label?.let { Text(it, color = tint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
        }
    }
}

/** Знак, цвет и подпись фишки: вещь - ромб цвета редкости, стопка - бусина латуни с числом, золото - монета с суммой. */
private fun chipLook(chip: DropChip): Triple<String, Color, String?> = when (chip) {
    is DropChip.Piece -> Triple("◆", rarityColor(chip.item.rarity.name), null)
    is DropChip.Stack -> Triple("●", Brass, "×${chip.amount}".takeIf { chip.amount > 1 })
    is DropChip.Gold -> Triple("¤", GoldBright, number(chip.amount.toDouble()))
}

private fun lerp(a: Dp, b: Dp, t: Float): Dp = a + (b - a) * t

/** Веер: длительность полёта, доля раскрытия и конец виса; сколько фишек летит самое большее; радиус дуги. */
private const val FAN_MS = 900
private const val FAN_OPEN = .3f
private const val FAN_HOLD = .5f
private const val FAN_CHIPS = 7
private val FAN_RADIUS = 80.dp

/** Пульс сумки при ответе: размах и длительность; сколько висит «+N». */
private const val PULSE_SCALE = 1.18f
private const val PULSE_MS = 300
private const val NEWS_MS = 2_600L

/** Фишка веера. */
private val CHIP = 24.dp
