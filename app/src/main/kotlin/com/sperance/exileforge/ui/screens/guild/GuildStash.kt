package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.PickOptions
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.guild.DepositBag
import com.sperance.exileforge.core.guild.DepositKind
import com.sperance.exileforge.core.guild.DepositLot
import com.sperance.exileforge.core.guild.DepositPreset
import com.sperance.exileforge.core.guild.StashFilter
import com.sperance.exileforge.core.guild.StashKind
import com.sperance.exileforge.core.guild.depositBag
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildDepositParcel
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildStashEntry
import com.sperance.exileforge.core.model.guild.GuildStashView
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.DepositBar
import com.sperance.exileforge.rules.content.GuildAction
import com.sperance.exileforge.rules.content.GuildPolicy
import com.sperance.exileforge.rules.content.GuildRole
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.sheet.StashTakePrice
import com.sperance.exileforge.ui.components.AccordionGroup
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonSize
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.ChipMode
import com.sperance.exileforge.ui.components.ChipSize
import com.sperance.exileforge.ui.components.ChoiceChips
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.FilterAccordionSheet
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeChip
import com.sperance.exileforge.ui.components.ForgeIconButton
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.components.ForgePicker
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.ForgeSheetDefaults
import com.sperance.exileforge.ui.components.GoldCost
import com.sperance.exileforge.ui.components.GoldPrice
import com.sperance.exileforge.ui.components.InfoCard
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.InspectAction
import com.sperance.exileforge.ui.components.ItemFilterSheet
import com.sperance.exileforge.ui.components.ItemSearchBar
import com.sperance.exileforge.ui.components.ItemTile
import com.sperance.exileforge.ui.components.LengthCounter
import com.sperance.exileforge.ui.components.LevelRangeHeader
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.PickDock
import com.sperance.exileforge.ui.components.PresetRow
import com.sperance.exileforge.ui.components.PropertyRow
import com.sperance.exileforge.ui.components.RarityChoice
import com.sperance.exileforge.ui.components.SearchBar
import com.sperance.exileforge.ui.components.SearchFilter
import com.sperance.exileforge.ui.components.SheetBody
import com.sperance.exileforge.ui.components.StackCell
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.components.rememberChips
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.components.rememberItemFilter
import com.sperance.exileforge.ui.components.rememberItemShelf
import com.sperance.exileforge.ui.components.rememberLazyRowScrollCue
import com.sperance.exileforge.ui.components.rememberPick
import com.sperance.exileforge.ui.components.scrollCue
import com.sperance.exileforge.ui.components.shortText
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.ItemTradeRow
import com.sperance.exileforge.ui.screens.auction.TradeRow
import com.sperance.exileforge.ui.screens.auction.templateSlots
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.rarityColor

/**
 * The guild stash: tabs of the rules' places; anyone puts in, taking needs the tab's rank and,
 * for a member, one of the day's takes — a stack is one take. What lies here is the guild's. Список - плиты
 * «Ценника», как витрина аукциона, с её отбором (на клиенте, [StashFilter]); ярлык - цена взятия в золоте
 * ([StashTakePrice]), взять и положить - удержанием.
 *
 * Участник и офицер видят «N/M» на сегодня, глава - без ограничений; под ними - пределы каждой роли одной строкой.
 * Чипы вкладок - их имена ([GuildText.stashTab]); у кого право настраивать вкладки - порог ранга общим выбором ([ForgePicker]:
 * ранги с порогом вклада и числом членов гильдии в ранге) вместо строки порога и имя открытой вкладки.
 */
