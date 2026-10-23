package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemSource
import com.sperance.exileforge.core.display.ItemUse
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.RuneText
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.chanceText
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.essenceCondense
import com.sperance.exileforge.core.display.essenceGuarantees
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.itemDetails
import com.sperance.exileforge.core.display.itemSourceIndex
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.itemUseIndex
import com.sperance.exileforge.core.display.key
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.display.title
import com.sperance.exileforge.core.display.tradeName
import com.sperance.exileforge.core.hero.BagFilter
import com.sperance.exileforge.core.hero.BagGroup
import com.sperance.exileforge.core.hero.BagOrder
import com.sperance.exileforge.core.hero.BagSection
import com.sperance.exileforge.core.hero.BagShelf
import com.sperance.exileforge.core.hero.BagStack
import com.sperance.exileforge.core.hero.StackUse
import com.sperance.exileforge.core.hero.StackUses
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.stashFree
import com.sperance.exileforge.rules.content.BagCategory
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.RuneDefinition
import com.sperance.exileforge.ui.components.AccordionGroup
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.CheckMark
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.FilterAccordionSheet
import com.sperance.exileforge.ui.components.FilterSwitch
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.InfoCard
import com.sperance.exileforge.ui.components.InfoMark
import com.sperance.exileforge.ui.components.LocalSettingsEdit
import com.sperance.exileforge.ui.components.LoreChip
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.RaritySpine
import com.sperance.exileforge.ui.components.RowActions
import com.sperance.exileforge.ui.components.RowButton
import com.sperance.exileforge.ui.components.SearchBar
import com.sperance.exileforge.ui.components.SearchFilter
import com.sperance.exileforge.ui.components.SortButton
import com.sperance.exileforge.ui.components.SortLabel
import com.sperance.exileforge.ui.components.SortOption
import com.sperance.exileforge.ui.components.StackCell
import com.sperance.exileforge.ui.components.StackCellSize
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.components.rememberChips
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.screens.skills.SkillPeek
import com.sperance.exileforge.ui.screens.skills.rememberSkillInspect
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Сумка героя на экране ([BagShelf]) под отбором, что помнится на устройстве ([com.sperance.exileforge.core.app.GameSettings.bag]), и поиском [query]; пересобирается,
 * только когда сдвинулись сумка, отбор, поиск, язык или контент.
 */
@Composable fun rememberBagShelf(game: GameUi, query: String): BagShelf {
    val bag = game.hero?.bag.orEmpty()
    val heroClass = game.hero?.heroClass
    return remember(bag, heroClass, game.settings.bag, query, game.lang, game.index) { BagShelf.of(game.index, bag, heroClass, game.settings.bag, query) }
}

/**
 * Строка поиска сумки ([SearchBar]): «Найти в сумке», кнопка фильтра со шторкой ([BagFilterSheet]) и порядок словом ([SortButton]);
 * включённые сужения - чипами под строкой. Отбор и порядок пишутся в настройки устройства ([LocalSettingsEdit]), поиск - нет.
 */
@Composable fun BagSearch(game: GameUi, shelf: BagShelf, query: String, onQuery: (String) -> Unit) {
    val filter = game.settings.bag
    val edit = LocalSettingsEdit.current
    val apply: (BagFilter) -> Unit = remember(edit) { { next -> edit?.invoke { copy(bag = next) } } }
    val narrowings = remember(filter, game.lang) { filter.narrowings(game.lang) }
    val orders = remember(filter, game.lang, apply) { BagOrder.entries.map { order -> SortOption(order.title(game.lang), order == filter.order) { apply(filter.copy(order = order)) } } }
    var sheet by remember { mutableStateOf(false) }
    SearchBar(
        query,
        onQuery,
        ui("bag.search"),
        filter = SearchFilter(rememberChips(narrowings, apply), onOpen = { sheet = true }, onReset = { apply(filter.cleared()) }),
        maxLength = game.inputs.search,
    ) {
        SortButton(SortLabel.Word(filter.order.title(game.lang)), orders)
    }
    if (sheet) {
        BagFilterSheet(game, shelf, filter, query, onDismiss = { sheet = false }) {
            sheet = false
            apply(it)
        }
    }
}

