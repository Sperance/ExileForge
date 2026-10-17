package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.TestContent
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Zone
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Сторожа режимов (быстрые варианты отчётов `:core:mobBal -Pmode=maps|abyss|rift|blight|vaal`): эталон своего уровня в каждом режиме
 * держит свою цель побед - страж карты своего уровня (уровень героя - уровень района) по тиру и Разлом недели (дойти до конца) по
 * уровню - падающую цель [FallingTarget], вожди глубин Бездны, Матерь Скверны на каждом акте и стражи Ваал - цель стража
 * [GuardianTarget.WINS]; Матерь с уровня [MotherHit.FROM] не убивает эталон одним ударом. Испытания: раш - доля зачисток по ступени
 * ([RushTarget]), башня - доля подъёмов до этажа цели ([TowerTarget]), награда испытаний в минуту - не меньше [RewardTarget.SHARE]
 * обычного захода. Промах каждой части выборки (тир, вождь, уровень, акт, страж, ступень) - в её допуске по числу боёв, средний промах
 * режима - в допуске [MEAN]; кости детерминированы. Эталоны - общий пул сторожей ([GuardHeroes]): уровни выборки - уровни зон
 * кампании, герои те же.
 */
class ModesBalanceGuardTest {
    private val index = TestContent.index
    private val classes = MobBal(index, 1, 1, 1, GuardHeroes.pool).classesOf(emptySet())

    /** Стражи карт своего уровня: каждый страж зон с тирами на тирах [MAP_TIERS] (районы 70/80/88/100 - уровни зон кампании). */
    @Test
    fun mapGuardiansOfOwnLevelMeetTheWinTarget() {
        val sim = MobBal(index, fights = MAP_ROLLS, rolls = MAP_ROLLS, monsterSamples = 1, heroes = GuardHeroes.pool)
        val zones = sim.mapZones()
        val keys = MAP_TIERS.flatMap { tier -> zones.flatMap { zone -> classes.map { Triple(tier, zone, it) } } }
        val top = MAP_TIERS.last().toDouble()
        val wins = keys.parallelStream().map { (tier, zone, c) -> Part("T$tier", sim.boss(zone, sim.mapHero(c, zone, tier), tier, Rarity.COMMON, CAP).values[WINS] as Double, FallingTarget.at(tier.toDouble(), 1.0, top)) }.toList()
        assertTrue(zones.size >= 50 && wins.size == keys.size, "the check proves nothing: ${zones.size} map guardians")
        verdict("стражи карт своего уровня", wins, MAP_PART)
    }

    /**
     * Вожди глубин Бездны: эталон уровня зоны в первой зоне Бездны каждого акта, [ABYSS_SEEDS] расщелин на класс (у каждого класса
     * свои). Каждый вождь держит цель в целом и в каждой из [ABYSS_BANDS] полос уровней зон - без провала и прогулки на части уровней.
     */
    @Test
    fun abyssLeadersMeetTheWinTarget() {
        val sim = MobBal(index, 1, 1, 1, GuardHeroes.pool)
        val modes = ModesBal(index)
        val rule = checkNotNull(index.campaign.abyss)
        val zones = index.campaign.regions.mapNotNull { region -> region.zones.filter { it.level >= rule.minLevel }.minByOrNull { it.level } }
        val depths = rule.waves.withIndex().filter { it.value.leader != null }.map { it.index + 1 }
        val keys = zones.flatMap { zone -> classes.flatMap { c -> depths.flatMap { d -> (0 until ABYSS_SEEDS).map { s -> Key(zone, c, d, s) } } } }
        val bands = zones.sortedBy { it.level }.let { sorted -> sorted.chunked((sorted.size + ABYSS_BANDS - 1) / ABYSS_BANDS) }
            .flatMap { band -> band.map { it to "ур. ${band.first().level}-${band.last().level}" } }.toMap()
        val fights = keys.parallelStream().map { k ->
            val hero = campaignHero(sim, k.zone, k.heroClass)
            k to if (modes.abyssDepth(hero, k.zone, modes.abyssSeed(hero, k.zone, k.seed), k.part).outcome == Outcome.WIN) 1.0 else 0.0
        }.toList()
        assertTrue(zones.size >= 9 && depths.size >= 3 && fights.size == keys.size, "the check proves nothing: ${zones.size} zones, ${depths.size} leaders")
        val leader = { k: Key -> "${rule.waves[k.part - 1].leader?.value}" }
        verdict("вожди Бездны", fights.map { (k, won) -> Part(leader(k), won) }, ABYSS_PART)
        verdict("вожди Бездны по уровням", fights.map { (k, won) -> Part("${leader(k)} ${bands.getValue(k.zone)}", won) }, ABYSS_BAND_PART)
    }

