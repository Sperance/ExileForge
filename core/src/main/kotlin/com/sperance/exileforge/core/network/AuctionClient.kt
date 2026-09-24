package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.creationFields
import com.sperance.exileforge.core.contract.editableFields
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.protectedFields
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.contract.validateModifierPool
import com.sperance.exileforge.core.display.IconManifest
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.LocaleLanguage
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.model.currency.CURRENCY_CATEGORY
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.hero.*
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierTier
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.progression.ExperienceLevel
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import com.sperance.exileforge.core.model.skilltree.SkillTreeState
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/** Route root of every auction command, kept in one place so a move is one edit. */
private const val AUCTION = "api/v1/auctionlot"

/** The player auction: the showcase, a character's own lots, listing, buying and withdrawing. */
class AuctionClient internal constructor(private val http: Transport) {
    /**
     * The showcase, narrowed and paged by the server.
     *
     * This is the one list the server filters itself: every field the filter compares is a snapshot
     * the lot carries, so the whole search is a single query. Nothing is narrowed here afterwards.
     */
    suspend fun search(characterId: String, filter: AuctionFilter, page: Int): AuctionPage {
        requireId(characterId); requirePage(page)
        return WireJson.decodeFromJsonElement(http.request("GET", "$AUCTION/search",
            mapOf("characterId" to characterId, "page" to page.toString(), "size" to AUCTION_PAGE_SIZE.toString()) + filter.query(),
            authenticated = true))
    }

    /** Everything the character ever listed, open and closed alike — the lots are their history. */
    /** The hero's lot places (0.34.0): how many are taken and what one more costs. */
    suspend fun slots(characterId: String): AuctionSlots {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("GET", "$AUCTION/slots", mapOf("characterId" to characterId), authenticated = true))
    }

    /** Buys one more lot place for gold. Never retried. */
    suspend fun buySlot(characterId: String): AuctionSlots {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("POST", "$AUCTION/slots", mapOf("characterId" to characterId), authenticated = true))
    }

    suspend fun myLots(characterId: String): List<AuctionLot> {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(AuctionLot.serializer()),
            http.request("GET", "$AUCTION/my", mapOf("characterId" to characterId), authenticated = true))
    }

    /**
     * Lists an item. The price is always counted in orbs, so [priceOrbId] must be a `CURRENCY`
     * document — the server refuses anything else rather than inventing a conversion.
     *
     * An equipment instance has to be off the character first: while it is listed the goods live
     * in the lot, and a worn item cannot be in two places.
     */
    suspend fun sellEquipment(characterId: String, inventoryId: String, priceOrbId: String, price: Long): AuctionLot {
        requireId(inventoryId)
        return sell("equipment", characterId, priceOrbId, price, mapOf("inventoryId" to inventoryId))
    }
    suspend fun sellItem(characterId: String, itemId: String, amount: Long, priceOrbId: String, price: Long): AuctionLot {
        requireId(itemId)
        require(amount > 0) { ui("api.amount_positive") }
        return sell("item", characterId, priceOrbId, price, mapOf("itemId" to itemId, "amount" to amount.toString()))
    }
    private suspend fun sell(what: String, characterId: String, priceOrbId: String, price: Long, extra: Map<String, String>): AuctionLot {
        requireId(characterId); requireId(priceOrbId)
        require(price > 0) { ui("api.price_positive") }
        return WireJson.decodeFromJsonElement(http.request("POST", "$AUCTION/sell/$what",
            extra + mapOf("characterId" to characterId, "priceOrbId" to priceOrbId, "price" to price.toString()),
            authenticated = true))
    }

    /**
     * Buys a lot, or takes one back off the showcase.
     *
     * Payment, delivery and closing the lot are one server transaction, so a buyer short of orbs
     * loses neither the orbs nor the goods. The client never checks the balance itself.
     */
    suspend fun buy(characterId: String, lotId: String): AuctionLot = lot("buy", characterId, lotId)
    suspend fun cancel(characterId: String, lotId: String): AuctionLot = lot("cancel", characterId, lotId)
    private suspend fun lot(operation: String, characterId: String, lotId: String): AuctionLot {
        requireId(characterId); requireId(lotId)
        return WireJson.decodeFromJsonElement(http.request("POST", "$AUCTION/$operation",
            mapOf("characterId" to characterId, "lotId" to lotId), authenticated = true))
    }
}
