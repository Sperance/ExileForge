package com.sperance.exileforge.presentation.hero

import com.sperance.exileforge.core.model.hero.UniqueFind
import com.sperance.exileforge.core.session.Remembered
import com.sperance.exileforge.core.session.ServerConnection
import kotlinx.coroutines.CancellationException

/**
 * Чтения героя вне снимка (3.94.1): находки уникалок («История» и «Уникальные предметы») и статистика летописи - общие
 * экранам и свежие [com.sperance.exileforge.core.session.READ_FRESH_MS]; null - чтение не удалось.
 */
class HeroReads(private val connection: ServerConnection) {
    private val uniques = Remembered<Map<String, UniqueFind>>()
    private val stats = Remembered<Map<String, Long>>()

    suspend fun uniques(heroId: String): Map<String, UniqueFind>? = quietly { uniques.get(heroId) { connection.api.hero.uniques(heroId).finds } }

    suspend fun stats(heroId: String): Map<String, Long>? = quietly { stats.get(heroId) { connection.api.hero.stats(heroId).values } }

    private suspend fun <T> quietly(read: suspend () -> T): T? = try {
        read()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}
