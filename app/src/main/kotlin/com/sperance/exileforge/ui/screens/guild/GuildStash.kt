package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.PickOption
import com.sperance.exileforge.core.display.PickOptions
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.groupedNumber
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.guild.DepositBag
import com.sperance.exileforge.core.guild.DepositKind
import com.sperance.exileforge.core.guild.DepositLot
import com.sperance.exileforge.core.guild.DepositPreset
import com.sperance.exileforge.core.guild.StashFilter
import com.sperance.exileforge.core.guild.StashKind
import com.sperance.exileforge.core.guild.StashTabEdit
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
import com.sperance.exileforge.rules.content.GuildCoinRule
import com.sperance.exileforge.rules.content.GuildPolicy
import com.sperance.exileforge.rules.content.GuildRole
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.sheet.StashTakeCost
import com.sperance.exileforge.rules.sheet.StashValue
import com.sperance.exileforge.ui.components.AccordionGroup
import com.sperance.exileforge.ui.components.AmountStepper
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.ChipMode
import com.sperance.exileforge.ui.components.ChipSize
import com.sperance.exileforge.ui.components.ChoiceChips
import com.sperance.exileforge.ui.components.CoinsRole
import com.sperance.exileforge.ui.components.CornerMark
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.FellowshipCoins
import com.sperance.exileforge.ui.components.FilterAccordionSheet
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeChip
import com.sperance.exileforge.ui.components.ForgeIconButton
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.ForgeSheetDefaults
import com.sperance.exileforge.ui.components.GoldCost
import com.sperance.exileforge.ui.components.GoldPrice
import com.sperance.exileforge.ui.components.IconButtonShape
import com.sperance.exileforge.ui.components.InfoCard
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.InspectAction
import com.sperance.exileforge.ui.components.ItemFilterSheet
import com.sperance.exileforge.ui.components.ItemSearchBar
import com.sperance.exileforge.ui.components.ItemTile
import com.sperance.exileforge.ui.components.LengthCounter
import com.sperance.exileforge.ui.components.LevelRangeHeader
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.Pick
import com.sperance.exileforge.ui.components.PickDock
import com.sperance.exileforge.ui.components.PickLine
import com.sperance.exileforge.ui.components.PickMark
import com.sperance.exileforge.ui.components.PillTabs
import com.sperance.exileforge.ui.components.PresetRow
import com.sperance.exileforge.ui.components.RarityChoice
import com.sperance.exileforge.ui.components.SearchBar
import com.sperance.exileforge.ui.components.SearchFilter
import com.sperance.exileforge.ui.components.SheetBody
import com.sperance.exileforge.ui.components.StackCell
import com.sperance.exileforge.ui.components.TabBadge
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
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.depthInset
import com.sperance.exileforge.ui.theme.rarityColor

/**
 * The guild stash: tabs of the rules' places; anyone puts in, taking needs the tab's rank and,
 * for a member, one of the day's takes — a stack is one take. What lies here is the guild's.
 *
 * Сверху - вкладки таблетками «Имя 14/50» ([StashTabs]: всегда, и одна - «Вкладка 1»; не влезают - листаются), у кого право настраивать
 * вкладки - ⚙ у правого края вне прокрутки: лист настроек вкладки ([TabSettingsSheet]: имя и порог ранга). Под ними - сводка
 * вдавленной капсулой «⛓ 1 240 · Взято 3/10 · Ветеран» ([StashSummary]; у главы взятий нет - без счёта), раскрытие - пределы ролей и
 * правило монет Содружества. Список - плиты «Ценника», как витрина аукциона, с её отбором (на клиенте, [StashFilter]); ярлык - цена
 * взятия в монетах и золоте ([StashValue.takeCost]), взять - удержанием в карточке. «Положить» закреплена внизу.
 */
