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

/** The world's reference tables: seeded, fixed for a session, read once and kept in state. */
class WorldClient internal constructor(private val http: Transport) {
    /** Descriptions are a small, shared catalogue: the whole set is read once and kept in state. */
    suspend fun modifiers(): List<ModifierDefinition> =
        WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(ModifierDefinition.serializer()), http.request("GET", "api/v1/modifierdefinition", authenticated = true))
    suspend fun tiers(modifierId: String): List<ModifierTier> {
        requireId(modifierId)
        return WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(ModifierTier.serializer()), http.request("GET", "api/v1/modifiertier/byModifier", mapOf("modifierId" to modifierId), authenticated = true))
    }

    /** The classes the world offers. A character references one; its base is never copied here. */
    suspend fun classes(): List<CharacterClass> =
        WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(CharacterClass.serializer()),
            http.request("GET", "api/v1/${EntitySource.CHARACTER_CLASS.path}", authenticated = true))

    /** The progression table: when a level is reached and how many skill points it hands over. */
    suspend fun levels(): List<ExperienceLevel> =
        WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(ExperienceLevel.serializer()),
            http.request("GET", "api/v1/${EntitySource.EXPERIENCE_LEVEL.path}", authenticated = true))
            .sortedBy { it.level }

    /** The whole shared tree. It is one seeded graph, so it is read once and drawn from memory. */
    suspend fun tree(): List<SkillTreeNode> =
        WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(SkillTreeNode.serializer()),
            http.request("GET", "api/v1/${EntitySource.SKILL_NODE.path}", authenticated = true))

    /**
     * Every currency orb the server serves, read out of the shared `items` collection.
     *
     * An orb is an ordinary stacking item filed under one category, so the catalogue is whatever the
     * server seeded: the client filters by that category and never carries a list of its own.
     */
    suspend fun orbs(): List<CurrencyItem> =
        http.all("api/v1/${Catalog.ITEMS.path}").filter { it.text("category") == CURRENCY_CATEGORY }
            .map { WireJson.decodeFromJsonElement(CurrencyItem.serializer(), it) }
            .sortedBy { it.price }

    suspend fun recipe(id: String): RecipeDocument { requireId(id); return WireJson.decodeFromJsonElement(http.request("GET", "api/v1/recipe", mapOf("id" to id), authenticated = true)) }
}
