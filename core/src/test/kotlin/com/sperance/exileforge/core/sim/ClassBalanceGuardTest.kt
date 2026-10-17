package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.TestContent
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.Rarity
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Сторож классов: эталон каждого класса в настоящем бою клиента - набор умений подобран боем ([LoadoutTuner]: пары
 * «накопитель + трата», поддержка), вещи редкие и смесь с уникалками - на уровнях [LEVELS]. Урон держится в ±35% среднего
 * геометрического классов (заменяет сторож ±20% по Мощи атак); и доля побед над стражами ближайших зон ([CLASS_BOSSES])
 * в [WIN_BAND] от средней классов (оборона классов). Оба допуска - цели баланса владельца (RULES «Общее», «Цели баланса»),
 * сторож держит их на широкой выборке: [GEAR_SEEDS] набора вещей на источник и четыре боя на стража - по бою на каждый из
 * четырёх бросков; один набор вещей решал класс своими костями сильнее его чисел. Эталоны строятся один раз на оба сторожа (общий пул
 * [GuardHeroes]). Полный отчёт - `:core:mobBal -Pmode=classes -PgearSeeds=3` (10-88 ур.).
 */
class ClassBalanceGuardTest {
    @Test
    fun everyClassDealsWithinAThirdOfTheMean() {
        val out = SAMPLE.groupBy { it.level }.flatMap { (level, rows) ->
            val byClass = rows.groupBy { it.code }.mapValues { (_, r) -> exp(r.map { ln(it.damage.coerceAtLeast(1.0)) }.average()) }
            val mean = exp(byClass.values.map { ln(it) }.average())
            byClass.filterValues { it / mean !in BAND }.map { (code, value) -> "$code@$level ${"%.2f".format(value / mean)}" }
        }
        assertTrue(out.isEmpty(), "классы вне ±35% урона среднего: $out")
    }

    @Test
    fun everyClassWinsCloseToTheMean() {
        val out = SAMPLE.groupBy { it.level }.flatMap { (level, rows) ->
            val byClass = rows.groupBy { it.code }.mapValues { (_, r) -> r.map { it.wins }.average() }
            val mean = byClass.values.average()
            byClass.filterValues { abs(it - mean) > WIN_BAND }.map { (code, wins) -> "$code@$level ${"%+.0f".format(100 * (wins - mean))} п.п. (среднее ${"%.0f".format(100 * mean)}%)" }
        }
        assertTrue(out.isEmpty(), "классы вне ±${(100 * WIN_BAND).toInt()} п.п. побед над стражами от среднего: $out")
    }

    /** Эталон выборки: уровень, класс, источник вещей и набор вещей. */
    private data class Key(val level: Int, val heroClass: HeroClass, val source: GearSource, val seed: Int)

    /** Эталон класса [code] уровня [level] одного источника и набора вещей: урон в бою и доля побед над стражами. */
    private data class Sample(val level: Int, val code: String, val damage: Double, val wins: Double)

    private companion object {
        val LEVELS = listOf(20, 40, 60, 80)
        val SOURCES = listOf(GearSource.RARE, GearSource.MIXED)
        val BAND = 0.65..1.35

        /** Наборов вещей на источник: урон класса - среднее геометрическое, победы - среднее по всем наборам. */
        const val GEAR_SEEDS = 3

        /** Допуск побед класса от средней классов уровня, доля: цель баланса. */
        const val WIN_BAND = 0.30

        /** Бросков стража и боёв на все броски (по бою на бросок: строки броска решают бой сильнее костей), выборка та же на каждом прогоне. */
        const val ROLLS = 4
        const val FIGHTS = 4
        const val CAP = 300.0

        val SAMPLE: List<Sample> by lazy {
            val index = TestContent.index
            val sim = MobBal(index, fights = FIGHTS, rolls = ROLLS, monsterSamples = 1, heroes = GuardHeroes.pool)
            val classes = sim.classesOf(emptySet())
            val keys = LEVELS.flatMap { level -> classes.flatMap { c -> SOURCES.flatMap { source -> (0 until GEAR_SEEDS).map { Key(level, c, source, it) } } } }
            val rows = keys.parallelStream().map { (level, c, source, seed) ->
                val (hero, damage) = sim.classDamage(c, level, source, seed)
                val wins = sim.nearZones(level, CLASS_BOSSES).map { sim.boss(it, hero, 0, Rarity.COMMON, CAP).values[WINS] as Double }.average()
                Sample(level, c.code, damage.score, wins)
            }.toList()
            check(classes.size >= 7 && rows.size == keys.size) { "the check proves nothing: ${rows.size} of ${keys.size}" }
            rows
        }

        /** Столбец доли побед в строке [MobBal.boss]. */
        const val WINS = 10
    }
}
