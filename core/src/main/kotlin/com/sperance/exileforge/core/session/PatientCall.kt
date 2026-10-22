package com.sperance.exileforge.core.session

import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueue
import com.sperance.exileforge.core.network.ForgeHttp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

/**
 * Где запрос с повтором ([PatientCall]) глазами экрана: ожидание, «нет связи» с «Повторить» и короткая весть «связь восстановлена».
 * [T] - что нажали ([tag]): по нему экран крутит спиннер на той самой кнопке и подбирает слова «нет связи».
 */
sealed interface CallState<out T> {
    /** Запроса нет. */
    data object Idle : CallState<Nothing>

    /** Запрос в полёте: начат в [since] (мс стены), идёт попытка [attempt] из [attempts] (null - без предела). */
    data class Waiting<out T>(val since: Long, val attempt: Int, val attempts: Int?, override val tag: T) : CallState<T>

    /** Попытки кончились, ответа нет: запрос не потерян - [PatientCall.retry] шлёт его снова тем же ключом. */
    data class Lost<out T>(override val tag: T) : CallState<T>

    /** Запрос прошёл после сбоя: весть держится [PatientCall.RESTORED_MS], потом - [Idle]. */
    data object Restored : CallState<Nothing>

    /** Что нажали у запроса в полёте или ждущего повтора; null - запроса нет. */
    val tag: T? get() = null

    /** Кнопки запросов гаснут: запрос в полёте или ждёт повтора. */
    val holds: Boolean get() = this is Waiting || this is Lost
}

/**
 * Ответ запроса, ещё не положенный на экран: зовётся вне разбора ошибок запроса ([PatientCall]) - его исключение не повторяет
 * принятый сервером запрос.
 */
typealias Landing = suspend () -> Unit

/**
 * Как запрос повторяется: до [attempts] попыток (null - без предела, тогда [CallState.Lost] не наступает), каждая не дольше
 * [timeoutMs]; пауза перед следующей - [firstPauseMs], удваиваясь до [doublings] раз.
 */
class RetryPolicy(val attempts: Int?, val timeoutMs: Long, private val firstPauseMs: Long, private val doublings: Int = 0) {
    /** Пауза после неудачной попытки [attempt] (с 1). */
    fun pauseAfter(attempt: Int): Long = firstPauseMs shl (attempt - 1).coerceIn(0, doublings)

    companion object {
        /** Шаг игрока: 3 попытки по 15 с (шаг лёгкий, сервер отвечает за доли секунды), пауза 1 с, затем «Повторить». */
        val STEP = RetryPolicy(attempts = 3, timeoutMs = 15_000L, firstPauseMs = 1_000L)

        /** Итог, что нельзя потерять (итог боя): без предела, попытка - предел вызова, пауза 2 с, удваиваясь до 16 с. */
        val ENDLESS = RetryPolicy(attempts = null, timeoutMs = ForgeHttp.CALL_MS, firstPauseMs = 2_000L, doublings = 3)
    }
}

/**
 * Запрос, что сеть не теряет: уходит под одним ключом идемпотентности по [policy]; повтор с тем же ключом сервер не выполняет второй
 * раз. Сбой связи - нет ответа за попытку, сеть, шлюз или «первый ещё идёт» ([CommandQueue.transient]) - следующая попытка; попытки
 * кончились - [CallState.Lost], запрос ждёт [retry]. Слово сервера - отказ - кончает запрос ([Request.refused]). Прошёл после сбоя -
 * [CallState.Restored] и [Request.restored]. Запрос один за раз; новый, пока прежний в полёте или ждёт повтора, не начинается, а
 * [supersede] бросает прежний.
 *
 * Нить одна: [scope] - область приложения на `Dispatchers.Main.immediate`, и звать методы можно только из неё. Каждый запуск -
 * своё поколение: брошенный ([drop], [supersede]) не пишет ни состояния, ни колбэков, даже если его корутина ещё доходит до конца.
 * Колбэки зовутся вне разбора ошибок запроса: исключение колбэка - не сбой связи, принятый сервером запрос не повторяется.
 */
