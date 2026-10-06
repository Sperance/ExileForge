package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.trade.Shortfall

/**
 * Недостача одной строкой (3.89.0): «Не хватает: 2 Сфера хаоса, 30 золота» - предметы по своим именам, золото последним.
 * Всего хватает - null. Золото после «не хватает» всегда в родительном падеже, поэтому формы числа ему не нужны.
 */
fun Shortfall.text(): String? = if (isEmpty) {
    null
} else {
    val parts = items.map { (code, missing) -> ui("shortfall.item", missing, itemTitle(code)) } + listOfNotNull(gold.takeIf { it > 0 }?.let { ui("shortfall.gold", it) })
    ui("shortfall.line", parts.joinToString(", "))
}
