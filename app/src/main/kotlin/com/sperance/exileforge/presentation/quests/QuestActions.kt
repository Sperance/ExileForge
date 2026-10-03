package com.sperance.exileforge.presentation.quests

import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.quests.QuestRepository
import com.sperance.exileforge.core.quests.Quests
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.rules.content.QuestBoard
import com.sperance.exileforge.rules.content.QuestClaimed
import com.sperance.exileforge.rules.content.QuestKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Квесты (3.23.0, сервер 1.21.0). Сервер бросает, считает и платит; клиент читает доску, называет квест и рисует доску,
 * которой команда ответила. Награда двигает героя - золото, опыт, сферы, - так что после сдачи герой перечитывается.
 * Действия общие для экрана квестов и для игры вне него: прогрев, возврат в приложение, конец похода.
 */
class QuestActions(
    private val repository: QuestRepository,
    private val heroes: HeroRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val events: GameEvents,
    private val content: ContentLoader,
    private val scope: CoroutineScope,
) {
    private val api: GameApi get() = connection.api

    private fun quests(transform: (Quests) -> Quests) = repository.update(transform)

    fun load() = commands.read(Reads.QUESTS) {
        val id = hero()
        content.ensure()
        val board = api.quests.board(id)
        if (heroes.onScreen(id)) quests { it.copy(board = board) }
    }

    fun loadGuild() = commands.read(Reads.GUILD_QUESTS) {
        val id = hero()
        val guild = api.quests.guild(id)
        if (heroes.onScreen(id)) quests { it.copy(guild = guild) }
    }

    fun claim(questId: String) = board(ui("quest.toast.claimed"), rewarded = true) { api.quests.claim(it, questId) }
    fun take(offerId: String) = board(ui("quest.toast.taken")) { api.quests.take(it, offerId) }
    fun abandon(questId: String) = board(ui("quest.toast.abandoned")) { api.quests.abandon(it, questId) }

    fun claimGuild(questId: String? = null, goal: String? = null) = command(ui("quest.toast.claimed"), rewarded = true) { id ->
        val guild = api.quests.claimGuild(id, questId, goal)
        if (heroes.onScreen(id)) quests { it.copy(guild = guild) }
        heroes.money(guild.money)
    }

    /**
     * После похода (3.24.0, сервер 1.22.0): всё, что поход закончил, сдаётся разом - личные квесты и доля героя в
     * гильдейских, - и тост говорит, что из этого вышло. Молча при ошибке: пропущенный вызов ничего не теряет,
     * квесты ждут на доске, а концу похода не место для отказа.
     */
    fun claimAll(heroId: String) {
        scope.launch {
            // Заголовки - шаблоны над целью; цели берутся с досок, какими они стояли до сдачи.
            val targets = repository.state.value.targets()
            val result = try {
                api.quests.claimAll(heroId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return@launch
            }
            if (!heroes.onScreen(heroId)) return@launch
            quests { it.copy(board = result.board) }
            heroes.money(result.money)
            if (result.claimed.isEmpty()) return@launch
            val guild = if (result.claimed.any { it.questId !in targets && it.kind in GUILD_KINDS }) {
                try {
                    api.quests.guild(heroId)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
            guild?.let { g -> quests { it.copy(guild = g) } }
            notices.toast(claimedLine(result.claimed, targets + Quests(guild = guild).targets()))
        }
    }

    private fun hero(): String = heroes.heroId.also { check(it.isNotBlank()) { ui("auction.choose_character") } }

    private fun board(done: String, rewarded: Boolean = false, block: suspend (String) -> QuestBoard) = command(done, rewarded) { id ->
        val board = block(id)
        if (heroes.onScreen(id)) quests { it.copy(board = board) }
        heroes.money(board.money)
    }

    /** Одна команда квестов: по одной за раз и без повторов; награждённая просит перечитать героя. */
    private fun command(done: String, rewarded: Boolean = false, block: suspend (String) -> Unit) = commands.task(writing = true, touches = setOf(Reads.QUESTS, Reads.GUILD_QUESTS, Reads.HERO)) {
        val id = hero()
        block(id)
        notices.toast(done)
        if (rewarded && heroes.onScreen(id)) events.heroChanged()
    }

    /** Что сдано, одной строкой: первые заголовки, сколько ещё, и награды суммой. */
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

    private companion object {
        /** Квесты гильдии, которые может назвать сдача; их цели - на доске гильдии. */
        val GUILD_KINDS = setOf(QuestKind.GUILD, QuestKind.GUILD_DAILY, QuestKind.GUILD_WEEKLY)

        /** Сколько заголовков тост называет, прежде чем считать остальные. */
        const val SHOWN = 3
    }
}
