package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.BagCategory

/** Ключ словаря заголовка полки сумки: `bag.section_<полка>`. */
val BagCategory.key: String get() = "bag.section_${name.lowercase()}"
