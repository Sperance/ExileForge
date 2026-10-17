package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.campaign.AbyssWaves
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.RiftArena
import com.sperance.exileforge.core.campaign.RiftStyles
import com.sperance.exileforge.core.campaign.Spawns
import com.sperance.exileforge.core.campaign.TrialArena
import com.sperance.exileforge.core.campaign.TrialPhase
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.rules.content.AtlasBonuses
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.content.Region
import com.sperance.exileforge.rules.content.RiftNodeKind
import com.sperance.exileforge.rules.content.TrialKind
import com.sperance.exileforge.rules.content.TrialRun
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.rift.RiftAct
import com.sperance.exileforge.rules.rift.RiftEngine
import com.sperance.exileforge.rules.rift.RiftHero
import com.sperance.exileforge.rules.rift.RiftOffer
import com.sperance.exileforge.rules.rift.RiftPlan
import com.sperance.exileforge.rules.rift.RiftRest
import com.sperance.exileforge.rules.rift.RiftRun
import com.sperance.exileforge.rules.rift.RiftStage
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.EssenceCrystals
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.rules.run.MapMechanic
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext

/**
 * Режимы MODES-BAL (0.14.0, отчёт): испытания - настоящей ареной клиента ([TrialArena]) без экрана: босс-раш региона на высшей
 * скорости боя с часами стены, как их мерит сервер (бой, итог боя и [TAP] секунд игрока на «В бой» между боссами), и башня с
 * первого этажа до гибели героя (строка угрозы десятка - первая, самая слабая); стражи кристаллов эссенций и глубины Бездны - боем
 * эталона уровня зоны. С 0.14.1 - Разлом недели настоящей ареной узла ([RiftArena]) по правилам забега сервера ([RiftEngine]) и
 * очаг Скверны точка за точкой.
 */
class ModesBal(private val index: ContentIndex) {
    private val trials = checkNotNull(index.campaign.trials)

    /** Итог раша: боссов пало из [bosses], полная зачистка, секунды стены и уложился ли в лимит бонуса. */
    data class Rush(val killed: Int, val bosses: Int, val wall: Double, val fast: Boolean)

    /** Итог башни: последний пройденный этаж и уровень его врагов, секунды боёв. */
    data class Tower(val floor: Int, val level: Int, val seconds: Double)

    fun rush(hero: Hero, region: Region, tier: Int, seed: Long): Rush {
        var now = 0L
        val arena = arena(hero, TrialRun("rush-$seed", TrialKind.RUSH, seed, hero.level, 0L, region = region.code, tier = tier)) { now }
        repeat(index.rules.fight.speeds.size - 1) { arena.send(RunCommand.Speed) }
        var seen = -1
        var guard = 0
        while (arena.hud.value.phase == TrialPhase.FIGHT && guard++ < LIMIT) {
            val hud = arena.hud.value
            if (hud.cleared != seen) {
                seen = hud.cleared
                now += (TAP * 1000).toLong()
            }
            arena.send(RunCommand.Begin)
            arena.update(DT)
            now += (DT * 1000).toLong()
        }
        val hud = arena.hud.value
        val wall = now / 1000.0
        val full = hud.phase == TrialPhase.DONE && hud.cleared >= hud.steps
        return Rush(hud.cleared, hud.steps, wall, full && wall <= trials.rush.seconds * hud.steps)
    }

    fun tower(hero: Hero, seed: Long, maxFloor: Int): Tower {
        var now = 0L
        val arena = arena(hero, TrialRun("tower-$seed", TrialKind.TOWER, seed, hero.level, 0L)) { now }
        repeat(index.rules.fight.speeds.size - 1) { arena.send(RunCommand.Speed) }
        var guard = 0
        var level = 0
        while (arena.hud.value.phase == TrialPhase.FIGHT && arena.hud.value.cleared < maxFloor && guard++ < LIMIT * 20) {
            val hud = arena.hud.value
            if (hud.choice != null) arena.send(RunCommand.PickLine(0))
            arena.send(RunCommand.Begin)
            if (hud.fight != null) level = hud.level
            arena.update(DT)
            now += (DT * 1000).toLong()
        }
        val hud = arena.hud.value
        return Tower(hud.cleared, level, hud.elapsed)
    }

