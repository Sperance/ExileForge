package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.model.trade.MerchantPurchase
import com.sperance.exileforge.core.model.trade.MerchantStock
import com.sperance.exileforge.rules.content.WearRequest

/** The merchant: a shelf of the server's rolls, sold for gold. */
class MerchantClient internal constructor(private val http: Transport) {
    /** Repeated only under its key ([SafeRetry]): the server answers a repeat with the first answer, never paying twice. */
    suspend fun buy(heroId: String, offerId: String): MerchantPurchase = http.post("api/v1/hero/merchant/buy", heroQuery(heroId, "offerId" to offerId))

    /**
     * Покупка с надеванием одной записью сервера: на место [wear] из нескольких (кольцо, фляга; без места - по правилам), снятое - в
     * тайник; надеть нельзя - отказ, золото не списано (`CH_044`, `CH_045`, `CH_048`, требования). Ответ - надетая копия. Повтор - только
     * под своим ключом ([SafeRetry]).
     */
    suspend fun buyAndWear(heroId: String, offerId: String, wear: WearRequest): MerchantPurchase = http.post("api/v1/hero/merchant/buy", heroQuery(heroId, "offerId" to offerId, "wear" to "true", "slot" to wear.slot?.name))

    /**
     * Трофеи босса [boss] - на его уникалку (Предначертание «Трофейщик»): копия ложится в тайник. Отказы - `FT_003`
     * (трофеев мало), `FT_004` (уникалки босса нет на уровне героя), `FT_005` (дар трофеи не меняет). Повтор - только под своим ключом ([SafeRetry]): уникалка катится один раз.
     */
    suspend fun trophy(heroId: String, boss: String): MerchantPurchase = http.post("api/v1/hero/merchant/trophy", heroQuery(heroId, "boss" to boss))
}