@Composable internal fun StashTab(game: GameUi, vm: GuildViewModel, me: GuildMember?) {
    val guilds by vm.guilds.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadStash() }
    val stash = guilds.stash ?: return
    val ranks = game.index?.guilds?.ranks.orEmpty()
    var tab by remember { mutableIntStateOf(0) }
    var depositing by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(StashFilter()) }
    val inTab = stash.entries.filter { it.tab == tab }
    val shown = inTab.filter { filter.matches(it.item?.let(game::view), it.code) }
    val minRank = stash.tabs.getOrNull(tab)?.minRank ?: 0
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            ForgePanel {
                if (stash.tabs.size > 1 || stash.tabs.any { it.name.isNotBlank() }) {
                    val cue = rememberLazyRowScrollCue(rememberLazyListState(), tab)
                    LazyRow(Modifier.scrollCue(cue, Panel), state = cue.state, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(stash.tabs.indices.toList(), key = { it }) { i ->
                            ForgeChip(GuildText.stashTab(stash.tabs[i], i), i == tab, { tab = i }, mode = ChipMode.Choice)
                        }
                    }
                }
                PropertyRow(ui("guild.stash_places"), "${inTab.size} / ${stash.tabSize}", Glyph.ITEM)
                val configures = me != null && GuildPolicy.can(me.role, GuildAction.STASH_TABS)
                if (!configures || ranks.isEmpty()) PropertyRow(ui("guild.stash_rank"), ranks.getOrNull(minRank)?.code?.let(GuildText::rank).orEmpty(), Glyph.CHARACTER)
                Text(
                    if (stash.takesPerDay < 0) ui("guild.stash_takes_free") else ui("guild.stash_takes", stash.takesLeft.coerceAtLeast(0), stash.takesPerDay),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                roleTakesLine(stash)?.let { MutedText(it, style = MaterialTheme.typography.labelSmall) }
                if (configures && ranks.isNotEmpty()) {
                    val members = guilds.mine?.guild?.members
                    val options = remember(ranks, members, game.lang) { PickOptions.ranks(ranks, members) }
                    ForgePicker(ui("guild.stash_rank_set"), minRank, options, enabled = !game.busy, hint = ui("guild.stash_rank_hint")) { rank ->
                        rank?.let { vm.tabRank(tab, it) }
                    }
                }
                if (configures) {
                    ForgeButton(state = ButtonState.of(!game.busy), onClick = { renaming = true }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) {
                        Text(ui("guild.stash_rename"))
                    }
                }
                ForgeButton(state = ButtonState.of(!game.busy && inTab.size < stash.tabSize), onClick = { depositing = true }, modifier = Modifier.fillMaxWidth()) { Text(ui("guild.stash_put")) }
            }
        }
        if (inTab.isNotEmpty()) item { StashSearch(game, filter) { filter = it } }
        when {
            inTab.isEmpty() -> item { MutedText(ui("guild.stash_empty")) }
            shown.isEmpty() -> item { MutedText(ui("guild.stash_none")) }
        }
        items(shown, key = { it.id }) { entry -> EntryRow(game, stash, entry) { vm.take(entry.id) } }
    }
    if (depositing) {
        DepositSheet(game, stash, tab, onDismiss = { depositing = false }) { parcels ->
            depositing = false
            vm.deposit(tab, parcels)
        }
    }
    if (renaming) {
        val limit = game.index?.guilds?.stash?.tabName ?: return
        TabNameSheet(stash.tabs.getOrNull(tab)?.name.orEmpty(), GuildText.stashTab(null, tab), limit, game.busy, onDismiss = { renaming = false }) { name ->
            renaming = false
            vm.tabName(tab, name)
        }
    }
}

