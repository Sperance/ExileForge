package com.sperance.exileforge.core.model.hero

import com.sperance.exileforge.core.character.HeroSheet
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.core.model.crafts.WorkState
import com.sperance.exileforge.core.model.trade.MerchantStock
import com.sperance.exileforge.rules.RuleViolation
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Counter
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.content.TreeAllocation
import com.sperance.exileforge.rules.roll.ItemInstance
import kotlinx.serialization.Serializable

/**
 * The hero's own part of the snapshot (the server's `HeroView`): who they are, what they know and
 * have spent — everything but the items, the bag, the tree and the campaign, which are parts of their own.
 */
@Serializable data class HeroInfo(
    val id: String,
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val heroClass: String = "",
    val level: Int = 1,
    val experience: Double = 0.0,
    val money: Long = 0,
    val skills: HeroSkills = HeroSkills(),
    /** Atlas nodes taken (the start left out) and achievements earned, `<kind>:<zone>`. */
    val atlas: List<String> = emptyList(),
    val earned: List<String> = emptyList(),
    /** Bench recipes found on maps; the rest of the bench stays hidden. */
    val recipes: List<String> = emptyList(),
    val version: Long = 0,
    /** Packs of stash places bought beyond the rules' base (1.1.0). */
    val stashSlots: Int = 0,
    /** The chronicle's counters as the server keeps them (1.3.0); the derived ones are added here. */
    val counters: Map<String, Long> = emptyMap(),
    /** The title worn beside the name, or blank. */
    val title: String = "",
    /** The loot filter (3.47.0): which rarities of which slot groups the merchant takes from a run's loot at once. */
    val autoSell: com.sperance.exileforge.rules.content.AutoSell = com.sperance.exileforge.rules.content.AutoSell(),
    /** The tree's plan (3.47.0): the server takes its nodes by itself as the points come. */
    val plannedTree: List<com.sperance.exileforge.rules.content.TakenNode> = emptyList(),
    /** Tree points beyond the level (3.73.0, server 1.69.0): only the testing window gives them. */
    val bonusPoints: Int = 0,
)

/** The stash's places as the server counts them after a command: used, held, the ceiling, the next pack's price (0 at the ceiling), the overflow. */
@Serializable data class StashState(val used: Int = 0, val capacity: Int = 0, val max: Int = 0, val price: Long = 0, val overflow: Int = 0, val overflowMax: Int = 0, val money: Long = 0)

/** What a merchant paid for an item: the copy is gone by the time this arrives. */
@Serializable data class SellOutcome(val itemId: String = "", val code: String = "", val gold: Long = 0, val money: Long = 0)

/**
 * What an orb, an essence or the bench answered: the copy as it now stands, the one a Mirror made, and
 * the key of a sentence with its arguments — themselves locale keys, so the client says it in its language.
 */
@Serializable data class CurrencyApplyResponse(
    val messageKey: String = "",
    val messageArgs: List<String> = emptyList(),
    val item: ItemInstance,
    val created: ItemInstance? = null,
) {
    val message: String get() = loc(messageKey, messageArgs)
}

/**
 * Everything one hero screen needs: the parts of the snapshot together, and the sheet the client added
 * up from them. Built whenever a part moves; the screens read it and never the parts.
 */
data class HeroView(
    val info: HeroInfo,
    val items: List<ItemInstance> = emptyList(),
    /** Items the stash had no place for: they wait here to be taken in or sold (1.1.0). */
    val overflow: List<ItemInstance> = emptyList(),
    val bag: Map<String, Long> = emptyMap(),
    val tree: List<TakenNode> = emptyList(),
    val campaign: CampaignState = CampaignState(),
    val crafts: WorkState = WorkState(),
    val merchant: MerchantStock = MerchantStock(),
    val sheet: HeroSheet = HeroSheet.EMPTY,
    /** The menagerie (3.5.0, server 1.5.0). */
    val pets: PetState = PetState(),
) {
    val id: String get() = info.id
    val level: Int get() = info.level
    val money: Long get() = info.money
    val heroClass: String get() = info.heroClass
    val skills: HeroSkills get() = info.skills
    /** What is worn on the body, by the place it fills; jewels are in [jewels]. */
    val equipped: Map<Slot, ItemInstance> get() = items.filter { it.equipped && !it.socketed }.associateBy { it.slot!! }
    /** Jewels sitting in tree sockets, keyed by the socket each one fills. */
    val jewels: Map<String, ItemInstance> get() = items.filter { it.socketed }.associateBy { it.socket!! }
    /** Everything neither worn nor socketed. */
    val stash: List<ItemInstance> get() = items.filter { !it.equipped }
    val stats: Map<String, Double> get() = sheet.stats
    /** Reasons an equipped item is not counted, keyed by its id; absent means it works. */
    val inactive: Map<String, List<String>> get() = sheet.inactive
    val takenNodes: Set<String> get() = tree.mapTo(HashSet()) { it.code }
    fun item(id: String): ItemInstance? = items.firstOrNull { it.id == id }
    /**
     * Whether [item] may go into a socket (3.32.0, server 1.31.0): a unique jewel is one of its kind per hero - the
     * server's rule, asked before the command so the tree can say why rather than send a refusal (`ST_022`).
     */
    fun jewelFree(index: ContentIndex, item: ItemInstance): Boolean {
        val template = index.template(item.template) ?: return true
        return try {
            TreeAllocation.requireUniqueJewelFree(template, jewels.values.filter { it.id != item.id }.map { it.template }); true
        } catch (refused: RuleViolation) { false }
    }
    fun count(code: String): Long = bag[code] ?: 0L
    /** The chronicle whole: the kept counters, and the level, zones, atlas and tree nodes at their record. */
    val chronicle: Map<String, Long> get() = Counter.values(info.counters, Counter.derived(level, campaign.cleared.size, info.atlas.size, tree.size))
}

