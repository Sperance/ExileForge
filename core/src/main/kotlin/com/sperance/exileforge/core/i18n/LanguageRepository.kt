package com.sperance.exileforge.core.i18n

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Язык интерфейса (3.80.32): единственный источник для экранов. Глобальный [uiLanguage], что читают строки
 * ядра, меняется в том же вызове и раньше потока - экран, перерисованный по новому языку, уже видит его строки.
 */
class LanguageRepository {
    private val mutable = MutableStateFlow(uiLanguage)
    val lang: StateFlow<Lang> = mutable.asStateFlow()

    fun set(lang: Lang) {
        uiLanguage = lang
        mutable.value = lang
    }
}
