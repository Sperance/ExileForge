package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.QuestState
import com.sperance.exileforge.presentation.state.QuestTab
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.rules.content.QuestBoard
import com.sperance.exileforge.rules.content.QuestClaimed
import com.sperance.exileforge.rules.content.QuestKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
    fun take(offerId: String) = board(ui("quest.toast.taken")) { runtime.api.quests.take(it, offerId) }
    fun abandon(questId: String) = board(ui("quest.toast.abandoned")) { runtime.api.quests.abandon(it, questId) }

    fun claimGuild(questId: String? = null, goal: String? = null) = command(ui("quest.toast.claimed"), rewarded = true) { id ->
        val guild = runtime.api.quests.claimGuild(id, questId, goal)
        if (onScreen(id)) quests { it.copy(guild = guild) }
        gold(guild.money)
    }

    /**
     * After a run (3.24.0, server 1.22.0): everything the run finished is handed in at once — the personal quests and the
     * hero's share of the guild's — and a toast says what came of it. Quiet on failure: a missed call loses nothing, the
     * quests wait on the board, and a run's end is no place for a refusal.
     */
    fun claimAll(heroId: String) { with(runtime) { scope.launch {
        // The titles are templates over the goal's target, read off the boards as they stood before the claim.
        val targets = targets(state.value.quests)
        val result = try { runtime.api.quests.claimAll(heroId) } catch (e: CancellationException) { throw e } catch (_: Exception) { return@launch }
        if (!onScreen(heroId)) return@launch
        quests { it.copy(board = result.board) }
        gold(result.money)
        if (result.claimed.isEmpty()) return@launch
        val guild = if (result.claimed.any { it.questId !in targets && it.kind in GUILD_KINDS }) try { runtime.api.quests.guild(heroId) }
            catch (e: CancellationException) { throw e } catch (_: Exception) { null } else null
        guild?.let { g -> quests { it.copy(guild = g) } }
        toast(claimedLine(result.claimed, targets + targets(QuestState(guild = guild))))
    } } }

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

    /** What was handed in, as one line: the first few titles, how many more, and the rewards summed. */
    private fun claimedLine(claimed: List<QuestClaimed>, targets: Map<String, Long>): String {
        val titles = claimed.map { loc(it.title, listOf(targets[it.questId]?.let { t -> number(t.toDouble()) } ?: "…")) }
        val named = titles.take(SHOWN).joinToString(", ") + if (titles.size > SHOWN) " " + ui("quest.toast.more", titles.size - SHOWN) else ""
        val rewards = claimed.map { it.rewards }
        val orbs = rewards.flatMap { it.orbs.entries }.groupBy({ it.key }, { it.value }).mapValues { it.value.sum() }
        val parts = listOfNotNull(
            rewards.sumOf { it.gold }.takeIf { it > 0 }?.let { ui("quest.toast.gold", number(it.toDouble())) },
            rewards.sumOf { it.experience }.takeIf { it > 0 }?.let { ui("quest.toast.experience", number(it)) },
        ) + orbs.map { (code, amount) -> ui("quest.toast.orb", itemTitle(code), amount) }
        return ui("quest.toast.claimed_all", claimed.size, named) + parts.joinToString(" · ").takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
    }

    /** Each quest's and common goal's target by the id a claim names it by: a quest's id, a guild goal's key. */
    private fun targets(state: QuestState): Map<String, Long> = buildMap {
        state.board?.let { b -> (b.daily + b.weekly + b.contracts + listOfNotNull(b.story)).forEach { put(it.id, it.target) } }
        state.guild?.let { g ->
            g.personal.forEach { put(it.id, it.target) }
            (g.daily + g.weekly).forEach { put(it.goal.key, it.goal.target) }
        }
    }

    private fun gold(money: Long) = update { s -> s.copy(play = s.play.copy(hero = s.play.hero?.let { it.copy(info = it.info.copy(money = money)) })) }

    private fun quests(transform: (QuestState) -> QuestState) = update { it.copy(quests = transform(it.quests)) }

    private companion object {
        /** The guild's quests a claim may name; their targets are on the guild's board. */
        val GUILD_KINDS = setOf(QuestKind.GUILD, QuestKind.GUILD_DAILY, QuestKind.GUILD_WEEKLY)
        /** Titles the toast names before it counts the rest. */
        const val SHOWN = 3
    }
}
