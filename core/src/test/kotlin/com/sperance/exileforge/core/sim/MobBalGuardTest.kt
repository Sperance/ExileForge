package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.TestContent
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Zone
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Сторож темпа MOB-BAL (В-11/12, быстрый, на выборке): эталонный герой полосы своего уровня ([campaignBand]) всех классов против
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
     * Сторож стражей (0.15.0, решения 1 и 25; быстрый вариант полного прогона `:core:mobBal -Pmode=guard`): эталон каждого класса - тот
     * же, что в полном прогоне (вещи полосы и штраф сопротивлений своей зоны, её уровень), - бьётся с каждым стражем кампании по [FIGHTS]
     * бою на каждом из [GUARD_ROLLS] наборов костей, кости детерминированы. Доля побед эталонов над стражем своего уровня держит цель
     * [GuardianTarget]: каждый страж - с допуском [EACH], средняя акта - с допуском [ACT]. Разброс стражей сторожит бой, а не Мощь.
     */
    @Test
    fun everyGuardianMeetsTheWinTarget() {
        val sim = MobBal(index, fights = FIGHTS * GUARD_ROLLS, rolls = GUARD_ROLLS, monsterSamples = 1, heroes = GuardHeroes.pool)
        val zones = index.campaign.zones.filter { it.boss.value.isNotBlank() && index.monster(it.boss) != null }
        // Старшие уровни первыми: их постройки и бои дольше, параллельный поток так меньше ждёт хвоста
        val keys = zones.sortedByDescending { it.level }.flatMap { zone -> sim.classesOf(emptySet()).map { zone to it } }
        fun hero(zone: Zone, heroClass: HeroClass) = sim.hero(heroClass, zone.level, Archetype(campaignBand(zone.level)), index.campaign.resistPenalty(zone.code, false))
        val rows = keys.parallelStream().map { (zone, heroClass) -> sim.boss(zone, hero(zone, heroClass), 0, Rarity.COMMON, CAP) }.toList()
        val verdicts = guard(index, rows, EACH)
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

    private companion object {
        const val ROLLS = 2

        /** Наборов костей стража в стороже побед. */
        const val GUARD_ROLLS = 4
        const val SAMPLES = 3
        const val CAP = 300.0

        /** Боёв на набор костей стража в стороже побед. */
        const val FIGHTS = 1

        /** Допуск стража: 28 боёв на стража (4 на класс) на тех же костях - доля каждого стража подогнана на выборке сторожа. */
        const val EACH = 0.10

        /** Допуск средней акта: 15-20 стражей - разброс выборки ~2 п.п. */
        const val ACT = 0.05
    }
}
