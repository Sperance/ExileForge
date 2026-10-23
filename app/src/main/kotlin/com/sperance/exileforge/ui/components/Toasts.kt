package com.sperance.exileforge.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.Notice
import com.sperance.exileforge.core.app.NoticeKind
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Тост на экране: слова, цвет и знак кружка; [refusal] - отказ: живёт дольше и слышен чтению с экрана сразу; [onDismiss] убирает его. */
private data class Toast(val text: String, val tint: Color, val mark: String, val key: Any, val refusal: Boolean, val onDismiss: () -> Unit) {
    /** Сколько тост живёт сам: отказ и длинный текст - [LONG_MS], прочие - [SHORT_MS]. */
    val lifeMs: Long get() = if (refusal || text.length > LONG_TEXT) LONG_MS else SHORT_MS

    /** Знак на красном кружке (отказ, сбой) - белый, на прочих - тёмный. */
    val markInk: Color get() = if (tint == LifeRed) Color.White else Ink
}

/** Срок обычного тоста, мс. */
private const val SHORT_MS = 3_000L

/** Срок отказа и длинного текста, мс. */
private const val LONG_MS = 4_000L

/** С этой длины текст на узком экране ложится во вторую строку и живёт [LONG_MS]. */
private const val LONG_TEXT = 40

/** Зазор между тостом и верхом того, над чем он лежит. */
private val TOAST_GAP = 12.dp

/** Отступ тоста над полосой навигации, когда у низа экрана ничего не отмечено ([toastFloor]): вход, карта похода, сюжетная сцена. */
private val TOAST_BOTTOM = 20.dp

/**
 * Что лежит у низа окна и не уходит под тост: нижняя панель, плашка обновления, кнопки боя, главная кнопка итогов, лист Атласа. Каждое
 * отмечено [toastFloor] и держит здесь свой верх в координатах окна, пока оно на экране; тост ложится над самым высоким.
 */
@Stable
class ToastFloors {
    private val tops = mutableStateMapOf<Any, Float>()

    /** Верх самого высокого отмеченного; ничего не отмечено - null. */
    val highest: Float? get() = tops.values.minOrNull()

    internal fun put(owner: Any, top: Float) {
        if (tops[owner] != top) tops[owner] = top
    }

    internal fun remove(owner: Any) {
        tops.remove(owner)
    }
}

/** Отметки низа окна для тоста: одни на окно - их даёт корень приложения, без него - слой тостов [ToastLayer]. */
val LocalToastFloors = staticCompositionLocalOf<ToastFloors?> { null }

/** Отмечает элемент у низа окна ([ToastFloors]): пока он на экране, тост слоя [ToastLayer] ложится над его верхом. */
fun Modifier.toastFloor(): Modifier = this then ToastFloorElement

private data object ToastFloorElement : ModifierNodeElement<ToastFloorNode>() {
    override fun create(): ToastFloorNode = ToastFloorNode()

    override fun update(node: ToastFloorNode) = Unit
}

/** Узел [toastFloor]: пишет свой верх в отметки окна при каждой раскладке и снимает его, уходя с экрана. */
private class ToastFloorNode : Modifier.Node(), GlobalPositionAwareModifierNode, CompositionLocalConsumerModifierNode {
    private var floors: ToastFloors? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val now = currentValueOf(LocalToastFloors) ?: return
        floors = now
        now.put(this, coordinates.boundsInWindow().top)
    }

    override fun onDetach() {
        floors?.remove(this)
        floors = null
    }
}

/**
 * Один слой тостов приложения поверх [content] - оболочки, похода, испытания, пролога, сюжетной сцены и прочих хозяев: экраны своих
 * тостов не рисуют. Тост - внизу по центру, над самым высоким из отмеченного у низа ([toastFloor]: нижняя панель, плашка мягкого
 * обновления, кнопки боя), без отметок - над полосой навигации. Окна диалогов и листов - над ним. Место считается при размещении, не
 * в композиции: отметки двигают тост, не перекомпонуя экран.
 */
