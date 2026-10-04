package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.display.ItemSearch
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot

/**
 * A shelf of the stash's slot chips (3.30.0): the slots the rules tag alike are one — both weapons, both rings,
 * the three flask bays — and every other slot is its own.
 */
@JvmInline value class SlotGroup(val tag: String) {
    fun title(lang: Lang): String = when (tag) {
        WEAPON, RING, FLASK, TOOL -> ui("stash.group.$tag")
        else -> Slot.entries.firstOrNull { it.tag == tag }?.let { slotTitle(it, lang) } ?: tag
    }

    companion object {
        const val WEAPON = "weapon"
        const val RING = "ring"
        const val FLASK = "flask"
        const val TOOL = "tool"
        fun of(slot: Slot) = SlotGroup(slot.tag)
    }
}

/**
 * The stash's filters (3.30.0): slot groups, rarities, «can wear» and a text query. Empty sets let everything
 * through; they are the screen's own and reset when it is left — only the [StashSort] and «hide equipped» are kept on the device.
 */
data class StashFilter(
    val groups: Set<SlotGroup> = emptySet(),
    val rarities: Set<Rarity> = emptySet(),
    val wearable: Boolean = false,
    val query: String = "",
) {
    val active: Boolean get() = groups.isNotEmpty() || rarities.isNotEmpty() || wearable || query.isNotBlank()

    /** Whether [piece] passes; [unmet] names the requirements the hero misses for a template. */
    fun admits(piece: ItemView, unmet: (String) -> List<String>): Boolean = (groups.isEmpty() || SlotGroup.of(piece.slot) in groups) &&
        (rarities.isEmpty() || piece.rarity in rarities) &&
        (!wearable || unmet(piece.code).isEmpty()) &&
        ItemSearch.matches(piece, query)

    fun toggle(group: SlotGroup) = copy(groups = groups.toggled(group))
    fun toggle(rarity: Rarity) = copy(rarities = rarities.toggled(rarity))

    private fun <T> Set<T>.toggled(value: T): Set<T> = if (value in this) this - value else this + value
}

/**
 * The stash in the chosen order. The server keeps it oldest first, so the newest is the list reversed; every
 * other order falls back to the newest first among equals. [price] is the merchant's for a copy, when known.
 */
fun StashSort.order(pieces: List<ItemView>, price: (ItemView) -> Long?): List<ItemView> {
    val newest = pieces.asReversed()
    return when (this) {
        StashSort.NEWEST -> newest
        StashSort.RARITY -> newest.sortedByDescending { it.rarity.ordinal }
        StashSort.LEVEL -> newest.sortedByDescending { it.level }
        StashSort.PRICE -> newest.sortedByDescending { price(it) ?: -1L }
    }
}

/** The stash, filtered and ordered as the screen shows it; [hideWorn] leaves out what is worn or socketed (3.69.0). */
fun ForgeState.stashShelf(pieces: List<ItemView>, filter: StashFilter, hideWorn: Boolean = false): List<ItemView> = stashSort.order(
    pieces.filter { piece ->
        !(hideWorn && piece.isWorn) && filter.admits(piece) { code -> unmetFor(code) }
    },
) { sellPrice(it.item) }

/** То же для среза «игра» (3.80.33). */
fun GameUi.stashShelf(pieces: List<ItemView>, filter: StashFilter, hideWorn: Boolean = false): List<ItemView> = stashSort.order(
    pieces.filter { piece ->
        !(hideWorn && piece.isWorn) && filter.admits(piece) { code -> unmetFor(code) }
    },
) { sellPrice(it.item) }

/** Whether the hero wears this copy, on the body or in a socket. */
val ItemView.isWorn: Boolean get() = equipped || socketed
