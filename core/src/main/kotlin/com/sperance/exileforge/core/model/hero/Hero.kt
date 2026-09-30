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
    /** Auction places bought beyond the rules' base. */
    val auctionSlots: Int = 0,
    val version: Long = 0,
    /** Packs of stash places bought beyond the rules' base (1.1.0). */
    val stashSlots: Int = 0,
    /** The chronicle's counters as the server keeps them (1.3.0); the derived ones are added here. */
    val counters: Map<String, Long> = emptyMap(),
    /** The title worn beside the name, or blank. */
    val title: String = "",
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

/** The menagerie as the server keeps it: the pets, the combat one and the helper at work by id, and the ceiling. */
@kotlinx.serialization.Serializable data class PetState(
    val pets: List<com.sperance.exileforge.rules.content.Pet> = emptyList(), val combat: String = "", val helper: String = "", val cap: Int = 0,
) {
    val active: List<com.sperance.exileforge.rules.content.Pet> get() = pets.filter { it.id == combat || it.id == helper }
    fun pet(id: String) = pets.firstOrNull { it.id == id }
    fun isActive(id: String) = id == combat || id == helper
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
