package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.deviceLanguage
import com.sperance.exileforge.core.network.refusalLine
import com.sperance.exileforge.core.session.Buzz
import com.sperance.exileforge.core.session.Buzzes
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.ConnectionEventsHub
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.core.world.WorldLoader
import com.sperance.exileforge.rules.content.RULES_VERSION
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Запуск приложения (из `ForgeRuntime`): связи ядра с сервисами приложения, поход и испытание, тихое
 * перечитывание героя по чужой команде и вход - быстрый с устройства, по сохранённой сессии или по устройству.
 */
class AppStartup(
    repositories: Repositories,
    actions: Actions,
    commands: CommandRunner,
    connection: ServerConnection,
    store: DeviceStore,
    scope: CoroutineScope,
    private val connectionHub: ConnectionEventsHub,
    private val buzzer: Buzzes,
    private val prefs: PreferencesRepository,
    private val content: ContentLoader,
    private val loader: WorldLoader,
    private val events: GameEvents,
    private val sessionActions: SessionActions,
    private val connectionActions: ConnectionActions,
    private val trace: StartupTrace,
    /** Зависания прошлых запусков, что записал сторож главного потока. */
    private val stalls: LeftReports,
    /** Падения прошлых запусков. */
    private val crashes: LeftReports,
    private val reach: ServerReach,
) : AppService(repositories, actions, commands, connection, store, scope) {
    /**
     * Запуск после сборки: отражения репозиториев и старт сессии. Из конструктора корутины не запускаются -
     * R8 в release-сборке переносил запись полей конструктора ниже лямбд, что их читали, и приложение падало на старте.
     */
    private var started = false

    /**
     * Герой, изменившийся на сервере, перечитывается тихо одним ключом с прочими чтениями - лишь если ответ команды не принёс снимка, отказ
     * остаётся команде. Мир других правил не открывается: сохранённое на устройстве уходит раньше, чем его кто-то прочтёт. Сервер,
     * обновившийся, пока окно запуска ждало, получает вход снова.
     */
    fun start() {
        if (started) return
        started = true
        connectionHub.delegate = connectionActions
        expedition.start()
        trial.start()
        buzzer.allowed = { kind -> prefs.settings.value.let { if (kind == Buzz.DANGER) it.buzzDanger else it.buzzButtons } }
        content.delegate = { fresh -> loader.ensureContent(fresh) }
        scope.launch { events.heroChanged.collect { heroSync.refreshIfStale() } }
        scope.launch {
            try {
                languages.set(Lang.byCode(store.language.first()) ?: deviceLanguage())
                val server = store.server.first()
                store.forgetWorldUnless(RULES_VERSION)
                api = sessionActions.newApi(server)
                sessionActions.apiReady.complete(Unit)
                reportLeft(stalls)
                reportLeft(crashes)
                val known = store.languages(server).mapNotNull { Lang.byCode(it) }
                sessions.update { it.copy(server = server) }
                world.update { it.copy(languages = known.ifEmpty { it.languages }) }
                commands.ready()
                loader.refreshLocale()
                loader.refreshIcons()
                join(server)
                scope.launch { reach.fitted.receiveAsFlow().collect { join(server) } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                api = sessionActions.newApi(DEFAULT_SERVER)
                sessionActions.apiReady.complete(Unit)
                commands.ready()
                commands.refuse(Phrase { refusalLine(e) })
            } finally {
                trace.started()
            }
        }
    }

    /**
     * Вход: быстрый с устройства, по сохранённой сессии или по устройству; сессии нет - ничего.
     *
     * Быстрый старт: последний герой с устройства сразу, сессия подтверждается за ним.
     */
    private suspend fun join(server: String) {
        val saved = trace.step(StartStage.SESSION, "start.step.token") { store.token(server) }
        if (saved != null) {
            if (!sessionActions.fastStart(server, saved)) sessionActions.resume(saved)
        } else if (store.deviceSession.first()) {
            sessionActions.playOnThisDevice(silent = true)
        } else {
            trace.note(StartStage.SESSION, "start.step.no_session")
        }
    }

    /**
     * Прошлые запуски зависали или падали ([left]): устаревшие записи стираются ([LeftReports.sweep]), каждый неотправленный отчёт уходит
     * баг-репортом в фоне, отправленный стирается. Ждёт
     * входа - тогда он подписан аккаунтом и героем; вход не случился за [STALL_REPORT_WAIT_MS] - уходит без них, с устройством и
     * версией. Не ушёл - останется до следующего запуска.
     */
    private fun reportLeft(left: LeftReports) {
        scope.launch {
            try {
                left.sweep()
                val pending = left.pending().ifEmpty { return@launch }
                withTimeoutOrNull(STALL_REPORT_WAIT_MS) { sessions.state.first { it.signedIn } }
                pending.forEach { report ->
                    api.reportBug(report.report)
                    report.sent()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { }
        }
    }
}

/** Дольше этого отчёт о прошлом зависании не ждёт входа, мс. */
private const val STALL_REPORT_WAIT_MS = 30_000L
