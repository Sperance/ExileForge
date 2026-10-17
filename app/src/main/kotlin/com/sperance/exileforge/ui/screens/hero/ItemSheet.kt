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
import com.sperance.exileforge.core.app.ForgeSection
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.TAB_CRAFT
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.bodyPlaces
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.sellPrice
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.unmetNeeds
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.screens.auction.ListingSheet
import com.sperance.exileforge.ui.screens.craft.SmithyViewModel
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** What the action row opened on top of the sheet, if anything. */
private enum class ItemAction { AUCTION, SELL, WORN }

/**
 * One item of the stash: its card, and what can be done with it.
 *
 * The card scrolls; the actions do not — they sit in a row at the foot of the sheet, so the thing a
 * player came to do is never below the fold. Each one is a single tap: wearing and taking off at
 * once, an orb and the crafting bench in the forge, opened over this item, a listing through a small sheet of its own, and selling to
 * the merchant through the held confirmation, because that one cannot be taken back. Every rule behind them is the
 * server's, and it checks them again; the client adds the sheet up itself, so wearing
 * is also off for an item whose requirements it misses, and the card says what it would change.
 *
 * Вещь может уйти, пока лист открыт (продана, выставлена, перекатана) - лист закрывается. Запертая вещь недоступна торговцу и аукциону,
 * кузня с ней работает. Вторую копию уникалки не надеть - кнопка гаснет с причиной. Закалка кузнеца - раз на оружие или броню. Самоцвет без
 * свободного взятого гнезда гасит «В дерево». Кнопки «Реликвария» - одна низкая строка. Карта не надевается и в зону не ведёт. Надетую вещь
 * не выставить и не продать (`AU_010`, `CH_014`) - касание говорит это. Продажа окончательна: спрашивается по имени с суммой по правилу
 * торговца и осколками.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSheet(game: GameUi, model: HeroViewModel, itemId: String, onDismiss: () -> Unit) {
    val shell: ShellViewModel = koinViewModel()
    val smithy = koinViewModel<SmithyViewModel>().model
    val instance = game.hero?.item(itemId)
    val view = instance?.let { game.view(it) }
    if (instance == null || view == null) {
        LaunchedEffect(itemId) { onDismiss() }
        return
    }
    val name = view.title
    val can = !game.busy && game.session.signedIn && (game.ownsCharacter || game.isAdmin)
    val loose = !instance.equipped && !instance.socketed
    val price = game.sellPrice(instance)
    val reachable = game.unmetFor(instance.template).isEmpty()
    val locked = instance.locked
    val waiting = instance.id in game.link.waitingItems
    var open by remember(itemId) { mutableStateOf<ItemAction?>(null) }
    var replacing by remember(itemId) { mutableStateOf<BodyPlace?>(null) }
    val wear = rememberWearChoice(game, instance)
    val twin = remember(instance, game.hero, game.index, wear.target) { loose && game.index?.let { index -> game.hero?.uniqueWorn(index, instance, wear.target) } == true }
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
                        unmet = game.unmetNeeds(instance.template),
                    )
                }
                if (locked) item { Text(ui("item.locked_hint"), color = Muted, style = MaterialTheme.typography.bodySmall) }
                temperOffer(game, instance)?.let { (ore, need) ->
                    item {
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
                if (view.slot == Slot.JEWEL && !instance.socketed && game.index?.let { game.hero?.freeSocket(it) } == null) {
                    item { Text(ui("hero.to_tree_none"), color = LifeRed, style = MaterialTheme.typography.bodySmall) }
                }
                if (twin) item { Text(ui("hero.unique_worn"), color = LifeRed, style = MaterialTheme.typography.bodySmall) }
                game.hero?.inactive?.get(instance.id)?.let { reasons ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(ui("hero.inactive"), color = LifeRed, style = MaterialTheme.typography.labelLarge)
                            reasons.forEach { Text(requirementReason(it, game.lang), color = LifeRed, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
            var more by remember(itemId) { mutableStateOf(false) }
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

                    view.slot == Slot.MAP -> Unit

                    view.slot == Slot.JEWEL -> {
                        val socket = remember(game.hero, game.index) { game.index?.let { game.hero?.freeSocket(it) } }
                        PrimaryPill(ui("hero.to_tree"), can && socket != null, look) {
                            onDismiss()
                            socket?.let(shell::openTree)
                        }
                    }

                    else -> PrimaryPill(ui("hero.equip"), can && reachable && !twin, look) {
                        onDismiss()
                        model.equip(instance.id, wear.target)
                    }
                }
                QuietPill(can, modifier = if (view.slot == Slot.MAP) Modifier.weight(1f) else Modifier, onClick = {
                    onDismiss()
                    smithy.open(instance.id, ForgeSection.ORBS)
                    shell.tab(TAB_CRAFT)
                }) { Text(ui("nav.forge"), color = if (can) GoldBright else Muted, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                Box {
                    QuietPill(true, square = true, onClick = { more = true }) {
                        Icon(Icons.Outlined.MoreHoriz, ui("common.more"), tint = GoldBright, modifier = Modifier.size(18.dp))
                    }
                    ForgeMenu(expanded = more, onDismissRequest = { more = false }, containerColor = PanelRaised) {
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

        ItemAction.SELL -> ConfirmSheet(
            title = ui("hero.sell_q"),
            subtitle = name,
            danger = true,
            icon = { ItemIcon(view, rarityColor(view.rarity.name), Modifier.size(44.dp)) },
            ledger = listOfNotNull(
                LedgerLine(ui("confirm.give"), name, Tone.SPEND),
                LedgerLine(ui("confirm.gain"), price?.let { ui("merchant.gold_amount", it) } ?: ui("confirm.gold_by_server"), Tone.GAIN),
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
            title = {
                ChordOverWindow()
                Text(ui("hero.worn_title"), color = Gold)
            },
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

/**
 * The ore and its amount the smith asks to temper [item], or null when it cannot be: not gear, tempered, corrupted.
 *
 * Ниже уровня кузнеца предложение не показывается, а не отказывается.
 */
private fun temperOffer(game: GameUi, item: com.sperance.exileforge.rules.roll.ItemInstance): Pair<String, Long>? {
    val index = game.index ?: return null
    val template = index.template(item.template) ?: return null
    val rule = index.rules.brews.temper
    if (item.tempered || item.corrupted || !(template.slot.isWeapon || template.slot.isArmour)) return null
    val smith = game.hero?.crafts?.professions?.get(SMITHING)?.level ?: 1
    if (smith < rule.smithLevel) return null
    return rule.oreFor(template.level)?.let { it to rule.ore }
}

/** The profession whose level tempering asks. */
private const val SMITHING = "SMITHING"

/** Высота кнопок карточки: одна низкая строка вместо крупных плит. */
private val PillHeight = 36.dp
