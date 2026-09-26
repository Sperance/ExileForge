package com.sperance.exileforge.core.character

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.hero.CharacterSheet
import com.sperance.exileforge.core.model.hero.CharacterSummary
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import com.sperance.exileforge.core.model.hero.InactiveEquipment
import com.sperance.exileforge.core.model.hero.UnwearableEquipment
import com.sperance.exileforge.core.model.modifier.Modifier
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierOperation
import com.sperance.exileforge.core.model.powers.PowerBook
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.skilltree.CharacterSkillNode
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor

/** One characteristic and its place in the order the server counts them in. */
@Serializable data class StatOrder(val stat: String, val order: Int)

/** The merchant's price rule: a share of the base per affix, and a factor per rarity. */
@Serializable data class SellRule(val affixShare: Double = 0.15, val rarity: Map<String, Double> = emptyMap())

/**
 * `GET /system/stats` (server 0.41.0): what the client needs to add the sheet up itself — the order
 * characteristics are counted in (a conversion reads a source counted before it), the order worn
 * items are checked in, and the merchant's price rule.
 */
@Serializable data class StatTables(
    val stats: List<StatOrder> = emptyList(),
    val slots: List<String> = emptyList(),
    val sell: SellRule = SellRule(),
    /** Stats that are a percent already (server 0.65.0, served since 0.66.0): INCREASED adds into them instead of multiplying. */
    val percent: List<String> = emptyList(),
    /** The powers of unique items (server 0.70.0): their sheet rules close [Sheet.compute], the fight reads the rest. */
    val powers: PowerBook = PowerBook(),
) {
    val order: Map<String, Int> by lazy { stats.associate { it.stat to it.order } }
    val percentStats: Set<String> by lazy { percent.toSet() }
}

/** What wearing an item changes: the characteristic, what it is now and what it would be. */
data class StatDelta(val stat: String, val before: Double, val after: Double) {
    val change: Double get() = after - before
}

/**
 * The character sheet, added up on the client since 2.46.0 — the owner's decision.
 *
 * It is the server's `CharacterStatsCalculator` line for line, so the two cannot disagree: the
 * class's base at the level, its conversions and the tree in the first pass, then the worn items in
 * the order of their slots, each checked against what the pass before it gave. An item's local
 * modifiers fold inside it first. The server still checks every command it is sent.
 */
object Sheet {

    internal class Op(val stat: String, val operation: ModifierOperation, val value: Double, val perStat: String?, val perAmount: Double) {
        fun resolve(source: Double): Double = when {
            perStat == null -> value
            perAmount <= 0.0 -> 0.0
            else -> value * floor(source / perAmount)
        }
    }

    fun calculate(
        character: CharacterSummary,
        characterClass: CharacterClass?,
        nodes: List<CharacterSkillNode>,
        inventory: List<EquipmentInstance>,
        templates: Map<String, JsonObject>,
        definitions: List<ModifierDefinition>,
        tables: StatTables,
    ): CharacterSheet {
        val defs = definitions.associateBy { it.code }
        val level = character.level
        val base = baseOn(characterClass, level)
        val treeOps = expand(characterClass?.params.orEmpty(), defs) + expand(nodes.flatMap { it.params }, defs)
        var stats = compute(base, treeOps, tables)
        val itemOps = mutableListOf<Op>()
        val active = mutableListOf<String>()
        val inactive = mutableListOf<InactiveEquipment>()
        val taken = nodes.mapTo(mutableSetOf()) { it.code }
        inventory.filter { it.equippedSlot != null }
            .sortedBy { tables.slots.indexOf(it.equippedSlot).takeIf { at -> at >= 0 } ?: Int.MAX_VALUE }
            .forEach { item ->
                val template = templates[item.equipmentId] ?: return@forEach
                // A tool works for its craft, a flask only while drunk in a fight (server 0.69.0): neither is on the sheet.
                if (template.text("slot").let { it.startsWith("TOOL_") || it in FLASK_SLOTS }) return@forEach
                val socket = item.socketCode
                if (!socket.isNullOrBlank() && socket !in taken) {
                    inactive += InactiveEquipment(item.id, template.text("code"), listOf("socket: need $socket, have none"))
                    return@forEach
                }
                val unmet = unmet(template, level, stats)
                if (unmet.isNotEmpty()) {
                    inactive += InactiveEquipment(item.id, template.text("code"), unmet)
                    return@forEach
                }
                active += item.id
                itemOps += foldItem(baseParams(template) + item.params, defs, tables)
                stats = compute(base, treeOps + itemOps, tables)
            }
        val unwearable = templates.mapNotNull { (id, template) ->
            unmet(template, level, stats).takeIf { it.isNotEmpty() }?.let { UnwearableEquipment(id, template.text("code"), it) }
        }
        return CharacterSheet(character.id, level, stats, active, inactive, unwearable, SheetModel(base, treeOps + itemOps, tables))
    }

