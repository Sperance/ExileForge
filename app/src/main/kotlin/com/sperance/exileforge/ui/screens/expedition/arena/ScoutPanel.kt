package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.expedition.Caption
import com.sperance.exileforge.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The scouting panel (2.70.0): one foe while nothing moves — what it is and where it stands, its
 * pools, block, how hard and how often it strikes, what it rolled — and whether the hero's weapon
 * reaches it. Its armour, evasion and resistances are not shown since 2.75.0: the fight is read by
 * what happens in it, not by the foe's defence sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScoutPanel(
    foe: FoeView,
    fight: FightHud,
    level: Int,
    rules: CombatRules,
    stance: HeroStance,
    index: ContentIndex?,
    traits: List<TraitView>,
) {
    val body = remember(foe.monster) { Combatant(foe.monster.stats, level, rules) }
    val shape = RoundedCornerShape(10.dp)
    val ring = rarityTint(foe.monster.rarity)
    Column(
        Modifier.fillMaxSize().background(Panel.copy(alpha = .95f), shape).border(1.dp, ring.copy(alpha = .8f), shape)
            .verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(monsterTitle(foe.monster.code), color = ring, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            listOf(ui(foe.monster.rarity.key()), ui("fight.level", level)).joinToString(" · "),
            color = Muted,
            style = MaterialTheme.typography.labelSmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Fact(ui("fight.stat_life"), number(body.maxLife), LifeRed)
            if (body.maxShield > 0) Fact(ui("fight.stat_shield"), number(body.maxShield), ShieldCyan)
            if (body.block > 0) Fact(ui("fight.stat_block"), "${(body.block * 100).roundToInt()}%", Parchment)
            Fact(ui("fight.stat_speed"), String.format(Locale.ROOT, "%.2f", body.attackSpeed), Parchment)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("fight.stat_damage"), color = Muted, style = MaterialTheme.typography.labelSmall)
            body.damage.filterValues { it > 0 }.forEach { (type, amount) ->
                Text("${ui(type.key())} ${number(amount)}", color = damageTint(type), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        // Every buildup on it (3.78.0): the card shows only the fullest.
        foe.buildup?.let { view ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Buildup.entries.forEach { kind ->
                    Text(buildupGlyph(kind), color = buildupTint(kind), fontSize = 10.sp)
                    Box(Modifier.weight(1f).height(4.dp).background(Color(0x1FFFFFFF), RoundedCornerShape(2.dp))) {
                        Box(Modifier.fillMaxWidth(view.bars.getOrElse(kind.ordinal) { 0f }).fillMaxHeight().background(buildupTint(kind), RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
        // What it casts for its mana (2.78.0): a boss's own skills, a caster's spell, a borrowed one.
        if (foe.monster.skills.isNotEmpty()) {
            Text(
                ui("fight.skills", foe.monster.skills.joinToString(", ") { SkillText.title(it) }),
                color = Rune,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        // Its traits (3.73.0): what its kind and its form do, at its rarity's strength.
        if (traits.isNotEmpty()) {
            Caption(ui("fight.traits", traits.size))
            traits.forEach { trait ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                    SkillGlyph(trait.icon, Modifier.size(18.dp), Color(0xFFE8B06A))
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(trait.title, color = Color(0xFFE8B06A), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(trait.text, color = Parchment, style = MaterialTheme.typography.labelSmall)
                        trait.lines.forEach { Text(it, color = ModBlue, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
        // What it means for this hero.
        val taunting = fight.foes.any { it.alive && it.taunt }
        when {
            foe.taunt -> Hint(ui("fight.taunt_hint"), LifeRed)
            fight.focus == foe.index && !foe.reachable -> Hint(ui("fight.out_of_reach"), LifeRed)
            taunting -> Hint(ui("fight.behind_taunt"), LifeRed)
        }
        Hint(if (fight.focus == foe.index) ui("fight.focus_on") else ui("fight.focus_off", ui("fight.rule.${stance.rule.name}")), GoldBright)
        monsterLines(foe.monster).takeIf { it.isNotEmpty() }?.let { lines ->
            Caption(ui("fight.modifiers", lines.size))
            lines.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Rhombus(if (line.fromMap) LifeRed else ModBlue, 4.dp)
                    Text(monsterLineText(line, index), color = ModBlue, style = MaterialTheme.typography.labelSmall)
                    // The sum first, then what each source put in it (2.73.0); a line the map alone gives is tagged.
                    val sources = monsterLineSources(line, index)
                    if (sources != null) {
                        Text("($sources)", color = Muted, style = MaterialTheme.typography.labelSmall)
                    } else if (line.fromMap) {
                        Text(ui("fight.line_map"), color = LifeRed, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable private fun Fact(label: String, value: String, tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun Hint(text: String, tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Rhombus(tint, 4.dp)
        Text(text, color = tint, style = MaterialTheme.typography.labelSmall)
    }
}
