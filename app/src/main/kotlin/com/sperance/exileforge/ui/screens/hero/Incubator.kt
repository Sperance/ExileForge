package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeDialog
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
@Composable internal fun IncubatorPanel(game: GameUi, vm: HeroViewModel, titled: Boolean = true) {
    val hero = game.hero ?: return
    val index = game.index ?: return
    val pets = hero.pets
    val incubator = pets.incubator
    val eggs = index.pets.eggs.values.distinct().filter { hero.count(it) > 0 }
    val now = rememberServerNow(incubator)
    val full = pets.pets.size >= pets.cap
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (titled) Text(ui("incubator.title", incubator.entries.count { it.busy }, incubator.slots), color = Gold, style = MaterialTheme.typography.labelLarge)
        val shown = maxOf(incubator.max, (incubator.entries.maxOfOrNull { it.slot } ?: -1) + 1)
        (0 until shown).forEach { place ->
            val slot = incubator.slot(place)
            when {
                slot == null || (!slot.open && !slot.busy) -> LockedSlot()
                !slot.busy -> EmptySlot(game, eggs, enabled = !game.busy) { egg -> vm.incubatePet(egg, place) }
                else -> BusySlot(game, slot, now, collectable = !game.busy && !full) { vm.collectPet(place) }
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

@Composable
private fun EmptySlot(game: GameUi, eggs: List<String>, enabled: Boolean, onLay: (String) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    SlotFrame(Gold) {
        Box(Modifier.size(40.dp).border(1.dp, Gold.copy(alpha = .3f), RoundedCornerShape(6.dp)))
        Text(ui("incubator.empty"), color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        if (eggs.isEmpty()) {
            MutedText(ui("pets.no_eggs"), style = MaterialTheme.typography.labelSmall)
        } else {
            ForgeOutlinedButton(onClick = { picking = true }, enabled = enabled) { Text(ui("incubator.lay")) }
        }
    }
    // Every egg at hand in one window (3.81.0), even a single kind: the player sees what goes in before it does.
    if (picking) {
        EggPicker(game, eggs, onDismiss = { picking = false }) { egg ->
            picking = false
            onLay(egg)
        }
    }
}

@Composable private fun EggPicker(game: GameUi, eggs: List<String>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val hero = game.hero ?: return
    ForgeDialog(ui("incubator.pick_title"), onDismiss) {
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            eggs.forEach { egg ->
                val shape = RoundedCornerShape(6.dp)
                Row(
                    Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, Bronze, shape).clickable { onPick(egg) }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StackIcon(game, egg, 36)
                    Column(Modifier.weight(1f)) {
                        Text(itemTitle(egg), color = Parchment, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        MutedText(ui("incubator.egg_count", hero.count(egg)), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable private fun BusySlot(game: GameUi, slot: IncubatorSlot, now: Long, collectable: Boolean, onCollect: () -> Unit) {
    val left = (slot.readyAt - now).coerceAtLeast(0)
    val ready = slot.ready || left == 0L
    val tint = slot.rarity?.let { rarityColor(it.name) } ?: Parchment
    SlotFrame(if (ready) Vital else tint) {
        StackIcon(game, slot.egg, 40)
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
