package com.sperance.exileforge.ui.screens.auction

import com.sperance.exileforge.rules.content.SlotGroup
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.AutoSell
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.trade.MerchantOffer
import com.sperance.exileforge.core.model.trade.MerchantOrb
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.screens.hero.WearPreview
import com.sperance.exileforge.ui.theme.*

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MerchantScreen(s: ForgeState, vm: ForgeViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(12.dp))
        ScreenHeader(ui("merchant.title"), null, ForgeGlyphs.Coins, guide = Guide.MERCHANT)
        // The shelf rides on the hero's snapshot; entering reads it afresh all the same.
        LaunchedEffect(s.play.heroId, s.account.sessionEpoch) {
            if (s.play.heroId.isNotBlank()) { vm.ensureHero(); vm.loadMerchant() }
        }
        PullToRefreshBox(isRefreshing = Reads.MERCHANT in s.loading, onRefresh = vm::loadMerchant, modifier = Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize()) { MerchantTab(s, vm) }
        }
    }
}

@Composable private fun ColumnScope.MerchantTab(s: ForgeState, vm: ForgeViewModel) {
    var chosen by remember { mutableStateOf<MerchantOffer?>(null) }
    var filtering by remember { mutableStateOf(false) }
    val stock = s.market.merchant
    val money = s.hero?.money
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            ForgePanel {
                Engraved(ui("merchant.title"))
                money?.let { PropertyRow(ui("merchant.gold"), number(it.toDouble()), Glyph.CURRENCY) }
                stock?.let { MutedText(ui("merchant.renews", untilText(it.refreshAt))) }
                MutedText(ui("merchant.note"))
                s.hero?.info?.autoSell?.let { filter -> AutoSellButton(filter) { filtering = true } }
            }
        }
        stock?.orbs?.takeIf { it.isNotEmpty() }?.let { orbs ->
            item {
                ForgePanel {
                    Engraved(ui("merchant.orbs"))
                    MutedText(ui("merchant.orbs_note"))
                    orbs.forEach { orb ->
                        val price = orb.price
                        OrbRow(orb, price, have = s.bagAmount(orb.code), enabled = !s.busy && !orb.soldOut && (money == null || money >= price)) { vm.buyOrb(orb.code) }
                    }
                }
            }
        }
        if (stock != null && stock.offers.isEmpty()) item { InfoCard(ui("merchant.empty"), ui("merchant.empty_hint")) }
        items(stock?.offers.orEmpty(), key = { it.id }) { offer ->
            // A copy whose template the content does not hold cannot be drawn, and is not offered.
            val view = s.view(offer.item) ?: return@items
            ItemRow(view, enabled = !s.busy, unwearable = s.unmetFor(offer.item.template),
                trailing = { GoldPrice(offer.price) }, onClick = { chosen = offer })
        }
    }
    chosen?.let { offer -> OfferSheet(s, offer, money, onDismiss = { chosen = null }) { chosen = null; vm.buyOffer(offer.id) } }
    // Read from the snapshot on every pass, so a chip turns as soon as the server has the new filter.
    if (filtering) s.hero?.info?.autoSell?.let { filter ->
        AutoSellSheet(filter, enabled = !s.busy, onChange = vm::autoSell, onDismiss = { filtering = false })
    }
}

/** One orb on the shelf: its glass and name, how many the bag holds, and the button with the next price. */
@Composable private fun OrbRow(orb: MerchantOrb, price: Long, have: Long?, enabled: Boolean, onBuy: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OrbGlyph(Orb.of(orb.code), Modifier.size(32.dp))
        Column(Modifier.weight(1f)) {
            Text(itemTitle(orb.code), style = MaterialTheme.typography.bodyMedium)
            have?.let { MutedText(ui("merchant.orb_have", it)) }
            // The window's stock (3.24.0): how many more the shelf holds, in ember once it holds none.
            orb.left?.let { Text(ui("merchant.orb_left", it), color = if (it > 0) Muted else Ember, style = MaterialTheme.typography.labelSmall) }
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
@Composable private fun OfferSheet(s: ForgeState, offer: MerchantOffer, money: Long?, onDismiss: () -> Unit, onBuy: () -> Unit) {
    val view = s.view(offer.item)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (view != null) item { ItemCard(view, enabled = false, detailed = true) }
                item { WearPreview(s, offer.item) }
            }
            OrnateDivider()
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                money?.let { PropertyRow(ui("merchant.gold"), number(it.toDouble()), Glyph.CURRENCY) }
                val price = offer.price
                if (money != null && money < price) Text(ui("merchant.short"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                HoldButton(ui("merchant.buy_for", number(price.toDouble())), Gold, Modifier.fillMaxWidth(),
                    enabled = !s.busy && (money == null || money >= price), onHeld = onBuy)
            }
        }
    }
}

/** How long until an epoch-millisecond moment, in hours and minutes by the device's clock. */
internal fun untilText(at: Long): String {
    val minutes = ((at - System.currentTimeMillis()) / 60_000).coerceAtLeast(0)
    return ui("merchant.time", minutes / 60, minutes % 60)
}

/** The loot filter's door in the header: its name and how many marks are on; the marks themselves wait in [AutoSellSheet]. */
@Composable private fun AutoSellButton(filter: AutoSell, onClick: () -> Unit) {
    val marks = filter.sell.values.sumOf { it.size }
    ForgeOutlinedButton(onClick = onClick) {
        Icon(Icons.Outlined.FilterList, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(ui("merchant.autosell"))
        if (marks > 0) {
            Spacer(Modifier.width(6.dp))
            Text(marks.toString(), color = GoldBright, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * The loot filter (3.47.0, server 1.45.0): a row per rarity, a chip per slot group — what is on, the merchant takes from a
 * run's loot before it reaches the stash. A unique, an influenced, corrupted, fractured or locked piece never goes.
 * A sheet behind the header's button, so the shelf is not pushed down by it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable private fun AutoSellSheet(filter: AutoSell, enabled: Boolean, onChange: (Rarity, Set<SlotGroup>) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Engraved(ui("merchant.autosell"))
            MutedText(ui("merchant.autosell_note"))
            AutoSell.SELLABLE.forEach { rarity ->
                val on = filter.sell[rarity].orEmpty()
                Text(ui("enum.rarity.${rarity.name}"), color = rarityColor(rarity.name), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SlotGroup.entries.forEach { group ->
                        FilterChip(selected = group in on, enabled = enabled, onClick = { onChange(rarity, if (group in on) on - group else on + group) },
                            label = { Text(ui("merchant.group.${group.name}")) })
                    }
                }
            }
        }
    }
}
