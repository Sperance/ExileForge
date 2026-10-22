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

/**
 * Итог сдачи разом после карты: [quests] - сданное, [storyReady] - шаг сюжета выполнен и ждёт, когда игрок сдаст его сам.
 */
data class RunTurnIns(val quests: List<QuestTurnIn>, val storyReady: Boolean)

/** Шаг сюжета [step] главы [chapter], чей текст «после» ждёт показа поверх игры. */
data class StoryAfter(val chapter: Int, val step: String)

/** Шаг сюжета на доске выполнен, но ещё не сдан. */
val QuestBoard.storyReady: Boolean get() = story?.let { it.done && !it.claimed } == true

/**
 * Единственный источник правды о квестах. [story] - текст «после» сданного или перечитываемого шага сюжета к показу; живёт только
 * в памяти: окно, закрытое любым путём, уже показано.
 */
class QuestRepository {
    private val mutable = MutableStateFlow(Quests())
    val state: StateFlow<Quests> = mutable

    private val pending = MutableStateFlow<StoryAfter?>(null)
    val story: StateFlow<StoryAfter?> = pending

    fun update(transform: (Quests) -> Quests) = mutable.update(transform)

    /** Показать текст «после» шага [after]. */
    fun tell(after: StoryAfter) {
        pending.value = after
    }

    /** Окно шага [after] закрыто; к показу уже стоит другой шаг - он остаётся. */
    fun told(after: StoryAfter) {
        pending.compareAndSet(after, null)
    }

    fun clear() {
        update { Quests() }
        pending.value = null
    }
}
