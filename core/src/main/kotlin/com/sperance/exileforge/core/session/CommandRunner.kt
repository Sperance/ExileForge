package com.sperance.exileforge.core.session

import com.sperance.exileforge.core.i18n.Phrase
import com.sperance.exileforge.core.i18n.phrase
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueued
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.network.refusalLine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/** Что происходит с сетью сейчас: команда в полёте, чтения, последняя беда и её строка. */
data class Activity(
    /** Команда в полёте - единственное, что гасит кнопки. */
    val busy: Boolean = true,
    /** Чтения в полёте по ключам, без тихих. */
    val loading: Set<String> = emptySet(),
    val failure: FailureState? = null,
    val message: Phrase? = null,
    val error: Boolean = false,
) {
    val reading: Boolean get() = loading.isNotEmpty()

    /** Отказ, который печатают там, где нажали; успех не показывают. */
    val refusal: Phrase? get() = message.takeIf { error }
}

/** Куда уходят вести о связи: очередь приняла команду, сервер не отвечает. */
interface ConnectionEvents {
    fun lost(error: Throwable? = null)

    fun queued()
}

/**
 * Шина команд и чтений (3.80.8, прежде внутри ForgeRuntime): команда - одна за раз, только она гасит
 * кнопки; чтение - одно на ключ, никогда не ждёт команду и не держит её. Беда превращается в
 * [Activity.failure] и строку для игрока; ушедшая в очередь команда - не беда вовсе.
 */

/** Вести о связи уходят тому, кто слушает сейчас: модель связи подключается после старта. */
class ConnectionEventsHub : ConnectionEvents {
    var delegate: ConnectionEvents? = null

    override fun lost(error: Throwable?) {
        delegate?.lost(error)
    }

    override fun queued() {
        delegate?.queued()
    }
}

class CommandRunner(private val scope: CoroutineScope, private val connection: ConnectionEvents) {
    private val mutable = MutableStateFlow(Activity())
    val state: StateFlow<Activity> = mutable
    private val reads = mutableMapOf<String, Job>()
    private var touching: Set<String> = emptySet()
    private val quiet = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /** Откуда пришли идущие задача и чтения (3.84.2): журнал запуска показывает, кто держит `busy` или `loading`. */
    @Volatile private var taskOrigin: String? = null
    private val readOrigins = java.util.concurrent.ConcurrentHashMap<String, String>()

    val busy: Boolean get() = state.value.busy

    /** Первый старт завершён: кнопки оживают. */
    fun ready() = mutable.update { it.copy(busy = false) }

    fun task(writing: Boolean = false, touches: Set<String> = emptySet(), block: suspend () -> Unit) {
        if (state.value.busy) return
        touches.forEach { reads.remove(it)?.cancel() }
        touching = touches
        mutable.update { it.copy(busy = true, loading = reads.keys.toSet(), message = null, error = false, failure = null) }
        taskOrigin = origin()
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                report(e, writing)
            } finally {
                taskOrigin = null
                touching = emptySet()
                mutable.update { it.copy(busy = false) }
            }
        }
    }

    fun read(key: String, restart: Boolean = false, silent: Boolean = false, block: suspend () -> Unit) {
        if (key in touching) return
        if (restart) reads.remove(key)?.cancel()
        if (reads[key]?.isActive == true) return
        if (silent) quiet += key else quiet -= key
        readOrigins[key] = origin()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                report(e, writing = false)
            } finally {
                if (reads[key] === coroutineContext[Job]) {
                    reads.remove(key)
                    quiet -= key
                    readOrigins -= key
                }
                mutable.update { it.copy(loading = loading()) }
            }
        }
        reads[key] = job
        mutable.update { it.copy(loading = loading()) }
        job.start()
    }

    /** Что держит раннер сейчас: занятость, задача и чтения с местом, откуда их позвали. */
    fun describe(): String = buildString {
        val now = state.value
        append("busy=${now.busy} loading=${now.loading}")
        taskOrigin?.let { append("\ntask from:\n").append(it) }
        reads.keys.toList().forEach { key -> append("\nread $key from:\n").append(readOrigins[key].orEmpty()) }
    }

    private fun origin(): String = Throwable().stackTrace.drop(2).take(ORIGIN_FRAMES).joinToString("\n") { "  at $it" }

    private fun loading(): Set<String> = reads.keys.filterNotTo(HashSet()) { it in quiet }

    fun cancelReads() {
        reads.values.forEach { it.cancel() }
        reads.clear()
        mutable.update { it.copy(loading = emptySet()) }
    }

    /** Беда, как её увидит игрок: очередь - не беда, пропавшая связь - значок и зонд, а не красная полоса. */
    fun report(e: Exception, writing: Boolean) {
        if (e is CommandQueued) {
            if (e.cause != null && e.cause !is ApiFailure) connection.lost()
            connection.queued()
            return
        }
        val problem = FailureState.from(e, writing)
        if (problem is FailureState.Offline) {
            mutable.update { it.copy(failure = problem) }
            connection.lost(e)
            if (writing) mutable.update { it.copy(error = true, message = Phrase { problem.cause.title + ". " + problem.cause.hint }) }
            return
        }
        if (problem == FailureState.UncertainWrite && e !is ApiFailure) connection.lost(e)
        mutable.update {
            it.copy(
                failure = problem,
                error = true,
                message = when (problem) {
                    FailureState.UncertainWrite -> phrase("runtime.uncertain_write")
                    else -> Phrase { refusalLine(e) }
                },
            )
        }
    }

    /** Отказ или заметка от экрана: строка на месте действия. */
    fun refuse(message: Phrase) = mutable.update { it.copy(message = message, error = true) }

    fun say(message: Phrase, error: Boolean = false) = mutable.update { it.copy(message = message, error = error) }

    fun dismissMessage() = mutable.update { it.copy(message = null, error = false) }

    /** Связь вернулась: беда «нет связи» снята вместе со строкой. */
    fun clearOffline() = mutable.update { if (it.failure is FailureState.Offline) it.copy(failure = null, message = null, error = false) else it }

    fun clearFailure() = mutable.update { it.copy(failure = null) }
}

/** Сколько кадров места вызова держит журнал раннера. */
private const val ORIGIN_FRAMES = 10
