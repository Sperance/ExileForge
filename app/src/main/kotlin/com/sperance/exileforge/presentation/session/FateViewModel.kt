package com.sperance.exileforge.presentation.session

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.app.CharacterActions
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.SessionActions
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.world.WorldLoader
import kotlinx.coroutines.flow.StateFlow

/** Выбор Предначертания аккаунта: три дара, выбор навсегда и уход к героям; логика - у `CharacterActions`. */
class FateViewModel(
    slice: GameSlice,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val loader: WorldLoader,
    private val sessionActions: SessionActions,
    private val characters: CharacterActions,
) : ViewModel() {
    val game: StateFlow<GameUi> = slice.ui

    fun choose(code: String) = characters.chooseFate(code)
    fun accepted() = characters.fateAccepted()
    fun refresh() = characters.refreshFate()
    fun logout() = sessionActions.logout()
    fun language(lang: Lang) = loader.language(lang)
    fun dismissMessage() = commands.dismissMessage()
    fun dismissNotice() = notices.dismiss()
}
