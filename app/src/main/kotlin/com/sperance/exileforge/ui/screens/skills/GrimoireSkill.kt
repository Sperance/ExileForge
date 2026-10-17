package com.sperance.exileforge.ui.screens.skills

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.BeltFlask
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.draught
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.SkillGrowthView
import com.sperance.exileforge.core.display.SkillPace
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SkillRules
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.sheet.FlaskKind
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.Caption
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** Страница навыка: лист с ценой и строками, действия ячеек, выбор навыка и условия. */
/**
 * A skill's page opened: what it does now — or at its first level, unlearned — and at the next, the slots it may go in.
 * - иконка «Неон рун» с тиром, полоса опыта, книга в опыт, гнёзда рун и ритуал следующего тира; строки - умение в бою (тир и руны).
 *
 * Только ячейки своего вида: пассивное не предлагает активную ячейку.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SkillSheet(
    game: GameUi,
    vm: GrimoireViewModel,
    index: ContentIndex,
    skill: SkillDefinition,
    skills: HeroSkills,
    heroLevel: Int,
    stats: Map<String, Double>,
    onDismiss: () -> Unit,
) {
    val view = remember(skill, skills, heroLevel, stats) { SkillGrowthView.of(index, skill, skills, heroLevel, stats) }
    val forged = remember(skill, skills, stats) { index.skillForge.effective(skill, skills, stats) }
    var socket by remember(skill.code) { mutableStateOf<Int?>(null) }
    val learned = view.level
    val next = (learned + 1).coerceAtMost(view.cap)
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NeonSkillIcon(skill, view.tier, 56.dp, dim = learned == 0)
                Column(Modifier.weight(1f)) {
                    Text(SkillText.title(skill.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
                    Text(skillKindLine(skill) + " · " + tierLine(view), color = Muted, style = MaterialTheme.typography.labelMedium)
                }
                Text(if (learned > 0) ui("skills.level_short", learned) else "—", color = Gold, style = MaterialTheme.typography.titleLarge)
            }
            if (learned > 0) XpBar(index, skill, view)
            SkillFacts(index, forged, learned.coerceAtLeast(1), stats, heroLevel)
            if (learned in 1 until view.cap) {
                Caption(ui("skills.at_level", next))
                SkillFacts(index, forged, next, stats, heroLevel, ModBlue.copy(alpha = .75f))
            }
            if (!view.capped || learned == 0) {
                BookButton(game, index, skill, view, heroLevel, stats) {
                    onDismiss()
                    vm.learnSkill(skill.code)
                }
            }
            if (learned > 0) {
                RuneSockets(game, vm, skill, view) { socket = it }
                RitualCard(game, vm, view)
                SlotActions(game, index, skill, skills, heroLevel) { kind, at, put ->
                    onDismiss()
                    vm.slotSkill(kind.name, at, if (put) skill.code else null)
                }
            }
        }
    }
    socket?.let { at -> RunePicker(game, vm, index, skill, skills, at, stats) { socket = null } }
}

/**
 * What a skill does at [level]: its price and pace, then every line. Подготовки нет: темп - время
 * применения и перезарядка по листу героя [stats] уровня [heroLevel], цена маны (атаки - от уровня героя) и строка «цена в секунду
 * / ваша регенерация». The grimoire's page and the fight's sheet both read it; a slotted skill's own [condition] stands in
 * for the page's default.
 */
@Composable internal fun SkillFacts(
    index: ContentIndex,
    skill: SkillDefinition,
    level: Int,
    stats: Map<String, Double>,
    heroLevel: Int,
    tone: Color = ModBlue,
    condition: SlotCondition? = null,
) {
    val hero = remember(stats, heroLevel) { Combatant(stats, heroLevel, index.campaign.combat) }
    val pace = remember(skill, level, hero) { SkillText.pace(skill, level, hero) }
    Price(skill, pace, condition)
    SkillLines(SkillText.lines(skill, level, index.campaign.combat) + listOfNotNull(pace?.let(SkillText::manaRate)), tone)
}

