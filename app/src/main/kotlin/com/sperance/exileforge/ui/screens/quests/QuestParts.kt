package com.sperance.exileforge.ui.screens.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Quest
import com.sperance.exileforge.rules.content.QuestKind
import com.sperance.exileforge.rules.content.QuestReward
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.GoldPrice
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.raritySpine
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.theme.*

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

/** The reward as chips: gold, experience, orbs, and for a guild's quest the guild's experience; сфера открывает карточку. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RewardChips(reward: QuestReward) {
    val inspect = rememberInspect()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Chip { GoldPrice(reward.gold) }
        if (reward.experience > 0) Chip { Text(ui("quest.reward_xp", number(reward.experience)), color = Vital, style = MaterialTheme.typography.labelMedium) }
        reward.orbs.forEach { (code, amount) -> Chip({ inspect(Inspect.Stack(code)) }) { Text(ui("quest.reward_orb", itemTitle(code), amount), color = Parchment, style = MaterialTheme.typography.labelMedium) } }
        if (reward.guildExperience > 0) Chip { Text(ui("quest.reward_guild_xp", number(reward.guildExperience.toDouble())), color = Rune, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable private fun Chip(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(PanelRaised).then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier).padding(horizontal = 6.dp, vertical = 2.dp)) { content() }
}

@Composable internal fun QuestBar(progress: Long, target: Long, color: Color, thickness: Dp = 6.dp) {
    val share = if (target <= 0) 1f else (progress.toFloat() / target).coerceIn(0f, 1f)
    LinearProgressIndicator(progress = { share }, modifier = Modifier.fillMaxWidth().height(thickness), color = color, trackColor = Bronze)
}

/**
 * Задание двухстрочной карточкой (~58dp): строка 1 - точка редкости,
 * имя и «7 / 10»; строка 2 - награды мелкими фишками (сфера открывает карточку), срок, условия значками ModBlue и место;
 * прогресс - кромкой 3dp по низу; [actions] - справа на обе строки (сдать, взять и сбросить, отказаться). Сданное - серым с ✓.
 * Описание шага сюжета - по касанию.
 *
 * Части задания - облик «книги учёта»: строка с кромкой и пилюлей редкости, цель, где она считается, условия синим, полоса, награда и
 * действия; прогресс - кромкой по низу. [modifier] - облик места, где строка лежит (гвоздик листка доски).
 */
@Composable internal fun QuestRow(quest: Quest, modifier: Modifier = Modifier, actions: @Composable ColumnScope.() -> Unit = {}) {
    val color = if (quest.claimed) Muted else rarityColor(quest.rarity.name)
    val shape = RoundedCornerShape(8.dp)
    val story = quest.kind == QuestKind.STORY
    var told by remember(quest.id) { mutableStateOf(false) }
    val share = if (quest.target <= 0) 1f else (quest.progress.toFloat() / quest.target).coerceIn(0f, 1f)
    Row(
        modifier.fillMaxWidth().greyed(quest.claimed).clip(shape).depthPanel(shape).raritySpine(color, 3.dp)
            .border(1.dp, if (quest.done && !quest.claimed) Vital.copy(alpha = .55f) else Bronze, shape)
            .drawBehind {
                val edge = QUEST_EDGE.toPx()
                drawRect(Bronze, Offset(0f, size.height - edge), Size(size.width, edge))
                drawRect(color, Offset(0f, size.height - edge), Size(size.width * share, edge))
            }
            .then(if (story) Modifier.clickable { told = !told } else Modifier)
            .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Text(
                    questTitle(quest),
                    color = if (quest.claimed) Muted else GoldBright,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(ui("quest.progress", number(quest.progress.toDouble()), number(quest.target.toDouble())), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            QuestFacts(quest)
            if (story && told) MutedText(loc("quest.story.${quest.goal}.description"), style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (quest.claimed) Icon(Icons.Outlined.Check, ui("quest.claimed"), tint = Vital, modifier = Modifier.size(18.dp)) else actions()
        }
    }
}

/** Строка 2 карточки задания: награды, срок, условия и место - мелко, с переносом. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestFacts(quest: Quest) {
    val inspect = rememberInspect()
    val reward = quest.reward
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp), itemVerticalAlignment = Alignment.CenterVertically) {
        Fact(ForgeGlyphs.Coins, compactNumber(reward.gold.toDouble()), GoldBright)
        if (reward.experience > 0) Text(ui("quest.reward_xp_short", compactNumber(reward.experience)), color = Vital, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        reward.orbs.forEach { (code, amount) ->
            Row(Modifier.clickable { inspect(Inspect.Stack(code)) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                BagIcon(code, Modifier.size(14.dp), kind = ItemVisualKind.CURRENCY)
                Text(ui("quest.reward_orb_short", amount), color = Parchment, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
        if (reward.guildExperience > 0) Text(ui("quest.reward_guild_xp", number(reward.guildExperience.toDouble())), color = Rune, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        if (quest.expiresAt > 0 && !quest.claimed) {
            val soon = quest.expiresAt - System.currentTimeMillis() < SOON_MS
            Fact(Icons.Outlined.Schedule, untilText(quest.expiresAt), if (soon) Caution else Muted)
        }
        quest.conditions.forEach { condition -> Badge(ui("quest.condition.${condition.name}"), ModBlue) }
        placeTitle(quest.place)?.takeIf { quest.kind != QuestKind.STORY }?.let { Fact(Icons.Outlined.Place, it, Muted) }
    }
}

@Composable private fun Fact(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(12.dp))
        Text(text, color = tint, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable private fun Badge(text: String, tint: Color) {
    Text(
        text,
        color = tint,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        modifier = Modifier.border(1.dp, tint.copy(alpha = .35f), RoundedCornerShape(5.dp)).padding(horizontal = 4.dp),
    )
}

/** Кромка прогресса по низу карточки задания. */
private val QUEST_EDGE = 3.dp

/** Срок задания короче часа - тревожным цветом. */
private const val SOON_MS = 3_600_000L

/** Серая «отработанная» строка: без цвета и вполсилы. */
private fun Modifier.greyed(on: Boolean): Modifier = if (!on) {
    this
} else {
    graphicsLayer { alpha = GREYED_ALPHA }.drawWithContent {
        val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), paint)
            drawContent()
            canvas.restore()
        }
    }
}

private const val GREYED_ALPHA = .55f