    /** Бой стража кристалла эссенций зоны [zone]: победа и секунды; [place] - номер кристалла (свои кости). */
    fun crystal(hero: Hero, zone: Zone, place: Int, seed: Long): Fought {
        val run = Run(index, zone, seed, RunContext(hero.heroClass.code, hero.level))
        val crystal = EssenceCrystals(index).roll(run.levelOf(zone), zone.monsters, Dice(Streams.mix(seed, place.toLong(), 5L)), AtlasBonuses())
        val guardian = Spawns(index, run).crystalGuardian(crystal, place, emptyList(), emptyList()) ?: error("no crystal guardian in ${zone.code}")
        val arena = Arena(index, hero, mapOf(MapStat.HERO_RESIST.code to index.campaign.resistPenalty(zone.code, false)))
        return arena.fight(listOf(arena.phases.foe(guardian, run.levelOf(zone))), seed + place, CAP, retinue = false)
    }

    /**
     * Глубины Бездны в зоне [zone] по порядку, как их идёт герой: каждая волна - бои по `fight.maxFoes`, между боями глубины здоровье не
     * возвращается (сам бой его несёт), новая глубина - с полным. Итог по глубине: победа и секунды.
     */
    fun abyss(hero: Hero, zone: Zone, seed: Long): List<Fought> {
        val rule = index.campaign.abyss ?: return emptyList()
        return rule.waves.indices.map { abyssDepth(hero, zone, seed, it + 1) }
    }

    /** Кости расщелины [crack] Бездны зоны [zone] у героя [hero]: у каждого класса свои расщелины - выборка не стоит на паре бросков волн. */
    fun abyssSeed(hero: Hero, zone: Zone, crack: Int): Long = Streams.mix(SimKit.SEED + 7L * zone.level, crack.toLong(), hero.heroClass.code.hashCode().toLong())

    /** Глубина [depth] Бездны зоны [zone] (с полным здоровьем, как её начинает герой): победа и секунды; глубины независимы. */
    fun abyssDepth(hero: Hero, zone: Zone, seed: Long, depth: Int): Fought {
        val rule = checkNotNull(index.campaign.abyss) { "campaign: no abyss" }
        val run = Run(index, zone, seed, RunContext(hero.heroClass.code, hero.level))
        val arena = Arena(index, hero, mapOf(MapStat.HERO_RESIST.code to index.campaign.resistPenalty(zone.code, false)))
        val fights = AbyssWaves(index, run).wave(rule, depth, 0, zone, emptyMap(), 0)
        val level = rule.waves[depth - 1].levelAt(run.levelOf(zone))
        val battles = fights.mapIndexed { i, foes -> arena.fight(foes.map { arena.phases.foe(it, level) }, seed + 31L * (depth - 1) + i, CAP, retinue = false) }
        return Fought(if (battles.all { it.outcome == Outcome.WIN }) Outcome.WIN else Outcome.LOSS, battles.sumOf { it.time }, -1.0, 0, 0.0, battles.maxOfOrNull { it.lifeLost } ?: 0.0)
    }

    /** Итог забега Разлома: павших стражей из [acts], выигранных боёв узлов, пал ли герой и на каком виде узла, бои стражей (победа, секунды). */
    data class Rift(val guardians: Int, val acts: Int, val fights: Int, val fallen: Boolean, val fatal: RiftNodeKind?, val wardens: List<Pair<Boolean, Double>>)