@Composable internal fun StashTab(game: GameUi, vm: GuildViewModel, me: GuildMember?) {
    val guilds by vm.guilds.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadStash() }
    val stash = guilds.stash ?: return
    val ranks = game.index?.guilds?.ranks.orEmpty()
    var tab by remember { mutableIntStateOf(0) }
    var depositing by remember { mutableStateOf(false) }
    var configuring by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(StashFilter()) }
    val inTab = remember(stash, tab) { stash.entries.filter { it.tab == tab } }
    val shown = inTab.filter { filter.matches(it.item?.let(game::view), it.code) }
    val minRank = stash.tabs.getOrNull(tab)?.minRank ?: 0
    val configures = me != null && GuildPolicy.can(me.role, GuildAction.STASH_TABS)
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
            item(key = "tabs") { StashTabs(stash, tab, configures && !game.busy, onTab = { tab = it }) { configuring = true } }
            item(key = "summary") { StashSummary(stash, ranks.getOrNull(minRank)?.code?.let(GuildText::rank), me?.role, game.index?.guilds?.stash?.coins) }
            if (inTab.isNotEmpty()) item(key = "search") { StashSearch(game, filter) { filter = it } }
            when {
                inTab.isEmpty() -> item(key = "empty") { MutedText(ui("guild.stash_empty")) }
                shown.isEmpty() -> item(key = "none") { MutedText(ui("guild.stash_none")) }
            }
            items(shown, key = { it.id }) { entry -> EntryRow(game, stash, entry) { vm.take(entry.id) } }
        }
        ForgeButton(state = ButtonState.of(!game.busy && inTab.size < stash.tabSize), onClick = { depositing = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), icon = ForgeGlyphs.Stash) {
            Text(ui("guild.stash_put"))
        }
    }
    if (depositing) {
        DepositSheet(game, stash, tab, onDismiss = { depositing = false }) { parcels ->
            depositing = false
            vm.deposit(tab, parcels)
        }
    }
    if (configuring) {
        val limit = game.index?.guilds?.stash?.tabName ?: return
        val members = guilds.mine?.guild?.members
        val options = remember(ranks, members, game.lang) { PickOptions.ranks(ranks, members) }
        TabSettingsSheet(stash.tabs.getOrNull(tab)?.name.orEmpty(), GuildText.stashTab(null, tab), limit, minRank, options, game.busy, onDismiss = { configuring = false }) { edit ->
            configuring = false
            vm.editTab(tab, edit)
        }
    }
}

/**
 * Вкладки хранилища таблетками «Имя 14/50» ([PillTabs], число - занято из мест вкладки); всегда, и одна - «Вкладка 1». [configurable] -
 * у правого края вне прокрутки круглая ⚙ - лист настроек открытой вкладки ([onConfigure]).
 */
@Composable private fun StashTabs(stash: GuildStashView, tab: Int, configurable: Boolean, onTab: (Int) -> Unit, onConfigure: () -> Unit) {
    val count = stash.tabs.size.coerceAtLeast(1)
    val labels = (0 until count).map { GuildText.stashTab(stash.tabs.getOrNull(it), it) }
    val badges = remember(stash) { (0 until count).map { i -> TabBadge("${stash.entries.count { it.tab == i }}/${stash.tabSize}") } }
    PillTabs(labels, tab, onTab, badges = badges, scroll = true, trailing = {
        if (configurable) ForgeIconButton(Icons.Outlined.Settings, ui("guild.stash_configure"), onClick = onConfigure, kind = ButtonKind.Secondary, shape = IconButtonShape.Circle)
    })
}

/**
 * Сводка хранилища вдавленной капсулой: монеты Содружества героя, «Взято 3/10» за сутки (у роли без счёта - главы - нет) и ранг доступа
 * открытой вкладки [rank] без подписи; касание раскрывает под ней пределы взятий каждой роли (строка своей роли [role] выделена) и
 * правило монет числами правила [rule]. Для чтеца экрана капсула - кнопка в состоянии «свёрнута / раскрыта».
 */