    /** Разлом недели: эталон уровней [RIFT_LEVELS] (уровни зон кампании) проходит [RIFT_WEEKS] недели моделью игрока - дошёл ли до конца. */
    @Test
    fun riftRunsFinishOnTarget() {
        val sim = MobBal(index, 1, 1, 1, GuardHeroes.pool)
        val modes = ModesBal(index)
        val keys = RIFT_LEVELS.flatMap { level -> classes.flatMap { c -> (0 until RIFT_WEEKS).map { w -> Triple(level, c, w) } } }
        val finished = keys.parallelStream().map { (level, c, w) ->
            val zone = sim.nearZones(level, 1).first()
            val hero = sim.hero(c, level, Archetype(campaignBand(level)), index.campaign.resistPenalty(zone.code, false))
            val run = modes.rift(hero, RIFT_WEEK + w)
            Part("ур. $level", if (!run.fallen && run.guardians == run.acts) 1.0 else 0.0, FallingTarget.at(level.toDouble(), RIFT_LEVELS.first().toDouble(), RIFT_LEVELS.last().toDouble()))
        }.toList()
        assertTrue(finished.size == keys.size && keys.isNotEmpty(), "the check proves nothing")
        verdict("Разлом (дошли до конца)", finished, RIFT_PART)
    }

    /**
     * Матерь Скверны: эталон уровня зоны в первой, средней и последней зоне каждого акта с очагом; с уровня [MotherHit.FROM] её
     * сильнейший удар - не больше [MotherHit.SHARE] запаса эталона в середине выборки и меньше всего запаса в любом бою.
     */
    @Test
    fun blightMotherMeetsTheWinTarget() {
        val sim = MobBal(index, 1, 1, 1, GuardHeroes.pool)
        val modes = ModesBal(index)
        val from = checkNotNull(index.campaign.features?.blight).fromAct
        val zones = index.campaign.regions.withIndex().filter { it.index + 1 >= from }.flatMap { (act, region) ->
            val guarded = region.zones.filter { it.boss.value.isNotBlank() && index.monster(it.boss) != null }
            listOf(guarded.first(), guarded[guarded.size / 2], guarded.last()).distinct().map { act + 1 to it }
        }
        val keys = zones.flatMap { (act, zone) -> classes.map { Key(zone, it, act, 0) } }
        val fights = keys.parallelStream().map { k -> k to modes.blightMother(campaignHero(sim, k.zone, k.heroClass), k.zone, SimKit.SEED + 13L * k.zone.level) }.toList()
        val wins = fights.map { (k, mother) -> Part("акт ${k.part}", if (mother?.won == true) 1.0 else 0.0) }
        assertTrue(zones.size >= 15 && wins.size == keys.size, "the check proves nothing: ${zones.size} zones")
        val hits = fights.filter { (k, _) -> k.zone.level >= MotherHit.FROM }.mapNotNull { it.second?.maxHit }.sorted()
        assertTrue(hits.isNotEmpty() && hits[hits.size / 2] <= MotherHit.SHARE && hits.last() < 1.0, "Матерь с ${MotherHit.FROM}-го: сильнейший удар долей запаса - медиана ${"%.2f".format(hits.getOrElse(hits.size / 2) { 0.0 })} (≤ ${MotherHit.SHARE}), наибольший ${"%.2f".format(hits.lastOrNull() ?: 0.0)} (< 1)")
        verdict("Матерь Скверны", wins, PART)
    }

    /** Стражи Ваал: каждый страж порчи в своих зонах (низшая, средняя, высшая), эталон уровня зоны, [VAAL_FIGHTS] боя на класс. */
    @Test
    fun vaalGuardiansMeetTheWinTarget() {
        val sim = MobBal(index, fights = VAAL_FIGHTS, rolls = VAAL_FIGHTS, monsterSamples = 1, heroes = GuardHeroes.pool)
        val zones = sim.vaalZones()
        val keys = zones.flatMap { zone -> classes.map { zone to it } }
        val wins = keys.parallelStream().map { (zone, c) -> Part(zone.boss.value, sim.boss(zone, campaignHero(sim, zone, c), 0, Rarity.COMMON, CAP).values[WINS] as Double) }.toList()
        assertTrue(zones.map { it.boss }.toSet().size >= 9 && wins.size == keys.size, "the check proves nothing: ${zones.size} Vaal zones")
        verdict("стражи Ваал", wins, VAAL_PART)
    }

