package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.Cell
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.Tile
import com.sperance.exileforge.rules.content.FeatureRules
import com.sperance.exileforge.rules.content.TrapKind
import com.sperance.exileforge.rules.run.FeatureKind
import com.sperance.exileforge.rules.run.FeatureState
import com.sperance.exileforge.rules.run.FeatureUse
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.rules.run.Run
import kotlin.math.hypot
import kotlin.random.Random

/** Что шаг героя сделал с объектом карты. */
sealed interface FeatureAction {
    val spot: FeatureSpot

    /** Герой подошёл к объекту с выбором: забег ждёт ответа игрока на листе объекта. */
    data class Offer(override val spot: FeatureSpot) : FeatureAction

    /** Объект сработал сам - ловушка, открытая стена, рычаг, сундук: выбор [choice] уходит в журнал сразу. */
    data class Trigger(override val spot: FeatureSpot, val choice: Int) : FeatureAction

    /** Взведённая снова ловушка ударила: только урон - журнал её не пишет, награда была за первый раз. */
    data class Spring(override val spot: TrapSpot) : FeatureAction

    /** Объект поднял своих монстров - группу [group] (точка очага Скверны): журнал молчит, жетоны встают на карте. */
    data class Rouse(override val spot: FeatureSpot, val group: Int) : FeatureAction
}

/**
 * Объект карты на земле - общая абстракция всего, что стоит на карте и ждёт героя: объект правил
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

    /** Клетки, где объект стоит и должен быть виден: ловушка лежит вровень с полом, её скала не закрывает. */
    open val landmarks: List<Cell> get() = listOf(cell)

    /** Клетка, где объект ждёт героя сейчас: её отмечает миникарта; у очага Скверны - активная точка. */
    open val focus: Cell get() = cell

    /** Доля простоя, что открывает объект (трещина тайной комнаты), 0..1; у прочих - ноль. */
    open val progress: Double get() = 0.0

    /** Что сказать над героем, стоящим у объекта (трещина поддаётся, дверь заперта); null - нечего. */
    open fun cue(world: ExpeditionWorld): EntranceCue? = null

    /** Клетки, где объект можно увидеть: хоть одна исследована - объект на карте ([shown]). */
    protected open val sites: List<Cell> get() = listOf(cell)

    /** Заметил ли герой объект сверх исследованной клетки: ловушку замечает лишь свет; прочие - сразу. */
    protected open fun noticed(world: ExpeditionWorld): Boolean = true

    /**
     * Виден ли объект герою - одно правило для всех объектов карты: только на исследованной клетке ([sites]) и только
     * замеченный ([noticed]). Восстановленный из журнала забег не рисует сработавшую ловушку в неувиденной пустоте.
     */
    fun shown(world: ExpeditionWorld): Boolean = sites.any { world.explored(it.x, it.y) } && noticed(world)

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

    /** Объект поднимает своих монстров группы [group]; у объектов без монстров - ничего. */
    internal open fun rouse(run: ExpeditionRun, group: Int) = Unit

    /**
     * Вход в заход снова: объект поднимает монстров, что уже стояли, по своему журналу и павшим жетонам [killed] - до
     * того, как мир уложит павших.
     */
    internal open fun resume(run: ExpeditionRun, killed: Collection<Int>) = Unit

    /** Что объект видит, решая о выборе: свои выборы, павшие жетоны карты [world] и стоят ли его стражи сокровищ. */
    fun state(world: ExpeditionWorld): FeatureState = FeatureState(taken.toList(), world.killed(Run.PACK_SLOTS), world.guarded(id))

    internal fun near(world: ExpeditionWorld, at: Cell, reach: Double) = hypot(at.x + 0.5 - world.heroX, at.y + 0.5 - world.heroY) < reach
}

/**
 * Объект с выбором на листе - алтарь, торговец, узел ремесла: шагнувший к нему герой видит его лист, отошедший - может
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

/**
 * Алтарь: выбранный вариант ложится на героя и бои сразу; мимо можно пройти - алтарь ждёт нетронутым.
 */
