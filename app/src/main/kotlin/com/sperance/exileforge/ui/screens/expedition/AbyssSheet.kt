package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.AbyssView
import com.sperance.exileforge.core.campaign.RunCommand
import com.sperance.exileforge.core.campaign.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.AbyssDepth
import com.sperance.exileforge.core.model.campaign.AbyssHoard
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeOutlinedButton
import com.sperance.exileforge.ui.components.ForgeTextButton
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * A crack of the Abyss (2.82.0, server 0.72.0): before the descent — how deep it leads, the first wave and
 * the hoard at its bottom; between depths — the hoard as it stands, the wave below and its leader, and the
 * choice: take the hoard and leave, or go deeper, where a fall burns it all but the atlas's share; after —
 * what the hoard brought.
 */
@Composable internal fun AbyssSheet(s: ForgeState, view: AbyssView, onCommand: (RunCommand) -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = .85f), Ink))), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp).glow(AbyssGlow, radius = 14.dp, shape = RoundedCornerShape(12.dp))
            .background(Panel.copy(alpha = .97f), RoundedCornerShape(12.dp)).border(1.dp, AbyssGlow.copy(alpha = .7f), RoundedCornerShape(12.dp))
            .padding(16.dp).heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(ForgeGlyphs.Rift, null, tint = AbyssGlow, modifier = Modifier.size(28.dp))
                Text(ui("abyss.title"), color = AbyssGlow, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(if (view.open) ui("abyss.progress", view.cleared, view.depth) else ui("abyss.depths", view.depth),
                    color = Parchment, style = MaterialTheme.typography.labelMedium)
            }
            val hoard = view.hoard
            when {
                hoard != null -> {
                    Text(ui(if (view.fallen) "abyss.fallen" else "abyss.taken"), color = if (view.fallen) LifeRed else GoldBright,
                        style = MaterialTheme.typography.titleMedium)
                    if (view.fallen && hoard.items.isEmpty() && hoard.equipment.isEmpty() && hoard.experience <= 0) MutedText(ui("abyss.burned"))
                    else RewardLines(s, hoard)
                }
                !view.open -> {
                    MutedText(ui("abyss.intro"))
                    view.depths.firstOrNull()?.let { Wave(1, it) }
                    view.depths.getOrNull(view.depth - 1)?.let { Hoard(ui("abyss.hoard_bottom", view.depth), it.hoard) }
                }
                else -> {
                    view.current?.let { Hoard(ui("abyss.hoard_now", view.cleared), it) }
                    val next = view.next
                    if (next != null) {
                        Wave(view.cleared + 1, next)
                        Hoard(ui("abyss.hoard_next", view.cleared + 1), next.hoard)
                    } else Text(ui("abyss.bottom"), color = AbyssGlow, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (hoard == null) Text(if (view.keep > 0) ui("abyss.keep", number(view.keep)) else ui("abyss.burns"), color = LifeRed.copy(alpha = .85f),
                style = MaterialTheme.typography.bodySmall)
            if (view.failed) Text(ui("abyss.failed"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
            if (view.pending) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AbyssGlow, trackColor = PanelRaised)
            Actions(view, onCommand)
        }
    }
}

/** The sheet's choice: open the crack or leave it; between depths take the hoard or go deeper; after, back to the map. */
@Composable private fun Actions(view: AbyssView, onCommand: (RunCommand) -> Unit) {
    val deep = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A1F66), contentColor = GoldBright)
    // Stepping away is for a crack not yet opened, or a descent the server did not answer: never with a hoard at stake.
    val away: @Composable () -> Unit = {
        ForgeTextButton(enabled = !view.pending, onClick = { onCommand(RunCommand.StepOff) }, modifier = Modifier.fillMaxWidth()) {
            Text(ui(if (view.open) "abyss.leave" else "abyss.later"), color = Muted)
        }
    }
    when {
        view.hoard != null -> ForgeButton(onClick = { onCommand(RunCommand.StepOff) }, modifier = Modifier.fillMaxWidth()) { Text(ui("abyss.leave")) }
        !view.open -> {
            ForgeButton(enabled = !view.pending, onClick = { onCommand(RunCommand.Descend) }, modifier = Modifier.fillMaxWidth(), colors = deep) {
                Icon(ForgeGlyphs.Rift, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(ui("abyss.descend"))
            }
            away()
        }
        view.fallen -> if (view.failed) away()
        else -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForgeOutlinedButton(enabled = !view.pending && view.cleared > 0, onClick = { onCommand(RunCommand.TakeHoard) }, modifier = Modifier.weight(1f)) {
                    Text(ui("abyss.take"))
                }
                if (view.next != null) ForgeButton(enabled = !view.pending, onClick = { onCommand(RunCommand.Descend) }, modifier = Modifier.weight(1f), colors = deep) {
                    Text(ui("abyss.deeper"))
                }
            }
            if (view.failed) away()
        }
    }
}

/** A depth's wave: how many, how many magic and rare, and the leader that rises with it. */
@Composable private fun Wave(depth: Int, floor: AbyssDepth) {
    Column(Modifier.fillMaxWidth().background(Abyss, RoundedCornerShape(8.dp)).border(1.dp, Bronze, RoundedCornerShape(8.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(ui("abyss.depth", depth, floor.level), color = GoldBright, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(ui("abyss.wave", span(floor.count), number(floor.magic), number(floor.rare)), color = Parchment, style = MaterialTheme.typography.bodySmall)
        floor.leader?.let { Text(ui("abyss.leader", monsterTitle(it.code)), color = rarityTint(com.sperance.exileforge.core.model.campaign.MonsterRarity.UNIQUE),
            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
    }
}

/** A hoard as it stands at a depth: the items of the Abyss and how many rare, the orbs, a unique's chance, the experience. */
@Composable private fun Hoard(title: String, hoard: AbyssHoard) {
    Column(Modifier.fillMaxWidth().background(AbyssGlow.copy(alpha = .06f), RoundedCornerShape(8.dp)).border(1.dp, AbyssGlow.copy(alpha = .35f), RoundedCornerShape(8.dp))
        .padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = AbyssGlow, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(ui("abyss.items", span(hoard.items), number(hoard.rare)), color = Parchment, style = MaterialTheme.typography.bodySmall)
        Text(ui("abyss.orbs", span(hoard.orbs)), color = Parchment, style = MaterialTheme.typography.bodySmall)
        Text(ui("abyss.unique", number(hoard.unique)), color = rarityTint(com.sperance.exileforge.core.model.campaign.MonsterRarity.UNIQUE),
            style = MaterialTheme.typography.bodySmall)
        if (hoard.experience > 0) Text(ui("abyss.experience", number(hoard.experience)), color = Rune, style = MaterialTheme.typography.bodySmall)
    }
}

/** A range as it reads: one number when low and high agree. */
private fun span(range: List<Int>): String {
    val low = range.getOrElse(0) { 0 }
    val high = range.getOrElse(1) { low }
    return if (low == high) "$low" else "$low–$high"
}
