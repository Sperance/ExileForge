package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.session.Reach
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.StartGate
import com.sperance.exileforge.core.session.WireFit
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

/**
 * Окно запуска глазами модели: связь, сколько идёт старт и отпущен ли он ([released], на весь процесс, а не на
 * экран) - по ним [StartGate] решает, держать ли окно; [fit] - провод ответившего сервера против этой сборки.
 */
data class StartWindow(val reach: ReachState = ReachState(), val elapsedMs: Long = 0, val released: Boolean = false) {
    val fit: WireFit? get() = reach.wire?.let(WireFit::of)
}

/**
 * Связь с сервером на старте: манифест сервера спрашивается сразу, а не ответил - каждые [RETRY_MS] снова или по
 * «Повторить сейчас». Ответ - провод сервера для проверки обновлений и знак окну запуска уйти; связь, пропавшая на старте,
 * дальше восстанавливается сама ([ConnectionActions.wake]).
 */
class ServerReach(
    private val connection: ServerConnection,
    private val lazyLink: Lazy<ConnectionActions>,
    private val scope: CoroutineScope,
    private val trace: StartupTrace,
    private val foreground: Foreground,
) {
    private val mutable = MutableStateFlow(ReachState())
    val state: StateFlow<ReachState> = mutable.asStateFlow()
    private val nudge = Channel<Unit>(Channel.CONFLATED)
    private var started = false
    private val elapsed = MutableStateFlow(0L)
    private val released = MutableStateFlow(false)
    private val awaitNudge = Channel<Unit>(Channel.CONFLATED)

    /** Окно запуска как его решает модель, а не экран: связь и время старта, тиком до предела [StartGate.LIMIT_MS]. */
    val window: StateFlow<StartWindow> = combine(state, elapsed, released, ::StartWindow).stateIn(scope, SharingStarted.Eagerly, StartWindow())

    /** Старт отпущен: окно старта больше не возвращается - ни экраном, ни после пересоздания Activity. */
    fun release() {
        released.value = true
    }

    /** В фоне манифест не спрашивается: повтор - по возврату в игру. */
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
                    awaitFit()
                    return@launch
                }
                failed = true
                mutable.value = ReachState(Reach.UNREACHABLE, retryAt = System.currentTimeMillis() + RETRY_MS)
                withTimeoutOrNull(RETRY_MS) { nudge.receive() }
                foreground.awaitFront()
                mutable.update { it.copy(reach = Reach.CONNECTING) }
            }
        }
    }

    /** «Повторить сейчас». */
    fun retry() {
        nudge.trySend(Unit)
    }

    /**
     * Провод сервера не совпал с этой сборкой: манифест перечитывается каждые [AWAIT_POLL_MS] или по «Проверить сейчас»
     * ([checkFit]), пока провода не сойдутся - сервер, что старше сборки, обновится сам; сборку новее ищет `UpdateModel`.
     * Сошлись - игра снова входит в сессию ([fitted]).
     */
    private suspend fun awaitFit() {
        if (WireFit.of(state.value.wire ?: return) == WireFit.MATCH) return
        while (state.value.wire?.let(WireFit::of) != WireFit.MATCH) {
            mutable.update { it.copy(retryAt = System.currentTimeMillis() + AWAIT_POLL_MS) }
            withTimeoutOrNull(AWAIT_POLL_MS) { awaitNudge.receive() }
            foreground.awaitFront()
            freshWire()
        }
        fitted.trySend(Unit)
    }

    /** «Проверить сейчас» при несовпавшем проводе. */
    fun checkFit() {
        awaitNudge.trySend(Unit)
    }

    /** Провода сошлись после ожидания: сессия, что не вошла с чужим сервером, входит снова. */
    val fitted = Channel<Unit>(Channel.CONFLATED)

    /**
     * Провод сервера сейчас: манифест спрашивается заново - сервер мог смениться, пока игра была в фоне, и провод старта
     * устарел. Ответ обновляет [state] (после старта); не ответил за [FRESH_MS] - прежний провод, null - сервер ни разу не
     * ответил.
     */
    suspend fun freshWire(): Wire? {
        val api = connection.current.value ?: return state.value.wire
        val wire = try {
            withTimeoutOrNull(FRESH_MS) { api.liveManifest() }?.let { Wire(it.revision, it.rules) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (wire != null) mutable.update { if (it.reach == Reach.ANSWERED) it.copy(wire = wire) else it }
        return wire ?: state.value.wire
    }

    /** Провод сервера, как только он ответит; null - не ответил за [wait]. */
    suspend fun wire(wait: Long): Wire? = withTimeoutOrNull(wait) { state.first { it.reach == Reach.ANSWERED }.wire }

    private companion object {
        const val RETRY_MS = 10_000L

        /** Дольше этого свежий провод не ждётся: проверка обновления идёт с прежним. */
        const val FRESH_MS = 10_000L
        const val TICK_MS = 500L

        /** Как часто перечитывается провод сервера, пока он не совпал со сборкой. */
        const val AWAIT_POLL_MS = 30_000L
    }
}
