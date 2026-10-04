package com.sperance.exileforge.presentation.session

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Session
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.presentation.app.CharacterActions
import com.sperance.exileforge.presentation.app.SessionActions
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.world.WorldLoader
import kotlinx.coroutines.flow.StateFlow

/**
 * Экран входа (3.80.22): сервер, вход по логину или по устройству, повтор подтверждения; сессия - у `SessionActions`,
 * словарь и иконки - у `WorldLoader` (3.80.44).
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
    /** Срез «игра» для экранов этой модели (3.80.33). */
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
    fun mode(mode: AppMode) = sessionActions.mode(mode)

    /** Тестовые учётки администратора (3.80.30), как их просят страницы настроек. */
    fun loadTesters() = sessionActions.loadTesters()
    fun createTester(login: String) = sessionActions.createTester(login)
    fun resetTester(id: String) = sessionActions.resetTester(id)
    fun setTesterActive(id: String, active: Boolean) = sessionActions.setTesterActive(id, active)
    fun refreshLocale() = loader.refreshLocale()
    fun refreshIcons() = loader.refreshIcons()

    fun connect(draft: String) = sessionActions.connect(draft)
    fun health() = sessionActions.health()
    fun leaveGame() = characters.leaveGame()
    fun closeShownTester() = sessionActions.closeShownTester()
}
