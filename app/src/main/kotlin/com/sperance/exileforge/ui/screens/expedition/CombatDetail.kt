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
import com.sperance.exileforge.core.campaign.Action
import com.sperance.exileforge.core.campaign.CombatEvent
import com.sperance.exileforge.core.campaign.EffectTrace
import com.sperance.exileforge.core.campaign.FactorKey
import com.sperance.exileforge.core.campaign.FactorTrace
import com.sperance.exileforge.core.campaign.FighterShot
import com.sperance.exileforge.core.campaign.HitKind
import com.sperance.exileforge.core.campaign.HitTrace
import com.sperance.exileforge.core.campaign.NoteTrace
import com.sperance.exileforge.core.campaign.RollTrace
import com.sperance.exileforge.core.campaign.Side
import com.sperance.exileforge.core.campaign.TickTrace
import com.sperance.exileforge.core.campaign.Trace
import com.sperance.exileforge.core.campaign.TraceOrigin
import com.sperance.exileforge.core.campaign.TypeTrace
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statValue
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.StatTrace
import com.sperance.exileforge.presentation.state.TraceExplainer
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.screens.expedition.arena.damageTint
import com.sperance.exileforge.ui.screens.expedition.arena.key
import com.sperance.exileforge.ui.screens.expedition.arena.noteLine
import com.sperance.exileforge.ui.screens.hero.petName
import com.sperance.exileforge.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A line of the fight's log, laid open (3.37.0): first the outcome — the figure, what marked the blow, and per damage type
 * what it was, what landed and what cut it; «Полный расчёт» lays out the rest — the formula as a chain of chips (a chip
 * tapped lays out the stats it reads by source), the dice, where the damage went, and tabs of the striker, the target and
 * what lay on both at that moment. Every figure is the fight's own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CombatDetailSheet(s: ForgeState, event: CombatEvent, monster: String, onDismiss: () -> Unit) {
    val trace = event.trace ?: run {
        LaunchedEffect(event) { onDismiss() }
        return
    }
    val explainer = remember(s.index, s.lang) { TraceExplainer(s) }
    var full by remember(event) { mutableStateOf(false) }
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().fillMaxHeight(.9f).navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Header(event, monster) }
            item { Marks(event, trace) }
            if (trace is HitTrace) items(trace.types.filter { it.raw > 0.05 || it.dealt > 0.05 }) { TypeRow(it) }
            item { FullToggle(full) { full = !full } }
            if (full) {
                when (trace) {
                    is HitTrace -> hitItems(trace, explainer, s)
                    is TickTrace -> tickItems(trace, explainer, s)
                    is EffectTrace -> effectItems(trace, explainer, s)
                    is NoteTrace -> noteItems(trace, explainer, s)
                }
            }
        }
    }
}

