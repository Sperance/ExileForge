package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.PickOptions
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.guild.StashField
import com.sperance.exileforge.core.guild.StashFilter
import com.sperance.exileforge.core.guild.StashKind
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildStashEntry
import com.sperance.exileforge.core.model.guild.GuildStashView
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.GuildAction
import com.sperance.exileforge.rules.content.GuildPolicy
import com.sperance.exileforge.rules.content.GuildRole
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.sheet.StashTakePrice
import com.sperance.exileforge.ui.components.AccordionGroup
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.ChipMode
import com.sperance.exileforge.ui.components.ChoiceChips
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.FilterAccordionSheet
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeChip
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.components.ForgePicker
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.ForgeSheetDefaults
import com.sperance.exileforge.ui.components.GoldCost
import com.sperance.exileforge.ui.components.GoldPrice
import com.sperance.exileforge.ui.components.Hold
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.InspectAction
import com.sperance.exileforge.ui.components.LengthCounter
import com.sperance.exileforge.ui.components.LevelRangeHeader
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.PropertyRow
import com.sperance.exileforge.ui.components.RarityTiles
import com.sperance.exileforge.ui.components.SheetBody
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.components.shortText
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.screens.auction.ActiveFilter
import com.sperance.exileforge.ui.screens.auction.ItemTradeRow
import com.sperance.exileforge.ui.screens.auction.TradeRow
import com.sperance.exileforge.ui.screens.auction.templateSlots
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    ForgePicker(ui("guild.stash_rank_set"), minRank.toString(), options, enabled = !game.busy, hint = ui("guild.stash_rank_hint")) {
                        vm.tabRank(tab, it.toInt())
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
        DepositSheet(game, onDismiss = { depositing = false }) { itemId, code, amount ->
            depositing = false
            vm.deposit(tab, itemId, code, amount)
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

/** Поиск по имени и кнопка отбора, под ними - заданные поля чипами с крестиком, как у витрины аукциона. */
@Composable private fun StashSearch(game: GameUi, filter: StashFilter, onFilter: (StashFilter) -> Unit) {
    var sheet by remember { mutableStateOf(false) }
    val active = filter.active()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            filter.title,
            { onFilter(filter.copy(title = it.take(game.inputs.search))) },
            placeholder = { Text(ui("auction.name")) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        ForgeButton(onClick = { sheet = true }, contentPadding = PaddingValues(horizontal = 12.dp), kind = ButtonKind.Secondary) {
            Icon(Icons.Outlined.FilterList, ui("auction.filters"), modifier = Modifier.size(18.dp))
            if (active.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(active.size.toString())
            }
        }
    }
    if (active.isNotEmpty()) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            active.forEach { field -> ActiveFilter(fieldLabel(game, filter, field)) { onFilter(filter.without(field)) } }
        }
    }
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

private fun kindTitle(kind: StashKind): String = ui("guild.stash_kind.${kind.name}")

private fun fieldLabel(game: GameUi, filter: StashFilter, field: StashField): String = when (field) {
    StashField.KIND -> filter.kind?.let(::kindTitle).orEmpty()
    StashField.SLOT -> slotTitle(filter.slot, game.lang)
    StashField.RARITY -> rarityTitle(filter.rarity, game.lang)
    StashField.LEVEL -> filter.level.title().orEmpty()
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
        AccordionGroup("kind", ui("auction.what_sold"), draft.kind?.let(::kindTitle) ?: any) {
            ChoiceChips(listOf(null) + StashKind.entries, { it == draft.kind }, { it?.let(::kindTitle) ?: any }) { draft = draft.copy(kind = it) }
        },
        AccordionGroup("slot", ui("filter.type"), draft.slot.takeIf { it.isNotBlank() }?.let { slotTitle(it, game.lang) } ?: any) {
            ChoiceChips(listOf("") + templateSlots.map { it.name }, { it == draft.slot }, { if (it.isBlank()) any else slotTitle(it, game.lang) }) { draft = draft.copy(slot = it) }
        },
        AccordionGroup("rarity", ui("filter.rarity"), draft.rarity.takeIf { it.isNotBlank() }?.let { rarityTitle(it, game.lang) } ?: any) {
            RarityTiles(Rarity.entries, { it.name == draft.rarity }) { draft = draft.copy(rarity = if (draft.rarity == it.name) "" else it.name) }
        },
    )
    FilterAccordionSheet(
        query = draft.title,
        onQuery = { draft = draft.copy(title = it.take(game.inputs.search)) },
        queryHint = ui("auction.name"),
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
    val action = InspectAction(ui("guild.stash_take"), enabled = !game.busy && reason == null, reason = reason, hold = true, figure = number(price.toDouble()).takeIf { price > 0 }) { onTake() }
    val open = { inspect(entry.item?.let { Inspect.Copy(it, action) } ?: Inspect.Stack(entry.code, action)) }
    val tag: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = { bill?.takeIf { price > 0 }?.let { GoldCost(it) } ?: GoldPrice(price) }
    val by = listOf(ui("guild.stash_by", entry.by))
    if (view != null) {
        ItemTradeRow(view, enabled = true, unmet = game.unmetFor(view.code), extra = by, onClick = open, price = tag)
    } else {
        TradeRow(
            "${itemTitle(entry.code)} × ${entry.amount}",
            Parchment,
            by,
            enabled = true,
            onClick = open,
            icon = { BagIcon(entry.code, Modifier.size(28.dp)) },
            price = tag,
        )
    }
}

/**
 * What the hero may put in: a loose item of the stash, or part of a stack of the bag; кладётся удержанием. Стопки, привязанные к герою
 * (`ContentIndex.bound`), в список не попадают.
 *
 * Касание - карточка, положить в хранилище - её кнопка удержанием. Выбранная стопка - сверху над списком с полем «сколько», её
 * «Положить» удержанием - в подвале листа.
 */
@Composable
private fun DepositSheet(game: GameUi, onDismiss: () -> Unit, onPut: (itemId: String?, code: String?, amount: Long) -> Unit) {
    val hero = game.hero ?: return
    val loose = hero.items.filter { !it.equipped && !it.socketed && !it.locked }
    val stacks = hero.bag.filter { (code, amount) -> amount > 0 && game.index?.bound(code) != true }.keys.sortedBy { itemTitle(it) }
    var stack by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf("1") }
    val inspect = rememberInspect()
    val put = ui("guild.stash_put")
    val count = amount.toLongOrNull() ?: 0L
    val chosen = stack
    val putStack: (@Composable RowScope.() -> Unit)? = if (chosen == null) {
        null
    } else {
        {
            ForgeButton(onClick = { onPut(null, chosen, count) }, modifier = Modifier.weight(1f), state = ButtonState.of(!game.busy && count in 1..(hero.bag[chosen] ?: 0L)), hold = Hold()) {
                Text(put)
            }
        }
    }
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = ForgeSheetDefaults.Bare,
        header = {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Engraved(put)
                chosen?.let { code ->
                    ForgePanel {
                        Text(itemTitle(code), color = Parchment, style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            amount,
                            { amount = it.filter(Char::isDigit).take(12) },
                            label = { Text(ui("sell.amount_owned", hero.bag[code] ?: 0L)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        footer = putStack,
    ) {
        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(loose, key = { it.id }) { item ->
                val view = game.view(item) ?: return@items
                Row(
                    Modifier.fillMaxWidth().clickable { inspect(Inspect.Copy(item, InspectAction(put, enabled = !game.busy, hold = true) { onPut(item.id, null, 1) })) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ItemIcon(view, rarityColor(view.rarity.name), Modifier.size(28.dp))
                    Text(view.title, color = rarityColor(view.rarity.name), style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(stacks, key = { "bag:$it" }) { code ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        inspect(
                            Inspect.Stack(
                                code,
                                InspectAction(put, enabled = !game.busy) {
                                    stack = code
                                    amount = "1"
                                },
                            ),
                        )
                    }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BagIcon(code, Modifier.size(28.dp))
                    Text("${itemTitle(code)} × ${hero.bag[code]}", color = Parchment, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
