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
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.sellPrice
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
 * from there.
 */
@Composable internal fun ReportScreen(s: ForgeState, hud: RunHud, report: FightReport, onContinue: () -> Unit) {
    val won = report.outcome == Outcome.WIN
    var logOpen by remember { mutableStateOf(false) }
    var looked by remember { mutableStateOf<EquipmentInstance?>(null) }
    Column(Modifier.fillMaxSize().background(Ink.copy(alpha = .94f)).statusBarsPadding().navigationBarsPadding().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FieldHead(report, won)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (won) Spoils(s, hud) { looked = it } else DeathPrice(hud)
            if (logOpen) Box(Modifier.fillMaxWidth().height(260.dp).background(Panel, RoundedCornerShape(8.dp))
                .border(1.dp, Bronze.copy(alpha = .4f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                FightLog(report.events, report.monster.code, Modifier.fillMaxSize())
            }
        }
        FightFigures(report, logOpen) { logOpen = !logOpen }
        Button(enabled = !hud.rewardPending && !hud.fallPending, onClick = onContinue, modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (won) Gold else LifeRed, contentColor = if (won) Ink else Parchment)) {
            Text(ui(if (won) "expedition.continue" else "expedition.back_to_camp"), style = MaterialTheme.typography.titleMedium)
        }
    }
    looked?.let { item -> LootCard(s, item) { looked = null } }
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
        }
    }
}

/** An engraved caption with a bronze rule running out of it, heading one part of the spoils. */
@Composable private fun Caption(text: String, tone: Color = Gold) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text(text.uppercase(), color = tone, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Bronze, Color.Transparent))))
    }
}

@Composable private fun Chip(text: String, tone: Color = Parchment) {
    Text(text, color = tone, style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.background(Abyss, RoundedCornerShape(3.dp)).border(1.dp, PanelRaised, RoundedCornerShape(3.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
}

/** What the kill brought, by section; the server's roll awaited, or its absence said plainly. */
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun Spoils(s: ForgeState, hud: RunHud, onItem: (EquipmentInstance) -> Unit) {
    val reward = hud.reward
    when {
        hud.rewardPending -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Gold, strokeWidth = 2.dp)
            Text(ui("expedition.loot_pending"), color = Muted)
        }
        hud.rewardFailed -> Text(ui("expedition.loot_failed"), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
        reward != null -> {
            reward.recipeFound?.let { recipe ->
                Caption(ui("expedition.report_recipe"))
                Chip(recipeText(recipe, s.world.definitions), Rune)
            }
            if (reward.equipment.isNotEmpty()) {
                Caption(ui("expedition.report_gear"))
                reward.equipment.forEach { SpoilLine(s, it) { onItem(it) } }
            }
            if (reward.items.isNotEmpty()) {
                Caption(ui("expedition.report_orbs"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    reward.items.forEach { stack ->
                        val orb = s.world.orbs.firstOrNull { it.id == stack.itemId }
                        Chip(ui("expedition.loot_stack", orb?.title(s.lang) ?: ui("common.item"), stack.amount))
                    }
                }
            }
            Caption(ui("expedition.report_reward"))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(ui("expedition.loot_gold", reward.gold), GoldBright)
                Chip(ui("expedition.loot_experience", number(reward.experience)), Rune)
            }
            if (reward.items.isEmpty() && reward.equipment.isEmpty()) Text(ui("expedition.loot_nothing"), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** One piece of the spoils: its icon in the rarity's frame, its name in that colour, what it is, and what the merchant pays. A tap opens its card. */
@Composable private fun SpoilLine(s: ForgeState, instance: EquipmentInstance, onClick: () -> Unit) {
    val document = inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId])
    val color = rarityColor(instance.rarity)
    val frame = RoundedCornerShape(6.dp)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(40.dp).background(color.copy(alpha = .08f), frame).border(1.dp, color, frame), contentAlignment = Alignment.Center) {
            ItemIcon(document, color, Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(document.text("name"), color = color, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(document.text("slot").takeIf { it.isNotBlank() }?.let { slotTitle(it, s.lang) },
                document.text("itemLevel").takeIf { it.isNotBlank() }?.let { ui("row.level", it) }).joinToString(" · "),
                color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        s.sellPrice(instance)?.let { GoldPrice(it) }
    }
}

/** A defeat: what the death cost, the server's word awaited, and what the run had gathered before it. */
@Composable private fun DeathPrice(hud: RunHud) {
    Caption(ui("expedition.report_death"), LifeRed)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(40.dp).border(1.dp, LifeRed, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Icon(ForgeGlyphs.Skull, null, tint = LifeRed, modifier = Modifier.size(22.dp))
        }
        val fall = hud.fall
        Column(Modifier.weight(1f)) {
            when {
                hud.fallPending -> Text(ui("expedition.fall_pending"), color = Muted, style = MaterialTheme.typography.bodyMedium)
                fall == null -> Text(ui("expedition.fall_failed"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                fall.lost > 0 -> Text(ui("expedition.fall_lost", number(fall.lost)), color = LifeRed, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                else -> Text(ui("expedition.fall_free"), color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
            Text(ui("expedition.dead_hint"), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
    Caption(ui("expedition.report_run"))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip(ui("expedition.report_kills", hud.kills))
        Chip(ui("expedition.loot_gold", hud.gold), GoldBright)
        Chip(ui("expedition.loot_experience", number(hud.experience)), Rune)
    }
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

/** A piece of the spoils, opened: its full card with the merchant's price and what wearing it would change. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun LootCard(s: ForgeState, instance: EquipmentInstance, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemCard(inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId]), enabled = false, detailed = true,
                definitions = s.world.definitions, price = s.sellPrice(instance))
            WearPreview(s, instance)
        }
    }
}
