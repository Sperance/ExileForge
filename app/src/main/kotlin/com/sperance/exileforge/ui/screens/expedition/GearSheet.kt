package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.itemSeals
import com.sperance.exileforge.core.sheet.sellPrice
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.screens.hero.EquipmentLedger
import com.sperance.exileforge.ui.theme.Vital
import org.koin.compose.viewmodel.koinViewModel

/**
 * The gear on the map: the body's ledger as the Equipment section draws it, over the walking map - только боевые места, без
 * инструментов ремёсел, и в наборе, что поход носит ([rememberRunGame]). В походе по карте касание места открывает выбор
 * ([RunSlotPicker]): что там надето, вещи, что в заходе можно надеть, с «Надеть» в карточке и приглушённые прочие вещи тайника. Вне
 * похода по карте (испытание) - только просмотр: надетая вещь открывает свою карточку, пустое место - ничего.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GearSheet(game: GameUi, onDismiss: () -> Unit) {
    val vm: ExpeditionViewModel = koinViewModel()
    val wearing = rememberRunWearing(vm)
    val dressed = rememberRunGame(game, vm)
    var worn by remember { mutableStateOf<String?>(null) }
    var place by remember { mutableStateOf<BodyPlace?>(null) }
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.85f).navigationBarsPadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("expedition.gear")) }
            item { MutedText(ui(if (wearing != null) "expedition.gear_hint_wear" else "expedition.gear_hint")) }
            item { EquipmentLedger(dressed, pickable = wearing != null, tools = false) { at, id -> if (wearing != null) place = at else worn = id } }
        }
    }
    val chosen = place
    if (wearing != null && chosen != null) RunSlotPicker(dressed, vm, chosen, wearing) { place = null }
    val instance = worn?.let { id -> dressed.hero?.item(id) } ?: return
    ForgeSheet(onDismissRequest = { worn = null }) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            game.view(instance)?.let { ItemCard(it, enabled = false, detailed = true, seals = game.itemSeals(instance)) }
        }
    }
}

/**
 * Выбор места [place] в походе по карте, по набору похода ([game] - срез с героем похода, [rememberRunGame]): надетое здесь - первым
 * (касание - карточка); ниже вещи, что в заходе можно надеть ([RunWearing.wearable]: набор входа и находки) и что сюда встают, - касание
 * открывает карточку с «Надеть» ([runWearAction]), лист закрывается вместе с ней; ниже прочие вещи тайника - приглушены, с замком, без
 * действия, причина плашкой над ними. Порядок - как в убежище: что не надеть - ниже, дороже - выше; вердикт - на это самое место.
 * Списки считаются по герою, месту и тому, что можно надеть, - не на каждую перерисовку.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RunSlotPicker(game: GameUi, vm: ExpeditionViewModel, place: BodyPlace, wearing: RunWearing, onDismiss: () -> Unit) {
    val hero = game.hero ?: return
    val inspect = rememberInspect()
    val (found, stash) = remember(hero, game.index, place, wearing.wearable) {
        hero.stash.filter { !it.socketed }.mapNotNull { game.view(it) }.filter { place.takes(it.slot) }
            .sortedWith(compareBy({ game.unmetFor(it.item).isNotEmpty() }, { -(game.sellPrice(it.item) ?: 0L) }))
            .partition { it.id in wearing.wearable }
    }
    val worn = remember(hero, game.index, place) { place.wornIn(hero.equipped)?.let { game.view(it) } }
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.8f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("hero.slot_pick", slotTitle(place.code, game.lang))) }
            item { MutedText(ui("expedition.wear_pick_hint")) }
            worn?.let { piece -> item(key = "worn") { WornLootRow(piece) } }
            item(key = "found") { Text(ui("expedition.wear_pick_found", found.size), color = Vital, style = MaterialTheme.typography.labelLarge) }
            items(found, key = { it.id }) { piece ->
                ItemRow(piece, unwearable = game.unmetFor(piece.item), price = game.sellPrice(piece.item), verdict = rememberGearVerdict(game, piece.item, place.place)) {
                    inspect(Inspect.Copy(piece.item, runWearAction(game, vm, piece.item, place.place, wearing, then = onDismiss)))
                }
            }
            if (stash.isNotEmpty()) {
                item(key = "stash") { InfoCard(ui("expedition.wear_pick_stash", stash.size), ui("expedition.wear_stash")) }
                items(stash, key = { it.id }) { piece ->
                    Box(Modifier.alpha(STASH_ALPHA)) { ItemRow(piece, price = game.sellPrice(piece.item), locked = true) { inspect(Inspect.Copy(piece.item)) } }
                }
            }
        }
    }
}

/** Насколько приглушены вещи тайника в выборе места похода: видны, но не к надеванию. */
private const val STASH_ALPHA = .5f