/** Имя вкладки листом с полем: до [limit] знаков, пустое - вкладка снова по номеру ([numbered] - подсказка в поле); «Сохранить» - в подвале. */
@Composable private fun TabNameSheet(current: String, numbered: String, limit: Int, busy: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(spacing = 8.dp),
        footer = {
            ForgeButton(state = ButtonState.of(!busy && name.trim() != current), onClick = { onSave(name) }, modifier = Modifier.weight(1f)) { Text(ui("guild.save")) }
        },
    ) {
        Text(ui("guild.stash_rename"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            name,
            { name = it.take(limit) },
            placeholder = { Text(numbered) },
            supportingText = { LengthCounter(name, limit) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Строка поиска хранилища ([SearchBar]): имя, кнопка отбора и заданные поля чипами под ней ([StashFilter.narrowings]). */
@Composable private fun StashSearch(game: GameUi, filter: StashFilter, onFilter: (StashFilter) -> Unit) {
    var sheet by remember { mutableStateOf(false) }
    SearchBar(
        filter.title,
        { onFilter(filter.copy(title = it)) },
        ui("guild.stash_search"),
        filter = SearchFilter(rememberChips(filter.narrowings(game.lang), onFilter), onOpen = { sheet = true }, onReset = { onFilter(filter.cleared()) }),
        maxLength = game.inputs.search,
    )
    if (sheet) {
        StashFilterSheet(game, filter, onDismiss = { sheet = false }) {
            sheet = false
            onFilter(it)
        }
    }
}

/** Пределы хранилища по ролям одной строкой: «Взятий в сутки: Глава - без счёта · Офицер - 20 · Участник - 10»; сервер не прислал - нет. */
private fun roleTakesLine(stash: GuildStashView): String? = stash.roleTakes.takeIf { it.isNotEmpty() }?.let { takes ->
    ui("guild.stash_roles", GuildRole.entries.mapNotNull { role -> takes[role]?.let { ui("guild.stash_role", GuildText.role(role), if (it < 0) ui("guild.stash_role_free") else it.toString()) } }.joinToString(" · "))
}

/**
 * Отбор хранилища в общем листе-«Аккордеоне», поля витрины аукциона без цены и продавца; вилка уровня - в шапке листа, как у
 * фильтра предметов ([LevelRangeHeader]); ничего не применяется до «Показать».
 *
 * «Сбросить» применяет сброс сразу и закрывает шторку.
 */
@Composable private fun StashFilterSheet(game: GameUi, filter: StashFilter, onDismiss: () -> Unit, onApply: (StashFilter) -> Unit) {
    var draft by remember { mutableStateOf(filter) }
    val any = ui("filter.any")
    val groups = listOf(
        AccordionGroup("kind", ui("auction.what_sold"), draft.kind?.title ?: any) {
            ChoiceChips(listOf(null) + StashKind.entries, { it == draft.kind }, { it?.title ?: any }) { draft = draft.copy(kind = it) }
        },
        AccordionGroup("slot", ui("filter.type"), draft.slot.takeIf { it.isNotBlank() }?.let { slotTitle(it, game.lang) } ?: any) {
            ChoiceChips(listOf("") + templateSlots.map { it.name }, { it == draft.slot }, { if (it.isBlank()) any else slotTitle(it, game.lang) }) { draft = draft.copy(slot = it) }
        },
        AccordionGroup("rarity", ui("filter.rarity"), draft.rarity.takeIf { it.isNotBlank() }?.let { rarityTitle(it, game.lang) } ?: any) {
            RarityChoice(Rarity.entries, { it?.name == draft.rarity }, { picked -> draft = draft.copy(rarity = picked?.name?.takeIf { it != draft.rarity }.orEmpty()) }, mode = ChipMode.Filter, size = ChipSize.Dense)
        },
    )
    FilterAccordionSheet(
        groups = groups,
        switches = emptyList(),
        show = ui("filter.show_all"),
        resettable = draft != draft.cleared(),
        onReset = { onApply(draft.cleared()) },
        onShow = { onApply(draft) },
        onDismiss = onDismiss,
        header = { LevelRangeHeader(draft.level, game.heroLevel) { draft = draft.copy(level = it) } },
    )
}

/**
 * Плита записи хранилища «Ценником»: копия - со всеми строками, как лот, стопка - иконка, имя и число; ярлык - цена
 * взятия в золоте. Касание - карточка, «Взять» в ней по удержанию; погашено, если взятий на сегодня нет или золота мало.
 *
 * Цена взятия против кошелька героя - тот же счёт, что у всех цен в золоте.
 */
@Composable private fun EntryRow(game: GameUi, stash: GuildStashView, entry: GuildStashEntry, onTake: () -> Unit) {
    val index = game.index ?: return
    val view = entry.item?.let { game.view(it) }
    val inspect = rememberInspect()
    val price = StashTakePrice.of(index, entry.item, entry.code, entry.amount)
    val bill = game.goldBill(price)
    val reason = when {
        stash.takesLeft == 0 -> ui("guild.stash_no_takes")
        bill?.affordable == false -> bill.shortText()
        else -> null
    }
    val action = InspectAction(ui("guild.stash_take"), enabled = !game.busy && reason == null, reason = reason, hold = true, figure = compactNumber(price).takeIf { price > 0 }) { onTake() }
    val open = { inspect(entry.item?.let { Inspect.Copy(it, action) } ?: Inspect.Stack(entry.code, action)) }
    val tag: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = { bill?.takeIf { price > 0 }?.let { GoldCost(it) } ?: GoldPrice(price) }
    val by = listOf(ui("guild.stash_by", entry.by))
    if (view != null) {
        ItemTradeRow(view, enabled = true, unmet = game.unmetFor(view.item), extra = by, onClick = open, price = tag)
    } else {
        TradeRow(
            itemTitle(entry.code),
            Parchment,
            by,
            enabled = true,
            onClick = open,
            icon = { StackCell(index, entry.code, entry.amount, size = 44.dp) },
            price = tag,
        )
    }
}

/**
 * Окно «Положить» во вкладку [tab] хранилища [stash] - всегда выбор пачкой: в шапке «Свободно N / M», строка поиска сумки
 * ([ItemSearchBar], список [ItemShelf.GUILD_DEPOSIT]), вкладки-чипы «Снаряжение / Стопки / Карты» с числом отмеченного и быстрые
 * наборы ([PresetRow]: всё видимое и по редкости). Строка - флажок слева и плитка тайника ([ItemTile], уровень на гнезде), стопка -
 * с числом; касание отмечает, удержание вещи - карточка, удержание стопки - поле «сколько» в шапке (по умолчанию вся стопка). У
 * запрещённого ([DepositLot.bar]: заперто, привязано к герою) вместо флажка замок, причина - в строке.
 *
 * Внизу закреплены «Отмена» и удерживаемая «Положить N · влезет K» ([PickDock]): K - по правилу сервера ([DepositBag.fits]), что не
 * влезет, останется у героя. Герою-тестировщику склад закрыт - причина в шапке, кнопка погашена.
 */
@Composable
private fun DepositSheet(game: GameUi, stash: GuildStashView, tab: Int, onDismiss: () -> Unit, onPut: (List<GuildDepositParcel>) -> Unit) {
    val hero = game.hero ?: return
    val bag = remember(hero.items, hero.bag, stash, tab, game.index, game.world) { game.depositBag(stash, tab) } ?: return
    val filterState = rememberItemFilter(ItemShelf.GUILD_DEPOSIT)
    var kind by remember { mutableStateOf(DepositKind.GEAR) }
    var filtering by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }
    val typed = remember { mutableStateMapOf<String, String>() }
    val pieces = remember(bag, kind) { bag.pieces(kind) }
    val shelf = rememberItemShelf(game, pieces, filterState) { null }
    val query = filterState.query
    val shown = remember(bag, kind, shelf, query) { bag.shown(kind, shelf, query) }
    val pick = rememberPick(bag.pickable, remember(shown) { shown.mapTo(HashSet()) { it.id } })
    val picked = pick.picked
    val inspect = rememberInspect()
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = ForgeSheetDefaults.Bare,
        header = {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Engraved(ui("guild.stash_put"), modifier = Modifier.weight(1f))
                    Text(ui("guild.deposit_free", bag.free, bag.size), color = if (bag.free == 0) LifeRed else Parchment, style = MaterialTheme.typography.labelMedium)
                }
                ItemSearchBar(filterState, ui("guild.deposit_search"), game.lang, onFilter = { filtering = true })
                val cue = rememberLazyRowScrollCue(rememberLazyListState(), DepositKind.entries.indexOf(kind))
                LazyRow(Modifier.scrollCue(cue, Panel), state = cue.state, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DepositKind.entries, key = { it.name }) { each -> ForgeChip(bag.tab(each, pick::chosen), each == kind, { kind = each }, mode = ChipMode.Choice) }
                }
                PresetRow(pick, DepositPreset.ALL) { preset ->
                    when (preset) {
                        DepositPreset.Visible -> ui("guild.deposit_preset_visible") to Parchment
                        is DepositPreset.OfRarity -> ui("stash.rarity_many.${preset.rarity.name}") to rarityColor(preset.rarity.name)
                    }
                }
                editing?.let { code -> bag.of(DepositKind.STACKS).firstOrNull { it.code == code } }?.let { lot ->
                    StackAmount(lot, typed[lot.code].orEmpty(), onType = { typed[lot.code] = it.filter(Char::isDigit).take(AMOUNT_DIGITS) }) { editing = null }
                }
            }
        },
        footer = {
            val fits = bag.fits(picked)
            PickDock(
                label = if (picked.isEmpty()) ui("sell.pick_hint") else ui("guild.deposit_do", picked.size),
                icon = ForgeGlyphs.Stash,
                enabled = !game.busy && fits > 0,
                figure = ui("guild.deposit_fits", fits).takeIf { picked.isNotEmpty() },
                onCancel = onDismiss,
                modifier = Modifier.weight(1f),
            ) { onPut(bag.parcels(picked, typed)) }
        },
    ) {
        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                bag.lots.isEmpty() -> item { InfoCard(ui("guild.deposit_empty"), ui("guild.deposit_empty_hint")) }
                shown.isEmpty() -> item { MutedText(ui("guild.stash_none")) }
                kind == DepositKind.STACKS -> item { MutedText(ui("guild.deposit_stack_hint")) }
            }
            items(shown, key = { it.id }) { lot ->
                DepositLine(lot, pick.chosen(lot.id), onToggle = { pick.toggle(lot.id) }) {
                    val piece = lot.piece
                    if (piece != null) {
                        ItemTile(piece, Modifier.weight(1f), selected = pick.chosen(lot.id), onClick = { if (lot.bar == null) pick.toggle(lot.id) else inspect(Inspect.Copy(piece.item)) }, onLongClick = { inspect(Inspect.Copy(piece.item)) }) {
                            lot.bar?.let { BarReason(it) }
                        }
                    } else {
                        StackLine(game.index, lot, typed[lot.code], Modifier.weight(1f), onClick = { if (lot.bar == null) pick.toggle(lot.id) else inspect(Inspect.Stack(lot.code)) }) {
                            if (lot.bar == null) {
                                editing = lot.code
                                if (!pick.chosen(lot.id)) pick.toggle(lot.id)
                            } else {
                                inspect(Inspect.Stack(lot.code))
                            }
                        }
                    }
                }
            }
        }
    }
    if (filtering && shelf != null) ItemFilterSheet(filterState, shelf) { filtering = false }
}