    /** The belt's three places (server 0.69.0); a flask's template is always the first. */
    val FLASK_SLOTS = listOf("FLASK", "FLASK_2", "FLASK_3")

    /** What an item's requirements miss, in the server's words: `strength: need 40, have 32`. */
    fun unmet(template: JsonObject, level: Int, stats: Map<String, Double>): List<String> = listOfNotNull(
        check("level", int(template, "requiredLevel"), level),
        check("strength", int(template, "requiredStrength"), (stats["STOCK_STRENGTH"] ?: 0.0).toInt()),
        check("dexterity", int(template, "requiredDexterity"), (stats["STOCK_AGILITY"] ?: 0.0).toInt()),
        check("intelligence", int(template, "requiredIntelligence"), (stats["STOCK_INTELLECT"] ?: 0.0).toInt()),
    )

    /**
     * What wearing [item] would change, by the server's own placement rule: a ring takes the free
     * one of two, a two-handed weapon frees both hands, a bow pairs with a quiver and any other
     * one-handed weapon with a shield. Only the characteristics that move are returned.
     */
    fun wearing(
        item: EquipmentInstance,
        character: CharacterSummary,
        characterClass: CharacterClass?,
        nodes: List<CharacterSkillNode>,
        inventory: List<EquipmentInstance>,
        templates: Map<String, JsonObject>,
        definitions: List<ModifierDefinition>,
        tables: StatTables,
        before: Map<String, Double>,
    ): List<StatDelta> {
        val template = templates[item.equipmentId] ?: return emptyList()
        val slot = template.text("slot")
        val worn = inventory.filter { it.equippedSlot != null && it.socketCode.isNullOrBlank() && it.id != item.id }
        val occupied = worn.mapNotNull { it.equippedSlot }.toSet()
        val target = if (slot != "RING") slot else listOf("RING", "RING_2").firstOrNull { it !in occupied } ?: "RING"
        val wornWeapon = worn.firstOrNull { it.equippedSlot == "WEAPON_1H" }?.let { templates[it.equipmentId]?.text("weaponType") }
        val freed = displaced(target, template.text("weaponType"), wornWeapon) + target
        // An item not yet the hero's — a merchant's offer, a lot — is weighed as if it were in the stash.
        val after = (if (inventory.none { it.id == item.id }) inventory + item else inventory).map {
            when {
                it.id == item.id -> it.copy(equippedSlot = target, socketCode = null)
                it.socketCode.isNullOrBlank() && it.equippedSlot in freed -> it.copy(equippedSlot = null)
                else -> it
            }
        }
        val next = calculate(character, characterClass, nodes, after, templates, definitions, tables).stats
        return (before.keys + next.keys).sortedBy { tables.order[it] ?: Int.MAX_VALUE }
            .map { StatDelta(it, before[it] ?: 0.0, next[it] ?: 0.0) }
            .filter { abs(it.change) >= 0.05 }
    }

    /**
     * What the merchant pays for [item]: the template's base, times the copy's rarity, times a share
     * per rolled line, times `STOCK_GOLD` — the server's `SellPrice`, never less than one.
     */
    fun sellPrice(item: EquipmentInstance, template: JsonObject?, rule: SellRule, stats: Map<String, Double>): Long {
        val base = (template?.get("price") as? JsonPrimitive)?.doubleOrNull ?: 1.0
        val price = base * (rule.rarity[item.rarity] ?: 1.0) * (1.0 + rule.affixShare * item.params.size) *
            (1.0 + (stats["STOCK_GOLD"] ?: 0.0) / 100.0)
        return floor(price).toLong().coerceAtLeast(1L)
    }

    private fun displaced(slot: String, weapon: String, wornWeapon: String?): Set<String> {
        val wornBow = wornWeapon == "BOW"
        return when (slot) {
            "WEAPON_2H" -> setOf("WEAPON_1H", "SHIELD", "QUIVER")
            "WEAPON_1H" -> if (weapon == "BOW") setOf("WEAPON_2H", "SHIELD") else setOf("WEAPON_2H", "QUIVER")
            "SHIELD" -> if (wornBow) setOf("WEAPON_2H", "QUIVER", "WEAPON_1H") else setOf("WEAPON_2H", "QUIVER")
            "QUIVER" -> if (wornWeapon != null && !wornBow) setOf("WEAPON_2H", "SHIELD", "WEAPON_1H") else setOf("WEAPON_2H", "SHIELD")
            else -> emptySet()
        }
    }

    private fun baseOn(characterClass: CharacterClass?, level: Int): Map<String, Double> {
        val steps = (level - 1).coerceAtLeast(0)
        val result = characterClass?.baseStats.orEmpty().associate { it.stat to it.value }.toMutableMap()
        characterClass?.perLevelStats.orEmpty().forEach { result[it.stat] = (result[it.stat] ?: 0.0) + it.value * steps }
        return result
    }

    private fun baseParams(template: JsonObject): List<Modifier> =
        (template["baseParams"] as? JsonArray)?.let { runCatching { WireJson.decodeFromJsonElement(ListSerializer(Modifier.serializer()), it) }.getOrNull() }.orEmpty()

