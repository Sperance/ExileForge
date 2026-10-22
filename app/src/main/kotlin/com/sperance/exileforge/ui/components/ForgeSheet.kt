package com.sperance.exileforge.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.PanelRaised

/** Шторка «Мягкого» стиля: скругление 22 dp сверху. */
val SheetShape: Shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)

/**
 * Тело листа: листается ли оно само и с какими полями и промежутками раскладывает содержимое.
 *
 * [padding] - поля тела, [spacing] - промежуток между его строками.
 */
@Immutable
sealed interface SheetBody {
    val padding: PaddingValues
    val spacing: Dp

    /**
     * Тело листается целиком (`verticalScroll`): форма с полями ввода. Ленивых списков и своих прокруток внутри нет - сетка
     * выбора раскладывается неленивой.
     */
    data class Scroll(override val padding: PaddingValues = ForgeSheetDefaults.BodyPadding, override val spacing: Dp = ForgeSheetDefaults.BodySpacing) : SheetBody

    /**
     * Тело не листается: его высота - остаток листа под шапкой и над подвалом, лишнее обрезается. Длинное содержимое листает себя
     * само: `LazyColumn` с `weight(1f)` или `fillMaxHeight`. Черты над подвалом нет - тело под него не уходит.
     */
    data class Fixed(override val padding: PaddingValues = ForgeSheetDefaults.BodyPadding, override val spacing: Dp = ForgeSheetDefaults.BodySpacing) : SheetBody
}

/** Поля и промежутки тела листа по умолчанию. */
object ForgeSheetDefaults {
    /** Поля тела: 16 dp по бокам, 12 dp снизу; сверху место даёт ручка листа. */
    val BodyPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp)

    /** Промежуток между строками тела. */
    val BodySpacing = 10.dp

    /** Тело как есть - без полей и промежутков, не листается: для листов, что раскладывают себя сами. */
    val Bare: SheetBody = SheetBody.Fixed(PaddingValues(0.dp), 0.dp)
}