/** Сколько цифр принимает поле количества стопки. */
private const val AMOUNT_DIGITS = 12

/** Строка окна вклада: слева флажок отметки ([chosen], касание - [onToggle]) или замок у запрещённого, справа - сама посылка [content]. */
@Composable private fun DepositLine(lot: DepositLot, chosen: Boolean, onToggle: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val bar = lot.bar
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (bar == null) {
            Checkbox(chosen, onCheckedChange = { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = Gold, uncheckedColor = Muted, checkmarkColor = Ink))
        } else {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Lock, ui("guild.deposit_why.${bar.name}"), tint = Muted, modifier = Modifier.size(18.dp)) }
        }
        content()
    }
}

/** Причина запрета посылки строкой под ней. */
@Composable private fun BarReason(bar: DepositBar) {
    Text(ui("guild.deposit_why.${bar.name}"), color = LifeRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
}

/** Стопка сумки строкой: значок, имя, число («× 37», часть - «× 10 из 37» по набранному [typed]) и причина запрета; удержание - [onHold]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StackLine(index: ContentIndex?, lot: DepositLot, typed: String?, modifier: Modifier, onClick: () -> Unit, onHold: () -> Unit) {
    Row(
        modifier.heightIn(min = 48.dp).combinedClickable(onLongClick = onHold, onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StackCell(index, lot.code, 1, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(itemTitle(lot.code), color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            lot.bar?.let { BarReason(it) }
        }
        Text(lot.countText(typed), color = GoldBright, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/** Поле «сколько» отмеченной стопки [lot] в шапке окна: пусто - вся стопка; крестик закрывает поле, набранное остаётся. */
@Composable private fun StackAmount(lot: DepositLot, typed: String, onType: (String) -> Unit, onClose: () -> Unit) {
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(itemTitle(lot.code), color = Parchment, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            ForgeIconButton(Icons.Outlined.Close, ui("common.close"), onClick = onClose, size = ButtonSize.Compact)
        }
        OutlinedTextField(
            typed,
            onType,
            label = { Text(ui("sell.amount_owned", lot.owned)) },
            placeholder = { Text(lot.owned.toString()) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
