package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Essence
import com.sperance.exileforge.rules.content.EssenceBook
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.content.TrialRules
import com.sperance.exileforge.rules.table.Ref
import com.sperance.exileforge.rules.table.TableKind
import java.util.WeakHashMap

/**
 * What a stack's card can say from the content alone, under the server's description: an essence's guaranteed
 * line per kind of item at its own tier, and where the stack comes from. Nothing here asks the server.
 */

/** An essence's guaranteed line on one kind of item: which slots take it, and the sentence with the range its tier rolls in. */
data class EssenceGuarantee(val slots: String, val line: String)

/**
 * The lines [code] guarantees, one per distinct modifier — slots sharing a line read together — at the tier the
 * essence forces: its share of the ladder, the top for a special one, as the rules' `applyEssence` picks it.
 */
fun essenceGuarantees(index: ContentIndex, code: String): List<EssenceGuarantee> {
    val essence = index.essence(code) ?: return emptyList()
    val kind = essence.kind
    val slots = listOf(
        "weapon" to kind.weapon, "caster" to (kind.caster ?: kind.weapon), "quiver" to (kind.quiver ?: kind.weapon),
        "armour" to kind.armour, "shield" to (kind.shield ?: kind.armour), "jewellery" to kind.jewellery, "belt" to (kind.belt ?: kind.jewellery),
    )
    val share = essenceShare(index, essence)
    return slots.groupBy({ it.second }, { it.first }).mapNotNull { (modifier, groups) ->
        val def = index.modifier(modifier) ?: return@mapNotNull null
        val count = def.tiers.size.takeIf { it > 0 } ?: return@mapNotNull null
        val tier = def.tier((count - Math.round(share * (count - 1)).toInt()).coerceIn(1, count)) ?: return@mapNotNull null
        EssenceGuarantee(groups.joinToString(", ") { ui("essence.slot.$it") }, rangedLine(index, modifier, tier.values))
    }
}

/** How far up its ladder an essence forces its line: the rules' share, the top for a special one. */
private fun essenceShare(index: ContentIndex, essence: Essence): Double {
    val tiers = index.essences.tiers.size
    return if (essence.special || tiers < 2) 1.0 else (essence.tier - 1).toDouble() / (tiers - 1)
}

/** Where a stack comes from, by kind; the order is the order a card lists them in. */
enum class SourceKind { WORK, MONSTERS, BOSSES, CHESTS, CORRUPTED, CRYSTALS, VAAL, EGGS, TRIALS, KEY, ABYSS, RUSH, MERCHANT, FIND }

/**
 * One way to come by a stack: its [kind], the zone [levels] it is found in, the best [chance] in percent where the
 * content gives one, and for a work or an egg what names it — [ref] the profession or the biomes, [detail] the work.
 */
data class ItemSource(val kind: SourceKind, val levels: IntRange? = null, val chance: Double? = null, val ref: String = "", val detail: String = "", val level: Int = 0)

/** The sources of [code], merged by kind and in a card's order; empty for a stack the content never hands out. */
fun itemSources(index: ContentIndex, code: String): List<ItemSource> = synchronized(sourceCache) {
    sourceCache.getOrPut(index) { SourceIndex(index).build() }
}[code].orEmpty()

/** The sources by stack, once per loaded content: a new content is a new index and so a new key. */
private val sourceCache = WeakHashMap<ContentIndex, Map<String, List<ItemSource>>>()

/** A source as a card's line: what and where, then the chance when there is one. */
fun ItemSource.title(): String = when (kind) {
    SourceKind.WORK -> ui("source.WORK", professionTitle(ref), jobTitle(detail), level)
    SourceKind.FIND -> ui("source.FIND", professionTitle(ref), jobTitle(detail))
    SourceKind.EGGS -> ui("source.EGGS", ref.split(',').joinToString(", ") { ui("expedition.biome.$it") })
    SourceKind.KEY -> ui("source.KEY", level, itemTitle(ref))
    else -> levels?.let { ui("source.$kind", if (it.first == it.last) "${it.first}" else "${it.first}–${it.last}") } ?: ui("source.$kind")
}

fun ItemSource.chanceText(): String? = chance?.takeIf { it > 0 }?.let { ui("source.chance", number(it)) }

/** The reverse of the content: every table, monster, zone, work, egg and shelf, read once into stack → sources. */
private class SourceIndex(private val index: ContentIndex) {
    private val found = HashMap<String, MutableList<ItemSource>>()

    fun build(): Map<String, List<ItemSource>> {
        monsters(); chests(); crystals(); works(); eggs(); trials()
        index.campaign.abyss?.let { abyss -> stacks(abyss.orbs).keys.forEach { add(it, ItemSource(SourceKind.ABYSS)) } }
        index.campaign.trials?.let { trials -> stacks(trials.rush.orbTable).keys.forEach { add(it, ItemSource(SourceKind.RUSH)) } }
        index.rules.merchant.orbs.codes.forEach { add(it, ItemSource(SourceKind.MERCHANT)) }
        return found.mapValues { (_, sources) -> merge(sources) }
    }

    private fun add(code: String, source: ItemSource) { found.getOrPut(code) { ArrayList() } += source }

