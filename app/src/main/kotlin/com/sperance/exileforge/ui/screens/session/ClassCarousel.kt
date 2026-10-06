package com.sperance.exileforge.ui.screens.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ClassGuide
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.nodeTitle
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statValue
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.ui.components.ClassPortrait
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.components.ModifierLine
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.SkillGlyph
import com.sperance.exileforge.ui.theme.*
import kotlin.math.abs

/**
 * The class carousel (3.13.0): one class to a card, turned by the arrows or a swipe. The card says who the class is
 * and how it plays, what a level-one hero of it starts with, the skills it learns and where it stands on the tree —
 * everything the one choice that is never undone should rest on.
 */
@Composable internal fun ClassCarousel(index: ContentIndex, classes: List<String>, chosen: String, portraits: Int, enabled: Boolean, onChoose: (String) -> Unit) {
    val at = classes.indexOf(chosen).coerceAtLeast(0)
    val guide = remember(index, chosen) { ClassGuide.of(index, chosen) } ?: return
    val ceiling = remember(index, classes) { classes.mapNotNull { ClassGuide.of(index, it) }.flatMap { it.attributes }.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0 }
    fun turn(step: Int) {
        if (enabled && classes.isNotEmpty()) onChoose(classes[(at + step).mod(classes.size)])
    }
    val accent = classAccent(guide)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CarouselHeader(guide, accent, portraits, onTurn = ::turn)
        Dots(classes.size, at)
        ForgePanel(accent = accent) { RoleSection(guide) }
        ForgePanel(accent = accent) { StartSection(guide, ceiling) }
        ForgePanel(accent = accent) { SkillsSection(guide) }
        ForgePanel(accent = accent) { TreeSection(index, guide, accent) }
    }
}

