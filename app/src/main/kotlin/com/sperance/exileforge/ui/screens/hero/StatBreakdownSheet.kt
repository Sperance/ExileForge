package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.StatGroup
import com.sperance.exileforge.core.display.StatLimit
import com.sperance.exileforge.core.display.StatLimits
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statValue
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.ManaReserve
import com.sperance.exileforge.presentation.state.ShareCard
import com.sperance.exileforge.presentation.state.ShareKind
import com.sperance.exileforge.presentation.state.ShareRow
import com.sperance.exileforge.presentation.state.StatExplainer
import com.sperance.exileforge.presentation.state.StatExplanation
import com.sperance.exileforge.presentation.state.manaReserve
import com.sperance.exileforge.presentation.state.passiveShares
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.*

internal fun ShareKind.color(): Color = when (this) {
    ShareKind.BASE -> Muted
    ShareKind.ATTRIBUTE -> Gold
    ShareKind.CLASS -> GoldBright
    ShareKind.NODE -> Rune
    ShareKind.ITEM -> Color(0xFFFFFF77)
    ShareKind.PET -> Elder
    ShareKind.SKILL -> ManaBlue
    ShareKind.OTHER -> Parchment
    ShareKind.AFTER -> Handcrafted
}

/**
 * A figure's own window (3.11.0): its total, then what it is made of — a bar of each kind's share,
 * a card per kind with every source by name, the formula, and for a stat others take from, what it
 * gives them now. A linked stat opens in the same window; the chip on top goes back.
 *
 * [shifts] are what lies over the hero's own sheet — a map's and the atlas's effects — by stat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatBreakdownSheet(game: GameUi, stat: String, shifts: Map<String, List<Shift>>, onDismiss: () -> Unit) {
    val explainer = game.hero?.sheet?.model?.explainer ?: run {
        LaunchedEffect(stat) { onDismiss() }
        return
    }
    var trail by remember(stat) { mutableStateOf(listOf(stat)) }
    val current = trail.last()
    val view = remember(current, explainer, shifts, game.lang, game.hero?.skills) {
        StatExplainer(game).explain(explainer.explain(current).shifted(shifts[current].orEmpty()), explainer.grants(current), explainer::holders, game.passiveShares(current))
    }
    val accent = StatGroup.of(current).accent()
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().fillMaxHeight(.9f).navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (trail.size > 1) {
                item {
                    AssistChip(onClick = { trail = trail.dropLast(1) }, label = { Text("← ${statTitle(trail[trail.size - 2], game.lang)}") })
                }
            }
            item { Header(current, view, accent, game) }
            game.index?.campaign?.combat?.let { StatLimits.of(current, game.hero?.stats.orEmpty(), it) }?.let { limit -> item { LimitCard(limit, game) { trail = trail + it } } }
            if (current == MANA_STAT) game.manaReserve()?.takeIf { it.percent > 0 }?.let { reserve -> item { ReserveCard(reserve, game) } }
            if (view.weights.values.sum() > 0) item { ShareBar(view.weights) }
            items(view.cards.size) { i -> SourceCard(view.cards[i], game) { trail = trail + it } }
            if (view.cards.isEmpty()) item { Text(ui("stat.empty"), color = Muted, style = MaterialTheme.typography.bodySmall) }
            if (view.cards.isNotEmpty()) item { Formula(view.formula) }
            if (view.grants.isNotEmpty()) item { Grants(current, view.grants, game) { trail = trail + it } }
        }
    }
}

@Composable private fun Header(stat: String, view: StatExplanation, accent: Color, game: GameUi) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val shape = RoundedCornerShape(10.dp)
        Box(Modifier.size(40.dp).clip(shape).background(accent.copy(alpha = .13f)).border(1.dp, accent.copy(alpha = .4f), shape), contentAlignment = Alignment.Center) {
            StatIcon(stat, accent, Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(StatGroup.of(stat).title(game.lang).uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall)
            Text(statTitle(stat, game.lang), color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(view.total, color = accent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

/**
 * The ceiling of a capped figure (3.12.0): the rules' base limit, each raise by the stat that gives it (a tap
 * opens that stat's own breakdown), the limit that stands and the hard ceiling; then what the fight counts
 * and what lies above the limit for nothing.
 */
