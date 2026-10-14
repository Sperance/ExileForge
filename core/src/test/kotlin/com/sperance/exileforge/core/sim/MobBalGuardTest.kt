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

    /**
     * Сторож каждого стража (MOB-BAL-GUARD, быстрый вариант полного прогона `:core:mobBal -Pmode=guard`): эталоны всех классов бьются
     * с каждым стражем кампании - по бою на каждом из [ROLLS] бросков босса, кости детерминированы. Вещи эталона ищутся на сетке уровней
     * [GEAR_GRID] (поиск вещей дороже боёв) и переносятся на уровень зоны ([MobKit.relevel]); полоса вещей и штраф сопротивлений - своей
     * зоны. Медиана времени победы стража держится в коридоре своего акта с допуском выборки [TOLERANCE] (полный прогон - 0,5-2×).
     */
    @Test
    fun everyGuardianStaysInItsActCorridor() {
        val sim = MobBal(index, fights = ROLLS, rolls = ROLLS, monsterSamples = 1, cache = null)
        val zones = index.campaign.zones.filter { it.boss.value.isNotBlank() && index.monster(it.boss) != null }
        // Старшие уровни первыми: их постройки и бои дольше, параллельный поток так меньше ждёт хвоста
        val keys = zones.sortedByDescending { it.level }.flatMap { zone -> sim.classesOf(emptySet()).map { zone to it } }
        fun penalty(zone: Zone) = index.campaign.resistPenalty(zone.code, false)
        fun gear(zone: Zone, heroClass: HeroClass) = sim.hero(heroClass, gearLevel(zone.level), Archetype(campaignBand(zone.level)), penalty(zone))
        keys.distinctBy { (zone, heroClass) -> listOf(heroClass.code, gearLevel(zone.level), campaignBand(zone.level), penalty(zone)) }
            .parallelStream().forEach { (zone, heroClass) -> gear(zone, heroClass) }
        val rows = keys.parallelStream().map { (zone, heroClass) -> sim.boss(zone, sim.kit.relevel(gear(zone, heroClass), zone.level), 0, Rarity.COMMON, CAP) }.toList()
        val verdicts = guard(index, rows, TOLERANCE)
        assertTrue(zones.size >= 100 && verdicts.size == zones.size, "the check proves nothing: ${verdicts.size} of ${zones.size} guardians")
        val out = verdicts.filter { it.values.last() == false }
        assertTrue(out.isEmpty(), "стражи вне коридора акта ($TOLERANCE медианы): " + out.joinToString("; ") { it.tsv() })
    }

    /** Уровень поиска вещей для зоны уровня [level]: старший узел [GEAR_GRID] не выше него - вещи отстают от зоны не больше шага сетки. */
    private fun gearLevel(level: Int): Int = GEAR_GRID.last { it <= level }

    private companion object {
        const val ROLLS = 2
        const val SAMPLES = 3
        const val CAP = 300.0

        /** Сетка уровней поиска вещей: шаг ~10% уровня (1, 2, ... 10, 12, 14, ... 84, 93). */
        val GEAR_GRID: List<Int> = generateSequence(1) { (it * 1.1).toInt() + 1 }.takeWhile { it <= 100 }.toList()

        /** Допуск выборки (2 боя на класс, вещи сетки): на контенте 0.13.2 доли стражей 0,40-2,49 медианы акта. */
        val TOLERANCE = 0.33..3.0
    }
}