    /** Раш: эталон уровня финала каждого региона на каждой ступени, [RUSH_SEEDS] попыток на класс; доля полных зачисток - по ступени. */
    @Test
    fun rushTiersMeetTheClearTarget() {
        val tiers = checkNotNull(index.campaign.trials).rush.tierCount
        val tries = TrialRuns.rush
        assertTrue(tiers >= 2 && tries.size >= tiers * 9 * classes.size, "the check proves nothing: ${tries.size} rushes")
        verdict("раш (полная зачистка)", tries.map { Part("ступень ${it.tier + 1}", if (it.cleared) 1.0 else 0.0, RushTarget.at(it.tier, tiers)) }, RUSH_PART)
    }

    /** Башня: эталон уровней [Trials.LEVELS] с первого этажа, [TOWER_SEEDS] подъёмов на класс; доля дошедших до этажа цели - по уровню. */
    @Test
    fun towerClimbsReachTheTargetFloor() {
        val tries = TrialRuns.tower
        assertTrue(tries.size == Trials.LEVELS.size * classes.size * TOWER_SEEDS, "the check proves nothing: ${tries.size} climbs")
        verdict("башня (этаж L−${TowerTarget.BELOW})", tries.map { Part("ур. ${it.level}", if (it.reached) 1.0 else 0.0, TowerTarget.SHARE) }, TOWER_PART)
    }

    /**
     * Награда испытаний: опыт и золото («заходы» `RunWorth`) каждой ступени раша и фарма башни с чекпоинта ([Trials.farm]) за минуту
     * стены - не меньше [RewardTarget.SHARE] обычного захода своего уровня за его минуты (бои на высшей скорости и ходьба).
     */
    @Test
    fun trialsPayMoreThanAMapRunPerMinute() {
        val perMinute = TrialRuns.runs.map { 60.0 / it.wall.coerceAtLeast(1.0) }.average()
        fun pay(runs: Double, wall: Double) = runs / (wall / 60).coerceAtLeast(1e-9) / perMinute
        val rush = TrialRuns.rush.groupBy { it.tier }.mapValues { (_, list) -> pay(list.sumOf { it.runs }, list.sumOf { it.wall }) }
        val tower = pay(TrialRuns.farm.sumOf { it.runs }, TrialRuns.farm.sumOf { it.wall })
        val low = rush.filterValues { it < RewardTarget.SHARE }.mapKeys { (tier, _) -> "ступень ${tier + 1}" } + listOfNotNull(("башня" to tower).takeIf { tower < RewardTarget.SHARE })
        assertTrue(perMinute > 0 && low.isEmpty(), "награда испытаний в минуту < ${RewardTarget.SHARE} захода: " + low.entries.joinToString { (part, it) -> "$part ×${"%.2f".format(it)}" })
    }

    /** Попытки испытаний - один прогон на три сторожа: раш, башня с первого этажа и с чекпоинта, обычные заходы тех же уровней. */
    private object TrialRuns {
        private val index = TestContent.index
        private val sim = MobBal(index, 1, 1, 1, GuardHeroes.pool)
        private val trials = Trials(index, sim, ModesBal(index))
        private val classes = sim.classesOf(emptySet())

        val rush: List<Trials.RushTry> by lazy {
            index.campaign.regions.flatMap { region -> classes.flatMap { c -> trials.rushTiers().flatMap { tier -> (0 until RUSH_SEEDS).map { Triple(region, c, tier) to it } } } }
                .parallelStream().map { (key, seed) -> trials.rush(key.first, key.second, key.third, seed) }.toList()
        }
        val tower: List<Trials.TowerTry> by lazy {
            Trials.LEVELS.flatMap { level -> classes.flatMap { c -> (0 until TOWER_SEEDS).map { Triple(level, c, it) } } }.parallelStream().map { (level, c, seed) -> trials.tower(level, c, seed) }.toList()
        }
        val farm: List<Trials.TowerTry> by lazy {
            Trials.LEVELS.flatMap { level -> classes.flatMap { c -> (0 until TOWER_SEEDS).map { Triple(level, c, it) } } }.parallelStream().map { (level, c, seed) -> trials.farm(level, c, seed) }.toList()
        }
        val runs: List<Trials.RunTry> by lazy { Trials.LEVELS.flatMap { level -> classes.map { level to it } }.parallelStream().map { (level, c) -> trials.run(level, c) }.toList() }
    }

