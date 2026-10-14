package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.TestContent
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
        val sim = MobBal(index, fights = 1, rolls = ROLLS, monsterSamples = SAMPLES, cache = null)
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

    private companion object {
        const val ROLLS = 2
        const val SAMPLES = 3
    }
}
