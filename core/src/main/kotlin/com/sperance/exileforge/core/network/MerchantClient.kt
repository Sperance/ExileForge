package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.model.trade.MerchantPurchase
import com.sperance.exileforge.core.model.trade.MerchantStock

/**
 * The merchant (since server 0.34.0): a shelf of the server's rolls, sold for gold. What lies on it
 * and what it costs is the server's; the client names an offer and prints what came back.
 */
class MerchantClient internal constructor(private val http: Transport) {
    suspend fun stock(characterId: String): MerchantStock =
        http.get("api/v1/character/merchant", heroQuery(characterId))

    /** Never retried: a repeat would be refused at best and paid twice at worst. */
    suspend fun buy(characterId: String, offerId: String): MerchantPurchase =
        http.post("api/v1/character/merchant/buy", heroQuery(characterId, "offerId" to offerId))
}
