package com.sperance.exileforge.ui.screens.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import com.sperance.exileforge.ui.components.PillTabs
import com.sperance.exileforge.ui.components.SkillGlyph
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.skills.SkillPeek
import com.sperance.exileforge.ui.screens.skills.rememberSkillInspect
import com.sperance.exileforge.ui.theme.*
import kotlin.math.abs

/**
 * Выбор класса: все классы жетонами в ряд, касание выбирает; под ними - имя, роль и сложность выбранного и его сведения
 * вкладками без подписей разделов ([ClassTab]): кто он и как играет, с чем герой начинает, умения и место на дереве - всё, на чём
 * держится единственный выбор, который не отменить. Открытая вкладка переживает смену класса.
 */
@Composable internal fun ClassPicker(index: ContentIndex, classes: List<String>, chosen: String, portraits: Int, enabled: Boolean, onChoose: (String) -> Unit) {
    val guide = remember(index, chosen) { ClassGuide.of(index, chosen) } ?: return
    val ceiling = remember(index, classes) { classes.mapNotNull { ClassGuide.of(index, it) }.flatMap { it.attributes }.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0 }
    var tab by rememberSaveable { mutableStateOf(ClassTab.OVERVIEW) }
    val accent = classAccent(guide)
    ForgePanel(accent = accent) {
        ClassTokens(classes, chosen, portraits, accent, enabled, onChoose)
        ClassHeader(guide, accent)
        PillTabs(ClassTab.entries.map { it.label(guide) }, tab.ordinal, onSelect = { tab = ClassTab.entries[it] })
        when (tab) {
            ClassTab.OVERVIEW -> OverviewTab(guide)
            ClassTab.START -> StartTab(guide, ceiling)
            ClassTab.SKILLS -> SkillsTab(guide)
            ClassTab.TREE -> TreeTab(index, guide, accent)
        }
    }
}

/** Вкладки сведений о классе; у умений - их число. */
private enum class ClassTab(private val key: String) {
    OVERVIEW("chars.class.overview"),
    START("chars.class.start"),
    SKILLS("chars.class.skills"),
    TREE("chars.class.tree"),
    ;

    fun label(guide: ClassGuide): String = if (this == SKILLS) "${ui(key)} ${guide.skills.size}" else ui(key)
}

/** Жетоны классов в ряд на всю ширину: выбранный - в кольце цвета класса, прочие приглушены; касание выбирает класс. */
@Composable private fun ClassTokens(classes: List<String>, chosen: String, portraits: Int, accent: Color, enabled: Boolean, onChoose: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        classes.forEach { code ->
            val on = code == chosen
            ClassPortrait(
                code,
                portraits,
                Modifier.weight(1f).alpha(if (on) 1f else .55f)
                    .clickable(enabled = enabled && !on, role = Role.RadioButton, onClickLabel = classTitle(code)) { onChoose(code) }
                    .semantics { selected = on },
                round = true,
                ring = if (on) accent else Bronze,
            )
        }
    }
}

/** Имя класса, его роль цветом класса и сложность звёздами (полные - из [ClassGuide.MAX_DIFFICULTY]). */
@Composable private fun ClassHeader(guide: ClassGuide, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(classTitle(guide.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            if (guide.role.isNotBlank()) Text(guide.role, color = accent, style = MaterialTheme.typography.labelMedium)
        }
        if (guide.difficulty > 0) {
            val stars = ui("chars.class.difficulty", guide.difficulty, ClassGuide.MAX_DIFFICULTY)
            Row(Modifier.semantics(mergeDescendants = true) { contentDescription = stars }) {
                Text("★".repeat(guide.difficulty), color = Gold, style = MaterialTheme.typography.titleMedium)
                Text("★".repeat(ClassGuide.MAX_DIFFICULTY - guide.difficulty), color = Bronze, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** «Обзор»: как класс играет, сильные и слабые стороны, сборки чипами и кто он - приглушённо. */
@Composable private fun ColumnScope.OverviewTab(guide: ClassGuide) {
    if (guide.style.isNotBlank()) Text(guide.style, color = Parchment, style = MaterialTheme.typography.bodyMedium)
    if (guide.pros.isNotEmpty() || guide.cons.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            guide.pros.forEach { Text("+  $it", color = Vital, style = MaterialTheme.typography.bodySmall) }
            guide.cons.forEach { Text("−  $it", color = LifeRed, style = MaterialTheme.typography.bodySmall) }
        }
    }
    if (guide.builds.isNotEmpty()) Chips(guide.builds)
    if (guide.lore.isNotBlank()) MutedText(guide.lore)
}

/** «Старт»: атрибуты полосами по наибольшему среди классов [ceiling], здоровье и запасы первого уровня ячейками и стартовое оружие. */
@Composable private fun ColumnScope.StartTab(guide: ClassGuide, ceiling: Double) {
    guide.attributes.forEach { (stat, value) ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(statTitle(stat), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(88.dp))
            Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(PanelRaised)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((value / ceiling).toFloat().coerceIn(0f, 1f)).background(attributeColor(stat)))
            }
            Text(statValue(stat, value), color = GoldBright, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(28.dp))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        guide.vitals.forEach { (stat, value) ->
            Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(PanelRaised).padding(6.dp)) {
                Text(statValue(stat, value), color = GoldBright, style = MaterialTheme.typography.titleMedium)
                Text(statTitle(stat), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
    if (guide.weapon.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(ForgeGlyphs.Swords, null, tint = Muted, modifier = Modifier.size(18.dp))
            Text(equipmentTitle(guide.weapon), color = Parchment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Умения класса: строка открывает карточку умения на первом уровне. */
@Composable private fun ColumnScope.SkillsTab(guide: ClassGuide) {
    val inspect = rememberSkillInspect()
    guide.skills.forEach { skill ->
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { inspect(SkillPeek(skill.code, level = 1)) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkillGlyph(skill.icon, Modifier.size(22.dp))
            Text(SkillText.title(skill.code), color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(ui("chars.class.skill_level", skill.unlock), color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** «Древо»: всё древо в малом и ключевые узлы ветки класса; у класса без своей ветки - строка, что он начинает в центре. */
@Composable private fun ColumnScope.TreeTab(index: ContentIndex, guide: ClassGuide, accent: Color) {
    MiniTree(index, guide, accent)
    if (guide.keystones.isEmpty()) {
        Text(ui("chars.class.center"), color = Parchment, style = MaterialTheme.typography.bodySmall)
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
