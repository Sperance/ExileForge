package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.TestContent
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Zone
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Сторож темпа (быстрый, на выборке): эталонный герой полосы своего уровня ([campaignBand]) всех классов против
 * финалов актов кампании - зондом настоящего боя клиента ([MobBal.tempo]). Медианы выборки держат цели баланса с допуском §11
 * аудита: обычный монстр - 2-3,5 действия, редкий - 8-25 с, страж - 50-150 с. Полный прогон - каждый страж кампании против медианы
 * своего акта (0,5-2×) - отчёт `:core:mobBal -Pmode=guard`.
 */
class MobBalGuardTest {
    private val index = TestContent.index

    @Test
    fun referenceHeroesMeetTheTempoOfActFinals() {
        val sim = MobBal(index, fights = 1, rolls = ROLLS, monsterSamples = SAMPLES, heroes = GuardHeroes.pool)
        val finals = index.campaign.regions.mapNotNull { region -> region.zones.filter { it.boss.value.isNotBlank() }.maxByOrNull { it.level } }
        val tempos = finals.parallelStream().flatMap { zone ->
            sim.classesOf(emptySet()).map { heroClass ->
                sim.tempo(zone, sim.hero(heroClass, zone.level, Archetype(campaignBand(zone.level)), index.campaign.resistPenalty(zone.code, false)))
            }.stream()
        }.toList()
        assertTrue(finals.size >= 9 && tempos.isNotEmpty(), "the check proves nothing: ${finals.size} finals")
        fun median(f: (Tempo) -> Double) = tempos.map(f).sorted()[tempos.size / 2]
        val normal = median { it.normalActions }
        val rare = median { it.rareSeconds }
        val boss = median { it.bossSeconds }
        assertTrue(normal in 2.0..3.5, "обычный монстр: $normal действий (цель 2-3,5)")
        assertTrue(rare in 8.0..25.0, "редкий монстр: $rare с (цель 8-25)")
        assertTrue(boss in 50.0..150.0, "страж: $boss с (цель 50-150)")
    }

    /**
     * Сторож стражей (быстрый вариант полного прогона `:core:mobBal -Pmode=guard`) на боях [Fights]: доля побед эталонов над стражем
     * своего уровня держит цель [GuardianTarget]: каждый страж - с допуском [EACH], средняя акта - с допуском [ACT]. Разброс стражей
     * сторожит бой, а не Мощь.
     */
    @Test
    fun everyGuardianMeetsTheWinTarget() {
        val zones = guarded()
        val verdicts = guard(index, Fights.rows, EACH)
        assertTrue(zones.size >= 100 && verdicts.size == zones.size, "the check proves nothing: ${verdicts.size} of ${zones.size} guardians")
        val target = GuardianTarget.WINS
        val acts = verdicts.groupBy { it.values[3] as Int }.mapValues { (_, list) -> list.map { it.values[5] as Double }.average() }
        val off = acts.filterValues { it !in (target.start - ACT)..(target.endInclusive + ACT) }
        val out = verdicts.filter { it.values.last() == false }
        assertTrue(
            out.isEmpty() && off.isEmpty(),
            "цель побед $target: стражи вне ±$EACH - " + out.joinToString("; ") { it.tsv() } +
                "; акты вне ±$ACT - " + off.entries.joinToString { (act, wins) -> "акт $act ${"%.0f".format(100 * wins)}%" },
        )
    }

    /**
     * Восстановление стражей: реген и вампиризм каждого стража кампании в тех же боях сторожа побед (сумма по всем боям) - не больше
     * доли урона эталонов его уровня ([com.sperance.exileforge.rules.content.BossRule.sustainShare] контента): стража не перебивают одним уроном,
     * его сила - в здоровье.
     */
    @Test
    fun everyGuardianRecoversUnderTheShare() {
        val share = index.campaign.bosses.sustainShare
        val sustain = Fights.rows.groupBy { it.values[2] as String }.mapValues { (_, rows) -> rows.sumOf { it.values[RESTORED] as Double } / rows.sumOf { it.values[RESTORED + 1] as Double }.coerceAtLeast(1.0) }
        assertTrue(sustain.size == guarded().map { it.boss }.toSet().size, "the check proves nothing: ${sustain.size} guardians")
        val over = sustain.filterValues { it > share }
        assertTrue(over.isEmpty(), "реген и вампиризм стража > ${"%.0f".format(100 * share)}% урона эталона: " + over.entries.sortedByDescending { it.value }.joinToString { (boss, it) -> "$boss ${"%.0f".format(100 * it)}%" })
    }

