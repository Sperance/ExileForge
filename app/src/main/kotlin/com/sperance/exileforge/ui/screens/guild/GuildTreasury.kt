package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.GUILD_GOLD
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.core.model.guild.nextRank
import com.sperance.exileforge.presentation.guild.GuildViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.orbArt
import com.sperance.exileforge.ui.theme.*

/**
 * Giving to the guild: gold or an orb of the bag, counted at the orb's price. The whole of it goes to the treasury, to the
 * guild's experience and to the hero's own contribution, which is their rank; a day holds a limit by the hero's level.
 * Under it, the treasury, the hero's way to the next rank and the week's givers.
 */
@Composable internal fun ContributeTab(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    val rules = game.index?.guilds
    val orbs = game.orbs.filter { (game.bagAmount(it.code.value) ?: 0L) > 0 }
    // The crafts' materials (3.79.0, server 1.74.0): they grow the guild at their price, the treasury does not keep them.
    val materials = game.index?.items?.values.orEmpty().filter { it.category in DONATED_STOCK && it.price > 0 && (game.bagAmount(it.code.value) ?: 0L) > 0 }.sortedBy { it.price }
    var item by remember { mutableStateOf(GUILD_GOLD) }
    var amount by remember { mutableStateOf("") }
    val count = amount.toLongOrNull() ?: 0L
    val have = if (item == GUILD_GOLD) game.hero?.money else game.bagAmount(item)
    val price = if (item == GUILD_GOLD) 1L else (orbs + materials).firstOrNull { it.code.value == item }?.price ?: 0L
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            ForgePanel {
                Engraved(ui("guild.give"))
                Spinner(
                    ui("guild.give_what"),
                    item,
                    mapOf(GUILD_GOLD to ui("guild.gold")) + (orbs + materials).associate { it.code.value to itemTitle(it.code.value) },
                    !game.busy,
                    glyph = Glyph.CURRENCY,
                    optionArt = orbArt(game.orbs),
                ) {
                    item = it
                    amount = ""
                }
                OutlinedTextField(
                    amount,
                    { amount = it.filter(Char::isDigit).take(12) },
                    label = { Text(ui("auction.amount")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { have?.let { Text(ui("guild.give_have", number(it.toDouble()))) } },
                )
                if (item != GUILD_GOLD && count > 0) PropertyRow(ui("guild.give_worth"), number((count * price).toDouble()), Glyph.CURRENCY)
                rules?.let { PropertyRow(ui("guild.daily_limit"), number(it.dailyLimit(game.heroLevel).toDouble()), Glyph.LEVEL) }
                if (have != null && count > have) Text(ui("guild.give_short"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                HoldButton(
                    ui("guild.give_do"),
                    Gold,
                    Modifier.fillMaxWidth(),
                    enabled = !game.busy && count > 0 && (have == null || count <= have),
                    icon = ForgeGlyphs.Coins,
                ) {
                    vm.contribute(item, count)
                    amount = ""
                }
            }
        }
        me?.let { item { RankPanel(game, it) } }
        item {
            ForgePanel {
                Engraved(ui("guild.treasury"))
                PropertyRow(ui("guild.gold"), number(guild.treasuryGold.toDouble()), Glyph.CURRENCY)
                guild.treasuryOrbs.filterValues { it > 0 }.forEach { (code, n) -> PropertyRow(itemTitle(code), number(n.toDouble()), Glyph.CURRENCY) }
            }
        }
        item { WeekPanel(guild, me) }
    }
}

/** The hero's rank and the way to the next: its threshold, the bar, and what remains. */
@Composable private fun RankPanel(game: GameUi, me: GuildMember) {
    val rules = game.index?.guilds
    ForgePanel(accent = Rune) {
        Engraved(ui("guild.my_contribution"), Rune)
        PropertyRow(ui("guild.rank"), GuildText.rank(me.rank), Glyph.RARITY)
        PropertyRow(ui("guild.contribution_all"), number(me.contribution.toDouble()), Glyph.CURRENCY)
        PropertyRow(ui("guild.contribution_week"), number(me.weekContribution.toDouble()), Glyph.CURRENCY)
        val next = rules?.nextRank(me.contribution)
        if (rules != null && next != null) {
            val from = rules.rankFor(me.contribution).from
            LinearProgressIndicator(
                progress = { ((me.contribution - from).toFloat() / (next.from - from).coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = Rune,
                trackColor = PanelRaised,
            )
            MutedText(ui("guild.rank_next", GuildText.rank(next.code), number((next.from - me.contribution).toDouble())))
        } else if (rules != null) {
            MutedText(ui("guild.rank_top"))
        }
    }
}

/** This week's givers, most first: the guild's own table, or the members' rows where the table is empty. */
@Composable private fun WeekPanel(guild: GuildView, me: GuildMember?) {
    val names = guild.members.associate { it.heroId to it.name }
    val week = guild.weekly.ifEmpty { guild.members.associate { it.heroId to it.weekContribution } }
        .filterValues { it > 0 }.entries.sortedByDescending { it.value }
    ForgePanel {
        Engraved(ui("guild.week_table"))
        if (week.isEmpty()) MutedText(ui("guild.week_empty"))
        week.forEachIndexed { place, (heroId, value) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${place + 1}.", color = Muted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(28.dp))
                Text(
                    names[heroId] ?: "…${heroId.takeLast(6)}",
                    color = if (heroId == me?.heroId) GoldBright else Parchment,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                GoldPrice(value)
            }
        }
    }
}

/** The bag's stocks a guild takes besides gold and orbs (3.79.0): the crafts' materials. */
private val DONATED_STOCK = setOf("MATERIAL", "STONE_STOCK", "WOOD_STOCK")
