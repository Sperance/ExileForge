package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.character.sellPrice
import com.sperance.exileforge.core.character.view
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/**
 * Места тайника строкой-подвалом окна автопродажи: значок тайника, «412 / 600 · предел 5000» и кнопка следующей пачки
 * «+50 · ◎ 17.9k»; нажатие раскрывает счёт «Есть · Спишется · Останется» ([GoldBillCells]) и кнопку покупки, не хватает золота -
 * кнопка погашена и называет недостачу. Пачек больше нет - кнопки нет. Места занимают только вещи: сферы и прочее в сумке
 * копятся без предела.
 */
@Composable fun StashPlacesBar(game: GameUi, vm: HeroViewModel, modifier: Modifier = Modifier) {
    val hero = game.hero ?: return
    val rules = game.index?.rules?.stash ?: return
    val capacity = rules.capacity(hero.info.stashSlots)
    val held = hero.items.size
    val full = held >= capacity
    val price = rules.price(hero.info.stashSlots)
    val pack = minOf(rules.slotStep, rules.maxSlots - capacity)
    val bill = game.goldBill(price) ?: return
    var open by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = PanelRaised)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Inventory2, null, tint = Gold, modifier = Modifier.size(20.dp))
            Text(
                ui("stash.places_chip", held, capacity),
                color = if (full) MaterialTheme.colorScheme.error else GoldBright,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { contentDescription = ui("stash.places", held, capacity) },
            )
            MutedText("· " + ui("stash.ceiling", rules.maxSlots), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            if (price > 0) {
                ForgeOutlinedButton(onClick = { open = !open }) {
                    Text(ui("stash.pack", pack))
                    Spacer(Modifier.width(6.dp))
                    GoldPrice(price)
                }
            }
        }
        if (open && price > 0) {
            GoldBillCells(bill)
            ForgeButton(onClick = {
                open = false
                vm.expandStash()
            }, Modifier.fillMaxWidth(), enabled = bill.affordable && !game.busy) { Text(bill.payLabel(ui("stash.expand", pack))) }
            MutedText(listOfNotNull(ui("stash.places_note"), ui("stash.full_hint", rules.overflowSlots).takeIf { full }).joinToString("\n"), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * What came when the stash was full: each piece taken in once a place frees, or sold.
 *
 * Запертая вещь ждёт в переполнении: её не продают ни руками, ни сервер.
 */
@Composable fun StashOverflow(game: GameUi, vm: HeroViewModel) {
    val hero = game.hero ?: return
    val rules = game.index?.rules?.stash ?: return
    if (hero.overflow.isEmpty()) return
    val full = hero.items.size >= rules.capacity(hero.info.stashSlots)
    val inspect = rememberInspect()
    ForgePanel(accent = Ember) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Engraved(ui("stash.overflow", hero.overflow.size, rules.overflowSlots), accent = Ember)
            Spacer(Modifier.weight(1f))
            ForgeTextButton(onClick = { vm.claimOverflow() }, enabled = !full && !game.busy) { Text(ui("stash.claim_all")) }
        }
        MutedText(ui("stash.overflow_hint"))
        hero.overflow.forEach { item ->
            game.view(item)?.let { piece ->
                ItemRow(piece, price = game.sellPrice(item), verdict = rememberGearVerdict(game, item), footer = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ForgeTextButton(onClick = { vm.claimOverflow(item.id) }, enabled = !full && !game.busy) { Text(ui("stash.claim")) }
                        ForgeTextButton(onClick = { vm.sellOverflow(item.id) }, enabled = !game.busy && !item.locked) { Text(ui("hero.sell_do")) }
                    }
                }) { inspect(Inspect.Copy(item)) }
            }
        }
    }
}
