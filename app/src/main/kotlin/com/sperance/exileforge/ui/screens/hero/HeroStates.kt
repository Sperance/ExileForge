package com.sperance.exileforge.ui.screens.hero

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.bodyPlaces
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.ClassesFile

/*
 * The Hero tab's parts, each cut from the state on its own (3.66.0). The screen as a whole is handed the state whenever
 * the hero moves; its parts are handed only what they draw, so gold that changed redraws the header and not the ledger
 * or a thousand lines of the stash.
 */

/** Who the hero is, as the header draws it: the purse and the experience move here and nowhere else. */
@Immutable data class HeroHeaderState(
    val name: String,
    val heroClass: String,
    val level: Int,
    val experience: Double,
    val money: Long,
    val portraits: Int,
    /** The tree's balance, counted by the rules; null until the content is read. */
    val tree: TreePoints?,
    /** The experience table the bar is scaled by; null until the content is read. */
    val classes: ClassesFile?,
)

/** The tree's points the header's chip says: free of all. */
@Immutable data class TreePoints(val available: Int, val total: Int)

/** One place on the body as the ledger draws it: the copy worn there (and its view, if the content knows its template). */
@Immutable data class PlaceState(
    val place: BodyPlace,
    val wornId: String?,
    val worn: ItemView?,
    val blocked: Boolean,
    /** Why the worn copy does not count; null when it does. */
    val reasons: List<String>?,
    /** How many loose items of the stash could fill the place. */
    val spare: Int,
)

/** What the hero wears, every place of the body in order, and what the lines need beside it. */
@Immutable data class EquipmentState(val places: List<PlaceState>, val lang: Lang, val signedIn: Boolean)

/** One line of the stash with what the rules say of it, counted once rather than on every pass over the list. */
@Immutable data class StashLine(val piece: ItemView, val worn: Boolean, val unwearable: List<String>, val price: Long?, val waiting: Boolean)

/** The header's cut, or null until the hero is read; the tree is counted again only when the level or the nodes move. */
@Composable fun rememberHeroHeader(s: ForgeState): HeroHeaderState? {
    val hero = s.hero ?: return null
    val index = s.index
    val tree = remember(index, hero.level, hero.tree) { s.treeState?.let { TreePoints(it.available, it.total) } }
    return remember(hero.info, s.world.portraits, tree, index) {
        val info = hero.info
        HeroHeaderState(info.name, info.heroClass, info.level, info.experience, info.money, s.world.portraits, tree, index?.classes)
    }
}

/** The ledger's cut, or null until the hero and the content are read; the purse is not part of it. */
@Composable fun rememberEquipment(s: ForgeState): EquipmentState? {
    val hero = s.hero ?: return null
    val index = s.index ?: return null
    return remember(hero.items, hero.inactive, index, s.lang, s.account.signedIn) {
        // How many loose items of each slot lie in the stash (2.48.0): each place says what it could take.
        val loose = hero.stash.filter { !it.socketed }.mapNotNull { index.template(it.template)?.slot }.groupingBy { it }.eachCount()
        val equipped = hero.equipped
        val places = bodyPlaces.map { place ->
            val worn = place.wornIn(equipped)
            PlaceState(place, worn?.id, worn?.let { s.view(it) }, blocked = place.blockedBy(equipped),
                reasons = worn?.let { hero.inactive[it.id] }, spare = place.fits.sumOf { loose[it] ?: 0 })
        }
        EquipmentState(places, s.lang, s.account.signedIn)
    }
}

/** The stash's lines over [visible], read again only when the shelf, the sheet or the queue of waiting commands moves. */
@Composable fun rememberStashLines(s: ForgeState, visible: List<ItemView>): List<StashLine> {
    val hero = s.hero
    val waiting = s.link.waitingItems
    return remember(visible, hero?.level, hero?.stats, s.index, waiting) {
        visible.map { piece ->
            val worn = piece.equipped || piece.socketed
            // The sheet added up here (2.46.0) says what the template needs, and the merchant's rule what it fetches.
            StashLine(piece, worn, s.unmetFor(piece.code), s.sellPrice(piece.item).takeUnless { worn }, piece.id in waiting)
        }
    }
}
