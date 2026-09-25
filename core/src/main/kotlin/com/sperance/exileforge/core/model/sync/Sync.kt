package com.sperance.exileforge.core.model.sync

import com.sperance.exileforge.core.character.StatTables
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.IconManifest
import com.sperance.exileforge.core.display.PortraitManifest
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.ApiCapabilities
import com.sperance.exileforge.core.model.command.RouteInfo
import com.sperance.exileforge.core.model.crafts.MaterialItem
import com.sperance.exileforge.core.model.currency.CURRENCY_CATEGORY
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.hero.CharacterItem
import com.sperance.exileforge.core.model.hero.CharacterSummary
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import com.sperance.exileforge.core.model.modifier.BenchRecipe
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.progression.ExperienceLevel
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import com.sperance.exileforge.core.model.skilltree.SkillTreeState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** The API revision this client is written against (server 0.52.0: the tree's masteries and attribute nodes). */
const val API_REVISION = 7

@Serializable data class WorldManifest(val hash: String = "", val file: String = "world.json")

/**
 * `static/index.json` (server 0.48.0): everything the client checks at start, in one request —
 * the routes it decides features by and the fingerprint of every static file it keeps.
 */
@Serializable data class StaticManifest(
    val version: String = "",
    val revision: Int = 0,
    val routes: List<RouteInfo> = emptyList(),
    val locale: LocaleManifest = LocaleManifest(),
    val icons: IconManifest = IconManifest(),
    val portraits: PortraitManifest = PortraitManifest(),
    val world: WorldManifest = WorldManifest(),
) {
    val capabilities: ApiCapabilities get() = ApiCapabilities.of(routes)

    /** A server older than [API_REVISION] has neither the world file nor the hero snapshot. */
    fun requireWorkbench() {
        require(revision >= API_REVISION) { ui("cmd.stale_revision", revision, API_REVISION) }
        capabilities.requireWorkbench()
    }
}

/**
 * Every reference table of the world, parsed from `world/world.json`.
 *
 * The file is kept on the device and replaced only when the manifest's hash moves, so a warm start
 * reads none of it from the server. Orbs and materials are the item collection filtered by the
 * category the server seeded, as they were when each was a request of its own.
 */
class WorldTables(
    val hash: String,
    val modifiers: List<ModifierDefinition>,
    val classes: List<CharacterClass>,
    val levels: List<ExperienceLevel>,
    val tree: List<SkillTreeNode>,
    val equipment: Map<String, JsonObject>,
    val orbs: List<CurrencyItem>,
    val materials: List<MaterialItem>,
    val stats: StatTables,
) {
    companion object {
        fun parse(hash: String, document: String): WorldTables {
            val root = WireJson.parseToJsonElement(document).jsonObject
            fun rows(key: String) = root[key]?.jsonArray.orEmpty().map { it.jsonObject }
            val items = rows("items")
            return WorldTables(
                hash,
                rows("modifiers").map { WireJson.decodeFromJsonElement(it) },
                rows("classes").map { WireJson.decodeFromJsonElement(it) },
                rows("levels").map { WireJson.decodeFromJsonElement<ExperienceLevel>(it) }.sortedBy { it.level },
                rows("tree").map { WireJson.decodeFromJsonElement(it) },
                rows("equipment").associateBy { it.entityId },
                items.filter { it.text("category") == CURRENCY_CATEGORY }.map { WireJson.decodeFromJsonElement<CurrencyItem>(it) }.sortedBy { it.price },
                items.filter { it.text("category") == MaterialItem.CATEGORY }.map { WireJson.decodeFromJsonElement<MaterialItem>(it) }
                    .sortedWith(compareBy({ it.subCategory }, { it.price })),
                root["stats"]?.let { WireJson.decodeFromJsonElement<StatTables>(it) } ?: StatTables(),
            )
        }
    }
}

/**
 * One part of the hero and its version. Whole when [base] is null: the client replaces it and
 * merges nothing. The inventory may come as a patch (server 0.49.0): [base] names the version it
 * lies over and [data] is an [InventoryPatch]; a patch over a version not held drops the part.
 */
@Serializable data class HeroPart(val version: String, val data: JsonElement, val base: String? = null)

/** What one command did to the inventory: the items that appeared or changed, and the ids that left. */
@Serializable data class InventoryPatch(val changed: List<JsonObject> = emptyList(), val removed: List<String> = emptyList())

/** The hero as the server answers it: only the parts whose fingerprints the client did not hold. */
@Serializable data class HeroSnapshot(val version: String, val parts: Map<String, HeroPart> = emptyMap())

/**
 * The parts of one character the client holds.
 *
 * Their fingerprints travel back in `X-Hero-Parts` with every command and every read, so the
 * server sends only what moved; [version] is the snapshot's own and answers `If-None-Match`.
 */
class HeroParts(val characterId: String, val version: String = "", private val parts: Map<String, HeroPart> = emptyMap()) {
    val complete: Boolean get() = NAMES.all { it in parts }

    /** `character=<hash>,…`, or `none`: the header is how a client asks for a snapshot at all. */
    fun header(): String = parts.entries.joinToString(",") { "${it.key}=${it.value.version}" }.ifEmpty { "none" }

    fun merge(snapshot: HeroSnapshot): HeroParts {
        val next = parts.toMutableMap()
        snapshot.parts.forEach { (name, part) ->
            val base = part.base
            if (base == null) { next[name] = part; return@forEach }
            val held = parts[name]
            // A patch over a version this client does not hold: the part is gone, the hero is read again.
            if (held == null || held.version != base) next.remove(name)
            else next[name] = HeroPart(part.version, patched(held.data, part.data))
        }
        return HeroParts(characterId, snapshot.version, next)
    }

    private fun patched(held: JsonElement, patch: JsonElement): JsonElement {
        val delta = WireJson.decodeFromJsonElement<InventoryPatch>(patch)
        val gone = delta.removed.toHashSet() + delta.changed.map { it.entityId }
        return JsonArray(held.jsonArray.filterNot { it.jsonObject.entityId in gone } + delta.changed)
    }

    val character: CharacterSummary get() = decode(CHARACTER)
    val inventory: List<EquipmentInstance> get() = decode(INVENTORY)
    val tree: SkillTreeState get() = decode(TREE)
    val bag: List<CharacterItem> get() = decode(BAG)
    val bench: List<BenchRecipe> get() = decode(BENCH)

    private inline fun <reified T> decode(name: String): T = WireJson.decodeFromJsonElement(parts.getValue(name).data)

    companion object {
        const val HEADER = "X-Hero-Parts"
        const val CHARACTER = "character"
        const val INVENTORY = "inventory"
        const val TREE = "tree"
        const val BAG = "bag"
        const val BENCH = "bench"
        val NAMES = listOf(CHARACTER, INVENTORY, TREE, BAG, BENCH)
    }
}