/** The menagerie as the server keeps it: the pets, the combat one and the helper at work by id, the ceiling and the incubator (server 1.67.0). */
@kotlinx.serialization.Serializable data class PetState(
    val pets: List<com.sperance.exileforge.rules.content.Pet> = emptyList(), val combat: String = "", val helper: String = "", val cap: Int = 0,
    val incubator: IncubatorState = IncubatorState(),
) {
    val active: List<com.sperance.exileforge.rules.content.Pet> get() = pets.filter { it.id == combat || it.id == helper }
    fun pet(id: String) = pets.firstOrNull { it.id == id }
    fun isActive(id: String) = id == combat || id == helper
}

/**
 * The incubator (server 1.67.0): [slots] places open by the hero's sheet, [max] at most, the places shown in [entries]
 * (open ones and any busy one past them) and the server's clock [now] when it was read, so a countdown runs on its time.
 */
@kotlinx.serialization.Serializable data class IncubatorState(
    val slots: Int = 0, val max: Int = 0, val entries: List<IncubatorSlot> = emptyList(), val now: Long = 0,
    /** The local clock when this was read off the server's answer: [now] belongs to that moment, not to when it is drawn. */
    @kotlinx.serialization.Transient val receivedAt: Long = System.currentTimeMillis(),
) {
    /** How far the server's clock runs ahead of the local one, measured when this was read; 0 when the server sent no clock. */
    val clockOffset: Long get() = if (now > 0) now - receivedAt else 0L
    fun slot(index: Int): IncubatorSlot? = entries.firstOrNull { it.slot == index }
    /** The server's clock now, as the local one reads it: the part is resent only on real changes, so ripeness is counted here. */
    val serverNow: Long get() = System.currentTimeMillis() + clockOffset
    val ready: Int get() = serverNow.let { at -> entries.count { it.busy && it.ripe(at) } }
    val incubating: Int get() = serverNow.let { at -> entries.count { it.busy && !it.ripe(at) } }
    /** The first open place with no egg, or null when every one is taken. */
    val free: IncubatorSlot? get() = entries.firstOrNull { it.open && !it.busy }
}

/** A place of the incubator: empty when [egg] is blank; otherwise the egg's settled [rarity] and [level] and its term, epoch ms. */
@kotlinx.serialization.Serializable data class IncubatorSlot(
    val slot: Int, val open: Boolean = true, val egg: String = "", val rarity: com.sperance.exileforge.rules.content.Rarity? = null, val level: Int = 0,
    val startedAt: Long = 0, val readyAt: Long = 0, val remainingSeconds: Long = 0, val ready: Boolean = false,
) {
    val busy: Boolean get() = egg.isNotBlank()
    /** Hatched by server time [at]: the flag the server sent, or the term passed since. */
    fun ripe(at: Long): Boolean = ready || (busy && readyAt in 1..at)
    /** Ripeness 0..1 at server time [now]. */
    fun progress(now: Long): Float = if (!busy || readyAt <= startedAt) 1f else ((now - startedAt).toFloat() / (readyAt - startedAt)).coerceIn(0f, 1f)
}

/** A hero of the account's list (`GET /hero/byUser`, the raw document): enough for the menu, the rest comes with the view. */
@kotlinx.serialization.Serializable data class HeroSummary(
    @kotlinx.serialization.SerialName("_id") val id: String,
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val heroClass: String = "",
    val level: Int = 1,
    val experience: Double = 0.0,
    val money: Long = 0,
    val title: String = "",
)

/** The hero's statistics (server 1.49.0): key — value, only what is not zero; the keys are [com.sperance.exileforge.rules.content.Stat]'s. */
@kotlinx.serialization.Serializable data class HeroStatsView(val values: Map<String, Long> = emptyMap())

/** A loot chest opened (3.76.0, server 1.71.0): what it gave — gold, stacks, things (already in the stash or its overflow). */
@kotlinx.serialization.Serializable
data class ChestOpening(val code: String, val gold: Long = 0, val items: Map<String, Long> = emptyMap(),
                        val equipment: List<com.sperance.exileforge.rules.roll.ItemInstance> = emptyList())
