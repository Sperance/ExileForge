package com.sperance.exileforge.core.model.auction

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.pick
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What is on offer. The two kinds are stored differently, so a lot carries one or the other. */
@Serializable enum class AuctionLotKind { EQUIPMENT, ITEM }

/** Where a lot stands. A closed lot never returns to the showcase; it stays as trading history. */
@Serializable enum class AuctionLotStatus { ACTIVE, SOLD, CANCELLED }

/**
 * One lot of the player auction (collection `AuctionLot`).
 *
 * While a lot is on the showcase the goods live inside it rather than with the seller: an
 * equipment instance leaves `CharacterEquipment` for the lot, and stacking items are debited from
 * the seller's bag. That is what stops the same item being worn or sold twice.
 *
 * [title], [slot], [rarity] and [itemLevel] are a snapshot taken when the lot was listed, which is
 * what lets the server's filter run as a single query. They describe the *instance*, not its
 * template — orbs may have changed its rarity before it was listed.
 */
@Serializable data class AuctionLot(
    @SerialName("_id") val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val kind: AuctionLotKind = AuctionLotKind.EQUIPMENT,
    /** The instance on offer; only an EQUIPMENT lot carries one, and only while it is open. */
    val equipment: EquipmentInstance? = null,
    /** The `items` document on offer; only an ITEM lot carries one. */
    val itemId: String = "",
    val amount: Long = 1,
    /** The orb the price is set in — a reference to a `CURRENCY` document of `items`. */
    val priceOrbId: String = "",
    val price: Long = 0,
    val title: String = "",
    val slot: String? = null,
    val rarity: String? = null,
    val itemLevel: Int = 0,
    val status: AuctionLotStatus = AuctionLotStatus.ACTIVE,
    val buyerId: String? = null,
    val version: Long = 0,
) {
    val onSale: Boolean get() = status == AuctionLotStatus.ACTIVE
    fun belongsTo(characterId: String): Boolean = sellerId == characterId
}

/**
 * The showcase filter, as `GET /api/v1/auctionlot/search` reads it from the query string.
 *
 * Every field is compared against the lot's own snapshot, so this is the one list in the app the
 * server narrows itself. A blank field means "do not filter by it".
 */
@Serializable data class AuctionFilter(
    val kind: String = "",
    val title: String = "",
    val slot: String = "",
    val rarity: String = "",
    val minItemLevel: String = "",
    val maxItemLevel: String = "",
    val priceOrbId: String = "",
    val maxPrice: String = "",
    val sellerId: String = "",
    /** Hide one character's own lots; they cannot be bought anyway, so the showcase drops them. */
    val excludeSellerId: String = "",
) {
    val isEmpty: Boolean get() = query().isEmpty()

    /** Only the fields actually set reach the query string: the server rejects an unknown enum. */
    fun query(): Map<String, String> = buildMap {
        put("kind", kind); put("title", title.trim()); put("slot", slot); put("rarity", rarity)
        put("minItemLevel", minItemLevel.trim()); put("maxItemLevel", maxItemLevel.trim())
        put("priceOrbId", priceOrbId); put("maxPrice", maxPrice.trim())
        put("sellerId", sellerId); put("excludeSellerId", excludeSellerId)
    }.filterValues { it.isNotBlank() }
}

/** One page of the showcase, as the server pages it. */
@Serializable data class AuctionPage(
    val items: List<AuctionLot> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)

/** Title of a lot kind. */
fun lotKindTitle(kind: AuctionLotKind, lang: Lang = uiLanguage): String = when (kind) {
    AuctionLotKind.EQUIPMENT -> lang.pick("Экипировка", "Equipment")
    AuctionLotKind.ITEM -> lang.pick("Предметы", "Items")
}

/** Title of a lot's state. */
fun lotStatusTitle(status: AuctionLotStatus, lang: Lang = uiLanguage): String = when (status) {
    AuctionLotStatus.ACTIVE -> lang.pick("На продаже", "On sale")
    AuctionLotStatus.SOLD -> lang.pick("Продан", "Sold")
    AuctionLotStatus.CANCELLED -> lang.pick("Снят", "Withdrawn")
}
