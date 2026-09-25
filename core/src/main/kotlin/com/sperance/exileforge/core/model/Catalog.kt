package com.sperance.exileforge.core.model

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage

enum class Catalog(val path: String) {
    ITEMS("items"), EQUIPMENT("equipment"), CHARACTERS("character"),
    /** Every pool of the world (server 0.56.0): a tag of one kind, the codes it holds and their weights. */
    POOLS("pool");
    fun title(lang: Lang = uiLanguage) = ui(lang, "enum.catalog.$name")
}
