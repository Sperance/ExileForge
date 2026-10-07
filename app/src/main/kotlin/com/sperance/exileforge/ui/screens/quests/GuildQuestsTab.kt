package com.sperance.exileforge.ui.screens.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.presentation.quests.QuestViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.GuildGoalView
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The guild's quests (3.23.0): the hero's own two for the day, then the common goals of the day and of the week. A common
 * goal fills with every member's counters; once it is full, each member who gave at least the threshold claims a share,
 * and the first claim brings the guild its experience. Under a goal, who gave most.
 */
@Composable internal fun GuildQuestsTab(game: GameUi, guild: GuildView) {
    val vm = koinViewModel<QuestViewModel>()
    val boards by vm.quests.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    val busy = activity.busy
    LaunchedEffect(game.heroId, guild.id) { vm.openGuild() }
    val quests = boards.guild
    val names = guild.members.associate { it.heroId to it.name }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (quests == null) {
            item { MutedText(ui("quest.loading")) }
            return@LazyColumn
        }
        item { Section(ui("quest.guild_personal"), ui("quest.resets", untilText(quests.dayEndsAt))) }
        if (quests.personal.isEmpty()) item { MutedText(ui("quest.none")) }
        items(quests.personal, key = { it.id }) { quest ->
            QuestRow(quest) {
                if (quest.done && !quest.claimed) ForgeButton({ vm.claimGuild(questId = quest.id) }, enabled = !busy) { Text(ui("quest.claim")) }
            }
        }
        goals(game.heroId, vm, busy, ui("quest.guild_daily"), ui("quest.resets", untilText(quests.dayEndsAt)), quests.daily, names)
        goals(game.heroId, vm, busy, ui("quest.guild_weekly"), ui("quest.resets", weekText(quests.weekEndsAt)), quests.weekly, names)
    }
}

@Composable private fun Section(title: String, note: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Engraved(title, modifier = Modifier.weight(1f))
        MutedText(note)
    }
}

private fun LazyListScope.goals(me: String, vm: QuestViewModel, busy: Boolean, title: String, note: String, goals: List<GuildGoalView>, names: Map<String, String>) {
    item { Section(title, note) }
    if (goals.isEmpty()) item { MutedText(ui("quest.none")) }
    items(goals, key = { it.goal.key }) { GoalRow(me, vm, busy, it, names) }
}

/** A common goal: the whole guild's bar, the hero's part against the threshold, the share and the best givers. */
@Composable private fun GoalRow(me: String, vm: QuestViewModel, busy: Boolean, view: GuildGoalView, names: Map<String, String>) {
    val goal = view.goal
    val color = rarityColor(goal.rarity.name)
    val shape = RoundedCornerShape(8.dp)
    val done = view.progress >= goal.target
    Row(Modifier.fillMaxWidth().clip(shape).depthPanel(shape).raritySpine(if (view.claimed) Muted else color)) {
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f).padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(goalTitle(goal.goal, goal.target), color = if (view.claimed) Muted else GoldBright, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                RarityPill(goal.rarity)
            }
            QuestBar(view.progress, goal.target, color)
            MutedText(ui("quest.progress", number(view.progress.coerceAtMost(goal.target).toDouble()), number(goal.target.toDouble())))
            Text(
                ui("quest.guild_mine", number(view.mine.toDouble()), number(view.need.toDouble())),
                color = if (view.mine >= view.need) Vital else Parchment,
                style = MaterialTheme.typography.bodySmall,
            )
            RewardChips(view.reward.copy(guildExperience = goal.guildExperience))
            view.contributions.entries.sortedByDescending { it.value }.take(TOP).forEachIndexed { place, (heroId, amount) ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MutedText("${place + 1}.")
                    Text(
                        names[heroId] ?: "—",
                        color = if (heroId == me) Gold else Parchment,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    MutedText(number(amount.toDouble()))
                }
            }
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                when {
                    view.claimed -> Text(ui("quest.claimed"), color = Vital, style = MaterialTheme.typography.labelMedium)
                    done && view.mine >= view.need -> ForgeButton({ vm.claimGuild(goal = goal.key) }, enabled = !busy) { Text(ui("quest.claim_share")) }
                    done -> MutedText(ui("quest.guild_short"))
                }
            }
        }
    }
}

/** Givers shown under a goal. */
private const val TOP = 5
