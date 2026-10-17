package com.sperance.exileforge.core.hero

import com.sperance.exileforge.core.model.hero.PlayerCard
import com.sperance.exileforge.core.model.hero.UniqueFind
import com.sperance.exileforge.core.session.Remembered
import com.sperance.exileforge.core.session.ServerConnection
import kotlinx.coroutines.CancellationException

/**
 * Чтения героя вне снимка: находки уникалок («История» и «Уникальные предметы») и статистика летописи - общие
 * экранам и свежие [com.sperance.exileforge.core.session.READ_FRESH_MS]; null - чтение не удалось.
 */
class HeroReads(private val connection: ServerConnection) {
    private val uniques = Remembered<Map<String, UniqueFind>>()
    private val stats = Remembered<Map<String, Long>>()
    private val card = Remembered<PlayerCard>()

    suspend fun uniques(heroId: String): Map<String, UniqueFind>? = quietly { uniques.get(heroId) { connection.api.hero.uniques(heroId).finds } }

    suspend fun stats(heroId: String): Map<String, Long>? = quietly { stats.get(heroId) { connection.api.hero.stats(heroId).values } }

    /** Своя карточка игрока - тот же запрос карточки, где цель - сам герой: место на доске Мощи для своей сводки. */
    suspend fun ownCard(heroId: String): PlayerCard? = quietly { card.get(heroId) { connection.api.hero.card(heroId, heroId) } }

    private suspend fun <T> quietly(read: suspend () -> T): T? = try {
        read()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}