/** Who did what to whom and when, then the line's figure large, with what it is. */
@Composable private fun Header(event: CombatEvent, monster: String) {
    val sentence = when (event.action) {
        Action.NOTE -> noteLine(event, monster)

        else -> {
            // The combat pet's line (3.70.0) names it: its blow, its healing, or the blow it took.
            val pet = event.pet?.let(::petName)
            listOfNotNull(
                if (event.actor == Side.HERO) pet ?: ui("trace.you") else monster,
                event.skill?.let { if (event.action == Action.FLASK) equipmentTitle(it) else SkillText.title(it) },
                when {
                    event.target == Side.HERO && event.actor != Side.HERO -> pet?.let { "→ $it" } ?: ui("trace.at_you")
                    event.actor == Side.HERO && !event.onSelf -> "→ $monster"
                    pet != null && event.onSelf -> ui("trace.pet_heals")
                    else -> null
                },
            ).joinToString(" ")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column {
            Text(sentence, color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(ui("trace.at_time", String.format(Locale.ROOT, "%.1f", event.time)), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        val figure = when {
            event.damage > 0 -> event.damage.roundToInt().toString() to ui(if (event.target == Side.HERO) "trace.figure.taken" else "trace.figure.dealt")
            event.healed >= 1 -> "+${event.healed.roundToInt()}" to ui("trace.effect.healed")
            else -> null
        }
        figure?.let { (value, caption) ->
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(value, color = if (event.damage > 0) GoldBright else Vital, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(caption, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

/** What marked the line, as small chips: a crit and its multiplier, a block, a dodge, a stun, the ailments, what the shield took. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Marks(event: CombatEvent, trace: Trace) {
    val hit = trace as? HitTrace
    val crit = event.kind == HitKind.CRIT
    val marks = buildList {
        when (event.kind) {
            HitKind.CRIT -> add(hit?.factors?.firstOrNull { it.key == FactorKey.CRIT }?.let { ui("trace.tag.crit_by", fineNumber(it.value)) } ?: ui("trace.tag.crit"))
            HitKind.EVADED -> add(ui("trace.tag.evaded"))
            HitKind.BLOCKED -> add(ui("trace.tag.blocked"))
            else -> Unit
        }
        if (event.stunned) add(ui("expedition.stunned"))
        event.inflicted.forEach { add(ui(it.key())) }
        event.ailment?.let { add(ui(it.key())) }
        hit?.landing?.shield?.takeIf { it >= 1 }?.let { add(ui("trace.tag.shield", it.roundToInt())) }
    }
    if (marks.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        marks.forEachIndexed { i, mark ->
            val lit = crit && i == 0
            Text(
                mark,
                color = if (lit) Ember else Parchment,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (lit) Ember.copy(alpha = .2f) else PanelRaised).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** A damage type on one row: its colour, its name, and what it was against what landed, with what cut it. */
@Composable private fun TypeRow(type: TypeTrace) {
    val figures = listOf("${fineNumber(type.raw)} → ${fineNumber(type.dealt)}") + cuts(type)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Abyss).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(damageTint(type.type)))
        Text(ui(type.type.key()), color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(figures.joinToString(" · "), color = Muted, style = MaterialTheme.typography.labelMedium)
    }
}

/** What cut a damage type on its way in: the armour's share, the resistance, the penetration that pierced it. */
private fun cuts(type: TypeTrace): List<String> = listOfNotNull(
    type.armour.takeIf { it > 0 }?.let { ui("trace.cut.armour", pct(it)) },
    type.resist.takeIf { it != 0.0 }?.let { ui("trace.cut.resist", pct(it)) },
    type.penetration.takeIf { it > 0 }?.let { ui("trace.cut.penetration", fineNumber(it)) },
)

/** «Полный расчёт ▾»: the formula, the dice and the sides, folded until asked for. */
@Composable private fun FullToggle(open: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        ui(if (open) "trace.full.hide" else "trace.full.show") + if (open) " ▴" else " ▾",
        color = Gold,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(shape).border(1.dp, Bronze, shape).clickable(onClick = onClick).padding(vertical = 9.dp),
    )
}

// ==================== A blow ====================

private fun LazyListScope.hitItems(trace: HitTrace, explainer: TraceExplainer, s: ForgeState) {
    if (trace.factors.isNotEmpty()) item { Chain(trace.factors, trace.attacker, trace.target, trace.origin, explainer, s) }
    if (trace.rolls.isNotEmpty()) item { Rolls(trace.rolls) }
    if (trace.types.isNotEmpty()) {
        item {
            Section(ui("trace.section.types")) {
                trace.types.forEach { type ->
                    Line(ui(type.type.key()), "${fineNumber(type.base)} → ${fineNumber(type.raw)} → ${fineNumber(type.dealt)}")
                    cuts(type).takeIf { it.isNotEmpty() }?.let { Note(it.joinToString(" · ")) }
                }
            }
        }
    }
    trace.landing?.let { landing ->
        item {
            Section(ui("trace.section.landing")) {
                listOf("barrier" to landing.barrier, "shield" to landing.shield, "mana" to landing.mana, "delayed" to landing.delayed, "life" to landing.life)
                    .filter { it.second > 0.05 }.forEach { (k, v) -> Line(ui("trace.land.$k"), fineNumber(v)) }
                listOf("leech" to landing.leech, "on_hit" to landing.onHit, "recoup" to landing.recoup)
                    .filter { it.second > 0.05 }.forEach { (k, v) -> Line(ui("trace.land.$k"), "+${fineNumber(v)}", Vital) }
                if (landing.culled) Line(ui("trace.land.culled"), "✓", LifeRed)
            }
        }
    }
    item { Sides(trace.attacker, trace.target, trace.origin, explainer, s, trace.factors) }
}

// ==================== An ailment's tick ====================

private fun LazyListScope.tickItems(trace: TickTrace, explainer: TraceExplainer, s: ForgeState) {
    item {
        Section(ui("trace.section.ailment")) {
            Line(ui("trace.tick.per_second"), fineNumber(trace.perSecond))
            Line(ui("trace.tick.left"), ui("trace.seconds", fineNumber(trace.left)))
            Line(ui("trace.tick.duration"), ui("trace.seconds", fineNumber(trace.duration)))
            if (trace.stacks > 1) Line(ui("trace.tick.stacks"), trace.stacks.toString())
        }
    }
    item { Chain(trace.factors, trace.striker ?: trace.target, trace.target, trace.origin, explainer, s) }
    item { Sides(trace.striker ?: trace.target, trace.target, trace.origin, explainer, s, trace.factors) }
}

// ==================== A draught, a buff, a curse ====================

private fun LazyListScope.effectItems(trace: EffectTrace, explainer: TraceExplainer, s: ForgeState) {
    item {
        Section(ui("trace.section.effect")) {
            if (trace.duration > 0) Line(ui("trace.tick.duration"), ui("trace.seconds", fineNumber(trace.duration)))
            if (trace.healed >= 0.05) Line(ui("trace.effect.healed"), "+${fineNumber(trace.healed)}", Vital)
            if (trace.scale != 1.0) Line(ui("trace.effect.scale"), "×${factor(trace.scale)}")
            trace.lines.forEach { line -> Line(statTitle(line.stat, s.lang), explainer.fmt(line.stat, line.value, line.op)) }
            if (trace.lines.isEmpty() && trace.healed < 0.05) Note(ui("trace.effect.none"))
        }
    }
    item { States(listOf(trace.actor), explainer) }
}

// ==================== A note ====================

private fun LazyListScope.noteItems(trace: NoteTrace, explainer: TraceExplainer, s: ForgeState) {
    if (trace.value != 0.0) item { Section(ui("trace.section.note")) { Line(ui("trace.note.value.${trace.kind.name}"), fineNumber(trace.value)) } }
    item { States(listOf(trace.actor), explainer) }
}

// ==================== The chain of the formula ====================

/** The formula as chips; the one tapped lays out every stat it reads, of the striker and of the target, by source. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chain(factors: List<FactorTrace>, attacker: FighterShot, target: FighterShot, origin: TraceOrigin, explainer: TraceExplainer, s: ForgeState) {
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

@Composable private fun Chip(f: FactorTrace, on: Boolean, onClick: () -> Unit) {
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

private fun sideLabel(shot: FighterShot, attacker: FighterShot): String = if (shot === attacker) ui("trace.side.attacker") else ui("trace.side.target")

// ==================== The dice ====================

@Composable private fun Rolls(rolls: List<RollTrace>) = Section(ui("trace.section.rolls")) {
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
@Composable private fun Sides(attacker: FighterShot, target: FighterShot, origin: TraceOrigin, explainer: TraceExplainer, s: ForgeState, factors: List<FactorTrace>) {
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

private val ATTACKER_STATS = listOf("STOCK_ACCURACY", "STOCK_CRITICAL_CHANCE", "STOCK_SPELL_CRITICAL_CHANCE", "STOCK_ATTACK_SPEED", "STOCK_CAST_SPEED")
private val TARGET_STATS = listOf("STOCK_HEALTH", "STOCK_ENERGY_SHIELD", "STOCK_EVASION", "STOCK_BLOCK_CHANCE", "STOCK_SPELL_BLOCK", "STOCK_ARMOR")

@Composable private fun Stats(shot: FighterShot, stats: List<String>, origin: TraceOrigin, explainer: TraceExplainer) {
    val shown = stats.filter { (shot.stats[it] ?: 0.0) != 0.0 }
    if (shown.isEmpty()) {
        Note(ui("trace.factor.no_stats"))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { shown.forEach { stat -> StatBlock(explainer.stat(shot, stat, origin), null, collapsed = true) } }
}

/** What lay on the fighters at that moment: the fight's lines by what laid them, their ailments and the hero's conditions. */
@Composable private fun States(shots: List<FighterShot>, explainer: TraceExplainer) = Section(ui("trace.section.states")) {
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
@Composable private fun StatBlock(trace: StatTrace, side: String?, collapsed: Boolean = false) {
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

@Composable private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(PanelRaised).border(1.dp, Bronze, shape).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(title.uppercase(), color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable private fun Line(title: String, value: String, tint: Color = GoldBright) = Row(Modifier.fillMaxWidth()) {
    Text(title, color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    Text(value, color = tint, style = MaterialTheme.typography.bodySmall)
}

@Composable private fun Note(text: String) = Text(text, color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))

private fun pct(share: Double): String = "${fineNumber(share * 100)}%"
private fun factor(value: Double): String = String.format(Locale.ROOT, "%.2f", value)
