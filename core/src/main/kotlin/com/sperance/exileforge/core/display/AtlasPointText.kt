package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.AtlasPoints

/**
 * За что очко Атласа: ключ `<вид>:<место>` ([AtlasPoints.key]) - «босс Склепа изгнанников», «башня, рубеж 10»,
 * «раш: Пустоши». Место карты и региона - их имя, у башни - этаж.
 */
fun atlasPointSource(key: String): String {
    val kind = key.substringBefore(':')
    val place = key.substringAfter(':', "")
    val named = when (kind) {
        AtlasPoints.RUSH -> regionTitle(place)
        AtlasPoints.TOWER -> place
        else -> mapTitle(place)
    }
    return if (kind in AtlasPoints.ALL) ui("atlas.earned.$kind", named) else named
}

/** Тост о новом очке Атласа: «+1 очко атласа — босс Склепа изгнанников». */
fun atlasPointToast(key: String): String = ui("atlas.earned.toast", atlasPointSource(key))
