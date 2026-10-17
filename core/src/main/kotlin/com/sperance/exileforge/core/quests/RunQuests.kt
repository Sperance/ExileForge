package com.sperance.exileforge.core.quests

import com.sperance.exileforge.rules.content.Quest
import com.sperance.exileforge.rules.content.QuestCondition
import com.sperance.exileforge.rules.content.QuestCounter

/**
 * Задание, каким оно будет, когда сервер примет события захода: боевой счётчик прибавляет [tally] этого захода;
 * «за один заход» считает только его. Прочие задания - как на доске. Доска - снятая на входе в заход (лист «Задания» карты):
 * перечитанная уже несёт принятые события, и счёт задвоился бы.
 */
fun Quest.live(tally: Map<String, Long>): Quest {
    if (claimed || counter !in QuestCounter.COMBAT) return this
    val run = tally[counter] ?: 0L
    val now = if (QuestCondition.ONE_RUN in conditions) run else progress + run
    return copy(progress = now.coerceAtMost(target))
}

/** Задания героя, что двигает заход: личные и гильдейские, ещё не сданные. */
fun Quests.running(): List<Quest> = listOfNotNull(board?.story) + board?.contracts.orEmpty() + board?.daily.orEmpty() +
    board?.weekly.orEmpty() + guild?.personal.orEmpty()