/**
 * Любой нижний лист игры: `ModalBottomSheet` Material в трёх границах.
 *
 * - **Одна остановка.** Полуоткрытой нет (`skipPartiallyExpanded`): при содержимом около половины экрана две остановки почти
 *   сходились, и бросок кидал лист между ними.
 * - **Не у самого верха.** Содержимое - не выше [MAX_SHARE] окна приложения (окно берётся снаружи окна листа, его высота читается в
 *   раскладке), так что лист не доходит до строки состояния; своих отступов лист не берёт (`contentWindowInsets` пусты), и ни один
 *   отступ не включается и не гаснет на ходу. Полоса жестов и клавиатура отступают здесь, один раз, в цвете листа.
 * - **Бросок остаётся в списке.** Остаток броска у верха списка съедается здесь, а не уходит листу: иначе лист дёргался к закрытию
 *   и обратно. Протяжка пальцем доходит до листа - тянуть вниз от верха списка по-прежнему закрывает его.
 *
 * Шапка, тело и подвал. [header] - закреплённая шапка над телом (кто написал письмо), [footer] - закреплённый подвал (кнопки
 * действия во всю ширину): они не листаются, подвал стоит над клавиатурой. Шапка не выше места, что остаётся телу и подвалу.
 * Тело - по [body]: [SheetBody.Scroll] листается само, [SheetBody.Fixed] - нет; по умолчанию [ForgeSheetDefaults.Bare]. Если
 * клавиатура открыта и телу рядом с шапкой и подвалом не остаётся [MIN_BODY], подвал прячется, пока клавиатура открыта или место не
 * вернётся с запасом [HYSTERESIS], - поле с фокусом остаётся видно. Решает раскладка ([SheetFramePolicy]): клавиатура и размеры не
 * перекомпоновывают лист. [contentModifier] ложится на всё содержимое листа под ручкой (полоса редкости подтверждения).
 *
 * Закрытие доходит до вызывающего один раз, сколько бы раз о нём ни сказали жест, затемнение и «назад». Лист с [held] держится: ни
 * жест, ни затемнение, ни «назад» его не закрывают и ручки у него нет - закрывает только его кнопка (итог, который нельзя пропустить).
 *
 * Аккорд громкости работает и поверх листа: у листа своё окно.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgeSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = SheetShape,
    containerColor: Color = PanelRaised,
    dragHandle: (@Composable () -> Unit)? = { BottomSheetDefaults.DragHandle() },
    body: SheetBody = ForgeSheetDefaults.Bare,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    footer: (@Composable RowScope.() -> Unit)? = null,
    contentModifier: Modifier = Modifier,
    held: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dismiss by rememberUpdatedState(onDismissRequest)
    var dismissed by remember { mutableStateOf(false) }
    val window = LocalWindowInfo.current
    ModalBottomSheet(
        onDismissRequest = {
            if (!dismissed) {
                dismissed = true
                dismiss()
            }
        },
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !held || it != SheetValue.Hidden }),
        sheetGesturesEnabled = !held,
        shape = shape,
        containerColor = containerColor,
        dragHandle = dragHandle.takeUnless { held },
        contentWindowInsets = { WindowInsets(0.dp) },
    ) {
        ChordOverWindow()
        SheetFrame(
            body = body,
            header = header,
            footer = footer,
            content = content,
            modifier = contentModifier
                .fillMaxWidth()
                .windowShare(window)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .nestedScroll(remember { FlingStaysInList() }),
        )
    }
}

/** Предел высоты - [MAX_SHARE] окна приложения [window]; высота окна читается в раскладке, не в композиции. Окна нет - предела нет. */
private fun Modifier.windowShare(window: WindowInfo): Modifier = layout { measurable, constraints ->
    val height = window.containerSize.height
    val limit = if (height > 0) (height * MAX_SHARE).toInt().coerceIn(constraints.minHeight, constraints.maxHeight) else constraints.maxHeight
    val placeable = measurable.measure(constraints.copy(maxHeight = limit))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** Тело листа между закреплёнными шапкой и подвалом: тело берёт место, что они оставили; подвал меряется по себе и не сжимается. */
@Composable
private fun SheetFrame(
    body: SheetBody,
    header: (@Composable ColumnScope.() -> Unit)?,
    footer: (@Composable RowScope.() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier,
) {
    val scroll = if (body is SheetBody.Scroll) rememberScrollState() else null
    val ime = WindowInsets.ime
    val policy = remember(ime) { SheetFramePolicy(ime) }
    Layout(
        contents = listOf(
            { header?.let { Column(Modifier.fillMaxWidth().clipToBounds(), content = it) } },
            {
                Column(
                    Modifier.fillMaxWidth().then(scroll?.let { Modifier.verticalScroll(it) } ?: Modifier.clipToBounds()).padding(body.padding),
                    verticalArrangement = Arrangement.spacedBy(body.spacing),
                    content = content,
                )
            },
            { footer?.let { SheetFooter(scroll, it) } },
        ),
        modifier = modifier,
        measurePolicy = policy,
    )
}

/**
 * Раскладка шапки, тела и подвала. Подвал меряется без предела высоты и не сжимается; шапка - не выше места, что остаётся после
 * подвала и [MIN_BODY] тела; тело берёт остаток. Высоту клавиатуры [ime] политика читает только в фазе измерения: её ход двигает
 * раскладку, не композицию. Спрятан ли подвал, политика помнит между измерениями: прячет, когда телу при открытой клавиатуре не
 * хватает [MIN_BODY], и возвращает, когда клавиатура закрыта или места снова хватает с запасом [HYSTERESIS], - у порога подвал не
 * мигает.
 *
 * Внутренние размеры - сумма высот частей и наибольшая их ширина; у тела [SheetBody.Fixed] с ленивым списком их нет, и лист внутри
 * `IntrinsicSize` не кладут.
 */
private class SheetFramePolicy(private val ime: WindowInsets) : MultiContentMeasurePolicy {
    private var hidden = false

    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val (headers, bodies, footers) = measurables
        val bounded = constraints.hasBoundedHeight
        val room = constraints.maxHeight
        val loose = constraints.copy(minHeight = 0)
        val free = loose.copy(maxHeight = Constraints.Infinity)
        val foot = footers.firstOrNull()?.measure(free)
        val minBody = MIN_BODY.roundToPx()
        val headMax = if (bounded) (room - minBody - (foot?.height ?: 0)).coerceAtLeast(0) else Constraints.Infinity
        val head = headers.firstOrNull()?.measure(loose.copy(maxHeight = headMax))
        val headHeight = head?.height ?: 0
        val spare = room - headHeight - (foot?.height ?: 0)
        hidden = when {
            foot == null || !bounded || ime.getBottom(this) == 0 -> false
            hidden -> spare < minBody + HYSTERESIS.roundToPx()
            else -> spare < minBody
        }
        val shown = foot?.takeUnless { hidden }
        val footHeight = shown?.height ?: 0
        val left = if (bounded) (room - headHeight - footHeight).coerceAtLeast(0) else Constraints.Infinity
        val main = bodies.firstOrNull()?.measure(loose.copy(maxHeight = left))
        val height = (headHeight + (main?.height ?: 0) + footHeight).coerceIn(constraints.minHeight, room)
        return layout(constraints.maxWidth, height) {
            head?.place(0, 0)
            main?.place(0, headHeight)
            shown?.place(0, height - footHeight)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<List<IntrinsicMeasurable>>, width: Int): Int = measurables.sumOf { part -> part.sumOf { it.minIntrinsicHeight(width) } }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<List<IntrinsicMeasurable>>, width: Int): Int = measurables.sumOf { part -> part.sumOf { it.maxIntrinsicHeight(width) } }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<List<IntrinsicMeasurable>>, height: Int): Int = measurables.flatten().maxOfOrNull { it.minIntrinsicWidth(height) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<List<IntrinsicMeasurable>>, height: Int): Int = measurables.flatten().maxOfOrNull { it.maxIntrinsicWidth(height) } ?: 0
}

