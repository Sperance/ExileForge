package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.model.fate.FateView

/**
 * Предначертание аккаунта (сервер `fate`): дар аккаунта или три на выбор и выбор раз и навсегда. Выбор повторяется только
 * под своим ключом ([SafeRetry]) - сервер отдаст первый ответ; второй выбор ответил бы `FT_001` (уже выбран), не из предложенных - `FT_002`.
 * Сменить дар можно «Колодой Рока» (раздача и выбор - стопкой героя: `hero/bag/deal`, `hero/bag/use`), а модератору и аккаунту с
 * героем-тестировщиком - и себе любым даром ([redrawOwn]).
 */
class FateClient internal constructor(private val http: Transport) {
    suspend fun view(): FateView = http.get(FATE)

    suspend fun choose(code: String): FateView = http.post("$FATE/choose", mapOf("code" to code))

    /** Свой дар [code] - модератору и аккаунту с героем-тестировщиком; ответ - дар, что теперь у аккаунта. */
    suspend fun redrawOwn(code: String): FateView = http.post("$FATE/self", mapOf("code" to code))

    private companion object {
        const val FATE = "api/v1/fate"
    }
}