    /** Эталон уровня зоны [zone] кампании - тот же, что у сторожа стражей ([MobBalGuardTest]). */
    private fun campaignHero(sim: MobBal, zone: Zone, heroClass: HeroClass) = sim.hero(heroClass, zone.level, Archetype(campaignBand(zone.level)), index.campaign.resistPenalty(zone.code, false))

    /** Промах каждой части выборки режима [mode] мимо её цели - не больше [part], средний промах частей - не больше [MEAN]. */
    private fun verdict(mode: String, wins: List<Part>, part: Double) {
        val parts = wins.groupBy { it.name }.mapValues { (_, list) -> list.map { it.win }.average() to list.first().target }
        val misses = parts.mapValues { (_, it) -> it.first - it.first.coerceIn(it.second) }
        val off = misses.filterValues { kotlin.math.abs(it) > part }
        val mean = misses.values.average()
        val report = parts.entries.joinToString { (name, it) -> "$name ${"%.0f".format(100 * it.first)}% (цель ${"%.0f".format(100 * it.second.start)}-${"%.0f".format(100 * it.second.endInclusive)})" }
        assertTrue(kotlin.math.abs(mean) <= MEAN && off.isEmpty(), "$mode: средний промах ${"%.1f".format(100 * mean)} п.п. (±$MEAN), части (±$part): $report")
    }

    /** Исход части выборки [name]: доля побед [win] и её цель [target]. */
    private data class Part(val name: String, val win: Double, val target: ClosedFloatingPointRange<Double> = GuardianTarget.WINS) {
        constructor(name: String, win: Double, target: Double) : this(name, win, target..target)
    }

    /** Ключ выборки: зона, класс, часть (глубина или акт), кости. */
    private data class Key(val zone: Zone, val heroClass: HeroClass, val part: Int, val seed: Int)

    private companion object {
        /** Тиры сторожа карт: районы 70/80/88/100 - уровни зон кампании актов 6-9 со штрафом карты, эталоны - из пула стражей. */
        val MAP_TIERS = listOf(1, 6, 10, 16)

        /** Бросков стража карты (по бою на бросок). */
        const val MAP_ROLLS = 2

        /** Расщелин на класс в зоне Бездны. */
        const val ABYSS_SEEDS = 6

        /** Полос уровней зон Бездны: по три зоны - начало, середина и конец кампании. */
        const val ABYSS_BANDS = 3

        /** Уровни сторожа Разлома: уровни зон кампании от 21 до 88 (их эталоны уже в пуле). */
        val RIFT_LEVELS = listOf(21, 41, 61, 71, 81, 88)

        /** Недель Разлома на класс: исход решает схема недели (есть недели-стены и недели-прогулки), меньшая выборка - не мерило. */
        const val RIFT_WEEKS = 24

        const val CAP = 300.0

        /** Столбец доли побед в строке [MobBal.boss]. */
        const val WINS = 10

        /** Допуск среднего промаха режима: сотни боёв - разброс выборки ~3 п.п. */
        const val MEAN = 0.06

        /** Допуск тира карт: ~900 боёв на тир - разброс выборки ~2 п.п., остальное - разница стражей тира. */
        const val MAP_PART = 0.07

        /** Боёв стража Ваал на класс в зоне. */
        const val VAAL_FIGHTS = 2

        /** Допуск стража Ваал: 42 боя - разброс выборки ~8 п.п. */
        const val VAAL_PART = 0.15

        /** Допуск вождя Бездны: ~380 боёв - разброс выборки ~3 п.п. */
        const val ABYSS_PART = 0.07

        /** Допуск вождя в полосе уровней: ~130 боёв - разброс ~4 п.п., остальное - неровность вождя по уровням (кривая одна на всех вождей). */
        const val ABYSS_BAND_PART = 0.15

        /** Допуск уровня Разлома: ~170 забегов - разброс недель ~4 п.п. */
        const val RIFT_PART = 0.07

        /** Допуск акта Матери: 20-30 боёв - разброс выборки ~10 п.п. */
        const val PART = 0.20

        /** Попыток раша на класс в регионе и ступени. */
        const val RUSH_SEEDS = 1

        /** Допуск ступени раша: ~60 попыток - разброс выборки ~6 п.п., остальное - разница регионов. */
        const val RUSH_PART = 0.12

        /** Подъёмов на башню на класс и уровень. */
        const val TOWER_SEEDS = 4

        /** Допуск уровня башни: 28 подъёмов - разброс выборки ~10 п.п. */
        const val TOWER_PART = 0.20
    }
}
