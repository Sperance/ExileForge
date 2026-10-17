package com.sperance.exileforge.core.model.hero

import com.sperance.exileforge.core.campaign.CombatSheet
import com.sperance.exileforge.core.character.HeroSheet
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.core.model.crafts.WorkState
import com.sperance.exileforge.core.model.trade.MerchantStock
import com.sperance.exileforge.rules.RuleViolation
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Counter
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.content.TreeAllocation
import com.sperance.exileforge.rules.content.UniqueCopies
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
    /** Packs of stash places bought beyond the rules' base. */
    val stashSlots: Int = 0,
    /** The chronicle's counters as the server keeps them; the derived ones are added here. */
    val counters: Map<String, Long> = emptyMap(),
    /** The title worn beside the name, or blank. */
    val title: String = "",
    /** The loot filter: which rarities of which slot groups the merchant takes from a run's loot at once. */
    val autoSell: com.sperance.exileforge.rules.content.AutoSell = com.sperance.exileforge.rules.content.AutoSell(),
    /** Tree points beyond the level: only the testing window gives them. */
    val bonusPoints: Int = 0,
    /** Взятые законы тронов (сюжет): строки листа героя, по одному закону на пройденный трон и навсегда. */
    val laws: List<String> = emptyList(),
    /** Предначертание аккаунта (код `fates.json`): строки листа и рычаги боя, забега и Атласа; null - не выбрано. */
    val fate: String? = null,
    /** Трофеи боссов (Предначертание «Трофейщик»): код босса - сколько; торговец меняет их на его уникалку. */
    val trophies: Map<String, Int> = emptyMap(),
    /** Выигранных в заходах боёв (Предначертание «Щедрость судьбы»): каждый `every`-й приносит сферу; копится навсегда. */
    val fateFights: Long = 0,
    /** Номер героя: шесть цифр - по ним зовут в гильдию; пусто - сервер старше. */
    val number: String = "",
    /** Тестировщик: окно тестирования и замки прогресса - по герою, а не по аккаунту. */
    val tester: Boolean = false,
)

/** The stash's places as the server counts them after a command: used, held, the ceiling, the next pack's price (0 at the ceiling), the overflow. */
@Serializable data class StashState(
    val used: Int = 0,
    val capacity: Int = 0,
    val max: Int = 0,
    val price: Long = 0,
    val overflow: Int = 0,
    val overflowMax: Int = 0,
    val money: Long = 0,
    val shards: List<com.sperance.exileforge.rules.content.ShardYield> = emptyList(),
)

/** What a merchant paid for an item: the copy is gone by the time this arrives. */
@Serializable data class SellOutcome(val itemId: String = "", val code: String = "", val gold: Long = 0, val money: Long = 0, val shards: List<com.sperance.exileforge.rules.content.ShardYield> = emptyList())

/** Продажа пачкой: проданные id, золото за них и золото героя после. */
@Serializable data class SellBatch(val itemIds: List<String> = emptyList(), val gold: Long = 0, val money: Long = 0, val shards: List<com.sperance.exileforge.rules.content.ShardYield> = emptyList())

/**
 * What an orb, an essence or the bench answered: the copy as it now stands, the one a Mirror made, and
 * the key of a sentence with its arguments — themselves locale keys, so the client says it in its language.
 */
