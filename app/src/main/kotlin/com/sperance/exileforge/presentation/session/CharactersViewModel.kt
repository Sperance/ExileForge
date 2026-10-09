package com.sperance.exileforge.presentation.session

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.model.hero.DeletionPreview
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Session
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.presentation.app.CharacterActions
import com.sperance.exileforge.presentation.app.SessionActions
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.world.WorldLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Меню героев (3.80.22): список, вход, создание и удаление; логика - у `CharacterActions` (3.80.44). */
class CharactersViewModel(
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

    fun enterCharacter(id: String) = characters.enter(id)
    fun refreshCharacters() = characters.refresh()
    fun createCharacter(name: String, heroClass: String) = characters.create(name, heroClass)
    fun markDeletion(id: String, name: String) = characters.markDeletion(id, name)
    fun restoreCharacter(id: String) = characters.restore(id)
    fun eraseCharacter(id: String, name: String) = characters.erase(id, name)

    private val mutablePreview = MutableStateFlow<DeletionPreview?>(null)

    /** Последствия удаления героя, чей лист подтверждения открыт (4.5.1); null - читаются или лист закрыт. */
    val deletionPreview: StateFlow<DeletionPreview?> = mutablePreview.asStateFlow()

    /** Лист удаления героя [id] открылся: прежний ответ про другого героя не показывается. */
    fun previewDeletion(id: String) {
        previewing = id
        mutablePreview.value = null
        characters.deletionPreview(id) { preview -> if (previewing == id) mutablePreview.value = preview }
    }

    fun closeDeletion() {
        previewing = null
        mutablePreview.value = null
    }

    private var previewing: String? = null
    fun ensureClasses() = characters.ensureClasses()

    private val mutableDraft = MutableStateFlow("")

    /** Класс, с которым создаётся новый герой (3.80.32: черновик экрана, не общего состояния); пусто - первый из контента. */
    val draftClass: StateFlow<String> = mutableDraft.asStateFlow()

    fun draftClass(value: String) {
        mutableDraft.value = value
    }

    fun logout() = sessionActions.logout()
    fun language(lang: Lang) = loader.language(lang)
    fun dismissMessage() = commands.dismissMessage()
    fun dismissNotice() = notices.dismiss()
}