@Composable private fun StashSummary(stash: GuildStashView, rank: String?, role: GuildRole?, rule: GuildCoinRule?) {
    var open by rememberSaveable { mutableStateOf(false) }
    val pill = RoundedCornerShape(50)
    val state = ui(if (open) "guild.stash_summary_open" else "guild.stash_summary_closed")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().depthInset(pill).clip(pill).clickable(role = Role.Button, onClickLabel = ui("guild.stash_summary_toggle")) { open = !open }
                .semantics { stateDescription = state }.heightIn(min = 40.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FellowshipCoins(stash.coins, CoinsRole.PRICE)
            if (stash.takesPerDay >= 0) SummaryPart(ui("guild.stash_taken", (stash.takesPerDay - stash.takesLeft).coerceAtLeast(0), stash.takesPerDay))
            rank?.let { SummaryPart(it) }
            Spacer(Modifier.weight(1f))
            Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Muted, modifier = Modifier.size(20.dp))
        }
        if (open) {
            ForgePanel {
                if (stash.roleTakes.isNotEmpty()) {
                    Engraved(ui("guild.stash_roles_title"))
                    GuildRole.entries.forEach { r -> stash.roleTakes[r]?.let { RoleTakes(r, it, r == role) } }
                }
                rule?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(ForgeGlyphs.Fellowship, null, tint = Rune, modifier = Modifier.size(16.dp))
                        Text(ui("guild.coins_title"), color = Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                    MutedText(coinsHelp(it), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Часть сводки хранилища после разделителя: «Взято 3/10», ранг. */
@Composable private fun RowScope.SummaryPart(text: String) {
    Text("·", color = Muted, style = MaterialTheme.typography.bodySmall)
    Text(text, color = Parchment, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
}

/** Предел взятий роли [role] в сутки [takes] (меньше нуля - без счёта); [mine] - своя роль, строка в рамке. */
@Composable private fun RoleTakes(role: GuildRole, takes: Int, mine: Boolean) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().background(Panel, shape).then(if (mine) Modifier.border(1.dp, Rune.copy(alpha = .6f), shape) else Modifier).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(GuildText.role(role), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(if (takes < 0) ui("guild.stash_role_free") else takes.toString(), color = Parchment, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Лист настроек вкладки: имя (до [limit] знаков, пустое - вкладка снова по номеру, [numbered] - подсказка в поле) и порог ранга, с
 * которого из неё берут, - чипами рангов [ranks], под ними - порог вклада и число членов гильдии выбранного. «Сохранить» горит, когда
 * что-то разошлось с [current] и [currentRank], и отдаёт только разошедшееся ([StashTabEdit]).
 */
@Composable private fun TabSettingsSheet(
    current: String,
    numbered: String,
    limit: Int,
    currentRank: Int,
    ranks: List<PickOption<Int>>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (StashTabEdit) -> Unit,
) {
    var name by remember { mutableStateOf(current) }
    var rank by remember { mutableIntStateOf(currentRank) }
    val edit = StashTabEdit.of(current, currentRank, name, rank)
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(spacing = 8.dp),
        footer = {
            ForgeButton(onClick = onDismiss, modifier = Modifier.weight(1f), kind = ButtonKind.Secondary) { Text(ui("common.cancel")) }
            ForgeButton(state = ButtonState.of(!busy && edit.changes), onClick = { onSave(edit) }, modifier = Modifier.weight(1f)) { Text(ui("guild.save")) }
        },
    ) {
        Text(ui("guild.stash_configure"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
        Engraved(ui("guild.stash_tab_name"))
        OutlinedTextField(
            name,
            { name = it.take(limit) },
            placeholder = { Text(numbered) },
            supportingText = { LengthCounter(name, limit) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (ranks.isNotEmpty()) {
            Engraved(ui("guild.stash_rank"))
            ChoiceChips(ranks, { it.key == rank }, { it.title }) { picked -> picked.key?.let { rank = it } }
            ranks.firstOrNull { it.key == rank }?.let { picked -> MutedText(rankNote(picked), style = MaterialTheme.typography.labelSmall) }
            MutedText(ui("guild.stash_rank_hint"), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Пояснение выбранного ранга под чипами: порог вклада и сколько членов гильдии в ранге. */
private fun rankNote(rank: PickOption<Int>): String = listOfNotNull(rank.subtitle, rank.data?.let { listOfNotNull(it.caption, it.value).joinToString(" ") }).joinToString(" · ")

/** Справка о монетах Содружества числами правила [rule]: очки редкостей, шаг уровня, шаг стопки и наценка взятия. */
private fun coinsHelp(rule: GuildCoinRule): String = ui(
    "guild.coins_help",
    rule.rarity[Rarity.MAGIC] ?: 0,
    rule.rarity[Rarity.RARE] ?: 0,
    rule.rarity[Rarity.UNIQUE] ?: 0,
    rule.rarity[Rarity.MYTHICAL] ?: 0,
    rule.levelStep,
    rule.pileStep,
    number(rule.takeRate),
)

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
 * Плита записи хранилища «Ценником»: копия - со всеми строками, как лот, стопка - иконка, имя и число; ярлык - цена взятия в монетах
 * Содружества и золоте ([StashValue.takeCost]). Касание - карточка, «Взять · 18 · 174 золота» в ней по удержанию; погашено с причиной,
 * если взятий на сегодня нет, монет или золота мало.
 *
 * Цена взятия против кошелька героя - тот же счёт, что у всех цен в золоте; монеты - против счёта героя [GuildStashView.coins].
 */
@Composable private fun EntryRow(game: GameUi, stash: GuildStashView, entry: GuildStashEntry, onTake: () -> Unit) {
    val index = game.index ?: return
    val view = entry.item?.let { game.view(it) }
    val inspect = rememberInspect()
    val cost = remember(index, entry) { StashValue.takeCost(index, entry.item, entry.code, entry.amount) }
    val coinsShort = cost.coins > stash.coins
    val bill = game.goldBill(cost.gold)
    val reason = when {
        stash.takesLeft == 0 -> ui("guild.stash_no_takes")
        coinsShort -> ui("guild.coins_short", groupedNumber(cost.coins))
        bill?.affordable == false -> bill.shortText()
        else -> null
    }
    val action = InspectAction(ui("guild.stash_take"), enabled = !game.busy && reason == null, reason = reason, hold = true, figure = takeFigure(cost)) { onTake() }
    val open = { inspect(entry.item?.let { Inspect.Copy(it, action) } ?: Inspect.Stack(entry.code, action)) }
    val tag: @Composable RowScope.() -> Unit = {
        if (cost.coins > 0) FellowshipCoins(cost.coins, CoinsRole.PRICE, affordable = !coinsShort)
        if (cost.coins > 0 && cost.gold > 0) Text("·", color = Muted, style = MaterialTheme.typography.labelMedium)
        if (cost.gold > 0) bill?.let { GoldCost(it, purse = false) } ?: GoldPrice(cost.gold)
    }
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

/** Число кнопки «Взять»: «18 · 174 золота», без монет - только золото, бесплатно - ничего. */
private fun takeFigure(cost: StashTakeCost): String? = when {
    cost.coins > 0 && cost.gold > 0 -> ui("guild.stash_take_both", compactNumber(cost.coins), compactNumber(cost.gold))
    cost.coins > 0 -> compactNumber(cost.coins)
    cost.gold > 0 -> ui("gold.amount", compactNumber(cost.gold))
    else -> null
}

/**
 * Окно «Положить» во вкладку [tab] хранилища [stash] - всегда выбор пачкой: в шапке «Свободно N / M», строка поиска сумки
 * ([ItemSearchBar], список [ItemShelf.GUILD_DEPOSIT]), вкладки-чипы «Снаряжение / Стопки / Карты» с числом отмеченного и быстрые
 * наборы ([PresetRow]: всё видимое и по редкости). Строка - общая строка выбора пачкой ([PickLine]): плитка тайника ([ItemTile],
 * уровень на гнезде) или стопка с числом; касание отмечает (золотая рамка и знак хранилища в углу), удержание - карточка. У
 * отмеченной стопки в строке - поле «− N +» ([AmountStepper], по умолчанию вся стопка). У запрещённого ([DepositLot.bar]: заперто,
 * привязано к герою) в углу замок, причина - в строке, касание - карточка.
 *
 * Справа у строки - начисление монет Содружества за неё ([DepositBag.award], «+0» серым), карточка вещи или стопки - с
 * «Положить · +12»: вклад одной этой посылки.
 *
 * Внизу закреплены «Отмена» и удерживаемая «Положить N · +S · влезет K» ([PickDock]): K - по правилу сервера ([DepositBag.fits]), что
 * не влезет, останется у героя; S - монеты за то, что ляжет ([DepositBag.earned]). Герою-тестировщику склад закрыт - причина в шапке,
 * кнопка погашена.
 */
@Composable
private fun DepositSheet(game: GameUi, stash: GuildStashView, tab: Int, onDismiss: () -> Unit, onPut: (List<GuildDepositParcel>) -> Unit) {
    val hero = game.hero ?: return
    val bag = remember(hero.items, hero.bag, stash, tab, game.index, game.world) { game.depositBag(stash, tab) } ?: return
    val filterState = rememberItemFilter(ItemShelf.GUILD_DEPOSIT)
    var kind by remember { mutableStateOf(DepositKind.GEAR) }
    var filtering by remember { mutableStateOf(false) }
    val amounts = remember { mutableStateMapOf<String, Long>() }
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
            }
        },
        footer = {
            val fits = bag.fits(picked)
            val earned = bag.earned(picked, amounts)
            PickDock(
                label = when {
                    picked.isEmpty() -> ui("sell.pick_hint")
                    earned > 0 -> ui("guild.deposit_do_coins", picked.size, compactNumber(earned))
                    else -> ui("guild.deposit_do", picked.size)
                },
                icon = ForgeGlyphs.Stash,
                enabled = !game.busy && fits > 0,
                figure = ui("guild.deposit_fits", fits).takeIf { picked.isNotEmpty() },
                onCancel = onDismiss,
                modifier = Modifier.weight(1f),
            ) { onPut(bag.parcels(picked, amounts)) }
        },
    ) {
        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                bag.lots.isEmpty() -> item { InfoCard(ui("guild.deposit_empty"), ui("guild.deposit_empty_hint")) }
                shown.isEmpty() -> item { MutedText(ui("guild.stash_none")) }
                kind == DepositKind.STACKS -> item { MutedText(ui("guild.deposit_stack_hint")) }
            }
            items(shown, key = { it.id }) { lot ->
                val award = bag.award(lot, amounts[lot.code])
                val single = depositOne(lot, award, room = bag.free > 0, busy = game.busy) { onPut(listOf(lot.parcel(amounts[lot.code]))) }
                DepositLine(pick, lot, award) { chosen, toggle ->
                    val piece = lot.piece
                    if (piece != null) {
                        val card = { inspect(Inspect.Copy(piece.item, single)) }
                        ItemTile(piece, selected = chosen, onClick = if (lot.bar == null) toggle else card, onLongClick = card) {
                            lot.bar?.let { BarReason(it) }
                        }
                    } else {
                        val card = { inspect(Inspect.Stack(lot.code, single)) }
                        StackLine(game.index, lot, chosen, amounts[lot.code], onAmount = { amounts[lot.code] = it }, onClick = if (lot.bar == null) toggle else card, onHold = card)
                    }
                }
            }
        }
    }
    if (filtering && shelf != null) ItemFilterSheet(filterState, shelf) { filtering = false }
}

/**
 * «Положить · +12» в карточке вещи или стопки окна вклада: одна эта посылка, удержанием. Запрещённая или без места во вкладке ([room] false) -
 * погашено с причиной, пока идёт запрос ([busy]) - погашено.
 */
private fun depositOne(lot: DepositLot, award: Long, room: Boolean, busy: Boolean, onPut: () -> Unit): InspectAction {
    val reason = lot.bar?.let { ui("guild.deposit_why.${it.name}") } ?: ui("guild.deposit_why.${DepositBar.FULL.name}").takeUnless { room }
    return InspectAction(ui("guild.stash_put"), enabled = !busy && reason == null, reason = reason, hold = true, figure = "+${compactNumber(award)}") { onPut() }
}

/**
 * Строка окна вклада: посылка [content] общей строкой выбора ([PickLine], знак хранилища в углу отмеченной), у запрещённого - замок
 * в углу; справа - начисление монет Содружества за неё [award] (у запрещённого - нет).
 */
@Composable private fun DepositLine(pick: Pick<DepositLot>, lot: DepositLot, award: Long, content: @Composable (chosen: Boolean, onToggle: () -> Unit) -> Unit) {
    val bar = lot.bar
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PickLine(pick, lot.id, PickMark.DEPOSIT, Modifier.weight(1f)) { chosen, toggle ->
            content(chosen, toggle)
            bar?.let { CornerMark(Icons.Outlined.Lock, ui("guild.deposit_why.${it.name}"), Muted, Modifier.align(Alignment.TopEnd)) }
        }
        if (bar == null) FellowshipCoins(award, CoinsRole.AWARD)
    }
}

/** Причина запрета посылки строкой под ней. */
@Composable private fun BarReason(bar: DepositBar) {
    Text(ui("guild.deposit_why.${bar.name}"), color = LifeRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
}

/**
 * Стопка сумки строкой: значок, имя, число («× 37», часть - «× 10 из 37» по выбранному [amount]) и причина запрета; касание - [onClick],
 * удержание - [onHold]. Отмеченная [chosen] - в золотой рамке, под именем - поле «− N +» ([AmountStepper]) от одной до всей стопки,
 * новое количество - [onAmount].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StackLine(index: ContentIndex?, lot: DepositLot, chosen: Boolean, amount: Long?, onAmount: (Long) -> Unit, onClick: () -> Unit, onHold: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape)
            .then(if (chosen) Modifier.border(2.dp, GoldBright, shape) else Modifier)
            .combinedClickable(onLongClickLabel = ui("hint.hold.info"), onLongClick = onHold, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StackCell(index, lot.code, 1, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(itemTitle(lot.code), color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                lot.bar?.let { BarReason(it) }
            }
            Text(lot.countText(amount), color = GoldBright, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
        if (chosen && lot.bar == null) {
            AmountStepper(
                lot.amountOf(amount),
                1L..lot.owned.coerceAtLeast(1),
                enabled = true,
                label = ui("sell.amount_owned", lot.owned),
                less = ui("guild.deposit_less"),
                more = ui("guild.deposit_more"),
                modifier = Modifier.align(Alignment.End),
                onChange = onAmount,
            )
        }
    }
}
