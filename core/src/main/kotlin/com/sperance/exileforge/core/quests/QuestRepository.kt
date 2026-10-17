package com.sperance.exileforge.core.quests

import com.sperance.exileforge.rules.content.GuildQuests
import com.sperance.exileforge.rules.content.QuestBoard
import com.sperance.exileforge.rules.content.QuestKind
import com.sperance.exileforge.rules.content.QuestReward
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Разделы доски квестов Города; сюжет - не раздел доски, а свой экран «Похода», открытый с первого уровня. */
enum class QuestTab { DAILY, WEEKLY, CONTRACTS }

/**
 * Квесты героя, как сервер ответил последним: [board] - ежедневные, недельные, контракты и шаг истории
 * на доске Города; [guild] - личные и общие квесты гильдии. Оба null, пока не прочитаны.
 */
data class Quests(val board: QuestBoard? = null, val guild: GuildQuests? = null) {
    /** Цель каждого квеста и общей цели по id, которым её называет сдача: id квеста, ключ цели гильдии. */
    fun targets(): Map<String, Long> = buildMap {
        board?.let { b -> (b.daily + b.weekly + b.contracts + listOfNotNull(b.story)).forEach { put(it.id, it.target) } }
        guild?.let { g ->
            g.personal.forEach { put(it.id, it.target) }
            (g.daily + g.weekly).forEach { put(it.goal.key, it.goal.target) }
        }
    }
}

/**
 * Задание, сданное разом после карты: его заголовок с целью и награда - строкой в итогах карты; [kind] - вид задания (метка у
 * прочего сданного), [run] - его продвинул этот заход («За эту карту»), иначе оно сдалось заодно («Также сдано»).
 */
data class QuestTurnIn(val title: String, val rewards: QuestReward, val kind: QuestKind, val run: Boolean)

/** Единственный источник правды о квестах. */
class QuestRepository {
    private val mutable = MutableStateFlow(Quests())
    val state: StateFlow<Quests> = mutable

    fun update(transform: (Quests) -> Quests) = mutable.update(transform)

    fun clear() = update { Quests() }
}