    /**
     * Забег Разлома недели [week] героем [hero] (0.14.1): схема недели и правила забега - сервера ([RiftPlan], [RiftEngine]), бой узла -
     * арена клиента ([RiftArena]) на высшей скорости. Игрок-модель: при здоровье ниже [REST_BELOW] - отдых, иначе сокровище, бой,
     * элита, кузня; алтарь и чемпион - если иного пути нет; дар - первый вариант, отдых - лечение, контракт - отказ.
     */
    fun rift(hero: Hero, week: Long): Rift {
        val rules = checkNotNull(trials.rift) { "campaign: no rift" }
        val engine = RiftEngine(rules, index.campaign, RiftPlan.of(rules, index.campaign, week), index.rules.fight)
        val gear = gear(hero)
        val context = RunContext(hero.heroClass.code, hero.level)
        val fate = com.sperance.exileforge.core.campaign.combat.FateRun()
        var run = RiftRun("sim-$week", week, hero.level, RiftHero(RiftStyles.of(gear)), 0L, content = index.hash)
        var fights = 0
        var fatal: RiftNodeKind? = null
        val wardens = ArrayList<Pair<Boolean, Double>>()
        var guard = 0
        while (run.stage != RiftStage.OVER && guard++ < LIMIT) {
            run = when (run.stage) {
                RiftStage.MOVE -> engine.act(run, RiftAct.Move(step(engine, run)), 0L)

                RiftStage.FIGHT -> {
                    val node = checkNotNull(run.at?.let(engine.plan::node))
                    var end: RiftAct.Fight? = null
                    val arena = RiftArena(index, rules, engine, run, checkNotNull(engine.fight(run)), context, gear, fate) { end = it }
                    repeat(index.rules.fight.speeds.size - 1) { arena.send(RunCommand.Speed) }
                    arena.send(RunCommand.Begin)
                    var frames = 0
                    while (end == null && frames++ < LIMIT) arena.update(DT)
                    val result = end ?: RiftAct.Fight(false, 0.0)
                    if (node.kind == RiftNodeKind.GUARDIAN) wardens += result.won to arena.hud.value.seconds
                    if (result.won) fights++ else fatal = node.kind
                    // Время боя меряет сервер: заявка - сразу после честного минимума
                    engine.act(run, result, run.fightAt + HOUR_MS)
                }

                RiftStage.OFFER -> engine.act(run, offer(run, rules.nodes.slots), 0L)

                RiftStage.OVER -> run
            }
        }
        return Rift(run.guardians, rules.acts, fights, run.fallen, fatal, wardens)
    }

    /** Следующий узел модели игрока: отдых при низком здоровье, иначе по [PATH]. */
    private fun step(engine: RiftEngine, run: RiftRun): Int {
        val next = engine.next(run).mapNotNull(engine.plan::node)
        val rest = next.firstOrNull { it.kind == RiftNodeKind.REST }.takeIf { run.life < REST_BELOW }
        return (rest ?: next.minWith(compareBy({ PATH.indexOf(it.kind) }, { it.id }))).id
    }

    /** Ответ модели игрока на предложение узла. */
    private fun offer(run: RiftRun, slots: Int): RiftAct = when (run.offer) {
        is RiftOffer.Boons -> if (run.boons.size < slots) RiftAct.Take(0) else RiftAct.Take(0, 0)
        RiftOffer.Rest -> RiftAct.Rest(RiftRest.HEAL)
        RiftOffer.Contract -> RiftAct.Contract(null)
        else -> RiftAct.Leave
    }

    /** Точка очага Скверны: номер ([MapFeature.Blight.points] - Матерь), победа, секунды, наибольшая доля потерянного здоровья и сильнейший удар долей запаса. */
    data class BlightPoint(val point: Int, val won: Boolean, val seconds: Double, val lifeLost: Double, val maxHit: Double = 0.0)

