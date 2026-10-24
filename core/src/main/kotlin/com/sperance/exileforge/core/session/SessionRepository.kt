package com.sperance.exileforge.core.session

import com.sperance.exileforge.core.hero.HeroHolding
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.fate.FateView
import com.sperance.exileforge.core.model.hero.HeroInfo
import com.sperance.exileforge.core.model.hero.HeroSummary
import com.sperance.exileforge.core.network.SanctionNotice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Сессия: какой сервер, кто вошёл и какими героями владеет. */
data class Session(
    val server: String,
    val profile: UserProfile? = null,
    val signedIn: Boolean = false,
    /** Сохранённая сессия, которую сервер не удалось подтвердить: экран входа предлагает повторить. */
    val resumable: Boolean = false,
    /** Растёт при каждом выходе: экраны по нему понимают, что данные прежней сессии устарели. */
    val sessionEpoch: Int = 0,
    /** Герои аккаунта и прочитаны ли они: «нет героев» и «ещё не спрашивали» - разные состояния. */
    val characters: List<HeroSummary> = emptyList(),
    val charactersRead: Boolean = false,
    /** Действующие санкции героев аккаунта по id героя: бан или удаление - карточка героя говорит, кто, за что и до когда. */
    val sanctions: Map<String, SanctionNotice> = emptyMap(),
    /** Названия гильдий героев аккаунта: id гильдии - название. */
    val guilds: Map<String, String> = emptyMap(),
    /** Доступ закрыт санкцией: её id для экрана санкции; пустая строка - аккаунт удалён и стирается. */
    val notice: String? = null,
    /** Токен апелляции санкции [notice] из отказа входа: секрет наказанного, с ним экран санкции подаёт апелляцию. */
    val noticeToken: String = "",
    /** Ответ сервера на проверку здоровья, как есть. */
    val health: String = ui("runtime.not_checked"),
    /** Предначертание аккаунта: выбранное или три на выбор; null - ещё не читали. */
    val fate: FateView? = null,
) {
    val isAdmin: Boolean get() = signedIn && profile?.role == "ADMIN"

    /** Окно модерации и отчёты: модератор и администратор. */
    val isModerator: Boolean get() = isAdmin || (signedIn && profile?.role == "MODERATOR")

    /**
     * Тестировщик ли герой [holding]: отметка героя, не роль аккаунта - окно тестирования и выдачи только у героя-тестировщика; живой
     * снимок героя главнее строки списка героев.
     */
    fun tester(holding: HeroHolding): Boolean = hero(holding, { it.tester }, { it.tester })

    /** Ссылка «Issue #N ↗» на GitHub в своих и известных отчётах: герою-тестировщику [holding] и модерации. */
    fun seesIssues(holding: HeroHolding): Boolean = tester(holding) || isModerator

    /**
     * Свободен ли прогресс героя [holding]: тестировщик включил «Свободный прогресс» в окне тестирования - замки уровня и прогресса
     * открыты, как на сервере; выключен - тестировщик идёт по прогрессу, как игрок. Признак готов у сервера (`ProgressGate.free`: без
     * отметки тестировщика его нет), клиент его не пересчитывает.
     */
    fun freeProgress(holding: HeroHolding): Boolean = hero(holding, { it.freeProgress }, { it.freeProgress })

    /** Признак героя [holding]: из живого снимка ([live]), пока он того же героя, иначе из строки списка героев ([listed]); нет героя - нет. */
    private fun hero(holding: HeroHolding, live: (HeroInfo) -> Boolean, listed: (HeroSummary) -> Boolean): Boolean = holding.hero?.takeIf { it.id == holding.heroId }?.info?.let(live) ?: (characters.firstOrNull { it.id == holding.heroId }?.let(listed) == true)

    /** Откроет ли сервер выдачи окна тестирования герою [holding]: администратор - любому, иначе - тестировщику. */
    fun grants(holding: HeroHolding): Boolean = isAdmin || tester(holding)

    /** Герои, что держат место аккаунта: удалённый самим игроком место освободил сразу, удалённый модерацией - нет. */
    val slotHolders: Int get() = characters.count { sanctions[it.id]?.voluntary != true }

    /** Аккаунт без имени: регистрация по устройству оставляет имя и логин пустыми. */
    val title: String get() = profile?.name?.takeIf { it.isNotBlank() }
        ?: profile?.login?.takeIf { it.isNotBlank() }
        ?: ui("session.guest") + " · …${profile?.id.orEmpty().takeLast(6)}"
}

/** Единственный источник правды о сессии: модели экранов читают поток, команды пишут. */
class SessionRepository(initialServer: String) {
    private val mutable = MutableStateFlow(Session(server = initialServer))
    val state: StateFlow<Session> = mutable

    fun update(transform: (Session) -> Session) = mutable.update(transform)

    /** Выход или смена сервера: всё, что принадлежало аккаунту, забывается, эпоха растёт. */
    fun clear() = update { it.copy(resumable = false, characters = emptyList(), charactersRead = false, sanctions = emptyMap(), guilds = emptyMap(), signedIn = false, profile = null, fate = null, sessionEpoch = it.sessionEpoch + 1) }
}
