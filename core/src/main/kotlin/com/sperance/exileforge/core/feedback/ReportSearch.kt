package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.feedback.IssueNote
import com.sperance.exileforge.core.model.feedback.IssueView

/**
 * Поиск по своим отчётам игрока - список приходит целиком, ищется на месте (очередь модерации ищет сервер тем же смыслом). Запрос
 * [query] находит отчёт по тексту отчёта, тексту его переписки (до Issue и в Issue) и номеру Issue (с «#» или без); регистр не важен,
 * пустой запрос находит любой отчёт.
 */
class ReportSearch(query: String) {
    private val words = query.trim()
    private val number = words.removePrefix("#").trim().toIntOrNull()

    /** Отчёт с текстом [text], перепиской [thread] и Issue [issue] под запросом. */
    fun matches(text: String, thread: List<IssueNote>, issue: IssueView?): Boolean = words.isEmpty() ||
        (number != null && issue?.number == number) ||
        has(text) ||
        thread.any { has(it.text) }

    private fun has(text: String): Boolean = text.contains(words, ignoreCase = true)
}
