package com.sperance.exileforge.presentation.admin

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.i18n.phrase
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.AccountRole
import com.sperance.exileforge.core.network.DeletionRequest
import com.sperance.exileforge.core.network.Dossier
import com.sperance.exileforge.core.network.GameServer
import com.sperance.exileforge.core.network.ModerationEntryView
import com.sperance.exileforge.core.network.ModerationPage
import com.sperance.exileforge.core.network.ModerationSegment
import com.sperance.exileforge.core.network.SanctionKind
import com.sperance.exileforge.core.network.SanctionRequest
import com.sperance.exileforge.core.network.SanctionView
import com.sperance.exileforge.core.network.TesterAccount
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Раздел окна модерации: три списка сервера и журнал действий. */
enum class ModerationTab { ALL, BANNED, TRASH, JOURNAL }

/**
 * Окно модерации (3.88.5, вариант B «Досье-карточки»): раздел, поиск, страница строк или журнала; открытое досье - поверх
 * списка, [dossierKey] - чьё оно (герой и аккаунт), чтобы после действия перечитать именно его.
 */
data class ModerationState(
    val tab: ModerationTab = ModerationTab.ALL,
    val query: String = "",
    val page: ModerationPage = ModerationPage(),
    val journal: List<ModerationEntryView> = emptyList(),
    val journalPage: Int = 0,
    val dossier: Dossier? = null,
    val dossierKey: Pair<String, String>? = null,
    /** Только что созданный тестировщик (3.88.7): логин и пароль показываются один раз. */
    val createdTester: TesterAccount? = null,
    /** Игровые серверы (3.91.0): администратор переносит между ними героя из досье. */
    val servers: List<GameServer> = emptyList(),
)

/**
 * Модерация глазами модератора и администратора (3.88.5, server 1.80.8): списки героев, баны, корзина и журнал, досье игрока,
 * бан героя, аккаунта или устройства, его снятие, удаление в корзину и восстановление. Права решает сервер; клиент только
 * прячет то, что досье ([Dossier.rights]) не разрешает.
 */
class ModerationViewModel(
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val sessions: SessionRepository,
    private val notices: Notices,
) : ViewModel() {
    private val mutable = MutableStateFlow(ModerationState())
    val state: StateFlow<ModerationState> = mutable.asStateFlow()
    val activity = commands.state

    private val api get() = connection.api.moderation

    fun search(query: String) = mutable.update { it.copy(query = query) }

    fun tab(tab: ModerationTab) {
        mutable.update { it.copy(tab = tab) }
        if (tab == ModerationTab.JOURNAL) journal(0) else load(0)
    }

    /** Страница [page] раздела под текущим поиском. */
    fun load(page: Int = mutable.value.page.page) = commands.read(Reads.MODERATION, restart = true) {
        staff()
        val state = mutable.value
        val segment = when (state.tab) {
            ModerationTab.BANNED -> ModerationSegment.BANNED
            ModerationTab.TRASH -> ModerationSegment.TRASH
            else -> ModerationSegment.ALL
        }
        val answer = api.rows(state.query, segment, page.coerceAtLeast(0))
        mutable.update { it.copy(page = answer) }
    }

    fun journal(page: Int) = commands.read(Reads.MODERATION, restart = true) {
        staff()
        val entries = api.log(page.coerceAtLeast(0))
        mutable.update { it.copy(journal = entries, journalPage = page.coerceAtLeast(0)) }
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

    fun ban(request: SanctionRequest) = act { api.ban(request).also { notices.toast(ui("moderation.banned", it.label)) } }

    /** Снимает бан или восстанавливает удалённое из корзины. */
    fun lift(sanction: SanctionView) = act {
        api.lift(sanction.id).also { notices.toast(ui(if (it.kind == SanctionKind.DELETION) "moderation.restored" else "moderation.unbanned", it.label)) }
    }

    /** Новая роль аккаунта [userId] (3.88.7): администратор выдаёт игрока, тестировщика или модератора. */
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

    /** Новый аккаунт [login] с ролью [role] (3.88.8): пароль сервер придумывает сам и отдаёт один раз. */
    fun createAccount(login: String, role: AccountRole) {
        val started = commands.task(writing = true) {
            staff()
            val account = connection.api.admin.createTester(login.trim(), role)
            mutable.update { it.copy(createdTester = account) }
            load()
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    /** Список серверов (3.91.0) - только администратору: перенос и новый сервер - его команды. */
    fun loadServers() = commands.read(Reads.SERVERS) {
        if (!sessions.state.value.isAdmin) return@read
        val servers = connection.api.admin.servers()
        mutable.update { it.copy(servers = servers) }
    }

    /** Новый игровой сервер (3.91.0). */
    fun createServer(code: String, name: String) {
        val started = commands.task(writing = true) {
            staff()
            val server = connection.api.admin.createServer(code, name)
            notices.toast(ui("admin.server_created", server.name))
            mutable.update { it.copy(servers = it.servers + server) }
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    /** Переносит героя [heroId] на сервер [server] (3.91.0); тост называет, вышел ли он из гильдии и сколько лотов вернулось. */
    fun moveHero(heroId: String, server: String) {
        val started = commands.task(writing = true) {
            staff()
            val move = connection.api.admin.moveHero(heroId, server)
            notices.toast(ui("admin.hero_moved", move.name, serverName(move.server), move.lotsReturned) + if (move.leftGuild) " " + ui("admin.left_guild") else "")
            reloadDossier()
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    /** Название сервера по коду; неизвестный - самим кодом. */
    fun serverName(code: String): String = mutable.value.servers.firstOrNull { it.code == code }?.name?.ifBlank { null } ?: code

    fun forgetTester() = mutable.update { it.copy(createdTester = null) }

    fun delete(request: DeletionRequest) = act { api.delete(request).also { notices.toast(ui("moderation.deleted", it.label)) } }

    /**
     * Действие модерации: уходит сразу или падает видимой ошибкой (команда администратора не ждёт сети, как в 3.88.0); после
     * него досье и раздел перечитываются - сервер отвечает санкцией, а не готовой строкой списка.
     */
    private fun act(block: suspend () -> SanctionView) {
        val started = commands.task(writing = true) {
            staff()
            block()
            if (mutable.value.dossierKey != null) reloadDossier()
            if (mutable.value.tab == ModerationTab.JOURNAL) journal(mutable.value.journalPage) else load()
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    private fun staff() = check(sessions.state.value.isModerator) { ui("moderation.staff_only") }
}
