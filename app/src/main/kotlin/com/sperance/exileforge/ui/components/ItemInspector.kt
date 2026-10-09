package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.screens.hero.StackInfoSheet
import com.sperance.exileforge.ui.screens.hero.rememberWearChoice
import com.sperance.exileforge.ui.screens.hero.wearTotals

/** Что показать в карточке предмета (4.3.2): где бы его ни коснулись - строка, плитка, чип, значок. */
sealed interface Inspect {
    /** Главная кнопка под карточкой - прежнее действие места касания; null - карточка только смотрит. */
    val action: InspectAction?

    /** Копия снаряжения [item]: полная карточка с ценой торговца и итогом «если надеть». */
    data class Copy(val item: ItemInstance, override val action: InspectAction? = null) : Inspect

    /** Шаблон [template] витриной: месту известен только шаблон (вложение письма, сделанное ремеслом). */
    data class Showcase(val template: String, override val action: InspectAction? = null) : Inspect

    /** Стопка сумки [code]: сферы, эссенции, катализаторы, материалы, книги, скарабеи, сундуки, яйца. */
    data class Stack(val code: String, override val action: InspectAction? = null) : Inspect
}

/** Действие места под карточкой: [label] на кнопке, [run] выполняется, и карточка закрывается. */
data class InspectAction(val label: String, val enabled: Boolean = true, val run: () -> Unit)

/** Как открыть карточку предмета отсюда; null - хозяина нет, касание ничего не открывает. */
val LocalItemInspector = staticCompositionLocalOf<((Inspect) -> Unit)?> { null }

private val NO_INSPECT: (Inspect) -> Unit = {}

/** Открыватель карточки предмета из [LocalItemInspector]; без хозяина - пустой. */
@Composable fun rememberInspect(): (Inspect) -> Unit = LocalItemInspector.current ?: NO_INSPECT

/** Хозяин карточек предметов (4.3.2): всё внутри открывает их через [LocalItemInspector]; лист один, поверх. */
@Composable fun ItemInspectorHost(game: GameUi, content: @Composable () -> Unit) {
    var open by remember { mutableStateOf<Inspect?>(null) }
    val opener: (Inspect) -> Unit = remember { { open = it } }
    CompositionLocalProvider(LocalItemInspector provides opener) { content() }
    val shown = open ?: return
    val close = { open = null }
    val footer: @Composable ColumnScope.() -> Unit = { shown.action?.let { InspectButton(it, close) } }
    when (shown) {
        is Inspect.Stack -> StackInfoSheet(game, shown.code, footer, close)

        is Inspect.Copy -> game.view(shown.item)?.let { CopySheet(game, it, footer, close) }

        is Inspect.Showcase -> {
            val index = game.index ?: return
            val view = remember(shown.template, index) { index.template(shown.template)?.let { ItemView.showcase(it, index) } } ?: return
            CardSheet(close) {
                ItemCard(view, enabled = false, detailed = true, action = false)
                footer()
            }
        }
    }
}

/** Карточка копии - как у добычи и товара торговца: цена, итог «если надеть», выполнены ли требования. */
@Composable private fun CopySheet(game: GameUi, view: ItemView, footer: @Composable ColumnScope.() -> Unit, onDismiss: () -> Unit) {
    val item = view.item
    CardSheet(onDismiss) {
        ItemCard(view, enabled = false, detailed = true, price = game.sellPrice(item), totals = wearTotals(game, item, rememberWearChoice(game, item)), requirementsMet = game.unmetFor(item.template).isEmpty())
        footer()
    }
}

/** Лист карточки: прокрутка, поля, кнопка места внизу. */
@Composable private fun CardSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** Главная кнопка карточки: выполняет действие места и закрывает лист. */
@Composable private fun InspectButton(action: InspectAction, onDone: () -> Unit) {
    ForgeButton(
        onClick = {
            action.run()
            onDone()
        },
        enabled = action.enabled,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(action.label) }
}
