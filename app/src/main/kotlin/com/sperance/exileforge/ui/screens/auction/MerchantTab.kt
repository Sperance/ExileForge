package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.trade.MerchantOffer
import com.sperance.exileforge.core.model.trade.MerchantOrb
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.market.MarketViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.AutoSell
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SlotGroup
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.screens.hero.StackInfoSheet
import com.sperance.exileforge.ui.screens.hero.WearPreview
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The merchant (since 2.36.0, server 0.34.0): a shelf of items the server rolled for this hero,
 * sold for gold, new every few hours and never sooner. The price is the server's — what the
 * merchant would pay for the item, several times over — and buying asks first, held.
 *
 * An offer is a copy like any in the stash (3.0.0): its view over the content draws it, and the
 * rules say whether this hero could wear it.
 *
 * Under the header, the orb shelf (3.15.0, server 1.13.0): lesser orbs for gold, each one of a kind
 * dearer than the last until the shelf renews with the wares.
 *
 * It left the auction's tabs in 3.22.0 for a building of the City of its own.
 */
@Composable fun MerchantScreen() {
    val game by koinViewModel<MarketViewModel>().game.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    val market = koinViewModel<MarketViewModel>()
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // The shelf rides on the hero's snapshot; entering reads it afresh all the same.
        LaunchedEffect(game.heroId, game.sessionEpoch) {
            if (game.heroId.isNotBlank()) {
                heroModel.ensure()
                market.loadMerchant()
            }
        }
        MerchantTab(game, market)
    }
}

/**
 * «Шапка и вкладки» (variant A): the gold, the time to the next shelf and the loot filter in one strip; the notes behind
 * the header's (i); the wares and the orbs as two halves of one switch, so the first item is in sight at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.MerchantTab(game: GameUi, market: MarketViewModel) {
    val heroModel: HeroViewModel = koinViewModel()
    val shelf by market.market.collectAsStateWithLifecycle()
    val activity by market.activity.collectAsStateWithLifecycle()
    val busy = activity.busy
    var chosen by remember { mutableStateOf<MerchantOffer?>(null) }
    var filtering by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(false) }
    var orbsShelf by rememberSaveable { mutableStateOf(false) }
    // The orb a tap on its glass or name opened: what it is for, before it is bought.
    var info by remember { mutableStateOf<String?>(null) }
    val stock = shelf.merchant
    val money = game.hero?.money
    // A copy whose template the content does not hold cannot be drawn, and is not offered.
    val offers = remember(stock?.offers, game.index, game.world) { stock?.offers.orEmpty().mapNotNull { offer -> game.view(offer.item)?.let { offer to it } } }
    val orbs = stock?.orbs.orEmpty()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(ForgeGlyphs.Coins, null, tint = Gold, modifier = Modifier.size(24.dp))
        Text(ui("merchant.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = { notes = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.Info, ui("merchant.notes"), tint = Muted) }
    }
    MerchantStrip(money, stock?.refreshAt?.takeIf { it > 0 }, game.hero?.info?.autoSell) { filtering = true }
    if (orbs.isNotEmpty()) {
        PillTabs(
            listOf(ui("merchant.wares_n", offers.size), ui("merchant.orbs_n", orbs.size)),
            if (orbsShelf) 1 else 0,
            { orbsShelf = it == 1 },
            segmented = true,
        )
    }
    PullToRefreshBox(isRefreshing = Reads.MERCHANT in activity.loading, onRefresh = market::loadMerchant, modifier = Modifier.weight(1f)) {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
            if (orbsShelf && orbs.isNotEmpty()) {
                item {
                    ForgePanel {
                        orbs.forEach { orb ->
                            val price = orb.price
                            OrbRow(
                                orb,
                                price,
                                have = game.bagAmount(orb.code),
                                enabled = !busy && !orb.soldOut && (money == null || money >= price),
                                onInfo = { info = orb.code },
                            ) { market.buyOrb(orb.code) }
                        }
                    }
                }
            } else {
                if (stock != null && offers.isEmpty()) item { InfoCard(ui("merchant.empty"), ui("merchant.empty_hint")) }
                items(offers, key = { it.first.id }) { (offer, view) ->
                    ItemTradeRow(view, enabled = !busy, unmet = game.unmetFor(offer.item.template), onClick = { chosen = offer }) { GoldPrice(offer.price) }
                }
            }
        }
    }
    if (notes) MerchantNotes { notes = false }
    info?.let { code -> StackInfoSheet(game, code) { info = null } }
    chosen?.let { offer ->
        OfferSheet(game, offer, money, onDismiss = { chosen = null }) {
            chosen = null
            market.buyOffer(offer.id)
        }
    }
    // Read from the snapshot on every pass, so a chip turns as soon as the server has the new filter.
    if (filtering) {
        game.hero?.info?.autoSell?.let { filter ->
            AutoSellSheet(filter, enabled = !game.busy, onChange = heroModel::autoSell, onDismiss = { filtering = false })
        }
    }
}

/** The strip under the header: the purse, when the shelf renews, and the loot filter's door with the number of marks on. */
@Composable private fun MerchantStrip(money: Long?, refreshAt: Long?, filter: AutoSell?, onFilter: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, PanelRaised, shape).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        money?.let { GoldPrice(it) }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            refreshAt?.let {
                Icon(Icons.Outlined.Schedule, null, tint = Muted, modifier = Modifier.size(14.dp))
                Text(
                    ui("merchant.renews", untilText(it)),
                    color = Muted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        filter?.let { AutoSellButton(it, onFilter) }
    }
}

