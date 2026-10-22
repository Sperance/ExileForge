package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.ui
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Короткое число - одна точка для золота, Мощи и тесных счётчиков: меньше [SHORT_FROM] - целиком с разрядами через неразрывный
 * пробел («99 999»), дальше - тысячи, миллионы и миллиарды суффиксом словаря (`number.short.<ступень>`: «150к», «12кк», «2ккк»;
 * по-английски «k», «kk», «kkk»). Дробная цифра - одна и только меньше десяти единиц ступени, со знаком дроби словаря
 * (`number.decimal`): «1,5кк»; дробь отбрасывается, а не округляется вверх - короткое число не больше полного. Полное число
 * сокращённого - [fullIfShortened].
 */
fun compactNumber(value: Double): String {
    val size = abs(value).roundToLong()
    if (size < SHORT_FROM) return groupedNumber(value.roundToLong())
    val step = NumberStep.of(size)
    val tenths = size / (step.unit / 10)
    val figure = if (tenths < 100) {
        (tenths / 10).toString() + if (tenths % 10 == 0L) "" else ui("number.decimal") + tenths % 10
    } else {
        (tenths / 10).toString()
    }
    return (if (value < 0) "-" else "") + ui(step.key, figure)
}

/** Короткое число целого [value] - [compactNumber]. */
fun compactNumber(value: Long): String = compactNumber(value.toDouble())

/** Полное число с разрядами («1 520 340»), если [compactNumber] его сократил; не сокращено - null: подсказке нечего добавить. */
fun fullIfShortened(value: Double): String? = value.roundToLong().takeIf { abs(it) >= SHORT_FROM }?.let(::groupedNumber)

/** Полное число целого [value], если оно сокращено, - [fullIfShortened]. */
fun fullIfShortened(value: Long): String? = fullIfShortened(value.toDouble())

/** Ступень короткого числа: единица [unit] и ключ словаря [key] с суффиксом; от большей к меньшей. */
private enum class NumberStep(val unit: Long, val key: String) {
    BILLION(1_000_000_000L, "number.short.kkk"),
    MILLION(1_000_000L, "number.short.kk"),
    THOUSAND(1_000L, "number.short.k"),
    ;

    companion object {
        /** Наибольшая ступень, что помещается в [size] хотя бы раз. */
        fun of(size: Long): NumberStep = entries.first { size >= it.unit }
    }
}

/** С этого числа короткая запись сокращает: меньше - целиком. */
private const val SHORT_FROM = 100_000L
