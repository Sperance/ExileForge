package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.currency.CURRENCY_CATEGORY
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierTier
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.progression.ExperienceLevel
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import kotlinx.serialization.json.*

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

    /** The materials the crafts gather (since server 0.37.0), by the category the server seeded. */
    suspend fun materials(): List<com.sperance.exileforge.core.model.crafts.MaterialItem> =
        http.all("api/v1/${Catalog.ITEMS.path}").filter { it.text("category") == com.sperance.exileforge.core.model.crafts.MaterialItem.CATEGORY }
            .map { WireJson.decodeFromJsonElement(com.sperance.exileforge.core.model.crafts.MaterialItem.serializer(), it) }
            .sortedWith(compareBy({ it.subCategory }, { it.price }))

    /** The stat order, the slot order and the price rule the client adds the sheet up by (server 0.41.0). Public, fixed per server. */
    suspend fun statTables(): com.sperance.exileforge.core.character.StatTables =
        WireJson.decodeFromJsonElement(com.sperance.exileforge.core.character.StatTables.serializer(), http.request("GET", "system/stats"))
}
