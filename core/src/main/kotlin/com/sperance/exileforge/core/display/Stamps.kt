package com.sperance.exileforge.core.display

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Миг в поясе устройства одной строкой: «07.10.2026 21:40» - находка уникалки, изготовление вещи. */
fun stampText(at: Long): String = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(STAMP)

private val STAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
