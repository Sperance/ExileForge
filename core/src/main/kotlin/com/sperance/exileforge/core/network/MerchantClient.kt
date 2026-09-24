package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.model.trade.MerchantPurchase
import com.sperance.exileforge.core.model.trade.MerchantStock
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * The merchant (since server 0.34.0): a shelf of the server's rolls, sold for gold. What lies on it
 * and what it costs is the server's; the client names an offer and prints what came back.
 */
class MerchantClient internal constructor(private val http: Transport) {
    suspend fun stock(characterId: String): MerchantStock {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("GET", "api/v1/character/merchant", mapOf("characterId" to characterId), authenticated = true))
    }

    /** Never retried: a repeat would be refused at best and paid twice at worst. */
    suspend fun buy(characterId: String, offerId: String): MerchantPurchase {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("POST", "api/v1/character/merchant/buy",
            mapOf("characterId" to characterId, "offerId" to offerId), authenticated = true))
    }
}
