package com.sperance.exileforge.core.session

import com.sperance.exileforge.core.i18n.Phrase
import com.sperance.exileforge.core.i18n.phrase
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueued
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.network.ForgeHttp
import com.sperance.exileforge.core.network.refusalLine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/** Что происходит с сетью сейчас: первый старт, команда в полёте, чтения, последняя беда и её строка. */
data class Activity(
    /** Первый старт ещё идёт ([CommandRunner.ready] не позван): команды ждут, тонкая полоса видна. */
    val starting: Boolean = true,
    /** Команда в полёте - единственное, что гасит кнопки после старта. */
    val busy: Boolean = false,
    /** Чтения в полёте по ключам, без тихих. */
    val loading: Set<String> = emptySet(),
    val failure: FailureState? = null,
    val message: Phrase? = null,
    val error: Boolean = false,
) {
    val reading: Boolean get() = loading.isNotEmpty()

    /** Кнопки команд гаснут: старт не кончился или команда в полёте. */
    val held: Boolean get() = starting || busy

    /** Отказ, который печатают там, где нажали; успех не показывают. */
    val refusal: Phrase? get() = message.takeIf { error }
}

/** Куда уходят вести о связи: очередь приняла команду, сервер не отвечает. */
interface ConnectionEvents {
    fun lost(error: Throwable? = null)

    fun queued()
}

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

/**
 * Что держал раннер дольше предела: [key] - `starting`, `task` или `read:<ключ>`; [details] - [CommandRunner.describe]; [since] - когда
 * работа началась (мс, часы стены).
 */
data class Stall(val key: String, val details: String, val since: Long)

/** Куда уходят зависания раннера: тихий баг-репорт разработчику. */
fun interface StallSink {
    fun stalled(stall: Stall)
}

/** Сколько ответ чтения считается свежим для экрана, открытого снова. */
const val READ_FRESH_MS = 30_000L

/** Срок свежести чтения: [fresh] - экран открылся и примет недавний ответ, иначе - читать сейчас. */
fun freshness(fresh: Boolean): Long = if (fresh) READ_FRESH_MS else 0L

/**
 * Шина команд и чтений: команда - одна за раз, только она гасит кнопки; чтение - одно на ключ, никогда не ждёт команду и не держит её. Беда
 * превращается в [Activity.failure] и строку для игрока; ушедшая в очередь команда - не беда вовсе.
 *
 * Сторож раннера: старт, задача и видимое чтение держат полосу не дольше [limitMs]; дольше - работа отменяется, её занятость снимается,
 * игроку - «Сервер долго отвечает», разработчику - [Stall] с журналом [CommandRunner.describe]. Старт тоже под пределом; сторож запускается
 * последней строкой конструктора, когда все поля уже записаны. Задача, снятая сторожем, уже не своя: её конец не трогает следующую; тихое
 * чтение полосу не держит.
 */
