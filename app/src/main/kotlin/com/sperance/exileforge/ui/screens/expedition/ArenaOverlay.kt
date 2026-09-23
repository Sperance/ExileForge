package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.presentation.features.key
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.scene.FightLayout
import com.sperance.exileforge.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

internal fun rarityTint(rarity: MonsterRarity) = when (rarity) {
    MonsterRarity.NORMAL -> Parchment
    MonsterRarity.MAGIC -> Color(0xFF8888FF)
    MonsterRarity.RARE -> Color(0xFFFFFF77)
}

/**
 * The fight — «Арена» (2.26.0): the two sides named on top, each with life, shield and a bar that
 * fills toward its next swing at its own attack speed; the fighters in the middle, drawn by the
 * scene; the log under them, newest line first, which unfolds over the scene on demand.
 *
 * Everything here is read off [FightHud]; the numbers flying off the fighters are placed over the
 * spots [FightLayout] gives the scene, so a hit rises from the one who took it.
 */
@Composable internal fun ArenaOverlay(s: ForgeState, hud: RunHud, fight: FightHud, level: Int, onSpeed: () -> Unit) {
    var logOpen by rememberSaveable { mutableStateOf(false) }
    val monster = fight.monster
    val hero = s.play.hero?.character
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight
        val density = LocalDensity.current
        fight.hits.forEach { hit ->
            val column = if (hit.target == Side.HERO) FightLayout.HERO_X else FightLayout.MONSTER_X
            val rise = (hit.age / ExpeditionRun.HIT_LIFETIME).toFloat()
            val x = with(density) { (width * column).toPx() } + ((hit.id % 3) - 1) * 40f
            val y = with(density) { (height * (FightLayout.GROUND_Y - .3f)).toPx() } - rise * 120f
            Text(hitText(hit), color = hitColour(hit).copy(alpha = (1 - rise).coerceIn(0f, 1f)),
                fontSize = if (hit.kind == HitKind.CRIT) 30.sp else 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.offset { IntOffset((x - 40f).roundToInt(), y.roundToInt()) }.width(80.dp))
        }
        fight.outcome?.let {
            Text(ui("expedition.outcome_${it.name.lowercase()}"), color = outcomeColour(it), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Nameplate(Modifier.weight(1f), hero?.name.orEmpty(), ui("expedition.hero_line", s.heroClass?.title.orEmpty(), hero?.level ?: 1),
                    GoldBright, fight.heroLife, hud.heroMaxLife, fight.heroShield, hud.heroMaxShield, fight.heroSwing, alignEnd = false)
                Nameplate(Modifier.weight(1f), monsterTitle(monster.code), ui("expedition.monster_line", ui(monster.rarity.key()), level),
                    rarityTint(monster.rarity), fight.monsterLife, fight.monsterMaxLife, fight.monsterShield, fight.monsterMaxShield, fight.monsterSwing, alignEnd = true)
            }
            // What the monster rolled sits under its own name: a tier's strength is read before the fight.
            Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.End) {
                monster.modifiers.forEach {
                    Text(monsterModifierText(it), color = Rune, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                }
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().animateContentSize().background(Panel.copy(alpha = .92f), RoundedCornerShape(10.dp))
                .border(1.dp, Bronze.copy(alpha = .5f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp)
                .height(if (logOpen) height * .5f else 116.dp)) {
                FightLog(fight.events, monster.code)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSpeed, modifier = Modifier.weight(1f)) { Text(ui("expedition.speed", fight.speed)) }
                OutlinedButton(onClick = { logOpen = !logOpen }, modifier = Modifier.weight(1f)) {
                    Text(ui(if (logOpen) "expedition.log_less" else "expedition.log"))
                }
            }
        }
    }
}

/** One side of the fight: its name, what it is, life with shield over it, and the swing bar. */
@Composable private fun Nameplate(modifier: Modifier, name: String, line: String, tint: Color, life: Int, maxLife: Int,
    shield: Int, maxShield: Int, swing: Float, alignEnd: Boolean) {
    val align = if (alignEnd) Alignment.End else Alignment.Start
    val shape = CutCornerShape(3.dp)
    val share by animateFloatAsState(if (maxLife > 0) life / maxLife.toFloat() else 0f, label = "life")
    Column(modifier, horizontalAlignment = align, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(name, color = tint, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(line, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.fillMaxWidth().height(16.dp).background(Color(0xCC0A0D12), shape).border(1.dp, LifeRed.copy(alpha = .8f), shape)) {
            Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(LifeRed, LifeRed.copy(alpha = .55f))), shape))
            if (maxShield > 0) Box(Modifier.fillMaxWidth((shield / maxShield.toFloat()).coerceIn(0f, 1f)).height(4.dp).background(ShieldCyan.copy(alpha = .85f)))
            Text(if (maxShield > 0) "$life / $maxLife · $shield" else "$life / $maxLife", color = Parchment, fontSize = 10.sp,
                modifier = Modifier.align(Alignment.Center))
        }
        // The swing bar: full the instant the next blow lands.
        Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0x14FFFFFF), RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(swing.coerceIn(0f, 1f)).fillMaxHeight().background(Gold, RoundedCornerShape(2.dp)))
        }
    }
}

