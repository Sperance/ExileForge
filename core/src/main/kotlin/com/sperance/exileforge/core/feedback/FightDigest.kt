package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.campaign.NoteTrace
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.CombatEvent
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.run.FightReport
import java.util.Locale
import kotlin.math.roundToLong

/** Последний бой забега, из которого отправляют отчёт: похода, испытания или Разлома; null - отчёт не из забега или боя ещё не было. */
fun interface LastFight {
    fun report(): FightReport?
}

/**
 * Сжатый разбор боя для отчёта об ошибке ([com.sperance.exileforge.core.model.command.BugReportRequest.fight]): враг, исход и время,
 * урон в обе стороны, пройденные фазы, добивающий удар и последние [LOG_LINES] строк журнала. Пишется кодами и числами - для разработчика
 * в теле Issue, без имени героя и без личного.
 */
object FightDigest {
    /** Сколько последних строк журнала идёт в разбор. */
    const val LOG_LINES = 30

    fun of(report: FightReport): List<String> = buildList {
        val stats = report.stats
        val monster = report.monster
        add("Враг: ${monster.code} (${monster.rarity}, ур. ${monster.level}), в бою ${report.packSize}")
        add("Исход: ${report.outcome} за ${seconds(report.duration)} с")
        add("Урон: нанесено ${whole(stats.dealt.total)} (${whole(stats.dps)}/с), получено ${whole(stats.taken.total)}")
        report.phases.takeIf { it.isNotEmpty() }?.let { phases -> add("Фазы: " + phases.joinToString(", ") { "${it.phase} ${whole(it.at)}%" }) }
        killingBlow(report)?.let { add("Добивающий: ${line(it)}") }
        val events = report.events
        add("Журнал: последние ${minOf(LOG_LINES, events.size)} из ${events.size}")
        events.takeLast(LOG_LINES).forEach { add(line(it)) }
    }

    /** Удар, что кончил бой: у победы - последний урон героя, у поражения - последний урон по нему. */
    private fun killingBlow(report: FightReport): CombatEvent? {
        val striker = if (report.outcome == Outcome.WIN) Side.HERO else Side.MONSTER
        return report.events.lastOrNull { it.actor == striker && it.damage > 0 && !it.onSelf }
    }

    /** Строка журнала кодами: время, кто, что, исход, урон и вид, враг, умение, недуги, заметка; здоровье сторон после. */
    private fun line(event: CombatEvent): String = buildString {
        append(seconds(event.time)).append(' ').append(event.actor).append(' ').append(event.action)
        if (event.action != Action.NOTE) append(' ').append(event.kind)
        if (event.damage > 0) append(' ').append(whole(event.damage)).append(event.type?.let { " $it" }.orEmpty())
        if (event.healed > 0) append(" +").append(whole(event.healed))
        append(" foe#").append(event.foe)
        event.skill?.let { append(' ').append(it) }
        event.ailment?.let { append(' ').append(it) }
        if (event.inflicted.isNotEmpty()) append(" ⇒").append(event.inflicted.joinToString(","))
        if (event.stunned) append(" STUN")
        (event.trace as? NoteTrace)?.let { append(" note=").append(it.kind).append(':').append(it.ref).append(':').append(seconds(it.value)) }
        append(" | hero ").append(whole(event.heroLife)).append(" foe ").append(whole(event.monsterLife))
    }

    private fun seconds(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

    private fun whole(value: Double): String = value.roundToLong().toString()
}
