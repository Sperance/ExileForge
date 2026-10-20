package com.sperance.exileforge.ui.screens.hero

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.ChordOverWindow
import com.sperance.exileforge.ui.components.DialogShape
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.screens.auction.ListingSheet
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment

/** Сделка с вещью, открытая поверх экрана: выставить на аукцион или сначала снять надетую. */
internal enum class ItemDeal { AUCTION, WORN }

/**
 * Сделки с вещью тайника - один путь для листа вещи и карточки кузницы: лот через лист выставления. Надетую вещь не выставить (`AU_010`) -
 * сделка предлагает её снять. Продажа торговцу - удержанием в ряду своей вещи ([com.sperance.exileforge.ui.components.OwnItemActions]), без листа. Листы рисует [ItemDealSheets].
 */
@Stable
class ItemDeals internal constructor() {
    internal var open by mutableStateOf<Pair<ItemDeal, String>?>(null)

    /** Выставить [item] на аукцион; надетую - сначала снять. */
    fun auction(item: ItemInstance) {
        open = (if (item.loose) ItemDeal.AUCTION else ItemDeal.WORN) to item.id
    }

    internal fun close() {
        open = null
    }
}

/** Вещь лежит в тайнике: не надета и не в гнезде. */
private val ItemInstance.loose: Boolean get() = !equipped && !socketed

@Composable fun rememberItemDeals(): ItemDeals = remember { ItemDeals() }

/** Листы сделок [deals] над вещью героя; [onDone] - сделка ушла (лот выставлен). Вещи уже нет - лист закрывается. */
@Composable fun ItemDealSheets(game: GameUi, model: HeroViewModel, deals: ItemDeals, onDone: () -> Unit = {}) {
    val (deal, itemId) = deals.open ?: return
    val instance = game.hero?.item(itemId)
    val view = instance?.let { game.view(it) }
    if (instance == null || view == null) {
        LaunchedEffect(itemId) { deals.close() }
        return
    }
    val name = view.title
    val can = !game.busy && game.session.signedIn && (game.ownsCharacter || game.isAdmin)
    when (deal) {
        ItemDeal.AUCTION -> ListingSheet(game, name, onDismiss = deals::close, hint = {
            model.priceHint(instance.template, instance.rarity, view.level)
        }) { orb, price, _ ->
            deals.close()
            onDone()
            model.sellEquipment(instance.id, orb, price)
        }

        ItemDeal.WORN -> AlertDialog(
            onDismissRequest = deals::close,
            containerColor = PanelRaised,
            shape = DialogShape,
            tonalElevation = 0.dp,
            title = {
                ChordOverWindow()
                Text(ui("hero.worn_title"), color = Gold)
            },
            text = { Text(ui("hero.worn_note"), color = Parchment) },
            confirmButton = {
                ForgeButton(state = ButtonState.of(can), onClick = {
                    deals.close()
                    if (instance.socketed) model.unsocketJewel(instance.id) else model.unequip(instance.id)
                }, kind = ButtonKind.Text) { Text(ui("hero.unequip")) }
            },
            dismissButton = { ForgeButton(onClick = deals::close, kind = ButtonKind.Text) { Text(ui("common.close")) } },
        )
    }
}

/** Действие надевания вещи: подпись, доступность и что делает с местом, выбранным в карточке (null - решает слот шаблона). */
class WearOffer(val label: String, val enabled: Boolean, val run: (Slot?) -> Unit)

/**
 * Надеть, снять или в дерево - по тому, где вещь [item] и что она: в гнезде и надетая - «Снять», самоцвет - «В дерево» (гаснет без
 * свободного взятого гнезда), прочее - «Надеть» (гаснет без требований и второй копией уникалки [twin]); карта не надевается - null.
 * [can] - герой свой и свободен; [onDone] - перед командой (лист закрывается).
 */
fun wearOffer(game: GameUi, model: HeroViewModel, shell: ShellViewModel, item: ItemInstance, can: Boolean, twin: Boolean, onDone: () -> Unit): WearOffer? {
    val slot = game.index?.template(item.template)?.slot
    return when {
        item.socketed -> WearOffer(ui("hero.unequip"), can) {
            onDone()
            model.unsocketJewel(item.id)
        }

        item.equipped -> WearOffer(ui("hero.unequip"), can) {
            onDone()
            model.unequip(item.id)
        }

        slot == null || slot == Slot.MAP -> null

        slot == Slot.JEWEL -> {
            val socket = game.index?.let { game.hero?.freeSocket(it) }
            WearOffer(ui("hero.to_tree"), can && socket != null) {
                onDone()
                socket?.let(shell::openTree)
            }
        }

        else -> WearOffer(ui("hero.equip"), can && game.unmetFor(item.template).isEmpty() && !twin) { place ->
            onDone()
            model.equip(item.id, place)
        }
    }
}
