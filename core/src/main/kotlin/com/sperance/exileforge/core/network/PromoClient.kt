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

/** Promo codes are not a [Catalog]: no player ever lists them, so they have no tab of their own. */
private val REDEMPTION_ROUTE = "api/v1/${EntitySource.REDEMPTION.path}"

/** Promo codes, as an administrator keeps them. A player only ever redeems one, through [HeroClient]. */
class PromoClient internal constructor(private val http: Transport) {
    /**
     * Every promo code the server holds.
     *
     * Read whole rather than paged: these are counted in dozens, an administrator wants to see
     * which codes exist rather than to walk them, and the server offers no filter here either.
     */
    suspend fun codes(): List<RedemptionCode> =
        http.all(REDEMPTION_ROUTE).map { WireJson.decodeFromJsonElement(RedemptionCode.serializer(), it) }

    /**
     * Creates one code. The server refuses a blank or duplicate code, an empty reward and a
     * non-positive amount, so none of that is re-checked here — only what a typed model can say.
     */
    suspend fun create(code: RedemptionCode): RedemptionCode {
        require(code.code.isNotBlank()) { ui("api.enter_promo") }
        require(code.treasure.isNotEmpty()) { ui("api.empty_reward") }
        code.treasure.forEach {
            require(it.amount > 0) { ui("api.reward_amount") }
            if (it.kind == RedemptionKind.ITEM || it.kind == RedemptionKind.EQUIPMENT) requireId(it.itemId)
        }
        // Trimmed on the way in exactly as redeem() trims on the way out: a player types the code
        // by hand, and a stored one with a trailing space is a code nobody can ever enter.
        val document = WireJson.encodeToJsonElement(RedemptionCode.serializer(), code.copy(id = "", used = 0, code = code.code.trim())).jsonObject
        val body = JsonObject(document.filterKeys { it != "_id" && it != "used" })
        return http.request("POST", REDEMPTION_ROUTE, body = JsonArray(listOf(body)), authenticated = true)
            .jsonArray.single().let { WireJson.decodeFromJsonElement(RedemptionCode.serializer(), it) }
    }

    suspend fun delete(id: String) {
        requireId(id)
        http.request("DELETE", REDEMPTION_ROUTE, mapOf("id" to id), authenticated = true)
    }
}
