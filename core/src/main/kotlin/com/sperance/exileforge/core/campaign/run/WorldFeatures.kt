package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.Cell
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.Tile
import com.sperance.exileforge.rules.content.TrapKind
import com.sperance.exileforge.rules.run.FeatureKind
import com.sperance.exileforge.rules.run.FeatureUse
import com.sperance.exileforge.rules.run.MapFeature
import kotlin.math.hypot
import kotlin.random.Random

/** Что шаг героя сделал с объектом карты (3.90.0, сервер 1.81.3). */
sealed interface FeatureAction {
    val spot: FeatureSpot

    /** Герой подошёл к объекту с выбором: забег ждёт ответа игрока на листе объекта. */
    data class Offer(override val spot: FeatureSpot) : FeatureAction

    /** Объект сработал сам - ловушка, открытая стена, рычаг, сундук: выбор [choice] уходит в журнал сразу. */
    data class Trigger(override val spot: FeatureSpot, val choice: Int) : FeatureAction
}

/**
 * Объект карты на земле (3.90.0, сервер 1.81.3) - общая абстракция всего, что стоит на карте и ждёт героя: объект правил
 * [feature] (что он и что в нём - бросок семени, тот же, что проверяет сервер) и клетка [cell], где его поставило семя. Мир
 * спрашивает каждый объект, что с ним сделал шаг героя ([touch]); выборы, уже записанные в журнал, - [taken], по ним объект
 * исчерпан ([spent]). Новый объект - подкласс, а не ветка в шаге мира.
 */
sealed class FeatureSpot(val feature: MapFeature, val cell: Cell) {
    val id: Int get() = feature.id
    val kind: FeatureKind get() = feature.kind

    /** Выборы объекта в журнале захода, по порядку. */
    val taken = mutableListOf<Int>()
    val spent: Boolean get() = feature.spent(taken)

    /** Доля простоя, что открывает объект (трещина тайной комнаты), 0..1; у прочих - ноль. */
    open val progress: Double get() = 0.0

    /** Виден ли объект герою: по умолчанию - когда его клетку уже видели. */
    open fun shown(world: ExpeditionWorld): Boolean = world.explored(cell.x, cell.y)

    /** Шаг героя на [dt] секунд: что объект просит у забега; null - ничего. */
    abstract fun touch(world: ExpeditionWorld, dt: Double): FeatureAction?

    /** Выбор записан в журнал; [undo] - сервер его отклонил, объект снова такой, как был. */
    open fun take(choice: Int) {
        taken += choice
    }

    open fun undo(choice: Int) {
        taken.remove(choice)
    }

    /** Выбор [choice] сделан на карте: в журнал, и что он делает в забеге сейчас - у каждого объекта своё. */
    internal open fun resolve(run: ExpeditionRun, choice: Int) {
        run.recordFeature(this, choice)
    }

    /** Сервер отклонил выбор [choice]: объект снова прежний. */
    internal open fun refused(run: ExpeditionRun, choice: Int) {
        undo(choice)
    }

    internal fun near(world: ExpeditionWorld, at: Cell, reach: Double) = hypot(at.x + 0.5 - world.heroX, at.y + 0.5 - world.heroY) < reach
}

/**
 * Объект с выбором на листе (3.90.0) - алтарь, торговец, узел ремесла: шагнувший к нему герой видит его лист, отошедший - может
 * вернуться; отказ под ногами не открывает лист снова, пока герой не сошёл с клетки.
 */
sealed class OfferSpot(feature: MapFeature, cell: Cell) : FeatureSpot(feature, cell) {
    private var armed = true

    override fun touch(world: ExpeditionWorld, dt: Double): FeatureAction? {
        if (spent) return null
        val near = near(world, cell, world.rules.chestReach)
        if (!near) armed = true
        if (!near || !armed) return null
        armed = false
        return FeatureAction.Offer(this)
    }
}

/** Алтарь сделки (3.90.0): выбранная пара ложится на героя и бои сразу; отказаться нельзя - лист не закрывается без выбора. */
class AltarSpot(feature: MapFeature.Altar, cell: Cell) : OfferSpot(feature, cell) {
    val altar: MapFeature.Altar get() = feature as MapFeature.Altar
}

/** Странствующий торговец (3.90.0): лист открыт, пока игрок покупает; купленное помечено, золото спишет сервер. */
class MerchantSpot(feature: MapFeature.Merchant, cell: Cell) : OfferSpot(feature, cell) {
    val merchant: MapFeature.Merchant get() = feature as MapFeature.Merchant
}

