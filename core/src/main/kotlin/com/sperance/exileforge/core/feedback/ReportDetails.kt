package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.feedback.AdminReport

/** Версии клиента отчётов [reports] для выбора версии в списке модерации: без пустых и повторов, новые первыми (по числам семвера). */
fun clientVersions(reports: List<AdminReport>): List<String> = reports.map { it.report.clientVersion }.filter { it.isNotBlank() }.distinct()
    .sortedWith(compareByDescending<String> { it.semver() }.thenByDescending { it })

/** Три числа семвера одним ключом сравнения: нечисловая и недостающая часть - 0. */
private fun String.semver(): Long = split('.').take(SEMVER_PARTS).fold(0L) { acc, part -> acc * SEMVER_BASE + (part.takeWhile(Char::isDigit).toLongOrNull() ?: 0L) }.let { key ->
    (split('.').size until SEMVER_PARTS).fold(key) { acc, _ -> acc * SEMVER_BASE }
}

private const val SEMVER_PARTS = 3
private const val SEMVER_BASE = 10_000L