/** The merchant's two notes — how the shelf renews and is priced, how the orbs grow dearer — behind the header's (i). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchantNotes(onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Engraved(ui("merchant.title"))
            Text(ui("merchant.note"), color = Parchment, style = MaterialTheme.typography.bodyMedium)
            Engraved(ui("merchant.orbs"))
            Text(ui("merchant.orbs_note"), color = Parchment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** One orb on the shelf: its glass and name (a tap on them opens the orb), how many the bag holds, and the button with the next price. */
@Composable private fun OrbRow(orb: MerchantOrb, price: Long, have: Long?, enabled: Boolean, onInfo: () -> Unit, onBuy: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.weight(1f).clickable(onClickLabel = itemTitle(orb.code), onClick = onInfo),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OrbGlyph(Orb.of(orb.code), Modifier.size(32.dp))
            Column(Modifier.weight(1f)) {
                Text(itemTitle(orb.code), style = MaterialTheme.typography.bodyMedium)
                have?.let { MutedText(ui("merchant.orb_have", it)) }
                // The window's stock (3.24.0): how many more the shelf holds, in ember once it holds none.
                orb.left?.let { Text(ui("merchant.orb_left", it), color = if (it > 0) Muted else Ember, style = MaterialTheme.typography.labelSmall) }
            }
        }
        ForgeOutlinedButton(enabled = enabled, onClick = onBuy) { GoldPrice(price) }
    }
}

/**
 * An offer's full card (since 2.40.0): the item as the stash would show it, scrolling, and under it
 * the one way to buy it — a button held for the price. Short of gold, it says so and the button
 * stays off (2.46.0); what wearing it would change is added up here, as on a stash card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfferSheet(game: GameUi, offer: MerchantOffer, money: Long?, onDismiss: () -> Unit, onBuy: () -> Unit) {
    val view = game.view(offer.item)
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (view != null) item { ItemCard(view, enabled = false, detailed = true) }
                item { WearPreview(game, offer.item) }
            }
            OrnateDivider()
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                money?.let { PropertyRow(ui("merchant.gold"), number(it.toDouble()), Glyph.CURRENCY) }
                val price = offer.price
                if (money != null && money < price) Text(ui("merchant.short"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                HoldButton(
                    ui("merchant.buy_for", number(price.toDouble())),
                    Gold,
                    Modifier.fillMaxWidth(),
                    enabled = !game.busy && (money == null || money >= price),
                    onHeld = onBuy,
                )
            }
        }
    }
}

/** How long until an epoch-millisecond moment, in hours and minutes by the device's clock. */
internal fun untilText(at: Long): String {
    val minutes = ((at - System.currentTimeMillis()) / 60_000).coerceAtLeast(0)
    return ui("merchant.time", minutes / 60, minutes % 60)
}

/** The loot filter's door in the strip: its glyph with how many marks are on; the marks themselves wait in [AutoSellSheet]. */
@Composable private fun AutoSellButton(filter: AutoSell, onClick: () -> Unit) {
    val marks = filter.sell.values.sumOf { it.size }
    BadgedBox(badge = { if (marks > 0) Badge(containerColor = Vital, contentColor = Ink) { Text(marks.toString()) } }) {
        IconButton(onClick = onClick) { Icon(Icons.Outlined.FilterList, ui("merchant.autosell"), tint = Gold) }
    }
}

/**
 * The loot filter (3.47.0, server 1.45.0): a row per rarity, a chip per slot group — what is on, the merchant takes from a
 * run's loot before it reaches the stash. A unique, an influenced, corrupted, fractured or locked piece never goes.
 * A sheet behind the header's button, so the shelf is not pushed down by it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AutoSellSheet(filter: AutoSell, enabled: Boolean, onChange: (Rarity, Set<SlotGroup>) -> Unit, onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Engraved(ui("merchant.autosell"))
            MutedText(ui("merchant.autosell_note"))
            AutoSell.SELLABLE.forEach { rarity ->
                val on = filter.sell[rarity].orEmpty()
                Text(ui("enum.rarity.${rarity.name}"), color = rarityColor(rarity.name), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SlotGroup.entries.forEach { group ->
                        FilterChip(
                            selected = group in on,
                            enabled = enabled,
                            onClick = { onChange(rarity, if (group in on) on - group else on + group) },
                            label = { Text(ui("merchant.group.${group.name}")) },
                        )
                    }
                }
            }
        }
    }
}
