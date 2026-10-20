package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.Item

/** Полка сумки: у каждой свой заголовок ([key] - ключ словаря) над своим рядом ячеек; порядок перечисления - порядок полок. */
enum class BagCategory(val key: String) {
    CHESTS("bag.section_chests"),
    ORBS("bag.section_orbs"),

    /** Печати Атласа: валюта только для карт. */
    SEALS("bag.section_seals"),
    ESSENCES("bag.section_essences"),
    BOOKS("bag.section_books"),

    /** Руны умений: вставляются в гнёзда гримуара. */
    RUNES("bag.section_runes"),
    MATERIALS("bag.section_materials"),
    OTHER("bag.section_other"),
    ;

    companion object {
        /** Полка стопки по категории её предмета [item]; предмета нет в контенте или категория не названа - «Прочее». */
        fun of(item: Item?): BagCategory = when (item?.category) {
            Item.CURRENCY, Item.OMEN -> ORBS
            Item.ATLAS_SEAL -> SEALS
            Item.ESSENCE -> ESSENCES
            Item.BOOK -> BOOKS
            Item.RUNE -> RUNES
            Item.MATERIAL -> MATERIALS
            Item.CHEST -> CHESTS
            else -> OTHER
        }
    }
}
