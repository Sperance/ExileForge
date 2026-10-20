package com.sperance.exileforge.ui.screens.craft

import androidx.compose.runtime.Composable
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.forge.ForgeTarget
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.sellPrice
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.InspectAction
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import com.sperance.exileforge.ui.screens.hero.ItemDealSheets
import com.sperance.exileforge.ui.screens.hero.ItemDeals
import com.sperance.exileforge.ui.screens.hero.rememberItemDeals
import com.sperance.exileforge.ui.screens.hero.wearOffer
import org.koin.compose.viewmodel.koinViewModel

/**
 * Карточки вещей кузницы: главное действие - «В кузницу», у вещи, что уже на наковальне, - сменить её ([ForgeTarget.changeKey]), поэтому
 * своего «В кузницу» ряда действий по умолчанию карточка не несёт ([Inspect.Copy.forge]); рядом равное - надеть или
 * снять ([wearOffer], тот же выбор, что у листа вещи тайника, с местом из вкладок сравнения карточки); в «⋯» - продажа торговцу с ценой и
 * выставление на аукцион тем же путём, что из тайника ([ItemDeals]).
 */
internal class ForgeCards(private val game: GameUi, private val hero: HeroViewModel, private val shell: ShellViewModel, private val deals: ItemDeals) {
    /** Карточка вещи [item]; главное действие [onMain] - с подписью [main]. */
    fun of(item: ItemInstance, main: String, onMain: () -> Unit): Inspect.Copy {
        val can = !game.busy && game.session.signedIn && (game.ownsCharacter || game.isAdmin)
        val loose = !item.equipped && !item.socketed
        val twin = loose && game.index?.let { index -> game.hero?.uniqueWorn(index, item) } == true
        val wear = wearOffer(game, hero, shell, item, can, twin) {}
        val price = game.sellPrice(item)
        return Inspect.Copy(
            item,
            action = InspectAction(main) { onMain() },
            side = wear?.let { offer -> InspectAction(offer.label, enabled = offer.enabled) { offer.run(wearInto) } },
            menu = listOf(
                InspectAction(ui("item.sell_merchant"), enabled = can && !item.locked, figure = price?.let { number(it.toDouble()) }) { deals.sell(item) },
                InspectAction(ui("item.list_auction"), enabled = can && !item.locked) { deals.auction(item) },
            ),
            forge = false,
        )
    }
}

/** Карточки вещей кузницы над героем [game] и листы их сделок поверх экрана. */
@Composable internal fun rememberForgeCards(game: GameUi): ForgeCards {
    val hero = koinViewModel<HeroViewModel>()
    val shell = koinViewModel<ShellViewModel>()
    val deals = rememberItemDeals()
    ItemDealSheets(game, hero, deals)
    return ForgeCards(game, hero, shell, deals)
}
