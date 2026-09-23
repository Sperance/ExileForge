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

/**
 * The catalogue's collections through the generic CRUD: the lists an administrator pages and
 * edits, and the equipment templates every card reads its base from.
 */
class CatalogClient internal constructor(private val http: Transport) : ItemRepository {
    private fun route(catalog: Catalog) = "api/v1/${catalog.path}"

    override suspend fun page(catalog: Catalog, page: Int): ItemPage { requirePage(page); return slice(http.all(route(catalog)), page, CATALOG_PAGE_SIZE) }

    /**
     * A filtered search narrows the collection on the client: the server offers no filter at all.
     * Nothing is recomputed — only fields the server already wrote are compared.
     */
    suspend fun search(catalog: Catalog, page: Int, filter: CatalogFilter): ItemPage {
        requirePage(page)
        return slice(http.all(route(catalog)).filter(filter::matches), page, CATALOG_PAGE_SIZE)
    }

    suspend fun referencePage(source: EntitySource, page: Int, query: String = ""): ItemPage {
        requirePage(page)
        val records = http.all("api/v1/${source.path}")
        val matching = if (query.isBlank()) records else records.filter { document ->
            listOf("name", "login", "code", "category", "subCategory").any { document.text(it).contains(query.trim(), true) }
        }
        return slice(matching, page, REFERENCE_PAGE_SIZE)
    }

    private fun slice(items: List<JsonObject>, page: Int, size: Int): ItemPage {
        val pages = (items.size + size - 1) / size
        return ItemPage(items.drop(page * size).take(size), page, pages, items.size.toLong())
    }

    override suspend fun get(catalog: Catalog, id: String): JsonObject? {
        requireId(id)
        return try { http.request("GET", route(catalog), mapOf("id" to id), authenticated = true).let { if (it == JsonNull) null else it.jsonObject } }
        catch (e: ApiFailure) { if (e.status == 404) null else throw e }
    }

    /** POST takes an array of documents and answers with the created ones, identity included. */
    override suspend fun create(catalog: Catalog, document: JsonObject): JsonObject {
        val allowed = editableFields(catalog) + creationFields(catalog)
        // Naming the offenders: a caller that posts a form's draft raw is the way this goes wrong,
        // and "one of your fields" leaves the reader to guess which of a dozen it was.
        val refused = document.keys.filterNot { it in allowed }
        require(refused.isEmpty()) {
            ui("api.create_fields", refused.joinToString())
        }
        validate(document, catalog)
        return http.request("POST", route(catalog), body = JsonArray(listOf(document)), authenticated = true).jsonArray.single().jsonObject
    }

    /**
     * PUT carries the changed fields only. The server reads the stored document, checks its own
     * version and rejects a racing write with an error — the client never retries one silently.
     */
    override suspend fun update(catalog: Catalog, id: String, changes: JsonObject): JsonObject {
        requireId(id)
        require(changes.isNotEmpty()) { ui("api.no_changes") }
        require(changes.keys.none { it in protectedFields }) { ui("api.service_fields") }
        require(changes.keys.all { it in editableFields(catalog) }) { ui("api.server_owned") }
        if (catalog == Catalog.EQUIPMENT) validateModifierPool(changes)
        return http.request("PUT", route(catalog), mapOf("id" to id), changes, authenticated = true).let {
            if (it == JsonNull) throw ApiFailure(200, null, ui("api.no_updated_item"))
            it.jsonObject
        }
    }

    override suspend fun delete(catalog: Catalog, id: String) { requireId(id); http.request("DELETE", route(catalog), mapOf("id" to id), authenticated = true) }

    /**
     * One equipment template of the chosen rarity and category, drawn at random.
     *
     * Choosing which base to ask for is an input, not a calculation: which modifiers land on the
     * instance, in which tier and with which values, is decided by the server when it is created.
     */
    suspend fun randomTemplate(rarity: String = "", slot: String = "", random: kotlin.random.Random = kotlin.random.Random): JsonObject {
        require(rarity.isBlank() || rarity in com.sperance.exileforge.core.contract.rarities) { ui("api.unknown_rarity") }
        require(slot.isBlank() || slot in com.sperance.exileforge.core.contract.slots) { ui("api.unknown_category") }
        val matching = http.all(route(Catalog.EQUIPMENT)).filter {
            (rarity.isBlank() || it.text("rarity") == rarity) && (slot.isBlank() || it.text("slot") == slot)
        }
        require(matching.isNotEmpty()) { ui("api.no_templates") }
        return matching[random.nextInt(matching.size)]
    }
    suspend fun count(catalog: Catalog): JsonElement = http.request("GET", "${route(catalog)}/count", authenticated = true)

    /**
     * The whole equipment catalogue, read once per session.
     *
     * Since 0.16.0 an instance stores only what it rolled: its base — armour, damage, the
     * requirements — belongs to the template and lives in the catalogue in one copy. So a template
     * is no longer a nicety a card waits for, it is half of what the card says, and the client
     * reads the lot rather than chasing them one at a time.
     */
    suspend fun equipment(): List<JsonObject> =
        http.request("GET", route(Catalog.EQUIPMENT), authenticated = true).jsonArray.map { it.jsonObject }
}
