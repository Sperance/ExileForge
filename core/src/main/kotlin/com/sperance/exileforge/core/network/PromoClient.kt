package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.core.model.command.named
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

private const val REDEMPTION = "api/v1/redemptioncodes"

/** Promo codes, as an administrator keeps them. A player only ever redeems one, through [HeroClient.redeem]. */
class PromoClient internal constructor(private val http: Transport) {
    /** Every promo code the server holds — counted in dozens, read whole. */
    suspend fun codes(): List<RedemptionCode> = http.all(REDEMPTION).map { WireJson.decodeFromJsonElement(RedemptionCode.serializer(), it) }

    suspend fun create(code: RedemptionCode): RedemptionCode {
        require(code.code.isNotBlank()) { ui("api.enter_promo") }
        require(code.treasure.isNotEmpty()) { ui("api.empty_reward") }
        code.treasure.forEach {
            require(it.quantity > 0) { ui("api.reward_amount") }
            it.named?.let { named -> require(named.isNotBlank()) { ui("api.choose_item") } }
        }
        val document = WireJson.encodeToJsonElement(RedemptionCode.serializer(), code.copy(id = "", used = 0, code = code.code.trim())).jsonObject
        val body = JsonObject(document.filterKeys { it != "_id" && it != "used" })
        return http.request("POST", REDEMPTION, body = JsonArray(listOf(body)), authenticated = true)
            .jsonArray.single().let { WireJson.decodeFromJsonElement(RedemptionCode.serializer(), it) }
    }

    suspend fun delete(id: String) {
        requireId(id)
        http.request("DELETE", REDEMPTION, mapOf("id" to id), authenticated = true)
    }
}
