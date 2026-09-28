package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.QuestState
import com.sperance.exileforge.presentation.state.QuestTab
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.rules.content.QuestBoard
import kotlinx.coroutines.CancellationException

/**
 * Quests (3.23.0, server 1.21.0). The server rolls, counts and pays; the client reads the board, names a quest and draws the
 * board the command answers. A reward moves the hero — gold, experience, orbs — so the hero is read after every claim.
 */
class QuestViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {

    fun tab(tab: QuestTab) = quests { it.copy(tab = tab) }

    fun load() { with(runtime) { read(Reads.QUESTS) {
        val id = heroId
        check(id.isNotBlank()) { ui("auction.choose_character") }
        ensureContent()
        val board = runtime.api.quests.board(id)
        if (onScreen(id)) quests { it.copy(board = board) }
    } } }

    fun loadGuild() { with(runtime) { read(Reads.GUILD_QUESTS) {
        val id = heroId
        check(id.isNotBlank()) { ui("auction.choose_character") }
        val guild = runtime.api.quests.guild(id)
        if (onScreen(id)) quests { it.copy(guild = guild) }
    } } }

    fun claim(questId: String) = board(ui("quest.toast.claimed"), rewarded = true) { runtime.api.quests.claim(it, questId) }
    fun reroll(questId: String) = board(ui("quest.toast.rerolled"), rewarded = true) { runtime.api.quests.reroll(it, questId) }
    fun take(offerId: String) = board(ui("quest.toast.taken")) { runtime.api.quests.take(it, offerId) }
    fun abandon(questId: String) = board(ui("quest.toast.abandoned")) { runtime.api.quests.abandon(it, questId) }
    fun renew() = board(ui("quest.toast.renewed"), rewarded = true) { runtime.api.quests.renew(it) }

    fun claimGuild(questId: String? = null, goal: String? = null) = command(ui("quest.toast.claimed"), rewarded = true) { id ->
        val guild = runtime.api.quests.claimGuild(id, questId, goal)
        if (onScreen(id)) quests { it.copy(guild = guild) }
        gold(guild.money)
    }

    private fun board(done: String, rewarded: Boolean = false, block: suspend (String) -> QuestBoard) = command(done, rewarded) { id ->
        val board = block(id)
        if (onScreen(id)) quests { it.copy(board = board) }
        gold(board.money)
    }

    /** One quest command: one at a time and never retried; a paid or rewarded one reads the hero after it. */
    private fun command(done: String, rewarded: Boolean = false, block: suspend (String) -> Unit) { with(runtime) {
        task(writing = true, touches = setOf(Reads.QUESTS, Reads.GUILD_QUESTS, Reads.HERO)) {
            val id = heroId
            check(id.isNotBlank()) { ui("auction.choose_character") }
            block(id)
            toast(done)
            if (rewarded && onScreen(id)) {
                try { heroViewModel.readHero() } catch (e: CancellationException) { throw e } catch (_: Exception) { }
            }
        }
    } }

    private fun gold(money: Long) = update { s -> s.copy(play = s.play.copy(hero = s.play.hero?.let { it.copy(info = it.info.copy(money = money)) })) }

    private fun quests(transform: (QuestState) -> QuestState) = update { it.copy(quests = transform(it.quests)) }
}
