package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.run.DropChip
import com.sperance.exileforge.core.campaign.run.MapDrop
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.scene.SceneCamera
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Brass
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.rarityColor
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Сумка на карте: плашка внизу справа - «Сумка» и сколько добычи
 * легло за карту; ответ сервера на сундук выпускает веер фишек редкостей от героя в плашку, и у неё на миг встают «+N» и лучшая
 * вещь. Ждущие ответа сундуки - «ждёт N сундук». Веер и пульс - декор: без `LocalMotion` их нет, «+N» и строка встают сразу.
 * Нажатие открывает «Сумку захода» ([onOpen]) - всю добычу захода; плашка встаёт с первой добычей ([looted]) или
 * первым сундуком. [gathered] - штук добычи за всю карту.
 *
 * Добыча, что легла до этого экрана, не вылетает снова: веер - только для ответов при нём; новый ответ обрывает веер. Фишки летят к значку
 * сумки; веер раскрывается над героем.
 */
@Composable internal fun MapBag(game: GameUi, drops: List<MapDrop>, looted: Boolean, gathered: Long, onOpen: () -> Unit) {
    if (drops.isEmpty() && !looted) return
    val moving = LocalMotion.current
    val told = remember { drops.filterNot { it.waiting }.mapTo(HashSet()) { it.n } }
    var flying by remember { mutableStateOf<MapDrop?>(null) }
    var news by remember { mutableStateOf<MapDrop?>(null) }
    val fan = remember { Animatable(0f) }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(drops) {
        drops.filter { !it.waiting && it.n !in told }.forEach { drop ->
            told += drop.n
            if (drop.chips.isEmpty()) return@forEach
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
    }
    LaunchedEffect(news) {
        if (news == null) return@LaunchedEffect
        delay(NEWS_MS)
        news = null
    }
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    var bag by remember { mutableStateOf<Offset?>(null) }
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        val target = bag?.let { with(density) { (it.x - origin.x).toDp() to (it.y - origin.y).toDp() } }
        if (target != null) flying?.let { drop -> Fan(drop, from = maxWidth / 2 to maxHeight * SceneCamera.HERO_DOWN, to = target, progress = fan.value) }
        Column(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = TRAY_END, bottom = TRAY_BOTTOM)
                .widthIn(max = (maxWidth - TRAY_END - HANDS_RESERVE).coerceAtLeast(TRAY_MIN)),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            news?.let { drop -> News(game, drop) }
            Tray(drops, gathered, pulse.value, onBag = { bag = it }, onClick = onOpen)
        }
    }
}

/** «+N» и лучшая вещь последнего ответа: «◆ Кольцо Ветра и ещё 4». */
@Composable private fun News(game: GameUi, drop: MapDrop) {
    Text("+${drop.pieces}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    val best = drop.best
    val others = drop.chips.size - 1
    val name = best?.let { game.view(it)?.title } ?: drop.reward?.gold?.takeIf { it > 0 }?.let { ui("quest.toast.gold", number(it.toDouble())) }
    name?.let {
        Text(
            listOf("◆ $it", ui("run.bag_more", others).takeIf { others > 0 }).filterNotNull().joinToString(" "),
            color = best?.let { item -> rarityColor(item.rarity.name) } ?: GoldBright,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.background(Abyss.copy(alpha = .85f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** Плашка сумки: значок в кольце, «Сумка» с ожиданием и счёт за карту; [scale] - пульс принятого ответа. */
@Composable private fun Tray(drops: List<MapDrop>, gathered: Long, scale: Float, onBag: (Offset) -> Unit, onClick: () -> Unit) {
    val waiting = drops.count { it.waiting }
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.background(Abyss.copy(alpha = .9f), shape).border(1.dp, Bronze, shape).clickable(role = Role.Button, onClick = onClick)
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(BAG).onGloballyPositioned { onBag(it.boundsInRoot().center) }.scale(scale).background(Abyss, CircleShape).border(1.dp, if (scale > 1f) Gold else Bronze, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ForgeGlyphs.Stash, null, tint = Brass, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(
                if (waiting > 0) ui("run.bag_waiting", waiting, plural("run.chest", waiting)) else ui("run.bag"),
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                ui("run.bag_gathered", gathered),
                color = Parchment,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
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

/** Пульс сумки при ответе: размах и длительность; сколько висят «+N» и строка лучшей вещи. */
private const val PULSE_SCALE = 1.18f
private const val PULSE_MS = 300
private const val NEWS_MS = 2_600L

/** Плашка сумки: отступы от края экрана, размер значка и фишки веера. */
private val TRAY_END = 12.dp
private val TRAY_BOTTOM = 24.dp

/** Место слева под рамку «рук» (пояс и четыре кнопки, ≈222 dp) с зазором: сумка и новость не заходят на кнопки. */
private val HANDS_RESERVE = 232.dp

/** Наименьшая ширина сумки, если экран уже рамки «рук». */
private val TRAY_MIN = 96.dp
private val BAG = 30.dp
private val CHIP = 24.dp
