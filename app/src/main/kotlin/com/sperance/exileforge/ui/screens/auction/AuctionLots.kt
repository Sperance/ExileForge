package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.itemIcon
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.itemVisualKind
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.market.MarketViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.icons.orbArt
import com.sperance.exileforge.ui.screens.hero.StackInfoSheet
import com.sperance.exileforge.ui.theme.*

/** Свои лоты и история сделок (3.80.24). */
/** The hero's own lots that are still on sale; withdrawn and sold lots leave this list. */
@Composable internal fun ColumnScope.MyLotsTab(s: ForgeState, market: Market, vm: MarketViewModel) {
    var openLot by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        // Lot places (3.47.0): the same for every hero, never bought.
        market.slots?.let { slots ->
            item {
                ForgePanel {
                    PropertyRow(ui("auction.slots"), ui("auction.slots_value", slots.used, slots.limit), Glyph.ITEM)
                    if (slots.full) Text(ui("auction.slots_full"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (market.ownLots.isEmpty()) {
            item {
                InfoCard(ui("auction.no_lots"), ui("auction.no_lots_hint"))
            }
        }
        items(market.ownLots, key = { it.id }) { lot ->
            val window = s.index?.rules?.auction?.extendWindowMillis ?: 0L
            LotRow(s, lot, mark = lotExpiry(lot)?.let { if (lot.extendable(window)) it + " · " + ui("auction.extend_now") else it }, withSeller = false) { openLot = lot.id }
        }
    }
    market.ownLots.firstOrNull { it.id == openLot }?.let { lot ->
        // On its last day the author may give it another week (3.79.0), as often as they like.
        val extendable = s.index?.rules?.auction?.let { lot.extendable(it.extendWindowMillis) } == true
        LotSheet(
            s,
            lot,
            action = ui("auction.withdraw"),
            enabled = !s.busy,
            note = lotExpiry(lot),
            onDismiss = { openLot = null },
            extra = if (extendable) {
                (
                    {
                        ForgeOutlinedButton(enabled = !s.busy, onClick = {
                            openLot = null
                            vm.extend(lot.id)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(ui("auction.extend", s.index?.rules?.auction?.lotDays ?: 7))
                        }
                    }
                    )
            } else {
                null
            },
        ) {
            openLot = null
            vm.cancel(lot.id)
        }
    }
}

/** Which deals the history shows (3.73.0). */
internal enum class DealFilter { ALL, SOLD, BOUGHT }

/**
 * The hero's deals of the last days (3.73.0): what they sold and to whom, what they bought and from whom, the copy as it
 * changed hands a tap away, and the orbs earned and spent summed on top.
 */
@Composable internal fun ColumnScope.HistoryTab(s: ForgeState, market: Market) {
    var filter by remember { mutableStateOf(DealFilter.ALL) }
    var openLot by remember { mutableStateOf<String?>(null) }
    val heroId = s.play.heroId
    val deals = market.history.map { it.deal }
    val shown = deals.filter { deal ->
        when (filter) {
            DealFilter.ALL -> true
            DealFilter.SOLD -> deal.belongsTo(heroId)
            DealFilter.BOUGHT -> !deal.belongsTo(heroId)
        }
    }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            ForgePanel {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DealFilter.entries.forEach { f ->
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(ui("auction.history_${f.name.lowercase()}")) })
                    }
                }
                orbTotals(deals.filter { it.belongsTo(heroId) }).takeIf { it.isNotBlank() }?.let { PropertyRow(ui("auction.history_earned"), it, Glyph.CURRENCY) }
                orbTotals(deals.filterNot { it.belongsTo(heroId) }).takeIf { it.isNotBlank() }?.let { PropertyRow(ui("auction.history_spent"), it, Glyph.CURRENCY) }
                MutedText(ui("auction.history_note"), style = MaterialTheme.typography.labelMedium)
            }
        }
        if (shown.isEmpty()) item { InfoCard(ui("auction.history_empty"), ui("auction.history_empty_hint")) }
        items(shown, key = { it.id }) { deal ->
            LotRow(s, deal, mark = dealMark(deal, heroId), withSeller = false) { openLot = deal.id }
        }
    }
    shown.firstOrNull { it.id == openLot }?.let { deal ->
        LotSheet(s, deal, action = null, enabled = false, note = dealMark(deal, heroId), onDismiss = { openLot = null })
    }
}

/** «Продано: Имя · 12.10 14:30» or «Куплено у Имя · …». */
internal fun dealMark(deal: AuctionLot, heroId: String): String {
    val at = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.ROOT).format(java.util.Date(deal.soldAt))
    return if (deal.belongsTo(heroId)) {
        ui("auction.history_sold_to", deal.buyerName.ifBlank { "…" }, at)
    } else {
        ui("auction.history_bought_from", sellerName(deal), at)
    }
}

/** The orbs of [deals] summed per orb: «12 × Сфера хаоса, 3 × Сфера соединения». */
internal fun orbTotals(deals: List<AuctionLot>): String = deals.groupBy { it.priceOrb }.entries.joinToString(", ") { (orb, of) -> "${of.sumOf { it.price }} × ${orbTitle(of.first())}" }