class PatientCall<T>(
    private val scope: CoroutineScope,
    private val policy: RetryPolicy = RetryPolicy.STEP,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /**
     * Что слать: [send] - сам запрос под ключом; его ответ - [Landing], что кладёт ответ на экран уже после разбора ошибок запроса;
     * [tag] - что нажали; [refused] - сервер отказал; [restored] - прошёл после сбоя (связь вернулась); [broken] - ответ не лёг
     * (ошибка [Landing]): запрос уже принят, он не повторяется.
     */
    class Request<T>(
        val tag: T,
        val refused: (ApiFailure) -> Unit = {},
        val restored: () -> Unit = {},
        val broken: (Exception) -> Unit = {},
        val send: suspend (key: String) -> Landing,
    )

    private class Pending<T>(val key: String, val request: Request<T>, val generation: Long)

    private val mutable = MutableStateFlow<CallState<T>>(CallState.Idle)
    val state: StateFlow<CallState<T>> = mutable.asStateFlow()

    private var pending: Pending<T>? = null
    private var generation = 0L
    private var job: Job? = null
    private var fading: Job? = null

    /** Шлёт [request] новым ключом; false - прежний запрос в полёте или ждёт повтора. */
    fun send(request: Request<T>): Boolean {
        if (mutable.value.holds) return false
        launch(Pending(UUID.randomUUID().toString(), request, ++generation), shaken = false)
        return true
    }

    /**
     * Шлёт [request] новым ключом, бросив прежний - в полёте или ждущий повтора: его ответа больше никто не ждёт (сервер мог его и
     * принять - [request] идёт уже после него). Так выход из режима не ждёт повисший шаг.
     */
    fun supersede(request: Request<T>) {
        drop()
        send(request)
    }

    /** «Повторить»: потерянный запрос уходит снова тем же ключом, с полным числом попыток. */
    fun retry() {
        val lost = pending?.takeIf { mutable.value is CallState.Lost } ?: return
        launch(Pending(lost.key, lost.request, ++generation), shaken = true)
    }

    /** Запрос брошен (экран закрыт): ответа больше никто не ждёт - поколение сменилось. */
    fun drop() {
        generation++
        job?.cancel()
        fading?.cancel()
        pending = null
        mutable.value = CallState.Idle
    }

    private fun current(call: Pending<T>): Boolean = call.generation == generation

    private fun launch(call: Pending<T>, shaken: Boolean) {
        fading?.cancel()
        pending = call
        val since = clock()
        val tag = call.request.tag
        mutable.value = CallState.Waiting(since, 1, policy.attempts, tag)
        job = scope.launch { drive(call, since, tag, shaken) }
    }

    /** Попытки запроса [call] по политике; каждая запись состояния - только своего поколения. */
    private suspend fun drive(call: Pending<T>, since: Long, tag: T, shaken: Boolean) {
        var troubled = shaken
        var attempt = 1
        while (true) {
            if (!current(call)) return
            mutable.value = CallState.Waiting(since, attempt, policy.attempts, tag)
            when (val outcome = attempt(call)) {
                is Outcome.Answered -> return done(call, outcome.landing, troubled)
                is Outcome.Refused -> return refused(call, outcome.failure)
                Outcome.Failed -> Unit
            }
            troubled = true
            if (policy.attempts?.let { attempt >= it } == true) break
            delay(policy.pauseAfter(attempt))
            attempt++
        }
        if (current(call)) mutable.value = CallState.Lost(tag)
    }

    /** Одна попытка: ответ, отказ сервера или сбой связи; колбэков здесь нет. */
    private suspend fun attempt(call: Pending<T>): Outcome = try {
        Outcome.Answered(withTimeout(policy.timeoutMs) { call.request.send(call.key) })
    } catch (_: TimeoutCancellationException) {
        Outcome.Failed
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiFailure) {
        if (CommandQueue.transient(e.status)) Outcome.Failed else Outcome.Refused(e)
    } catch (_: Exception) {
        Outcome.Failed
    }

    private fun refused(call: Pending<T>, failure: ApiFailure) {
        if (!current(call)) return
        pending = null
        mutable.value = CallState.Idle
        call.request.refused(failure)
    }

    /** Ответ лёг: пока он ложится, кнопки ещё погашены (запрос в ожидании); затем - покой или весть «связь восстановлена». */
    private suspend fun done(call: Pending<T>, landing: Landing, troubled: Boolean) {
        if (!current(call)) return
        try {
            landing()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            call.request.broken(e)
        }
        if (!current(call)) return
        pending = null
        mutable.value = if (troubled) CallState.Restored else CallState.Idle
        if (!troubled) return
        call.request.restored()
        fading = scope.launch {
            delay(RESTORED_MS)
            if (current(call) && mutable.value == CallState.Restored) mutable.value = CallState.Idle
        }
    }

    /** Исход одной попытки. */
    private sealed interface Outcome {
        class Answered(val landing: Landing) : Outcome

        class Refused(val failure: ApiFailure) : Outcome

        data object Failed : Outcome
    }

    companion object {
        /** Сколько держится весть «связь восстановлена», мс. */
        const val RESTORED_MS = 2_500L
    }
}
