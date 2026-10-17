package com.sperance.exileforge.core.network

import com.sperance.exileforge.rules.content.HallBoard
import com.sperance.exileforge.rules.content.HallTable

/**
 * Доска славы: любая таблица реестра [HallBoard] одним чтением от имени героя - в пределах его
 * игрового сервера. Отказы - [ApiFailure] (`BRT_003` неизвестная таблица, `CP_001` чужой раздел).
 */
class HallClient internal constructor(private val http: Transport) {
    /** Таблица [board] раздела [scope] (раш - `регион:ступень`, профессия - её код) на игровом сервере героя. */
    suspend fun table(heroId: String, board: HallBoard, scope: String = ""): HallTable = http.get(
        "api/v1/hall/table",
        heroQuery(heroId, "board" to board.name, "scope" to scope.takeIf { it.isNotEmpty() }),
    )
}
