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
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.trade.LotKind
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.icons.orbArt
import com.sperance.exileforge.ui.screens.hero.StackInfoSheet
import com.sperance.exileforge.ui.theme.*

/**
 * One lot as a line (variant A): everything a trader decides on without opening it — what it is, every line it
 * rolled, who sells it, and the price opposite. A copy is read through its view as the stash reads it; a stack has
 * no rolls, and its line is the lot's own: the item's icon, its name and how many.
 *
 * Rarity is not written anywhere: it is the frame of the icon and the colour of the name.
 */
@Composable internal fun LotRow(game: GameUi, lot: AuctionLot, mark: String?, withSeller: Boolean = true, onClick: () -> Unit) {
    var orbInfo by remember { mutableStateOf(false) }
    if (orbInfo) StackInfoSheet(game, lot.priceOrb) { orbInfo = false }
    val price: @Composable ColumnScope.() -> Unit = { LotPrice(lot) { orbInfo = true } }
    val seller = listOfNotNull(sellerName(lot).takeIf { withSeller })
    val view = lot.equipment?.let { game.view(it) }
    // The rules' verdict on the template, as the stash marks it: a lot the buyer cannot wear yet says why instead of its facts.
    if (view != null) {
        ItemTradeRow(view, enabled = !game.busy, unmet = lotUnmet(game, lot), extra = seller, mark = mark, onClick = onClick, price = price)
    } else {
        TradeRow(
            lot.title, lotColor(lot), stackFacts(game, lot) + seller, emptyList(), enabled = !game.busy, mark = mark, onClick = onClick,
            icon = { LotIcon(game, lot, Modifier.size(28.dp)) }, price = price,
        )
    }
}

/**
 * The price opposite the name: the count and the orb in its own glass (2.69.0) — the plain glyph for one the client has no art
 * for — and the orb's name under them, since the glasses of the lesser orbs look alike; a tap on a known orb's glass opens it.
 */
@Composable internal fun LotPrice(lot: AuctionLot, onOrb: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(lot.price.toString(), color = Vital, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        Orb.of(lot.priceOrb)?.let { OrbGlyph(it, Modifier.clickable(onClickLabel = orbTitle(lot), onClick = onOrb).padding(2.dp).size(16.dp)) }
            ?: Icon(ForgeGlyphs.Orb, orbTitle(lot), tint = Gold, modifier = Modifier.size(15.dp))
    }
    Text(
        orbTitle(lot),
        color = Muted,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 96.dp),
    )
}

/** How long an own lot still stands (server 1.30.0): days and hours, hours and minutes on its last day; null when it names no end. */
internal fun lotExpiry(lot: AuctionLot): String? {
    val minutes = ((lot.timeLeft() ?: return null) + 59_999) / 60_000
    return if (minutes >= MINUTES_A_DAY) {
        ui("auction.left_days", minutes / MINUTES_A_DAY, minutes % MINUTES_A_DAY / 60)
    } else {
        ui("auction.left_hours", minutes / 60, minutes % 60)
    }
}

internal const val MINUTES_A_DAY = 1_440L

/**
 * The lot's drawing: the server's sprite for its code, the bundled emblem of its kind otherwise — a
 * copy by its template, a stack by the item of the bag it is a stack of.
 */
@Composable internal fun LotIcon(game: GameUi, lot: AuctionLot, modifier: Modifier) {
    val sprite = if (lot.kind == LotKind.EQUIPMENT) equipmentIcon(lot.itemCode) else itemIcon(lot.itemCode)
    if (!SpriteIcon(sprite, lotColor(lot), modifier, halo = lot.kind == LotKind.EQUIPMENT)) ItemEmblem(lotVisualKind(game, lot), lotColor(lot), modifier)
}

internal fun lotVisualKind(game: GameUi, lot: AuctionLot): ItemVisualKind = when (lot.kind) {
    LotKind.EQUIPMENT -> game.index?.template(lot.itemCode)?.let(::itemVisualKind) ?: ItemVisualKind.ITEM
    LotKind.ITEM -> game.index?.item(ItemCode(lot.itemCode))?.let(::bagVisualKind) ?: ItemVisualKind.ITEM
}

/** The rarity's colour for a copy; a stack has none and keeps bone white. */
internal fun lotColor(lot: AuctionLot) = rarityColor(lot.rarity?.name.orEmpty())

internal fun sellerName(lot: AuctionLot): String = lot.sellerName.ifBlank { "…${lot.sellerId.takeLast(6)}" }

/** The requirements the lot's template misses against the hero's sheet; empty for a stack, and for a wearable copy. */
internal fun lotUnmet(game: GameUi, lot: AuctionLot): List<String> = lot.equipment?.let { game.unmetFor(it.template) }.orEmpty()

/** A stack lot's facts: how many, or what kind of goods when it is one. */
internal fun stackFacts(game: GameUi, lot: AuctionLot): List<String> = listOf(if (lot.amount > 1) ui("auction.pieces", lot.amount) else lotKindTitle(lot.kind, game.lang))

/**
 * One lot in full, with the goods drawn as the stash draws them.
 *
 * The copy travels inside the lot, rolls and all, and its template is in the content every hero
 * reads once: looking at a lot costs no request at all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LotSheet(
    game: GameUi,
    lot: AuctionLot,
    action: String?,
    enabled: Boolean,
    note: String?,
    onDismiss: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
    onAction: () -> Unit = {},
) {
    val view = lot.equipment?.let { game.view(it) }
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.9f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (view != null) {
                item {
                    ItemCard(view, enabled = false, detailed = true, actionLabel = ui("auction.lot") + " · ${lot.id.takeLast(6)}")
                }
            }
            item {
                ForgePanel {
                    Engraved(ui("auction.lot"))
                    if (view == null) Text(lot.title, color = Gold, style = MaterialTheme.typography.titleMedium)
                    if (lot.kind == LotKind.ITEM) PropertyRow(ui("auction.amount"), lot.amount.toString(), Glyph.ITEM)
                    // The price is always counted in orbs; the content gives the orb its name.
                    PropertyRow(ui("card.price"), orbPrice(lot), Glyph.CURRENCY)
                    PropertyRow(ui("auction.seller"), sellerName(lot), Glyph.CHARACTER)
                    listedAt(lot.createdAt)?.let { PropertyRow(ui("auction.listed_at"), it, Glyph.LEVEL) }
                    note?.let { MutedText(it, style = MaterialTheme.typography.labelMedium) }
                    extra?.invoke()
                    action?.let { ForgeButton(enabled = enabled, onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(it) } }
                }
            }
        }
    }
}

/**
 * When the lot was listed, in the zone the device is standing in.
 *
 * The server writes the stamp in UTC — its `LocalDateTime.now()` is built from
 * `TimeZone.UTC` — so it carries no zone of its own and this is the one place that gives it one.
 * A stamp that will not parse is simply not shown: a wrong time is worse than no time.
 */
internal fun listedAt(stamp: String): String? {
    if (stamp.isBlank()) return null
    return try {
        java.time.LocalDateTime.parse(stamp)
            .atZone(java.time.ZoneOffset.UTC)
            .withZoneSameInstant(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    } catch (_: Exception) {
        null
    }
}

/** A lot's price, in the orb it was set in; an orb the rules do not know keeps its tail as a name. */
internal fun orbPrice(lot: AuctionLot): String = ui("confirm.amount", lot.price, orbTitle(lot))

internal fun orbTitle(lot: AuctionLot): String = if (Orb.of(lot.priceOrb) != null) itemTitle(lot.priceOrb) else ui("auction.orb_id", lot.priceOrb.takeLast(6))
