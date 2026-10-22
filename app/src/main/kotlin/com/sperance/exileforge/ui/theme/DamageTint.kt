package com.sperance.exileforge.ui.theme

import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.campaign.combat.DamageType

/** Цвет вида урона [type] - у числа удара, в журнале боя и у урона на карточке вещи; физический и без вида - по герою красным, иначе светлым. */
internal fun damageTint(type: DamageType?, onHero: Boolean = false): Color = when (type) {
    DamageType.FIRE -> Ember
    DamageType.COLD -> ShieldCyan
    DamageType.LIGHTNING -> Color(0xFFFFD34A)
    DamageType.CHAOS -> Elder
    else -> if (onHero) LifeRed else Parchment
}
