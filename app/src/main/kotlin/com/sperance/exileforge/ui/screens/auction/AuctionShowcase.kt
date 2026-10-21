package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.itemIcon
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.itemVisualKind
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.text
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.trade.LotKind
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.screens.server.SettingsViewModel
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** Витрина аукциона: список лотов, строка поиска и фильтры. */
/** The showcase: the server's own search, so the page and the filter both belong to it. */
@Composable internal fun ColumnScope.ShowcaseTab(game: GameUi, market: Market, vm: MarketViewModel) {
    ShowcaseList(game, market, header = { ShowcaseHeader(game, market, vm) }, onBuy = vm::buy, onMore = vm::moreShowcase, onRetry = vm::loadShowcase)
}

/**
 * The lots themselves, the next page added under them by scrolling to the edge ([PagedColumn]) while the server has one.
 *
 * The filter travels in as a header so the list can be driven without a view model: what is worth
 * checking here is which lot offers a Buy button and what the price says, not the form wiring.
 *
 * Buying happens in the lot's own sheet and nowhere else. A line in a list is not enough to decide
 * on — the rolls are what a trader is paying for — and a purchase is irreversible, so it is worth
 * the extra tap that guarantees the item was looked at.
 *
 * Поиск и фильтры уходят при прокрутке вниз. Не загрузилась витрина - отказ и «Повторить», а не «ничего не найдено». Свой лот не купить.
 * Нехватку видно сразу: «Купить» гаснет, под ней - чего не хватает. Покупку не отменить, поэтому она спрашивается, а вещь, которую не
 * надеть, называется в том же вопросе. Сбор золотом называет долю цены лота, «Останется» - тем же счётом, что у всех оплат.
 */
@Composable internal fun ColumnScope.ShowcaseList(
    game: GameUi,
    market: Market,
    header: @Composable () -> Unit = {},
    onBuy: (String) -> Unit,
    onMore: () -> Unit,
    onRetry: () -> Unit = {},
) {
    var openLot by remember { mutableStateOf<String?>(null) }
    var confirmBuy by remember { mutableStateOf<String?>(null) }
    Box(Modifier.padding(top = 10.dp)) { CollapsibleHeader { header() } }
    val showcase = market.showcase
    PagedColumn(showcase.items.size, showcase.next != null, Reads.AUCTION in game.loading, onMore, market.filter, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (market.showcase.items.isEmpty() && market.showcaseFailed) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoCard(ui("auction.showcase_failed"), ui("auction.showcase_failed_hint"), failure = true)
                    ForgeButton(state = ButtonState.of(Reads.AUCTION !in game.loading), onClick = onRetry, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) {
                        Text(ui("auction.showcase_retry"))
                    }
                }
            }
        } else if (market.showcase.items.isEmpty()) {
            item {
                InfoCard(
                    ui("tree.nothing_found"),
                    ui("auction.showcase_empty"),
                )
            }
        }
        items(market.showcase.items, key = { it.id }) { lot ->
            LotRow(game, lot, marks = if (lot.belongsTo(game.heroId)) listOf(TradeMark(ui("auction.your_lot"))) else emptyList()) { openLot = lot.id }
        }
    }
    market.showcase.items.firstOrNull { it.id == openLot }?.let { lot ->
        val own = lot.belongsTo(game.heroId)
        val short = game.shortfall(lot.cost)?.text()
        LotSheet(
            game,
            lot,
            action = ui("auction.buy"),
            enabled = !game.busy && !own && short == null,
            note = if (own) ui("auction.own_lot") else short,
            onDismiss = { openLot = null },
            seller = !own,
        ) {
            openLot = null
            confirmBuy = lot.id
        }
    }
    market.showcase.items.firstOrNull { it.id == confirmBuy }?.let { lot ->
        val blocked = lotUnmet(game, lot)
        val orb = orbTitle(lot)
        val have = game.bagAmount(lot.priceOrb)
        val fee = lot.fee
        val feeRate = game.index?.rules?.auction?.buyerFee?.let { r -> if (r % 1.0 == 0.0) r.toLong().toString() else r.toString() }
        val short = game.shortfall(lot.cost)?.text()
        ConfirmSheet(
            title = ui("auction.buy_q"),
            subtitle = lot.title,
            icon = { LotIcon(game, lot, Modifier.size(44.dp)) },
            ledger = listOfNotNull(
                LedgerLine(ui("confirm.spend"), ui("confirm.minus", lot.price, orb), Tone.SPEND),
                fee.takeIf { it > 0 }?.let { LedgerLine(feeRate?.let { r -> ui("auction.fee_rate", r) } ?: ui("auction.fee"), ui("merchant.gold_amount", it), Tone.SPEND) },
                have?.takeIf { it >= lot.price }?.let { LedgerLine(ui("confirm.left"), ui("confirm.amount", it - lot.price, orb)) },
                game.goldBill(fee)?.takeIf { fee > 0 && it.affordable }?.let { LedgerLine(ui("confirm.left"), ui("merchant.gold_amount", number(it.left.toDouble()))) },
                LedgerLine(ui("confirm.gain"), lot.title, Tone.GAIN),
                LedgerLine(ui("auction.seller"), sellerName(lot)),
            ),
            warning = listOfNotNull(
                short,
                blocked.takeIf { it.isNotEmpty() }?.let {
                    ui("auction.unwearable", it.joinToString(", ") { r -> requirementReason(r, game.lang) })
                },
            ).joinToString("\n").ifBlank { null },
            blocked = short != null,
            confirm = ui("auction.buy_do"),
            onDismiss = { confirmBuy = null },
        ) { onBuy(lot.id) }
    }
}

