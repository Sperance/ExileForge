package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.EffectTrace
import com.sperance.exileforge.core.campaign.FactorKey
import com.sperance.exileforge.core.campaign.FactorTrace
import com.sperance.exileforge.core.campaign.FighterShot
import com.sperance.exileforge.core.campaign.HitTrace
import com.sperance.exileforge.core.campaign.NoteTrace
import com.sperance.exileforge.core.campaign.RollTrace
import com.sperance.exileforge.core.campaign.TickTrace
import com.sperance.exileforge.core.campaign.Trace
import com.sperance.exileforge.core.campaign.TraceOrigin
import com.sperance.exileforge.core.campaign.TypeTrace
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.CombatEvent
import com.sperance.exileforge.core.campaign.combat.HitKind
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statValue
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.StatTrace
import com.sperance.exileforge.presentation.state.TraceExplainer
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.screens.expedition.arena.damageTint
import com.sperance.exileforge.ui.screens.expedition.arena.key
import com.sperance.exileforge.ui.screens.expedition.arena.noteLine
import com.sperance.exileforge.ui.screens.hero.petName
import com.sperance.exileforge.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

/** Части разбора удара (3.80.24): цепочка, броски, стороны, статы и состояния. */
/** The formula as chips; the one tapped lays out every stat it reads, of the striker and of the target, by source. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Chain(factors: List<FactorTrace>, attacker: FighterShot, target: FighterShot, origin: TraceOrigin, explainer: TraceExplainer, game: GameUi) {
    var chosen by remember(factors) { mutableStateOf(factors.firstOrNull { it.key == FactorKey.CRIT } ?: factors.first()) }
    Section(ui("trace.section.formula")) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            factors.forEachIndexed { i, f ->
                if (i > 0) Text(if (f.key == FactorKey.TOTAL) "=" else "×", color = Muted, modifier = Modifier.align(Alignment.CenterVertically))
                Chip(f, f == chosen) { chosen = f }
            }
        }
        val stats = chosen.attacker.map { attacker to it } + chosen.target.map { target to it }
        val shown = stats.map { (shot, stat) -> shot to explainer.stat(shot, stat, origin) }
            .filter { (shot, trace) -> (shot.stats[trace.stat] ?: 0.0) != 0.0 || trace.rows.isNotEmpty() }
        Spacer(Modifier.height(6.dp))
        Text(ui("trace.factor.${chosen.key.name}.about"), color = Muted, style = MaterialTheme.typography.bodySmall)
        shown.forEach { (shot, trace) -> StatBlock(trace, sideLabel(shot, attacker)) }
        if (shown.isEmpty() && chosen.key != FactorKey.TOTAL) Note(ui("trace.factor.no_stats"))
    }
}

@Composable internal fun Chip(f: FactorTrace, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    val value = when (f.key) {
        FactorKey.BASE, FactorKey.TOTAL -> fineNumber(f.value)
        else -> factor(f.value)
    }
    Column(Modifier.clip(shape).background(PanelRaised).border(1.dp, if (on) Gold else Bronze, shape).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(value, color = if (f.key == FactorKey.TOTAL) GoldBright else Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(ui("trace.factor.${f.key.name}"), color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

internal fun sideLabel(shot: FighterShot, attacker: FighterShot): String = if (shot === attacker) ui("trace.side.attacker") else ui("trace.side.target")

// ==================== The dice ====================

@Composable internal fun Rolls(rolls: List<RollTrace>) = Section(ui("trace.section.rolls")) {
    rolls.forEach { roll ->
        val title = ui("trace.roll.${roll.key.name}") + (roll.ailment?.let { " · ${ui(it.key())}" } ?: "")
        Line(
            title,
            ui("trace.roll.value", pct(roll.chance), String.format(Locale.ROOT, "%.2f", roll.rolled)) + if (roll.success) " ✓" else " ✗",
            if (roll.success) Vital else Muted,
        )
    }
}

// ==================== The sides ====================

/** The striker, the target and what lay on both, as tabs; a stat tapped opens its sources. */
@Composable internal fun Sides(attacker: FighterShot, target: FighterShot, origin: TraceOrigin, explainer: TraceExplainer, game: GameUi, factors: List<FactorTrace>) {
    var tab by remember { mutableIntStateOf(0) }
    Column {
        TabRow(selectedTabIndex = tab, containerColor = Panel) {
            listOf("trace.side.attacker", "trace.side.target", "trace.side.states").forEachIndexed { i, key ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(ui(key), style = MaterialTheme.typography.labelLarge) })
            }
        }
        Spacer(Modifier.height(8.dp))
        when (tab) {
            0 -> Stats(attacker, (factors.flatMap { it.attacker } + ATTACKER_STATS).distinct(), origin, explainer)
            1 -> Stats(target, (factors.flatMap { it.target } + TARGET_STATS).distinct(), origin, explainer)
            else -> States(listOfNotNull(attacker, target.takeIf { it !== attacker }), explainer)
        }
    }
}

