package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.Slot

/**
 * One line of the equipment ledger: a place on the body and the template slots that fill it. The hands are
 * two places rather than four slots — a one- or two-handed weapon in the main hand, a shield or a quiver in
 * the other. The rings and the flasks are places filled from one template slot: [place] is the worn slot the
 * server is asked to put the item in (`RING_2`, `FLASK_3`), null where the template's own slot decides.
 */
data class BodyPlace(val code: String, val fits: List<Slot>, val place: Slot? = null) {
    /** What is worn here, out of the hero's items keyed by the slot they are worn in. */
    fun <T> wornIn(equipped: Map<Slot, T>): T? = if (place != null) equipped[place] else fits.firstNotNullOfOrNull { equipped[it] }

    /** The off hand is taken whenever a two-handed weapon is. */
    fun blockedBy(equipped: Map<Slot, *>): Boolean = code == OFF_HAND && Slot.WEAPON_2H in equipped

    /** Whether an item of [slot] goes here. */
    fun takes(slot: Slot): Boolean = slot in fits

    companion object {
        const val MAIN_HAND = "MAIN_HAND"
        const val OFF_HAND = "OFF_HAND"
    }
}

/** The ledger's lines, top to bottom: the hands first, then the body, then the jewellery, then the belt's flasks. */
val bodyPlaces: List<BodyPlace> = listOf(
    BodyPlace(BodyPlace.MAIN_HAND, listOf(Slot.WEAPON_1H, Slot.WEAPON_2H)),
    BodyPlace(BodyPlace.OFF_HAND, listOf(Slot.SHIELD, Slot.QUIVER)),
) + listOf(Slot.HELMET, Slot.BODY, Slot.GLOVES, Slot.BOOTS, Slot.AMULET).map { BodyPlace(it.name, listOf(it)) } +
    listOf(BodyPlace(Slot.RING.name, listOf(Slot.RING), Slot.RING), BodyPlace(Slot.RING_2.name, listOf(Slot.RING), Slot.RING_2)) +
    listOf(Slot.BELT, Slot.WINGS, Slot.COLLAR).map { BodyPlace(it.name, listOf(it)) } +
    Slot.FLASKS.map { BodyPlace(it.name, listOf(Slot.FLASK), it) }
