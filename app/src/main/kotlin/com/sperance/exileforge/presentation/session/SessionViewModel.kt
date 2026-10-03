package com.sperance.exileforge.presentation.session

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.Session
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AppMode
import kotlinx.coroutines.flow.StateFlow

/**
 * Экран входа (3.80.22): сервер, вход по логину или по устройству, повтор подтверждения. Сама сессия пока в
 * `ForgeRuntime` - её логика уедет вместе с навигацией (этап 3), экран уже не получает общую модель.
 */
class SessionViewModel(private val runtime: ForgeRuntime) : ViewModel() {
    val session: StateFlow<Session> = runtime.sessions.state
    val activity: StateFlow<Activity> = runtime.commands.state

    fun login(login: String, password: String) = runtime.sessionViewModel.login(login, password)
    fun playOnThisDevice() = runtime.sessionViewModel.playOnThisDevice()
    fun retryResume() = runtime.sessionViewModel.retryResume()
    fun resetServer() = runtime.sessionViewModel.resetServer()
    fun logout() = runtime.sessionViewModel.logout()
    fun language(lang: Lang) = runtime.language(lang)
    fun dismissMessage() = runtime.commands.dismissMessage()
    fun dismissNotice() = runtime.notices.dismiss()

    fun changePassword(current: String, replacement: String) = runtime.sessionViewModel.changePassword(current, replacement)
    fun serverDraft(value: String) = runtime.sessionViewModel.serverDraft(value)
    fun mode(mode: AppMode) = runtime.sessionViewModel.mode(mode)

    /** Тестовые учётки администратора (3.80.30), как их просят страницы настроек. */
    fun loadTesters() = runtime.sessionViewModel.loadTesters()
    fun createTester(login: String) = runtime.sessionViewModel.createTester(login)
    fun resetTester(id: String) = runtime.sessionViewModel.resetTester(id)
    fun setTesterActive(id: String, active: Boolean) = runtime.sessionViewModel.setTesterActive(id, active)
    fun refreshLocale() = runtime.refreshLocale()
    fun refreshIcons() = runtime.refreshIcons()

    fun connect() = runtime.sessionViewModel.connect()
    fun health() = runtime.sessionViewModel.health()
    fun leaveGame() = runtime.characterViewModel.leaveGame()
    fun closeShownTester() = runtime.sessionViewModel.closeShownTester()
}
