package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Brass
import com.sperance.exileforge.ui.theme.Ember
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.Vital
import com.sperance.exileforge.ui.theme.rarityColor
import kotlin.math.roundToLong

/**
 * Плитка наград итогов: значок [icon] (цвета [mark]), итог [value] цветом [tone], подпись [label] и пометка в углу [corner]
 * (множитель разбора); [panel] - разбор, что плитка раскрывает под сеткой, null - плитка не нажимается.
 */
internal class SummaryTile(
    val id: String,
    val icon: ImageVector,
    val value: String,
    val label: String,
    val tone: Color,
    val mark: Color = Muted,
    val corner: String? = null,
    val panel: (@Composable () -> Unit)? = null,
)

/**
 * Плитки наград по две в ряд: итог каждой виден сразу; нажатая раскрывает свой разбор под сеткой - один за раз, повторное нажатие
 * сворачивает. Открыт разбор первой плитки, что его несёт. Плиток столько, сколько наград пришло: нечётная встаёт на полряда.
 */
@Composable internal fun RewardTiles(tiles: List<SummaryTile>) {
    if (tiles.isEmpty()) return
    var open by rememberSaveable { mutableStateOf(tiles.firstOrNull { it.panel != null }?.id) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { tile -> Tile(tile, open == tile.id, Modifier.weight(1f).fillMaxHeight()) { open = if (open == tile.id) null else tile.id } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        val panel = tiles.firstOrNull { it.id == open }?.panel
        if (panel != null) {
            val shape = RoundedCornerShape(10.dp)
            Box(Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, PanelRaised, shape).padding(10.dp)) { panel() }
        }
    }
}

/** Одна плитка: значок, итог, подпись и пометка в углу; выбранная - в золотой рамке с отсветом. */
@Composable private fun Tile(tile: SummaryTile, chosen: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val fill = if (chosen) Brush.verticalGradient(listOf(Gold.copy(alpha = .12f), Abyss)) else SolidColor(Abyss)
    Box(
        modifier.heightIn(min = 54.dp).clip(shape).background(fill, shape).border(1.dp, if (chosen) Gold else PanelRaised, shape)
            .clickable(enabled = tile.panel != null, role = Role.Button, onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(tile.icon, null, tint = tile.mark, modifier = Modifier.size(18.dp))
            Column {
                Text(tile.value, color = tile.tone, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tile.label, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        tile.corner?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopEnd)) }
    }
}

/** Разбор: полоса долей (сток и источники), строки «Без бонусов», «Источник +N%» с долей и «Итого»; под ними - оговорка [note]. */
@Composable internal fun BreakdownParts(breakdown: LootBreakdown, tone: Color, note: String? = null) {
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
        note?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall) }
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
    BoostSource.LEVEL -> Brass
    BoostSource.FALLOFF -> Muted
}

private fun amount(value: Double): String = number(value.roundToLong().toDouble())

/**
 * Сданные разом после карты задания двумя разделами: «За эту карту» - их продвинул заход, «Также сдано» - сдалось заодно, с меткой
 * вида; раздел без заданий не пишется. Задание - имя на всю ширину, под ним награда фишками; между заданиями тонкая черта. «Всего за
 * задания» - только при двух и больше: у одного задания она повторяла бы его фишки.
 */
@Composable internal fun TurnedInLines(turnedIn: List<QuestTurnIn>) {
    val (run, also) = turnedIn.partition { it.run }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TurnInSection(ui("summary.quests_run"), run, tagged = false)
        TurnInSection(ui("summary.quests_also"), also, tagged = true)
        if (turnedIn.size < 2) return@Column
        HorizontalDivider(thickness = 1.dp, color = PanelRaised)
        Text(ui("summary.quests_total"), color = Muted, style = MaterialTheme.typography.labelMedium)
        val all = turnedIn.map { it.rewards }
        RewardChips(
            QuestReward(
                gold = all.sumOf { it.gold },
                experience = all.sumOf { it.experience },
                orbs = all.flatMap { it.orbs.entries }.groupBy({ it.key }, { it.value }).mapValues { it.value.sum() },
            ),
        )
    }
}

/** Раздел сданного: заголовок с чертой до края и задания; [tagged] - у каждого метка вида (`summary.quest_kind.<вид>`). */
@Composable private fun TurnInSection(title: String, quests: List<QuestTurnIn>, tagged: Boolean) {
    if (quests.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title.uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = PanelRaised)
    }
    quests.forEachIndexed { i, quest ->
        if (i > 0) HorizontalDivider(thickness = 1.dp, color = PanelRaised)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("✓", color = Vital, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(quest.title, color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                if (tagged) KindTag(ui("summary.quest_kind.${quest.kind.name}"))
            }
            RewardChips(quest.rewards, Modifier.padding(start = 18.dp))
        }
    }
}

/** Метка вида задания: мелко, в тонкой рамке, в одну строку. */
@Composable private fun KindTag(text: String) {
    val shape = RoundedCornerShape(6.dp)
    Text(text, color = Rune, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, modifier = Modifier.border(1.dp, PanelRaised, shape).padding(horizontal = 5.dp))
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
    SummaryCard(ui("summary.atlas"), "+${keys.size}", Rune) { AtlasLines(keys) }
}

/** Очки атласа строками: «+1» и откуда очко. */
@Composable internal fun AtlasLines(keys: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
        Caption(title) { Text(count, color = tone, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1) }
        content()
    }
}
