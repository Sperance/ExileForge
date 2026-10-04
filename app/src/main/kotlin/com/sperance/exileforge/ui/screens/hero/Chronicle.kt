package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.FightFigures
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.jobTitle
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.Achievement
import com.sperance.exileforge.rules.content.Counter
import com.sperance.exileforge.rules.content.Stat
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/** A title as the dictionary names it; its code while the dictionary has not come. */
fun titleName(code: String): String = locOr("title.$code", code)

/** The three steps of an achievement: bronze, silver, gold — a single one-off step is gold. */
private val Medals = listOf(Color(0xFFC08457), Color(0xFFC9D1D9), Color(0xFFFFD166))

/** How many of the content's achievements the hero has complete, of how many; null until the hero and the content are read. */
fun GameUi.chronicleDone(): Pair<Int, Int>? {
    val hero = hero ?: return null
    val achievements = index?.achievements?.achievements ?: return null
    return achievements.count { it.complete(hero.chronicle[it.counter] ?: 0L) } to achievements.size
}

/**
 * The chronicle (3.3.0, server 1.3.0): the hero's title, and a way into all they have done — the
 * counters by section, the achievements with their steps and the titles they open. Only what the
 * server counts is counted; the level, the zones and the atlas are read off the hero.
 *
 * A page of the «Развитие» tab since 3.69.0, opened from its tile; it was a card and a sheet on the Hero tab.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChronicleScreen(game: GameUi, vm: HeroViewModel) {
    val hero = game.hero ?: return
    val achievements = game.index?.achievements ?: return
    val values = hero.chronicle
    val titles = achievements.titles(values)
    // The statistics (3.51.0) are read apart, when the page opens: hundreds of figures ride with no hero snapshot.
    val stats by produceState<Map<String, Long>?>(null, hero.id) { value = vm.heroStats(hero.id) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            hero.info.title.takeIf { it.isNotBlank() }?.let(::titleName) ?: ui("chronicle.no_title"),
            color = if (hero.info.title.isNotBlank()) GoldBright else Muted,
            style = MaterialTheme.typography.titleMedium,
        )
        game.chronicleDone()?.let { (done, all) -> MutedText(ui("chronicle.done", done, all)) }
        Engraved(ui("chronicle.titles"))
        if (titles.isEmpty()) {
            MutedText(ui("chronicle.no_titles"))
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = hero.info.title.isBlank(), enabled = !game.busy, onClick = { vm.setTitle("") }, label = { Text(ui("chronicle.no_title")) })
                titles.forEach { code ->
                    FilterChip(selected = hero.info.title == code, enabled = !game.busy, onClick = { vm.setTitle(code) }, label = { Text(titleName(code)) })
                }
            }
        }
        // Only what the hero has begun: an untouched achievement or counter is noise, not a record.
        val begun = achievements.achievements.filter { (values[it.counter] ?: 0L) > 0L }
        Engraved(ui("chronicle.achievements"))
        if (begun.isEmpty()) MutedText(ui("chronicle.nothing_yet"))
        begun.forEach { AchievementRow(it, values[it.counter] ?: 0L) }
        Counter.SECTIONS.forEach { (section, all) ->
            val counters = all.filter { (values[it] ?: 0L) > 0L }
            if (counters.isEmpty()) return@forEach
            Engraved(ui("chronicle.section.$section"))
            counters.forEach { counter -> Figure(ui("chronicle.counter.$counter"), number((values[counter] ?: 0L).toDouble())) }
        }
        stats?.let { StatSections(it) }
    }
}

/** One line of the chronicle: what, and how much. */
@Composable private fun Figure(label: String, value: String, indent: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(start = if (indent) 14.dp else 0.dp)) {
        Text(label, color = if (indent) Muted else Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(value, color = GoldBright, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

/**
 * The hero's statistics (3.51.0, server 1.49.0): the fights' figures — what the server was told of every fight — and the
 * breakdowns by kind, each a group folded under its total that a tap opens. Nothing that is zero is shown, at either level.
 */
@Composable private fun StatSections(stats: Map<String, Long>) {
    val combat = Stat.COMBAT.filter { (stats[it] ?: 0L) > 0L }
    if (combat.isNotEmpty()) {
        Engraved(ui("chronicle.section.FIGHT"))
        combat.forEach { key ->
            val value = stats.getValue(key)
            Figure(ui("stats.$key"), if (key in SECONDS) clock(value) else number(value.toDouble()))
            if (key == Stat.DEALT) {
                FightFigures.types.forEach { type ->
                    stats[Stat.dealt(type)]?.takeIf { it > 0 }?.let { Figure(ui("enum.damage.$type").replaceFirstChar { c -> c.uppercase() }, number(it.toDouble()), indent = true) }
                }
            }
        }
    }
    Stat.GROUPS.forEach { group ->
        val prefix = "$group:"
        val entries = stats.filter { (key, value) -> key.startsWith(prefix) && value > 0 }.map { (key, value) -> key.removePrefix(prefix) to value }
            .sortedByDescending { it.second }
        if (entries.isEmpty()) return@forEach
        var open by remember(group) { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                (if (open) "▾ " else "▸ ") + ui("stats.group.$group", entries.size),
                color = GoldBright,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(number(entries.sumOf { it.second }.toDouble()), color = GoldBright, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        if (open) {
            entries.forEach { (code, value) ->
                val name = when (group) {
                    Stat.KILL, Stat.BOSS, Stat.KILLER -> monsterTitle(code)
                    Stat.JOB -> jobTitle(code)
                    Stat.RARITY -> ui("enum.monster_rarity.$code")
                    Stat.ZONE_RUNS, Stat.ZONE_DEATHS, Stat.ZONE_CHESTS -> mapTitle(code)
                    else -> itemTitle(code)
                }
                Figure(name, number(value.toDouble()), indent = true)
            }
        }
    }
}

private val SECONDS = setOf(Stat.FIGHT_SECONDS, Stat.FIGHT_LONGEST, Stat.BOSS_FASTEST)

/** Seconds as `h:mm:ss`, or `m:ss` under an hour. */
private fun clock(seconds: Long): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = (seconds % 60).toString().padStart(2, '0')
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$s" else "$m:$s"
}

/** An achievement: its name, what the next step asks and how far it is, and a medal for each step taken. */
@Composable private fun AchievementRow(achievement: Achievement, value: Long) {
    val reached = achievement.reached(value)
    val next = achievement.tiers.getOrNull(reached)
    val target = next ?: achievement.tiers.last()
    Column(Modifier.fillMaxWidth().background(PanelRaised, MaterialTheme.shapes.small).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                loc("achievement.${achievement.code}.name"),
                color = if (next == null) GoldBright else Parchment,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            val medals = if (achievement.tiers.size == 1) listOf(Medals.last()) else Medals.take(achievement.tiers.size)
            medals.forEachIndexed { i, medal ->
                Box(Modifier.size(12.dp).background(if (i < reached) medal else medal.copy(alpha = .18f), CircleShape))
            }
        }
        MutedText(loc("achievement.${achievement.code}.desc", listOf(number(target.toDouble()))))
        LinearProgressIndicator(
            progress = { (value.toFloat() / target).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = if (next == null) GoldBright else Gold,
            trackColor = Abyss,
        )
        Text(
            ui("chronicle.progress", number(value.coerceAtMost(target).toDouble()), number(target.toDouble())) +
                (achievement.title.takeIf { it.isNotBlank() }?.let { " · " + ui("chronicle.opens", titleName(it)) } ?: ""),
            color = Muted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
