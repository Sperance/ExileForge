package com.sperance.exileforge.core.model

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage

enum class Catalog(val path: String) {
    ITEMS("items"), EQUIPMENT("equipment"), CHARACTERS("character");
    fun title(lang: Lang = uiLanguage) = ui(lang, "enum.catalog.$name")
}
