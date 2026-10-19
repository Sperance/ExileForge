package com.sperance.exileforge.core.crafts

import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.rules.roll.ActiveWork
import com.sperance.exileforge.rules.roll.WorkGains

/**
 * Досчёт после отлучки: что принесли циклы работы [job] профессии [profession] (вариант [choice]) за [millis] мс без игрока; [running] -
 * работа идёт дальше, иначе она встала (кончился вход, одноразовая работа сделала свой цикл или игрока не было `offlineHours`); [level] - уровень профессии после досчёта.
 * Показывается листом «Пока вас не было» один раз, до тех пор вкладка ремёсел помечена.
 */
data class Absence(
    val gains: WorkGains,
    val millis: Long,
    val profession: String,
    val job: String,
    val choice: String,
    val running: Boolean,
    val level: Int,
) {
    /** Ещё одна отлучка до показа листа: сборы складываются, время - тоже, работа и уровень - последние. */
    operator fun plus(next: Absence): Absence = next.copy(gains = gains + next.gains, millis = millis + next.millis)

    companion object {
        /**
         * Отлучка из ответа досчёта [answer]: циклы были, а прежний досчёт старше `awayMinutes` правил ремёсел. Работа - идущая, а
         * если она встала - прежняя [before] (ремёсла прошлого ответа или снимок героя); без неё отлучки не назвать.
         */
        fun of(answer: CraftsState, before: ActiveWork?): Absence? {
            val rules = answer.rules ?: return null
            val millis = answer.now - answer.from
            if (answer.from <= 0 || answer.gains.isEmpty || millis < rules.awayMinutes * MINUTE) return null
            val work = answer.work
            val profession = work?.profession ?: before?.profession ?: return null
            return Absence(
                gains = answer.gains,
                millis = millis,
                profession = profession,
                job = work?.job ?: before?.job.orEmpty(),
                choice = work?.choice ?: before?.choice.orEmpty(),
                running = work != null && !work.stopped,
                level = answer.professions.firstOrNull { it.code == profession }?.level ?: 1,
            )
        }

        private const val MINUTE = 60_000L
    }
}
