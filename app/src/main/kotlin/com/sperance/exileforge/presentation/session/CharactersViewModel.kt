package com.sperance.exileforge.presentation.session

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.Session
import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Меню героев (3.80.22): список, вход, создание и удаление; логика - в `ForgeRuntime` до переезда навигации. */
class CharactersViewModel(private val runtime: ForgeRuntime) : ViewModel() {
    val session: StateFlow<Session> = runtime.sessions.state
    val activity: StateFlow<Activity> = runtime.commands.state

    fun enterCharacter(id: String) = runtime.characterViewModel.enter(id)
    fun refreshCharacters() = runtime.characterViewModel.refresh()
    fun createCharacter(name: String, heroClass: String) = runtime.characterViewModel.create(name, heroClass)
    fun deleteCharacter(id: String) = runtime.characterViewModel.delete(id)
    fun ensureClasses() = runtime.characterViewModel.ensureClasses()

    /** Класс, с которым создаётся новый герой. */
    fun draftClass(value: String) = runtime.mutable.update { it.copy(play = it.play.copy(draftClass = value)) }

    fun logout() = runtime.sessionViewModel.logout()
    fun language(lang: Lang) = runtime.language(lang)
    fun dismissMessage() = runtime.commands.dismissMessage()
    fun dismissNotice() = runtime.notices.dismiss()
}
