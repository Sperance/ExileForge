package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.MapTally
import com.sperance.exileforge.core.campaign.RunHud
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.screens.hero.StackInfoSheet
import com.sperance.exileforge.ui.theme.*

/**
 * How a map's summary opens: its title and line, the colour and glyph of the ending, and the button that closes it.
 * Read off the run: a fall, the exit, the portal out — a Vaal zone and an autorun in their own words.
 */
internal data class SummaryHead(val title: String, val hint: String, val accent: Color, val glyph: ImageVector, val done: String) {
    companion object {
        fun of(hud: RunHud): SummaryHead {
            val back = ui(if (hud.vaal) "vaal.back" else "expedition.back_to_camp")
            return when (hud.tally.end) {
                MapEnd.FELL -> SummaryHead(ui("expedition.dead"), ui(if (hud.vaal) "vaal.dead_hint" else "expedition.dead_hint"), LifeRed, ForgeGlyphs.Skull, back)

                MapEnd.CLEARED -> when {
                    hud.vaal -> SummaryHead(ui("vaal.done"), ui("vaal.done_hint"), Vital, ForgeGlyphs.Banner, back)
                    hud.autoReward != null -> SummaryHead(ui("auto.done"), ui("expedition.map_done_hint"), Vital, ForgeGlyphs.Banner, back)
                    else -> SummaryHead(ui("expedition.map_done"), ui("expedition.map_done_hint"), Vital, ForgeGlyphs.Banner, back)
                }

                MapEnd.LEFT, null -> SummaryHead(ui("summary.left"), ui("summary.left_hint"), Gold, ForgeGlyphs.Portal, back)
            }
        }
    }
}

/**
 * The map is over — by a fall, the exit or the portal out — and before the camp, what it came to: the ending, the
 * figures (foes and guardians slain, the time on the map, deaths, damage dealt and taken), the gold and experience,
 * and every stack and piece the server granted for it, compact, each with its icon. The loot is the server's roll:
 * while some of it is on its way the screen says so, asks for it at once and fills in as the answers arrive.
 * A piece opens its comparison with what is worn, a stack its description.
 */
@Composable internal fun MapSummary(s: ForgeState, vm: ExpeditionViewModel, hud: RunHud, head: SummaryHead = SummaryHead.of(hud), onDone: () -> Unit) {
    val tally = hud.tally
    var looked by remember { mutableStateOf<ItemView?>(null) }
    var stack by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(tally.receiving) { if (tally.receiving) vm.flushRun() }
    Column(
        Modifier.fillMaxSize().background(Ink.copy(alpha = .94f)).statusBarsPadding().navigationBarsPadding().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SummaryBanner(head, mapTitle(hud.mapCode))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MutedText(head.hint, style = MaterialTheme.typography.bodySmall)
            Tallies(tally)
            if (tally.end == MapEnd.FELL) {
                hud.fall?.takeIf { it > 0 }?.let {
                    Text(ui("expedition.fall_lost", number(it)), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                }
            }
            Loot(s, tally, onItem = { looked = it }, onStack = { stack = it })
            if (tally.end == MapEnd.FELL) DeathRecap(hud.recap)
            RunFigures(tally.figures)
        }
        ForgeButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = head.accent, contentColor = if (head.accent == LifeRed) Parchment else Ink),
        ) {
            Text(head.done, style = MaterialTheme.typography.titleMedium)
        }
    }
    looked?.let { item -> LootSheet(s, vm, item, onDismiss = { looked = null }) }
    stack?.let { code -> StackInfoSheet(s, code) { stack = null } }
}

