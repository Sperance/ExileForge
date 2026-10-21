package com.sperance.exileforge.core.network

import com.sperance.exileforge.rules.content.ContestBoard
import com.sperance.exileforge.rules.content.ContestKind
import com.sperance.exileforge.rules.content.ContestTable

/**
 * Состязания: оба нынешних состязания героя и таблица лиги - в пределах его игрового сервера. Отказы - [ApiFailure] (`BRT_003`
 * неизвестный вид или лига не числом, `CP_001` лига вне правил).
 */
class ContestClient internal constructor(private val http: Transport) {
    /** Здание: сутки и неделя в лиге героя. */
    suspend fun board(heroId: String): ContestBoard = http.get("api/v1/contests", heroQuery(heroId))

    /** Таблица лиги [league] (null - лига героя) состязания вида [kind]. */
    suspend fun table(heroId: String, kind: ContestKind, league: Int?): ContestTable = http.get(
        "api/v1/contests/table",
        heroQuery(heroId, "kind" to kind.name, "league" to league?.toString()),
    )
}
