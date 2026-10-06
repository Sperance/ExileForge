package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.campaign.LootEntry
import com.sperance.exileforge.core.character.GearVerdict
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.LootPresence
import com.sperance.exileforge.presentation.state.lootPresence
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.upgrades
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.hero.EquipmentLedger
import com.sperance.exileforge.ui.screens.hero.SlotPicker
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel

/**
 * «Новый лут» (2.45.0): the gear this run brought, maps aside. Each piece is its view over the content (3.0.0): the hero's
 * copy of it when the hero holds it, the roll's otherwise. Положение - общее правило лута (3.90.0, [lootPresence]): проданной
 * вещи нет, надетая остаётся с меткой «Надето»; до чтения героя после её выпадения вещь ждёт, а не пропадает.
 */
fun newLoot(game: GameUi, runLoot: List<LootEntry>): List<Pair<ItemView, LootPresence>> = runLoot.mapNotNull { entry ->
    val presence = game.lootPresence(entry.item.id, arriving = game.holding.seenAt < entry.at).takeIf { it.shown } ?: return@mapNotNull null
    game.view(game.hero?.item(entry.item.id) ?: entry.item)?.takeIf { it.slot != Slot.MAP }?.let { it to presence }
}

/**
 * The gear on the map (since 2.40.0): the body's ledger as the Equipment section draws it, over the
 * walking map. An empty place opens the stash narrowed to what fits it; a worn one shows its card
 * with «Снять» and «Заменить». The server decides, the hero is re-read, and the run takes the new
 * sheet before its next fight — life and mana keep their share.
 * Вкладка «Подходящее» (3.90.0) между надетым и лутом заменила отдельный лист смены снаряжения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GearSheet(game: GameUi, vm: ExpeditionViewModel, onDismiss: () -> Unit) {
    var place by remember { mutableStateOf<BodyPlace?>(null) }
    var worn by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(GearTab.WORN) }
    var looked by remember { mutableStateOf<String?>(null) }
    val expedition by vm.state.collectAsStateWithLifecycle()
    val loot = newLoot(game, expedition.runLoot)
    val wear = rememberLootWear(game, vm)
    // Вердикты тайника складываются вне главного потока; до ответа вкладка пуста, а не замерла.
    val fits by produceState<List<Pair<ItemInstance, GearVerdict>>?>(null, game.hero, game.index) {
        value = withContext(Dispatchers.Default) { game.upgrades() }
    }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.85f).navigationBarsPadding()) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    GearTab.WORN -> {
                        item { Engraved(ui("expedition.gear")) }
                        item { MutedText(ui("expedition.gear_hint")) }
                        item {
                            EquipmentLedger(game) { p, w ->
                                place = p
                                worn = w
                            }
                        }
                    }

                    GearTab.FITTING -> fitting(game, fits, wear)

                    GearTab.LOOT -> {
                        item { Engraved(ui("expedition.loot_tab")) }
                        if (loot.isEmpty()) item { MutedText(ui("expedition.loot_empty")) }
                        items(loot, key = { it.first.id }) { (item, presence) ->
                            if (presence.worn) {
                                WornLootRow(item)
                            } else {
                                ItemRow(item, enabled = !game.busy, unwearable = game.unmetFor(item.code), price = game.sellPrice(item.item), verdict = rememberGearVerdict(game, item.item)) { looked = item.id }
                            }
                        }
                    }
                }
            }
            // The tabs sit at the foot (2.45.0): the body's ledger, what the stash offers to wear (3.90.0), and what this run brought.
            TabRow(selectedTabIndex = tab.ordinal, containerColor = Abyss, contentColor = Gold) {
                GearTab.entries.forEach { one ->
                    Tab(selected = tab == one, onClick = { tab = one }, text = { Text(one.title(loot.size)) })
                }
            }
        }
    }
    loot.firstOrNull { it.first.id == looked }?.first?.let { item ->
        LootSheet(game, vm, item, onDismiss = { looked = null })
    }
    val chosen = place
    val instance = worn?.let { id -> game.hero?.item(id) }
    if (chosen != null && instance != null) {
        ForgeSheet(onDismissRequest = {
            place = null
            worn = null
        }) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                game.view(instance)?.let { ItemCard(it, enabled = false, detailed = true, price = game.sellPrice(instance)) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ForgeOutlinedButton(enabled = !game.busy, onClick = {
                        vm.unequip(instance.id)
                        place = null
                        worn = null
                    }, modifier = Modifier.weight(1f)) {
                        Text(ui("hero.unequip"))
                    }
                    ForgeButton(enabled = !game.busy, onClick = { worn = null }, modifier = Modifier.weight(1f)) { Text(ui("expedition.gear_replace")) }
                }
            }
        }
    } else if (chosen != null && worn == null) {
        SlotPicker(game, chosen, onDismiss = { place = null }, onEquip = { id -> vm.equip(id, chosen.place) })
    }
}

/** Вкладки листа снаряжения (3.90.0): надетое, подходящее из тайника и лут забега - со счётчиком. */
private enum class GearTab {
    WORN,
    FITTING,
    LOOT,
    ;

    fun title(loot: Int): String = when (this) {
        WORN -> ui("expedition.gear")
        FITTING -> ui("expedition.fitting_tab")
        LOOT -> ui("expedition.loot_tab_count", loot)
    }
}

/**
 * «Подходящее» (3.90.0): всё из тайника, что герой может надеть, улучшения первыми по сумме изменений урона и защиты, -
 * плитка вещи в облике её редкости, итог «Урон ▲ · Защита ▼» и «Надеть» одним нажатием. Сменённая вещь действует со
 * следующего боя; [fits] null - вердикты ещё считаются.
 */
private fun LazyListScope.fitting(game: GameUi, fits: List<Pair<ItemInstance, GearVerdict>>?, wear: LootWear) {
    item { Engraved(ui("expedition.fitting_tab")) }
    val list = fits ?: return
    if (list.isEmpty()) item { MutedText(ui("expedition.fitting_empty")) }
    items(list, key = { it.first.id }) { (instance, verdict) ->
        game.view(instance)?.let { piece ->
            ItemTile(piece) {
                GearVerdictSummary(verdict)
                ForgeButton(enabled = wear.ready(piece), onClick = { wear.wear(piece) }, modifier = Modifier.fillMaxWidth()) { Text(wear.label(piece)) }
            }
        }
    }
}