@Composable private fun LimitCard(limit: StatLimit, game: GameUi, open: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val value = { v: Double -> statValue(limit.stat, v, game.index) }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(PanelRaised).border(1.dp, Bronze, shape).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ui("stat.cap.title"), color = Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(value(limit.cap), color = Gold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        SourceLine(ShareRow(ui("stat.cap.base"), value(limit.base)), open)
        limit.raises.forEach { (stat, amount) -> SourceLine(ShareRow(statTitle(stat, game.lang), (if (amount >= 0) "+" else "−") + value(kotlin.math.abs(amount)), link = stat), open) }
        limit.hard?.let { SourceLine(ShareRow(ui("stat.cap.hard"), value(it)), open) }
        Text(
            ui("stat.cap.counts", value(minOf(limit.effective, limit.cap))) + if (limit.over > 0) " · " + ui("stat.cap.over", value(limit.over)) else "",
            color = if (limit.over > 0) Muted else Parchment,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/** What the passive auras hold of the mana: their share and amount, and the mana left free to spend. */
@Composable private fun ReserveCard(reserve: ManaReserve, game: GameUi) {
    val shape = RoundedCornerShape(12.dp)
    val value = { v: Double -> statValue(MANA_STAT, v, game.index) }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(PanelRaised).border(1.dp, ManaBlue.copy(alpha = .45f), shape).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(ui("stat.reserve.title"), color = Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        SourceLine(ShareRow(ui("stat.reserve.held", number(reserve.percent)), "−" + value(reserve.held))) {}
        SourceLine(ShareRow(ui("stat.reserve.free"), value(reserve.free))) {}
    }
}

/** Each kind's share of the total as one bar and its legend, in percent of the positive part. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareBar(weights: Map<ShareKind, Double>) {
    val sum = weights.values.sum()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val shape = RoundedCornerShape(7.dp)
        Row(Modifier.fillMaxWidth().height(14.dp).clip(shape).border(1.dp, Bronze, shape)) {
            weights.forEach { (kind, value) -> Box(Modifier.weight((value / sum).toFloat().coerceAtLeast(.001f)).fillMaxHeight().background(kind.color())) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            weights.forEach { (kind, value) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(kind.color()))
                    Text("${ui("stat.kind.${kind.name}")} ${Math.round(value / sum * 100)}%", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable private fun KindTag(kind: ShareKind) {
    val color = kind.color()
    Text(
        ui("stat.tag.${kind.name}"),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = .16f)).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** A kind's card: its tag, its name and what it gives together, then a line per source, a grey note under one that has it. */
@Composable private fun SourceCard(card: ShareCard, game: GameUi, open: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(PanelRaised).border(1.dp, Bronze, shape).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KindTag(card.kind)
            Text(ui("stat.kind.${card.kind.name}"), color = Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(card.summary, color = if (card.summary.startsWith("−")) LifeRed else Gold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        card.rows.forEach { SourceLine(it, open) }
    }
}

@Composable private fun SourceLine(row: ShareRow, open: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().then(row.link?.let { link -> Modifier.clickable { open(link) } } ?: Modifier).padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.title,
                color = if (row.link != null) ModBlue else Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                textDecoration = if (row.link != null) TextDecoration.Underline else null,
            )
            Text(row.value, color = if (row.value.startsWith("−")) LifeRed else Parchment, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
        row.note?.let { Text(it, color = Muted.copy(alpha = .8f), style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable private fun Formula(text: String) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        text,
        color = Parchment,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(shape).background(Ink).border(1.dp, Bronze, shape).padding(10.dp),
    )
}

/** What the stat gives others now; a tap opens the one given to. */
@Composable private fun Grants(stat: String, rows: List<ShareRow>, game: GameUi, open: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(ui("stat.gives", statTitle(stat, game.lang)).uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall)
        rows.forEach { row ->
            val shape = RoundedCornerShape(8.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(PanelRaised).clickable { row.link?.let(open) }.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.title, color = ModBlue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text(row.value, color = if (row.value.startsWith("−")) LifeRed else Gold, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("  ›", color = Muted)
            }
        }
    }
}
