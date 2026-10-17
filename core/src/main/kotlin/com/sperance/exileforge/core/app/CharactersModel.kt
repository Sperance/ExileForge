package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.model.hero.DeletionPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Меню героев на экране: последствия удаления открытого листа и класс черновика нового героя; команды - у [CharacterActions]. */
class CharactersModel(private val characters: CharacterActions) {
    private val mutablePreview = MutableStateFlow<DeletionPreview?>(null)

    /** Последствия удаления героя, чей лист подтверждения открыт; null - читаются или лист закрыт. */
    val deletionPreview: StateFlow<DeletionPreview?> = mutablePreview.asStateFlow()

    private var previewing: String? = null

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

    private val mutableDraft = MutableStateFlow("")

    /** Класс, с которым создаётся новый герой (черновик экрана, не общего состояния); пусто - первый из контента. */
    val draftClass: StateFlow<String> = mutableDraft.asStateFlow()

    fun draftClass(value: String) {
        mutableDraft.value = value
    }
}