    private fun expand(modifiers: List<Modifier>, defs: Map<String, ModifierDefinition>): List<Op> = modifiers.flatMap { modifier ->
        val definition = defs[modifier.modifierCode] ?: return@flatMap emptyList()
        definition.effects.mapIndexedNotNull { index, effect ->
            val value = modifier.values.getOrNull(index) ?: return@mapIndexedNotNull null
            Op(effect.stat, effect.operation, value, effect.perStat, effect.perAmount)
        }
    }

    /** Local modifiers count inside their own item from a zero base and hand the result out as an ADD. */
    private fun foldItem(modifiers: List<Modifier>, defs: Map<String, ModifierDefinition>, tables: StatTables): List<Op> {
        val (local, global) = modifiers.partition { defs[it.modifierCode]?.isLocal == true }
        if (local.isEmpty()) return expand(global, defs)
        val folded = compute(emptyMap(), expand(local, defs), tables).filterValues { it != 0.0 }
            .map { (stat, value) -> Op(stat, ModifierOperation.ADD, value, null, 1.0) }
        return folded + expand(global, defs)
    }

    internal fun compute(base: Map<String, Double>, operations: List<Op>, tables: StatTables): Map<String, Double> {
        val byStat = operations.groupBy { it.stat }
        val result = mutableMapOf<String, Double>()
        (byStat.keys + base.keys).sortedBy { tables.order[it] ?: Int.MAX_VALUE }.forEach { stat ->
            val applied = byStat[stat].orEmpty().map { it.operation to it.resolve(it.perStat?.let { source -> result[source] } ?: 0.0) }
            result[stat] = apply(base[stat] ?: 0.0, applied, stat in tables.percentStats)
        }
        return tables.powers.applySheet(result)
    }

    /** The server's ModifierMath, rounded to one decimal half-up as it rounds; a percent stat takes INCREASED as an addition. */
    private fun apply(base: Double, operations: List<Pair<ModifierOperation, Double>>, percent: Boolean = false): Double {
        var result = base + operations.filter { it.first == ModifierOperation.ADD }.sumOf { it.second }
        val increased = operations.filter { it.first == ModifierOperation.INCREASED }.sumOf { it.second }
        if (percent) result += increased else result *= 1.0 + increased / 100.0
        operations.filter { it.first == ModifierOperation.MORE }.forEach { result *= 1.0 + it.second / 100.0 }
        operations.lastOrNull { it.first == ModifierOperation.SET }?.let { result = it.second }
        return BigDecimal.valueOf(result).setScale(1, RoundingMode.HALF_UP).toDouble()
    }

    private fun check(name: String, required: Int, actual: Int): String? =
        if (required <= actual) null else "$name: need $required, have $actual"

    private fun int(template: JsonObject, key: String): Int = (template[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() } ?: 0
}

/** A line laid on a sheet for a while — a buff, a curse, a flask, a passive skill — shaped as a modifier's effect. */
data class StatLine(val stat: String, val operation: ModifierOperation, val value: Double)

/**
 * What a sheet was added up from (2.78.0): the class's base and every operation in the server's order,
 * so a fight can lay [StatLine]s among them and add the sheet up again as the server would — an increase
 * joins the increases of its stat instead of multiplying the total. A percent stat, which takes an
 * increase as an addition, takes MORE on its whole multiplier: 20% more damage over 30% increased is 56%.
 */
class SheetModel internal constructor(private val base: Map<String, Double>, private val ops: List<Sheet.Op>, private val tables: StatTables) {
    /** The sheet with nothing laid over it: the one the server has. */
    val plain: Map<String, Double> by lazy { Sheet.compute(base, ops, tables) }

    fun with(lines: List<StatLine>): Map<String, Double> {
        if (lines.isEmpty()) return plain
        val (scaling, folding) = lines.partition { it.operation == ModifierOperation.MORE && it.stat in tables.percentStats }
        val stats = Sheet.compute(base, ops + folding.map { Sheet.Op(it.stat, it.operation, it.value, null, 1.0) }, tables).toMutableMap()
        scaling.forEach { line -> stats[line.stat] = (100 + (stats[line.stat] ?: 0.0)) * (1 + line.value / 100) - 100 }
        return stats
    }

    /** The increases of [stat] summed, [lines] among them: what a spell of that element is multiplied by. */
    fun increased(stat: String, lines: List<StatLine> = emptyList()): Double =
        ops.filter { it.stat == stat && it.operation == ModifierOperation.INCREASED }.sumOf { op -> op.resolve(op.perStat?.let { plain[it] } ?: 0.0) } +
            lines.filter { it.stat == stat && it.operation == ModifierOperation.INCREASED }.sumOf { it.value }

    override fun equals(other: Any?): Boolean = other is SheetModel && other.plain == plain
    override fun hashCode(): Int = plain.hashCode()
}
