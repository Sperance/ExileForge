package com.sperance.exileforge.core.display.icons

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.svg.SvgIcon
import com.sperance.exileforge.core.model.combat.Monster
import com.sperance.exileforge.core.model.command.EquipmentSlot
import com.sperance.exileforge.core.model.icons.FALLBACK_ICON
import com.sperance.exileforge.core.model.icons.IconBindingTables
import com.sperance.exileforge.core.model.icons.IconDescriptor
import com.sperance.exileforge.core.model.icons.IconManifest
import com.sperance.exileforge.core.model.passives.PassiveNode
import com.sperance.exileforge.core.model.passives.PassiveNodeKind
import kotlinx.serialization.json.JsonObject

/** Icon ids of the loaded set. The admin editor offers them; empty until the manifest arrives. */
var iconSuggestions: List<String> = emptyList()

/**
 * The server icon set as the client holds it: manifest, binding tables and the parsed drawings.
 *
 * Every payload the server builds now carries an `icon`; documents written before the set existed
 * carry none, and then the binding tables answer instead. `ui-unknown` closes the chain.
 * Nothing here draws or recolours: the picture is the server's, this only chooses which one.
 * Identity is the comparison on purpose: the set is replaced whole, and the state object that holds
 * it is compared on every update — a structural walk over a hundred drawings would not be free.
 */
class IconSet(
    val manifest: IconManifest? = null,
    val bindings: IconBindingTables? = null,
    val drawings: Map<String, SvgIcon> = emptyMap()
) {
    private val byId: Map<String, IconDescriptor> by lazy { manifest?.icons.orEmpty().associateBy { it.id } }
    val version: String get() = manifest?.version?.ifBlank { null } ?: bindings?.version.orEmpty()
    val fallback: String get() = manifest?.fallback?.ifBlank { null } ?: bindings?.fallback ?: FALLBACK_ICON
    val ready: Boolean get() = drawings.isNotEmpty()
    fun descriptor(id: String?): IconDescriptor? = byId[id]
    /** Drawing for an id, falling back to the set's own placeholder; null lets the caller draw its emblem. */
    fun drawing(id: String?): SvgIcon? = if(id.isNullOrBlank()) null else drawings[id] ?: drawings[fallback]

    fun forStat(stat: String): String? = bindings?.stats?.get(stat.lowercase())
    fun forTag(tag: String): String? = bindings?.tags?.get(tag.lowercase())
    fun forSlot(slot: String): String? = bindings?.slots?.get(slot)
    fun forSlot(slot: EquipmentSlot): String? = forSlot(when(slot) {
        EquipmentSlot.RING_LEFT, EquipmentSlot.RING_RIGHT -> "RING"
        EquipmentSlot.MAIN_HAND -> "WEAPON_1H"
        EquipmentSlot.OFF_HAND -> "SHIELD"
        else -> slot.name
    })
    fun forRarity(rarity: String): String? = bindings?.let { it.rarities[rarity] ?: it.poeRarities[rarity] }
    fun forCurrency(currency: String): String? = bindings?.currencies?.get(currency)
    fun forBattleAction(action: String): String? = bindings?.battleActions?.get(action)
    fun forBattleStatus(status: String): String? = bindings?.battleStatuses?.get(status)

    /** An item, a character or an equipment instance projection: server value first, tables after. */
    fun forDocument(document: JsonObject): String? = document.text("icon").ifBlank {
        val tables = bindings
        when {
            tables == null -> ""
            else -> tables.bases[document.text("poeBaseId")]
                ?: tables.weapons[document.text("weaponType")]
                ?: tables.slots[document.text("slot")]
                ?: tables.itemClasses[document.text("subCategory")]
                ?: tables.itemClasses[document.text("category")]
                ?: if(document["userId"] != null) "ui-character" else ""
        }
    }.ifBlank { null }

    /** A rolled modifier or a definition: its own icon, then the bundled table, then its tags. */
    fun forModifier(document: JsonObject): String? = document.text("icon")
        .ifBlank { bindings?.modifiers?.get(document.text("definitionId").ifBlank { document.text("id") }).orEmpty() }
        .ifBlank { forProperty(document.text("definitionId").ifBlank { document.text("id") }).orEmpty() }
        .ifBlank { bindings?.modifierSources?.get(document.text("source")).orEmpty() }
        .ifBlank { null }

    /** Property rows carry stat ids, definition ids and tags in the same slot; try each table. */
    fun forProperty(key: String): String? = bindings?.let { it.stats[key.lowercase()] ?: it.modifiers[key] ?: it.tags[key.lowercase()] }

    fun forMonster(monster: Monster): String = monster.icon.orEmpty()
        .ifBlank { if(monster.boss) "combat-boss" else bindings?.combatElements?.get(monster.element).orEmpty() }
        .ifBlank { "combat-monster" }

    /** Keystones and notables are known by their shape; a small node shows the stat it grants. */
    fun forNode(node: PassiveNode): String? = node.icon.orEmpty()
        .ifBlank { if(node.kind == PassiveNodeKind.SMALL) node.effects.firstNotNullOfOrNull { forStat(it.stat) }.orEmpty() else "" }
        .ifBlank { bindings?.passiveKinds?.get(node.kind.name).orEmpty() }
        .ifBlank { null }
}
