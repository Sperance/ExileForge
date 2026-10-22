package com.sperance.exileforge.core.admin

import com.sperance.exileforge.core.display.PickOption
import com.sperance.exileforge.core.filter.Narrowing
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.phrase
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.core.model.paging.Page
import com.sperance.exileforge.core.network.AccountRole
import com.sperance.exileforge.core.network.DeletionRequest
import com.sperance.exileforge.core.network.Dossier
import com.sperance.exileforge.core.network.GameServer
import com.sperance.exileforge.core.network.ModerationEntryView
import com.sperance.exileforge.core.network.ModerationFate
import com.sperance.exileforge.core.network.ModerationRow
import com.sperance.exileforge.core.network.ModerationSegment
import com.sperance.exileforge.core.network.ModerationSort
import com.sperance.exileforge.core.network.SanctionRequest
import com.sperance.exileforge.core.network.SanctionView
import com.sperance.exileforge.core.network.TesterAccount
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.rules.content.ContentIndex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Раздел окна модерации: три списка сервера и журнал действий. */
enum class ModerationTab { ALL, BANNED, TRASH, JOURNAL }

/**
 * Порядок и отбор списка модерации: [sort] - порядок, [fate] - дар. Новый отбор - поле здесь и строка в [narrowings].
 */
data class ModerationFilters(val sort: ModerationSort = ModerationSort.ACTIVITY, val fate: ModerationFate = ModerationFate.All) {
    /**
     * Не обычные порядок и дар сужениями под строкой поиска, каждое - с отбором без него, подписи - на языке [lang]; их число - на
     * кнопке фильтров. Дар называется по контенту [index], без контента - кодом.
     */
    fun narrowings(lang: Lang, index: ContentIndex?): List<Narrowing<ModerationFilters>> = buildList {
        if (sort != DEFAULT.sort) add(Narrowing(ui(lang, "moderation.sort.${sort.name}"), copy(sort = DEFAULT.sort)))
        val fateTitle = when (val chosen = fate) {
            ModerationFate.All -> null
            ModerationFate.Fateless -> ui(lang, "moderation.fate.none")
            is ModerationFate.Chosen -> FATE_MARK + (index?.fates?.all?.firstOrNull { it.code == chosen.code }?.let(FateCard::of)?.title ?: chosen.code)
        }
        fateTitle?.let { add(Narrowing(it, copy(fate = DEFAULT.fate))) }
    }

    companion object {
        val DEFAULT = ModerationFilters()

        /** Знак дара контента перед его именем. */
        private const val FATE_MARK = "✦ "

        /** Варианты дара, что не ищутся, а стоят первыми всегда: «Все дары» и «Без дара». */
        fun fixedFates(lang: Lang): List<PickOption<ModerationFate>> = listOf(
            PickOption(ModerationFate.All, ui(lang, "moderation.fate.all")),
            PickOption(ModerationFate.Fateless, ui(lang, "moderation.fate.none")),
        )

        /** Дары контента [index] по имени - варианты дара с поиском; контента нет - пусто. */
        fun fates(index: ContentIndex?): List<PickOption<ModerationFate>> = index?.fates?.all.orEmpty().map(FateCard::of).sortedBy { it.title }
            .map { PickOption(ModerationFate.Chosen(it.code), FATE_MARK + it.title) }
    }
}

/**
 * Окно модерации: раздел, поиск, прочитанные страницы строк ([page]) или журнала ([journal]) - строки всех страниц подряд; открытое
 * досье - поверх списка, [dossierKey] - чьё оно (герой и аккаунт), чтобы после действия перечитать именно его.
 */
data class ModerationState(
    val tab: ModerationTab = ModerationTab.ALL,
    val query: String = "",
    /** Поиск, с которым прочитан показанный список: набранный [query] уходит в список только поиском с клавиатуры. */
    val searched: String = "",
    /** Порядок списка и отбор по Предначертанию. */
    val filters: ModerationFilters = ModerationFilters.DEFAULT,
    val page: Page<ModerationRow> = Page(),
    val journal: Page<ModerationEntryView> = Page(),
    val dossier: Dossier? = null,
    val dossierKey: Pair<String, String>? = null,
    /** Только что созданный тестировщик: логин и пароль показываются один раз. */
    val createdTester: TesterAccount? = null,
    /** Игровые серверы: администратор переносит между ними героя из досье. */
    val servers: List<GameServer> = emptyList(),
) {
    /** Список сервера раздела: корзина и баны - свои, журнал и «Все» - все герои. */
    val segment: ModerationSegment
        get() = when (tab) {
            ModerationTab.BANNED -> ModerationSegment.BANNED
            ModerationTab.TRASH -> ModerationSegment.TRASH
            else -> ModerationSegment.ALL
        }

    /** Тот же ли список у окна [other]: раздел и отбор - ответ на прежний список в новый не ложится. */
    fun sameList(other: ModerationState): Boolean = tab == other.tab && filters == other.filters
}