/**
 * Подвал: ряд кнопок с полями листа. Черта сверху - пока под подвалом есть ещё тело ([ScrollState.canScrollForward], читается в
 * рисовании); у тела [SheetBody.Fixed] черты нет.
 */
@Composable
private fun SheetFooter(scroll: ScrollState?, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind { if (scroll?.canScrollForward == true) drawLine(Bronze, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * Съедает остаток броска у края содержимого; протяжка пальцем уходит листу нетронутой. Отпускание после того, как палец сдвинул
 * лист, отдаёт ему скорость, и лист встаёт - открытым или закрытым, а не на полпути; бросок, что только листал список, кончается в
 * списке.
 */
private class FlingStaysInList : NestedScrollConnection {
    private var sheetMoved = false

    /** Пока лист сдвинут пальцем, остаток броска не съедается: иначе бросок списка крутился бы вхолостую, а лист стоял полузакрытым. */
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = when {
        source == NestedScrollSource.SideEffect -> if (sheetMoved) Offset.Zero else available

        available.y != 0f -> {
            sheetMoved = true
            Offset.Zero
        }

        else -> Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = if (sheetMoved) {
        sheetMoved = false
        Velocity.Zero
    } else {
        available
    }
}

/** Наибольшая доля высоты окна приложения под содержимое листа: с запасом до строки состояния. */
private const val MAX_SHARE = .85f

/** Наименьшая высота тела над открытой клавиатурой рядом с подвалом: поле ввода с подписью; меньше - подвал прячется. */
private val MIN_BODY = 120.dp

/** Запас сверх [MIN_BODY], с которым спрятанный подвал возвращается при открытой клавиатуре: у порога он не мигает. */
private val HYSTERESIS = 24.dp