class AltarSpot(feature: MapFeature.Altar, cell: Cell) : OfferSpot(feature, cell) {
    val altar: MapFeature.Altar get() = feature as MapFeature.Altar
}

/** Странствующий торговец: лист открыт, пока игрок покупает; купленное помечено, золото спишет сервер. */
class MerchantSpot(feature: MapFeature.Merchant, cell: Cell) : OfferSpot(feature, cell) {
    val merchant: MapFeature.Merchant get() = feature as MapFeature.Merchant
}

/** Узел ремесла: собранное сырьё приходит ответом сервера, как добыча сундука. */
class NodeSpot(feature: MapFeature.Node, cell: Cell) : OfferSpot(feature, cell) {
    val node: MapFeature.Node get() = feature as MapFeature.Node

    override fun resolve(run: ExpeditionRun, choice: Int) {
        run.showLoot(run.recordFeature(this, choice))
    }
}

/**
 * Ловушка: видна только в свете героя - больший радиус света замечает её раньше; однажды замеченная или сработавшая
 * остаётся на карте. Срабатывает, когда герой наступил ближе [reach] к центру клетки; первый раз - в журнал (награда за
 * ловушку), затем взводится снова через [rearm] секунд и бьёт опять - только уроном. [rearm] 0 - срабатывает раз.
 */
class TrapSpot(feature: MapFeature.Trap, cell: Cell, private val reach: Double, val rearm: Double = 0.0) : FeatureSpot(feature, cell) {
    val trap: TrapKind get() = (feature as MapFeature.Trap).trap
    var seen = false
        private set

    override val landmarks: List<Cell> get() = emptyList()

    /** Секунды до нового взвода; ноль - ловушка взведена. */
    var rearming = 0.0
        private set

    /** Взведена ли: сработавшая без повторного взвода стоит разряженной до конца карты. */
    val armed: Boolean get() = rearming <= 0 && (!spent || rearm > 0)

    override fun noticed(world: ExpeditionWorld): Boolean = seen || spent

    override fun touch(world: ExpeditionWorld, dt: Double): FeatureAction? {
        if (!seen && world.lit(cell.x, cell.y)) seen = true
        if (rearming > 0) rearming = (rearming - dt).coerceAtLeast(0.0)
        if (!armed || !near(world, cell, reach)) return null
        rearming = rearm
        return if (spent) FeatureAction.Spring(this) else FeatureAction.Trigger(this, MapFeature.TRIGGER)
    }

    /** Уникалка ловушек приходит ответом сервера - в итог карты, без панели сундука. */
    override fun resolve(run: ExpeditionRun, choice: Int) {
        run.rewarding(run.recordFeature(this, choice))
        run.spring(feature as MapFeature.Trap)
    }

    /** Сработавшая ловушка остаётся сработавшей: удар уже нанесён. */
    override fun refused(run: ExpeditionRun, choice: Int) = Unit
}

/**
 * Комната за стеной: тайная - трещина [entrance], у которой стоят [seconds] секунд; запертая - дверь [entrance], что
 * открывает рычаг [lever] неподалёку (в `expedition.lever` шагах от двери). Открытая - сундук сокровищницы [chest] внутри.
 * [hint] - середина размытой метки «рычаг где-то здесь» миникарты, в клетках карты; у тайной - null.
 */
class RoomSpot(feature: MapFeature.Room, val entrance: Cell, val chest: Cell, val lever: Cell?, private val seconds: Double, val hint: Pair<Double, Double>? = null) : FeatureSpot(feature, entrance) {
    /** Сундук открытой комнаты - за её каменным краем: край над ним рисуется полупрозрачным. */
    override val landmarks: List<Cell> get() = if (opened) listOf(chest) else emptyList()

    val opened: Boolean get() = MapFeature.OPEN in taken
    val looted: Boolean get() = MapFeature.LOOT in taken

    /** Доля простоя у трещины, 0..1; ноль, пока герой не стоит рядом. */
    override var progress = 0.0
        private set

    override val sites: List<Cell> get() = listOf(entrance)

