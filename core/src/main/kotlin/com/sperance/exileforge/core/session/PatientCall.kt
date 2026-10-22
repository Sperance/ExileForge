package com.sperance.exileforge.core.session

import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueue
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
 * Когда в ожидании показать спиннер на кнопке и полосу «Сервер долго отвечает», экран решает сам по своим часам ([CallTimes]).
 */
sealed interface CallState {
    /** Запроса нет. */
    data object Idle : CallState

    /**
     * Запрос в полёте: нажат в [since] (мс стены), идёт попытка [attempt] из [attempts]; [tag] - что нажали (по нему экран крутит
     * спиннер на той самой кнопке).
     */
    data class Waiting(val since: Long, val attempt: Int, val attempts: Int, override val tag: Any?) : CallState

    /** Попытки кончились, ответа нет: запрос [tag] не потерян - [PatientCall.retry] шлёт его снова тем же ключом. */
    data class Lost(override val tag: Any?) : CallState

    /** Запрос прошёл после сбоя: весть держится [PatientCall.RESTORED_MS], потом - [Idle]. */
    data object Restored : CallState

    /** Кнопки запросов гаснут: запрос в полёте или ждёт повтора. */
    val holds: Boolean get() = this is Waiting || this is Lost

    /** Что нажали у запроса в полёте или ждущего повтора; null - запроса нет. */
    val tag: Any? get() = null
}

/** Пороги ожидания для экрана, мс от нажатия: спиннер на нажатой кнопке и полоса «Сервер долго отвечает». */
object CallTimes {
    const val SPINNER_MS = 1_500L
    const val SLOW_MS = 8_000L
}

/**
 * Запрос, что сеть не теряет: уходит под одним ключом идемпотентности до [attempts] раз, каждая попытка - не дольше [timeoutMs];
 * повтор с тем же ключом сервер не выполняет второй раз. Сбой связи - нет ответа за попытку, сеть, шлюз или «первый ещё идёт»
 * ([CommandQueue.transient]) - следующая попытка через [pauseMs]; попытки кончились - [CallState.Lost], запрос ждёт [retry]. Слово
 * сервера - отказ - кончает запрос ([Request.refused]). Прошёл после сбоя - [CallState.Restored] и [Request.restored]. Запрос один
 * за раз; новый, пока прежний в полёте или ждёт повтора, не начинается.
 */
class PatientCall(
    private val scope: CoroutineScope,
    private val attempts: Int = ATTEMPTS,
    private val timeoutMs: Long = TIMEOUT_MS,
    private val pauseMs: Long = PAUSE_MS,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /**
     * Что слать: [send] - сам запрос под ключом; [tag] - что нажали; [refused] - сервер отказал; [restored] - прошёл после сбоя
     * (связь вернулась).
     */
    class Request(
        val tag: Any? = null,
        val refused: (ApiFailure) -> Unit = {},
        val restored: () -> Unit = {},
        val send: suspend (key: String) -> Unit,
    )

    private class Pending(val key: String, val request: Request)

    private val mutable = MutableStateFlow<CallState>(CallState.Idle)
    val state: StateFlow<CallState> = mutable.asStateFlow()

    private var pending: Pending? = null
    private var job: Job? = null
    private var fading: Job? = null

    /** Шлёт [request] новым ключом; false - прежний запрос в полёте или ждёт повтора. */
    fun send(request: Request): Boolean {
        if (mutable.value.holds) return false
        val next = Pending(UUID.randomUUID().toString(), request)
        pending = next
        launch(next, shaken = false)
        return true
    }

    /**
     * Шлёт [request] новым ключом, бросив прежний - в полёте или ждущий повтора: его ответа больше никто не ждёт (сервер мог его и
     * принять - [request] идёт уже после него). Так выход из режима не ждёт повисший шаг.
     */
    fun supersede(request: Request) {
        drop()
        send(request)
    }

    /** «Повторить»: потерянный запрос уходит снова тем же ключом, с полным числом попыток. */
    fun retry() {
        val lost = pending?.takeIf { mutable.value is CallState.Lost } ?: return
        launch(lost, shaken = true)
    }

    /** Запрос брошен (экран закрыт): ответа больше никто не ждёт. */
    fun drop() {
        job?.cancel()
        fading?.cancel()
        pending = null
        mutable.value = CallState.Idle
    }

    private fun launch(call: Pending, shaken: Boolean) {
        fading?.cancel()
        val since = clock()
        mutable.value = CallState.Waiting(since, 1, attempts, call.request.tag)
        job = scope.launch {
            var troubled = shaken
            for (attempt in 1..attempts) {
                mutable.value = CallState.Waiting(since, attempt, attempts, call.request.tag)
                val refusal = try {
                    withTimeout(timeoutMs) { call.request.send(call.key) }
                    done(call, troubled)
                    return@launch
                } catch (_: TimeoutCancellationException) {
                    null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ApiFailure) {
                    e.takeUnless { CommandQueue.transient(it.status) }
                } catch (_: Exception) {
                    null
                }
                if (refusal != null) {
                    pending = null
                    mutable.value = CallState.Idle
                    call.request.refused(refusal)
                    return@launch
                }
                troubled = true
                if (attempt < attempts) delay(pauseMs)
            }
            mutable.value = CallState.Lost(call.request.tag)
        }
    }

    private fun done(call: Pending, troubled: Boolean) {
        pending = null
        if (!troubled) {
            mutable.value = CallState.Idle
            return
        }
        mutable.value = CallState.Restored
        call.request.restored()
        fading = scope.launch {
            delay(RESTORED_MS)
            if (mutable.value == CallState.Restored) mutable.value = CallState.Idle
        }
    }

    companion object {
        /** Попыток до «нет связи». */
        const val ATTEMPTS = 3

        /** Предел одной попытки, мс: шаг лёгкий, сервер отвечает за доли секунды - дольше значит, что ответа не будет. */
        const val TIMEOUT_MS = 15_000L

        /** Пауза между попытками, мс. */
        const val PAUSE_MS = 1_000L

        /** Сколько держится весть «связь восстановлена», мс. */
        const val RESTORED_MS = 2_500L
    }
}