@Composable fun ToastLayer(game: GameUi, onRefusal: () -> Unit, onNotice: (Notice) -> Unit, content: @Composable () -> Unit) {
    val floors = LocalToastFloors.current ?: remember { ToastFloors() }
    CompositionLocalProvider(LocalToastFloors provides floors) {
        var bottom by remember { mutableFloatStateOf(0f) }
        val navigation = WindowInsets.navigationBars
        Box(Modifier.fillMaxSize().onGloballyPositioned { bottom = it.boundsInWindow().bottom }) {
            content()
            ToastHost(game, onRefusal, onNotice, Modifier.align(Alignment.BottomCenter).offset { IntOffset(0, -lift(floors.highest, bottom, navigation)) })
        }
    }
}

/** Подъём тоста от низа слоя с низом [bottom] в пикселях: над верхом [floor] с зазором или, без отметок, над полосой [navigation]. */
private fun Density.lift(floor: Float?, bottom: Float, navigation: WindowInsets): Int =
    if (floor == null) navigation.getBottom(this) + TOAST_BOTTOM.roundToPx() else (bottom - floor).roundToInt().coerceAtLeast(0) + TOAST_GAP.roundToPx()

/**
 * The toasts («Эфир»): one capsule at the bottom - a refusal in red, a success in ether, loot in the rare item's yellow, a finished
 * craft in frost. It slides up, stays three seconds (four for a refusal or a long text) and leaves by itself; a tap on it sends it away
 * sooner, a tap beside it reaches the screen. A refusal goes first; the successes wait their turn in [Notice] order and follow it.
 */
@Composable private fun ToastHost(game: GameUi, onRefusal: () -> Unit, onNotice: (Notice) -> Unit, modifier: Modifier = Modifier) {
    val toast = game.refusal?.let { Toast(it.read(), LifeRed, "!", it, refusal = true, onRefusal) }
        ?: game.notice?.let { notice -> Toast(notice.text, notice.kind.tint(), notice.kind.mark(), notice.at, refusal = false) { onNotice(notice) } }
    toast?.let {
        LaunchedEffect(it.key) {
            delay(it.lifeMs)
            it.onDismiss()
        }
    }
    AnimatedContent(
        toast,
        modifier.fillMaxWidth().padding(horizontal = 12.dp),
        label = "toast",
        contentKey = { it?.key },
        transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut()) },
    ) { shown ->
        if (shown != null) ToastCard(shown)
    }
}

/** Значок в кружке тоста: ✓ - сделано, i - сообщение; отказ - «!». */
private fun NoticeKind.mark(): String = when (this) {
    NoticeKind.ATLAS -> "i"
    NoticeKind.FAILED -> "!"
    else -> "✓"
}

private fun NoticeKind.tint(): Color = when (this) {
    NoticeKind.DONE -> Gold
    NoticeKind.LOOT -> rarityColor("RARE")
    NoticeKind.CRAFT -> Rune
    NoticeKind.ATLAS -> GoldBright
    NoticeKind.FAILED -> LifeRed
}

/**
 * Тост «Мягкого» стиля: капсула по центру по ширине текста, значок в кружке цвета тоста; длинный текст - во вторую строку, капсула
 * скругляется меньше. Касание убирает его; чтение с экрана слышит его само (отказ - перебивая).
 */
@Composable private fun ToastCard(toast: Toast) {
    val shape = RoundedCornerShape(ToastRound)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.clip(shape).background(ToastFill, shape)
                .semantics { liveRegion = if (toast.refusal) LiveRegionMode.Assertive else LiveRegionMode.Polite }
                .clickable(onClickLabel = ui("common.close"), onClick = toast.onDismiss)
                .padding(start = 9.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(22.dp).background(toast.tint, CircleShape), contentAlignment = Alignment.Center) {
                Text(toast.mark, color = toast.markInk, style = MaterialTheme.typography.labelLarge)
            }
            Text(toast.text, color = Parchment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Скругление капсулы: строка в одну линию - полный полукруг, в две - мягкий угол. */
private val ToastRound = 20.dp

/** Фон капсулы тоста: чуть светлее приподнятой панели, чтобы тост читался над любым экраном. */
private val ToastFill = Color(0xFF1D2B35)