/** An active skill's price and pace (цена, применение, перезарядка у героя), an aura's reserve: the chips over its lines. */
@Composable internal fun Price(skill: SkillDefinition, pace: SkillPace?, condition: SlotCondition? = null) {
    val chips = listOfNotNull(
        pace?.let { ui("skills.cost", fineNumber(it.mana)) },
        pace?.let { ui("skills.cast", fineNumber(it.cast)) },
        pace?.cooldown?.takeIf { it > 0 }?.let { ui("skills.cooldown", fineNumber(it)) },
        (condition ?: skill.condition).takeIf { skill.kind == SkillKind.ACTIVE }?.let { ui("skills.default_condition", conditionTitle(it)) },
    )
    if (chips.isNotEmpty()) Text(chips.joinToString(" · "), color = Rune, style = MaterialTheme.typography.labelMedium)
}

@Composable internal fun SkillLines(lines: List<String>, tone: Color = ModBlue) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        lines.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Rhombus(tone, 4.dp)
                Text(line, color = tone, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Where a learned skill may go: each open slot of its kind, and out of the one it stands in. */
@Composable internal fun SlotActions(
    game: GameUi,
    index: ContentIndex,
    skill: SkillDefinition,
    skills: HeroSkills,
    heroLevel: Int,
    onSlot: (SkillKind, Int, Boolean) -> Unit,
) {
    val open = if (skill.kind == SkillKind.ACTIVE) index.skillRules.activeSlots(heroLevel) else index.skillRules.passiveSlots(heroLevel)
    val standing = if (skill.kind == SkillKind.ACTIVE) skills.active.indexOfFirst { it?.skill == skill.code } else skills.passive.indexOf(skill.code)
    Caption(ui(if (skill.kind == SkillKind.ACTIVE) "skills.slots_active" else "skills.slots_passive"))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (0 until open).forEach { at ->
            val here = at == standing
            ForgeOutlinedButton(enabled = !game.busy, onClick = {
                onSlot(skill.kind, at, !here)
            }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text(if (here) ui("skills.take_out") else ui("skills.to_slot", at + 1), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

/**
 * A skill for a slot: the class's learned ones of its kind, or nothing to empty it.
 *
 * Касание ставит умение в ячейку, долгое нажатие - его карточка.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SkillPicker(game: GameUi, pick: Pick.Slot, skills: HeroSkills, pages: List<SkillDefinition>, onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    val offered = pages.filter { it.kind == pick.kind && skills.level(it.code) > 0 }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                ui(if (pick.kind == SkillKind.ACTIVE) "skills.pick_active" else "skills.pick_passive", pick.index + 1),
                color = GoldBright,
                style = MaterialTheme.typography.titleMedium,
            )
            if (offered.isEmpty()) MutedText(ui("skills.nothing_learned")) else HoldHint(HoldSubject.SKILL)
            val inspect = rememberSkillInspect()
            offered.forEach { skill ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(PanelRaised)
                        .combinedClickable(enabled = !game.busy, onLongClick = { inspect(SkillPeek(skill.code)) }) { onPick(skill.code) }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeonSkillIcon(skill, skills.tier(skill.code), 34.dp)
                    Column(Modifier.weight(1f)) {
                        Text(SkillText.title(skill.code), color = GoldBright, style = MaterialTheme.typography.bodyMedium)
                        Text(skillKindLine(skill), color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(ui("skills.level_short", skills.level(skill.code)), color = Gold)
                }
            }
            ForgeTextButton(enabled = !game.busy, onClick = { onPick(null) }, modifier = Modifier.fillMaxWidth()) { Text(ui("skills.empty_it"), color = LifeRed) }
        }
    }
}

/**
 * When a slot fires, or a flask is drunk, by itself: every condition — a flask's own kind's too, and the mana's only for a flask.
 * [onSwap] - «Заменить умение» ячейки под условиями.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConditionPicker(title: String, current: SlotCondition?, flask: Boolean, onDismiss: () -> Unit, onSwap: (() -> Unit)? = null, onPick: (SlotCondition?) -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, color = GoldBright, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
            val options: List<SlotCondition?> = (if (flask) listOf(null) else emptyList<SlotCondition?>()) + SlotCondition.entries.filter { flask || !it.flaskOnly }
            options.forEach { condition ->
                val on = condition == current
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (on) PanelRaised else Color.Transparent).clickable { onPick(condition) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RadioButton(selected = on, onClick = { onPick(condition) }, colors = RadioButtonDefaults.colors(selectedColor = Gold))
                    Text(condition?.let(::conditionTitle) ?: ui("skills.condition.KIND"), color = if (on) GoldBright else Parchment)
                }
            }
            onSwap?.let { ForgeTextButton(onClick = it, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) { Text(ui("skills.swap_skill"), color = Rune) } }
        }
    }
}
