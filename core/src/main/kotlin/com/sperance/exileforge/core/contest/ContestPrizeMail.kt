package com.sperance.exileforge.core.contest

import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.MailKind
import com.sperance.exileforge.rules.content.ContestHonour
import com.sperance.exileforge.rules.content.ContestKind
import com.sperance.exileforge.rules.content.ContestMetric
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.SystemMail

/**
 * Письмо о призе состязания ([SystemMail.CONTEST_PRIZE], [SystemMail.CONTEST_PODIUM]), прочитанное по своим аргументам: вид [kind] и мера
 * [metric] (у меры «босс» - [boss]), лига уровнями от [leagueFrom] до [leagueTo] (0 - верхней границы нет), место [place] из [size],
 * результат [value] и титул [honour] (null - мест без титула); сферы награды - [orbs] вложения письма. Одна точка разбора аргументов:
 * текст письма ([com.sperance.exileforge.core.display.MailSchemas]) и итоги при входе читают её.
 */
data class ContestPrizeMail(
    val mailId: String,
    val kind: ContestKind,
    val metric: ContestMetric,
    val boss: MonsterCode?,
    val leagueFrom: Int,
    val leagueTo: Int,
    val place: Int,
    val size: Int,
    val value: Long,
    val honour: ContestHonour?,
    val orbs: Map<String, Long>,
    val createdAt: String,
) {
    /** Сфер в награде всего. */
    val orbCount: Long get() = orbs.values.sum()

    companion object {
        /** Номера аргументов письма о призе - в порядке сервера (`ContestAwards.give`). */
        const val KIND = 0
        const val METRIC = 1
        const val BOSS = 2
        const val FROM = 3
        const val TO = 4
        const val PLACE = 5
        const val SIZE = 6
        const val VALUE = 7
        const val UNTIL = 8

        private val KEYS = setOf(SystemMail.CONTEST_PRIZE, SystemMail.CONTEST_PODIUM)

        /** Письмо ли это о призе состязания. */
        fun holds(mail: Mail): Boolean = mail.kind == MailKind.SYSTEM && mail.key in KEYS

        /** Приз письма [mail]; не письмо о призе или вид, мера и место не прочитать (письмо более нового сервера) - null. */
        fun of(mail: Mail): ContestPrizeMail? {
            if (!holds(mail)) return null
            val args = mail.args
            val kind = enumArg<ContestKind>(args, KIND) ?: return null
            val metric = enumArg<ContestMetric>(args, METRIC) ?: ContestMetric.OTHER
            val place = args.getOrNull(PLACE)?.toIntOrNull()?.takeIf { it > 0 } ?: return null
            val until = args.getOrNull(UNTIL)?.toLongOrNull() ?: 0
            return ContestPrizeMail(
                mailId = mail.id,
                kind = kind,
                metric = metric,
                boss = args.getOrNull(BOSS)?.takeIf(String::isNotBlank)?.let(::MonsterCode),
                leagueFrom = args.getOrNull(FROM)?.toIntOrNull() ?: 0,
                leagueTo = args.getOrNull(TO)?.toIntOrNull() ?: 0,
                place = place,
                size = args.getOrNull(SIZE)?.toIntOrNull() ?: 0,
                value = args.getOrNull(VALUE)?.toLongOrNull() ?: 0,
                honour = ContestHonour(kind, metric, place, until).takeIf { until > 0 },
                orbs = mail.attachment.items,
                createdAt = mail.createdAt,
            )
        }

        /** Значение перечисления [E] аргумента [index]; нет или незнакомое - null. */
        inline fun <reified E : Enum<E>> enumArg(args: List<String>, index: Int): E? = args.getOrNull(index)?.let { name -> enumValues<E>().firstOrNull { it.name == name } }
    }
}
