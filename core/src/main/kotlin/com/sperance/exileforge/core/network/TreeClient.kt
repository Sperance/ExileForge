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

/** Route root of every tree command, kept in one place so a move is one edit. */
private const val TREE = "api/v1/character/skilltree"

/** One character's passive tree: its state and the three commands that change it. */
class TreeClient internal constructor(private val http: Transport) {
    /**
     * The character's own tree.
     *
     * Since 0.12.0 the taken nodes live inside the character document rather than a collection of
     * their own, so the whole tree travels under `character/skilltree` with the character.
     */
    suspend fun state(characterId: String): SkillTreeState {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("GET", "$TREE/state",
            mapOf("characterId" to characterId), authenticated = true))
    }

    /**
     * Takes, gives back or drops tree nodes; every one of them answers with the whole tree state.
     *
     * Which node may be taken, whether a refund would leave the rest hanging and what a node costs
     * are the server's rules: the client names a node and reports the refusal it gets.
     */
    suspend fun allocate(characterId: String, nodeCode: String): SkillTreeState = node("allocate", characterId, nodeCode)
    suspend fun refund(characterId: String, nodeCode: String): SkillTreeState = node("refund", characterId, nodeCode)
    suspend fun reset(characterId: String): SkillTreeState {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(http.request("POST", "$TREE/reset",
            mapOf("characterId" to characterId), authenticated = true))
    }
    private suspend fun node(operation: String, characterId: String, nodeCode: String): SkillTreeState {
        requireId(characterId)
        require(nodeCode.isNotBlank()) { ui("api.choose_node") }
        return WireJson.decodeFromJsonElement(http.request("POST", "$TREE/$operation",
            mapOf("characterId" to characterId, "nodeCode" to nodeCode), authenticated = true))
    }
}
