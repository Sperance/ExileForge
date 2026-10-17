package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.TestContent

/**
 * Эталоны сторожей баланса на прогон тестов: постройка героя - самая дорогая часть сторожа, а эталон уровня зоны кампании
 * тот же на карте своего уровня, в Бездне, Скверне и Разломе - сторожа делят один пул ([HeroPool]) и строят каждого героя один раз.
 */
object GuardHeroes {
    val pool: HeroPool by lazy { HeroPool(MobKit(TestContent.index), cache = null) }
}
