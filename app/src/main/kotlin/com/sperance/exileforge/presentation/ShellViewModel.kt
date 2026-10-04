package com.sperance.exileforge.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.campaign.LogKind
import com.sperance.exileforge.core.campaign.TrialArena
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.model.sync.StaticManifest
import com.sperance.exileforge.presentation.features.Warmup
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.Building
import com.sperance.exileforge.presentation.state.Buzz
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.StashSort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Оболочка приложения (3.80.30): вкладки и здания по прежнему номеру, «Настройки» поверх экрана, строка в тосты.
 * Пока над `ForgeRuntime` ради проверки административных вкладок; уедет вместе с ним.
 */
class ShellViewModel(private val runtime: ForgeRuntime, slice: GameSlice) : ViewModel() {
    init {
        // Рантайм запускается один раз, кто бы из оболочки или активности ни попросил первым.
        runtime.start()
    }

    /** Верх стека навигатора: фаза, вкладка, здание (3.80.40). */
    val route: StateFlow<Route> = runtime.navigator.current

    /** Прогрев героя, поход и испытание - состояния игры поверх стека. */
    val warmup: StateFlow<Warmup?> = runtime.warmupViewModel.state
    val run: StateFlow<ExpeditionRun?> = runtime.expedition.run
    val arena: StateFlow<TrialArena?> = runtime.trial.arena
    val logs = runtime.logs

    /** Что телефон отзывает вибрацией (3.77.0), уже по настройкам. */
    val buzzes: SharedFlow<Buzz> get() = runtime.buzzes

    fun warmUp() = runtime.warmupViewModel.start()

    /** Отчёт жука (3.48.0): уходит сразу, со входом или без; [onSent] - когда сервер его принял. */
    fun reportBug(report: BugReportRequest, onSent: suspend () -> Unit = {}) = runtime.reportBug(report, onSent)

    /** Срез «игра» для экранов этой модели (3.80.33). */
    val game: StateFlow<GameUi> = slice.ui

    /** Вкладка по прежнему номеру; закрытую уровнем героя навигатор не откроет и скажет, с какого. */
    fun tab(tab: Int) = runtime.tab(tab)

    /** Здание Города (3.22.0) или площадь для null. */
    fun building(building: Building?) {
        runtime.commands.dismissMessage()
        runtime.navigator.tab(Route.ofBuilding(building))
    }

    /** «Настройки» (3.77.0) поверх открытой вкладки; закрытие возвращает на неё. */
    fun openSettings() {
        runtime.commands.dismissMessage()
        runtime.navigator.open(Route.Settings)
    }

    fun closeSettings() = runtime.navigator.back()

    /** Проба связи сразу (3.30.0): нажата иконка «не в сети». */
    fun retryLink() = runtime.connectionViewModel.wake(now = true)

    /** Строка в тосты с экранов (3.76.0: место, открытое уровнем). */
    fun announce(text: String) = runtime.toast(text)

    fun dismissMessage() = runtime.commands.dismissMessage()
    fun dismissNotice() = runtime.notices.dismiss()
    fun buzz(kind: Buzz) = runtime.buzz(kind)
    fun clearLogs() = runtime.journal.clear()

    /** Настройки устройства, в хранилище устройства: порядок сундука и «скрыть надетое» (3.30.0, 3.69.0), фильтр журнала боя. */
    fun stashSort(sort: StashSort) {
        viewModelScope.launch { runtime.prefs.saveStashSort(sort) }
    }

    fun stashHideWorn(hide: Boolean) {
        viewModelScope.launch { runtime.prefs.saveStashHideWorn(hide) }
    }

    fun logFilter(kinds: Set<LogKind>) {
        viewModelScope.launch { runtime.prefs.saveLogFilter(kinds) }
    }

    // ---- жизненный цикл активности (3.80.42: из удалённой общей модели) ----

    /** Возврат на передний план: связь восстанавливается без нажатия. */
    fun reconnect() = runtime.sessionViewModel.reconnect()

    /** Уход с переднего плана: момент запомнен, чтобы долгое отсутствие обновило экран на возврате. */
    fun away() = runtime.sessionViewModel.away()

    /** Журнал похода уходит сразу: процесс, убитый в фоне, не унесёт его с собой. */
    fun flushRun() = runtime.expedition.flushRun()

    /** Вход встретил сервер новее сборки (3.74.0): проверка обновлений идёт сразу. */
    val newerServer: Flow<Unit> get() = runtime.newerServer

    /** Манифест живого сервера для проверки обновлений (3.72.0); null, пока сервер не отвечает. */
    suspend fun serverManifest(): StaticManifest? = try {
        // 3.74.0: после того как у приложения есть сервер, и как его отдаёт он сейчас - манифест старой выкладки прятал обновление.
        withTimeoutOrNull(API_WAIT_MS) { runtime.apiReady.await() }
        runtime.api.liveManifest()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private companion object {
        /** Сколько проверка обновлений ждёт, пока у приложения появится сервер. */
        const val API_WAIT_MS = 10_000L
    }
}
