package com.sperance.exileforge.presentation.app

import com.sperance.exileforge.core.session.Reach
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.StartGate
import com.sperance.exileforge.core.update.Wire
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Связь со стартом в миг: где она, провод ответившего сервера и когда следующая попытка. */
data class ReachState(val reach: Reach = Reach.CONNECTING, val wire: Wire? = null, val retryAt: Long = 0)

/** Окно запуска глазами модели (3.87.0): связь и сколько идёт старт - по ним [StartGate] решает, держать ли окно. */
data class StartWindow(val reach: ReachState = ReachState(), val elapsedMs: Long = 0)

/**
 * Связь с сервером на старте (3.86.0): манифест сервера спрашивается сразу, а не ответил - каждые [RETRY_MS] снова или по
 * «Повторить сейчас». Ответ - провод сервера для проверки обновлений и знак окну запуска уйти; связь, пропавшая на старте,
 * дальше восстанавливается сама ([ConnectionActions.wake]).
 */
class ServerReach(
    private val connection: ServerConnection,
    private val lazyLink: Lazy<ConnectionActions>,
    private val scope: CoroutineScope,
    private val trace: StartupTrace,
) {
    private val mutable = MutableStateFlow(ReachState())
    val state: StateFlow<ReachState> = mutable.asStateFlow()
    private val nudge = Channel<Unit>(Channel.CONFLATED)
    private var started = false
    private val elapsed = MutableStateFlow(0L)

    /** Окно запуска как его решает модель, а не экран: связь и время старта, тиком до предела [StartGate.LIMIT_MS]. */
    val window: StateFlow<StartWindow> = combine(state, elapsed, ::StartWindow).stateIn(scope, SharingStarted.Eagerly, StartWindow())

    fun start() {
        if (started) return
        started = true
        val began = System.currentTimeMillis()
        scope.launch {
            while (elapsed.value < StartGate.LIMIT_MS) {
                delay(TICK_MS)
                elapsed.value = System.currentTimeMillis() - began
            }
        }
        scope.launch {
            var failed = false
            while (true) {
                val api = connection.current.filterNotNull().first()
                val wire = try {
                    trace.step(StartStage.VERSION, "start.step.server_manifest") { api.liveManifest() }.let { Wire(it.revision, it.rules) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                if (wire != null) {
                    mutable.value = ReachState(Reach.ANSWERED, wire)
                    if (failed) lazyLink.value.wake(now = true)
                    return@launch
                }
                failed = true
                mutable.value = ReachState(Reach.UNREACHABLE, retryAt = System.currentTimeMillis() + RETRY_MS)
                withTimeoutOrNull(RETRY_MS) { nudge.receive() }
                mutable.update { it.copy(reach = Reach.CONNECTING) }
            }
        }
    }

    /** «Повторить сейчас». */
    fun retry() {
        nudge.trySend(Unit)
    }

    /** Провод сервера, как только он ответит; null - не ответил за [wait]. */
    suspend fun wire(wait: Long): Wire? = withTimeoutOrNull(wait) { state.first { it.reach == Reach.ANSWERED }.wire }

    private companion object {
        const val RETRY_MS = 10_000L
        const val TICK_MS = 500L
    }
}
