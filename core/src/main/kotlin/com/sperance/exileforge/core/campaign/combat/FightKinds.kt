package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.rules.content.FightKind

/** Вид боя по его врагам (4.3.0): там, где арена своего вида не задаёт. */
object FightKinds {
    /** Бой похода: со стражем в стае - [FightKind.GUARDIAN], иначе - [FightKind.PACK]. */
    fun expedition(foes: List<Foe>): FightKind = if (foes.any { it.guards }) FightKind.GUARDIAN else FightKind.PACK
}
