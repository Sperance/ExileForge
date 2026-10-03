package com.sperance.exileforge.core.quests

import com.sperance.exileforge.rules.content.GuildQuests
import com.sperance.exileforge.rules.content.QuestBoard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Разделы доски квестов Города. */
enum class QuestTab { DAILY, WEEKLY, CONTRACTS, STORY }

/**
 * Квесты героя, как сервер ответил последним (3.23.0): [board] - ежедневные, недельные, контракты и шаг истории
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

/** Единственный источник правды о квестах (3.80.10). */
class QuestRepository {
    private val mutable = MutableStateFlow(Quests())
    val state: StateFlow<Quests> = mutable

    fun update(transform: (Quests) -> Quests) = mutable.update(transform)

    fun clear() = update { Quests() }
}