    /** Each monster's table, as the zones it walks in: a boss, a corrupted area's keeper, or the zone's own crowd. */
    private fun monsters() {
        val levels = HashMap<String, MutableList<Int>>()
        index.campaign.zones.forEach { zone -> (zone.monsters + zone.boss + zone.corrupted).forEach { levels.getOrPut(it) { ArrayList() } += zone.level } }
        index.campaign.monsters.forEach { monster ->
            val kind = when { monster.boss -> SourceKind.BOSSES; monster.corrupted -> SourceKind.CORRUPTED; else -> SourceKind.MONSTERS }
            val range = levels[monster.code]?.let { it.min()..it.max() } ?: return@forEach
            stacks(monster.loot).forEach { (code, chance) -> add(code, ItemSource(kind, range, chance)) }
        }
    }

    private fun chests() = index.campaign.zones.forEach { zone ->
        stacks(zone.chestLoot).forEach { (code, chance) -> add(code, ItemSource(SourceKind.CHESTS, zone.level..zone.level, chance)) }
    }

    /** An essence of a tier lies in the crystals of zones of that tier, and of the next one, which sometimes gives one lower; a special one only by a Vaal orb. */
    private fun crystals() {
        val book = index.essences
        val zones = index.campaign.zones
        book.essences.values.forEach { essence ->
            if (essence.special) { add(essence.code, ItemSource(SourceKind.VAAL)); return@forEach }
            val levels = zones.filter { book.tierOf(it.level) in essence.tier..essence.tier + 1 }.map { it.level }
            if (levels.isNotEmpty()) add(essence.code, ItemSource(SourceKind.CRYSTALS, levels.min()..levels.max()))
        }
    }

    /** What the professions make: a work's product, what a work finds on the side, the condensed essences and the copied books. */
    private fun works() {
        val book = index.essences
        index.professions.professions.forEach { profession ->
            profession.jobs.forEach { job ->
                val work = { code: String, level: Int -> add(code, ItemSource(SourceKind.WORK, ref = profession.code, detail = job.code, level = level)) }
                when (job.kind) {
                    JobKind.ITEM -> if (job.output.isNotBlank()) work(job.output, job.level)
                    JobKind.CONDENSE -> book.kinds.forEach { kind ->
                        (2..book.tiers.size).forEach { tier -> work(EssenceBook.code(kind.code, tier, special = false), book.condense.levels.getOrElse(tier - 2) { job.level }) }
                    }
                    JobKind.BOOK -> index.skills.skills.filter { it.unlock <= (job.band.singleOrNull() ?: 0) }.forEach { work(it.book, job.level) }
                    else -> Unit
                }
                job.extra.forEach { add(it.item, ItemSource(SourceKind.FIND, chance = it.chance, ref = profession.code, detail = job.code)) }
            }
        }
    }

    /** A biome's egg falls from its rare monsters and bosses. */
    private fun eggs() {
        val pets = index.pets
        pets.eggs.entries.groupBy({ it.value }, { it.key }).forEach { (egg, biomes) ->
            add(egg, ItemSource(SourceKind.EGGS, chance = maxOf(pets.eggChance.rare, pets.eggChance.boss) * 100, ref = biomes.joinToString(",")))
        }
    }

    /** The crest and the seal fall from any reward of a run; the rush's key is put together from crests. */
    private fun trials() {
        val trials = index.campaign.trials ?: return
        add(TrialRules.CREST, ItemSource(SourceKind.TRIALS, chance = trials.crest))
        add(TrialRules.SEAL, ItemSource(SourceKind.TRIALS, chance = trials.seal))
        add(TrialRules.KEY, ItemSource(SourceKind.KEY, ref = TrialRules.CREST, level = trials.rush.key))
    }

    /**
     * The stacks a table can hand out, with the chance in percent where the table rolls by chance (a loot table) and
     * null where it draws by weight; a nested table multiplies in. Equipment, monsters and modifiers are not stacks.
     */
    private fun stacks(tag: String, seen: Set<String> = emptySet()): Map<String, Double?> {
        if (tag in seen) return emptyMap()
        val kind = index.tables.kind(tag) ?: return emptyMap()
        val out = HashMap<String, Double?>()
        index.tables.members(tag).values.filter { it.weight > 0 }.forEach { entry ->
            val chance = entry.chance?.let { it * 100 }
            when {
                Ref.isTable(entry.ref) -> stacks(entry.code, seen + tag).forEach { (code, inner) ->
                    out.keep(code, if (inner != null && chance != null) inner * chance / 100 else null) }
                (entry.kind ?: kind) == TableKind.ITEM -> out.keep(entry.code, chance)
            }
        }
        return out
    }

    /** Keeps the better chance of two ways to one stack; a weighted draw (null) gives way to a known chance. */
    private fun HashMap<String, Double?>.keep(code: String, chance: Double?) { this[code] = if (containsKey(code)) best(this[code], chance) else chance }

    private fun best(a: Double?, b: Double?): Double? = if (a == null) b else if (b == null) a else maxOf(a, b)

    /** One line per kind for the zone-wide sources — their levels joined, their best chance — and one per work, egg and shelf. */
    private fun merge(sources: List<ItemSource>): List<ItemSource> = sources.groupBy { if (it.levels != null) it.kind.name else "${it.kind}:${it.ref}:${it.detail}" }.values
        .map { same ->
            same.reduce { a, b ->
                a.copy(levels = a.levels?.let { x -> b.levels?.let { y -> minOf(x.first, y.first)..maxOf(x.last, y.last) } ?: x },
                    chance = best(a.chance, b.chance), level = minOf(a.level, b.level))
            }
        }
        .sortedWith(compareBy({ it.kind.ordinal }, { it.level }))
}
