package com.sperance.exileforge.core.questboard

import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.quests.QuestRepository
import com.sperance.exileforge.core.quests.QuestTurnIn
import com.sperance.exileforge.core.quests.Quests
import com.sperance.exileforge.core.quests.RunTurnIns
import com.sperance.exileforge.core.quests.StoryAfter
import com.sperance.exileforge.core.quests.StoryOverlay
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
import com.sperance.exileforge.rules.content.StoryBookOutcome
import com.sperance.exileforge.rules.content.StoryBookTaken
import com.sperance.exileforge.rules.text.LocaleKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Квесты. Сервер бросает, считает и платит; клиент читает доску, называет квест и рисует доску,
 * которой команда ответила. Награда двигает героя - золото, опыт, сферы, - так что после сдачи герой перечитывается.
 * Действия общие для экрана квестов и для игры вне него: прогрев, возврат в приложение, конец похода.
 */
class QuestActions(
    private val repository: QuestRepository,
    private val story: StoryOverlay,
    private val heroes: HeroRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val content: ContentLoader,
    private val scope: CoroutineScope,
) {
    private val api: GameApi get() = connection.api

    private fun quests(transform: (Quests) -> Quests) = repository.update(transform)

    /** Доски героя, как они прочитаны сейчас: снимок на входе в заход. */
    fun board(): Quests = repository.state.value

    /** Доска героя; [fresh] - экран открылся: ответ моложе полуминуты не перечитывается. */
    fun load(fresh: Boolean = false) = commands.read(Reads.QUESTS, tag = heroes.heroId, maxAgeMs = freshness(fresh)) {
        val id = hero()
        content.ensure()
        land(id, api.quests.board(id))
    }

    /** Доска героя [id] из ответа - своего или входа в Город. */
    internal fun land(id: String, board: QuestBoard) {
        if (heroes.onScreen(id)) quests { it.copy(board = board) }
    }

    fun loadGuild(fresh: Boolean = false) = commands.read(Reads.GUILD_QUESTS, tag = heroes.heroId, maxAgeMs = freshness(fresh)) {
        val id = hero()
        val guild = api.quests.guild(id)
        if (heroes.onScreen(id)) quests { it.copy(guild = guild) }
    }

    /**
     * Сдать задание [questId]. Шаг сюжета игрок сдаёт только так, по одному: после сдачи его текст «после» встаёт поверх игры
     * ([StoryOverlay]) - глава и шаг берутся с доски до сдачи.
     */
    fun claim(questId: String) = storyAfter(questId).let { after ->
        command(ui("quest.toast.claimed")) { id ->
            show(id, api.quests.claim(id, questId))
            if (after != null && heroes.onScreen(id)) story.show(after)
        }
    }

    /** Перечитать текст «после» уже сданного шага [step] главы [chapter] - то же окно, что после сдачи. */
    fun reread(chapter: Int, step: String) = story.show(StoryAfter(chapter, step))

    fun take(offerId: String) = board(ui("quest.toast.taken")) { api.quests.take(it, offerId) }

    /** Невзятый листок [offerId] снят с доски: место заполнится само по своему таймеру. */
    fun reset(offerId: String) = board(ui("quest.toast.reset")) { api.quests.reset(it, offerId) }
    fun abandon(questId: String) = board(ui("quest.toast.abandoned")) { api.quests.abandon(it, questId) }

    /** Закон трона [code] за пройденную главу - навсегда; лист героя перечитывается снимком того же ответа. */
    fun law(code: String) = board(ui("story.law.toast", loc(LocaleKey.lawName(code)))) { api.quests.law(it, code) }

    /**
     * Книга умения [code] за пройденную главу [chapter]: неизученное умение, что герой тянет, сервер учит сразу,
     * иначе книга ложится в сумку - тост называет, что стало с книгой ([bookToast]); снимок героя - тем же ответом.
     */
    fun book(chapter: Int, code: String) = command(::bookToast) { id ->
        val taken = api.quests.book(id, chapter, code)
        show(id, taken.board)
        taken
    }

    fun claimGuild(questId: String? = null, goal: String? = null) = command(ui("quest.toast.claimed")) { id ->
        val guild = api.quests.claimGuild(id, questId, goal)
        if (heroes.onScreen(id)) quests { it.copy(guild = guild) }
        heroes.money(guild.money)
    }

    /**
     * После похода: всё, что поход закончил, сдаётся разом - личные квесты и доля героя в
     * гильдейских, но не шаг сюжета: его игрок сдаёт сам. Сдаёт сам сервер в ответе журнала, что закрыл заход ([turnIns], доски
     * гильдии - [guild]); не сдал - сдача отдельным запросом, как прежде. Сданное и готовый к сдаче шаг сюжета уходят в [shown] -
     * итоги карты; не взявшие их итоги (уже закрыты) - тост. Молча при ошибке: пропущенный вызов ничего не теряет, квесты ждут на
     * доске, а концу похода не место для отказа.
     *
     * Заголовки - шаблоны над целью; цели берутся с досок, какими они стояли до сдачи.
     */
    fun turnIn(heroId: String, turnIns: QuestClaimAll?, guild: GuildQuests?, shown: (RunTurnIns) -> Boolean) {
        scope.launch {
            val targets = repository.state.value.targets()
            val result = turnIns ?: quietly { api.quests.claimAll(heroId) } ?: return@launch
            if (!heroes.onScreen(heroId)) return@launch
            val boards = settle(heroId, result, guild, targets)
            val ready = result.board.awaitsHand
            if (result.claimed.isEmpty() && !ready) return@launch
            val named = targets + Quests(guild = boards).targets()
            val lines = RunTurnIns(result.claimed.map { QuestTurnIn(title(it, named), it.rewards, it.kind, it.run) }, ready)
            if (!shown(lines)) notices.toast(turnInLine(result.claimed, named, ready))
        }
    }

    /**
     * «Сдать всё» доски заданий: всё готовое разом, кроме шага сюжета, - тот же запрос, что сдача после похода, но командой экрана
     * (по одной за раз, отказ - тостом); тост - что сдано и награды суммой, и что шаг сюжета ждёт своей сдачи.
     */
    fun claimAll() = command({ line: String -> line }) { id ->
        val targets = repository.state.value.targets()
        val result = api.quests.claimAll(id)
        val boards = if (heroes.onScreen(id)) settle(id, result, null, targets) else null
        turnInLine(result.claimed, targets + Quests(guild = boards).targets(), result.board.awaitsHand)
    }

    /** Шаг сюжета [questId] с доски, как она стоит до сдачи: глава и код шага; не шаг сюжета - null. */
    private fun storyAfter(questId: String): StoryAfter? {
        val board = repository.state.value.board ?: return null
        return board.story?.takeIf { it.id == questId }?.let { StoryAfter(board.chapter, it.goal) }
    }

    /**
     * Сдача разом героя [id] легла: доска и золото - из ответа [result], доски гильдии - [guild] или перечитанные, если сдалась доля
     * в задании, которого нет среди целей [targets] до сдачи. Итог - доски гильдии, что легли (их цели называют сданное).
     */
    private suspend fun settle(id: String, result: QuestClaimAll, guild: GuildQuests?, targets: Map<String, Long>): GuildQuests? {
        quests { it.copy(board = result.board) }
        heroes.money(result.money)
        val boards = guild ?: if (result.claimed.any { it.questId !in targets && it.kind in GUILD_KINDS }) quietly { api.quests.guild(id) } else null
        boards?.let { g -> quests { it.copy(guild = g) } }
        return boards
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

    private fun board(done: String, block: suspend (String) -> QuestBoard) = command(done) { id -> show(id, block(id)) }

    /** Доска [board] из ответа команды героя [id]: на экран, если герой ещё тот же, и его золото. */
    private fun show(id: String, board: QuestBoard) {
        if (heroes.onScreen(id)) quests { it.copy(board = board) }
        heroes.money(board.money)
    }

    /** Одна команда квестов: по одной за раз и без повторов; награда приходит снимком героя в том же ответе. */
    private fun command(done: String, block: suspend (String) -> Unit) = command({ done }, block)

    /** Команда квестов, чей тост [done] читает её итог. */
    private fun <R> command(done: (R) -> String, block: suspend (String) -> R) = commands.task(writing = true, touches = setOf(Reads.QUESTS, Reads.GUILD_QUESTS, Reads.HERO)) {
        notices.toast(done(block(hero())))
    }

    /** Тост книги за главу: умение изучено или книга в сумке (уже изучено, не хватает требований). */
    private fun bookToast(taken: StoryBookTaken): String = when (taken.outcome) {
        StoryBookOutcome.LEARNED -> ui("story.book.learned", SkillText.title(taken.skill))
        StoryBookOutcome.KNOWN, StoryBookOutcome.UNMET -> ui("story.book.toast", SkillText.title(taken.skill))
    }

    /** Тост сдачи разом: что сдано ([claimedLine]) и, если [storyReady], что шаг сюжета готов к сдаче; ничего - пусто. */
    private fun turnInLine(claimed: List<QuestClaimed>, targets: Map<String, Long>, storyReady: Boolean): String = listOfNotNull(
        claimed.takeIf { it.isNotEmpty() }?.let { claimedLine(it, targets) },
        ui("story.ready_to_turn_in").takeIf { storyReady },
    ).joinToString("\n")

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
