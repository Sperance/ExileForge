package com.sperance.exileforge.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Цвета механик: значки вкладок режимов ([com.sperance.exileforge.ui.components.StoneTabs]) - Доски славы и «Похода».
 * Свой цвет у каждой механики, экраны берут его отсюда, а не пишут числом.
 */
object ModeHue {
    /** Разлом - ядовито-зелёный его палитры. */
    val Rift = Color(0xFF39FF88)

    /** Башня - бронза. */
    val Tower = Brass

    /** Испытание боссов - кровь. */
    val Bosses = LifeRed

    /** Гильдии - руна. */
    val Guilds = Rune

    /** Профессии - золото. */
    val Professions = Color(0xFFF2D27A)

    /** Мощь - золото печати ⚜. */
    val Might = Caution

    /** Находки уникальных - имя уникальной вещи, «тёплый уголь». */
    val Uniques = Color(0xFFFFB774)
}
