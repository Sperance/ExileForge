package com.sperance.exileforge.presentation.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/** Пункт окна запуска (3.82.0): версия, вход, контент, словарь, иконки; [title] - ключ словаря. */
enum class StartStage(val title: String) {
    VERSION("start.version"),
    SESSION("start.session"),
    CONTENT("start.content"),
    DICTIONARY("start.dictionary"),
    ICONS("start.icons"),
}

enum class StepState { RUN, DONE, FAIL }

/**
 * Подпункт запуска: [key] - ключ словаря `start.step.*`, [args] - его подстановки (например, «файлы 5/12»),
 * время начала и конца по часам устройства, [error] - почему не вышло.
 */
data class StartStep(
    val id: Long,
    val stage: StartStage,
    val key: String,
    val args: List<String>,
    val state: StepState,
    val startedAt: Long,
    val endedAt: Long = 0,
    val error: String? = null,
) {
    fun millis(now: Long): Long = (if (endedAt > 0) endedAt else now) - startedAt
}

/**
 * Журнал запуска (3.82.0): каждый шаг старта - чтение ключа сессии, копия героя, запрос к серверу, файлы контента -
 * отмечается здесь началом и концом. Окно запуска показывает под активным пунктом его подпункты с временем, так что
 * видно, где встало; тот же журнал уходит в баг-репорт и в отчёт сторожа главного потока. Хранит последние [LIMIT] шагов.
 */
class StartupTrace(private val clock: () -> Long = System::currentTimeMillis) {
    private val ids = AtomicLong()
    private val mutable = MutableStateFlow<List<StartStep>>(emptyList())
    val steps: StateFlow<List<StartStep>> = mutable.asStateFlow()

    private val done = MutableStateFlow(false)

    /** Свои шаги запуска позади: язык, сервер, сессия - начата или её нет. Окно запуска держится до этого. */
    val startupDone: StateFlow<Boolean> = done.asStateFlow()

    fun started() {
        done.value = true
    }

    /** Шаг [key] пункта [stage] на время [block]: успех - DONE, исключение - FAIL с его текстом (и летит дальше). */
    suspend fun <T> step(stage: StartStage, key: String, vararg args: Any, block: suspend () -> T): T {
        val id = begin(stage, key, *args)
        return try {
            block().also { end(id) }
        } catch (e: CancellationException) {
            end(id, e)
            throw e
        } catch (e: Throwable) {
            end(id, e)
            throw e
        }
    }

    fun begin(stage: StartStage, key: String, vararg args: Any): Long {
        val id = ids.incrementAndGet()
        mutable.update { (it + StartStep(id, stage, key, args.map(Any::toString), StepState.RUN, clock())).takeLast(LIMIT) }
        return id
    }

    /** Подстановки идущего шага сменились: «файлы 5/12». */
    fun progress(id: Long, vararg args: Any) = change(id) { it.copy(args = args.map(Any::toString)) }

    fun end(id: Long, failure: Throwable? = null) = change(id) {
        it.copy(state = if (failure == null) StepState.DONE else StepState.FAIL, endedAt = clock(), error = failure?.let(::reason))
    }

    /** Мгновенная отметка: «сохранённой сессии нет». */
    fun note(stage: StartStage, key: String, vararg args: Any) = end(begin(stage, key, *args))

    /** Журнал текстом: «+120ms SESSION start.step.token DONE 35ms», от первого шага. */
    fun journal(now: Long = clock()): String {
        val steps = mutable.value
        val origin = steps.firstOrNull()?.startedAt ?: now
        return steps.joinToString("\n") { s ->
            val args = if (s.args.isEmpty()) "" else s.args.joinToString(",", "(", ")")
            "+${s.startedAt - origin}ms ${s.stage} ${s.key}$args ${s.state} ${s.millis(now)}ms" + s.error?.let { " - $it" }.orEmpty()
        }
    }

    private fun change(id: Long, transform: (StartStep) -> StartStep) = mutable.update { steps -> steps.map { if (it.id == id && it.state == StepState.RUN) transform(it) else it } }

    private fun reason(e: Throwable): String = if (e is CancellationException) "cancelled" else e.message?.take(160) ?: e::class.simpleName.orEmpty()

    private companion object {
        const val LIMIT = 120
    }
}
