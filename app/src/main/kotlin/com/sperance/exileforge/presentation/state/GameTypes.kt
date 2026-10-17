package com.sperance.exileforge.presentation.state

/** Типы игры для экранов: режим, фаза, вкладки, здания, разделы кузницы и порядок сундука; общего состояния нет - экраны читают срезы. */
enum class AppMode { PLAYER, ADMIN }

typealias NoticeKind = com.sperance.exileforge.core.session.NoticeKind

/** Строка на языке игрока в момент показа - из :core. */
typealias Phrase = com.sperance.exileforge.core.i18n.Phrase

fun phrase(key: String, vararg args: Any?): Phrase = com.sperance.exileforge.core.i18n.phrase(key, *args)

typealias Notice = com.sperance.exileforge.core.session.Notice

/** Which of the three screens the app is on, above the tabs: the tabs only make sense once there is an account and a hero. */
enum class AppPhase { AUTH, CHARACTERS, GAME }

/** Вещь, принесённая походом, - из :core. */
typealias LootEntry = com.sperance.exileforge.core.campaign.LootEntry

/**
 * The forge's sections: orbs, the bench and the essences work on one item; печати Атласа - только у карты. [omens] - раздел
 * кладёт рядом с валютой знамение.
 */
enum class ForgeSection(val omens: Boolean = false) {
    ORBS(omens = true),
    SEALS(omens = true),
    BENCH,
    ESSENCES,
}

/**
 * The City's buildings: each one a screen of its own behind the square; «История» - прошлое героя,
 * «Летопись» (прежде страница «Развития») - его деяния и титулы, «Доска славы» - таблицы сервера.
 */
enum class Building { QUESTS, MERCHANT, AUCTION, GUILD, HISTORY, HALL }

/** How many heroes one account may hold when the rules have not been read yet. */
const val MAX_CHARACTERS = 3

/** The tabs, by name: the bottom bar a player sees, and the screens a button opens. */
const val TAB_ACCOUNT = 3
const val TAB_HERO = 4
const val TAB_TREE = 5

/** The City: the merchant, the auction and the guild. */
const val TAB_CITY = 6
const val TAB_CRAFT = 7
const val TAB_ADMIN = 8
const val TAB_REDEMPTION = 9
const val TAB_EXPEDITION = 10
const val TAB_CRAFTS = 11
const val TAB_SKILLS = 12

/** «Развитие»: кузня ([TAB_CRAFT]), дерево и гримуар; испытания и атлас - страницы «Похода». */
const val TAB_PROGRESS = 13
const val TAB_TRIALS = 15

/** «Настройки»: from the banner's menu, «back» leading to the tab it was opened over. */
const val TAB_SETTINGS = 17

// Порядок нижних кнопок (Ремёсла перед Городом): Герой, Развитие, Поход, Ремёсла, Город
val PLAYER_TABS = listOf(TAB_HERO, TAB_PROGRESS, TAB_EXPEDITION, TAB_CRAFTS, TAB_CITY)

/** Все кнопки нижней панели слева направо: вкладки игрока и вкладка администратора последней - по ним считается сторона перехода. */
val BAR_TABS = PLAYER_TABS + TAB_ADMIN

/** Screens only an administrator may open, whichever button leads to them. */
val ADMIN_TABS = setOf(TAB_ADMIN, TAB_REDEMPTION)