/**
 * Шапка витрины - строка поиска ([SearchBar]): имя, кнопка ко всем прочим фильтрам и заданные фильтры чипами под ней
 * ([AuctionFilter.narrowings], «Мои лоты» - тоже сужение фильтра).
 *
 * Сам по себе здесь ничего не спрашивает сервер: спрашивают клавиша поиска клавиатуры, очистка поля, «Показать» в шторке и
 * снятие чипа - чип есть фильтр, что понимает сервер, и его крестик снимает его и спрашивает снова: самый короткий путь назад от
 * «ничего не найдено».
 */
@Composable internal fun ShowcaseHeader(game: GameUi, market: Market, vm: MarketViewModel) {
    var sheet by remember { mutableStateOf(false) }
    val f = market.filter
    val narrow = { next: AuctionFilter ->
        vm.filter(next)
        vm.loadShowcase()
    }
    SearchBar(
        f.title,
        { vm.filter(f.copy(title = it)) },
        ui("auction.search"),
        onSearch = vm::loadShowcase,
        filter = SearchFilter(
            rememberChips(f.narrowings(game.lang), narrow),
            onOpen = { sheet = true },
            onReset = { narrow(f.cleared()) },
            enabled = !game.busy,
        ),
        maxLength = game.inputs.search,
    )
    if (sheet) {
        AuctionFilterSheet(game, market, onDismiss = { sheet = false }) { filter ->
            sheet = false
            narrow(filter)
        }
    }
}

/** The slots a template may have: the second ring and the belt's other flasks are places of the worn, never of a lot. */
internal val templateSlots: List<Slot> = Slot.entries.filter { it != Slot.RING_2 && it != Slot.FLASK_2 }

/**
 * Every filter the server understands, on a draft, in the shared «Аккордеон» sheet: each filter a row with its value that opens
 * on a tap, own lots as a switch, the item level range in the sheet's head ([LevelRangeHeader]); the name is typed in the search bar
 * above the showcase. Nothing reaches the showcase until
 * «Показать», so trying a combination costs no request, and «Сбросить» clears all but the typed name.
 *
 * Сервер ищет по одной редкости: плитка выбирает её, повторное нажатие снимает. Валюта цены - код предмета. Продавец - по id героя. Свои
 * лоты не показываются, если продавец не сравнивает цены. Раскрытие свойств легенд - настройка устройства: действует сразу, «Сбросить» её
 * не трогает; «Сбросить» применяет сброс сразу и закрывает шторку.
 */
