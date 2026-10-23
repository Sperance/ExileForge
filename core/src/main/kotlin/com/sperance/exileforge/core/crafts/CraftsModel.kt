package com.sperance.exileforge.core.crafts

import com.sperance.exileforge.core.app.HeroCache
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.rules.content.SmithChoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Ремёсла на экране: открытое окно профессии, последний выбор кузнеца и уровни карт картографа героя на этом устройстве. */
class CraftsModel(
    private val store: HeroCache,
    private val sessions: SessionRepository,
    /** Область экрана: выбор кузнеца пишется, пока он жив. */
    private val scope: CoroutineScope,
) {
    private val mutableProfession = MutableStateFlow("")

    /** Код профессии, чьё окно открыто; пусто - плитки. */
    val profession: StateFlow<String> = mutableProfession

    fun openProfession(code: String) {
        mutableProfession.value = code
    }

    /** Последний запущенный выбор кузнеца героя на этом устройстве: лист открывается в его режиме. */
    suspend fun smithChoice(heroId: String): SmithChoice? = SmithChoice.of(store.smithChoice(sessions.state.value.server, heroId).orEmpty())

    fun rememberSmithChoice(heroId: String, choice: SmithChoice) {
        val server = sessions.state.value.server
        scope.launch { store.saveSmithChoice(server, heroId, choice.name) }
    }

    /** Последний запущенный уровень карты работы картографа [job] героя на этом устройстве: лист работы открывается на нём. */
    suspend fun chartLevel(heroId: String, job: String): Int? = store.chartLevel(sessions.state.value.server, heroId, job)

    fun rememberChartLevel(heroId: String, job: String, level: Int) {
        val server = sessions.state.value.server
        scope.launch { store.saveChartLevel(server, heroId, job, level) }
    }
}
