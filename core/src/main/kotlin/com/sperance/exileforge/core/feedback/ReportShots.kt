package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.feedback.ReportShotView
import com.sperance.exileforge.core.network.ApiFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Base64

/** Что вышло из чтения снимка экрана отчёта. */
sealed interface ShotLoad {
    /** Снимок пришёл: сжатый кадр [image] (WebP или JPEG). */
    class Ready(val image: ByteArray) : ShotLoad

    /** Снимка на сервере нет: стёрт по сроку или отчёт не виден смотрящему. */
    data object Gone : ShotLoad

    /** Связи нет или сервер отказал иначе: можно попробовать ещё раз. */
    data object Failed : ShotLoad
}

/**
 * Снимки экрана отчётов для тикетов автора и модерации: читает их [fetch] и держит последние [CAPACITY] в памяти экрана - тикет, свёрнутый
 * и раскрытый снова, не качает снимок второй раз. Отказ `BUG_012` (стёрт по сроку) и `BUG_004` (отчёта нет) - [ShotLoad.Gone].
 */
class ReportShots(private val fetch: suspend (String) -> ReportShotView) {
    private val lock = Mutex()
    private val recent = LinkedHashMap<String, ByteArray>(CAPACITY, LOAD_FACTOR, true)

    /** Снимок отчёта [reportId]. */
    suspend fun load(reportId: String): ShotLoad {
        lock.withLock { recent[reportId] }?.let { return ShotLoad.Ready(it) }
        val image = try {
            Base64.getDecoder().decode(fetch(reportId).image)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiFailure) {
            return if (e.code in GONE) ShotLoad.Gone else ShotLoad.Failed
        } catch (_: Exception) {
            return ShotLoad.Failed
        }
        lock.withLock {
            recent[reportId] = image
            while (recent.size > CAPACITY) recent.remove(recent.keys.first())
        }
        return ShotLoad.Ready(image)
    }

    private companion object {
        const val CAPACITY = 6
        const val LOAD_FACTOR = 0.75f

        /** Отказы сервера, после которых снимка нет и повтор не поможет. */
        val GONE = setOf("BUG_012", "BUG_004")
    }
}
