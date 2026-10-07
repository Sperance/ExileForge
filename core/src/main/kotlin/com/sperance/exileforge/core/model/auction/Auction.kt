package com.sperance.exileforge.core.model.auction

import com.sperance.exileforge.core.display.mapItemTitle
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.trade.Cost
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.rules.trade.LotGoods
import com.sperance.exileforge.rules.trade.LotKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Where a lot stands. A closed lot never returns to the showcase; it stays as trading history. [EXPIRED] (server 1.30.0): its time ran out, the goods went back to the seller. */
@Serializable enum class LotStatus { ACTIVE, SOLD, CANCELLED, EXPIRED }

/**
 * One lot of the player auction. While it is on the showcase the goods live inside it: a copy leaves the
 * seller's document, a stack is debited from the bag. The price is in orbs — [priceOrb] is the item code
 * of a currency. [itemCode], [slot], [rarity] and [itemLevel] are a snapshot taken at listing.
 */
@Serializable data class AuctionLot(
    @SerialName("_id") val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    /** What is on offer (server 1.75.0): a copy of an item or a stack of the bag, each with its own fields. */
    val goods: LotGoods? = null,
    /** The kind of [goods] as a snapshot the showcase filters by. */
    val kind: LotKind = LotKind.EQUIPMENT,
    val priceOrb: String = "",
    val price: Long = 0,
    /** The buyer's fee in gold, on top of the orbs (3.15.0, server 1.13.0): it burns, the seller gets none. */
    val fee: Long = 0,
    val itemCode: String = "",
    val slot: Slot? = null,
    val rarity: Rarity? = null,
    val itemLevel: Int = 0,
    val status: LotStatus = LotStatus.ACTIVE,
    val buyerId: String? = null,
    /** When the lot was listed, as the server wrote it: an ISO date and time in UTC. */
    val createdAt: String = "",
    val closedAt: String? = null,
    /** When the lot leaves the showcase (server 1.30.0), epoch millis UTC; 0 on a lot listed before lots expired. */
    val expiresAt: Long = 0,
    /** The deal (3.73.0, server 1.69.0): who bought, when (epoch millis UTC) and the copy as it was sold — the history's. */
    val buyerName: String = "",
    val soldAt: Long = 0,
    val sold: ItemInstance? = null,
    val version: Long = 0,
) {
    /** The deal as the history shows it: the copy that was sold in place of the one the lot no longer holds. */
    val deal: AuctionLot get() = if (sold != null && equipment == null) copy(goods = LotGoods.Equipment(sold)) else this

    /** Цена покупки (3.89.0): сферы цены и сбор золотом. */
    val cost: Cost get() = Cost.item(priceOrb, price) + Cost.gold(fee)

    /** The copy on offer, or null for a stack. */
    val equipment: ItemInstance? get() = (goods as? LotGoods.Equipment)?.item

    /** The pet on offer (3.91.0, server 1.81.0), or null for another kind. */
    val pet: Pet? get() = (goods as? LotGoods.Beast)?.pet

    /** How many units go for the one price: one for an item. */
    val amount: Long get() = goods?.amount ?: 1
    val onSale: Boolean get() = status == LotStatus.ACTIVE && (expiresAt == 0L || System.currentTimeMillis() < expiresAt)

    /** Milliseconds the lot still stands, or null when it names no end. */
    fun timeLeft(now: Long = System.currentTimeMillis()): Long? = expiresAt.takeIf { it > 0 }?.let { (it - now).coerceAtLeast(0) }

    /** Whether its author may extend it now (3.79.0, server 1.74.0): on sale, with no more than [window] ms left. */
    fun extendable(window: Long, now: Long = System.currentTimeMillis()): Boolean = onSale && timeLeft(now)?.let { it in 1..window } == true
    fun belongsTo(heroId: String): Boolean = sellerId == heroId

    /** An equipment lot names a template, a stack lot names an item of the bag. */
    val title: String get() = equipment?.mapZone?.takeIf { it.isNotEmpty() }?.let(::mapItemTitle)
        ?: when (kind) {
            LotKind.EQUIPMENT -> locOr(LocaleKey.equipmentName(itemCode), itemCode)
            LotKind.PET -> locOr("pet.$itemCode", itemCode)
            LotKind.ITEM -> locOr(LocaleKey.itemName(itemCode), itemCode)
        }
}