    /**
     * Слабый билд: герой полосы [GearBand.WEAK] (небрежный игрок: вещи, дерево и умения подобраны, роллы нижней четверти) на [WeakTarget.LEAD]
     * уровней выше стража побеждает его в доле [WeakTarget.SHARE] боёв - среднее по всем стражам кампании и классам, по [FIGHTS] бою
     * на каждый из [GUARD_ROLLS] наборов костей.
     */
    @Test
    fun weakBuildsBeatGuardiansBelowTheirLevel() {
        val rows = Fights.weak
        assertTrue(rows.size >= 100 * 7, "the check proves nothing: ${rows.size} weak fights")
        val wins = rows.map { it.values[WINS] as Double }.average()
        val acts = rows.groupBy { actOf(it.values[1] as String) }.toSortedMap().mapValues { (_, list) -> list.map { it.values[WINS] as Double }.average() }
        assertTrue(
            wins >= WeakTarget.SHARE,
            "слабый билд +${WeakTarget.LEAD} ур.: ${"%.0f".format(100 * wins)}% побед (цель ≥ ${"%.0f".format(100 * WeakTarget.SHARE)}%), по актам: " +
                acts.entries.joinToString { (act, it) -> "акт $act ${"%.0f".format(100 * it)}%" },
        )
    }

    private fun actOf(zone: String): Int = index.campaign.regions.indexOfFirst { region -> region.zones.any { it.code.value == zone } } + 1

    private fun guarded(): List<Zone> = index.campaign.zones.filter { it.boss.value.isNotBlank() && index.monster(it.boss) != null }

    /**
     * Бои сторожа стражей - один прогон на оба сторожа: эталон каждого класса - тот же, что в полном прогоне (вещи полосы и штраф
     * сопротивлений своей зоны, её уровень), - бьётся с каждым стражем кампании по [FIGHTS] бою на каждом из [GUARD_ROLLS] наборов
     * костей, кости детерминированы. Старшие уровни первыми: их постройки и бои дольше, параллельный поток меньше ждёт хвоста.
     */
    private object Fights {
        val rows: List<Row> by lazy {
            val index = TestContent.index
            val sim = MobBal(index, fights = FIGHTS * GUARD_ROLLS, rolls = GUARD_ROLLS, monsterSamples = 1, heroes = GuardHeroes.pool)
            val zones = index.campaign.zones.filter { it.boss.value.isNotBlank() && index.monster(it.boss) != null }
            val keys = zones.sortedByDescending { it.level }.flatMap { zone -> sim.classesOf(emptySet()).map { zone to it } }
            fun hero(zone: Zone, heroClass: HeroClass) = sim.hero(heroClass, zone.level, Archetype(campaignBand(zone.level)), index.campaign.resistPenalty(zone.code, false))
            keys.parallelStream().map { (zone, heroClass) -> sim.boss(zone, hero(zone, heroClass), 0, Rarity.COMMON, CAP) }.toList()
        }

        /** Бои слабого билда на [WeakTarget.LEAD] уровней выше каждого стража, чей уровень это допускает. */
        val weak: List<Row> by lazy {
            val index = TestContent.index
            val sim = MobBal(index, fights = FIGHTS * GUARD_ROLLS, rolls = GUARD_ROLLS, monsterSamples = 1, heroes = GuardHeroes.pool)
            val zones = index.campaign.zones.filter { it.boss.value.isNotBlank() && index.monster(it.boss) != null && it.level + WeakTarget.LEAD <= index.classes.maxLevel }
            val keys = zones.sortedByDescending { it.level }.flatMap { zone -> sim.classesOf(emptySet()).map { zone to it } }
            keys.parallelStream().map { (zone, heroClass) ->
                val hero = sim.hero(heroClass, zone.level + WeakTarget.LEAD, Archetype(GearBand.WEAK), index.campaign.resistPenalty(zone.code, false))
                sim.boss(zone, hero, 0, Rarity.COMMON, CAP)
            }.toList()
        }
    }

    private companion object {
        const val ROLLS = 1

        /** Столбец восстановления стража в строке [MobBal.boss]; за ним - урон эталона по стражу. */
        const val RESTORED = 21

        /** Столбец доли побед в строке [MobBal.boss]. */
        const val WINS = 10

        /** Наборов костей стража в стороже побед. */
        const val GUARD_ROLLS = 1
        const val SAMPLES = 1
        const val CAP = 300.0

        /** Боёв на набор костей стража в стороже побед. */
        const val FIGHTS = 1

        /** Допуск стража: по бою на класс - грубая сетка, ловит только явные перекосы. */
        const val EACH = 0.25

        /** Допуск средней акта: 15-20 стражей по бою на класс. */
        const val ACT = 0.12
    }
}
