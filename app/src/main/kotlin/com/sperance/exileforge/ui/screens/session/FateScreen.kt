package com.sperance.exileforge.ui.screens.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.presentation.session.FateViewModel
import com.sperance.exileforge.ui.components.AltarGlow
import com.sperance.exileforge.ui.components.EmberField
import com.sperance.exileforge.ui.components.FateInk
import com.sperance.exileforge.ui.components.FateTarot
import com.sperance.exileforge.ui.components.HoldToAccept
import com.sperance.exileforge.ui.components.LanguageButton
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.ToastHost
import com.sperance.exileforge.ui.components.ashGround
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Выбор Предначертания аккаунта (4.6.0, макет «Пепел предков» - вариант 1, утверждён владельцем): до списка героев, пока дар
 * не выбран, - у нового аккаунта и у прежнего одинаково. Тёплый алтарь дышит внизу, со дна поднимаются угли; заголовок
 * проявляется золотом; три карты выезжают из огня веером и переворачиваются по одной. Касание выбирает карту (она поднимается
 * и светится, прочие гаснут), кнопку держат ~1,2 с - золотая вспышка, и экран уходит к героям. Декор стоит при выключенных
 * анимациях, раздача тогда мгновенна.
 */
@Composable fun FateScreen() {
    val vm = koinViewModel<FateViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val view = game.session.fate
    val offers = view?.offers.orEmpty()
    val chosen = view?.chosen
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf(false) }
    val motion = LocalMotion.current
    Box(Modifier.fillMaxSize().ashGround()) {
        AltarGlow(Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp).size(280.dp, 60.dp))
        EmberField(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            FateTitle(motion, Modifier.fillMaxWidth().padding(top = 34.dp))
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                when {
                    offers.isNotEmpty() -> FateDeck(offers, picked ?: chosen?.code, chosen != null, motion) { code ->
                        if (chosen == null && !game.busy) picked = code
                    }

                    game.busy || game.reading -> CircularProgressIndicator(color = Gold)

                    else -> TextButton(onClick = vm::refresh) { Text(ui("fate.retry"), color = FateInk.GoldLight) }
                }
            }
            FateFooter(motion, offers.isNotEmpty(), picked != null, chosen != null, hint) { refused ->
                if (refused) hint = true else picked?.let(vm::choose)
            }
        }
        FateFlash(chosen != null, motion, onDone = vm::accepted)
        TextButton(onClick = vm::logout, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp), enabled = !game.busy) {
            Text(ui("fate.sign_out"), color = Muted, style = MaterialTheme.typography.labelMedium)
        }
        LanguageButton(game.lang, game.world.languages, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 6.dp, end = 6.dp), enabled = !game.busy, onLanguage = vm::language)
        ToastHost(game, vm::dismissMessage, vm::dismissNotice, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp))
    }
}

/** Заголовок с золотым ореолом и подзаголовок: проявляются снизу вверх (1,2 с), подзаголовок - позже. */
@Composable private fun FateTitle(motion: Boolean, modifier: Modifier) {
    val title = remember { Animatable(if (motion) 0f else 1f) }
    val sub = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(Unit) {
        launch {
            delay(TITLE_DELAY)
            title.animateTo(1f, tween(FADE_MS))
        }
        delay(SUBTITLE_DELAY)
        sub.animateTo(1f, tween(FADE_MS))
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            ui("fate.title"),
            style = relicName(21).copy(color = FateInk.GoldLight, letterSpacing = 4.6.sp, shadow = Shadow(FateInk.Gold.copy(alpha = .55f), blurRadius = 36f)),
            modifier = Modifier.graphicsLayer {
                alpha = title.value
                translationY = (1 - title.value) * 10.dp.toPx()
            },
        )
        Text(
            ui("fate.subtitle"),
            color = Color(0xFFB9BFCC),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp).graphicsLayer {
                alpha = sub.value
                translationY = (1 - sub.value) * 10.dp.toPx()
            },
        )
    }
}

/** Место карты в веере: сдвиг по ширине и высоте и наклон. */
private class FanSpot(val x: Float, val y: Float, val tilt: Float)

private val FAN = listOf(FanSpot(-1f, 22f, -9f), FanSpot(0f, 0f, 0f), FanSpot(1f, 22f, 9f))

/**
 * Веер из трёх карт: каждая выезжает из огня (снизу, малой, рубашкой), встаёт на своё место и переворачивается по очереди.
 * Выбранная [picked] поднимается, выпрямляется и растёт, прочие гаснут; [locked] - дар уже принят, касания не нужны.
 */
