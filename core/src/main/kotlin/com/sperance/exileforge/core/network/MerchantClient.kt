package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.model.trade.MerchantPurchase
import com.sperance.exileforge.core.model.trade.MerchantStock

/** The merchant: a shelf of the server's rolls, sold for gold. */
class MerchantClient internal constructor(private val http: Transport) {
    suspend fun stock(heroId: String): MerchantStock = http.get("api/v1/hero/merchant", heroQuery(heroId))

    /** Never retried: a repeat would be refused at best and paid twice at worst. */
    suspend fun buy(heroId: String, offerId: String): MerchantPurchase = http.post("api/v1/hero/merchant/buy", heroQuery(heroId, "offerId" to offerId))
}
