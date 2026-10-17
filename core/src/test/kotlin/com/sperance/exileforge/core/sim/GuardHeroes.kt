package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.TestContent
import java.io.File

/**
 * Эталоны сторожей баланса на прогон тестов: постройка героя - самая дорогая часть сторожа, а эталон уровня зоны кампании
 * тот же на карте своего уровня, в Бездне, Скверне и Разломе - сторожа делят один пул ([HeroPool]) и строят каждого героя один раз.
 * Задача `:core:balanceGuard` отдаёт папку дискового кэша (`guardHeroes`) и отпечаток кода (`guardCode`): эталоны лежат в папке
 * ключа [HeroCacheKey] и переживают прогон, пока не сдвинулись код и контент постройки (папка ключа помечается временем прогона - CI
 * сохраняет только её); без папки - строятся заново, ничего не пишут.
 */
object GuardHeroes {
    val pool: HeroPool by lazy {
        val cache = System.getProperty(CACHE_DIR)?.let { root -> HeroCacheKey.dir(File(root), System.getProperty(CODE).orEmpty(), TestContent.contentDir, TestContent.index) }
        cache?.apply { mkdirs() }?.setLastModified(System.currentTimeMillis())
        HeroPool(MobKit(TestContent.index), cache = cache)
    }

    private const val CACHE_DIR = "guardHeroes"
    private const val CODE = "guardCode"
}