/**
 * Модерация глазами модератора и администратора: списки героев, баны, корзина и журнал, досье игрока,
 * бан героя, аккаунта или устройства, его снятие, удаление в корзину и восстановление. Права решает сервер; клиент только
 * прячет то, что досье ([Dossier.rights]) не разрешает.
 */
class ModerationModel(
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val sessions: SessionRepository,
    private val notices: Notices,
) {
    private val mutable = MutableStateFlow(ModerationState())
    val state: StateFlow<ModerationState> = mutable.asStateFlow()
    val activity = commands.state

    private val api get() = connection.api.moderation

    fun search(query: String) = mutable.update { it.copy(query = query) }

    /** Новые порядок и отбор списка (лист «Фильтры»): с первой страницы, одним запросом. */
    fun filter(filters: ModerationFilters) {
        mutable.update { it.copy(filters = filters) }
        load()
    }

    fun tab(tab: ModerationTab) {
        mutable.update { it.copy(tab = tab) }
        if (tab == ModerationTab.JOURNAL) journal() else load()
    }

    /** Первая страница раздела под текущим поиском и отбором; новый вызов сменяет идущий. */
    fun load() = commands.read(Reads.MODERATION, restart = true) {
        staff()
        val state = mutable.value
        val answer = api.rows(state.query, state.segment, 0, state.filters.sort, state.filters.fate)
        mutable.update { if (it.sameList(state)) it.copy(page = answer, searched = state.query) else it }
    }

    /**
     * Следующая страница раздела под прочитанными (листание до края списка): чтение не перезапускается, страница ложится, только пока
     * раздел, поиск, отбор и прочитанные страницы те же.
     */
    fun more() = commands.read(Reads.MODERATION) {
        val state = mutable.value
        if (!state.page.more) return@read
        val next = api.rows(state.searched, state.segment, state.page.page + 1, state.filters.sort, state.filters.fate)
        mutable.update { if (it.sameList(state) && it.searched == state.searched && it.page.page == state.page.page) it.copy(page = it.page.followedBy(next) { row -> row.key }) else it }
    }

    /** Первая страница журнала модерации; новый вызов сменяет идущий. */
    fun journal() = commands.read(Reads.MODERATION, restart = true) {
        staff()
        val read = api.log(0)
        mutable.update { it.copy(journal = read) }
    }

    /** Следующая страница журнала под прочитанными (листание до края): ложится, только пока прочитанные страницы те же. */
    fun moreJournal() = commands.read(Reads.MODERATION) {
        val shown = mutable.value.journal
        if (!shown.more) return@read
        val read = api.log(shown.page + 1)
        mutable.update { if (it.journal.page == shown.page) it.copy(journal = Page(it.journal.items + read.items, read.page, read.total, read.more)) else it }
    }

    /** Досье героя [heroId] или, без героя, аккаунта [userId]. */
    fun open(heroId: String, userId: String) {
        mutable.update { it.copy(dossierKey = heroId to userId, dossier = null) }
        reloadDossier()
    }

    fun close() = mutable.update { it.copy(dossier = null, dossierKey = null) }

    private fun reloadDossier() = commands.read(Reads.DOSSIER, restart = true) {
        staff()
        val (hero, user) = mutable.value.dossierKey ?: return@read
        val dossier = api.dossier(hero, user)
        mutable.update { if (it.dossierKey == hero to user) it.copy(dossier = dossier) else it }
    }

    /** Санкция на срок - бан или немота: подпись по виду санкции. */
    fun ban(request: SanctionRequest) = act { api.ban(request).also { notices.toast(ui("moderation.imposed.${it.kind.name}", it.label)) } }

    /** Снимает бан или немоту, восстанавливает удалённое из корзины. */
    fun lift(sanction: SanctionView) = act {
        api.lift(sanction.id).also { notices.toast(ui("moderation.lifted.${it.kind.name}", it.label)) }
    }

    /** Новая роль аккаунта [userId]: администратор выдаёт игрока, тестировщика или модератора. */
    fun setRole(userId: String, role: AccountRole) {
        val started = commands.task(writing = true) {
            staff()
            api.setRole(userId, role)
            notices.toast(ui("moderation.role_set", ui("moderation.role.${role.name}")))
            if (mutable.value.dossierKey != null) reloadDossier()
            load()
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    /** Новый аккаунт [login] с ролью [role]: пароль сервер придумывает сам и отдаёт один раз. */
    fun createAccount(login: String, role: AccountRole) {
        val started = commands.task(writing = true) {
            staff()
            val account = connection.api.admin.createTester(login.trim(), role)
            mutable.update { it.copy(createdTester = account) }
            load()
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    /** Список серверов - только администратору: перенос и новый сервер - его команды. */
    fun loadServers() = commands.read(Reads.SERVERS) {
        if (!sessions.state.value.isAdmin) return@read
        val servers = connection.api.admin.servers()
        mutable.update { it.copy(servers = servers) }
    }

    /** Новый игровой сервер. */
    fun createServer(code: String, name: String) {
        val started = commands.task(writing = true) {
            staff()
            val server = connection.api.admin.createServer(code, name)
            notices.toast(ui("admin.server_created", server.name))
            mutable.update { it.copy(servers = it.servers + server) }
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    /**
     * Отметка тестировщика героя [heroId]: только администратор, в досье героя.
     *
     * Переносит героя на другой сервер - тост называет, вышел ли он из гильдии и сколько лотов вернулось.
     */
    fun setTester(heroId: String, tester: Boolean) {
        val started = commands.task(writing = true) {
            staff()
            val now = api.setTester(heroId, tester)
            notices.toast(ui(if (now) "moderation.tester_on" else "moderation.tester_off"))
            reloadDossier()
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    fun moveHero(heroId: String, server: String) {
        val started = commands.task(writing = true) {
            staff()
            val move = connection.api.admin.moveHero(heroId, server)
            notices.toast(ui("admin.hero_moved", move.name, serverName(move.server), move.lotsReturned) + if (move.leftGuild) " " + ui("admin.left_guild") else "")
            reloadDossier()
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    /** Свой ли аккаунт [userId] у смотрящего: своё досье - себе, без причины и письма. */
    fun owns(userId: String): Boolean = userId == sessions.state.value.profile?.id

    /**
     * Предначертание [code] аккаунту [userId] досье: только администратор (право досье `rights.fate`). Себе - без причины, дар
     * ложится в профиль сессии, герои и список героев перечитываются при следующем чтении; чужому - с причиной [reason] для
     * игрока, он получит письмо. Досье перечитывается.
     */
    fun redrawFate(code: String, userId: String, reason: String) {
        val own = owns(userId)
        val started = commands.task(writing = true, touches = if (own) setOf(Reads.HERO, Reads.CHARACTERS) else emptySet()) {
            check(sessions.state.value.isAdmin) { ui("moderation.staff_only") }
            val view = connection.api.admin.redrawFate(code, userId, reason)
            if (own) sessions.update { it.copy(fate = view, profile = it.profile?.let { p -> p.copy(fate = view.chosen?.code ?: code) }) }
            notices.toast(ui("moderation.fate.changed", view.chosen?.title ?: FateCard(code).title))
            reloadDossier()
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    /** Название сервера по коду; неизвестный - самим кодом. */
    fun serverName(code: String): String = mutable.value.servers.firstOrNull { it.code == code }?.name?.ifBlank { null } ?: code

    fun forgetTester() = mutable.update { it.copy(createdTester = null) }

    fun delete(request: DeletionRequest) = act { api.delete(request).also { notices.toast(ui("moderation.deleted", it.label)) } }

    /**
     * Действие модерации: уходит сразу или падает видимой ошибкой (команда администратора не ждёт сети); после
     * него досье и раздел перечитываются - сервер отвечает санкцией, а не готовой строкой списка.
     */
    private fun act(block: suspend () -> SanctionView) {
        val started = commands.task(writing = true) {
            staff()
            block()
            if (mutable.value.dossierKey != null) reloadDossier()
            if (mutable.value.tab == ModerationTab.JOURNAL) journal() else load()
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    private fun staff() = check(sessions.state.value.isModerator) { ui("moderation.staff_only") }
}
