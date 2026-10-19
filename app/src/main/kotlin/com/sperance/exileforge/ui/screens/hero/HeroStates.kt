package com.sperance.exileforge.ui.screens.hero

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.FateKit
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.FateCounter
import com.sperance.exileforge.core.display.FateSource
import com.sperance.exileforge.core.display.FateTally
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.bodyPlaces
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.core.sheet.sellPrice
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.ClassesFile
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.content.FateLever
import com.sperance.exileforge.ui.components.accountRole
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Who the hero is, as the header draws it: the purse and the experience move here and nowhere else.
 *
 * Части вкладки «Герой», каждая вырезана из состояния отдельно: часть получает только то, что рисует, - золото перерисовывает шапку, а не
 * тайник.
 */
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
    /** Роль аккаунта: значок рядом с именем, у игрока - нет. */
    val role: com.sperance.exileforge.core.network.AccountRole? = null,
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
@Composable fun rememberHeroHeader(game: GameUi): HeroHeaderState? {
    val hero = game.hero ?: return null
    val index = game.index
    val tree = remember(index, hero.level, hero.tree) { game.treeState?.let { TreePoints(it.available, it.total) } }
    val role = game.session.profile?.role
    return remember(hero.info, game.world.portraits, tree, index, role) {
        val info = hero.info
        HeroHeaderState(info.name, info.heroClass, info.level, info.experience, info.money, game.world.portraits, tree, index?.classes, accountRole(role))
    }
}

/** Предначертание аккаунта героя карточкой; не выбрано или нет в контенте - null. */
internal fun heroFate(game: GameUi): FateCard? = game.index?.fates?.of(game.hero?.info?.fate)?.let(FateCard::of)

/** Дар героя на его силе и с действующими строками надетого - тем же резолвером правил, что бой; нет дара - null. */
@Composable internal fun rememberFateEffects(game: GameUi): FateEffects? {
    val hero = game.hero ?: return null
    val index = game.index ?: return null
    return remember(hero, index) { FateKit.of(index, hero.info.fate, hero.stats, hero.equipped.values).effects.takeIf { it.fate != null } }
}

/**
 * Счёт дара героя ([FateCounter]) - чип шапки и подпись ленты; дар без счёта - null. Вне захода - рычаги героя [effects] и
 * счета сервера; в идущем заходе [runs] с Разгоном - рычаги захода и его выигранные бои, и счёт пересчитывается, только когда
 * меняется их число, а не на каждый снимок захода.
 */
@Composable internal fun rememberFateTally(game: GameUi, effects: FateEffects?, runs: StateFlow<ExpeditionRun?>): FateTally? {
    val info = game.heroInfo ?: return null
    val run by runs.collectAsStateWithLifecycle()
    val streak = run?.takeIf { it.run.fate.has(FateLever.WIN_STREAK) }
    val won by remember(streak) { streak?.let { r -> r.hud.map { r.fateRun.won }.distinctUntilChanged() } ?: flowOf(0) }.collectAsStateWithLifecycle(streak?.fateRun?.won ?: 0)
    return remember(info, effects, streak, won, game.lang) {
        val source = streak?.let { FateSource(info, it.run.fate, won) } ?: effects?.let { FateSource.idle(info, it) }
        source?.let(FateCounter::of)
    }
}

/**
 * The ledger's cut, or null until the hero and the content are read; the purse is not part of it.
 *
 * Сколько свободных вещей каждого слота лежит в тайнике: место говорит, что могло бы взять.
 */
@Composable fun rememberEquipment(game: GameUi): EquipmentState? {
    val hero = game.hero ?: return null
    val index = game.index ?: return null
    return remember(hero.items, hero.inactive, index, game.lang, game.session.signedIn) {
        val loose = hero.stash.filter { !it.socketed }.mapNotNull { index.template(it.template)?.slot }.groupingBy { it }.eachCount()
        val equipped = hero.equipped
        val places = bodyPlaces.map { place ->
            val worn = place.wornIn(equipped)
            PlaceState(
                place,
                worn?.id,
                worn?.let { game.view(it) },
                blocked = place.blockedBy(equipped),
                reasons = worn?.let { hero.inactive[it.id] },
                spare = place.fits.sumOf { loose[it] ?: 0 },
            )
        }
        EquipmentState(places, game.lang, game.session.signedIn)
    }
}

/**
 * The stash's lines over [visible], read again only when the shelf, the sheet or the queue of waiting commands moves.
 *
 * Лист, сложенный здесь, говорит, что нужно шаблону, правило торговца - сколько стоит.
 */
@Composable fun rememberStashLines(game: GameUi, visible: List<ItemView>): List<StashLine> {
    val hero = game.hero
    val waiting = game.link.waitingItems
    return remember(visible, hero?.level, hero?.stats, game.index, waiting) {
        visible.map { piece ->
            val worn = piece.equipped || piece.socketed
            StashLine(piece, worn, game.unmetFor(piece.code), game.sellPrice(piece.item).takeUnless { worn }, piece.id in waiting)
        }
    }
}
