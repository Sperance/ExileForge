package com.sperance.exileforge.core.model

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.pick
import com.sperance.exileforge.core.i18n.uiLanguage

enum class Catalog(val path: String, private val ru: String, private val en: String) {
    ITEMS("items", "Предметы", "Items"), EQUIPMENT("equipment", "Экипировка", "Equipment"), CHARACTERS("character", "Персонажи", "Characters");
    fun title(lang: Lang = uiLanguage) = lang.pick(ru, en)
}
