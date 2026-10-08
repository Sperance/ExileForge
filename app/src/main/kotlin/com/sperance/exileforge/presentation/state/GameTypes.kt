package com.sperance.exileforge.presentation.state

/* Типы игры для экранов (3.80.41): режим, фаза, вкладки, здания, разделы кузницы и порядок сундука. Общее состояние удалено - экраны читают срезы. */

enum class AppMode { PLAYER, ADMIN }

typealias NoticeKind = com.sperance.exileforge.core.session.NoticeKind

/** Строка на языке игрока в момент показа - из :core (3.80.8). */
typealias Phrase = com.sperance.exileforge.core.i18n.Phrase

fun phrase(key: String, vararg args: Any?): Phrase = com.sperance.exileforge.core.i18n.phrase(key, *args)

typealias Notice = com.sperance.exileforge.core.session.Notice

/** Which of the three screens the app is on, above the tabs: the tabs only make sense once there is an account and a hero. */
enum class AppPhase { AUTH, CHARACTERS, GAME }

/** The stash's orders (3.30.0): the newest first as the server keeps it, or by rarity, item level or the merchant's price. */
enum class StashSort {
    NEWEST,
    RARITY,
    LEVEL,
    PRICE,
    ;

    companion object {
        fun of(name: String?): StashSort = entries.firstOrNull { it.name == name } ?: NEWEST
    }
}

/** Вещь, принесённая походом, - из :core (3.80.20). */
typealias LootEntry = com.sperance.exileforge.core.campaign.LootEntry

/** The forge's sections: orbs, the bench and the essences work on one item. */
enum class ForgeSection { ORBS, BENCH, ESSENCES }

/**
 * The City's buildings (3.22.0): each one a screen of its own behind the square; «История» (3.90.2) - прошлое героя,
 * «Летопись» (4.0.0, прежде страница «Развития») - его деяния и титулы, «Доска славы» (4.2.0) - таблицы сервера.
 */
enum class Building { QUESTS, MERCHANT, AUCTION, GUILD, HISTORY, CHRONICLE, HALL }

/** How many heroes one account may hold when the rules have not been read yet. */
const val MAX_CHARACTERS = 3

/** The tabs, by name: the bottom bar a player sees, and the screens a button opens. */
const val TAB_ACCOUNT = 3
const val TAB_HERO = 4
const val TAB_TREE = 5

/** The City (3.22.0): the merchant, the auction and the guild, where the auction's tab was. */
const val TAB_CITY = 6
const val TAB_CRAFT = 7
const val TAB_ADMIN = 8
const val TAB_REDEMPTION = 9
const val TAB_EXPEDITION = 10
const val TAB_CRAFTS = 11
const val TAB_SKILLS = 12

/** «Развитие»: кузня ([TAB_CRAFT]), зверинец, дерево и гримуар; испытания и атлас - страницы «Похода» (4.0.0). */
const val TAB_PROGRESS = 13
const val TAB_PETS = 14
const val TAB_TRIALS = 15

/** «Настройки» (3.77.0): from the banner's menu, «back» leading to the tab it was opened over. */
const val TAB_SETTINGS = 17

// Порядок нижних кнопок (3.95.3): Герой, Развитие, Поход, Город, Ремёсла
val PLAYER_TABS = listOf(TAB_HERO, TAB_PROGRESS, TAB_EXPEDITION, TAB_CITY, TAB_CRAFTS)

/** Screens only an administrator may open, whichever button leads to them. */
val ADMIN_TABS = setOf(TAB_ADMIN, TAB_REDEMPTION)