/** The ending over the map's name: its glyph in a ring of its colour, on a glow of it. */
@Composable private fun SummaryBanner(head: SummaryHead, map: String) {
    Box(
        Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp))
            .background(Brush.radialGradient(listOf(head.accent.copy(alpha = .22f), Ink))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Ink).border(2.dp, head.accent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(head.glyph, null, tint = head.accent, modifier = Modifier.size(26.dp))
            }
            Text(head.title, color = head.accent, style = MaterialTheme.typography.titleLarge)
            MutedText(map, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** The map's figures, three to a row: the slaughter, the time and the damage, then what it paid. */
@Composable private fun Tallies(tally: MapTally) {
    Caption(ui("summary.figures"))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Tile(ForgeGlyphs.Swords, tally.kills.toString(), ui("summary.kills"))
        Tile(ForgeGlyphs.Banner, tally.bosses.toString(), ui("summary.bosses"))
        Tile(ForgeGlyphs.Skull, tally.deaths.toString(), ui("summary.deaths"), if (tally.deaths > 0) LifeRed else Parchment)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Tile(ForgeGlyphs.Portal, clock(tally.seconds), ui("summary.time"))
        Tile(ForgeGlyphs.Target, number(tally.figures.totalDealt), ui("summary.dealt"))
        Tile(ForgeGlyphs.Helm, number(tally.figures.totalTaken), ui("summary.taken"))
    }
    Caption(ui("expedition.report_reward"))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Tile(ForgeGlyphs.Coins, number(tally.gold.toDouble()), ui("summary.gold"), GoldBright)
        Tile(ForgeGlyphs.Tome, number(tally.experience), ui("summary.experience"), Rune)
    }
}

@Composable private fun RowScope.Tile(icon: ImageVector, value: String, label: String, tone: Color = Parchment) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier.weight(1f).background(Abyss, shape).border(1.dp, PanelRaised, shape).padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = Muted, modifier = Modifier.size(16.dp))
        Column {
            Text(value, color = tone, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Everything the server granted for the map: the recipe, the pieces a line each, the stacks as chips; on its way, or its absence said. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Loot(s: ForgeState, tally: MapTally, onItem: (ItemView) -> Unit, onStack: (String) -> Unit) {
    val loot = tally.loot
    Caption(ui("summary.loot"))
    loot.recipe?.let { code ->
        val text = s.index?.let { i -> i.recipe(code)?.let { recipeText(i, it) } } ?: displayName(code)
        Text(ui("summary.recipe", text), color = Rune, style = MaterialTheme.typography.bodySmall)
    }
    loot.equipment.mapNotNull { s.view(it) }.forEach { PieceLine(it) { onItem(it) } }
    if (loot.items.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            loot.items.entries.sortedByDescending { it.value }.forEach { (code, amount) ->
                StackChip(s, code, amount) { onStack(code) }
            }
        }
    }
    if (tally.receiving) {
        Receiving()
    } else if (tally.empty) {
        MutedText(ui("summary.loot_none"))
    }
}

/** A piece in one line: its icon in its rarity's frame and its name in that colour; the whole card is one tap behind it. */
@Composable internal fun PieceLine(item: ItemView, onClick: () -> Unit) {
    val color = rarityColor(item.rarity.name)
    val frame = RoundedCornerShape(5.dp)
    Row(
        Modifier.fillMaxWidth().clip(frame).background(Panel, frame).border(1.dp, Bronze.copy(alpha = .5f), frame)
            .clickable(role = Role.Button, onClick = onClick).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(32.dp).background(color.copy(alpha = .08f), frame).border(1.dp, color, frame), contentAlignment = Alignment.Center) {
            ItemIcon(item, color, Modifier.size(22.dp))
        }
        Text(item.title, color = color, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        MutedText(ui("row.level", item.level), style = MaterialTheme.typography.labelSmall)
    }
}

/** A stack of the bag as a chip: its icon, its name and how many. */
@Composable internal fun StackChip(s: ForgeState, code: String, amount: Long, onClick: () -> Unit) {
    val kind = s.index?.item(code)?.let(::bagVisualKind) ?: ItemVisualKind.ITEM
    val shape = RoundedCornerShape(3.dp)
    Row(
        Modifier.clip(shape).background(Abyss, shape).border(1.dp, Bronze.copy(alpha = .6f), shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        BagIcon(code, Modifier.size(18.dp), kind = kind)
        Text(ui("expedition.loot_stack", itemTitle(code), amount), color = Parchment, style = MaterialTheme.typography.labelMedium)
    }
}
