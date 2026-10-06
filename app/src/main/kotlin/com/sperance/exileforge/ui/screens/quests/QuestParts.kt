package com.sperance.exileforge.ui.screens.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Quest
import com.sperance.exileforge.rules.content.QuestKind
import com.sperance.exileforge.rules.content.QuestReward
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.GoldPrice
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.RaritySpine
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.theme.*

/*
 * The pieces of a quest (3.23.0) — the board's «ledger» look: a row with the rarity's edge and pill, the goal, where it
 * counts, its conditions in the modifiers' blue, the bar, the reward and whatever may be done with it.
 */

/** What the quest asks, in the server's words: a story step by its own name, any other goal with its number. */
internal fun questTitle(quest: Quest): String = if (quest.kind == QuestKind.STORY) loc("quest.story.${quest.goal}.name") else goalTitle(quest.goal, quest.target)

internal fun goalTitle(goal: String, target: Long): String = loc("quest.goal.$goal", listOf(number(target.toDouble())))

/** Where the quest counts: a zone of the world map or a region; nothing for a quest that counts anywhere. */
internal fun placeTitle(place: String): String? = when {
    place.isBlank() -> null
    place.startsWith("REGION_") -> loc("region.$place.name")
    else -> loc("map.$place.name")
}

/** A quest's rarity in a quest's words — its own labels, since «Волшебное задание» agrees with a noun an item does not. */
internal fun questRarity(rarity: Rarity): String = ui("quest.rarity.${rarity.name}")

@Composable internal fun RarityPill(rarity: Rarity) {
    val color = rarityColor(rarity.name)
    Text(
        questRarity(rarity),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.border(1.dp, color.copy(alpha = .6f), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 1.dp),
    )
}

/** The reward as chips: gold, experience, orbs, and for a guild's quest the guild's experience. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RewardChips(reward: QuestReward) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Chip { GoldPrice(reward.gold) }
        if (reward.experience > 0) Chip { Text(ui("quest.reward_xp", number(reward.experience)), color = Vital, style = MaterialTheme.typography.labelMedium) }
        reward.orbs.forEach { (code, amount) -> Chip { Text(ui("quest.reward_orb", itemTitle(code), amount), color = Parchment, style = MaterialTheme.typography.labelMedium) } }
        if (reward.guildExperience > 0) Chip { Text(ui("quest.reward_guild_xp", number(reward.guildExperience.toDouble())), color = Rune, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable private fun Chip(content: @Composable () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(PanelRaised).padding(horizontal = 6.dp, vertical = 2.dp)) { content() }
}

@Composable internal fun QuestBar(progress: Long, target: Long, color: Color) {
    val share = if (target <= 0) 1f else (progress.toFloat() / target).coerceIn(0f, 1f)
    LinearProgressIndicator(progress = { share }, modifier = Modifier.fillMaxWidth().height(6.dp), color = color, trackColor = Bronze)
}

/**
 * One quest as a row of the ledger. [actions] is what the screen offers for it — claim, replace, take, abandon;
 * a claimed quest is drawn dimmed, with a stamp instead.
 */
@Composable internal fun QuestRow(quest: Quest, actions: @Composable RowScope.() -> Unit = {}) {
    val color = rarityColor(quest.rarity.name)
    val shape = RoundedCornerShape(8.dp)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(shape).depthPanel(shape)) {
        RaritySpine(if (quest.claimed) Muted else color)
        Column(Modifier.weight(1f).padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(questTitle(quest), color = if (quest.claimed) Muted else GoldBright, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                RarityPill(quest.rarity)
            }
            if (quest.kind == QuestKind.STORY) MutedText(loc("quest.story.${quest.goal}.description"))
            placeTitle(quest.place)?.takeIf { quest.kind != QuestKind.STORY }?.let { MutedText(ui("quest.place", it)) }
            quest.conditions.forEach { Text("◆ " + ui("quest.condition.${it.name}"), color = ModBlue, style = MaterialTheme.typography.bodySmall) }
            QuestBar(quest.progress, quest.target, color)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val left = if (quest.expiresAt > 0 && !quest.claimed) " · " + ui("quest.expires", untilText(quest.expiresAt)) else ""
                MutedText(ui("quest.progress", number(quest.progress.toDouble()), number(quest.target.toDouble())) + left, Modifier.weight(1f))
                if (quest.claimed) Text(ui("quest.claimed"), color = Vital, style = MaterialTheme.typography.labelMedium)
            }
            RewardChips(quest.reward)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = actions,
            )
        }
    }
}
