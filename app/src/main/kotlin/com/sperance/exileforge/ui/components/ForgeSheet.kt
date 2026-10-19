package com.sperance.exileforge.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.PanelRaised

/** Шторка «Мягкого» стиля: скругление 22 dp сверху. */
val SheetShape: Shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)

/**
 * Любой нижний лист игры: `ModalBottomSheet` Material в трёх границах.
 *
 * - **Одна остановка.** Полуоткрытой нет (`skipPartiallyExpanded`): при содержимом около половины экрана две остановки почти
 *   сходились, и бросок кидал лист между ними.
 * - **Не у самого верха.** Содержимое - не выше [MAX_SHARE] окна приложения (окно читается снаружи окна листа), так что лист не
 *   доходит до строки состояния; своих отступов лист не берёт (`contentWindowInsets` пусты), и ни один отступ не включается и не
 *   гаснет на ходу. Полоса жестов и клавиатура отступают здесь, один раз, в цвете листа.
 * - **Бросок остаётся в списке.** Остаток броска у верха списка съедается здесь, а не уходит листу: иначе лист дёргался к закрытию
 *   и обратно. Протяжка пальцем доходит до листа - тянуть вниз от верха списка по-прежнему закрывает его.
 *
 * Шапка, тело и подвал. [header] - закреплённая шапка над телом (кто написал письмо), [footer] - закреплённый подвал (кнопки
 * действия во всю ширину): они не листаются, подвал стоит над клавиатурой и не сжимается. Тело при [scroll] листается само; без него
 * тело держит свои списки (`LazyColumn` с `weight`). Если клавиатура открыта и над ней не остаётся телу хотя бы [MIN_BODY] рядом с
 * шапкой и подвалом, подвал прячется, пока клавиатура открыта, - поле с фокусом остаётся видно. Решение принимает фаза раскладки
 * ([SheetFrame]): клавиатура и размеры не перекомпоновывают лист.
 *
 * Закрытие доходит до вызывающего один раз, сколько бы раз о нём ни сказали жест, затемнение и «назад».
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
    header: (@Composable ColumnScope.() -> Unit)? = null,
    footer: (@Composable RowScope.() -> Unit)? = null,
    scroll: Boolean = footer != null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dismiss by rememberUpdatedState(onDismissRequest)
    var dismissed by remember { mutableStateOf(false) }
    val window = LocalWindowInfo.current.containerSize.height
    val limit: Dp = with(LocalDensity.current) { if (window > 0) (window * MAX_SHARE).toDp() else Dp.Infinity }
    ModalBottomSheet(
        onDismissRequest = {
            if (!dismissed) {
                dismissed = true
                dismiss()
            }
        },
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = shape,
        containerColor = containerColor,
        dragHandle = dragHandle,
        contentWindowInsets = { WindowInsets(0.dp) },
    ) {
        ChordOverWindow()
        SheetFrame(
            scroll = if (scroll) rememberScrollState() else null,
            header = header,
            footer = footer,
            content = content,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = limit)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .nestedScroll(remember { FlingStaysInList() }),
        )
    }
}

/** Тело листа между закреплёнными шапкой и подвалом: тело берёт место, что они оставили, а они меряются по себе и не сжимаются. */
@Composable
private fun SheetFrame(
    scroll: ScrollState?,
    header: (@Composable ColumnScope.() -> Unit)?,
    footer: (@Composable RowScope.() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier,
) {
    val ime = WindowInsets.ime
    val policy = remember(ime) { FooterPolicy(ime) }
    Layout(
        contents = listOf(
            { header?.let { Column(Modifier.fillMaxWidth(), content = it) } },
            { Column(Modifier.fillMaxWidth().then(scroll?.let { Modifier.verticalScroll(it) } ?: Modifier), content = content) },
            { footer?.let { SheetFooter(scroll, it) } },
        ),
        modifier = modifier,
        measurePolicy = policy,
    )
}

/**
 * Раскладка шапки, тела и подвала. Высоту клавиатуры [ime] читает только здесь, в фазе измерения: её ход двигает раскладку, не композицию.
 */
private class FooterPolicy(private val ime: WindowInsets) : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val (headers, bodies, footers) = measurables
        val loose = constraints.copy(minHeight = 0)
        val free = loose.copy(maxHeight = Constraints.Infinity)
        val head = headers.firstOrNull()?.measure(free)
        val headHeight = head?.height ?: 0
        val foot = footers.firstOrNull()?.measure(free)
        val room = constraints.maxHeight
        val keyboard = ime.getBottom(this) > 0
        val shown = foot?.takeUnless { constraints.hasBoundedHeight && keyboard && room - headHeight - it.height < MIN_BODY.roundToPx() }
        val footHeight = shown?.height ?: 0
        val left = if (constraints.hasBoundedHeight) (room - headHeight - footHeight).coerceAtLeast(0) else Constraints.Infinity
        val body = bodies.firstOrNull()?.measure(loose.copy(maxHeight = left))
        val height = (headHeight + (body?.height ?: 0) + footHeight).coerceIn(constraints.minHeight, room)
        return layout(constraints.maxWidth, height) {
            head?.place(0, 0)
            body?.place(0, headHeight)
            shown?.place(0, height - footHeight)
        }
    }
}

/** Подвал: ряд кнопок с полями листа; черта сверху - пока тело листается под ним (читается в рисовании). */
@Composable
private fun SheetFooter(scroll: ScrollState?, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind { if ((scroll?.maxValue ?: 0) > 0) drawLine(Bronze, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
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
