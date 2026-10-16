package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.TestContent
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Zone
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Сторожа режимов (0.15.0, решение 44; быстрые варианты отчётов `:core:mobBal -Pmode=maps|abyss|rift|blight`): эталон своего уровня
 * в каждом режиме держит цель побед стража [GuardianTarget.WINS] - страж карты своего уровня (уровень героя - уровень района) на
 * каждом тире, вожди глубин Бездны, Разлом недели (дойти до конца) на каждом уровне и Матерь Скверны на каждом акте. Сводная доля
 * режима - в цели с допуском [MEAN], доля каждой части выборки (тир, вождь, уровень, акт) - со своим допуском по её числу боёв;
 * кости детерминированы. Время: эталоны общие со сторожем стражей, сам сторож режимов - бои (~3-4 мин на 4 ядрах). Эталоны - общий пул сторожей ([GuardHeroes]): уровни выборки - уровни зон кампании, герои те же.
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
        val wins = keys.parallelStream().map { (tier, zone, c) -> tier to (sim.boss(zone, sim.mapHero(c, zone, tier), tier, Rarity.COMMON, CAP).values[WINS] as Double) }.toList()
        assertTrue(zones.size >= 50 && wins.size == keys.size, "the check proves nothing: ${zones.size} map guardians")
        verdict("стражи карт своего уровня", wins.map { (tier, w) -> "T$tier" to w }, MAP_PART)
    }

    /** Вожди глубин Бездны: эталон уровня зоны в первой зоне Бездны каждого акта, [ABYSS_SEEDS] расщелины на класс. */
    @Test
    fun abyssLeadersMeetTheWinTarget() {
        val sim = MobBal(index, 1, 1, 1, GuardHeroes.pool)
        val modes = ModesBal(index)
        val rule = checkNotNull(index.campaign.abyss)
        val zones = index.campaign.regions.mapNotNull { region -> region.zones.filter { it.level >= rule.minLevel }.minByOrNull { it.level } }
        val depths = rule.waves.withIndex().filter { it.value.leader != null }.map { it.index + 1 }
        val keys = zones.flatMap { zone -> classes.flatMap { c -> depths.flatMap { d -> (0 until ABYSS_SEEDS).map { s -> Key(zone, c, d, s) } } } }
        val wins = keys.parallelStream().map { k ->
            val won = modes.abyssDepth(campaignHero(sim, k.zone, k.heroClass), k.zone, SimKit.SEED + 7L * k.zone.level + 1009L * k.seed, k.part).outcome == Outcome.WIN
            "${rule.waves[k.part - 1].leader?.value}" to if (won) 1.0 else 0.0
        }.toList()
        assertTrue(zones.size >= 9 && depths.size >= 3 && wins.size == keys.size, "the check proves nothing: ${zones.size} zones, ${depths.size} leaders")
        verdict("вожди Бездны", wins, ABYSS_PART)
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
            "ур. $level" to if (!run.fallen && run.guardians == run.acts) 1.0 else 0.0
        }.toList()
        assertTrue(finished.size == keys.size && keys.isNotEmpty(), "the check proves nothing")
        verdict("Разлом (дошли до конца)", finished, PART)
    }

    /** Матерь Скверны: эталон уровня зоны в первой, средней и последней зоне каждого акта с очагом. */
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
        val wins = keys.parallelStream().map { k ->
            val mother = modes.blightMother(campaignHero(sim, k.zone, k.heroClass), k.zone, SimKit.SEED + 13L * k.zone.level)
            "акт ${k.part}" to if (mother?.won == true) 1.0 else 0.0
        }.toList()
        assertTrue(zones.size >= 15 && wins.size == keys.size, "the check proves nothing: ${zones.size} zones")
        verdict("Матерь Скверны", wins, PART)
    }

    /** Эталон уровня зоны [zone] кампании - тот же, что у сторожа стражей ([MobBalGuardTest]). */
    private fun campaignHero(sim: MobBal, zone: Zone, heroClass: HeroClass) = sim.hero(heroClass, zone.level, Archetype(campaignBand(zone.level)), index.campaign.resistPenalty(zone.code, false))

    /** Сводная доля режима [mode] - в цели ±[MEAN], доля каждой части выборки - ±[part]. */
    private fun verdict(mode: String, wins: List<Pair<String, Double>>, part: Double) {
        val target = GuardianTarget.WINS
        val mean = wins.map { it.second }.average()
        val parts = wins.groupBy({ it.first }, { it.second }).mapValues { (_, v) -> v.average() }
        val off = parts.filterValues { it !in (target.start - part)..(target.endInclusive + part) }
        val report = parts.entries.joinToString { (name, w) -> "$name ${"%.0f".format(100 * w)}%" }
        assertTrue(mean in (target.start - MEAN)..(target.endInclusive + MEAN) && off.isEmpty(), "$mode: цель $target, средняя ${"%.1f".format(100 * mean)}% (±$MEAN), части (±$part): $report")
    }

    /** Ключ выборки: зона, класс, часть (глубина или акт), кости. */
    private data class Key(val zone: Zone, val heroClass: HeroClass, val part: Int, val seed: Int)

    private companion object {
        /** Тиры сторожа карт: районы 70/80/88/100 - уровни зон кампании актов 6-9 со штрафом карты, эталоны - из пула стражей. */
        val MAP_TIERS = listOf(1, 6, 10, 16)

        /** Бросков стража карты (по бою на бросок). */
        const val MAP_ROLLS = 2

        const val ABYSS_SEEDS = 2

        /** Уровни сторожа Разлома: уровни зон кампании от 21 до 88 (их эталоны уже в пуле). */
        val RIFT_LEVELS = listOf(21, 41, 61, 71, 81, 88)
        const val RIFT_WEEKS = 4

        const val CAP = 300.0

        /** Столбец доли побед в строке [MobBal.boss]. */
        const val WINS = 10

        /** Допуск сводной доли режима: сотни боёв - разброс выборки ~3 п.п. */
        const val MEAN = 0.06

        /** Допуск тира карт: ~900 боёв на тир - разброс выборки ~2 п.п., остальное - разница стражей тира. */
        const val MAP_PART = 0.08

        /** Допуск вождя Бездны: ~130 боёв - разброс ~4 п.п., сила вождя по уровням неровна (вождь - одна кривая на все уровни). */
        const val ABYSS_PART = 0.15

        /** Допуск уровня Разлома и акта Матери: 20-30 забегов или боёв - разброс выборки ~10 п.п. */
        const val PART = 0.20
    }
}