    /**
     * Сундук под стражами сокровищ - только когда они пали, как его примет сервер. Трещина открывается простоем рядом: шаг прочь начинает
     * отсчёт заново.
     */
    override fun touch(world: ExpeditionWorld, dt: Double): FeatureAction? {
        if (opened) return if (!looted && !world.guarded(id) && near(world, chest, world.rules.chestReach)) FeatureAction.Trigger(this, MapFeature.LOOT) else null
        lever?.let { return if (near(world, it, world.rules.chestReach)) FeatureAction.Trigger(this, MapFeature.OPEN) else null }
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

    /** Герой у входа: у двери - «заперто, найди рычаг», у трещины - её простой (0 - подошёл, ещё не стоит). */
    override fun cue(world: ExpeditionWorld): EntranceCue? = when {
        opened || !near(world, entrance, ADJACENT) -> null
        lever != null -> EntranceCue.Locked
        else -> EntranceCue.Crack(progress.toFloat())
    }

    /** Размытая метка рычага видна: дверь найдена, заперта, а сам рычаг ещё не увиден. */
    fun hinted(world: ExpeditionWorld): Boolean = !opened && lever?.let { !world.explored(it.x, it.y) } == true && shown(world)

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
 * Ставит объекты карты, один раз: где - по семени (точки очага Скверны - цепью), на земле под объекты ([ExpeditionWorld.ground]) вдали
 * от прочих объектов; комнаты вырезаются в скале за стеной, касаясь пола только входом (клетка перед дверью - тоже на этой земле), так что карта остаётся связной, а комната закрыта до открытия.
 * [log] - журнал объектов, с которым герой вернулся в заход: использованное стоит использованным, открытые комнаты открыты.
 *
 * Рычаг запертой комнаты - рядом с дверью, в `expedition.lever` шагах от клетки перед ней; нет такого места - следующая дверь: комната
 * вырезается, только когда место рычага есть.
 */
fun ExpeditionWorld.placeFeatures(features: List<MapFeature>, log: Collection<FeatureUse>, featureRules: FeatureRules?) {
    val traps = featureRules?.traps
    if (features.isEmpty() || this.features.isNotEmpty()) return
    val placing = Random(seed * 2_750_159 + 71)
    val taken = (chests.map { it.cell } + fountains.map { it.cell } + crystals.map { it.cell } + cracks.map { it.cell } + listOfNotNull(portal)).toMutableSet()
    val ground = ground()
    val open = ground.filterTo(LinkedHashSet()) { it !in taken }
    val roomy = open.roomy().shuffled(placing).toMutableList()
    val anywhere = open.shuffled(placing).toMutableList()
    fun apart(cell: Cell, spacing: Double) = this.features.all { hypot((it.cell.x - cell.x).toDouble(), (it.cell.y - cell.y).toDouble()) >= spacing }
    fun claim(pool: MutableList<Cell>, spacing: Double): Cell? = pool.firstOrNull { it !in taken && apart(it, spacing) }?.also { taken += it }
    features.forEach { feature ->
        val spot = when (feature) {
            is MapFeature.Trap -> claim(anywhere, TRAP_SPACING)?.let { TrapSpot(feature, it, traps?.reach ?: 0.0, traps?.rearm ?: 0.0) }

            is MapFeature.Room -> {
                val vault = feature.kind == FeatureKind.VAULT
                RoomCarver.plans(map, ground, feature.room.size, placing).firstNotNullOfOrNull { plan ->
                    val lever = if (vault) leverFor(plan.front, roomy, taken, ::apart) ?: return@firstNotNullOfOrNull null else null
                    lever?.let { taken += it }
                    RoomCarver.cut(map, plan)
                    RoomSpot(feature, plan.entrance, plan.chest, lever, feature.seconds, lever?.let { leverHint(it, placing) })
                }
            }

            is MapFeature.Altar -> claim(roomy, rules.chestSpacing)?.let { AltarSpot(feature, it) }

            is MapFeature.Merchant -> claim(roomy, rules.chestSpacing)?.let { MerchantSpot(feature, it) }

            is MapFeature.Node -> claim(roomy, rules.chestSpacing)?.let { NodeSpot(feature, it) }

            is MapFeature.Blight -> claim(roomy, rules.chestSpacing)?.let { first -> BlightSpot(feature, blightCells(feature.points, featureRules?.blight?.spacing ?: 1, first, roomy, taken, placing)) }
        } ?: return@forEach
        log.filter { it.id == feature.id }.forEach { spot.take(it.choice) }
        this.features += spot
        if (spot is RoomSpot && spot.opened) openRoom(spot)
    }
}

/**
 * Место рычага двери с клеткой [front] перед ней: первая из [pool], что не занята ([taken]), стоит поодаль от прочих
 * объектов ([apart]) и лежит в `expedition.lever` шагах по полу от двери; null - такого места нет.
 */
private fun ExpeditionWorld.leverFor(front: Cell, pool: List<Cell>, taken: Set<Cell>, apart: (Cell, Double) -> Boolean): Cell? {
    val steps = distances(front, rules.lever.max)
    return pool.firstOrNull { cell -> cell !in taken && (steps[cell] ?: -1) >= rules.lever.min && apart(cell, rules.chestSpacing) }
}

/**
 * Середина размытой метки рычага [lever] на миникарте: сдвинута от него по семени не дальше половины `lever.hint`, так
 * что рычаг всегда внутри пятна, но не в его центре.
 */
private fun ExpeditionWorld.leverHint(lever: Cell, random: Random): Pair<Double, Double> {
    val angle = random.nextDouble() * 2 * Math.PI
    val shift = random.nextDouble() * rules.lever.hint / 2
    return lever.x + .5 + kotlin.math.cos(angle) * shift to lever.y + .5 + kotlin.math.sin(angle) * shift
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
 * Комната в скале: вход - стена рядом с достижимым полом, за ней квадрат [size]×[size] пола; всё вокруг квадрата,
 * кроме входа, - скала, так что до открытия комната отрезана от карты, а после - связана с ней одной клеткой.
 */
internal object RoomCarver {
    private val SIDES = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

    /**
     * Где комната может встать (план отдельно от выреза - вырезается только принятый, [cut]): по очереди семени,
     * лениво. План - вход, клетка перед ним, сундук и пол комнаты.
     */
    fun plans(map: ExpeditionMap, reachable: Set<Cell>, size: Int, random: Random): Sequence<Plan> {
        val doors = reachable.flatMap { floor -> SIDES.map { side -> floor to side } }
            .filter { (floor, side) -> map.tile(floor.x + side.first, floor.y + side.second) == Tile.WALL }
            .shuffled(random)
        return doors.asSequence().mapNotNull { (floor, side) ->
            val entrance = Cell(floor.x + side.first, floor.y + side.second)
            val margin = rect(entrance, side, size, 0..size + 1, 1)
            if (margin.any { it.x !in 1 until map.width - 1 || it.y !in 1 until map.height - 1 || map.tile(it.x, it.y) != Tile.WALL }) return@mapNotNull null
            Plan(entrance, floor, rect(entrance, side, 1, (size + 1) / 2..(size + 1) / 2, 0).single(), rect(entrance, side, size, 1..size, 0))
        }
    }

    /** Пол комнаты плана [plan] вырезается в скале; вход остаётся стеной до открытия. */
    fun cut(map: ExpeditionMap, plan: Plan) = plan.room.forEach { map.carve(it, Tile.FLOOR) }

    /** Место комнаты: вход [entrance], клетка пола перед ним [front], сундук [chest], пол комнаты [room]. */
    class Plan(val entrance: Cell, val front: Cell, val chest: Cell, val room: List<Cell>)

    /** Клетки за входом [entrance] по направлению [side]: шаги [depth] вглубь, поперёк - ширина [width] с полями [pad]. */
    private fun rect(entrance: Cell, side: Pair<Int, Int>, width: Int, depth: IntRange, pad: Int): List<Cell> {
        val (dx, dy) = side
        val across = -(width / 2) - pad..(width - 1 - width / 2) + pad
        return depth.flatMap { k -> across.map { j -> Cell(entrance.x + dx * k - dy * j, entrance.y + dy * k + dx * j) } }
    }
}
