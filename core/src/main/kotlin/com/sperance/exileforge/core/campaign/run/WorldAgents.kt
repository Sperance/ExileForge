package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.Cell
import com.sperance.exileforge.rules.content.BehaviourRule
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.roll.RolledMonster
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.random.Random

/**
 * What a monster is doing on the map, between fights; [sees] - видит героя сейчас: бой рядом втягивает её в пределах её
 * взгляда ([gathered]); потерявшая героя ([HUNTING]) идёт к месту, где видела, и в чужой бой не вступает дальше `gatherRadius`.
 */
enum class AgentMode(val sees: Boolean = false) { IDLE, ASLEEP, LURKING, CHASING(sees = true), HUNTING, RETURNING }

/**
 * A monster walking the map: where it lives, where it is going, and whether it is still there.
 *
 * [pack] is one or more: a jetton is usually a single foe, sometimes a pack of up to
 * three, fought all at once. [monster] — the map token, its walking behaviour and its
 * portrait on the ground — is always the strongest of the pack; [fallen] are those already killed.
 */
class MonsterAgent(
    val id: Int,
    val pack: List<RolledMonster>,
    val homeX: Double,
    val homeY: Double,
    /** The guardian of a crystal of essences: which one of the map's, by its place; null for everyone else. */
    val crystal: Int? = null,
    /**
     * Стая вступает в общую свалку (и призванные стаи объектов карты): её бой собирает соседние такие же, и
     * она идёт в чужой. Босс, страж кристалла и Бездна - false: у их боя своя механика, они бьются одни.
     */
    val joinsBrawl: Boolean = true,
) {
    val monster: RolledMonster = pack.maxBy { it.rarity.ordinal }
    val rule: BehaviourRule get() = monster.behaviour

    /** Members of [pack] already killed: a pack the hero walked away from keeps its dead dead. */
    val fallen = mutableSetOf<Int>()

    /** Members still standing, by their place in [pack]. */
    val standing: List<Int> get() = pack.indices.filterNot { it in fallen }
    var x = homeX
    var y = homeY

    /** Клетка, где стоит жетон. */
    internal val cell: Cell get() = Cell(floor(x).toInt(), floor(y).toInt())
    var targetX = homeX
    var targetY = homeY
    var idle = 0.0

    var alive = true
    var mode = when (monster.behaviour.type) {
        Behaviours.AMBUSH -> AgentMode.LURKING
        Behaviours.SLEEP -> AgentMode.ASLEEP
        else -> AgentMode.IDLE
    }

    /** Seconds since it last saw the hero it is hunting. */
    var unseen = 0.0

    /** Where it last saw the hero: a hunt goes there before it gives up. */
    var lastX = homeX
    var lastY = homeY

    /** The far end of a patrol, or null for a monster that does not patrol. */
    var patrol: Cell? = null
    var outbound = true
    internal var path: List<Cell> = emptyList()
    internal var pathTo: Cell? = null
    internal var repath = 0.0
}

/**
 * A chest on the map: where it stands and whether the hero has opened it; [event] - его событие журнала в
 * этом заходе: пока сервер его не ответил, сундук приоткрыт и ждёт.
 */
class Chest(val id: Int, val cell: Cell) {
    var opened = false
    internal var event: Int? = null
}

/** A fountain on the map: where it stands, how much life it gives back, and whether it was drunk dry. */
class Fountain(val id: Int, val cell: Cell, val heal: Double) {
    var used = false
}

/**
 * A crystal of essences on the map: where it stands, what it holds as the server
 * last said, and whether its guardian was slain. [id] is its place among the zone's crystals at the entry.
 */
class CrystalSpot(val id: Int, val cell: Cell, var crystal: Crystal) {
    var freed = false
}

/**
 * A crack of the Abyss on the map: where it gapes, how many depths it leads down with
 * the map entered, and whether it was opened. [id] is its place among the zone's cracks at the entry.
 */
class AbyssSpot(val id: Int, val cell: Cell, val depth: Int) {
    var opened = false
}

/** What a step of the world ran into. */
sealed interface WorldEvent {
    data class Encounter(val agent: MonsterAgent) : WorldEvent
    data class Opened(val chest: Chest) : WorldEvent

    /** The hero stepped up to a fountain still full: it is offered, not drunk underfoot. */
    data class AtFountain(val fountain: Fountain) : WorldEvent

    /** The hero stepped up to a crystal of essences. */
    data class Crystal(val spot: CrystalSpot) : WorldEvent

    /** The hero stepped up to a crack of the Abyss. */
    data class Abyss(val spot: AbyssSpot) : WorldEvent

    /** Шаг героя сделал что-то с объектом карты: лист к выбору или сработавший объект. */
    data class Feature(val action: FeatureAction) : WorldEvent

    /** The hero reached the Vaal portal. */
    data object Portal : WorldEvent
    data object Exit : WorldEvent
}

/** The kinds of behaviour the campaign names: how a monster walks the map before a fight. */
object Behaviours {
    const val PATROL = "PATROL"
    const val AMBUSH = "AMBUSH"
    const val SLEEP = "SLEEP"
}
