package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.rules.content.GuildQuests
import com.sperance.exileforge.rules.content.QuestBoard
import com.sperance.exileforge.rules.content.QuestClaimAll
import com.sperance.exileforge.rules.content.StoryBookTaken

/**
 * Quests: the hero's board — dailies, weeklies, contracts and the story — and the guild's quests.
 * The server rolls, counts and pays; the client names a quest and draws the board it answers. A command is repeated
 * only under its own key ([SafeRetry]): the server answers the repeat with the first answer, never paying twice.
 */
class QuestClient internal constructor(private val http: Transport) {
    /** Доска с выдачей суток, недели и пустых мест (сервер `POST hero/quests/refresh`): чтение доски сервер не пишет. */
    suspend fun board(heroId: String): QuestBoard = http.post("$HERO_QUESTS/refresh", heroQuery(heroId))

    suspend fun claim(heroId: String, questId: String): QuestBoard = onQuest("claim", heroId, questId)

    /** Everything done handed in at once: the personal quests and the hero's share of the guild's. */
    suspend fun claimAll(heroId: String): QuestClaimAll = http.post("$HERO_QUESTS/claimAll", heroQuery(heroId))
    suspend fun abandon(heroId: String, questId: String): QuestBoard = onQuest("abandon", heroId, questId)

    suspend fun take(heroId: String, offerId: String): QuestBoard {
        requireId(offerId)
        return http.post("$HERO_QUESTS/take", heroQuery(heroId, "offerId" to offerId))
    }

    /**
     * Сбросить невзятый контракт [offerId]: место на доске пустеет и само заполнится по своему
     * таймеру `QuestBoard.refills`. Отказ `QU_007` - предложения нет или оно уже взято.
     */
    suspend fun reset(heroId: String, offerId: String): QuestBoard {
        requireId(offerId)
        return http.post("$HERO_QUESTS/reset", heroQuery(heroId, "offerId" to offerId))
    }

    /**
     * Взять закон трона [code] за пройденную главу (сервер, `API_REVISION` 63): один на трон и навсегда. Отказы - `QU_008` (закона
     * нет), `QU_009` (глава трона не пройдена), `QU_010` (закон этого трона уже выбран).
     */
    suspend fun law(heroId: String, code: String): QuestBoard = http.post("$HERO_QUESTS/law", heroQuery(heroId, "code" to code))

    /**
     * Книга умения [code] за пройденную главу [chapter]: одна на главу, из вариантов `StoryBooks.options`.
     * Неизученное умение, что герой тянет, учится сразу - ответ говорит, что стало с книгой
     * ([StoryBookTaken.outcome]). Отказы - `QU_011` (книги нет среди вариантов), `QU_012` (глава не пройдена или книга уже взята).
     */
    suspend fun book(heroId: String, chapter: Int, code: String): StoryBookTaken = http.post("$HERO_QUESTS/book", heroQuery(heroId, "chapter" to chapter.toString(), "code" to code))

    /** Задания гильдии с выдачей суток (сервер `POST guild/quests/refresh`). */
    suspend fun guild(heroId: String): GuildQuests = http.post("$GUILD_QUESTS/refresh", heroQuery(heroId))

    /** The guild's reward: a personal quest by [questId], or a share of a common goal by its [goal] key. */
    suspend fun claimGuild(heroId: String, questId: String? = null, goal: String? = null): GuildQuests {
        require(questId != null || goal != null)
        return http.post("$GUILD_QUESTS/claim", heroQuery(heroId, "questId" to questId, "goal" to goal))
    }

    private suspend fun onQuest(command: String, heroId: String, questId: String): QuestBoard {
        requireId(questId)
        return http.post("$HERO_QUESTS/$command", heroQuery(heroId, "questId" to questId))
    }

    private companion object {
        const val HERO_QUESTS = "api/v1/hero/quests"
        const val GUILD_QUESTS = "api/v1/guild/quests"
    }
}
