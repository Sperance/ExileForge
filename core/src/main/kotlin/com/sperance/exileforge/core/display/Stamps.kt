package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.uiLanguage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Миг в поясе устройства одной строкой `дд.мм.гггг чч:мм` - находка уникалки, изготовление вещи. */
fun stampText(at: Long): String = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(STAMP)

/** День мига в поясе устройства `дд.мм` - короткий срок: «до 24.10». */
fun dayText(at: Long): String = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(DAY)

/** Дата мига в поясе устройства словами языка интерфейса: «12 марта 2026». */
fun dateText(at: Long): String = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag(uiLanguage.code)))

/** Миг в поясе устройства словами языка интерфейса: «30 октября, 18:00» - срок санкции. */
fun momentText(at: Long): String = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale.forLanguageTag(uiLanguage.code)))

private val STAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

private val DAY = DateTimeFormatter.ofPattern("dd.MM")
