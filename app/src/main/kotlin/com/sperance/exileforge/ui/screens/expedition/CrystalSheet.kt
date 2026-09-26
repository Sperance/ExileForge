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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.CrystalView
import com.sperance.exileforge.core.campaign.RunCommand
import com.sperance.exileforge.core.campaign.monsterTitle
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.currency.CurrencyOrb
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.theme.*

/**
 * A crystal of essences (2.78.0, the owner's mockup A): what it holds, who guards it — the zone's monster
 * standing up rare with the modifier of every essence inside — and the choice. «Освободить» takes the
 * guardian on; a Vaal orb passes over the crystal once — every essence a step higher, one of them
 * special, or a stronger guardian — and stepping away leaves it standing for later.
 */
@Composable internal fun CrystalSheet(s: ForgeState, view: CrystalView, onCommand: (RunCommand) -> Unit) {
    val book = s.world.essenceBook
    val vaalOrbs = s.orbOf(CurrencyOrb.VAAL_ORB)?.let { s.bagAmount(it.id) } ?: 0L
    val kinds = view.essences.mapNotNull(book::essence).map { it.kind.code }.distinct()
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = .85f), Ink))), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp).background(Panel.copy(alpha = .97f), RoundedCornerShape(12.dp))
            .border(1.dp, CrystalViolet.copy(alpha = .7f), RoundedCornerShape(12.dp)).padding(16.dp).heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(ForgeGlyphs.Shard, null, tint = CrystalViolet, modifier = Modifier.size(28.dp))
                Text(ui("crystal.title"), color = CrystalViolet, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (view.vaal) Text(ui("crystal.corrupted"), color = LifeRed, style = MaterialTheme.typography.labelMedium)
            }
            view.essences.forEach { code ->
                val special = book.essence(code)?.special == true
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).background(if (special) GoldBright else CrystalViolet, RoundedCornerShape(2.dp)))
                    Text(locOr(LocaleKey.itemName(code), displayName(code)), color = if (special) GoldBright else Parchment, style = MaterialTheme.typography.bodyMedium)
                }
            }
            // The guardian: the zone's monster, rare, named by what its essences make of it.
            val traits = kinds.map { "«" + locOr("essence.$it.monster", displayName(it)) + "»" }
            Text(ui("crystal.guardian", monsterTitle(view.guardian), traits.joinToString(", ")), color = Rune, style = MaterialTheme.typography.bodySmall)
            if (view.stronger) Text(ui("crystal.stronger", number(book.crystals.stronger)), color = LifeRed, style = MaterialTheme.typography.bodySmall)
            view.outcome?.let { Text(ui("crystal.outcome.$it"), color = GoldBright, style = MaterialTheme.typography.bodyMedium) }
            if (view.failed) Text(ui("crystal.failed"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
            if (!view.vaal) MutedText(ui("crystal.vaal_hint"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(enabled = !view.vaal && !view.pending && vaalOrbs > 0, onClick = { onCommand(RunCommand.VaalCrystal) }, modifier = Modifier.weight(1f)) {
                    OrbGlyph(CurrencyOrb.VAAL_ORB, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(ui("crystal.vaal", vaalOrbs))
                }
                Button(enabled = !view.pending, onClick = { onCommand(RunCommand.Release) }, modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = GoldBright)) { Text(ui("crystal.release")) }
            }
            if (view.pending) LinearProgressIndicator(Modifier.fillMaxWidth(), color = CrystalViolet, trackColor = PanelRaised)
            TextButton(enabled = !view.pending, onClick = { onCommand(RunCommand.StepOff) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(ui("crystal.later"), color = Muted, textAlign = TextAlign.Center)
            }
        }
    }
}
