package com.sperance.exileforge.presentation.app

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.network.refusalLine
import com.sperance.exileforge.core.session.Buzzes
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.ConnectionEventsHub
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.data.settings.DEFAULT_SERVER
import com.sperance.exileforge.data.settings.PreferencesRepository
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.data.settings.deviceLanguage
import com.sperance.exileforge.presentation.Actions
import com.sperance.exileforge.presentation.Repositories
import com.sperance.exileforge.presentation.state.Buzz
import com.sperance.exileforge.presentation.state.Phrase
import com.sperance.exileforge.presentation.world.WorldLoader
import com.sperance.exileforge.rules.content.RULES_VERSION
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Запуск приложения (3.80.44, из `ForgeRuntime`): связи ядра с сервисами приложения, поход и испытание, тихое
 * перечитывание героя по чужой команде и вход - быстрый с устройства, по сохранённой сессии или по устройству.
 */
class AppStartup(
    repositories: Repositories,
    actions: Actions,
    commands: CommandRunner,
    connection: ServerConnection,
    store: ServerStore,
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
    private val watchdog: StallWatchdog,
) : AppService(repositories, actions, commands, connection, store, scope) {
    /**
     * Запуск после сборки (3.80.12): отражения репозиториев и старт сессии. Из конструктора корутины не запускаются -
     * R8 в release-сборке переносил запись полей конструктора ниже лямбд, что их читали, и приложение падало на старте.
     */
    private var started = false

    fun start() {
        if (started) return
        started = true
        connectionHub.delegate = connectionActions
        expedition.start()
        trial.start()
        buzzer.allowed = { kind -> prefs.settings.value.let { if (kind == Buzz.DANGER) it.buzzDanger else it.buzzButtons } }
        content.delegate = { fresh -> loader.ensureContent(fresh) }
        // Герой изменился на сервере: перечитывается тихо и одним ключом с прочими чтениями - и лишь если ответ команды не
        // принёс снимка (3.94.1); отказ остаётся команде, что его просила.
        scope.launch { events.heroChanged.collect { heroSync.refreshIfStale() } }
        scope.launch {
            try {
                languages.set(Lang.byCode(store.language.first()) ?: deviceLanguage())
                val server = store.server.first()
                // A world of other rules is not opened (3.81.2): what the device kept of it goes before anything reads it.
                store.forgetWorldUnless(RULES_VERSION)
                api = sessionActions.newApi(server)
                sessionActions.apiReady.complete(Unit)
                reportStall()
                val known = store.languages(server).mapNotNull { Lang.byCode(it) }
                sessions.update { it.copy(server = server) }
                world.update { it.copy(languages = known.ifEmpty { it.languages }) }
                commands.ready()
                loader.refreshLocale()
                loader.refreshIcons()
                val saved = trace.step(StartStage.SESSION, "start.step.token") { store.token(server) }
                // The fast start (3.30.0): the last hero from the device at once, the session confirmed behind it.
                if (saved != null) {
                    if (!sessionActions.fastStart(server, saved)) sessionActions.resume(saved)
                } else if (store.deviceSession.first()) {
                    sessionActions.playOnThisDevice(silent = true)
                } else {
                    trace.note(StartStage.SESSION, "start.step.no_session")
                }
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
     * Прошлый запуск завис (3.82.0): отчёт сторожа уходит баг-репортом в фоне, отправленный стирается. Ждёт входа (3.88.0) -
     * тогда он подписан аккаунтом и героем; вход не случился за [STALL_REPORT_WAIT_MS] - уходит без них, с устройством и версией.
     */
    private fun reportStall() {
        scope.launch {
            try {
                val report = watchdog.pending() ?: return@launch
                withTimeoutOrNull(STALL_REPORT_WAIT_MS) { sessions.state.first { it.signedIn } }
                api.reportBug(report)
                watchdog.sent()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { }
        }
    }
}

/** Дольше этого отчёт о прошлом зависании не ждёт входа, мс. */
private const val STALL_REPORT_WAIT_MS = 30_000L
