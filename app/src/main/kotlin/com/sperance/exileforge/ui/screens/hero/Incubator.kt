package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.IncubatorSlot
import com.sperance.exileforge.core.model.hero.IncubatorState
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeOutlinedButton
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.screens.crafts.duration
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay

/**
 * The incubator (server 1.67.0): an egg from the bag is laid into an open place, ripens by the server's clock — its rarity and
 * level settled the moment it was laid — and is taken out as a pet. Open places come from the hero's sheet; the ones a collar
 * could still open are drawn locked up to the ceiling.
 */
@Composable internal fun IncubatorPanel(s: ForgeState, vm: HeroViewModel) {
    val hero = s.hero ?: return
    val index = s.index ?: return
    val pets = hero.pets
    val incubator = pets.incubator
    val eggs = index.pets.eggs.values.distinct().filter { hero.count(it) > 0 }
    val now = rememberServerNow(incubator)
    val full = pets.pets.size >= pets.cap
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(ui("incubator.title", incubator.entries.count { it.busy }, incubator.slots), color = Gold, style = MaterialTheme.typography.labelLarge)
        val shown = maxOf(incubator.max, (incubator.entries.maxOfOrNull { it.slot } ?: -1) + 1)
        (0 until shown).forEach { place ->
            val slot = incubator.slot(place)
            when {
                slot == null || (!slot.open && !slot.busy) -> LockedSlot()
                !slot.busy -> EmptySlot(s, eggs, enabled = !s.busy) { egg -> vm.incubatePet(egg, place) }
                else -> BusySlot(s, slot, now, collectable = !s.busy && !full) { vm.collectPet(place) }
            }
        }
        if (full && incubator.ready > 0) MutedText(ui("incubator.full"))
    }
}

/** The server's clock, ticking each second on the local one, set by the moment this [incubator] was read off the server. */
@Composable private fun rememberServerNow(incubator: IncubatorState): Long {
    val offset = incubator.clockOffset
    val local by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    return local + offset
}

@Composable private fun SlotFrame(accent: Color, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier.fillMaxWidth().background(Abyss, shape).border(1.dp, accent.copy(alpha = .55f), shape).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable private fun LockedSlot() = SlotFrame(Bronze, Modifier.alpha(.55f)) {
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.size(20.dp)) }
    Column(Modifier.weight(1f)) {
        Text(ui("incubator.locked"), color = Muted, style = MaterialTheme.typography.labelMedium)
        MutedText(ui("incubator.locked_hint"), style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptySlot(s: ForgeState, eggs: List<String>, enabled: Boolean, onLay: (String) -> Unit) {
    val hero = s.hero ?: return
    var picking by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SlotFrame(Gold) {
            Box(Modifier.size(40.dp).border(1.dp, Gold.copy(alpha = .3f), RoundedCornerShape(6.dp)))
            Text(ui("incubator.empty"), color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            if (eggs.isEmpty()) {
                MutedText(ui("pets.no_eggs"), style = MaterialTheme.typography.labelSmall)
            } else {
                ForgeOutlinedButton(onClick = { if (eggs.size == 1) onLay(eggs.single()) else picking = !picking }, enabled = enabled) {
                    Text(ui("incubator.lay"))
                }
            }
        }
        if (picking && eggs.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                eggs.forEach { egg ->
                    val shape = RoundedCornerShape(6.dp)
                    Row(
                        Modifier.background(Panel, shape).border(1.dp, Bronze, shape).clickable(enabled = enabled) {
                            picking = false
                            onLay(egg)
                        }.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StackIcon(s, egg, 28)
                        Text(
                            ui("incubator.egg", itemTitle(egg), hero.count(egg)),
                            color = Parchment,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun BusySlot(s: ForgeState, slot: IncubatorSlot, now: Long, collectable: Boolean, onCollect: () -> Unit) {
    val left = (slot.readyAt - now).coerceAtLeast(0)
    val ready = slot.ready || left == 0L
    val tint = slot.rarity?.let { rarityColor(it.name) } ?: Parchment
    SlotFrame(if (ready) Vital else tint) {
        StackIcon(s, slot.egg, 40)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(itemTitle(slot.egg), color = tint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MutedText(
                listOfNotNull(slot.rarity?.let { ui("enum.rarity.${it.name}") }, ui("pets.level", slot.level)).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
            )
            if (!ready) {
                LinearProgressIndicator(progress = { slot.progress(now) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = tint, trackColor = Panel)
                Text(ui("incubator.left", duration(left)), color = Parchment, style = MaterialTheme.typography.labelSmall)
            } else if (!slot.open) {
                MutedText(ui("incubator.closed"), style = MaterialTheme.typography.labelSmall)
            }
        }
        if (ready) ForgeButton(onClick = onCollect, enabled = collectable) { Text(ui("incubator.collect")) }
    }
}
