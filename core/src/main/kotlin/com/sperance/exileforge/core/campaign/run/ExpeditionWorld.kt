package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.Cell
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.MapGenerator
import com.sperance.exileforge.core.campaign.SceneSight
import com.sperance.exileforge.core.campaign.Shoreline
import com.sperance.exileforge.rules.content.BehaviourRule
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.ExpeditionRules
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.run.Run
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.random.Random

/**
 * The map in motion — positions in tile units, stepped by the scene every frame.
 *
 * It is pure so a test can walk it: the scene reads positions from here and draws them, and the
 * stick's direction comes in already in world axes (see [IsoGrid.pace]). A fight starts on
 * contact and the exit ends the map. None of this is a game rule the server knows — it is how a
 * run feels, and nothing of it travels except which monster was killed.
 *
 * The map is lit and remembered: the hero sees [lightRadius] cells —
 * the sheet's `STOCK_LIGHT_RADIUS` times the biome's `light` — along lines rock does not block
 * ([lit]); what was ever seen stays [explored], dim. Monsters walk by their [BehaviourRule]: they
 * notice a hero they can see, chase along a path round the rock, hunt the spot they lost them at
 * and go home after `giveUp` seconds; an ambusher waits until the hero is close, a sleeper wakes.
 */
class ExpeditionWorld(
    /** Шаг, радиусы и дистанции карты - из контента. */
    val rules: ExpeditionRules,
    val map: ExpeditionMap,
    /** One entry per spawn, usually of one monster — a pack is more than one. */
    packs: List<List<RolledMonster>>,
    heroSpeed: Double,
    internal val seed: Long,
    lightRadius: Double = rules.defaultLight,
    bossMonster: RolledMonster? = null,
    hasPortal: Boolean = false,
) {
    /** The hero's pace and sight; both follow the gear when it is changed on the map. */
    internal var heroSpeed = heroSpeed
    var lightRadius = lightRadius
        internal set

    fun regear(speed: Double, light: Double) {
        heroSpeed = speed
        lightRadius = light
    }

    /** Виден ли стоящий жетон [agent] сейчас: в свете. */
    fun shows(agent: MonsterAgent): Boolean = lit(floor(agent.x).toInt(), floor(agent.y).toInt())

    /** Виден ли сундук [chest]: разведанный. */
    fun shows(chest: Chest): Boolean = explored(chest.cell.x, chest.cell.y)

    internal val random = Random(seed)

    /** The map's boss: the guardian of the exit, standing beside it; none from an older server. */
    val boss: MonsterAgent? = bossMonster?.let { monster -> guardPost()?.let { cell -> MonsterAgent(packs.size, listOf(monster), cell.x + 0.5, cell.y + 0.5, joinsBrawl = false) } }

    /**
     * The Vaal portal, if this run rolled one — away from the everyday spawns and the
     * exit. Touching it opens the gate; it is gone once the zone was entered or refused.
     */
    var portal: Cell? = if (hasPortal) portalPost() else null
        internal set

    /** The portal is spent: entered, refused, or its zone closed. */
    fun closePortal() {
        portal = null
    }

    /** Stepping off the portal a refused gate left the hero on: it does not open again underfoot. */
    internal var portalArmed = true

    /** Монстры карты: жетоны, босс и стаи, что встают посреди захода ([summon]: стражи сокровищ). */
    private val roster: MutableList<MonsterAgent> = (
        packs.take(map.spawns.size).zip(map.spawns).mapIndexed { index, (pack, cell) ->
            MonsterAgent(index, pack, cell.x + 0.5, cell.y + 0.5).also { agent ->
                if (agent.monster.behaviour.type == Behaviours.PATROL) agent.patrol = patrolEnd(cell, agent.monster.behaviour.wanderRadius)
            }
        } + listOfNotNull(boss)
        ).toMutableList()
    val agents: List<MonsterAgent> get() = roster

    /**
     * Стая жетона [token] встаёт рядом с [near]: в паре шагов от клетки, на полу. Один
     * жетон встаёт один раз.
     */
    fun summon(token: Int, pack: List<RolledMonster>, near: Cell) {
        if (pack.isEmpty() || roster.any { it.id == token }) return
        summonAt(token, pack, distances(near, SUMMON_STEPS).entries.filter { it.value >= 2 }.maxByOrNull { it.value }?.key ?: near)
    }

    /** Пак жетона [token] встаёт ровно на клетке [cell] (кольцо очага Скверны). Один жетон встаёт один раз. */
    fun summonAt(token: Int, pack: List<RolledMonster>, cell: Cell) {
        if (pack.isEmpty() || roster.any { it.id == token }) return
        roster += MonsterAgent(token, pack, cell.x + 0.5, cell.y + 0.5)
    }

    /** Жетон [token] стоит на карте и пал - весь его пак. */
    fun fell(token: Int): Boolean = roster.firstOrNull { it.id == token }?.let { it.standing.isEmpty() } == true

    /** Стражи сокровищ объекта [feature] (жетон `Run.GUARD + объект`) ещё стоят: сундук комнаты и сбор узла ждут их гибели. */
    fun guarded(feature: Int): Boolean = roster.any { it.id == Run.GUARD + feature && it.standing.isNotEmpty() }

    /** Павшие члены паков карты ключами `жетон × [slots] + член`: что объект видит, решая о выборе. */
    fun killed(slots: Int): Set<Int> = roster.flatMapTo(HashSet()) { agent -> agent.fallen.map { agent.id * slots + it } }

    /** The exit does not open while its guardian lives. */
    val sealed: Boolean get() = boss?.alive == true

    /** The boss was slain within its respawn: it is not on the map this run - until its rest ends ([bossReturns]). */
    fun bossAbsent() {
        boss?.alive = false
    }

    /** Отдых стража кончился посреди захода: он встаёт на свой пост целым. */
    fun bossReturns() {
        val guardian = boss ?: return
        guardian.fallen.clear()
        guardian.x = guardian.homeX
        guardian.y = guardian.homeY
        guardian.targetX = guardian.homeX
        guardian.targetY = guardian.homeY
        guardian.alive = true
    }

    /** The members the server already counts as killed, by token `i*[slots]+m`: they stay down, and a pack with none standing is gone. */
    fun restore(killed: Collection<Int>, slots: Int) {
        if (killed.isEmpty()) return
        val dead = killed.toHashSet()
        agents.forEach { agent ->
            agent.pack.indices.forEach { m -> if (agent.id * slots + m in dead) agent.fallen += m }
            if (agent.standing.isEmpty()) agent.alive = false
        }
    }

    /**
     * The cells of these with floor on all four sides: a fountain, a chest, a crystal or a portal set against a wall
     * hid behind it in the scene's perspective. Should none be roomy, all of them.
     *
     * Скала перед местом по ходу камеры закрыла бы объект: такие места - только когда других нет.
     */
    internal fun Set<Cell>.roomy(): Set<Cell> {
        val open = filter { (x, y) -> map.walkable(x - 1, y) && map.walkable(x + 1, y) && map.walkable(x, y - 1) && map.walkable(x, y + 1) }.toSet().ifEmpty { this }
        return open.filter { (x, y) -> SceneSight.inView(x, y) { cx, cy -> !map.clear(cx, cy) } }.toSet().ifEmpty { open }
    }

    /**
     * Ориентиры, что скала закрывает от камеры: выход, портал Ваал, сундуки, фонтаны, кристаллы, трещины и
     * объекты карты, уже увиденные, со скалой перед ними. Сцена открывает вокруг каждого «окно» в скале - так виден и сундук
     * комнаты, что стоит за её каменным краем.
     *
     * Середина каждой пропасти - ориентир, скала перед ней прозрачна.
     */
    fun screened(): List<Cell> {
        val landmarks = buildList {
            add(map.exit)
            portal?.let(::add)
            chests.mapTo(this) { it.cell }
            fountains.mapTo(this) { it.cell }
            crystals.mapTo(this) { it.cell }
            cracks.mapTo(this) { it.cell }
            features.forEach { spot -> addAll(spot.landmarks) }
            addAll(chasmMarks)
        }
        return landmarks.filter { explored(it.x, it.y) && !SceneSight.inView(it.x, it.y) { x, y -> !map.clear(x, y) } }
    }

    /** Береговые контуры пропастей и вод: одни для сцены и тел; пересобираются только с правкой карты. */
    val shoreline: Shoreline get() = Shoreline.of(map, rules.shore, shore).also { shore = it }
    private var shore: Shoreline? = null

    /** Тела в скале: круг против клеток со скруглёнными углами и против контура глади, как их рисует сцена. */
    internal val body = WallBody(map, rules.cornerRadius) { shoreline }

    /** Середины пропастей карты: по клетке на связную пропасть - ближайшая к её центру; считаются раз на карту. */
    private val chasmMarks: List<Cell> by lazy {
        val seen = HashSet<Cell>()
        buildList {
            for (y in 0 until map.height) {
                for (x in 0 until map.width) {
                    val start = Cell(x, y)
                    if (map.tile(x, y) != com.sperance.exileforge.core.campaign.Tile.CHASM || !seen.add(start)) continue
                    val part = mutableListOf(start)
                    var i = 0
                    while (i < part.size) {
                        val c = part[i++]
                        STEPS.forEach { (dx, dy) ->
                            val n = Cell(c.x + dx, c.y + dy)
                            if (map.tile(n.x, n.y) == com.sperance.exileforge.core.campaign.Tile.CHASM && seen.add(n)) part += n
                        }
                    }
                    val mx = part.sumOf { it.x }.toDouble() / part.size
                    val my = part.sumOf { it.y }.toDouble() / part.size
                    add(part.minBy { hypot(it.x - mx, it.y - my) })
                }
            }
        }
    }

    /** Where the guardian stands: the floor nearest the exit, a step or two from it. */
    internal fun guardPost(): Cell? {
        val near = distances(map.exit, 3)
        return near.entries.filter { it.value in 1..2 && it.key !in map.spawns }.maxByOrNull { it.value }?.key
    }

    /**
     * Земля под объекты карты - одна точка расстановки всех объектов и портала Ваал: пол, куда дойти от входа, не на местах стай и вне
     * чистой зоны - ближе `expedition.clearSteps` шагов по полу от входа, выхода и портала Ваал (пока он стоит) ничего не встаёт.
     */
    internal fun ground(): Set<Cell> {
        val clear = listOfNotNull(map.start, map.exit, portal).flatMapTo(HashSet()) { distances(it, rules.clearSteps - 1).keys }
        val spawns = map.spawns.toSet()
        return distances(map.start, Int.MAX_VALUE).keys.filterTo(LinkedHashSet()) { it !in clear && it !in spawns }
    }

    /** Where the Vaal portal stands: on the [ground], by the seed. */
    internal fun portalPost(): Cell? {
        val placing = Random(seed * 15485863 + 53)
        return ground().roomy().shuffled(placing).firstOrNull()
    }
    var heroX = map.start.x + 0.5
    var heroY = map.start.y + 0.5

    /** The last direction the hero moved in, for the drawing to face. */
    var facingX = 1.0
    var facingY = 0.0
    var moving = false

    /** Every cell the hero has ever seen, floor and rock alike. */
    val explored = BooleanArray(map.width * map.height)

    /** The cells the hero sees right now. */
    val lit = BooleanArray(map.width * map.height)
    internal var litFrom: Cell? = null

    /** The chests the server says stand on this map, placed by [placeChests]. */
    val chests = mutableListOf<Chest>()

    /** The fountains, placed by [placeFountains]. */
    val fountains = mutableListOf<Fountain>()

    /** The fountain the hero stands at, until they step off it: one turned down is not offered again underfoot. */
    internal var atFountain: Fountain? = null

    /** The crystals of essences, placed by [placeCrystals]. */
    val crystals = mutableListOf<CrystalSpot>()

    /** The crystal the hero stands at, until they step off it: it does not open again underfoot. */
    internal var atCrystal: CrystalSpot? = null

    /** The cracks of the Abyss, placed by [placeCracks]. */
    val cracks = mutableListOf<AbyssSpot>()

    /** The crack the hero stands at, until they step off it. */
    internal var atCrack: AbyssSpot? = null

    /** Объекты карты: алтари, торговец, ловушки, комнаты, узлы - ставит [placeFeatures]. */
    val features = mutableListOf<FeatureSpot>()

    init {
        light()
    }

    /**
     * Puts [count] chests on the map, once: on the [ground], a few steps apart, nooks first — a chest is found by walking, not by standing still.
     * Where they stand is the seed's; how many, the server's. [opened] are those a run entered again already opened:
     * they stand where they stood, open, so the closed ones keep their places too.
     */
    fun placeChests(count: Int, opened: Collection<Int> = emptyList()) {
        if (count <= 0 || chests.isNotEmpty()) return
        val placing = Random(seed * 7919 + 17)
        val taken = fountains.map { it.cell }.toSet() + listOfNotNull(portal)
        fun nook(cell: Cell) = STEPS.count { (dx, dy) -> !map.walkable(cell.x + dx, cell.y + dy) }
        val candidates = ground().filterTo(LinkedHashSet()) { it !in taken }.roomy().shuffled(placing).sortedByDescending(::nook)
        for (cell in candidates) {
            if (chests.size >= count) break
            if (chests.all { hypot((it.cell.x - cell.x).toDouble(), (it.cell.y - cell.y).toDouble()) >= rules.chestSpacing }) chests += Chest(chests.size, cell)
        }
        chests.forEach { it.opened = it.id in opened }
    }

    /**
     * Puts the map's fountains down, once: how many, between the rule's low and high,
     * and where, both by the seed — on the [ground], off the chests and apart.
     * Each gives back [heal] percent of life, once.
     */
    fun placeFountains(low: Int, high: Int, heal: Double) {
        if (high <= 0 || fountains.isNotEmpty()) return
        val placing = Random(seed * 104729 + 31)
        val count = low + placing.nextInt(high - low + 1)
        if (count <= 0) return
        val taken = chests.map { it.cell }.toSet() + listOfNotNull(portal)
        val candidates = ground().filterTo(LinkedHashSet()) { it !in taken }.roomy().shuffled(placing)
        for (cell in candidates) {
            if (fountains.size >= count) break
            if (fountains.all { hypot((it.cell.x - cell.x).toDouble(), (it.cell.y - cell.y).toDouble()) >= rules.fountainSpacing }) fountains += Fountain(fountains.size, cell, heal)
        }
    }

    /**
     * Puts the zone's crystals down, once: what they hold is the server's, where they stand the
     * seed's — on the [ground], off the chests and fountains, and apart.
     */
    fun placeCrystals(held: List<Crystal>) {
        if (held.isEmpty() || crystals.isNotEmpty()) return
        val placing = Random(seed * 7727 + 97)
        val taken = chests.map { it.cell }.toSet() + fountains.map { it.cell } + listOfNotNull(portal)
        val candidates = ground().filterTo(LinkedHashSet()) { it !in taken }.roomy().shuffled(placing)
        for (cell in candidates) {
            if (crystals.size >= held.size) break
            if (crystals.all { hypot((it.cell.x - cell.x).toDouble(), (it.cell.y - cell.y).toDouble()) >= rules.chestSpacing }) crystals += CrystalSpot(crystals.size, cell, held[crystals.size])
        }
    }

    /** The crystals still standing, in the server's order: a crystal's place among them is what the server names it by. */
    val standingCrystals: List<CrystalSpot> get() = crystals.filterNot { it.freed }

    /**
     * Puts the zone's cracks of the Abyss down, once: how deep each leads is the server's, where it
     * gapes the seed's — on the [ground], away from everything else on the map, the crystals too, and apart.
     */
    fun placeCracks(depths: List<Int>) {
        if (depths.isEmpty() || cracks.isNotEmpty()) return
        val placing = Random(seed * 6151 + 53)
        val taken = chests.map { it.cell }.toSet() + fountains.map { it.cell } + crystals.map { it.cell } + listOfNotNull(portal)
        val candidates = ground().filterTo(LinkedHashSet()) { it !in taken }.roomy().shuffled(placing)
        for (cell in candidates) {
            if (cracks.size >= depths.size) break
            val apart = (cracks.map { it.cell } + crystals.map { it.cell }).all { hypot((it.x - cell.x).toDouble(), (it.y - cell.y).toDouble()) >= rules.chestSpacing }
            if (apart) cracks += AbyssSpot(cracks.size, cell, depths[cracks.size])
        }
    }

    /** A fountain still full the hero can see now, by its [id]: one tapped on the map is offered from where the hero stands. */
    fun fountainInSight(id: Int): Fountain? = fountains.firstOrNull { it.id == id && !it.used && lit(it.cell.x, it.cell.y) }

    /** The cracks not yet opened, in the server's order: a crack's place among them is what the server names it by. */
    val standingCracks: List<AbyssSpot> get() = cracks.filterNot { it.opened }

    fun explored(x: Int, y: Int) = x in 0 until map.width && y in 0 until map.height && explored[y * map.width + x]
    fun lit(x: Int, y: Int) = x in 0 until map.width && y in 0 until map.height && lit[y * map.width + x]

    /**
     * Шаг героя за [dt] секунд по шагу стика в осях карты ([paceX], [paceY] - доли скорости героя, [IsoGrid.pace]): длина шага
     * больше 1 не срезается - вверх-вниз по экрану стик ведёт быстрее скорости героя в клетках.
     *
     * Каждый объект карты слышит шаг (ловушка замечается светом, трещина считает простой); отвечает первый, кому есть что сказать.
     */
    fun step(dt: Double, paceX: Double, paceY: Double): WorldEvent? {
        val length = hypot(paceX, paceY)
        moving = length > 0.1
        if (moving) {
            val speed = heroSpeed * length
            facingX = paceX / length
            facingY = paceY / length
            val (nx, ny) = slide(heroX, heroY, facingX * speed * dt, facingY * speed * dt, rules.heroRadius)
            heroX = nx
            heroY = ny
        }
        light()
        chests.firstOrNull { !it.opened && hypot(it.cell.x + 0.5 - heroX, it.cell.y + 0.5 - heroY) < rules.chestReach }?.let { chest ->
            chest.opened = true
            return WorldEvent.Opened(chest)
        }
        val fountain = fountains.firstOrNull { !it.used && hypot(it.cell.x + 0.5 - heroX, it.cell.y + 0.5 - heroY) < rules.chestReach }
        if (fountain == null) {
            atFountain = null
        } else if (fountain !== atFountain) {
            atFountain = fountain
            return WorldEvent.AtFountain(fountain)
        }
        val crystal = crystals.firstOrNull { !it.freed && hypot(it.cell.x + 0.5 - heroX, it.cell.y + 0.5 - heroY) < rules.chestReach }
        if (crystal == null) {
            atCrystal = null
        } else if (crystal !== atCrystal) {
            atCrystal = crystal
            return WorldEvent.Crystal(crystal)
        }
        val crack = cracks.firstOrNull { !it.opened && hypot(it.cell.x + 0.5 - heroX, it.cell.y + 0.5 - heroY) < rules.chestReach }
        if (crack == null) {
            atCrack = null
        } else if (crack !== atCrack) {
            atCrack = crack
            return WorldEvent.Abyss(crack)
        }
        features.map { it.touch(this, dt) }.firstOrNull { it != null }?.let { return WorldEvent.Feature(it) }
        portal?.let { cell ->
            val near = hypot(cell.x + 0.5 - heroX, cell.y + 0.5 - heroY) < rules.chestReach
            if (near && portalArmed) {
                portalArmed = false
                return WorldEvent.Portal
            }
            if (!near) portalArmed = true
        }
        agents.filter { it.alive }.forEach { agent ->
            val toHero = hypot(heroX - agent.x, heroY - agent.y)
            if (toHero < rules.contact) return WorldEvent.Encounter(agent)
            think(agent, toHero, dt)
        }
        if (!sealed && hypot(heroX - (map.exit.x + 0.5), heroY - (map.exit.y + 0.5)) < rules.exitReach) return WorldEvent.Exit
        return null
    }

    /** Monsters still standing, the boss apart: it is counted as the exit's seal, not as one of them. */
    val alive: Int get() = agents.count { it.alive && it !== boss }
    val total: Int get() = agents.count { it !== boss }

    companion object {

        /** What a hero sees by when the server has not said: the level-1 base. */
        internal const val MIN_WALK = 0.8

        /** Как далеко от объекта встаёт стая стражей, в шагах. */
        private const val SUMMON_STEPS = 3
        internal const val REPATH = 0.4
        internal val STEPS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)

        /**
         * A new world of [zone]: the ground carved from [seed], one spawn per pack of [packs] (the run's own
         * rolls), the boss at the exit and, [hasPortal], a Vaal portal.
         */
        fun create(
            rules: ExpeditionRules,
            zone: Zone,
            packs: List<List<RolledMonster>>,
            heroStats: Map<String, Double>,
            seed: Long,
            boss: RolledMonster?,
            hasPortal: Boolean,
        ): ExpeditionWorld {
            val layout = MapGenerator.generate(seed, zone.biome, packs.size, rules.terrain, zone.size, rules.minChasm, rules.minPool, rules.shapeOf(zone.biome).shape)
            return ExpeditionWorld(rules, layout, packs, heroSpeed(rules, heroStats), seed, lightRadius(rules, heroStats, zone.light), boss, hasPortal)
        }

        /**
         * The hero's pace, sped up by movement speed from the sheet; [bossSlain] - босс основной карты убит в этом заходе:
         * шаг ещё × `bossSlainSpeed`, поверх предела скорости листа.
         */
        fun heroSpeed(rules: ExpeditionRules, stats: Map<String, Double>, bossSlain: Boolean = false): Double = rules.heroSpeed * (1 + (stats[CoreStat.MOVEMENT_SPEED.code] ?: 0.0) / 100).coerceIn(rules.minSpeed, rules.maxSpeed) * (if (bossSlain) rules.bossSlainSpeed else 1.0)

        /**
         * How far the hero sees: the sheet's light radius — the base, if the sheet has none — times the biome's light. Свет листа ноль и
         * ниже («−100% света» и сильнее) - наименьший радиус правил, а не база: база - только без строки в листе.
         */
        fun lightRadius(rules: ExpeditionRules, stats: Map<String, Double>, biomeLight: Double): Double = ((stats[CoreStat.LIGHT_RADIUS.code]?.coerceAtLeast(0.0) ?: rules.defaultLight) * biomeLight).coerceIn(rules.minLight, rules.maxLight)
    }
}
