package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.CombatEvent
import com.sperance.exileforge.core.campaign.FightReport
import com.sperance.exileforge.core.campaign.Outcome
import com.sperance.exileforge.core.campaign.RunHud
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.screens.hero.WearPreview
import com.sperance.exileforge.ui.theme.*
import java.util.Locale

/**
 * After the fight — «Поле боя», the owner's pick of five mockups (2.49.0). On top the scene: the
 * fallen monster's token in the half-dark with the outcome over it. Under it what the fight brought,
 * in engraved sections — «Снаряжение», a line per piece with its price, a tap opening its card;
 * «Сферы» as chips; «Награда», gold and experience — or, after a defeat, what the death cost and
 * what the run had gathered. The fight itself is a row of figures at the foot, and its log unfolds
 * from there. The spoils are the server's roll (1.30.0): the screen opens at once, says the loot is on its way,
 * and fills in as the answers arrive — offline, when the connection is back.
 */
@Composable internal fun ReportScreen(s: ForgeState, vm: ForgeViewModel, hud: RunHud, report: FightReport, onContinue: () -> Unit) {
    val won = report.outcome == Outcome.WIN
    var logOpen by remember { mutableStateOf(false) }
    var line by remember { mutableStateOf<Pair<CombatEvent, String>?>(null) }
    var looked by remember { mutableStateOf<ItemView?>(null) }
    Column(Modifier.fillMaxSize().background(Ink.copy(alpha = .94f)).statusBarsPadding().navigationBarsPadding().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FieldHead(report, won)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (won) Spoils(s, hud) { looked = it } else DeathPrice(s, hud)
            if (logOpen) Box(Modifier.fillMaxWidth().height(260.dp).background(Panel, RoundedCornerShape(8.dp))
                .border(1.dp, Bronze.copy(alpha = .4f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                Column {
                    LogShelves(s.logFilter, vm::logFilter)
                    FightLog(report.pack, Modifier.fillMaxSize(), s.logFilter) { event, name -> line = event to name }
                }
            }
        }
        FightFigures(report, logOpen) { logOpen = !logOpen }
        ForgeButton(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (won) Gold else LifeRed, contentColor = if (won) Ink else Parchment)) {
            Text(ui(if (won) "expedition.continue" else "expedition.back_to_camp"), style = MaterialTheme.typography.titleMedium)
        }
    }
    // A line of the log opened (3.37.0): its card over the report.
    line?.let { (event, name) -> CombatDetailSheet(s, event, name) { line = null } }
    // Compared and worn right here (3.24.0), as on the gear sheet.
    looked?.let { item -> LootSheet(s, vm, item, onDismiss = { looked = null }) }
}

/** The scene: the monster's round token in its rarity's ring, lit warm for a victory and red for a defeat, and the outcome in words. */
@Composable private fun FieldHead(report: FightReport, won: Boolean) {
    val monster = report.monster
    val time by rememberClock()
    val glow = if (won) Color(0xFF3B2A17) else Color(0xFF3B1717)
    Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(8.dp)).background(Brush.radialGradient(listOf(glow, Ink))),
        contentAlignment = Alignment.Center) {
        Icon(if (won) ForgeGlyphs.Swords else ForgeGlyphs.Skull, null, tint = if (won) Gold else LifeRed,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(20.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(68.dp).clip(CircleShape).background(Color.Black).border(3.dp, rarityTint(monster.rarity), CircleShape),
                contentAlignment = Alignment.TopCenter) {
                Canvas(Modifier.requiredSize(68.dp, 91.dp).offset(y = 12.dp)) {
                    Portraits.monster(this, monster.code, monster.form, rarityTint(monster.rarity), time)
                    if (won) drawRect(Ink.copy(alpha = .35f))
                }
            }
            Text(if (won) ui("expedition.report_slain", monsterTitle(monster.code)) else ui("expedition.report_fallen"),
                color = outcomeColour(report.outcome), style = MaterialTheme.typography.titleLarge)
            // A pack (since 2.54.0) says its size under the outcome.
            if (report.packSize > 1) MutedText(ui("expedition.report_pack", report.packSize), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** An engraved caption with a bronze rule running out of it, heading one part of the spoils. */
@Composable internal fun Caption(text: String, tone: Color = Gold) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text(text.uppercase(), color = tone, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Bronze, Color.Transparent))))
    }
}

