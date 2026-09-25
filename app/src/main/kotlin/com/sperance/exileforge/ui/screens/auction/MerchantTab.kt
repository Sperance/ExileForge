package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.trade.MerchantOffer
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.hero.WearPreview
import com.sperance.exileforge.ui.theme.*

/**
 * The merchant (since 2.36.0, server 0.34.0): a shelf of four to six items the server rolled for
 * this hero, sold for gold, new every four hours and never sooner. The price is the server's —
 * what the merchant would pay for the item, four times over — and buying asks first, held.
 */
@Composable internal fun ColumnScope.MerchantTab(s: ForgeState, vm: ForgeViewModel) {
    var chosen by remember { mutableStateOf<MerchantOffer?>(null) }
    val stock = s.market.merchant
    val money = s.play.hero?.character?.money
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            ForgePanel {
                Engraved(ui("merchant.title"))
                money?.let { PropertyRow(ui("merchant.gold"), number(it.toDouble()), com.sperance.exileforge.core.display.Glyph.CURRENCY) }
                stock?.let { MutedText(ui("merchant.renews", untilText(it.refreshAt))) }
                MutedText(ui("merchant.note"))
            }
        }
        if (stock != null && stock.offers.isEmpty()) item { InfoCard(ui("merchant.empty"), ui("merchant.empty_hint")) }
        items(stock?.offers.orEmpty(), key = { it.id }) { offer ->
            ItemRow(inventoryDocument(offer.item, s.world.inventoryBases[offer.item.equipmentId]), definitions = s.world.definitions, enabled = !s.busy,
                unwearable = s.play.hero?.sheet?.unwearableBy?.get(offer.item.equipmentId).orEmpty(),
                trailing = { GoldPrice(offer.price) }, onClick = { chosen = offer })
        }
    }
    chosen?.let { offer -> OfferSheet(s, offer, money, onDismiss = { chosen = null }) { chosen = null; vm.buyOffer(offer.id) } }
}

/**
 * An offer's full card (since 2.40.0): the item as the stash would show it, scrolling, and under it
 * the one way to buy it — a button held for the price. Short of gold, it says so and the button
 * stays off (2.46.0); what wearing it would change is added up here, as on a stash card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun OfferSheet(s: ForgeState, offer: MerchantOffer, money: Long?, onDismiss: () -> Unit, onBuy: () -> Unit) {
    val document = inventoryDocument(offer.item, s.world.inventoryBases[offer.item.equipmentId])
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { ItemCard(document, enabled = false, detailed = true, definitions = s.world.definitions) }
                item { WearPreview(s, offer.item) }
            }
            OrnateDivider()
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                money?.let { PropertyRow(ui("merchant.gold"), number(it.toDouble()), com.sperance.exileforge.core.display.Glyph.CURRENCY) }
                if (money != null && money < offer.price) Text(ui("merchant.short"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                HoldButton(ui("merchant.buy_for", number(offer.price.toDouble())), Gold, Modifier.fillMaxWidth(),
                    enabled = !s.busy && (money == null || money >= offer.price), onHeld = onBuy)
            }
        }
    }
}

/** How long until an epoch-millisecond moment, in hours and minutes by the device's clock. */
internal fun untilText(at: Long): String {
    val minutes = ((at - System.currentTimeMillis()) / 60_000).coerceAtLeast(0)
    return ui("merchant.time", minutes / 60, minutes % 60)
}
