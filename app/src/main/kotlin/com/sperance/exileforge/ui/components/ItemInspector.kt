package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.itemSeals
import com.sperance.exileforge.core.sheet.unmetNeeds
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.screens.hero.StackInfoSheet
import com.sperance.exileforge.ui.screens.hero.WearChoice
import com.sperance.exileforge.ui.screens.hero.rememberWearChoice
import com.sperance.exileforge.ui.screens.hero.wearTotals
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment

/**
 * Что показать в карточке предмета: где бы его ни коснулись - строка, плитка, чип, значок. Под карточкой - действия места:
 * главное [action], равное ему второе [side] рядом и второстепенные [menu] в меню «⋯»; у своей вещи под ними - ряд действий по
 * умолчанию ([OwnItemActions]).
 */
sealed interface Inspect {
    /** Главная кнопка под карточкой - прежнее действие места касания; null - карточка только смотрит. */
    val action: InspectAction?

    /** Второе действие, равное главному, - рядом с ним; null - главное во всю ширину. */
    val side: InspectAction? get() = null

    /** Второстепенные действия - меню «⋯» рядом с кнопками; пусто - меню нет. */
    val menu: List<InspectAction> get() = emptyList()

    /**
     * Копия снаряжения [item]: полная карточка с итогом «если надеть». [forge] - предлагать своей вещи «В кузницу»;
     * место, что само ведёт в кузницу, его снимает.
     */
    data class Copy(
        val item: ItemInstance,
        override val action: InspectAction? = null,
        override val side: InspectAction? = null,
        override val menu: List<InspectAction> = emptyList(),
        val forge: Boolean = true,
    ) : Inspect

    /** Шаблон [template] витриной: месту известен только шаблон (вложение письма, сделанное ремеслом). */
    data class Showcase(val template: String, override val action: InspectAction? = null) : Inspect

    /** Стопка сумки [code]: сферы, эссенции, катализаторы, материалы, книги, скарабеи, сундуки. */
    data class Stack(val code: String, override val action: InspectAction? = null) : Inspect
}

/** Что выбрано в карточке к моменту действия: [wearInto] - место надевания по вкладкам сравнения ([WearChoice.target]); null - решает слот шаблона. */
class InspectChoice(val wearInto: Slot?)

/**
 * Действие места под карточкой: [label] на кнопке, [run] выполняется с выбором карточки, и карточка закрывается. [reason] - почему
 * кнопка погашена, строкой под ней. [hold] - действие по удержанию ([Hold], только у одиночной главной кнопки), [figure] - цена
 * в его ярлыке или справа в строке меню. [notes] - что действие изменит, строками над кнопками подвала (у главного действия).
 */
data class InspectAction(
    val label: String,
    val enabled: Boolean = true,
    val reason: String? = null,
    val hold: Boolean = false,
    val figure: String? = null,
    val notes: (@Composable ColumnScope.() -> Unit)? = null,
    val run: InspectChoice.() -> Unit,
)

/** Как открыть карточку предмета отсюда; null - хозяина нет, касание ничего не открывает. */
val LocalItemInspector = staticCompositionLocalOf<((Inspect) -> Unit)?> { null }

private val NO_INSPECT: (Inspect) -> Unit = {}

/** Открыватель карточки предмета из [LocalItemInspector]; без хозяина - пустой. */
@Composable fun rememberInspect(): (Inspect) -> Unit = LocalItemInspector.current ?: NO_INSPECT

/** Хозяин карточек предметов: всё внутри открывает их через [LocalItemInspector]; лист один, поверх ([ItemCardSheet]). */
@Composable fun ItemInspectorHost(game: GameUi, content: @Composable () -> Unit) {
    var open by remember { mutableStateOf<Inspect?>(null) }
    val opener: (Inspect) -> Unit = remember { { open = it } }
    CompositionLocalProvider(LocalItemInspector provides opener) { content() }
    open?.let { shown -> ItemCardSheet(game, shown) { open = null } }
}

/**
 * Лист карточки предмета [shown] - один для хозяина карточек и мест, что держат лист сами (фляга в бою): стопка, копия с ценой и
 * итогом «если надеть», витрина шаблона; под карточкой - действия места. Закрытие и любое действие - [onDismiss].
 */
@Composable fun ItemCardSheet(game: GameUi, shown: Inspect, onDismiss: () -> Unit) {
    val footer: @Composable ColumnScope.() -> Unit = { InspectActions(shown, NO_CHOICE, onDismiss) }
    when (shown) {
        is Inspect.Stack -> StackInfoSheet(game, shown.code, footer, onDismiss)

        is Inspect.Copy -> game.view(shown.item)?.let { CopySheet(game, it, shown, onDismiss) }

        is Inspect.Showcase -> {
            val index = game.index ?: return
            val view = remember(shown.template, index) { index.template(shown.template)?.let { ItemView.showcase(it, index) } } ?: return
            CardSheet(onDismiss, footer.takeIf { shown.action != null }) {
                ItemCard(view, enabled = false, detailed = true, action = false)
            }
        }
    }
}

/** Выбор карточки без вкладок мест: стопка и витрина. */
private val NO_CHOICE = InspectChoice(null)

