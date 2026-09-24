package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.trade.MerchantOffer
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
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
                stock?.let { Text(ui("merchant.renews", untilText(it.refreshAt)), color = Muted, style = MaterialTheme.typography.bodySmall) }
                Text(ui("merchant.note"), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (stock != null && stock.offers.isEmpty()) item { InfoCard(ui("merchant.empty"), ui("merchant.empty_hint")) }
        items(stock?.offers.orEmpty(), key = { it.id }) { offer ->
            ItemRow(inventoryDocument(offer.item, s.world.inventoryBases[offer.item.equipmentId]), definitions = s.world.definitions, enabled = !s.busy,
                unwearable = s.play.hero?.sheet?.unwearableBy?.get(offer.item.equipmentId).orEmpty(),
                trailing = { GoldPrice(offer.price) }, onClick = { chosen = offer })
        }
    }
    chosen?.let { offer ->
        val left = money?.let { it - offer.price }
        ConfirmSheet(title = ui("merchant.buy_q"), confirm = ui("merchant.buy"), onDismiss = { chosen = null },
            ledger = listOfNotNull(LedgerLine(ui("confirm.spend"), ui("merchant.gold_amount", offer.price), Tone.SPEND),
                left?.takeIf { it >= 0 }?.let { LedgerLine(ui("confirm.left"), ui("merchant.gold_amount", it)) }),
            warning = if (left != null && left < 0) ui("merchant.short") else null,
            icon = { Icon(ForgeGlyphs.Coins, null, tint = Gold, modifier = Modifier.size(40.dp)) }) {
            chosen = null
            vm.buyOffer(offer.id)
        }
    }
}

/** A price in gold, as a lot's price in orbs is drawn: the coin and the figure. */
@Composable internal fun GoldPrice(price: Long) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(ForgeGlyphs.Coins, null, tint = Gold, modifier = Modifier.size(15.dp))
        Text(number(price.toDouble()), color = GoldBright, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** How long until an epoch-millisecond moment, in hours and minutes by the device's clock. */
internal fun untilText(at: Long): String {
    val minutes = ((at - System.currentTimeMillis()) / 60_000).coerceAtLeast(0)
    return ui("merchant.time", minutes / 60, minutes % 60)
}
