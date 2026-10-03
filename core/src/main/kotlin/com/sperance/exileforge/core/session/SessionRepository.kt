package com.sperance.exileforge.core.session

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.hero.HeroSummary
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
    /** Ответ сервера на проверку здоровья, как есть. */
    val health: String = ui("runtime.not_checked"),
) {
    val isAdmin: Boolean get() = signedIn && profile?.role == "ADMIN"

    /** Окно тестирования: тестировщик и администратор. */
    val isTester: Boolean get() = isAdmin || (signedIn && profile?.role == "TESTER")

    /** Аккаунт без имени: регистрация по устройству оставляет имя и логин пустыми. */
    val title: String get() = profile?.name?.takeIf { it.isNotBlank() }
        ?: profile?.login?.takeIf { it.isNotBlank() }
        ?: ui("session.guest") + " · …${profile?.id.orEmpty().takeLast(6)}"
}

/** Единственный источник правды о сессии (3.80.7): модели экранов читают поток, команды пишут. */
class SessionRepository(initialServer: String) {
    private val mutable = MutableStateFlow(Session(server = initialServer))
    val state: StateFlow<Session> = mutable

    fun update(transform: (Session) -> Session) = mutable.update(transform)

    /** Выход или смена сервера: всё, что принадлежало аккаунту, забывается, эпоха растёт. */
    fun clear() = update { it.copy(resumable = false, characters = emptyList(), charactersRead = false, signedIn = false, profile = null, sessionEpoch = it.sessionEpoch + 1) }
}