/** Узел ремесла (3.90.0): собранное сырьё приходит ответом сервера, как добыча сундука. */
class NodeSpot(feature: MapFeature.Node, cell: Cell) : OfferSpot(feature, cell) {
    val node: MapFeature.Node get() = feature as MapFeature.Node

    override fun resolve(run: ExpeditionRun, choice: Int) {
        run.showLoot(run.recordFeature(this, choice))
    }
}

/**
 * Ловушка (3.90.0): видна только в свете героя - больший радиус света замечает её раньше; однажды замеченная или сработавшая
 * остаётся на карте. Срабатывает раз, когда герой наступил ближе [reach] к центру клетки.
 */
class TrapSpot(feature: MapFeature.Trap, cell: Cell, private val reach: Double) : FeatureSpot(feature, cell) {
    val trap: TrapKind get() = (feature as MapFeature.Trap).trap
    var seen = false
        private set

    override fun shown(world: ExpeditionWorld): Boolean = seen || spent

    override fun touch(world: ExpeditionWorld, dt: Double): FeatureAction? {
        if (!seen && world.lit(cell.x, cell.y)) seen = true
        if (spent || !near(world, cell, reach)) return null
        return FeatureAction.Trigger(this, MapFeature.TRIGGER)
    }

    override fun resolve(run: ExpeditionRun, choice: Int) {
        // Уникалка ловушек приходит ответом сервера - в итог карты, без панели сундука
        run.rewarding(run.recordFeature(this, choice))
        run.spring(feature as MapFeature.Trap)
    }

    /** Сработавшая ловушка остаётся сработавшей: удар уже нанесён. */
    override fun refused(run: ExpeditionRun, choice: Int) = Unit
}

/**
 * Комната за стеной (3.90.0): тайная - трещина [entrance], у которой стоят [seconds] секунд; запертая - дверь [entrance], что
 * открывает рычаг [lever] в другом конце карты. Открытая - сундук сокровищницы [chest] внутри.
 */
class RoomSpot(feature: MapFeature.Room, val entrance: Cell, val chest: Cell, val lever: Cell?, private val seconds: Double) : FeatureSpot(feature, entrance) {
    val opened: Boolean get() = MapFeature.OPEN in taken
    val looted: Boolean get() = MapFeature.LOOT in taken

    /** Доля простоя у трещины, 0..1; ноль, пока герой не стоит рядом. */
    override var progress = 0.0
        private set

    override fun shown(world: ExpeditionWorld): Boolean = world.explored(entrance.x, entrance.y)

    override fun touch(world: ExpeditionWorld, dt: Double): FeatureAction? {
        if (opened) return if (!looted && near(world, chest, world.rules.chestReach)) FeatureAction.Trigger(this, MapFeature.LOOT) else null
        lever?.let { return if (near(world, it, world.rules.chestReach)) FeatureAction.Trigger(this, MapFeature.OPEN) else null }
        // Трещина открывается простоем рядом: шаг прочь начинает отсчёт заново.
        if (!near(world, entrance, ADJACENT) || world.moving) {
            progress = 0.0
            return null
        }
        progress = (progress + dt / seconds.coerceAtLeast(MIN_SECONDS)).coerceAtMost(1.0)
        return if (progress >= 1.0) FeatureAction.Trigger(this, MapFeature.OPEN) else null
    }

    override fun undo(choice: Int) {
        super.undo(choice)
        progress = 0.0
    }

    override fun resolve(run: ExpeditionRun, choice: Int) {
        val event = run.recordFeature(this, choice)
        if (choice == MapFeature.OPEN) run.world.openRoom(this) else run.showLoot(event)
    }

    /** Открытая стена или дверь открытой и остаётся: отклонён может быть лишь сундук. */
    override fun refused(run: ExpeditionRun, choice: Int) {
        if (choice == MapFeature.LOOT) undo(choice)
    }

    private companion object {
        /** Как близко к стене надо стоять: соседняя клетка. */
        const val ADJACENT = 1.3
        const val MIN_SECONDS = 0.1
    }
}

/**
 * Ставит объекты карты, один раз (3.90.0): где - по семени, вдали от входа, выхода, монстров и прочих объектов; комнаты
 * вырезаются в скале за стеной, касаясь пола только входом, так что карта остаётся связной, а комната закрыта до открытия.
 * [log] - журнал объектов, с которым герой вернулся в заход: использованное стоит использованным, открытые комнаты открыты.
 */
