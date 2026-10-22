package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.ui
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

/**
 * Короткое число - одна точка для золота, Мощи и тесных счётчиков: меньше [SHORT_FROM] - целиком с разрядами через неразрывный
 * пробел («99 999»), дальше - тысячи, миллионы и миллиарды суффиксом словаря (`number.short.<ступень>`: «150к», «12кк», «2ккк»;
 * по-английски «k», «kk», «kkk»). Дробная цифра - одна и только меньше десяти единиц ступени, со знаком дроби словаря
 * (`number.decimal`): «1,5кк»; дробь отбрасывается, а не округляется вверх - короткое число не больше полного. Полное число
 * сокращённого - [fullIfShortened].
 */
fun compactNumber(value: Long): String {
    if (!shortened(value)) return groupedNumber(value)
    val size = if (value == Long.MIN_VALUE) Long.MAX_VALUE else value.absoluteValue
    val step = NumberStep.of(size)
    val tenths = size / (step.unit / 10)
    val figure = (tenths / 10).toString() + if (tenths < 100 && tenths % 10 != 0L) ui("number.decimal") + tenths % 10 else ""
    return (if (value < 0) "-" else "") + ui(step.key, figure)
}

/** Короткое число дробного [value]: округлено до целого - [compactNumber]. */
fun compactNumber(value: Double): String = compactNumber(value.roundToLong())

/** Полное число с разрядами («1 520 340»), если [compactNumber] его сократил; не сокращено - null: подсказке нечего добавить. */
fun fullIfShortened(value: Long): String? = if (shortened(value)) groupedNumber(value) else null

/** Сокращает ли [compactNumber] число [value]. */
private fun shortened(value: Long): Boolean = value <= -SHORT_FROM || value >= SHORT_FROM

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
