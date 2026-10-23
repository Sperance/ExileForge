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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
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
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.sheet.itemSeals
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.unmetNeeds
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.trade.LotKind
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.icons.orbArt
import com.sperance.exileforge.ui.screens.hero.rememberWearChoice
import com.sperance.exileforge.ui.screens.hero.wearTotals
import com.sperance.exileforge.ui.theme.*

/**
 * One lot as a line (variant A): everything a trader decides on without opening it — what it is, every line it
 * rolled, who sells it, and the price opposite. A copy is read through its view as the stash reads it; a stack has
 * no rolls, and its line is the lot's own: the item's icon, its name and how many.
 *
 * Rarity is not written anywhere: it is the frame of the icon and the colour of the name.
 *
 * Вердикт правил о шаблоне, как в тайнике: лот, который не надеть, говорит почему; свойства легенд свёрнуты, пока игрок не велел их
 * раскрывать.
 */
@Composable internal fun LotRow(game: GameUi, lot: AuctionLot, marks: List<TradeMark>, withSeller: Boolean = true, onClick: () -> Unit) {
    val inspect = rememberInspect()
    val price: @Composable RowScope.() -> Unit = { LotPrice(lot) { inspect(Inspect.Stack(lot.priceOrb)) } }
    val seller = listOfNotNull(sellerName(lot).takeIf { withSeller })
    val view = lot.equipment?.let { game.view(it) }
    if (view != null) {
        val lines = LineFold.legend(view, LocalSettings.current.legendsOpen)
        ItemTradeRow(view, enabled = !game.busy, unmet = lotUnmet(game, lot), extra = seller, marks = marks, lines = lines, onClick = onClick, price = price)
    } else {
        TradeRow(
            lot.title,
            lotColor(lot),
            stackFacts(game.lang, lot) + seller,
            enabled = !game.busy,
            marks = marks,
            onClick = onClick,
            icon = { LotIcon(game, lot, 28.dp) },
            price = price,
        )
    }
}

/**
 * Цена лота в ярлыке «Ценника»: сфера в своём стекле - простой знак, если рисунка нет, - число и имя сферы,
 * ведь стёкла младших сфер похожи; нажатие на известную сферу открывает её.
 */
@Composable internal fun RowScope.LotPrice(lot: AuctionLot, onOrb: () -> Unit) {
    Orb.of(lot.priceOrb)?.let { OrbGlyph(it, Modifier.clickable(onClickLabel = orbTitle(lot), onClick = onOrb).size(15.dp)) }
        ?: Icon(ForgeGlyphs.Orb, orbTitle(lot), tint = Gold, modifier = Modifier.size(14.dp))
    Text(lot.price.toString(), color = GoldBright, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    Text(
        orbTitle(lot),
        color = Color(0xFF8FA0AB),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 96.dp),
    )
}

/** Куда ушёл истёкший свой лот: «Вернулся в тайник», «В почте», «Возвращается»; null - лот ещё на витрине. */
internal fun lotReturn(lot: AuctionLot): String? {
    lot.returned?.takeIf { lot.expiredBack }?.let { return ui("auction.returned.${it.name}") }
    return ui("auction.returning").takeIf { lot.returning }
}

/**
 * Сколько ещё стоит лот к «сейчас» [now] (часы сервера): дни и часы, в последний день - часы и минуты; null - срока нет или лот ушёл с
 * витрины ([lotReturn]).
 */
internal fun lotLeft(lot: AuctionLot, now: Long): String? {
    if (lotReturn(lot) != null) return null
    val minutes = ((lot.timeLeft(now) ?: return null) + 59_999) / 60_000
    return if (minutes >= MINUTES_A_DAY) {
        ui("auction.left_days", minutes / MINUTES_A_DAY, minutes % MINUTES_A_DAY / 60)
    } else {
        ui("auction.left_hours", minutes / 60, minutes % 60)
    }
}

internal const val MINUTES_A_DAY = 1_440L

/** Такт живых сроков рынка ([lotLeft], полоса времени торгов): подпись - с точностью до минуты. */
internal const val LOT_TICK_MS = 30_000L

/** Значок лота стороной [size]: стопка - её ячейкой ([StackCell]), копия - рисунком ([LotIcon]). */
@Composable internal fun LotIcon(game: GameUi, lot: AuctionLot, size: Dp) {
    if (lot.kind == LotKind.ITEM) StackCell(game.index, lot.itemCode, lot.amount, size = size) else LotIcon(lot, lotVisualKind(game, lot), Modifier.size(size))
}

/** Рисунок лота: спрайт сервера по коду, иначе эмблема вида [visual] ([lotVisualKind]). */
@Composable internal fun LotIcon(lot: AuctionLot, visual: ItemVisualKind, modifier: Modifier) {
    val sprite = when (lot.kind) {
        LotKind.EQUIPMENT -> equipmentIcon(lot.itemCode)
        LotKind.ITEM -> itemIcon(lot.itemCode)
    }
    if (!SpriteIcon(sprite, lotColor(lot), modifier, halo = lot.kind == LotKind.EQUIPMENT)) ItemEmblem(visual, lotColor(lot), modifier)
}

internal fun lotVisualKind(game: GameUi, lot: AuctionLot): ItemVisualKind = when (lot.kind) {
    LotKind.EQUIPMENT -> game.index?.template(lot.itemCode)?.let(::itemVisualKind) ?: ItemVisualKind.ITEM
    LotKind.ITEM -> game.index?.item(ItemCode(lot.itemCode))?.let(::bagVisualKind) ?: ItemVisualKind.ITEM
}

/** The rarity's colour for a copy; a stack has none and keeps bone white. */
internal fun lotColor(lot: AuctionLot) = rarityColor(lot.rarity?.name.orEmpty())

internal fun sellerName(lot: AuctionLot): String = lot.sellerName.ifBlank { "…${lot.sellerId.takeLast(6)}" }

/** The requirements the lot's template misses against the hero's sheet; empty for a stack, and for a wearable copy. */
internal fun lotUnmet(game: GameUi, lot: AuctionLot): List<String> = lot.equipment?.let { game.unmetFor(it) }.orEmpty()

/** A stack lot's facts: how many, or what kind of goods when it is one. */
internal fun stackFacts(lang: Lang, lot: AuctionLot): List<String> = listOf(if (lot.amount > 1) ui("auction.pieces", lot.amount) else lotKindTitle(lot.kind, lang))

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

internal fun orbTitle(lot: AuctionLot): String = if (Orb.of(lot.priceOrb) != null) itemTitle(lot.priceOrb) else ui("auction.orb_id", lot.priceOrb.takeLast(6))
