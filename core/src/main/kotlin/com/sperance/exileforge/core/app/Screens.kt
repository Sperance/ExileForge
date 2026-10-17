package com.sperance.exileforge.core.app

/** Экран сверху стека глазами ядра: фаза, номер вкладки и здание Города. */
data class Spot(val phase: AppPhase, val tab: Int, val building: Building?)

/**
 * Куда модели ядра ведут игрока: фазы входа, корни вкладок героя и администратора, Атлас «Похода», аукцион и гильдия Города,
 * страницы настроек с письмом администратора и окном модерации.
 */
enum class Landing { AUTH, CHARACTERS, FATE, HERO, ADMIN, ATLAS, AUCTION, GUILD, MAIL, MODERATION }

/**
 * Стек экранов глазами ядра: его держит навигатор `:app` (ключи Navigation 3), сервисы читают верх и просят переходы. Вкладка,
 * закрытая уровнем героя, не открывается - как у самого навигатора.
 */
interface Screens {
    /** Что сейчас сверху стека. */
    val spot: Spot

    /** Смена фазы: стек из одного экрана [landing]. */
    fun reset(landing: Landing)

    /** Вкладка [landing] или экран под её корнем. */
    fun tab(landing: Landing)

    /** Экран [landing] поверх открытого. */
    fun open(landing: Landing)
}
