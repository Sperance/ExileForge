package com.sperance.exileforge.ui.screens.session

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.app.CharacterActions
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.SessionActions
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Session
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.world.WorldLoader
import kotlinx.coroutines.flow.StateFlow

/**
 * Экран входа: сервер, вход по логину или по устройству, повтор подтверждения; сессия - у `SessionActions`,
 * словарь и иконки - у `WorldLoader`.
 */
class SessionViewModel(
    slice: GameSlice,
    sessions: SessionRepository,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val loader: WorldLoader,
    private val sessionActions: SessionActions,
    private val characters: CharacterActions,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели. */
    val game: StateFlow<GameUi> = slice.ui
    val session: StateFlow<Session> = sessions.state
    val activity: StateFlow<Activity> = commands.state

    fun login(login: String, password: String) = sessionActions.login(login, password)
    fun playOnThisDevice() = sessionActions.playOnThisDevice()
    fun retryResume() = sessionActions.retryResume()
    fun resetServer() = sessionActions.resetServer()
    fun logout() = sessionActions.logout()
    fun language(lang: Lang) = loader.language(lang)
    fun dismissMessage() = commands.dismissMessage()
    fun dismissNotice() = notices.dismiss()

    fun changePassword(current: String, replacement: String) = sessionActions.changePassword(current, replacement)
    fun bind(login: String, password: String) = sessionActions.bind(login, password)

    /** Тестовые учётки администратора, как их просят страницы настроек. */
    fun refreshLocale() = loader.refreshLocale()
    fun refreshIcons() = loader.refreshIcons()

    fun connect(draft: String) = sessionActions.connect(draft)
    fun health() = sessionActions.health()
    fun leaveGame() = characters.leaveGame()
}
