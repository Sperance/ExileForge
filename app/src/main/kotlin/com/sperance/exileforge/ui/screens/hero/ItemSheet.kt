package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.bodyPlaces
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.forge.SmithyViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.screens.auction.ListingSheet
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** What the action row opened on top of the sheet, if anything. */
private enum class ItemAction { AUCTION, SELL, WORN }

/** The template a zone's map is named after: `MAP_<zone code>`; the card's map action walks it back. */

/**
 * One item of the stash: its card, and what can be done with it.
 *
 * The card scrolls; the actions do not — they sit in a row at the foot of the sheet, so the thing a
 * player came to do is never below the fold. Each one is a single tap: wearing and taking off at
 * once, an orb and the crafting bench in the forge, opened over this item, a listing through a small sheet of its own, and selling to
 * the merchant through the held confirmation, because that one cannot be taken back. Every rule behind them is the
 * server's, and it checks them again; since 2.46.0 the client adds the sheet up itself, so wearing
 * is also off for an item whose requirements it misses, and the card says what it would change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSheet(game: GameUi, model: HeroViewModel, itemId: String, onDismiss: () -> Unit) {
    val shell: ShellViewModel = koinViewModel()
    val smithy = koinViewModel<SmithyViewModel>()
    val instance = game.hero?.item(itemId)
    val view = instance?.let { game.view(it) }
    // The item can leave while its sheet is open — sold, listed, rolled into a copy — and then the
    // sheet has nothing left to be about; nor is there a card for a copy the content cannot explain.
    if (instance == null || view == null) {
        LaunchedEffect(itemId) { onDismiss() }
        return
    }
    val name = view.title
    val can = !game.busy && game.session.signedIn && (game.ownsCharacter || game.isAdmin)
    val loose = !instance.equipped && !instance.socketed
    val price = game.sellPrice(instance)
    val reachable = game.unmetFor(instance.template).isEmpty()
    // A locked item (3.30.0) is kept from the merchant and the auction; the forge still works on it.
    val locked = instance.locked
    val waiting = instance.id in game.link.waitingItems
    var open by remember(itemId) { mutableStateOf<ItemAction?>(null) }
    // «Заменить» (3.81.0): the place this copy is worn in, opened on what could go there instead.
    var replacing by remember(itemId) { mutableStateOf<BodyPlace?>(null) }
    // Место пары для «Если надеть» и «Надеть» (3.90.3): с каким из колец сравнивать и куда надевать.
    val wear = rememberWearChoice(game, instance)
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
            val look = relicLook(view.rarity)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    ItemCard(
                        view,
                        enabled = false,
                        detailed = true,
                        price = price,
                        waiting = waiting,
                        totals = wearTotals(game, instance, wear),
                        requirementsMet = reachable,
                    )
                }
                if (locked) item { Text(ui("item.locked_hint"), color = Muted, style = MaterialTheme.typography.bodySmall) }
                temperOffer(game, instance)?.let { (ore, need) ->
                    item {
                        // The smith's tempering (3.79.0): once per weapon or armour, the ore of its level.
                        ForgePanel {
                            Engraved(ui("temper.title"))
                            MutedText(
                                ui(
                                    "temper.hint",
                                    game.index?.rules?.brews?.temper?.let { "${it.minQuality}–${it.maxQuality}" }.orEmpty(),
                                    game.index?.rules?.brews?.temper?.maxLevels ?: 0,
                                    game.index?.rules?.brews?.temper?.smithLevel ?: 0,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            ForgeOutlinedButton(enabled = can && (game.bagAmount(ore) ?: 0L) >= need, onClick = { model.temper(instance.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text(ui("temper.go", itemTitle(ore), need, game.bagAmount(ore) ?: 0L))
                            }
                        }
                    }
                }
                // Самоцвет без свободного взятого гнезда (3.94.1): почему «В дерево» погасла.
                if (view.slot == Slot.JEWEL && !instance.socketed && game.index?.let { game.hero?.freeSocket(it) } == null) {
                    item { Text(ui("hero.to_tree_none"), color = LifeRed, style = MaterialTheme.typography.bodySmall) }
                }
                // Worn but not counting: the rules' reasons, as the slot cell prints them.
                game.hero?.inactive?.get(instance.id)?.let { reasons ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(ui("hero.inactive"), color = LifeRed, style = MaterialTheme.typography.labelLarge)
                            reasons.forEach { Text(requirementReason(it, game.lang), color = LifeRed, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
            // Кнопки «Реликвария» (3.88.6; 3.91.0 - одна низкая строка): главное действие, кузня и «ещё» с заменой, замком, аукционом и продажей.
            var more by remember(itemId) { mutableStateOf(false) }
            // Место, где надета вещь (3.88.8): «Заменить» открывает его на всё, что туда идёт.
            val wornPlace = game.hero?.equipped?.let { worn -> bodyPlaces.firstOrNull { it.wornIn(worn)?.id == instance.id } }
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    instance.socketed -> PrimaryPill(ui("hero.unequip"), can, look) {
                        onDismiss()
                        model.unsocketJewel(instance.id)
                    }

                    instance.equipped && wornPlace != null -> PrimaryPill(ui("hero.replace"), can, look) { replacing = wornPlace }

                    instance.equipped -> PrimaryPill(ui("hero.unequip"), can, look) {
                        onDismiss()
                        model.unequip(instance.id)
                    }

                    // Карта не надевается и в зону из карточки не ведёт (3.88.7): её выбирают в окне запуска зоны.
                    view.slot == Slot.MAP -> Unit

                    // Самоцвет не надевается, а встаёт в гнездо дерева (3.94.1): кнопка ведёт к первому свободному взятому
                    // гнезду; нет такого - гаснет, причина под карточкой.
                    view.slot == Slot.JEWEL -> {
                        val socket = remember(game.hero, game.index) { game.index?.let { game.hero?.freeSocket(it) } }
                        PrimaryPill(ui("hero.to_tree"), can && socket != null, look) {
                            onDismiss()
                            socket?.let(shell::openTree)
                        }
                    }

                    else -> PrimaryPill(ui("hero.equip"), can && reachable, look) {
                        onDismiss()
                        model.equip(instance.id, wear.target)
                    }
                }
                // One way into the forge (2.51.0): its orbs and bench are its own tabs; у карты кузница - главная кнопка.
                QuietPill(can, modifier = if (view.slot == Slot.MAP) Modifier.weight(1f) else Modifier, onClick = {
                    onDismiss()
                    smithy.open(instance.id, ForgeSection.ORBS)
                    shell.tab(TAB_CRAFT)
                }) { Text(ui("nav.forge"), color = if (can) GoldBright else Muted, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                Box {
                    QuietPill(true, square = true, onClick = { more = true }) {
                        Icon(Icons.Outlined.MoreHoriz, ui("common.more"), tint = GoldBright, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = more, onDismissRequest = { more = false }, containerColor = PanelRaised) {
                        // «Снять» надетую вещь (3.88.8) - в меню: главная кнопка у неё «Заменить».
                        if (instance.equipped && wornPlace != null) {
                            MenuLine(ForgeGlyphs.Helm, ui("hero.unequip"), can) {
                                more = false
                                onDismiss()
                                model.unequip(instance.id)
                            }
                        }
                        MenuLine(if (locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen, ui(if (locked) "item.unlock" else "item.lock"), can) {
                            more = false
                            model.lockItem(instance.id, !locked)
                        }
                        // A worn item cannot be listed or sold (AU_010, CH_014): the tap says so instead of doing nothing.
                        MenuLine(ForgeGlyphs.Scales, ui("hero.action_auction"), can && !locked) {
                            more = false
                            open = if (loose) ItemAction.AUCTION else ItemAction.WORN
                        }
                        MenuLine(ForgeGlyphs.Coins, ui("hero.action_sell"), can && !locked, LifeRed) {
                            more = false
                            open = if (loose) ItemAction.SELL else ItemAction.WORN
                        }
                    }
                }
            }
        }
    }
    when (open) {
        ItemAction.AUCTION -> ListingSheet(game, name, onDismiss = { open = null }, hint = {
            model.priceHint(instance.template, instance.rarity, game.index?.template(instance.template)?.let(instance::level) ?: 0)
        }) { orb, price, _ ->
            open = null
            onDismiss()
            model.sellEquipment(instance.id, orb, price)
        }

        // Selling is final and takes the rolls with it, so it is asked about by name, with the sum
        // worked out here by the merchant's own rule.
        ItemAction.SELL -> ConfirmSheet(
            title = ui("hero.sell_q"),
            subtitle = name,
            danger = true,
            icon = { ItemIcon(view, rarityColor(view.rarity.name), Modifier.size(44.dp)) },
            ledger = listOfNotNull(
                LedgerLine(ui("confirm.give"), name, Tone.SPEND),
                LedgerLine(ui("confirm.gain"), price?.let { ui("merchant.gold_amount", it) } ?: ui("confirm.gold_by_server"), Tone.GAIN),
                // Осколки сфер за неё (3.95.3): что продажа даст кроме золота
                game.shardsFor(listOf(view.rarity))?.let { LedgerLine(ui("confirm.gain"), it, Tone.GAIN) },
            ),
            note = ui("hero.sell_confirm"),
            confirm = ui("hero.sell_do"),
            onDismiss = { open = null },
        ) {
            onDismiss()
            model.sellForGold(instance.id)
        }

        ItemAction.WORN -> AlertDialog(
            onDismissRequest = { open = null },
            containerColor = PanelRaised,
            shape = DialogShape,
            tonalElevation = 0.dp,
            title = { Text(ui("hero.worn_title"), color = Gold) },
            text = { Text(ui("hero.worn_note"), color = Parchment) },
            confirmButton = {
                ForgeTextButton(enabled = can, onClick = {
                    open = null
                    if (instance.socketed) model.unsocketJewel(instance.id) else model.unequip(instance.id)
                }) { Text(ui("hero.unequip")) }
            },
            dismissButton = { ForgeTextButton(onClick = { open = null }) { Text(ui("common.close")) } },
        )

        null -> Unit
    }
    replacing?.let { place ->
        SlotPicker(game, place, onDismiss = { replacing = null }, onEquip = { id ->
            onDismiss()
            model.equip(id, place.place)
        })
    }
}

/** Главная кнопка листа: пилюля во всю ширину, у легенды - золотая. */
@Composable private fun RowScope.PrimaryPill(label: String, enabled: Boolean, look: RelicLook, onClick: () -> Unit) {
    val shape = RoundedCornerShape(PillHeight / 2)
    Box(
        Modifier.weight(1f).height(PillHeight).then(if (enabled) Modifier.glow(look.glow, radius = 10.dp, shape = shape) else Modifier).clip(shape)
            .then(if (enabled) Modifier.background(look.primary, shape) else Modifier.background(PanelRaised, shape))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) look.onPrimary else Muted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

/** Тихая кнопка рядом с главной: «Кузня» и «ещё». */
@Composable private fun QuietPill(enabled: Boolean, square: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val shape = RoundedCornerShape(PillHeight / 2)
    Row(
        modifier.height(PillHeight).then(if (square) Modifier.width(PillHeight) else Modifier).depthRaised(shape).clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = if (square) 0.dp else 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Строка меню «ещё»: рисунок и слово. */
@Composable private fun MenuLine(icon: ImageVector, label: String, enabled: Boolean, accent: Color = Gold, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = if (enabled) Parchment else Muted.copy(alpha = .6f)) },
        leadingIcon = { Icon(icon, null, tint = if (enabled) accent else Muted.copy(alpha = .45f), modifier = Modifier.size(20.dp)) },
        enabled = enabled,
        onClick = onClick,
    )
}

/** The ore and its amount the smith asks to temper [item] (3.79.0), or null when it cannot be: not gear, tempered, corrupted. */
private fun temperOffer(game: GameUi, item: com.sperance.exileforge.rules.roll.ItemInstance): Pair<String, Long>? {
    val index = game.index ?: return null
    val template = index.template(item.template) ?: return null
    val rule = index.rules.brews.temper
    if (item.tempered || item.corrupted || !(template.slot.isWeapon || template.slot.isArmour)) return null
    // The smith's own level (3.81.0) as the server asks it: below it the offer is not shown at all, rather than refused.
    val smith = game.hero?.crafts?.professions?.get(SMITHING)?.level ?: 1
    if (smith < rule.smithLevel) return null
    return rule.oreFor(template.level)?.let { it to rule.ore }
}

/** The profession whose level tempering asks. */
private const val SMITHING = "SMITHING"

/** Высота кнопок карточки (3.91.0): одна низкая строка вместо крупных плит. */
private val PillHeight = 36.dp
