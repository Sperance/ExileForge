package com.sperance.exileforge.presentation.guild

import com.sperance.exileforge.core.guild.GuildRepository
import com.sperance.exileforge.core.guild.GuildTab
import com.sperance.exileforge.core.guild.Guilds
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.phrase
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildCard
import com.sperance.exileforge.core.model.guild.GuildMine
import com.sperance.exileforge.core.model.guild.GuildStashView
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.MemberCommand
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.rules.content.GuildMode
import kotlinx.coroutines.CancellationException

/**
 * Гильдии (3.22.0, сервер 1.20.0). Все правила у сервера; клиент называет гильдию или участника, печатает отказ и
 * рисует то, чем команда ответила - всю картину гильдии героя или гильдию, как она теперь стоит. Команда, что двигает
 * героя (вступить, уйти, отдать), просит перечитать его: золото едет на снимке.
 */
class GuildActions(
    private val repository: GuildRepository,
    private val heroes: HeroRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val events: GameEvents,
    private val content: ContentLoader,
) {
    private val api: GameApi get() = connection.api
    private val guilds: Guilds get() = repository.state.value

    private fun guild(transform: (Guilds) -> Guilds) = repository.update(transform)

    fun tab(tab: GuildTab?) = guild { it.copy(tab = tab) }
    fun query(text: String) = guild { it.copy(query = text) }

    /** Сужает список до одной фракции, пустой код - до всех, и читает его снова с первой страницы. */
    fun filterFaction(code: String) {
        guild { it.copy(faction = code) }
        search(0)
    }

    /** Гильдия героя; контент прежде - числа экрана берутся из `guilds.json`. */
    fun load() = commands.read(Reads.GUILD) {
        val id = hero()
        content.ensure()
        val mine = api.guild.mine(id)
        if (heroes.onScreen(id)) guild { it.copy(mine = mine) }
    }

    fun search(page: Int = 0) = commands.read(Reads.GUILD_SEARCH, restart = true) {
        val id = hero()
        val found = guilds.let { api.guild.search(id, it.query, page, it.faction) }
        if (heroes.onScreen(id)) guild { it.copy(search = found) }
    }

    fun create(name: String, tag: String, faction: String, emblem: String, color: String, mode: GuildMode, minLevel: Int) = moving(ui("guild.toast.created", name.trim())) { api.guild.create(it, name, tag, faction, emblem, color, mode, minLevel) }

    /** Открытая гильдия берёт героя сразу; в любую другую - заявка. */
    fun join(card: GuildCard) = if (card.mode == GuildMode.OPEN) {
        moving(ui("guild.toast.joined", card.name)) { api.guild.join(it, card.id) }
    } else {
        moving(ui("guild.toast.applied", card.name)) { api.guild.apply(it, card.id) }
    }

    fun acceptInvite(guildId: String) = moving(ui("guild.toast.joined", inviteName(guildId))) { api.guild.acceptInvite(it, guildId) }
    fun declineInvite(guildId: String) = moving(ui("guild.toast.declined")) { api.guild.declineInvite(it, guildId) }
    fun leave() = moving(ui("guild.toast.left")) { api.guild.leave(it) }
    fun disband() = moving(ui("guild.toast.disbanded")) { api.guild.disband(it) }

    fun acceptApplicant(applicantId: String) = inside(ui("guild.toast.accepted")) { api.guild.acceptApplicant(it, applicantId) }
    fun declineApplicant(applicantId: String) = inside(ui("guild.toast.declined")) { api.guild.declineApplicant(it, applicantId) }
    fun invite(name: String) = inside(ui("guild.toast.invited", name.trim())) { api.guild.invite(it, name) }
    fun member(command: MemberCommand, memberId: String) = inside(ui("guild.toast.done")) { api.guild.member(it, command, memberId) }
    fun settings(mode: GuildMode, minLevel: Int, emblem: String, color: String, announcement: String) = inside(ui("guild.toast.saved")) { api.guild.settings(it, mode, minLevel, emblem, color, announcement) }

    /** Древо бонусов (3.79.0): глава берёт ранг или сбрасывает всё. */
    fun takeNode(node: String) = moving(ui("guild.toast.node")) { api.guild.takeNode(it, node) }
    fun resetTree() = moving(ui("guild.toast.tree_reset")) { api.guild.resetTree(it) }

    /** Хранилище гильдии (3.79.0): чтение, вклад, выдача, ранг вкладки; каждая запись отвечает хранилищем как оно стоит. */
    fun loadStash() = commands.read(Reads.GUILD) {
        val id = hero()
        val stash = api.guild.stash(id)
        if (heroes.onScreen(id)) guild { it.copy(stash = stash) }
    }
    fun deposit(tab: Int, itemId: String?, code: String?, amount: Long) = stashCommand(ui("guild.toast.deposited")) { api.guild.deposit(it, tab, itemId, code, amount) }
    fun take(entryId: String) = stashCommand(ui("guild.toast.taken")) { api.guild.take(it, entryId) }
    fun tabRank(tab: Int, minRank: Int) = stashCommand(ui("guild.toast.saved")) { api.guild.tabRank(it, tab, minRank) }

    private fun stashCommand(done: String, block: suspend (String) -> GuildStashView) = command(done) { id ->
        val stash = block(id)
        guild { it.copy(stash = stash) }
        refreshHero(id)
    }

    /** Золото или сфера в казну: ответ несёт гильдию, строку героя и остаток золота, так что перечитывается один герой. */
    fun contribute(item: String, amount: Long) = commands.task(writing = true, touches = setOf(Reads.GUILD, Reads.HERO)) {
        val id = hero()
        val given = api.guild.contribute(id, item, amount)
        if (!heroes.onScreen(id)) return@task
        guild { it.copy(mine = it.mine?.copy(guild = given.guild, me = given.me)) }
        heroes.money(given.money)
        notices.toast(ui("guild.toast.contributed"))
        refreshHero(id)
    }

    /** Журнал с новейшей страницы или следующая за показанными; пустая страница - конец. */
    fun loadLog(more: Boolean = false) = commands.read(Reads.GUILD_LOG, restart = !more) {
        val id = hero()
        val page = if (more) guilds.logPage + 1 else 0
        val entries = api.guild.log(id, page)
        if (heroes.onScreen(id)) guild { it.copy(log = if (more) it.log + entries else entries, logPage = page, logEnd = entries.isEmpty()) }
    }

    private fun inviteName(guildId: String): String = guilds.mine?.invites?.firstOrNull { it.guild.id == guildId }?.guild?.name.orEmpty()

    private fun hero(): String = heroes.heroId.also { check(it.isNotBlank()) { ui("auction.choose_character") } }

    /**
     * Команда, что вводит героя в гильдию или выводит из неё - или отвечает на приглашение - и отвечает всей [GuildMine].
     * Герой перечитывается после: золото едет на снимке, 304 - когда его принесла команда.
     */
    private fun moving(done: String, block: suspend (String) -> GuildMine) = command(done) { id ->
        val mine = block(id)
        guild { it.copy(mine = mine, tab = if (mine.guild == null) null else it.tab) }
        refreshHero(id)
    }

    /** Команда внутри гильдии, отвеченная гильдией, как она теперь стоит. */
    private fun inside(done: String, block: suspend (String) -> GuildView) = command(done) { id ->
        val view = block(id)
        guild { it.copy(mine = it.mine?.let { mine -> mine.copy(guild = view, me = view.member(id) ?: mine.me) }) }
    }

    /** Одна команда гильдии: по одной за раз - героя под ней не сменить - и без повторов. */
    private fun command(done: String, block: suspend (String) -> Unit) = commands.task(writing = true, touches = setOf(Reads.GUILD, Reads.HERO)) {
        val id = hero()
        block(id)
        notices.toast(done)
    }

    /** Чтения после команды, которую сервер уже совершил: их ошибка - не ошибка команды. */
    private suspend fun after(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            commands.refuse(phrase("guild.done_refresh"))
        }
    }

    private fun refreshHero(heroId: String) {
        if (heroes.onScreen(heroId)) events.heroChanged()
    }
}