/** The showcase filter, as `GET /api/v1/auctionlot/search` reads it from the query string. A blank field means "do not filter by it". */
@Serializable data class AuctionFilter(
    val kind: String = "",
    val title: String = "",
    val slot: String = "",
    val rarity: String = "",
    val minItemLevel: String = "",
    val maxItemLevel: String = "",
    val priceOrb: String = "",
    val maxPrice: String = "",
    val sellerId: String = "",
    /** Hide one hero's own lots; they cannot be bought anyway, so the showcase drops them. */
    val excludeSellerId: String = "",
    /** Which dictionary the server resolves [title] against: lots carry codes, so a search by text is a search by code. */
    val lang: String = "",
) {
    val isEmpty: Boolean get() = query().keys.none { it != "lang" }

    fun query(): Map<String, String> = buildMap {
        put("kind", kind)
        put("title", title.trim())
        put("slot", slot)
        put("rarity", rarity)
        put("minItemLevel", minItemLevel.trim())
        put("maxItemLevel", maxItemLevel.trim())
        put("priceOrb", priceOrb)
        put("maxPrice", maxPrice.trim())
        put("sellerId", sellerId)
        put("excludeSellerId", excludeSellerId)
        if (title.isNotBlank()) put("lang", lang)
    }.filterValues { it.isNotBlank() }

    /** The filters set besides the name, in the order the showcase shows them as chips. */
    fun active(): List<FilterField> = FilterField.entries.filter { value(it).isNotBlank() }

    fun value(field: FilterField): String = when (field) {
        FilterField.KIND -> kind
        FilterField.SLOT -> slot
        FilterField.RARITY -> rarity
        FilterField.MIN_LEVEL -> minItemLevel.trim()
        FilterField.MAX_LEVEL -> maxItemLevel.trim()
        FilterField.ORB -> priceOrb
        FilterField.MAX_PRICE -> maxPrice.trim()
        FilterField.SELLER -> sellerId
    }

    fun without(field: FilterField): AuctionFilter = when (field) {
        FilterField.KIND -> copy(kind = "")
        FilterField.SLOT -> copy(slot = "")
        FilterField.RARITY -> copy(rarity = "")
        FilterField.MIN_LEVEL -> copy(minItemLevel = "")
        FilterField.MAX_LEVEL -> copy(maxItemLevel = "")
        FilterField.ORB -> copy(priceOrb = "")
        FilterField.MAX_PRICE -> copy(maxPrice = "")
        FilterField.SELLER -> copy(sellerId = "")
    }

    /** Every filter off, the name kept. */
    fun cleared(): AuctionFilter = AuctionFilter(title = title, lang = lang)
}

/** A filter of the showcase that is not the name: each one a chip when set. */
enum class FilterField { KIND, SLOT, RARITY, MIN_LEVEL, MAX_LEVEL, ORB, MAX_PRICE, SELLER }

/**
 * The showcase by cursor (server 1.62.0): [next] opens the following page, `null` — nothing further. [page] and [cursors] are the
 * client's own: the cursor each page read began at (the first — ""). «Показать ещё» adds each next page under the lots shown.
 */
@Serializable data class AuctionPage(
    val items: List<AuctionLot> = emptyList(),
    val next: String? = null,
    val totalItems: Long = 0,
    val page: Int = 0,
    val cursors: List<String> = listOf(""),
) {
    /** The cursors to the page after the last one read, or null when there is none. */
    val nextTrail: List<String>? get() = next?.let { cursors + it }

    /** [more], the page after this one, under the lots already shown — a lot listed twice across the seam kept once. */
    fun followedBy(more: AuctionPage): AuctionPage {
        val shown = items.mapTo(HashSet()) { it.id }
        return more.copy(items = items + more.items.filterNot { it.id in shown })
    }

    /** The showcase without a lot that left it — bought — the count down by one. */
    fun without(lotId: String): AuctionPage = if (items.none { it.id == lotId }) this else copy(items = items.filterNot { it.id == lotId }, totalItems = (totalItems - 1).coerceAtLeast(0))
}

fun lotKindTitle(kind: LotKind, lang: Lang = uiLanguage): String = ui(lang, "enum.lot.${kind.name}")

/** The hero's lot places: [used] of [limit] taken — the same for every hero (3.47.0). */
@Serializable data class AuctionSlots(val used: Int = 0, val limit: Int = 12) {
    val full: Boolean get() = used >= limit
}

/**
 * Свой аукцион героя (3.94.1, сервер 1.81.15): [lots] - его лоты, [slots] - места, [history] - сделки; [lot] - лот только что
 * прошедшей команды (выставлен, снят, продлён).
 */
@Serializable data class AuctionMine(val lots: List<AuctionLot> = emptyList(), val slots: AuctionSlots = AuctionSlots(), val history: List<AuctionLot> = emptyList(), val lot: AuctionLot? = null)

/** The price hint (3.79.0, server 1.74.0): the median [price] a piece in [priceOrb] over [sales] recent deals of the like. */
@kotlinx.serialization.Serializable data class PriceHint(val priceOrb: String = "", val price: Long = 0, val sales: Int = 0)
