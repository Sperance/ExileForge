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
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.core.model.guild.nextRank
import com.sperance.exileforge.presentation.guild.GuildViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * Вклад в гильдию (4.8.0) - только золото: целиком идёт в казну, в опыт гильдии и в личный вклад героя, то есть в его ранг;
 * у дня свой предел по уровню героя и дереву гильдии, остаток на сегодня говорит сервер.
 * Ниже - казна, путь героя к следующему рангу и вкладчики недели.
 */
@Composable internal fun ContributeTab(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    var amount by remember { mutableStateOf("") }
    val count = amount.toLongOrNull() ?: 0L
    val have = game.hero?.money
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            ForgePanel {
                Engraved(ui("guild.give"))
                OutlinedTextField(
                    amount,
                    { amount = it.filter(Char::isDigit).take(12) },
                    label = { Text(ui("auction.amount")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { have?.let { Text(ui("guild.give_have", number(it.toDouble()))) } },
                )
                // Суточный лимит и остаток считает сервер по одной формуле `GuildRules.dailyLimit` (с бонусом древа)
                val left = me?.dayLeft
                me?.let {
                    PropertyRow(ui("guild.daily_limit"), number(it.dayLimit.toDouble()), Glyph.LEVEL)
                    MutedText(ui("guild.daily_left", number(it.dayLeft.toDouble())))
                }
                // «Макс.»: сколько золота влезает в сегодняшний остаток и в кошелёк героя
                val most = listOfNotNull(left, have).minOrNull()
                if (most != null && most > 0) {
                    ForgeTextButton(enabled = !game.busy, onClick = { amount = most.toString() }) { Text(ui("guild.give_max", number(most.toDouble()))) }
                }
                val over = left != null && count > left
                val short = have != null && count > have
                if (short) Text(ui("guild.give_short"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                if (over) Text(ui("guild.give_over_limit", number((left ?: 0L).toDouble())), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                // Кнопка остаётся на экране: после каждого удержания она снова взводится, поле чистится только принятым вкладом
                HoldButton(
                    ui("guild.give_do"),
                    Gold,
                    Modifier.fillMaxWidth(),
                    enabled = !game.busy && count > 0 && !short && !over,
                    rearm = true,
                    icon = ForgeGlyphs.Coins,
                ) {
                    vm.contribute(count) { amount = "" }
                }
            }
        }
        me?.let { item { RankPanel(game, it) } }
        item {
            ForgePanel {
                Engraved(ui("guild.treasury"))
                PropertyRow(ui("guild.gold"), number(guild.treasuryGold.toDouble()), Glyph.CURRENCY)
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