@Composable
internal fun AuctionFilterSheet(game: GameUi, market: Market, onDismiss: () -> Unit, onApply: (AuctionFilter) -> Unit) {
    var draft by remember { mutableStateOf(market.filter) }
    val settings = koinViewModel<SettingsViewModel>()
    val legendsOpen = settings.settings.collectAsStateWithLifecycle().value.legendsOpen
    val any = ui("filter.any")
    val digits = KeyboardOptions(keyboardType = KeyboardType.Number)
    val groups = listOf(
        AccordionGroup("kind", ui("auction.what_sold"), draft.kind.takeIf { it.isNotBlank() }?.let { draft.fieldTitle(FilterField.KIND, game.lang) } ?: any) {
            ChoiceChips(listOf<LotKind?>(null) + LotKind.entries, { (it?.name ?: "") == draft.kind }, { it?.let { kind -> lotKindTitle(kind, game.lang) } ?: any }) { draft = draft.copy(kind = it?.name.orEmpty()) }
        },
        AccordionGroup("slot", ui("filter.type"), draft.slot.takeIf { it.isNotBlank() }?.let { slotTitle(it, game.lang) } ?: any) {
            ChoiceChips(listOf("") + templateSlots.map { it.name }, { it == draft.slot }, { if (it.isBlank()) any else slotTitle(it, game.lang) }) { draft = draft.copy(slot = it) }
        },
        AccordionGroup("rarity", ui("filter.rarity"), draft.rarity.takeIf { it.isNotBlank() }?.let { rarityTitle(it, game.lang) } ?: any) {
            RarityChoice(Rarity.entries, { it?.name == draft.rarity }, { picked -> draft = draft.copy(rarity = picked?.name?.takeIf { it != draft.rarity }.orEmpty()) }, mode = ChipMode.Filter, size = ChipSize.Dense)
        },
        AccordionGroup("price", ui("auction.price"), listOfNotNull(draft.priceOrb.takeIf { it.isNotBlank() }?.let(::itemTitle), draft.maxPrice.takeIf { it.isNotBlank() }?.let { ui("auction.chip_max_price", it) }).joinToString(" · ").ifBlank { any }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OrbGrid(ui("auction.priced_in"), game.currencies, draft.priceOrb, { draft = draft.copy(priceOrb = it) }, any = any)
                OutlinedTextField(
                    draft.maxPrice,
                    { draft = draft.copy(maxPrice = it.filter(Char::isDigit).take(game.inputs.number)) },
                    label = { Text(ui("auction.price_max")) },
                    singleLine = true,
                    keyboardOptions = digits,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        AccordionGroup("seller", ui("auction.seller"), draft.sellerId.takeIf { it.isNotBlank() }?.let { draft.fieldTitle(FilterField.SELLER, game.lang) } ?: any) {
            OutlinedTextField(
                draft.sellerId,
                { draft = draft.copy(sellerId = it.trim().take(game.inputs.code), sellerName = "") },
                label = { Text(ui("auction.seller")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
    FilterAccordionSheet(
        groups = groups,
        switches = listOf(
            FilterSwitch(ui("auction.show_mine"), draft.showOwnLots) { draft = draft.copy(showOwnLots = it) },
            FilterSwitch(ui("auction.legends_open"), legendsOpen) { open -> settings.change { copy(legendsOpen = open) } },
        ),
        show = ui("filter.show_all"),
        resettable = draft != draft.cleared(),
        onReset = { if (!game.busy) onApply(draft.cleared()) },
        onShow = { if (!game.busy) onApply(draft) },
        onDismiss = onDismiss,
        header = { LevelRangeHeader(draft.level, game.heroLevel) { draft = draft.copy(level = it) } },
    )
}
