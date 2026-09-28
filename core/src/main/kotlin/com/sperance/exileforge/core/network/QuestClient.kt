package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.rules.content.GuildQuests
import com.sperance.exileforge.rules.content.QuestBoard
import com.sperance.exileforge.rules.content.QuestClaimAll

/**
 * Quests (3.23.0, server 1.21.0): the hero's board — dailies, weeklies, contracts and the story — and the guild's quests.
 * The server rolls, counts and pays; the client names a quest and draws the board it answers. Commands are never retried:
 * a repeat would be refused at best and paid twice at worst.
 */
class QuestClient internal constructor(private val http: Transport) {
    suspend fun board(heroId: String): QuestBoard = http.get("$HERO_QUESTS", heroQuery(heroId))

    suspend fun claim(heroId: String, questId: String): QuestBoard = onQuest("claim", heroId, questId)

    /** Everything done handed in at once (server 1.22.0): the personal quests and the hero's share of the guild's. */
    suspend fun claimAll(heroId: String): QuestClaimAll = http.post("$HERO_QUESTS/claimAll", heroQuery(heroId))
    suspend fun abandon(heroId: String, questId: String): QuestBoard = onQuest("abandon", heroId, questId)

    suspend fun take(heroId: String, offerId: String): QuestBoard {
        requireId(offerId)
        return http.post("$HERO_QUESTS/take", heroQuery(heroId, "offerId" to offerId))
    }

    suspend fun guild(heroId: String): GuildQuests = http.get("$GUILD_QUESTS", heroQuery(heroId))

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
