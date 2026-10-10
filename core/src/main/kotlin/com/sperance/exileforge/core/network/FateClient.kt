package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.model.fate.FateView

/**
 * Предначертание аккаунта (4.6.0, сервер `fate`): дар аккаунта или три на выбор и выбор раз и навсегда. Выбор не повторяется:
 * второй ответил бы `FT_001` (уже выбран), не из предложенных - `FT_002`.
 */
class FateClient internal constructor(private val http: Transport) {
    suspend fun view(): FateView = http.get(FATE)

    suspend fun choose(code: String): FateView = http.post("$FATE/choose", mapOf("code" to code))

    private companion object {
        const val FATE = "api/v1/fate"
    }
}
