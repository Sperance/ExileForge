package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.FullReport
import com.sperance.exileforge.core.model.feedback.IssueNote
import com.sperance.exileforge.core.model.feedback.IssueView
import com.sperance.exileforge.core.model.feedback.NoteAuthor
import com.sperance.exileforge.core.model.feedback.OwnReport
import com.sperance.exileforge.core.model.feedback.ReportStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/** Переписка тикета: слово модерации встаёт один раз - полем до Issue, его комментарием с Issue. */
class ReportThreadTest {
    private val word = "Принято, спасибо"
    private val answer = IssueNote(NoteAuthor.AUTHOR, "Ещё деталь")
    private val issued = IssueView(7, notes = listOf(answer, IssueNote(NoteAuthor.MODERATION, word)))

    /** До Issue слово решения модерации - в переписке полем отчёта, после переписки до Issue. */
    @Test
    fun the_word_stands_in_the_thread_until_the_issue() {
        val waiting = FullReport("r", "text", status = ReportStatus.SENT, reason = word, answers = listOf(answer))
        assertEquals(listOf(answer.text, word), waiting.thread.map { it.text })
    }

    /** С Issue слово модерации - только его комментарий: сервер прислал его и полем, а в тикете модерации и у автора оно одно. */
    @Test
    fun the_word_is_not_repeated_once_the_issue_carries_it() {
        val moderation = FullReport("r", "text", status = ReportStatus.SENT, reason = word, issue = issued)
        val own = OwnReport("r", FeedbackKind.BUG, "text", ReportStatus.SENT, reason = word, issue = issued)
        listOf(moderation.thread, own.thread).forEach { thread ->
            assertEquals(1, thread.count { it.text == word })
            assertEquals(issued.notes, thread)
        }
    }
}
