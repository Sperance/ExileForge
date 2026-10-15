package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.TestContent
import kotlin.math.exp
import kotlin.math.ln
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Сторож классов (0.14.0, SKILL-02): урон эталона каждого класса в настоящем бою клиента - набор умений подобран боем
 * ([LoadoutTuner]: пары «накопитель + трата», поддержка), вещи редкие и смесь с уникалками - держится в ±35% среднего
 * геометрического классов на уровнях [LEVELS]. Заменяет сторож ±20% по Мощи атак (ASANA-17). Полный отчёт с победами над стражами -
 * `:core:mobBal -Pmode=classes`.
 */
class ClassBalanceGuardTest {
    private val index = TestContent.index

    @Test
    fun everyClassDealsWithinAThirdOfTheMean() {
        val sim = MobBal(index, fights = 1, rolls = 1, monsterSamples = 1, cache = null)
        val classes = sim.classesOf(emptySet())
        val keys = LEVELS.flatMap { level -> classes.flatMap { c -> SOURCES.map { Triple(level, c, it) } } }
        val damage = keys.parallelStream().map { (level, c, source) -> Triple(level, c.code, sim.classDamage(c, level, source).second.score) }.toList()
        assertTrue(classes.size >= 7 && damage.size == keys.size, "the check proves nothing: ${damage.size} of ${keys.size}")
        val out = damage.groupBy { it.first }.flatMap { (level, rows) ->
            val byClass = rows.groupBy { it.second }.mapValues { (_, r) -> exp(r.map { ln(it.third.coerceAtLeast(1.0)) }.average()) }
            val mean = exp(byClass.values.map { ln(it) }.average())
            byClass.filterValues { it / mean !in BAND }.map { (code, value) -> "$code@$level ${"%.2f".format(value / mean)}" }
        }
        assertTrue(out.isEmpty(), "классы вне ±35% урона среднего: $out")
    }

    private companion object {
        val LEVELS = listOf(20, 40, 60, 80)
        val SOURCES = listOf(GearSource.RARE, GearSource.MIXED)
        val BAND = 0.65..1.35
    }
}
