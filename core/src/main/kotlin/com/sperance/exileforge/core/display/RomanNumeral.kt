package com.sperance.exileforge.core.display

/**
 * Число [value] римскими цифрами - места, главы и ряды: «I», «XIV», «MMXXVI». Римской записи нет у нуля, отрицательных и чисел больше
 * 3999 - они печатаются арабскими.
 */
fun romanNumeral(value: Int): String {
    if (value !in 1..ROMAN_MAX) return value.toString()
    var rest = value
    return buildString {
        ROMAN_DIGITS.forEach { (size, sign) ->
            while (rest >= size) {
                append(sign)
                rest -= size
            }
        }
    }
}

private const val ROMAN_MAX = 3999

private val ROMAN_DIGITS = listOf(
    1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
)