@Composable private fun FateDeck(offers: List<FateCard>, picked: String?, locked: Boolean, motion: Boolean, onPick: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.TopCenter) {
        val cardWidth = (maxWidth / 3.3f).coerceAtMost(116.dp)
        val step = cardWidth * 1.13f
        offers.take(FAN.size).forEachIndexed { i, card ->
            val spot = FAN[i]
            val deal = remember(card.code) { Animatable(if (motion) 0f else 1f) }
            val flip = remember(card.code) { Animatable(if (motion) 0f else 1f) }
            LaunchedEffect(card.code) {
                launch {
                    delay(DEAL_DELAY + i * DEAL_STEP)
                    deal.animateTo(1f, tween(TURN_MS, easing = Overshoot))
                }
                delay(FLIP_DELAY + i * FLIP_STEP)
                flip.animateTo(1f, tween(TURN_MS, easing = Overshoot))
            }
            val selected = picked == card.code
            val dimmed = picked != null && !selected
            val lift by animateFloatAsState(if (selected) 1f else 0f, tween(TURN_MS, easing = Overshoot), label = "fate-lift")
            val shade by animateFloatAsState(if (dimmed) 1f else 0f, tween(400), label = "fate-dim")
            Box(
                Modifier.zIndex(if (selected) 5f else 1f)
                    .offset(step * spot.x, (spot.y - 22f * lift).dp)
                    .size(cardWidth, cardWidth * 1.75f)
                    .graphicsLayer {
                        val d = deal.value
                        alpha = d.coerceIn(0f, 1f)
                        translationY = (1 - d) * 260.dp.toPx()
                        val grown = .7f + .3f * d
                        val scale = grown * (1f + .12f * lift - .05f * shade)
                        scaleX = scale
                        scaleY = scale
                        rotationZ = spot.tilt * (1 - lift)
                    }
                    .clickable(remember { MutableInteractionSource() }, null, enabled = !locked && flip.value >= 1f) { onPick(card.code) },
            ) {
                if (lift > 0f) {
                    // Свечение выбранной - за картой, шире её
                    Canvas(Modifier.matchParentSize()) {
                        drawRect(
                            Brush.radialGradient(listOf(FateInk.Gold.copy(alpha = .55f * lift), Color.Transparent), center, size.maxDimension * .75f),
                            topLeft = Offset(-size.width * .2f, -size.height * .2f),
                            size = size.copy(width = size.width * 1.4f, height = size.height * 1.4f),
                        )
                    }
                }
                FateTarot(card, 180f * (1 - flip.value), Modifier.matchParentSize())
                if (shade > 0f) {
                    Canvas(Modifier.matchParentSize()) { drawRoundRect(Color.Black.copy(alpha = .45f * shade), cornerRadius = CornerRadius(14.dp.toPx())) }
                }
            }
        }
    }
}

/** Низ экрана: кнопка удержания и предупреждение «навсегда» - проявляются после раздачи. */
@Composable private fun FateFooter(motion: Boolean, dealt: Boolean, picked: Boolean, accepted: Boolean, hint: Boolean, onHold: (refused: Boolean) -> Unit) {
    val shown = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(dealt) {
        if (!dealt) return@LaunchedEffect
        delay(FOOTER_DELAY)
        shown.animateTo(1f, tween(800))
    }
    Column(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 26.dp).graphicsLayer {
            alpha = shown.value
            translationY = (1 - shown.value) * 10.dp.toPx()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val label = when {
            accepted -> ui("fate.accepted")
            hint && !picked -> ui("fate.pick_first")
            else -> ui("fate.hold")
        }
        HoldToAccept(label, Modifier.fillMaxWidth(), enabled = picked && dealt, done = accepted, onRefused = { onHold(true) }) { onHold(false) }
        Text(ui("fate.warning"), color = FateInk.Warn, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

/**
 * Золотая вспышка принятия (1,4 с) и уход к героям ([onDone]). Без анимаций - уход сразу. Переход экрана - не декор: вспышка
 * играет всегда, когда анимации включены.
 */
@Composable private fun FateFlash(go: Boolean, motion: Boolean, onDone: () -> Unit) {
    val flash = remember { Animatable(0f) }
    LaunchedEffect(go) {
        if (!go) return@LaunchedEffect
        if (motion) {
            flash.animateTo(1f, tween(FLASH_RISE_MS, easing = FastOutSlowInEasing))
            flash.animateTo(0f, tween(FLASH_MS - FLASH_RISE_MS))
        }
        onDone()
    }
    if (flash.value > 0f) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = flash.value }) {
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xF2FFE6AA), FateInk.Gold.copy(alpha = .4f), Color.Transparent),
                    Offset(size.width / 2, size.height * .45f),
                    size.maxDimension * .65f,
                ),
            )
        }
    }
}

/** Упругий выход карты, как в макете: cubic-bezier(.2, .9, .3, 1.2). */
private val Overshoot = CubicBezierEasing(.2f, .9f, .3f, 1.2f)

/** Тайминги макета, мс. */
private const val TITLE_DELAY = 200L
private const val SUBTITLE_DELAY = 700L
private const val FADE_MS = 1_200
private const val DEAL_DELAY = 700L
private const val DEAL_STEP = 260L
private const val FLIP_DELAY = 1_700L
private const val FLIP_STEP = 320L
private const val TURN_MS = 550
private const val FOOTER_DELAY = 2_300L
private const val FLASH_MS = 1_400
private const val FLASH_RISE_MS = 210
