package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.character.FlaskMeasure
import com.sperance.exileforge.core.character.FlaskRow
import com.sperance.exileforge.core.i18n.ui
import kotlin.math.roundToInt

/** Имя строки сравнения фляг (4.2.0): мера листа фляги или характеристика, что фляга кладёт на героя. */
fun flaskRowTitle(row: FlaskRow): String = if (row.measure == FlaskMeasure.LINE) statTitle(row.stat) else ui("flask.measure.${row.measure.name}")

/** Число строки сравнения фляг [value] в единицах её меры: доли - процентами, длительность - секундами. */
fun flaskRowValue(row: FlaskRow, value: Double): String = when (row.measure) {
    FlaskMeasure.LIFE, FlaskMeasure.MANA -> number(value)
    FlaskMeasure.INSTANT, FlaskMeasure.EFFECT -> "${(value * 100).roundToInt()}%"
    FlaskMeasure.DURATION -> ui("flask.seconds", fineNumber(value))
    FlaskMeasure.CHARGES, FlaskMeasure.PER_USE -> fineNumber(value)
    FlaskMeasure.USES -> value.toInt().toString()
    FlaskMeasure.LINE -> statNumber(row.stat, value) + effectUnit(row.stat, row.op)
}
