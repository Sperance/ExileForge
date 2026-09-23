package com.sperance.exileforge.core.model.auction

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.locOr
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
 * [itemCode], [slot], [rarity] and [itemLevel] are a snapshot taken when the lot was listed, which
 * is what lets the server's filter run as a single query. They describe the *instance*, not its
 * template — orbs may have changed its rarity before it was listed.
 *
 * The lot stores no name: since 0.14.0 the text lives in the locale bundle under the item's code,
 * and which section that code belongs to depends on [kind].
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
    val itemCode: String = "",
    val slot: String? = null,
    val rarity: String? = null,
    val itemLevel: Int = 0,
    /**
     * When the lot was listed, as the server wrote it: an ISO date and time in **UTC**.
     *
     * The server's `LocalDateTime.now()` is `Clock.System.now().toLocalDateTime(TimeZone.UTC)`,
     * so this carries no zone of its own and the client is free to show it in the device's.
     */
    val createdAt: String = "",
    val status: AuctionLotStatus = AuctionLotStatus.ACTIVE,
    val buyerId: String? = null,
    val version: Long = 0,
) {
    val onSale: Boolean get() = status == AuctionLotStatus.ACTIVE
    fun belongsTo(characterId: String): Boolean = sellerId == characterId
    /** Which section of the dictionary this lot's name lives in: equipment, or an `items` row. */
    private val nameKey: String get() =
        if (kind == AuctionLotKind.EQUIPMENT) LocaleKey.equipmentName(itemCode) else LocaleKey.itemName(itemCode)
    /** An equipment lot names a template, a stack lot names an `items` document. */
    val title: String get() = locOr(nameKey, itemCode)
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
    /**
     * Which dictionary the server resolves [title] against.
     *
     * Lots carry codes, not names, so a search by text is turned into a search by code on the
     * server side — and that needs to know the language the player typed in.
     */
    val lang: String = "",
) {
    /** `lang` alone is not a filter: it only says how to read the text, so it does not count. */
    val isEmpty: Boolean get() = query().keys.none { it != "lang" }

    /** Only the fields actually set reach the query string: the server rejects an unknown enum. */
    fun query(): Map<String, String> = buildMap {
        put("kind", kind); put("title", title.trim()); put("slot", slot); put("rarity", rarity)
        put("minItemLevel", minItemLevel.trim()); put("maxItemLevel", maxItemLevel.trim())
        put("priceOrbId", priceOrbId); put("maxPrice", maxPrice.trim())
        put("sellerId", sellerId); put("excludeSellerId", excludeSellerId)
        // The language only matters when there is text to resolve.
        if (title.isNotBlank()) put("lang", lang)
    }.filterValues { it.isNotBlank() }

    /**
     * The filters set besides the name, in the order the showcase shows them as chips. The name
     * has its own field and the seller's own lots are the market's switch, so neither is here.
     */
    fun active(): List<FilterField> = FilterField.entries.filter { value(it).isNotBlank() }

    fun value(field: FilterField): String = when (field) {
        FilterField.KIND -> kind; FilterField.SLOT -> slot; FilterField.RARITY -> rarity
        FilterField.MIN_LEVEL -> minItemLevel.trim(); FilterField.MAX_LEVEL -> maxItemLevel.trim()
        FilterField.ORB -> priceOrbId; FilterField.MAX_PRICE -> maxPrice.trim(); FilterField.SELLER -> sellerId
    }

    /** The same filter with one field dropped: what a chip's cross asks for. */
    fun without(field: FilterField): AuctionFilter = when (field) {
        FilterField.KIND -> copy(kind = ""); FilterField.SLOT -> copy(slot = ""); FilterField.RARITY -> copy(rarity = "")
        FilterField.MIN_LEVEL -> copy(minItemLevel = ""); FilterField.MAX_LEVEL -> copy(maxItemLevel = "")
        FilterField.ORB -> copy(priceOrbId = ""); FilterField.MAX_PRICE -> copy(maxPrice = ""); FilterField.SELLER -> copy(sellerId = "")
    }

    /** Every filter off, the name kept: "reset" in the sheet leaves what was typed in the field. */
    fun cleared(): AuctionFilter = AuctionFilter(title = title, lang = lang)
}

/** A filter of the showcase that is not the name: each one a chip when set. */
enum class FilterField { KIND, SLOT, RARITY, MIN_LEVEL, MAX_LEVEL, ORB, MAX_PRICE, SELLER }

/** One page of the showcase, as the server pages it. */
@Serializable data class AuctionPage(
    val items: List<AuctionLot> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
)

/** Title of a lot kind. */
fun lotKindTitle(kind: AuctionLotKind, lang: Lang = uiLanguage): String = ui(lang, "enum.lot.${kind.name}")

/** Title of a lot's state. */
fun lotStatusTitle(status: AuctionLotStatus, lang: Lang = uiLanguage): String = ui(lang, "enum.lot_status.${status.name}")