/**
 * Карточка копии - как у добычи и товара торговца: итог «если надеть», выполнены ли требования; действия знают выбранное место.
 * Своя вещь тайника без действия места получает главным «Надеть» ([ownWearAction]), если её можно надеть, и под действиями - ряд действий
 * по умолчанию ([OwnItemActions]).
 */
@Composable private fun CopySheet(game: GameUi, view: ItemView, shown: Inspect.Copy, onDismiss: () -> Unit) {
    val item = view.item
    val wear = rememberWearChoice(game, item)
    val own = ownWearAction(game, item).takeIf { shown.action == null }
    val acted = own?.let { shown.copy(action = it) } ?: shown
    val acts = acted.action != null || game.hero?.item(item.id) != null
    val footer: @Composable ColumnScope.() -> Unit = {
        InspectActions(acted, InspectChoice(wear.target), onDismiss)
        OwnItemActions(game, item, shown.forge, onDismiss)
    }
    CardSheet(onDismiss, footer.takeIf { acts }) {
        ItemCard(view, enabled = false, detailed = true, totals = wearTotals(game, item, wear), unmet = game.unmetNeeds(item), seals = game.itemSeals(item))
    }
}

/** Лист карточки: карточка прокручивается, подвал [footer] с действиями закреплён внизу листа над чертой; действий нет - подвала нет. */
@Composable private fun CardSheet(onDismiss: () -> Unit, footer: (@Composable ColumnScope.() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
            footer?.let { SheetFooter(it) }
        }
    }
}

/** Закреплённый подвал листа карточки предмета: черта сверху, поля и действия столбцом - общий хозяину карточек и листу вещи тайника. */
@Composable internal fun SheetFooter(content: @Composable ColumnScope.() -> Unit) {
    HorizontalDivider(color = Bronze)
    Column(Modifier.fillMaxWidth().background(Panel).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

/**
 * Действия карточки [shown]: одно главное - во всю ширину ([InspectButton]); с равным вторым или меню - строкой: главное, второе и
 * квадрат «⋯» с второстепенными. Над ними - что изменит главное ([InspectAction.notes]). Действие выполняется с выбором [choice] и
 * закрывает лист ([onDone]); почему погашено - под строкой.
 */
@Composable internal fun ColumnScope.InspectActions(shown: Inspect, choice: InspectChoice, onDone: () -> Unit) {
    val main = shown.action ?: return
    main.notes?.invoke(this)
    val side = shown.side
    if (side == null && shown.menu.isEmpty()) {
        InspectButton(main, choice, onDone)
        return
    }
    val run = { action: InspectAction ->
        action.run(choice)
        onDone()
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        ForgeButton(onClick = { run(main) }, state = ButtonState.of(main.enabled), modifier = Modifier.weight(1f), contentPadding = RowPadding) { Text(main.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        side?.let { ForgeButton(onClick = { run(it) }, state = ButtonState.of(it.enabled), modifier = Modifier.weight(1f), contentPadding = RowPadding, kind = ButtonKind.Secondary) { Text(it.label, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
        if (shown.menu.isNotEmpty()) MoreActions(shown.menu, run)
    }
    listOfNotNull(main, side).filter { !it.enabled }.mapNotNull { it.reason }.forEach { Text(it, color = LifeRed, style = MaterialTheme.typography.bodySmall) }
}

/** Квадрат «⋯» и его меню: строка - слово действия и справа [InspectAction.figure] золотом. */
@Composable private fun MoreActions(actions: List<InspectAction>, run: (InspectAction) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        ForgeButton(onClick = { open = true }, modifier = Modifier.width(MORE_WIDTH), contentPadding = PaddingValues(0.dp), kind = ButtonKind.Secondary) {
            Icon(Icons.Outlined.MoreHoriz, ui("common.more"), modifier = Modifier.size(18.dp))
        }
        ForgeMenu(expanded = open, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(action.label, color = if (action.enabled) Parchment else Muted.copy(alpha = .6f), modifier = Modifier.weight(1f, fill = false))
                            action.figure?.let { Text(it, color = Gold, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                        }
                    },
                    enabled = action.enabled,
                    onClick = {
                        open = false
                        run(action)
                    },
                )
            }
        }
    }
}

/** Ширина квадрата «⋯» у кнопок карточки. */
private val MORE_WIDTH = 48.dp

/** Поля кнопок строкой: две равные и «⋯» в ширину листа на 360 dp - подписи «В кузницу» и «To the forge» целиком. */
private val RowPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)

/** Главная кнопка карточки во всю ширину: выполняет действие места с выбором [choice] и закрывает лист. */
@Composable internal fun InspectButton(action: InspectAction, choice: InspectChoice = NO_CHOICE, onDone: () -> Unit) {
    val run = {
        action.run(choice)
        onDone()
    }
    if (action.hold) {
        ForgeButton(onClick = run, modifier = Modifier.fillMaxWidth(), state = ButtonState.of(action.enabled), hold = Hold()) {
            Text(action.label, Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
            action.figure?.let { ButtonFigure(it) }
        }
    } else {
        ForgeButton(onClick = run, state = ButtonState.of(action.enabled), modifier = Modifier.fillMaxWidth()) { Text(action.label) }
    }
    action.reason?.takeIf { !action.enabled }?.let { Text(it, color = LifeRed, style = MaterialTheme.typography.bodySmall) }
}