@Composable private fun CarouselHeader(guide: ClassGuide, accent: Color, portraits: Int, onTurn: (Int) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(accent.copy(alpha = .14f)).border(1.dp, accent.copy(alpha = .5f), shape)
            .pointerInput(guide.code) {
                var drag = 0f
                detectHorizontalDragGestures(onDragStart = { drag = 0f }, onDragEnd = { if (abs(drag) > SWIPE) onTurn(if (drag < 0) 1 else -1) }) { _, delta -> drag += delta }
            }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = { onTurn(-1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, ui("chars.class.prev"), tint = Gold) }
        ClassPortrait(guide.code, portraits, Modifier.width(72.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(classTitle(guide.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            if (guide.role.isNotBlank()) Text(guide.role.uppercase(), color = accent, style = MaterialTheme.typography.labelMedium)
            if (guide.difficulty > 0) {
                Text(
                    ui("chars.class.difficulty") + " " + "★".repeat(guide.difficulty) + "☆".repeat(ClassGuide.MAX_DIFFICULTY - guide.difficulty),
                    color = Muted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        IconButton(onClick = { onTurn(1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, ui("chars.class.next"), tint = Gold) }
    }
}

@Composable private fun Dots(count: Int, at: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        repeat(count) { i -> Box(Modifier.size(7.dp).clip(CircleShape).background(if (i == at) Gold else Bronze)) }
    }
}

@Composable private fun SectionLabel(text: String) = Text(text.uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall)

@Composable private fun ColumnScope.RoleSection(guide: ClassGuide) {
    if (guide.lore.isNotBlank()) {
        SectionLabel(ui("chars.class.lore"))
        Text(guide.lore, color = Parchment, style = MaterialTheme.typography.bodyMedium)
    }
    if (guide.style.isNotBlank()) {
        SectionLabel(ui("chars.class.style"))
        Text(guide.style, color = Parchment, style = MaterialTheme.typography.bodyMedium)
    }
    if (guide.pros.isNotEmpty() || guide.cons.isNotEmpty()) {
        SectionLabel(ui("chars.class.pros_cons"))
        guide.pros.forEach { Text("+  $it", color = Vital, style = MaterialTheme.typography.bodySmall) }
        guide.cons.forEach { Text("−  $it", color = LifeRed, style = MaterialTheme.typography.bodySmall) }
    }
    if (guide.builds.isNotEmpty()) {
        SectionLabel(ui("chars.class.builds"))
        Chips(guide.builds)
    }
}

@Composable private fun ColumnScope.StartSection(guide: ClassGuide, ceiling: Double) {
    SectionLabel(ui("chars.class.attributes"))
    guide.attributes.forEach { (stat, value) ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(statTitle(stat), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(88.dp))
            Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(PanelRaised)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((value / ceiling).toFloat().coerceIn(0f, 1f)).background(attributeColor(stat)))
            }
            Text(statValue(stat, value), color = GoldBright, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(28.dp))
        }
    }
    SectionLabel(ui("chars.class.vitals"))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        guide.vitals.forEach { (stat, value) ->
            Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(PanelRaised).padding(6.dp)) {
                Text(statValue(stat, value), color = GoldBright, style = MaterialTheme.typography.titleMedium)
                Text(statTitle(stat), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
    if (guide.weapon.isNotBlank()) {
        SectionLabel(ui("chars.class.weapon"))
        Text(equipmentTitle(guide.weapon), color = Parchment, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun ColumnScope.SkillsSection(guide: ClassGuide) {
    SectionLabel(ui("chars.class.skills"))
    guide.skills.forEach { skill ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkillGlyph(skill.icon, Modifier.size(22.dp))
            Text(SkillText.title(skill.code), color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(ui("chars.class.skill_level", skill.unlock), color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable private fun ColumnScope.TreeSection(index: ContentIndex, guide: ClassGuide, accent: Color) {
    SectionLabel(ui("chars.class.tree"))
    MiniTree(index, guide, accent)
    if (guide.keystones.isEmpty()) {
        MutedText(ui("chars.class.center"))
    } else {
        guide.keystones.forEach { node ->
            Column(Modifier.border(1.dp, accent.copy(alpha = .4f), RoundedCornerShape(6.dp)).padding(8.dp).fillMaxWidth()) {
                Text(nodeTitle(node.code), color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                node.lines.forEach { line -> ModifierLine(index, line) }
            }
        }
    }
}

/** The whole tree, small: the class's own branch in its colour, its start and keystones marked, the rest dimmed. */
@Composable private fun MiniTree(index: ContentIndex, guide: ClassGuide, accent: Color) {
    val nodes = index.tree.byCode.values
    val bounds = remember(index) { nodes.maxOfOrNull { maxOf(abs(it.x), abs(it.y)) }?.toFloat()?.coerceAtLeast(1f) ?: 1f }
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(Abyss)) {
        val scale = size.minDimension / 2 / (bounds * 1.05f)
        fun at(x: Int, y: Int) = Offset(size.width / 2 + x * scale, size.height / 2 + y * scale)
        nodes.forEach { node ->
            node.connections.forEach { other ->
                index.tree.node(other)?.let { to ->
                    val own = node.code in guide.region && to.code in guide.region
                    drawLine(if (own) accent.copy(alpha = .8f) else Bronze, at(node.x, node.y), at(to.x, to.y), strokeWidth = if (own) 2f else 1.2f)
                }
            }
        }
        nodes.forEach { node ->
            val own = node.code in guide.region
            val (radius, color) = when (node.type) {
                SkillNodeType.START -> (if (node.code == guide.start?.code) 9f else 5f) to (if (node.code == guide.start?.code) GoldBright else Bronze)
                SkillNodeType.KEYSTONE -> 6f to (if (own) Gold else Bronze)
                SkillNodeType.NOTABLE, SkillNodeType.MASTERY, SkillNodeType.JEWEL_SOCKET -> 3.5f to (if (own) Parchment else Bronze)
                else -> 2f to (if (own) accent else Bronze)
            }
            drawCircle(color, radius, at(node.x, node.y))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(chips: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        chips.forEach { text ->
            Text(
                text,
                color = Parchment,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clip(CircleShape).background(PanelRaised).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** A class's colour: its leading attribute's, a class even in all three in gold. */
private fun classAccent(guide: ClassGuide): Color {
    val top = guide.attributes.maxOfOrNull { it.second } ?: return Gold
    val leaders = guide.attributes.filter { it.second == top }
    return if (leaders.size == guide.attributes.size) Gold else attributeColor(leaders.first().first)
}

private fun attributeColor(stat: String): Color = when (stat) {
    CoreStat.STRENGTH.code -> LifeRed
    CoreStat.AGILITY.code -> Vital
    CoreStat.INTELLECT.code -> ManaBlue
    else -> Gold
}

private const val SWIPE = 80f