class CommandRunner(
    private val scope: CoroutineScope,
    private val connection: ConnectionEvents,
    private val stalls: StallSink = StallSink { },
    private val limitMs: Long = RUNNER_LIMIT_MS,
) {
    private val mutable = MutableStateFlow(Activity())
    val state: StateFlow<Activity> = mutable
    private val reads = mutableMapOf<String, Job>()
    private var running: Job? = null
    private var touching: Set<String> = emptySet()
    private val quiet = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /** Последнее удачное чтение ключа: чей ответ ([Read.tag], обычно герой) и когда - для срока свежести. */
    private val done = java.util.concurrent.ConcurrentHashMap<String, Read>()

    private data class Read(val tag: String, val at: Long)

    /** Откуда пришли идущие задача и чтения: журнал запуска показывает, кто держит `busy` или `loading`. */
    @Volatile private var taskOrigin: String? = null
    private val readOrigins = java.util.concurrent.ConcurrentHashMap<String, String>()

    val busy: Boolean get() = state.value.busy

    /** Первый старт завершён: кнопки оживают. */
    fun ready() = mutable.update { it.copy(starting = false) }

    /**
     * Команда - одна за раз. Пока старт не кончился или другая в полёте, новая не начинается: `false`, и в журнал -
     * откуда её звали, так что нажатие не пропадает молча, а вызывающий может сказать об этом игроку.
     */
    fun task(writing: Boolean = false, touches: Set<String> = emptySet(), block: suspend () -> Unit): Boolean {
        if (state.value.held) {
            System.err.println("CommandRunner: task refused (${describeHeld()}) from:\n${origin()}")
            return false
        }
        touches.forEach {
            reads.remove(it)?.cancel()
            done.remove(it)
        }
        touching = touches
        mutable.update { it.copy(busy = true, loading = reads.keys.toSet(), message = null, error = false, failure = null) }
        taskOrigin = origin()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                report(e, writing)
            } finally {
                if (running === coroutineContext[Job]) endTask()
            }
        }
        running = job
        guard(job, TASK) { if (running === job) endTask() }
        job.start()
        return true
    }

    private fun endTask() {
        running = null
        taskOrigin = null
        touching = emptySet()
        mutable.update { it.copy(busy = false) }
    }

    /**
     * Чтение под ключом [key]: одно за раз. С [tag] (обычно id героя) удачное чтение запоминается, и вызов с
     * [maxAgeMs] пропускается, пока прошлый ответ для того же [tag] моложе - экран, открытый снова, не спрашивает сервер.
     * Команда, что трогает ключ, срок сбрасывает.
     */
    fun read(key: String, restart: Boolean = false, silent: Boolean = false, tag: String? = null, maxAgeMs: Long = 0, block: suspend () -> Unit) {
        if (key in touching) return
        val now = System.currentTimeMillis()
        if (!restart && tag != null && maxAgeMs > 0 && done[key]?.let { it.tag == tag && now - it.at < maxAgeMs } == true) return
        if (restart) reads.remove(key)?.cancel()
        if (reads[key]?.isActive == true) return
        if (silent) quiet += key else quiet -= key
        readOrigins[key] = origin()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
                tag?.let { done[key] = Read(it, System.currentTimeMillis()) }
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
        if (!silent) {
            guard(job, READ + key) {
                if (reads[key] === job) {
                    reads.remove(key)
                    quiet -= key
                    readOrigins -= key
                }
                mutable.update { it.copy(loading = loading()) }
            }
        }
        job.start()
    }

    /**
     * Предел [job]: не кончилась за [limitMs] - журнал снимается, пока она ещё держит, она отменяется, [release] снимает
     * её занятость сразу (отмена может дойти до неё позже), игроку - строка, разработчику - [Stall].
     */
    private fun guard(job: Job, key: String, release: () -> Unit) {
        val since = System.currentTimeMillis()
        val watch = scope.launch {
            delay(limitMs)
            if (!job.isActive) return@launch
            val details = describe()
            job.cancel()
            release()
            stalled(Stall(key, details, since))
        }
        job.invokeOnCompletion { watch.cancel() }
    }

    private fun stalled(stall: Stall) {
        say(phrase("runtime.slow_server"), error = true)
        runCatching { stalls.stalled(stall) }
    }

    init {
        val since = System.currentTimeMillis()
        scope.launch {
            delay(limitMs)
            if (!state.value.starting) return@launch
            val details = describe()
            ready()
            stalled(Stall(STARTING, details, since))
        }
    }

    /** Что держит раннер сейчас: занятость, задача и чтения с местом, откуда их позвали. */
    fun describe(): String = buildString {
        val now = state.value
        append("starting=${now.starting} busy=${now.busy} loading=${now.loading}")
        taskOrigin?.let { append("\ntask from:\n").append(it) }
        reads.keys.toList().forEach { key -> append("\nread $key from:\n").append(readOrigins[key].orEmpty()) }
    }

    private fun describeHeld(): String = state.value.let { if (it.starting) STARTING else "busy" }

    private fun origin(): String = Throwable().stackTrace.drop(2).take(ORIGIN_FRAMES).joinToString("\n") { "  at $it" }

    private fun loading(): Set<String> = reads.keys.filterNotTo(HashSet()) { it in quiet }

    /**
     * Снимает все чтения. Карты чистятся до отмены: на немедленном диспетчере отмена сразу выполняет `finally` чтения,
     * а он правит те же карты.
     */
    fun cancelReads() {
        val cancelled = reads.values.toList()
        reads.clear()
        quiet.clear()
        readOrigins.clear()
        mutable.update { it.copy(loading = emptySet()) }
        cancelled.forEach { it.cancel() }
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
internal const val ORIGIN_FRAMES = 10

/** Запас сверх предела вызова: запрос, упёршийся в свой предел, успевает кончиться и лечь в журнал раньше сторожа, мс. */
private const val RUNNER_GRACE_MS = 2_000L

/**
 * Дольше этого старт, задача или видимое чтение не держат полосу, мс: предел одного вызова ([ForgeHttp.CALL_MS], с его повтором) и
 * запас - зависший запрос сторож не ловит, его кончает свой тайм-аут.
 */
const val RUNNER_LIMIT_MS = ForgeHttp.CALL_MS + RUNNER_GRACE_MS

private const val STARTING = "starting"
private const val TASK = "task"
private const val READ = "read:"