/** The swings so far, newest first: when, who, and what came of it, coloured by what it was. */
@Composable internal fun FightLog(events: List<CombatEvent>, monsterCode: String, modifier: Modifier = Modifier) {
    val monster = monsterTitle(monsterCode)
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        items(events) { event ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui("expedition.log_time", String.format(Locale.ROOT, "%.1f", event.time)), color = Muted,
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
                Text(logLine(event, monster), color = logColour(event), style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (event.kind == HitKind.CRIT) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

private fun logLine(event: CombatEvent, monster: String): String {
    val damage = event.damage.roundToInt()
    val hero = event.attacker == Side.HERO
    return when (event.kind) {
        HitKind.HIT -> if (hero) ui("expedition.log_you_hit", damage) else ui("expedition.log_they_hit", monster, damage)
        HitKind.CRIT -> if (hero) ui("expedition.log_you_crit", damage) else ui("expedition.log_they_crit", monster, damage)
        // An evasion or a block belongs to the one who was struck at.
        HitKind.EVADED -> if (hero) ui("expedition.log_they_evade", monster) else ui("expedition.log_you_evade")
        HitKind.BLOCKED -> if (hero) ui("expedition.log_they_block", monster) else ui("expedition.log_you_block")
    }
}

private fun logColour(event: CombatEvent): Color = when (event.kind) {
    HitKind.CRIT -> Color(0xFFFFD34A)
    HitKind.HIT -> if (event.attacker == Side.HERO) Parchment else Color(0xFFE9A0A0)
    else -> Muted
}

private fun hitText(hit: FloatingHit): String = when (hit.kind) {
    HitKind.EVADED -> ui("expedition.evaded")
    HitKind.BLOCKED -> ui("expedition.blocked")
    HitKind.CRIT -> ui("expedition.crit", hit.amount)
    HitKind.HIT -> hit.amount.toString()
}

private fun hitColour(hit: FloatingHit): Color = when (hit.kind) {
    HitKind.EVADED, HitKind.BLOCKED -> Muted
    HitKind.CRIT -> Color(0xFFFFD34A)
    HitKind.HIT -> if (hit.target == Side.HERO) LifeRed else Parchment
}

private fun outcomeColour(outcome: Outcome) = when (outcome) { Outcome.WIN -> Vital; Outcome.LOSS -> LifeRed; Outcome.RETREAT -> Muted }

/**
 * After the fight: how it ended, what it came to, the whole log to scroll back through and — for a
 * victory — what the server rolled. The same screen closes a defeat, without the loot.
 */
@Composable internal fun ReportScreen(s: ForgeState, hud: RunHud, report: FightReport, onContinue: () -> Unit) {
    val won = report.outcome == Outcome.WIN
    Column(Modifier.fillMaxSize().background(Ink.copy(alpha = .9f)).statusBarsPadding().navigationBarsPadding().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(ui("expedition.outcome_${report.outcome.name.lowercase()}"), color = outcomeColour(report.outcome), style = MaterialTheme.typography.headlineSmall)
        Text(monsterTitle(report.monster.code), color = rarityTint(report.monster.rarity), style = MaterialTheme.typography.titleMedium)
        val cells = listOf(
            report.dealt.toString() to "expedition.sum_dealt", report.taken.toString() to "expedition.sum_taken",
            ui("expedition.log_time", String.format(Locale.ROOT, "%.1f", report.duration)) to "expedition.sum_time",
            report.crits.toString() to "expedition.sum_crits", report.blocked.toString() to "expedition.sum_blocked",
            report.evaded.toString() to "expedition.sum_evaded")
        cells.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    Column(Modifier.weight(1f).background(Panel, RoundedCornerShape(8.dp)).border(1.dp, Bronze.copy(alpha = .4f), RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(ui(label), color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).border(1.dp, Bronze.copy(alpha = .4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)) {
            // The whole fight, from the first swing down, as it happened.
            FightLog(report.events, report.monster.code, Modifier.fillMaxSize())
        }
        if (won) Loot(s, hud)
        else Text(ui("expedition.dead_hint"), color = Parchment, style = MaterialTheme.typography.bodySmall)
        Button(enabled = !hud.rewardPending, onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(ui(if (won) "expedition.continue" else "expedition.back_to_camp"))
        }
    }
}

/** What the kill brought: the server's roll, or its absence said plainly. */
@Composable private fun Loot(s: ForgeState, hud: RunHud) {
    val reward = hud.reward
    when {
        hud.rewardPending -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Gold, strokeWidth = 2.dp)
            Text(ui("expedition.loot_pending"), color = Muted)
        }
        hud.rewardFailed -> Text(ui("expedition.loot_failed"), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
        reward != null -> Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(ui("expedition.loot_experience", number(reward.experience)), color = Rune)
                if (reward.gold > 0) Text(ui("expedition.loot_gold", reward.gold), color = GoldBright)
            }
            reward.items.forEach { stack ->
                val orb = s.world.orbs.firstOrNull { it.id == stack.itemId }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(ForgeGlyphs.Orb, null, tint = Gold, modifier = Modifier.size(18.dp))
                    Text(ui("expedition.loot_stack", orb?.title(s.lang) ?: ui("common.item"), stack.amount), color = Parchment)
                }
            }
            reward.equipment.forEach { instance ->
                ItemRow(inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId]), s.world.definitions) {}
            }
            if (reward.items.isEmpty() && reward.equipment.isEmpty()) Text(ui("expedition.loot_nothing"), color = Muted)
        }
    }
}