internal val ATTACKER_STATS = listOf(CoreStat.ACCURACY.code, CoreStat.CRITICAL_CHANCE.code, CoreStat.SPELL_CRITICAL_CHANCE.code, CoreStat.ATTACK_SPEED.code, CoreStat.CAST_SPEED.code)
internal val TARGET_STATS = listOf(CoreStat.HEALTH.code, CoreStat.ENERGY_SHIELD.code, CoreStat.EVASION.code, CoreStat.BLOCK_CHANCE.code, CoreStat.SPELL_BLOCK.code, CoreStat.ARMOR.code)

@Composable internal fun Stats(shot: FighterShot, stats: List<String>, origin: TraceOrigin, explainer: TraceExplainer) {
    val shown = stats.filter { (shot.stats[it] ?: 0.0) != 0.0 }
    if (shown.isEmpty()) {
        Note(ui("trace.factor.no_stats"))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { shown.forEach { stat -> StatBlock(explainer.stat(shot, stat, origin), null, collapsed = true) } }
}

/** What lay on the fighters at that moment: the fight's lines by what laid them, their ailments and the hero's conditions. */
@Composable internal fun States(shots: List<FighterShot>, explainer: TraceExplainer) = Section(ui("trace.section.states")) {
    shots.forEach { shot ->
        val groups = shot.lines.groupBy { explainer.lineTitle(it) }
        groups.forEach { (title, lines) ->
            Line(title, "")
            lines.forEach { Note("${statTitle(it.line.stat)} ${explainer.fmt(it.line.stat, it.line.value, it.line.op)}") }
        }
        shot.ailments.groupBy { it.ailment }.forEach { (ailment, active) ->
            Line(ui(ailment.key()), ui("trace.ailment.state", fineNumber(active.sumOf { it.magnitude }), active.size))
        }
        shot.conditions.forEach { Line(locOr("condition.${it.name}", it.name), "✓", Rune) }
        if (groups.isEmpty() && shot.ailments.isEmpty() && shot.conditions.isEmpty()) Note(ui("trace.states.none"))
        Note(ui("trace.pools", fineNumber(shot.life), fineNumber(shot.maxLife), fineNumber(shot.shield)))
    }
}

/** A stat of a side: its figure then, its sources under it — folded until tapped where many are listed. */
@Composable internal fun StatBlock(trace: StatTrace, side: String?, collapsed: Boolean = false) {
    var open by remember(trace.stat) { mutableStateOf(!collapsed) }
    Column(Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 3.dp)) {
        Row {
            Text(
                (if (open) "▾ " else "▸ ") + listOfNotNull(side, trace.title).joinToString(" · "),
                color = Parchment,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(trace.total, color = GoldBright, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        if (open) {
            if (trace.rows.isEmpty()) Note(ui("trace.src.none"))
            trace.rows.forEach { row ->
                Row(Modifier.padding(start = 14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(row.title, color = Muted, style = MaterialTheme.typography.bodySmall)
                        row.note?.let { Text(it, color = Muted.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall) }
                    }
                    Text(row.value, color = ModBlue, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ==================== Pieces ====================

@Composable internal fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(PanelRaised).border(1.dp, Bronze, shape).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(title.uppercase(), color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable internal fun Line(title: String, value: String, tint: Color = GoldBright) = Row(Modifier.fillMaxWidth()) {
    Text(title, color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    Text(value, color = tint, style = MaterialTheme.typography.bodySmall)
}

@Composable internal fun Note(text: String) = Text(text, color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))

internal fun pct(share: Double): String = "${fineNumber(share * 100)}%"
internal fun factor(value: Double): String = String.format(Locale.ROOT, "%.2f", value)
