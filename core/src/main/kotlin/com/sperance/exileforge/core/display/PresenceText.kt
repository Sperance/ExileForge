package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Давность присутствия - одна для всех экранов (список и досье модерации, карточка игрока): «в сети», пока [online]; иначе по последней
 * команде [lastSeenAt] (мс эпохи, 0 - неизвестно) на миг [now]: «только что» - меньше минуты, «14 мин назад» - меньше часа, дальше
 * календарём ([calendarText]). Без глагола «был»: подпись не знает рода игрока. Дни - в поясе [zone].
 */
fun presenceText(lastSeenAt: Long, now: Long, online: Boolean, zone: ZoneId = ZoneId.systemDefault()): String {
    val ago = now - lastSeenAt
    return when {
        online -> ui("presence.online")
        lastSeenAt <= 0 -> ui("presence.unknown")
        ago < MINUTE_MS -> ui("presence.just_now")
        ago < HOUR_MS -> (ago / MINUTE_MS).toInt().let { ui(pluralKey("presence.minutes", it), it) }
        else -> calendarText(lastSeenAt, now, zone)
    }
}

/**
 * Миг [at] календарём относительно [now] в поясе [zone]: «сегодня в 14:05», «вчера в 21:30», в этом году - «12 окт. в 09:10», раньше -
 * «12.10.2025».
 */
fun calendarText(at: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val moment = Instant.ofEpochMilli(at).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val time = moment.format(TIME)
    return when (moment.toLocalDate()) {
        today -> ui("calendar.today", time)
        today.minusDays(1) -> ui("calendar.yesterday", time)
        else -> if (moment.year == today.year) ui("calendar.day_time", dayOf(moment), time) else moment.format(FULL_DATE)
    }
}

/** День [at] в поясе [zone] коротко: «24 нояб.». */
fun dayText(at: Long, zone: ZoneId = ZoneId.systemDefault()): String = dayOf(Instant.ofEpochMilli(at).atZone(zone))

/** День [at] в поясе [zone] со временем: «24 нояб. в 09:10». */
fun dayTimeText(at: Long, zone: ZoneId = ZoneId.systemDefault()): String = Instant.ofEpochMilli(at).atZone(zone).let { ui("calendar.day_time", dayOf(it), it.format(TIME)) }

/** Дата [at] в поясе [zone] цифрами: «12.03.2026». */
fun dateText(at: Long, zone: ZoneId = ZoneId.systemDefault()): String = Instant.ofEpochMilli(at).atZone(zone).format(FULL_DATE)

private fun dayOf(moment: ZonedDateTime): String = ui("calendar.day", moment.dayOfMonth, ui("calendar.month.${moment.monthValue}"))

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val FULL_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
