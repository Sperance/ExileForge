package com.sperance.exileforge.ui.screens.crafts

import com.sperance.exileforge.ui.components.ForgeSheet
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.workTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.roll.AwayStop
import com.sperance.exileforge.rules.roll.CraftsAway
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*

/**
 * «Пока вас не было» (3.69.0, server 1.66.0): the crafts catch-up of an absence of five minutes or more, once.
 *
 * The server keeps the last such catch-up in the hero's crafts part; the device keeps the `until` of the one it
 * showed last, per hero, so a recomposition, a tab or a relaunch does not bring the same one back. Until that mark
 * is read nothing is shown, rather than a sheet that flashes and goes.
 */
@Composable fun CraftsAwayHost(s: ForgeState, vm: ForgeViewModel) {
    val heroId = s.play.heroId
    var seen by remember(heroId) { mutableStateOf<Long?>(null) }
    LaunchedEffect(heroId) { if (heroId.isNotBlank()) seen = vm.craftsAwaySeen(heroId) }
    val away = s.hero?.crafts?.away?.takeIf { away -> seen.let { it != null && away.until > it } } ?: return
    CraftsAwaySheet(s, away) { seen = away.until; vm.markCraftsAwaySeen(heroId, away.until) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun CraftsAwaySheet(s: ForgeState, away: CraftsAway, onDismiss: () -> Unit) {
    val offlineHours = s.index?.professions?.rules?.offlineHours
    val cap = offlineHours?.let { (it * 3_600_000).toLong() } ?: Long.MAX_VALUE
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ui("away.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AwayFigure(ui("away.time"), clock(minOf(away.until - away.since, cap)), Modifier.weight(1f))
                AwayFigure(ui("away.cycles"), away.cycles.toString(), Modifier.weight(1f))
                AwayFigure(ui("away.xp"), "+" + number(away.xp) + (if (away.levels > 0) " ▲${away.levels}" else ""), Modifier.weight(1f))
            }
            Text(workTitle(away.job, away.choice), color = Parchment, style = MaterialTheme.typography.titleSmall)
            if (away.levels > 0) Text(ui("away.levels", away.levels), color = Vital, style = MaterialTheme.typography.labelMedium)
            Engraved(ui("away.gained"))
            if (away.gained.isEmpty()) MutedText(ui("away.nothing"))
            else AwayStacks(s, away.gained, "+", Vital)
            if (away.spent.isNotEmpty()) {
                Engraved(ui("away.spent"), accent = LifeRed)
                AwayStacks(s, away.spent, "−", LifeRed)
            }
            away.stopReason?.let { stop ->
                Text(when (stop) {
                    AwayStop.INPUTS -> ui("away.stop.inputs")
                    AwayStop.CAP -> ui("away.stop.cap", offlineHours?.let(::number) ?: "?")
                    AwayStop.FULL -> ui("away.stop.full")
                }, color = LifeRed, style = MaterialTheme.typography.bodySmall)
            }
            ForgeButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(ui("away.ok")) }
        }
    }
}

/** The absence as «h:mm». */
private fun clock(millis: Long): String {
    val minutes = (millis / 60_000).coerceAtLeast(0)
    return "%d:%02d".format(minutes / 60, minutes % 60)
}

@Composable private fun AwayFigure(title: String, figure: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(figure, color = Parchment, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(title, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A line per code, most first: its icon, «+3» or «−3», its name. A code may be a bag stack or an equipment template. */
@Composable private fun AwayStacks(s: ForgeState, stacks: Map<String, Int>, sign: String, tone: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        stacks.entries.sortedByDescending { it.value }.forEach { (code, amount) ->
            val template = s.index?.template(code)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (template != null) ItemIcon(template, Gold, Modifier.size(24.dp))
                else BagIcon(code, Modifier.size(24.dp), kind = s.index?.item(code)?.let(::bagVisualKind) ?: ItemVisualKind.ITEM)
                Text("$sign$amount", color = tone, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(if (template != null) equipmentTitle(code) else itemTitle(code), color = Parchment, style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
