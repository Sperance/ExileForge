package com.sperance.exileforge.ui.screens.crafts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.HeroCache
import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.crafts.CraftsActions
import com.sperance.exileforge.core.crafts.CraftsModel
import com.sperance.exileforge.core.crafts.CraftsRepository
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.rules.content.SmithChoice
import com.sperance.exileforge.rules.roll.ItemInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Экран ремёсел: ремёсла из репозитория, действия общие; открытое окно профессии - состояние экрана. */
class CraftsViewModel(
    private val actions: CraftsActions,
    repository: CraftsRepository,
    commands: CommandRunner,
    store: HeroCache,
    sessions: SessionRepository,
    slice: GameSlice,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели. */
    val game: StateFlow<GameUi> = slice.ui
    val crafts: StateFlow<Crafts> = repository.state
    val activity: StateFlow<Activity> = commands.state

    private val model = CraftsModel(store, sessions, viewModelScope)

    /**
     * Вещи героя, что сделала идущая работа ([com.sperance.exileforge.core.model.crafts.WorkView.made]): ищутся вне главного потока, когда
     * сменилась работа или вещи героя; работы нет - пусто.
     */
    val made: StateFlow<List<ItemInstance>> = combine(
        crafts.map { it.state?.work }.distinctUntilChanged(),
        game.map { it.hero }.distinctUntilChanged { a, b -> a?.items === b?.items && a?.overflow === b?.overflow },
    ) { work, hero -> work?.made(hero?.items.orEmpty() + hero?.overflow.orEmpty()).orEmpty() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SHARE_STOP_MS), emptyList())

    /** Код профессии, чьё окно открыто; пусто - плитки. */
    val profession: StateFlow<String> = model.profession

    fun openProfession(code: String) = model.openProfession(code)

    fun load(silent: Boolean = false, fresh: Boolean = false) = actions.load(silent, fresh)
    fun watch(on: Boolean) = actions.watch(on)
    fun start(job: String, choice: String = "", additives: List<String> = emptyList()) = actions.start(job, choice, additives)
    fun stop() = actions.stop()
    fun resume() = actions.resume()
    fun equipTool(itemId: String) = actions.equipTool(itemId)

    /** Лист «Пока вас не было» закрыт. */
    fun seen() = actions.seen()

    /** Последний запущенный выбор кузнеца героя на этом устройстве: лист открывается в его режиме. */
    suspend fun smithChoice(heroId: String): SmithChoice? = model.smithChoice(heroId)

    fun rememberSmithChoice(heroId: String, choice: SmithChoice) = model.rememberSmithChoice(heroId, choice)

    /** Последний запущенный уровень карты работы [job] героя на этом устройстве. */
    suspend fun chartLevel(heroId: String, job: String): Int? = model.chartLevel(heroId, job)

    fun rememberChartLevel(heroId: String, job: String, level: Int) = model.rememberChartLevel(heroId, job, level)
}

/** Сколько счёт сделанного живёт без экрана: поворот экрана не начинает его заново. */
private const val SHARE_STOP_MS = 5_000L