@Serializable data class CurrencyApplyResponse(
    val messageKey: String = "",
    val messageArgs: List<String> = emptyList(),
    val item: ItemInstance,
    val created: ItemInstance? = null,
    /** Сфера осталась в сумке (Предначертание «Бережливый кузнец»). */
    val kept: Boolean = false,
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
    /** Items the stash had no place for: they wait here to be taken in or sold. */
    val overflow: List<ItemInstance> = emptyList(),
    val bag: Map<String, Long> = emptyMap(),
    val tree: List<TakenNode> = emptyList(),
    val campaign: CampaignState = CampaignState(),
    val crafts: WorkState = WorkState(),
    val merchant: MerchantStock = MerchantStock(),
    val sheet: HeroSheet = HeroSheet.EMPTY,
    /** Лист боя: цифры сводки, разбор и сравнения - с пассивными умениями и Предначертанием; null - листа ещё нет. */
    val combat: CombatSheet? = null,
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
     * Whether [item] may go into a socket: a unique jewel is one of its kind per hero - the
     * server's rule, asked before the command so the tree can say why rather than send a refusal (`ST_022`).
     */
    fun jewelFree(index: ContentIndex, item: ItemInstance): Boolean {
        val template = index.template(item.template) ?: return true
        return try {
            TreeAllocation.requireUniqueJewelFree(template, jewels.values.filter { it.id != item.id }.map { it.template })
            true
        } catch (refused: RuleViolation) {
            false
        }
    }

    /**
     * Надеть [item] на место [slot] - вторая копия уникалки: уникальную и мифическую вещь шаблона герой носит одну -
     * правило [UniqueCopies], по нему сервер отказывает `EQ_002`. Вещь на самом месте [slot] уходит с него и не считается; null - место
     * по слоту шаблона.
     */
    fun uniqueWorn(index: ContentIndex, item: ItemInstance, slot: Slot? = null): Boolean {
        val template = index.template(item.template) ?: return false
        val freed = slot ?: template.slot
        return UniqueCopies.clash(template, equipped.filter { (place, worn) -> place != freed && worn.id != item.id }.values.map { it.template })
    }
    fun count(code: String): Long = bag[code] ?: 0L

    /** Первое взятое гнездо самоцвета дерева, где ничего не стоит; null - такого нет. */
    fun freeSocket(index: ContentIndex): String? = tree.map { it.code }.firstOrNull { code -> index.tree.node(code)?.type == SkillNodeType.JEWEL_SOCKET && code !in jewels }

    /** The chronicle whole: the kept counters, and the level, zones, atlas and tree nodes at their record. */
    val chronicle: Map<String, Long> get() = Counter.values(info.counters, Counter.derived(level, campaign.cleared.size, info.atlas.size, tree.size))
}

/**
 * The server's clock against the local one: measured when a fresh snapshot arrives with the server's
 * [com.sperance.exileforge.core.model.sync.HeroSnapshot.now], not when a kept one is read again.
 */
object ServerClock {
    @Volatile var offset: Long = 0L
        private set

    /** Часы сервера сейчас по последнему измеренному сдвигу. */
    fun now(): Long = System.currentTimeMillis() + offset

    /** The server said its clock read [serverNow] just now. */
    fun heard(serverNow: Long) {
        if (serverNow > 0) offset = serverNow - System.currentTimeMillis()
    }
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
    /** Удалён модерацией в корзину: виден до очистки, играть им нельзя - санкция в [com.sperance.exileforge.core.session.Session.sanctions]. */
    val deleted: Boolean = false,
    /** Когда герой создан: время сервера в UTC, как документ его хранит; пусто - сервер не прислал. */
    val createdAt: String = "",
    /** Тестировщик: окно тестирования - по герою. */
    val tester: Boolean = false,
) {
    /** [createdAt] в мс эпохи; null - не прочитать. */
    val createdAtMs: Long? get() = runCatching { java.time.LocalDateTime.parse(createdAt).toInstant(java.time.ZoneOffset.UTC).toEpochMilli() }.getOrNull()
}

/** The hero's statistics: key — value, only what is not zero; the keys are [com.sperance.exileforge.rules.content.Stat]'s. */
@kotlinx.serialization.Serializable data class HeroStatsView(val values: Map<String, Long> = emptyMap())

/**
 * A unique template's find: the first one, in epoch milliseconds, and how many copies came in all.
 * [lowest] - наименьший номер копии в мире героя, 0 - номера не было (копия продана при получении).
 */
@kotlinx.serialization.Serializable data class UniqueFind(val at: Long, val count: Long, val lowest: Long = 0)

/** The hero's unique finds as `GET hero/uniques` answers them: template code — its find. */
@kotlinx.serialization.Serializable data class UniquesView(val finds: Map<String, UniqueFind> = emptyMap())

/** A loot chest opened: what it gave — gold, stacks, things (already in the stash or its overflow). */
@kotlinx.serialization.Serializable
data class ChestOpening(
    val code: String,
    val gold: Long = 0,
    val items: Map<String, Long> = emptyMap(),
    val equipment: List<com.sperance.exileforge.rules.roll.ItemInstance> = emptyList(),
)
