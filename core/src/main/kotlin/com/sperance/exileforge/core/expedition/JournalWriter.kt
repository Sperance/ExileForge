package com.sperance.exileforge.core.expedition

import com.sperance.exileforge.core.app.DeviceStore
import com.sperance.exileforge.core.campaign.RunJournal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Единственный писатель журнала похода на диск: заявки на запись ([later], [now]) сливаются в одну ждущую, пишет по одной под
 * замком - две записи одного файла не идут вместе, и стирание ([clear]) не обгоняет запись. Пишется журнал, что к мигу записи всё
 * ещё идёт ([current]): закрытый поход на диске не воскресает.
 *
 * Запись по заявке - не чаще раза в [SAVE_EVERY]: залп событий - одна запись, новое событие ждущую не откладывает. [now] пишет сразу,
 * снимая ждущую по часам. [written] ждёт, пока на диске не окажется журнал не старше последней заявки; заявок не было - не пишет.
 * Заявки и ожидания - с одного потока ([scope]), как весь поход.
 */
internal class JournalWriter(private val store: DeviceStore, private val scope: CoroutineScope, private val current: () -> RunJournal?) {
    private val disk = Mutex()

    /** Запись заявлена и ещё не началась. */
    private var wanted = false

    /** Запись, назначенная по часам. */
    private var delayed: Job? = null

    /** Последняя начатая запись: ожидание без заявок ждёт её конца. */
    private var writing: Job? = null

    /** Журнал на диск через [SAVE_EVERY], если запись по часам ещё не назначена; идущая сейчас запись новых заявок не несёт. */
    fun later() {
        wanted = true
        if (delayed?.isActive == true) return
        delayed = scope.launch {
            delay(SAVE_EVERY)
            write()
        }
    }

    /** Журнал на диск сейчас: уход в фон - процесс могут убить до плановой записи. */
    fun now() {
        wanted = true
        begin()
    }

    /** Ждёт, пока заявленное окажется на диске: ждущая запись идёт сразу; без заявок - только конца идущей, если она есть. */
    suspend fun written() {
        if (wanted) begin().join() else writing?.join()
    }

    /** Журнал героя [heroId] стёрт с диска, заявки на запись сняты: идущая запись дописывается прежде. */
    suspend fun clear(heroId: String) {
        wanted = false
        delayed?.cancel()
        disk.withLock { store.clearJournal(heroId) }
    }

    private fun begin(): Job {
        delayed?.cancel()
        return scope.launch { write() }.also { writing = it }
    }

    /** Запись под замком; заявку уже забрала запись перед этой - нечего писать. */
    private suspend fun write() {
        disk.withLock {
            if (!wanted) return
            wanted = false
            current()?.let { store.saveJournal(it.heroId, it.encode()) }
        }
    }

    private companion object {
        /** Не чаще этого журнал пишется на диск между отправками. */
        const val SAVE_EVERY = 5_000L
    }
}
