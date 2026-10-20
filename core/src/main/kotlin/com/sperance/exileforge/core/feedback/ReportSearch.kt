package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.feedback.IssueView

/**
 * Поиск по отчётам - один у вкладок «Мои» и «Известные» и у списка модерации. Запрос [query] находит отчёт по тексту отчёта,
 * тексту переписки его Issue ([IssueView.notes]), номеру Issue («#212» или «212») и по полю места (логин автора у
 * модерации); регистр не важен, пустой запрос находит любой отчёт.
 */
class ReportSearch(query: String) {
    private val words = query.trim()
    private val number = words.removePrefix("#").trim().toIntOrNull()

    /** Отчёт с текстом [text] и Issue [issue] под запросом; [extra] - ещё одно поле, где тоже ищется запрос. */
    fun matches(text: String, issue: IssueView?, extra: String? = null): Boolean = words.isEmpty() ||
        (number != null && issue?.number == number) ||
        has(text) ||
        issue?.notes?.any { has(it.text) } == true ||
        extra?.let(::has) == true

    private fun has(text: String): Boolean = text.contains(words, ignoreCase = true)
}
