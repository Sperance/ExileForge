package com.sperance.exileforge.core.app

/** Типы игры для экранов: фаза, вкладки, здания, разделы кузницы и порядок сундука; общего состояния нет - экраны читают срезы. */
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
 * «Летопись» (прежде страница «Развития») - его деяния и титулы, «Доска славы» - таблицы сервера, «Состязания» - сутки и неделя по лигам.
 * Гильдия - не здание, а своя вкладка панели ([TAB_GUILD]).
 */
enum class Building { QUESTS, MERCHANT, AUCTION, HISTORY, HALL, CONTESTS }

/** How many heroes one account may hold when the rules have not been read yet. */
const val MAX_CHARACTERS = 3

/** The tabs, by name: the bottom bar a player sees, and the screens a button opens. */
const val TAB_ACCOUNT = 3
const val TAB_HERO = 4
const val TAB_TREE = 5

/** Город: доска заданий, торговец, рынок, история и доска славы. */
const val TAB_CITY = 6
const val TAB_CRAFT = 7
const val TAB_REDEMPTION = 9
const val TAB_EXPEDITION = 10

/** Ремёсла: экран над хабом «Развития» ([TAB_PROGRESS]). */
const val TAB_CRAFTS = 11
const val TAB_SKILLS = 12

/** «Развитие»: кузня ([TAB_CRAFT]), дерево, гримуар и ремёсла ([TAB_CRAFTS]); испытания и атлас - страницы «Похода». */
const val TAB_PROGRESS = 13
const val TAB_TRIALS = 15

/** «Настройки»: from the banner's menu, «back» leading to the tab it was opened over. */
const val TAB_SETTINGS = 17

/** «Гильдия»: своя вкладка панели; до уровня открытия гильдий - экран замка. */
const val TAB_GUILD = 18

/** Кнопки нижней панели слева направо: Герой, Развитие, Поход, Город, Гильдия - по ним считается сторона перехода. */
val PLAYER_TABS = listOf(TAB_HERO, TAB_PROGRESS, TAB_EXPEDITION, TAB_CITY, TAB_GUILD)
