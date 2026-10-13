package com.sperance.exileforge.presentation.quests

import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.quests.QuestRepository
import com.sperance.exileforge.core.quests.QuestTurnIn
import com.sperance.exileforge.core.quests.Quests
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.freshness
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.rules.content.GuildQuests
import com.sperance.exileforge.rules.content.QuestBoard
import com.sperance.exileforge.rules.content.QuestClaimAll
import com.sperance.exileforge.rules.content.QuestClaimed
import com.sperance.exileforge.rules.content.QuestKind
import com.sperance.exileforge.rules.text.LocaleKey
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
    private val content: ContentLoader,
    private val scope: CoroutineScope,
) {
    private val api: GameApi get() = connection.api

    private fun quests(transform: (Quests) -> Quests) = repository.update(transform)

    /** Доски героя, как они прочитаны сейчас (4.10.0): снимок на входе в заход. */
    fun board(): Quests = repository.state.value

    /** Доска героя; [fresh] (3.94.1) - экран открылся: ответ моложе полуминуты не перечитывается. */
    fun load(fresh: Boolean = false) = commands.read(Reads.QUESTS, tag = heroes.heroId, maxAgeMs = freshness(fresh)) {
        val id = hero()
        content.ensure()
        val board = api.quests.board(id)
        if (heroes.onScreen(id)) quests { it.copy(board = board) }
    }

    fun loadGuild(fresh: Boolean = false) = commands.read(Reads.GUILD_QUESTS, tag = heroes.heroId, maxAgeMs = freshness(fresh)) {
        val id = hero()
        val guild = api.quests.guild(id)
        if (heroes.onScreen(id)) quests { it.copy(guild = guild) }
    }

    fun claim(questId: String) = board(ui("quest.toast.claimed")) { api.quests.claim(it, questId) }
    fun take(offerId: String) = board(ui("quest.toast.taken")) { api.quests.take(it, offerId) }

    /** Невзятый листок [offerId] снят с доски (4.2.0): место заполнится само по своему таймеру. */
    fun reset(offerId: String) = board(ui("quest.toast.reset")) { api.quests.reset(it, offerId) }
    fun abandon(questId: String) = board(ui("quest.toast.abandoned")) { api.quests.abandon(it, questId) }

    /** Закон трона [code] за пройденную главу - навсегда; лист героя перечитывается снимком того же ответа. */
    fun law(code: String) = board(ui("story.law.toast", loc(LocaleKey.lawName(code)))) { api.quests.law(it, code) }

    fun claimGuild(questId: String? = null, goal: String? = null) = command(ui("quest.toast.claimed")) { id ->
        val guild = api.quests.claimGuild(id, questId, goal)
        if (heroes.onScreen(id)) quests { it.copy(guild = guild) }
        heroes.money(guild.money)
    }

    /**
     * После похода (3.24.0, сервер 1.22.0): всё, что поход закончил, сдаётся разом - личные квесты и доля героя в
     * гильдейских. С 4.11.x сдаёт сам сервер в ответе журнала, что закрыл заход ([turnIns], доски гильдии - [guild]); не сдал -
     * сдача отдельным запросом, как прежде. Сданное (4.10.0) уходит в [shown] - строки итогов карты; не взявшие его итоги (уже
     * закрыты) - тост. Молча при ошибке: пропущенный вызов ничего не теряет, квесты ждут на доске, а концу похода не место для отказа.
     */
    fun turnIn(heroId: String, turnIns: QuestClaimAll?, guild: GuildQuests?, shown: (List<QuestTurnIn>) -> Boolean) {
        scope.launch {
            // Заголовки - шаблоны над целью; цели берутся с досок, какими они стояли до сдачи.
            val targets = repository.state.value.targets()
            val result = turnIns ?: quietly { api.quests.claimAll(heroId) } ?: return@launch
            if (!heroes.onScreen(heroId)) return@launch
            quests { it.copy(board = result.board) }
            heroes.money(result.money)
            val boards = guild ?: if (result.claimed.any { it.questId !in targets && it.kind in GUILD_KINDS }) quietly { api.quests.guild(heroId) } else null
            boards?.let { g -> quests { it.copy(guild = g) } }
            if (result.claimed.isEmpty()) return@launch
            val named = targets + Quests(guild = boards).targets()
            if (!shown(result.claimed.map { QuestTurnIn(title(it, named), it.rewards) })) notices.toast(claimedLine(result.claimed, named))
        }
    }

    /** Чтение конца похода: ошибка - null, отмена идёт дальше. */
    private suspend fun <T> quietly(read: suspend () -> T): T? = try {
        read()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    /** Заголовок сданного задания с его целью из [targets]; цели нет - «…». */
    private fun title(claimed: QuestClaimed, targets: Map<String, Long>): String = loc(claimed.title, listOf(targets[claimed.questId]?.let { t -> number(t.toDouble()) } ?: "…"))

    private fun hero(): String = heroes.heroId.also { check(it.isNotBlank()) { ui("auction.choose_character") } }

    private fun board(done: String, block: suspend (String) -> QuestBoard) = command(done) { id ->
        val board = block(id)
        if (heroes.onScreen(id)) quests { it.copy(board = board) }
        heroes.money(board.money)
    }

    /** Одна команда квестов: по одной за раз и без повторов; награда приходит снимком героя в том же ответе (3.94.1). */
    private fun command(done: String, block: suspend (String) -> Unit) = commands.task(writing = true, touches = setOf(Reads.QUESTS, Reads.GUILD_QUESTS, Reads.HERO)) {
        block(hero())
        notices.toast(done)
    }

    /** Что сдано, одной строкой: первые заголовки, сколько ещё, и награды суммой. */
    private fun claimedLine(claimed: List<QuestClaimed>, targets: Map<String, Long>): String {
        val titles = claimed.map { title(it, targets) }
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