fun ExpeditionWorld.placeFeatures(features: List<MapFeature>, log: Collection<FeatureUse>, trapReach: Double) {
    if (features.isEmpty() || this.features.isNotEmpty()) return
    val placing = Random(seed * 2_750_159 + 71)
    val taken = (map.spawns + map.exit + map.start + chests.map { it.cell } + fountains.map { it.cell } + crystals.map { it.cell } + cracks.map { it.cell } + listOfNotNull(portal)).toMutableSet()
    val reach = distances(map.start, Int.MAX_VALUE)
    val open = reach.filter { (cell, steps) -> steps >= rules.chestSteps && cell !in taken }.keys
    val roomy = open.roomy().shuffled(placing).toMutableList()
    val anywhere = open.shuffled(placing).toMutableList()
    fun apart(cell: Cell, spacing: Double) = this.features.all { hypot((it.cell.x - cell.x).toDouble(), (it.cell.y - cell.y).toDouble()) >= spacing }
    fun claim(pool: MutableList<Cell>, spacing: Double): Cell? = pool.firstOrNull { it !in taken && apart(it, spacing) }?.also { taken += it }
    features.forEach { feature ->
        val spot = when (feature) {
            is MapFeature.Trap -> claim(anywhere, TRAP_SPACING)?.let { TrapSpot(feature, it, trapReach) }

            is MapFeature.Room -> RoomCarver.carve(map, reach.keys, feature.room.size, placing)?.let { (entrance, chest) ->
                val lever = if (feature.kind == FeatureKind.VAULT) claim(roomy, rules.chestSpacing) ?: return@let null else null
                RoomSpot(feature, entrance, chest, lever, feature.seconds)
            }

            is MapFeature.Altar -> claim(roomy, rules.chestSpacing)?.let { AltarSpot(feature, it) }

            is MapFeature.Merchant -> claim(roomy, rules.chestSpacing)?.let { MerchantSpot(feature, it) }

            is MapFeature.Node -> claim(roomy, rules.chestSpacing)?.let { NodeSpot(feature, it) }
        } ?: return@forEach
        log.filter { it.id == feature.id }.forEach { spot.take(it.choice) }
        this.features += spot
        if (spot is RoomSpot && spot.opened) openRoom(spot)
    }
}

/** Стена или дверь комнаты стала полом: комната связана с картой, свет считается заново. */
fun ExpeditionWorld.openRoom(spot: RoomSpot) {
    map.carve(spot.entrance, Tile.FLOOR)
    litFrom = null
    light()
}

/** Ловушки стоят не ближе стольких клеток друг к другу. */
private const val TRAP_SPACING = 3.0

/**
 * Комната в скале (3.90.0): вход - стена рядом с достижимым полом, за ней квадрат [size]×[size] пола; всё вокруг квадрата,
 * кроме входа, - скала, так что до открытия комната отрезана от карты, а после - связана с ней одной клеткой.
 */
private object RoomCarver {
    private val SIDES = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

    /** Вход и клетка сундука; null - места нет нигде. */
    fun carve(map: ExpeditionMap, reachable: Set<Cell>, size: Int, random: Random): Pair<Cell, Cell>? {
        val doors = reachable.flatMap { floor -> SIDES.map { side -> floor to side } }
            .filter { (floor, side) -> map.tile(floor.x + side.first, floor.y + side.second) == Tile.WALL }
            .shuffled(random)
        for ((floor, side) in doors) {
            val entrance = Cell(floor.x + side.first, floor.y + side.second)
            val room = rect(entrance, side, size, 1..size, 0)
            val margin = rect(entrance, side, size, 0..size + 1, 1)
            if (margin.any { it.x !in 1 until map.width - 1 || it.y !in 1 until map.height - 1 || map.tile(it.x, it.y) != Tile.WALL }) continue
            room.forEach { map.carve(it, Tile.FLOOR) }
            return entrance to rect(entrance, side, 1, (size + 1) / 2..(size + 1) / 2, 0).single()
        }
        return null
    }

    /** Клетки за входом [entrance] по направлению [side]: шаги [depth] вглубь, поперёк - ширина [width] с полями [pad]. */
    private fun rect(entrance: Cell, side: Pair<Int, Int>, width: Int, depth: IntRange, pad: Int): List<Cell> {
        val (dx, dy) = side
        val across = -(width / 2) - pad..(width - 1 - width / 2) + pad
        return depth.flatMap { k -> across.map { j -> Cell(entrance.x + dx * k - dy * j, entrance.y + dy * k + dx * j) } }
    }
}
