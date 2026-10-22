package com.sperance.exileforge.ui.components

import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.rules.content.ContestHonour
import com.sperance.exileforge.ui.theme.Bronze

/** Медали трёх ступеней по порядку: бронза, серебро, золото - ступени достижений летописи и рамки призовых мест состязаний. */
val Medals: List<Color> = listOf(Color(0xFFC08457), Color(0xFFC9D1D9), Color(0xFFFFD166))

/** Медаль места [place] состязания: I - золото, II - серебро, III - бронза. */
fun medalOf(place: Int): Color = Medals[(Medals.size - place).coerceIn(0, Medals.lastIndex)]

/** Рамка портрета героя с титулом [honour] - медаль его места; без титула - [plain]. */
fun honourRing(honour: ContestHonour?, plain: Color = Bronze): Color = honour?.let { medalOf(it.place) } ?: plain
