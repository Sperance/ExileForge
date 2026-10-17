package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.text.LocaleKey

/**
 * Слово мира: глоссарий лора в кодексе окна «Сюжет» - печать, трон, наследник, Изнанка, Пустота и прочее, на
 * что опираются имена вещей и механик. В отличие от [Term] (правило механики под строкой) это не правило, а история; порядок
 * перечня - порядок в кодексе.
 */
enum class LoreTerm {
    SEAL,
    THRONE,
    KEEPER,
    HEIR,
    BRAND,
    GREY_CLOAKS,
    LAW,
    LINE,
    ARCHITECT,
    VOID,
    ABYSS,
    UNDERSIDE,
    RIFT,
    ECHO,
    BLIGHT,
    ATLAS,
    ESSENCE,
    EMPIRE,
    MYTHIC,
    ;

    val title: String get() = ui("term.lore.$name")
    val text: String get() = ui("term.lore.$name.text")
}

/** Строка кодекса стража: только у хранителей тронов и стражей механик; у прочих - null, а не сырой ключ. */
fun monsterLore(code: String): String? = serverLocale.string(LocaleKey.monsterLore(code))

/** Кодекс трона региона: имя трона, хранитель, законы и фраза истории; без словаря - null. */
fun regionLore(region: String): String? = serverLocale.string(LocaleKey.regionLore(region))
