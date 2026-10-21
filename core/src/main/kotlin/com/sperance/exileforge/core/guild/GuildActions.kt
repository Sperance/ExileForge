package com.sperance.exileforge.core.guild

import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.phrase
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildCard
import com.sperance.exileforge.core.model.guild.GuildDepositParcel
import com.sperance.exileforge.core.model.guild.GuildMine
import com.sperance.exileforge.core.model.guild.GuildStashView
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.core.model.paging.Page
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.MemberCommand
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.freshness
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.rules.content.GuildMode

/**
 * Гильдии. Все правила у сервера; клиент называет гильдию или участника, печатает отказ и
 * рисует то, чем команда ответила - всю картину гильдии героя или гильдию, как она теперь стоит. Команда, что двигает
 * героя (вступить, уйти, отдать), просит перечитать его: золото едет на снимке.
 */
class GuildActions(
    private val repository: GuildRepository,
    private val heroes: HeroRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val notices: Notices,
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
        search()
    }

    /** Гильдия героя; контент прежде - числа экрана берутся из `guilds.json`. [fresh] - экран открылся. */
    fun load(fresh: Boolean = false) = commands.read(Reads.GUILD, tag = heroes.heroId, maxAgeMs = freshness(fresh)) {
        val id = hero()
        content.ensure()
        land(id, api.guild.mine(id))
    }

    /** Гильдия героя [id] из ответа - своего или входа в Город. */
    internal fun land(id: String, mine: GuildMine) {
        if (heroes.onScreen(id)) guild { it.copy(mine = mine) }
    }

    /** Первая страница списка гильдий под набранным поиском и фракцией; новый вызов сменяет идущий. */
    fun search() = commands.read(Reads.GUILD_SEARCH, restart = true) {
        val id = hero()
        val asked = guilds
        val found = api.guild.search(id, asked.query, 0, asked.faction)
        if (heroes.onScreen(id)) guild { if (it.faction == asked.faction) it.copy(search = found, searched = asked.query) else it }
    }

    /**
     * Следующая страница списка гильдий под прочитанными (листание до края): чтение не перезапускается, страница ложится, только пока
     * поиск, фракция и прочитанные страницы те же.
     */
    fun moreSearch() = commands.read(Reads.GUILD_SEARCH) {
        val id = hero()
        val asked = guilds
        if (!asked.search.more) return@read
        val next = api.guild.search(id, asked.searched, asked.search.page + 1, asked.faction)
        if (heroes.onScreen(id)) guild { if (it.faction == asked.faction && it.searched == asked.searched && it.search.page == asked.search.page) it.copy(search = it.search.followedBy(next) { card -> card.id }) else it }
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
    fun invite(target: String) = inside(ui("guild.toast.invited", guilds.recruit?.name ?: target.trim())) {
        api.guild.invite(it, target).also { guild { state -> state.copy(recruit = null) } }
    }

    /** Превью приглашения: одно чтение по нажатию «Найти», не на каждую букву - сервер не нагружается вводом. */
    fun lookup(target: String) = commands.read(Reads.GUILD_SEARCH, restart = true) {
        val id = hero()
        val found = api.guild.lookup(id, target)
        if (heroes.onScreen(id)) guild { it.copy(recruit = found) }
    }

    fun clearRecruit() = guild { it.copy(recruit = null) }

    /** Новый код вступления. */
    fun renewCode() = inside(ui("guild.toast.code_renewed")) { api.guild.renewCode(it) }

    /** Вступление по коду гильдии. */
    fun joinByCode(code: String) = moving(ui("guild.toast.joined_code")) { api.guild.joinByCode(it, code) }
    fun member(command: MemberCommand, memberId: String) = inside(ui("guild.toast.done")) { api.guild.member(it, command, memberId) }
    fun settings(mode: GuildMode, minLevel: Int, announcement: String) = inside(ui("guild.toast.saved")) { api.guild.settings(it, mode, minLevel, announcement) }

    /** Древо бонусов: глава берёт ранг или сбрасывает всё. */
    fun takeNode(node: String) = moving(ui("guild.toast.node")) { api.guild.takeNode(it, node) }
    fun resetTree() = moving(ui("guild.toast.tree_reset")) { api.guild.resetTree(it) }

    /** Хранилище гильдии: чтение, вклад, выдача, ранг вкладки; каждая запись отвечает хранилищем как оно стоит. */
    fun loadStash() = commands.read(Reads.GUILD) {
        val id = hero()
        val stash = api.guild.stash(id)
        if (heroes.onScreen(id)) guild { it.copy(stash = stash) }
    }

    /**
     * Пачка во вкладку [tab]: сервер кладёт по порядку, пока есть места. Тост - сколько легло, а легло не всё - из скольких и
     * почему («Положено 7 из 9: нет места»).
     */
    fun deposit(tab: Int, parcels: List<GuildDepositParcel>) = command { id ->
        val done = api.guild.deposit(id, tab, parcels)
        guild { it.copy(stash = done.stash) }
        heroes.money(done.stash.money)
        if (done.refused.isEmpty()) {
            ui("guild.toast.deposited_n", done.placed)
        } else {
            ui("guild.toast.deposited_part", done.placed, parcels.size, done.refused.keys.sortedBy { it.ordinal }.joinToString(", ") { ui("guild.deposit_bar.${it.name}") })
        }
    }

    fun take(entryId: String) = stashCommand(ui("guild.toast.taken")) { api.guild.take(it, entryId) }
    fun tabRank(tab: Int, minRank: Int) = stashCommand(ui("guild.toast.saved")) { api.guild.tabRank(it, tab, minRank) }

    /** Имя вкладки [tab]; пустое возвращает её номер. Длину и право проверяет сервер. */
    fun tabName(tab: Int, name: String) = stashCommand(ui("guild.toast.saved")) { api.guild.tabName(it, tab, name.trim()) }

    /** Ответ хранилища несёт золото героя после записи: взятие за золото видно в кошельке сразу. */
    private fun stashCommand(done: String, block: suspend (String) -> GuildStashView) = command(done) { id ->
        val stash = block(id)
        guild { it.copy(stash = stash) }
        heroes.money(stash.money)
    }

    /**
     * Золото в казну: ответ несёт гильдию, строку героя и остаток золота, так что перечитывается один герой.
     * [onDone] - только после принятого вклада (экран чистит поле суммы); занятый раннер говорит об этом игроку.
     */
    fun contribute(amount: Long, onDone: () -> Unit) {
        val started = commands.task(writing = true, touches = setOf(Reads.GUILD, Reads.HERO)) {
            val id = hero()
            val given = api.guild.contribute(id, amount)
            if (!heroes.onScreen(id)) return@task
            guild { it.copy(mine = it.mine?.copy(guild = given.guild, me = given.me)) }
            heroes.money(given.money)
            notices.toast(ui("guild.toast.contributed"))
            onDone()
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    /** Журнал с новейшей страницы или следующая за показанными; конец - когда сервер говорит, что старше нет. */
    fun loadLog(more: Boolean = false) = commands.read(Reads.GUILD_LOG, restart = !more) {
        val id = hero()
        val shown = guilds.log
        if (more && !shown.more) return@read
        val read = api.guild.log(id, if (more) shown.page + 1 else 0)
        if (heroes.onScreen(id)) guild { if (more && it.log.page != shown.page) it else it.copy(log = if (more) Page(it.log.items + read.items, read.page, read.total, read.more) else read) }
    }

    private fun inviteName(guildId: String): String = guilds.mine?.invites?.firstOrNull { it.guild.id == guildId }?.guild?.name.orEmpty()

    private fun hero(): String = heroes.heroId.also { check(it.isNotBlank()) { ui("auction.choose_character") } }

    /**
     * Команда, что вводит героя в гильдию или выводит из неё - или отвечает на приглашение - и отвечает всей [GuildMine].
     * Герой едет снимком в том же ответе: перечитывать нечего.
     */
    private fun moving(done: String, block: suspend (String) -> GuildMine) = command(done) { id ->
        val mine = block(id)
        guild { it.copy(mine = mine, tab = if (mine.guild == null) null else it.tab) }
    }

    /** Команда внутри гильдии, отвеченная гильдией, как она теперь стоит. */
    private fun inside(done: String, block: suspend (String) -> GuildView) = command(done) { id ->
        val view = block(id)
        guild { it.copy(mine = it.mine?.let { mine -> mine.copy(guild = view, me = view.member(id) ?: mine.me) }) }
    }

    /** Одна команда гильдии: по одной за раз - героя под ней не сменить - и без повторов; тост [done] - после ответа. */
    private fun command(done: String, block: suspend (String) -> Unit): Boolean = command { id ->
        block(id)
        done
    }

    /** Команда гильдии, чей тост складывает сам ответ: [block] возвращает его текст. */
    private fun command(block: suspend (String) -> String): Boolean = commands.task(writing = true, touches = setOf(Reads.GUILD, Reads.HERO)) {
        notices.toast(block(hero()))
    }
}