/**
 * Шторка фильтра сумки: полки множеством - списком в два столбца (флажок, имя, число стопок всей сумки справа; пустая полка серая и
 * не нажимается) и «Можно применить» ([StackUses]); ничего не применяется до «Показать N», «Сбросить» снимает всё сразу.
 */
@Composable
private fun BagFilterSheet(game: GameUi, shelf: BagShelf, filter: BagFilter, query: String, onDismiss: () -> Unit, onApply: (BagFilter) -> Unit) {
    var draft by remember { mutableStateOf(filter) }
    val chosen = BagCategory.entries.filter { it in draft.shelves }
    val groups = listOf(
        AccordionGroup("shelves", ui("bag.shelves"), if (chosen.isEmpty()) ui("bag.shelves_all") else chosen.joinToString(", ") { ui(it.key) }) {
            Column {
                BagCategory.entries.chunked(SHELF_COLUMNS).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { category ->
                            val on = category in draft.shelves
                            ShelfOption(ui(category.key), shelf.counts[category] ?: 0, on, Modifier.weight(1f)) {
                                draft = draft.copy(shelves = if (on) draft.shelves - category else draft.shelves + category)
                            }
                        }
                        repeat(SHELF_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
    )
    FilterAccordionSheet(
        groups = groups,
        switches = listOf(FilterSwitch(ui("bag.filter.applicable"), draft.applicable, ui("bag.filter.applicable_hint")) { draft = draft.copy(applicable = it) }),
        show = ui("filter.show", remember(shelf, draft, query) { shelf.count(draft, query) }),
        resettable = draft.narrowed,
        onReset = { onApply(draft.cleared()) },
        onShow = { onApply(draft) },
        onDismiss = onDismiss,
    )
}

/** Полка в шторке фильтра: флажок, имя (переносится, не режется) и число стопок [count] справа; пустая полка серая и не нажимается. */
@Composable
private fun ShelfOption(title: String, count: Int, checked: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val enabled = count > 0
    Row(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).toggleable(checked, enabled = enabled, role = Role.Checkbox) { onToggle() }
            .alpha(if (enabled) 1f else DISABLED_ALPHA).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CheckMark(checked && enabled)
        Text(title, color = if (enabled) Parchment else Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(compactNumber(count.toLong()), color = Muted, style = MaterialTheme.typography.labelMedium)
    }
}

private const val SHELF_COLUMNS = 2
private const val DISABLED_ALPHA = .45f

/**
 * Пусто под отбором и поиском: «Ничего не нашлось», почему - словами ([BagShelf.emptyReason]) и одно действие - снять фильтры, а без
 * них очистить поиск.
 */
@Composable fun BagNothingFound(game: GameUi, shelf: BagShelf, query: String, onQuery: (String) -> Unit) {
    val filter = game.settings.bag
    val edit = LocalSettingsEdit.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        InfoCard(ui("bag.none_title"), shelf.emptyReason(filter, query, game.lang))
        if (filter.narrowed) {
            ForgeButton(onClick = { edit?.invoke { copy(bag = filter.cleared()) } }, kind = ButtonKind.Secondary) { Text(ui("bag.clear_filters")) }
        } else {
            ForgeButton(onClick = { onQuery("") }, kind = ButtonKind.Secondary) { Text(ui("picker.clear_search")) }
        }
    }
}

/**
 * The bag as a table: a heading per category and under it the stack cells ([StackCell]) - as many to a row as fit [CELL]. The name, the rule and the ways on
 * are one tap behind a cell, in [BagSheet]; [onOpen] gets the stack's code. Части полки - под своими подзаголовками ([BagGroup]): у «Книг» героя
 * сначала книги его класса, ниже «Другие классы».
 */
@Composable fun BagGrid(game: GameUi, sections: List<BagSection>, onOpen: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = ((maxWidth + GAP) / (CELL + GAP)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sections.forEach { (category, groups) ->
                Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
                    MutedText(ui(category.key), style = MaterialTheme.typography.labelMedium)
                    groups.forEach { (heading, stacks) ->
                        heading?.let { Text(it.title(), color = Brass, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp)) }
                        stacks.chunked(columns).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                                row.forEach { stack -> StackCell(game.index, stack.code, stack.amount, size = CELL, onClick = { onOpen(stack.code) }) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val CELL = StackCellSize
private val GAP = 6.dp

/**
 * Значок стопки без рамки, как в её ячейке ([StackCell]): стекло сервера, а без него - знак её категории (тусклый у кода, которого
 * контент не знает).
 */
@Composable internal fun StackIcon(game: GameUi, code: String, size: Int) {
    val known = game.index?.item(ItemCode(code))
    BagIcon(code, Modifier.size(size.dp), tint = if (known != null) Gold else Muted, kind = known?.let(::bagVisualKind) ?: ItemVisualKind.ITEM)
}

/**
 * A stack of the bag, opened: its name, how many there are, what it does, and where it goes next.
 *
 * The forge is offered for an orb that is applied to items — Regret is spent on the tree and the
 * server refuses it on an item (`CR_009`), so it has no way in. The auction takes any stack but one bound to the hero
 * (`ContentIndex.bound`): у такой стопки вместо кнопки аукциона - плашка «Привязан к герою» с причиной в ⓘ ([BoundPlate]).
 * Every callback gets the stack's code, but [onRead], which gets the skill's.
 *
 * [onRead] - книга умения класса читается сразу, [onEssence] - эссенция в кузню, [onOpenChest] - сундук-добыча открывается на месте,
 * [onOpenAllChests] - «Открыть все (N)» у стопки больше одного сундука: вся стопка одним запросом, пока в тайнике есть место; тайник полон
 * с самого начала - кнопка погашена с подписью «Тайник полон» (то же правило свободных мест, что у сервера, `StashRules.free`),
 * [onSeal] - печать Атласа в раздел печатей кузни, [onUse] - «Использовать» стопки с действием контента ([BagUseFlow]). Сундук можно и
 * выставить на аукцион, торговцу - нет.
 */
@Composable fun BagSheet(
    game: GameUi,
    stack: BagStack,
    onDismiss: () -> Unit,
    onForge: (String) -> Unit,
    onAuction: (String) -> Unit,
    onRead: (String) -> Unit = {},
    onEssence: (String) -> Unit = {},
    onOpenChest: (String) -> Unit = {},
    onOpenAllChests: (String) -> Unit = {},
    onSeal: (String) -> Unit = {},
    onUse: (String) -> Unit = {},
) {
    val code = stack.code
    val uses = StackUses.of(game.index, code, game.hero?.heroClass)
    val skill = game.index?.skills?.byBook(code)
    val bound = game.index?.bound(code) == true
    StackPanel(onDismiss) {
        StackFace(game, code, stack.amount)
        if (bound) BoundPlate()
        RowActions(Modifier.fillMaxWidth()) {
            val idle = ButtonState.of(!game.busy)
            if (!bound) RowButton(ui("hero.action_auction"), onClick = { onAuction(code) }, kind = ButtonKind.Secondary, state = idle)
            if (StackUse.ORB in uses) RowButton(ui("bag.to_forge"), onClick = { onForge(code) }, state = idle)
            if (StackUse.ESSENCE in uses) RowButton(ui("bag.to_forge"), onClick = { onEssence(code) }, state = idle)
            if (StackUse.SEAL in uses) RowButton(ui("bag.to_forge"), onClick = { onSeal(code) }, state = idle)
            if (StackUse.BOOK in uses && skill != null) {
                RowButton(ui(if ((game.hero?.skills?.level(skill.code) ?: 0) > 0) "bag.read_book" else "bag.learn_book"), onClick = { onRead(skill.code) }, state = idle)
            }
            if (StackUse.CHEST in uses) {
                RowButton(ui("chest.open"), onClick = { onOpenChest(code) }, state = idle, kind = if (stack.amount > 1) ButtonKind.Secondary else ButtonKind.Primary)
                if (stack.amount > 1) {
                    val room = game.stashFree()?.let { it > 0 } ?: true
                    RowButton(
                        if (room) ui("chest.open_all", stack.amount) else ui("chest.stash_full"),
                        onClick = { onOpenAllChests(code) },
                        state = ButtonState.of(!game.busy && room),
                    )
                }
            }
            if (StackUse.USE in uses) RowButton(ui("bag.use"), onClick = { onUse(code) }, state = idle)
        }
    }
}

/** Плашка привязанной к герою стопки: на аукцион и во вклад гильдии её не отдать, почему - в ⓘ. */
@Composable private fun BoundPlate() {
    Row(Modifier.fillMaxWidth().depthInset(RoundedCornerShape(12.dp)).padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(ui("bag.bound"), color = Brass, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        InfoMark(ui("bag.bound"), Brass, tint = Brass) { Text(ui("bag.bound_tip"), color = Parchment, style = MaterialTheme.typography.bodySmall) }
    }
}

/**
 * A stack seen away from the bag — a fight's spoils: its face alone, with no way on, since the run is still underway.
 * [code] is the stack's item code; the count is what the hero holds now. [footer] - что место кладёт под лицо (действие инспектора).
 */
@Composable fun StackInfoSheet(game: GameUi, code: String, footer: @Composable ColumnScope.() -> Unit = {}, onDismiss: () -> Unit) {
    StackPanel(onDismiss) {
        StackFace(game, code, game.hero?.bag?.get(code) ?: 0L)
        footer()
    }
}

/** The sheet a stack opens in: the gold spine along its edge and [content] beside it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StackPanel(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).navigationBarsPadding()) {
            RaritySpine(Gold, 4.dp)
            Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

/**
 * A stack's face: its icon, name and English trade name, how many the hero holds, and what it is for.
 *
 * Книга умения: её описание («Учит умению …») - чип, касание открывает карточку умения; без описания чип называет умение.
 */
@Composable private fun StackFace(game: GameUi, code: String, owned: Long) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        StackIcon(game, code, 56)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(itemTitle(code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            tradeName(code, equipment = false)?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
            MutedText(ui("bag.owned", owned), style = MaterialTheme.typography.labelSmall)
        }
    }
    val description = remember(code, game.lang, game.index) { itemDescription(code, game.index).takeIf { it.isNotBlank() } }
    val details = remember(code, game.lang, game.index) { itemDetails(code, game.index) }
    val book = game.index?.skills?.byBook(code)
    if (book != null) {
        val inspect = rememberSkillInspect()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { LoreChip(description ?: ui("bag.book_skill", SkillText.title(book.code)), Rune) { inspect(SkillPeek(book.code)) } }
            DetailsMark(details)
        }
    } else {
        description?.let { StackDescription(it, details) }
    }
    game.index?.skillGrowth?.rune(code)?.let { RuneLore(it, 1 + (game.hero?.stats?.get(CoreStat.RUNE_EFFECT.code) ?: 0.0) / 100) }
    game.index?.let { StackLore(it, code, game.hero?.crafts?.professions.orEmpty().mapValues { (_, p) -> p.level }) }
}

/** Короткое описание стопки [text]; подробное [details] (правила и оговорки) - под знаком ⓘ «Подробнее» справа ([DetailsMark]). */
@Composable private fun StackDescription(text: String, details: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text, color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        DetailsMark(details)
    }
}

/** Знак ⓘ «Подробнее» с подробным описанием стопки [details]; пустое - знака нет. */
@Composable private fun DetailsMark(details: String) {
    if (details.isNotBlank()) InfoMark(ui("bag.details"), { Tip(ui("bag.details"), details) })
}

/** Руна умения: семейство, действие с силой рун героя [power] и виды умений, куда она встаёт. */
@Composable private fun RuneLore(rune: RuneDefinition, power: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Engraved(ui("runes.family", RuneText.family(rune)))
        Text(RuneText.effect(rune, power), color = ModBlue, style = MaterialTheme.typography.bodyMedium)
        MutedText(ui("runes.types", RuneText.types(rune)), style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * What the content says of a stack under its description: an essence's guaranteed line by kind of item, where it is found and where it goes - работы профессий; закрытые по уровню героя [levels] - серым с нужным уровнем.
 * Под гарантией эссенции, что сгущается, - откуда взять сильнее: профессия и уровень ступени сгущения ([essenceCondense]).
 *
 * Обратный указатель всего контента строится раз на контент вне главного потока; до этого карточка не показывает ничего. Источников - первые
 * [MAX_SOURCES] с шансом и его единицей, остальные - строкой «ещё N»: касание раскрывает их на месте карточки.
 */
@Composable private fun StackLore(index: ContentIndex, code: String, levels: Map<String, Int>) {
    val guarantees = remember(index, code) { essenceGuarantees(index, code) }
    val condense = remember(index, code) { essenceCondense(index, code) }
    val sourceIndex by produceState<Map<String, List<ItemSource>>>(emptyMap(), index) {
        value = withContext(Dispatchers.Default) { itemSourceIndex(index) }
    }
    val sources = sourceIndex[code].orEmpty()
    var allSources by remember(code) { mutableStateOf(false) }
    val useIndex by produceState<Map<String, List<ItemUse>>>(emptyMap(), index) {
        value = withContext(Dispatchers.Default) { itemUseIndex(index) }
    }
    val uses = useIndex[code].orEmpty()
    if (guarantees.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Engraved(ui("bag.essence_guarantee"))
            guarantees.forEach { guarantee ->
                Column {
                    MutedText(guarantee.slots, style = MaterialTheme.typography.labelSmall)
                    Text(guarantee.line, color = ModBlue, style = MaterialTheme.typography.bodyMedium)
                }
            }
            condense?.let { MutedText(ui("bag.essence_condense", professionTitle(it.profession), it.level), style = MaterialTheme.typography.labelSmall) }
        }
    }
    if (sources.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Engraved(ui("bag.sources"))
            (if (allSources) sources else sources.take(MAX_SOURCES)).forEach { source ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(source.title(), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    source.chanceText()?.let { MutedText(it, style = MaterialTheme.typography.labelSmall) }
                }
            }
            if (!allSources && sources.size > MAX_SOURCES) {
                MutedText(
                    ui("bag.sources_more", sources.size - MAX_SOURCES),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clickable(role = Role.Button) { allSources = true }.padding(vertical = 4.dp),
                )
            }
        }
    }
    if (uses.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Engraved(ui("bag.uses"))
            uses.take(MAX_USES).forEach { use ->
                val open = (levels[use.profession] ?: 1) >= use.level
                Text(use.title(), color = if (open) Parchment else Muted, style = MaterialTheme.typography.bodySmall)
            }
            if (uses.size > MAX_USES) MutedText(ui("bag.uses_more", uses.size - MAX_USES), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** «Идёт в» показывает столько работ, остальные - числом. */
private const val MAX_USES = 6

/** Карточка сразу показывает столько источников - первые по виду; остальные - строкой «ещё N», касание раскрывает их на месте. */
private const val MAX_SOURCES = 5
