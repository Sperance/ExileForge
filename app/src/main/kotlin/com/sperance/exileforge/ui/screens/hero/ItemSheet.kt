package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.sperance.exileforge.core.sheet.itemSeals
import com.sperance.exileforge.core.sheet.sellPrice
import com.sperance.exileforge.core.sheet.unmetNeeds
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.craft.SmithyViewModel
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

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
    val can = !game.busy && game.session.signedIn && (game.ownsCharacter || game.isAdmin)
    val loose = !instance.equipped && !instance.socketed
    val price = game.sellPrice(instance)
    val locked = instance.locked
    val waiting = instance.id in game.link.waitingItems
    val deals = rememberItemDeals()
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
                        seals = game.itemSeals(instance),
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
                            ForgeButton(state = ButtonState.of(can && (game.bagAmount(ore) ?: 0L) >= need), onClick = { model.temper(instance.id) }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) {
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
                val offer = wearOffer(game, model, shell, instance, can, twin, onDismiss)
                when {
                    instance.equipped && wornPlace != null -> ForgeButton(onClick = { replacing = wornPlace }, modifier = Modifier.weight(1f), size = ButtonSize.Compact, state = ButtonState.of(can), accent = look.gold.takeIf { look.legend != null }) {
                        Text(ui("hero.replace"), maxLines = 1)
                    }

                    offer != null -> ForgeButton(onClick = { offer.run(wear.target) }, modifier = Modifier.weight(1f), size = ButtonSize.Compact, state = ButtonState.of(offer.enabled), accent = look.gold.takeIf { look.legend != null }) {
                        Text(offer.label, maxLines = 1)
                    }
                }
                ForgeButton(onClick = {
                    onDismiss()
                    smithy.open(instance.id, ForgeSection.ORBS)
                    shell.tab(TAB_CRAFT)
                }, modifier = if (view.slot == Slot.MAP) Modifier.weight(1f) else Modifier, kind = ButtonKind.Secondary, size = ButtonSize.Compact, state = ButtonState.of(can)) { Text(ui("nav.forge"), maxLines = 1) }
                Box {
                    ForgeIconButton(Icons.Outlined.MoreHoriz, ui("common.more"), onClick = { more = true }, kind = ButtonKind.Secondary, size = ButtonSize.Compact)
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
                            deals.auction(instance)
                        }
                        MenuLine(ForgeGlyphs.Coins, ui("hero.action_sell"), can && !locked, LifeRed) {
                            more = false
                            deals.sell(instance)
                        }
                    }
                }
            }
        }
    }
    ItemDealSheets(game, model, deals, onDismiss)
    replacing?.let { place ->
        SlotPicker(game, place, onDismiss = { replacing = null }, onEquip = { id ->
            onDismiss()
            model.equip(id, place.place)
        })
    }
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
