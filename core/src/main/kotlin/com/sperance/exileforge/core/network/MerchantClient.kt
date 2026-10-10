package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.model.trade.MerchantOrbPurchase
import com.sperance.exileforge.core.model.trade.MerchantPurchase
import com.sperance.exileforge.core.model.trade.MerchantStock

/** The merchant: a shelf of the server's rolls, sold for gold. */
class MerchantClient internal constructor(private val http: Transport) {
    /** Never retried: a repeat would be refused at best and paid twice at worst. */
    suspend fun buy(heroId: String, offerId: String): MerchantPurchase = http.post("api/v1/hero/merchant/buy", heroQuery(heroId, "offerId" to offerId))

    /** One orb off the shelf, never retried either. */
    suspend fun buyOrb(heroId: String, code: String): MerchantOrbPurchase = http.post("api/v1/hero/merchant/buyOrb", heroQuery(heroId, "code" to code))

    /**
     * Трофеи босса [boss] - на его уникалку (4.6.0, Предначертание «Трофейщик»): копия ложится в тайник. Отказы - `FT_003`
     * (трофеев мало), `FT_004` (уникалки босса нет на уровне героя), `FT_005` (дар трофеи не меняет). Не повторяется: уникалка катится.
     */
    suspend fun trophy(heroId: String, boss: String): MerchantPurchase = http.post("api/v1/hero/merchant/trophy", heroQuery(heroId, "boss" to boss))
}
