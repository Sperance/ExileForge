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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.RunHud
import com.sperance.exileforge.core.campaign.mapTitle
import com.sperance.exileforge.core.campaign.monsterTitle
import com.sperance.exileforge.core.display.modifierText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.VaalZone
import com.sperance.exileforge.core.model.modifier.Modifier as Affix
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The Vaal zone's own reds (2.65.0, the owner's mockup I «Кровавый алтарь»). */
private object Altar {
    val deep = Color(0xFF070203)
    val night = Color(0xFF110305)
    val glow = Color(0xFF3A0808)
    val vein = Color(0xFFFF4A3A)
    val title = Color(0xFFFF8A78)
    val figure = Color(0xFFFFB4A6)
    val line = Color(0xFFB01E1E)
    val muted = Color(0xFFC9A9A2)
    val deed = Color(0xFFB3261E)
}

/**
 * The gate before a Vaal zone (2.65.0): what the server rolled for it — every modifier, and what
 * the zone adds to the loot for bearing them — and the choice. «Войти» closes the portal behind the
 * hero, «Отказаться» closes it for good; the zone is never re-rolled by walking away and back.
 */
@Composable fun VaalGate(s: ForgeState, hud: RunHud, guardian: String?, onEnter: () -> Unit, onRefuse: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Altar.glow, Altar.night, Altar.deep), radius = 1600f)), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ui("vaal.eyebrow"), color = Altar.vein, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(mapTitle(hud.mapCode), color = Altar.title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall.copy(shadow = Shadow(Altar.vein.copy(alpha = .55f), blurRadius = 18f)))
            val zone = hud.gate
            zone?.let { Text(guardian?.let { g -> ui("vaal.level_guardian", it.level, monsterTitle(g)) } ?: ui("vaal.level", it.level),
                color = Parchment.copy(alpha = .75f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            when {
                zone != null -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    zone.modifiers.forEach { ModLine(modifierText(it.document(), s.world.definitions)) }
                }
                hud.gateFailed -> Text(ui("vaal.failed"), color = LifeRed, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                else -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Altar.vein) }
            }
            zone?.let { Reward(it) }
            Text(ui("vaal.warning"), color = Altar.muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = if (zone != null) onRefuse else onBack, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Altar.muted)) { Text(ui(if (zone != null) "vaal.refuse" else "common.close")) }
                Button(onClick = onEnter, enabled = zone != null, modifier = Modifier.weight(1.4f),
                    colors = ButtonDefaults.buttonColors(containerColor = Altar.deed, contentColor = Color.White)) { Text(ui("vaal.enter")) }
            }
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(ui("vaal.later"), color = Altar.muted) }
        }
    }
}

/** One modifier: a scarlet rhombus and its sentence on a dark red strip, a vein down its edge. */
@Composable private fun ModLine(text: String) {
    val shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(Altar.line.copy(alpha = .22f), shape),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(Altar.line))
        Box(Modifier.size(7.dp).rotate(45f).background(Altar.vein))
        Text(text, color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(vertical = 7.dp, horizontal = 2.dp))
    }
}

/** What bearing the modifiers pays: quantity and rarity of every drop in the zone and its guardian's. */
@Composable private fun Reward(zone: VaalZone) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Figure(ui("expedition.launch_quantity"), zone.quantity, Modifier.weight(1f))
        Figure(ui("expedition.launch_rarity"), zone.rarity, Modifier.weight(1f))
        Figure(ui("expedition.launch_experience"), zone.experience, Modifier.weight(1f))
    }
}

@Composable private fun Figure(label: String, value: Double, modifier: Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Column(modifier.background(Altar.vein.copy(alpha = .08f), shape).border(1.dp, Altar.vein.copy(alpha = .3f), shape).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Text(label.uppercase(), color = Altar.muted, style = MaterialTheme.typography.labelSmall)
        Text(ui("vaal.percent", number(value)), color = Altar.figure, style = MaterialTheme.typography.titleMedium)
    }
}

/** A rolled modifier as the item presentation reads one. */
private fun Affix.document() = JsonObject(mapOf("modifierCode" to JsonPrimitive(modifierCode), "values" to JsonArray(values.map(::JsonPrimitive)),
    "tier" to JsonPrimitive(tier)))