    /**
     * Очаг Скверны зоны [zone] целиком (0.14.1): механика поставлена насильно - все точки и Матерь; жетоны точки - монстры захода
     * ([Spawns.summoned]), бои по `fight.maxFoes` с полным здоровьем каждый (как глубины Бездны), Матерь - бой стража со свитой.
     */
    fun blight(hero: Hero, zone: Zone, seed: Long): List<BlightPoint> = hearth(hero, zone, seed)?.let { h -> (0 until h.blight.choices).map(h::point) }.orEmpty()

    /** Только бой Матери очага зоны [zone] (0.15.0, сторож Скверны): null - Матери нет. */
    fun blightMother(hero: Hero, zone: Zone, seed: Long): BlightPoint? = hearth(hero, zone, seed)?.let { h -> h.point(h.blight.motherChoice).takeIf { h.blight.mother } }

    private fun hearth(hero: Hero, zone: Zone, seed: Long): Hearth? {
        val run = Run(index, zone, seed, RunContext(hero.heroClass.code, hero.level, forced = setOf(MapMechanic.BLIGHT)))
        val blight = run.features.all.filterIsInstance<MapFeature.Blight>().firstOrNull() ?: return null
        return Hearth(blight, Spawns(index, run), Arena(index, hero, mapOf(MapStat.HERO_RESIST.code to index.campaign.resistPenalty(zone.code, false))), seed)
    }

    /** Очаг [blight] захода: бой каждой точки - своими костями от [seed]. */
    private inner class Hearth(val blight: MapFeature.Blight, private val spawns: Spawns, private val arena: Arena, private val seed: Long) {
        fun point(point: Int): BlightPoint {
            val foes = blight.tokens(point).flatMap { spawns.summoned(it, emptyList()) }
            val mother = point == blight.motherChoice && blight.mother
            val battles = if (mother) {
                listOf(arena.fight(foes.map { arena.phases.foe(it, it.level) }, seed + point, CAP, retinue = true))
            } else {
                index.rules.fight.fights(foes).mapIndexed { i, chunk -> arena.fight(chunk.map { arena.phases.foe(it, it.level) }, seed + 31L * point + i, CAP, retinue = false) }
            }
            return BlightPoint(point, battles.all { it.outcome == Outcome.WIN }, battles.sumOf { it.time }, battles.maxOfOrNull { it.lifeLost } ?: 0.0, battles.maxOfOrNull { it.maxHit } ?: 0.0)
        }
    }

    private fun gear(hero: Hero): HeroGear {
        val b = hero.build
        val sheet = Sheets.calculate(index, hero.level, hero.heroClass.code, b.tree, b.items)
        val loadout = Loadout.of(b.skills, index, hero.heroClass.code, b.flasks, sheet.stats, index.powers, index.rules.charges)
        return HeroGear(sheet.stats, hero.level, sheet.model, HeroStance.of(hero.heroClass.code), loadout, index.stats.percent)
    }

    private fun arena(hero: Hero, trial: TrialRun, clock: () -> Long): TrialArena = TrialArena(index, trial, RunContext(hero.heroClass.code, hero.level), gear(hero), emptyList(), onEvent = {}, clock = clock)

    private companion object {
        /** Кадр часов, реальные секунды. */
        const val DT = 0.05

        /** Секунды игрока между боями: касание «В бой». */
        const val TAP = 2.0

        /** Потолок секунд боя стража кристалла и волны Бездны. */
        const val CAP = 300.0

        /** Потолок кадров на испытание. */
        const val LIMIT = 200_000

        /** Модель игрока Разлома: доля здоровья, ниже которой он идёт на отдых. */
        const val REST_BELOW = .5

        /** Порядок узлов, что выбирает модель игрока Разлома. */
        val PATH = listOf(RiftNodeKind.TREASURE, RiftNodeKind.FIGHT, RiftNodeKind.ELITE, RiftNodeKind.FORGE, RiftNodeKind.REST, RiftNodeKind.ALTAR, RiftNodeKind.CHAMPION, RiftNodeKind.GUARDIAN)

        const val HOUR_MS = 3_600_000L
    }
}
