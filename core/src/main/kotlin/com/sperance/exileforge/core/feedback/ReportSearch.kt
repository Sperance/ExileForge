package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.feedback.IssueView

/**
 * Поиск по отчётам - один у вкладок «Мои» и «Известные» и у списка модерации. Запрос [query] находит отчёт по тексту отчёта,
 * тексту переписки его Issue ([IssueView.notes]), номеру Issue (с «#» или без) и по логину автора (у
 * модерации); регистр не важен, пустой запрос находит любой отчёт.
 */
class ReportSearch(query: String) {
    private val words = query.trim()
    private val number = words.removePrefix("#").trim().toIntOrNull()

    /** Отчёт с текстом [text] и Issue [issue] под запросом; [author] - логин автора (у модерации), запрос ищется и в нём. */
    fun matches(text: String, issue: IssueView?, author: String? = null): Boolean = words.isEmpty() ||
        (number != null && issue?.number == number) ||
        has(text) ||
        issue?.notes?.any { has(it.text) } == true ||
        author?.let(::has) == true

    private fun has(text: String): Boolean = text.contains(words, ignoreCase = true)
}
