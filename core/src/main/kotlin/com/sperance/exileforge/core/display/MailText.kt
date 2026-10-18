package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.MailKind
import com.sperance.exileforge.rules.content.SystemMail
import com.sperance.exileforge.rules.text.LocaleKey

/** Тема письма словами игрока: системное - по схеме своего ключа ([MailSchemas]), письмо администратора и героя - как написано. */
fun mailSubject(mail: Mail): String = if (mail.kind == MailKind.SYSTEM) MailSchemas.of(mail.key).subject(mail) else mail.subject

/** Текст письма словами игрока: системное - по схеме своего ключа с причиной строкой ниже, прочие - как написаны. */
fun mailBody(mail: Mail): String = if (mail.kind == MailKind.SYSTEM) MailSchemas.of(mail.key).body(mail) else mail.body

/** Подстановка системного письма: строка из аргументов письма словами игрока. */
fun interface MailSlot {
    fun text(args: List<String>): String
}

/**
 * Схема системного письма: подстановки темы [subjectSlots] и текста [bodySlots] в строки словаря сервера `<ключ>.subject` и `<ключ>.body`
 * (null - аргументы письма как есть) и номер аргумента причины [reason]: непустая ложится строкой `<ключ>.reason` под текстом.
 */
class MailSchema(private val subjectSlots: List<MailSlot>? = null, private val bodySlots: List<MailSlot>? = null, private val reason: Int? = null) {
    fun subject(mail: Mail): String = loc("${mail.key}.subject", fill(subjectSlots, mail.args))

    fun body(mail: Mail): String = listOfNotNull(
        loc("${mail.key}.body", fill(bodySlots, mail.args)),
        reason?.let(mail.args::getOrNull)?.takeIf(String::isNotBlank)?.let { loc("${mail.key}.reason", listOf(it)) },
    ).joinToString("\n\n")

    private fun fill(slots: List<MailSlot>?, args: List<String>): List<String> = slots?.map { it.text(args) } ?: args
}

/**
 * Реестр схем системных писем по ключу [SystemMail]: какой аргумент чем печатается. Письмо, чьего ключа нет в реестре, печатает
 * аргументы как есть. Новое системное письмо с особыми аргументами - запись сюда, не ветка экрана.
 */
object MailSchemas {
    fun of(key: String): MailSchema = SCHEMAS[key] ?: PLAIN

    /** Аргумент [index] как есть. */
    private fun arg(index: Int) = MailSlot { it.getOrNull(index).orEmpty() }

    /** Аргумент [index], непустой, словами [title]. */
    private fun titled(index: Int, title: (String) -> String) = MailSlot { args -> args.getOrNull(index)?.takeIf(String::isNotBlank)?.let(title).orEmpty() }

    /** Предмет лота кодом: шаблон снаряжения или стопка сумки. */
    private fun lotItem(code: String): String = if (serverLocale.contains(LocaleKey.equipmentName(code))) equipmentTitle(code) else itemTitle(code)

    /** Судьба Issue по имени словами сервера; исправленный - «Исправлено в X», если версия - аргумент [version] - известна. */
    private fun issueState(version: Int) = MailSlot { args ->
        val name = args.getOrNull(STATUS).orEmpty()
        val fixed = args.getOrNull(version).orEmpty()
        when {
            name.isBlank() -> ""
            name == FIXED && fixed.isNotBlank() -> ui("feedback.fate.fixed_in", fixed)
            else -> locOr("enum.IssueState.$name", name)
        }
    }

    /** Как письмо об отчёте называет его: «#N от даты», без Issue - «от даты»; без даты - пусто. */
    private val reportRef = MailSlot { args ->
        val day = args.getOrNull(REPORT_DAY)?.let(::reportDay)?.takeIf(String::isNotBlank)
        val number = args.getOrNull(REPORT_NUMBER).orEmpty()
        when {
            day == null -> ""
            number.isBlank() -> loc("mail.feedback.ref_dated", listOf(day))
            else -> loc("mail.feedback.ref", listOf(number, day))
        }
    }

    /** День отчёта из даты ISO `гггг-мм-дд` - «дд.мм.гггг»; иная строка - пусто. */
    private fun reportDay(iso: String): String = iso.take(10).split('-').takeIf { it.size == 3 }?.let { (year, month, day) -> "$day.$month.$year" }.orEmpty()

    private val kind = titled(KIND) { locOr("enum.FeedbackKind.$it", it) }
    private val status = titled(STATUS) { locOr("enum.BugStatus.$it", it) }

    /** Письмо об отчёте с судьбой [fate]: тема - вид и судьба, текст - судьба, первая строка отчёта и ссылка на него. */
    private fun feedback(fate: MailSlot) = MailSchema(listOf(kind, fate), listOf(fate, arg(LINE), reportRef), reason = REPLY)

    /** Письмо о лоте: предмет лота и число. */
    private val auction = listOf(titled(0, ::lotItem), arg(1)).let { lot -> MailSchema(lot, lot) }

    private val PLAIN = MailSchema()

    private val SCHEMAS: Map<String, MailSchema> = mapOf(
        SystemMail.FEEDBACK_STATUS to feedback(status),
        SystemMail.FEEDBACK_ISSUE to feedback(issueState(ISSUE_VERSION)),
        SystemMail.FEEDBACK_REPLY to feedback(status),
        SystemMail.AUCTION_EXPIRED to auction,
        SystemMail.AUCTION_RECALLED to auction,
        SystemMail.AUCTION_EXPIRING to auction,
        SystemMail.FATE_REDRAWN to listOf(titled(0) { FateCard(it).title }).let { fate -> MailSchema(fate, fate, reason = 1) },
    )

    /** Номера аргументов письма об отчёте ([SystemMail.FEEDBACK_STATUS]). */
    private const val KIND = 0
    private const val STATUS = 1
    private const val LINE = 2
    private const val REPLY = 3
    private const val ISSUE_VERSION = 4
    private const val REPORT_NUMBER = 5
    private const val REPORT_DAY = 6

    private const val FIXED = "FIXED"
}
