package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.BoostSource
import com.sperance.exileforge.core.campaign.LootBreakdown
import com.sperance.exileforge.core.display.atlasPointSource
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.signedNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.quests.QuestTurnIn
import com.sperance.exileforge.rules.content.QuestReward
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Brass
import com.sperance.exileforge.ui.theme.Ember
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.Vital
import com.sperance.exileforge.ui.theme.rarityColor
import kotlin.math.roundToLong

/**
 * Строка награды итогов карты: значок, сумма, подпись и множитель «×1,35 ▾»; нажатие
 * раскрывает разбор [breakdown] - полоса долей, сток, каждый источник с процентом и его долей, итог. Без разбора - просто строка.
 */
@Composable internal fun RewardRow(icon: ImageVector, label: String, tone: Color, breakdown: LootBreakdown) {
    var open by rememberSaveable(label) { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val parts = breakdown.parts
    Column(
        Modifier.fillMaxWidth().background(Abyss, shape).border(1.dp, PanelRaised, shape)
            .clickable(enabled = parts.isNotEmpty(), role = Role.Button) { open = !open }.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = Muted, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text(amount(breakdown.total), color = tone, style = MaterialTheme.typography.titleSmall)
                Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
            }
            if (parts.isNotEmpty()) {
                Text(ui("summary.multiplier", number(breakdown.multiplier)), color = Muted, style = MaterialTheme.typography.labelSmall)
                Text(if (open) "▴" else "▾", color = Muted, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (LocalMotion.current) {
            AnimatedVisibility(open) { Parts(breakdown, tone) }
        } else if (open) {
            Parts(breakdown, tone)
        }
    }
}

/** Разбор: полоса долей (сток и источники), строки «Сток», «Источник +N%» с долей и «Итого». */
@Composable private fun Parts(breakdown: LootBreakdown, tone: Color) {
    val positive = breakdown.parts.filter { it.percent > 0 }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val whole = breakdown.stock + positive.sumOf { breakdown.share(it) }
        if (whole > 0) {
            Row(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))) {
                Box(Modifier.weight((breakdown.stock / whole).toFloat().coerceAtLeast(.01f)).fillMaxHeight().background(Muted))
                positive.forEach { part ->
                    Box(Modifier.weight((breakdown.share(part) / whole).toFloat().coerceAtLeast(.01f)).fillMaxHeight().background(sourceTone(part.source)))
                }
            }
        }
        Line(Muted, ui("summary.stock"), amount(breakdown.stock))
        breakdown.parts.forEach { part ->
            val share = breakdown.share(part)
            Line(sourceTone(part.source), ui("summary.part", ui("summary.source.${part.source.name}"), signedNumber(part.percent)), signedNumber(share.roundToLong().toDouble()))
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            Text(ui("summary.total"), color = Parchment, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(amount(breakdown.total), color = tone, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable private fun Line(dot: Color, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(7.dp).background(dot, CircleShape))
        Text(label, color = Parchment, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        Text(value, color = Parchment, style = MaterialTheme.typography.labelMedium)
    }
}

/** Цвет доли источника в полосе разбора. */
private fun sourceTone(source: BoostSource): Color = when (source) {
    BoostSource.HERO -> Ember
    BoostSource.ATLAS -> Rune
    BoostSource.MAP -> rarityColor("RARE")
    BoostSource.VAAL -> Color(0xFFFF8A78)
    BoostSource.PACT -> Color(0xFFC9A0FF)
    BoostSource.POTION -> Vital
}

private fun amount(value: Double): String = number(value.roundToLong().toDouble())

/**
 * «Сданы задания»: задания, что сдались разом после
 * карты, - имя задания на всю ширину, под ним награда фишками; между заданиями тонкая черта. «Всего за задания» - только при двух
 * и больше: у одного задания она повторяла бы его фишки.
 */
@Composable internal fun TurnedInBlock(turnedIn: List<QuestTurnIn>) {
    if (turnedIn.isEmpty()) return
    SummaryCard(ui("summary.quests"), turnedIn.size.toString(), Vital) {
        turnedIn.forEachIndexed { i, quest ->
            if (i > 0) HorizontalDivider(thickness = 1.dp, color = PanelRaised)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✓", color = Vital, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(quest.title, color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
                RewardChips(quest.rewards, Modifier.padding(start = 18.dp))
            }
        }
        if (turnedIn.size < 2) return@SummaryCard
        val all = turnedIn.map { it.rewards }
        val total = QuestReward(
            gold = all.sumOf { it.gold },
            experience = all.sumOf { it.experience },
            orbs = all.flatMap { it.orbs.entries }.groupBy({ it.key }, { it.value }).mapValues { it.value.sum() },
        )
        HorizontalDivider(thickness = 1.dp, color = PanelRaised)
        Text(ui("summary.quests_total"), color = Muted, style = MaterialTheme.typography.labelMedium)
        RewardChips(total)
    }
}

/** Награда задания фишками: золото, опыт, сферы - с начала строки, с переносом. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RewardChips(reward: QuestReward, modifier: Modifier = Modifier) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (reward.gold > 0) Chip(ui("quest.toast.gold", number(reward.gold.toDouble())), GoldBright)
        if (reward.experience > 0) Chip(ui("quest.toast.experience", number(reward.experience)), Rune)
        reward.orbs.forEach { (code, amount) -> Chip(ui("quest.toast.orb", itemTitle(code), amount), Brass) }
    }
}

@Composable private fun Chip(text: String, tone: Color) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        color = tone,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        modifier = Modifier.background(tone.copy(alpha = .1f), shape).border(1.dp, tone.copy(alpha = .35f), shape).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

/**
 * «Очки атласа»: очки, что принесли ответы карты или испытания, - «+1 очко атласа — босс Склепа»; в итогах карты и
 * испытаний.
 */
@Composable internal fun AtlasBlock(keys: List<String>) {
    if (keys.isEmpty()) return
    SummaryCard(ui("summary.atlas"), "+${keys.size}", Rune) {
        keys.forEach { key ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("+1", Rune)
                Text(ui("atlas.earned.line", atlasPointSource(key)), color = Parchment, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Новый блок итогов - в рамке своего цвета: подпись со счётом и строки. */
@Composable private fun SummaryCard(title: String, count: String, tone: Color, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier.fillMaxWidth().background(Abyss, shape).border(1.dp, tone.copy(alpha = .45f), shape).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Caption(title)
            Text(count, color = tone, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        content()
    }
}