@Composable private fun Chip(text: String, tone: Color = Parchment) {
    Text(text, color = tone, style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.background(Abyss, RoundedCornerShape(3.dp)).border(1.dp, PanelRaised, RoundedCornerShape(3.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
}

/** What the kill brought, by section, as the server's answers bring it (1.30.0); on its way, or its absence said plainly. */
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun Spoils(s: ForgeState, hud: RunHud, onItem: (ItemView) -> Unit) {
    val reward = hud.reward ?: return
    val index = s.index
    reward.recipe?.let { code ->
        Caption(ui("expedition.report_recipe"))
        Chip(index?.let { i -> i.recipe(code)?.let { recipeText(i, it) } } ?: displayName(code), Rune)
    }
    val gear = reward.equipment.mapNotNull { s.view(it) }
    if (gear.isNotEmpty()) {
        Caption(ui("expedition.report_gear"))
        // Every piece whole (3.2.0): base, every line with its tier and range, the roll quality and the price — no tap needed to judge it
        gear.forEach { LootCard(s, it, onItem) }
    }
    if (reward.items.isNotEmpty()) {
        Caption(ui("expedition.report_orbs"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            reward.items.forEach { (code, amount) -> Chip(ui("expedition.loot_stack", itemTitle(code), amount)) }
        }
    }
    Caption(ui("expedition.report_reward"))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip(ui("expedition.loot_gold", reward.gold), GoldBright)
        Chip(ui("expedition.loot_experience", number(reward.experience)), Rune)
    }
    if (hud.rewardAwaiting > 0) Receiving()
    else if (reward.items.isEmpty() && reward.equipment.isEmpty()) MutedText(ui("expedition.loot_nothing"))
}

/** A defeat: what the death cost by the rules' price — the server's answer stands — and what the run had gathered before it. */
@Composable private fun DeathPrice(s: ForgeState, hud: RunHud) {
    Caption(ui("expedition.report_death"), LifeRed)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(40.dp).border(1.dp, LifeRed, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Icon(ForgeGlyphs.Skull, null, tint = LifeRed, modifier = Modifier.size(22.dp))
        }
        val fall = hud.fall
        Column(Modifier.weight(1f)) {
            when {
                // A fall in a Vaal zone costs no experience; a fall the journal could not record is priced when the hero is read.
                fall == null -> if (hud.vaal) MutedText(ui("expedition.fall_free"), style = MaterialTheme.typography.bodyMedium)
                    else Text(ui("expedition.fall_failed"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                fall > 0 -> Text(ui("expedition.fall_lost", number(fall)), color = LifeRed, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                else -> MutedText(ui("expedition.fall_free"), style = MaterialTheme.typography.bodyMedium)
            }
            MutedText(ui(if (hud.vaal) "vaal.dead_hint" else "expedition.dead_hint"), style = MaterialTheme.typography.labelSmall)
        }
    }
    // A fall in the Abyss (2.82.0) burns its hoard, but for the atlas's share.
    hud.abyss?.takeIf { it.fallen }?.let { abyss ->
        Caption(ui("abyss.fallen"), AbyssGlow)
        val kept = abyss.hoard
        if (abyss.hoardAwaiting) Receiving()
        else if (kept == null || kept.items.isEmpty() && kept.equipment.isEmpty() && kept.experience <= 0) MutedText(ui("abyss.burned"))
        else { MutedText(ui("abyss.kept")); RewardLines(s, kept) }
    }
    Caption(ui("expedition.report_run"))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip(ui("expedition.report_kills", hud.kills))
        Chip(ui("expedition.loot_gold", hud.gold), GoldBright)
        Chip(ui("expedition.loot_experience", number(hud.experience)), Rune)
    }
    if (hud.awaiting > 0) Receiving()
}

/** The fight as a row of figures — dealt, taken, how long, criticals — and the way into its log. */
@Composable private fun FightFigures(report: FightReport, logOpen: Boolean, onLog: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Abyss, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Figure(ForgeGlyphs.Swords, report.dealt.toString())
        Figure(ForgeGlyphs.Helm, report.taken.toString())
        Figure(ForgeGlyphs.Portal, ui("expedition.log_time", String.format(Locale.ROOT, "%.1f", report.duration)))
        Figure(ForgeGlyphs.Sigil, report.crits.toString())
        Text(ui(if (logOpen) "expedition.log_less" else "expedition.log"), color = Rune, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clickable(role = Role.Button, onClick = onLog).padding(4.dp))
    }
}

@Composable private fun Figure(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = Muted, modifier = Modifier.size(13.dp))
        Text(value, color = Parchment, style = MaterialTheme.typography.labelMedium)
    }
}
