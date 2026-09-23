package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.IconKey
import com.sperance.exileforge.core.display.icon
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.currency.CurrencyOrb
import com.sperance.exileforge.core.model.hero.CharacterItem
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.RaritySpine
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.spriteVector
import com.sperance.exileforge.ui.theme.*

/** A bag stack's name: the orb's own, or the tail of an id the catalogue does not name. */
fun bagTitle(s: ForgeState, itemId: String): String =
    s.world.orbs.firstOrNull { it.id == itemId }?.title(s.lang) ?: (ui("common.item") + " …${itemId.takeLast(6)}")

/**
 * The bag in the order the forge lists its orbs — the catalogue's — and whatever the catalogue
 * does not name after them, so one orb sits in the same place on both screens.
 */
fun bagStacks(s: ForgeState): List<CharacterItem> {
    val bag = s.play.hero?.bag.orEmpty()
    val order = s.world.orbs.withIndex().associate { (index, orb) -> orb.id to index }
    return bag.sortedBy { order[it.itemId] ?: Int.MAX_VALUE }
}

/**
 * One stack of the bag as a line: the orb, what it does, and how many there are.
 *
 * What it does is the dictionary's sentence, cut to two lines — the sheet behind the tap has room
 * for the whole of it.
 */
@Composable fun BagRow(s: ForgeState, stack: CharacterItem, onClick: () -> Unit) {
    val orb = s.world.orbs.firstOrNull { it.id == stack.itemId }
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(8.dp)).border(1.dp, PanelRaised, RoundedCornerShape(8.dp))
        .clickable(role = Role.Button, onClick = onClick).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        StackIcon(orb, 40)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(bagTitle(s, stack.itemId), color = Parchment, style = MaterialTheme.typography.titleSmall)
            orb?.details(s.lang)?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(stack.amount.toString(), color = GoldBright, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * An orb or an unnamed stack, drawn in a gold frame the way an item row frames its icon: the
 * server's sprite for the orb when the set has one, the bundled glyph otherwise.
 */
@Composable private fun StackIcon(orb: CurrencyItem?, size: Int) {
    val frame = RoundedCornerShape(6.dp)
    val art = orb?.let { icon(IconKey.item(it.code))?.let(::spriteVector) }
    Box(Modifier.size(size.dp).background(Gold.copy(alpha = .08f), frame).border(1.dp, Gold.copy(alpha = .55f), frame), contentAlignment = Alignment.Center) {
        Icon(art ?: if (orb != null) ForgeGlyphs.Orb else ForgeGlyphs.Gem, null, tint = if (orb != null) Gold else Muted,
            modifier = Modifier.size((size * .55f).dp))
    }
}

/**
 * A stack of the bag, opened: its name, how many there are, what it does, and where it goes next.
 *
 * The forge is offered for an orb that is applied to items — Regret is spent on the tree and the
 * server refuses it on an item (`CR_009`), so it has no way in. The auction takes any stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BagSheet(s: ForgeState, stack: CharacterItem, onDismiss: () -> Unit, onForge: (String) -> Unit, onAuction: (String) -> Unit) {
    val orb = s.world.orbs.firstOrNull { it.id == stack.itemId }
    val forgeable = orb != null && orb.orb != CurrencyOrb.ORB_OF_REGRET
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).navigationBarsPadding()) {
            RaritySpine(Gold, 4.dp)
            Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    StackIcon(orb, 56)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(bagTitle(s, stack.itemId), color = GoldBright, style = MaterialTheme.typography.titleLarge)
                        Text(ui("bag.owned", stack.amount), color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                orb?.details(s.lang)?.takeIf { it.isNotBlank() }?.let { Text(it, color = Parchment, style = MaterialTheme.typography.bodyMedium) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (forgeable) Button(enabled = !s.busy, onClick = { onForge(stack.itemId) }, modifier = Modifier.weight(1f)) {
                        Text(ui("bag.to_forge"))
                    }
                    OutlinedButton(enabled = !s.busy, onClick = { onAuction(stack.itemId) }, modifier = Modifier.weight(1f)) {
                        Text(ui("hero.action_auction"))
                    }
                }
            }
        }
    }
}
