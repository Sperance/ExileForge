package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.BuffShare
import com.sperance.exileforge.core.campaign.TraitView
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.monsterSkillDamage
import com.sperance.exileforge.core.campaign.monsterLineParts
import com.sperance.exileforge.core.campaign.monsterLineText
import com.sperance.exileforge.core.campaign.monsterLines
import com.sperance.exileforge.core.campaign.run.BossHud
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.campaign.run.FoeView
import com.sperance.exileforge.core.campaign.run.SlotView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.phaseText
import com.sperance.exileforge.core.display.phaseTitle
import com.sperance.exileforge.core.display.totemText
import com.sperance.exileforge.core.display.totemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

/** Вкладки листа врага: характеристики, навыки, фазы и тотемы (только у босса с ними), модификаторы. */
private enum class FoeTab(val key: String) { STATS("fight.tab_stats"), SKILLS("fight.tab_skills"), PHASES("fight.tab_phases"), MODS("fight.tab_mods") }

/**
 * Лист врага на паузе (3.93.0): босс или приспешник целиком - защита с тем, что от неё остаётся против героя, урон, умения и
 * свойства с сутью, фазы и тотемы босса, его строки с источниками - монстр, карта, атлас, сделки алтаря.
 */
@Composable internal fun FoeSheet(
    foe: FoeView,
    fight: FightHud,
    level: Int,
    rules: CombatRules,
    index: ContentIndex?,
    traits: List<TraitView>,
    boss: BossHud?,
    shares: List<BuffShare>,
    onDismiss: () -> Unit,
) {
    val body = remember(foe.monster, level) { Combatant(foe.monster.stats, level, rules) }
    val hero = fight.heroBody
    val ownBoss = boss?.takeIf { it.index == foe.index }
    val totems = remember(foe.monster.code, index) { index?.monster(foe.monster.code)?.totems.orEmpty() }
    val tabs = FoeTab.entries.filter { it != FoeTab.PHASES || (ownBoss != null && (ownBoss.phase != null || totems.isNotEmpty())) }
    var tab by rememberSaveable(foe.index) { mutableStateOf(FoeTab.STATS) }
    val name = monsterTitle(foe.monster.code)
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = rarityTint(foe.monster.rarity), fontFamily = FontFamily.Serif, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                MutedText(listOf(ui(foe.monster.rarity.key()), ui("fight.level", level)).joinToString(" · "), style = MaterialTheme.typography.labelMedium)
            }
            ScrollableTabRow(selectedTabIndex = tabs.indexOf(tab).coerceAtLeast(0), containerColor = PanelRaised, contentColor = Gold, edgePadding = 12.dp) {
                tabs.forEach { t -> Tab(selected = t == tab, onClick = { tab = t }, text = { Text(ui(t.key)) }) }
            }
            Column(
                Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (tab) {
                    FoeTab.STATS -> StatsTab(foe, body, hero, rules)
                    FoeTab.SKILLS -> SkillsTab(foe, body, hero, traits, index, name)
                    FoeTab.PHASES -> ownBoss?.let { PhasesTab(it, totems, name) }
                    FoeTab.MODS -> ModsTab(foe, shares, index)
                }
            }
        }
    }
}

@Composable private fun Row2(label: String, value: String, tint: Color = GoldBright, note: String? = null) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(value, color = tint, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
        note?.let { Text(it, color = ModBlue, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 3.dp)) }
        HorizontalDivider(color = Bronze.copy(alpha = .4f))
    }
}

@Composable private fun Head(text: String) = Text(text.uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))

private fun pct(share: Double) = "${(share * 100).roundToInt()}%"

/** Характеристики: запасы, скорость, урон, защита - и что от защиты остаётся против героя. */
@Composable private fun StatsTab(foe: FoeView, body: Combatant, hero: Combatant?, rules: CombatRules) {
    Head(ui("fight.sheet_pools"))
    Row2(ui("fight.stat_life"), "${number(foe.life.toDouble())} / ${number(body.maxLife)}", LifeRed)
    if (body.maxShield > 0) Row2(ui("fight.stat_shield"), "${number(foe.shield.toDouble())} / ${number(body.maxShield)}", ShieldCyan)
    if (foe.barrier > 0) Row2(ui("fight.stat_barrier"), number(foe.barrier.toDouble()), FrameGoldBright)
    if (body.maxMana > 0) Row2(ui("fight.stat_mana"), "${number(foe.mana.toDouble())} / ${number(body.maxMana)}", ManaBlue)
    Head(ui("fight.sheet_attack"))
    Row2(ui("fight.stat_speed"), String.format(Locale.ROOT, "%.2f", body.attackSpeed))
    body.damage.filterValues { it > 0 }.forEach { (type, amount) -> Row2(ui(type.key()), number(amount), damageTint(type)) }
    Head(ui("fight.sheet_defence"))
    val physical = hero?.damage?.get(DamageType.PHYSICAL)?.takeIf { it > 0 }
    Row2(
        ui("fight.stat_armour"),
        number(body.armour),
        note = physical?.let { ui("fight.vs_armour", pct(body.physicalMitigation(it, rules.armour.factor))) },
    )
    val hit = hero?.let { 1 - rules.accuracy.evaded(it.accuracy(rules.accuracy), body.evasion).coerceAtMost(body.evasionCap) }
    Row2(ui("fight.stat_evasion"), number(body.evasion), note = hit?.let { ui("fight.vs_evasion", pct(it)) })
    if (body.block > 0) Row2(ui("fight.stat_block"), pct(body.block))
    (DamageType.ELEMENTS + DamageType.CHAOS).forEach { type ->
        val own = body.resist(type)
        val against = hero?.let { body.resist(type, it.penetration(type)) }
        Row2(
            ui("fight.resist_of", ui(type.key())),
            pct(own),
            damageTint(type),
            note = against?.takeIf { it != own }?.let { ui("fight.vs_resist", pct(it)) },
        )
    }
    if (foe.effects.isNotEmpty() || foe.ailments.isNotEmpty() || foe.held) {
        Head(ui("fight.sheet_now"))
        StateTiles(foe.ailments, foe.held, foe.effects)
    }
}

/** Навыки и свойства: суть каждого умения, урон по герою, перезарядка; свойства с их строками. */
@Composable private fun SkillsTab(foe: FoeView, body: Combatant, hero: Combatant?, traits: List<TraitView>, index: ContentIndex?, name: String) {
    val lore = LocalLore.current
    if (foe.monster.skills.isEmpty() && traits.isEmpty()) MutedText(ui("fight.sheet_no_skills"))
    foe.monster.skills.forEach { code ->
        val skill = index?.skills?.monsterByCode?.get(code)
        val hit = skill?.let { monsterSkillDamage(it, body, hero) }
        Column(Modifier.fillMaxWidth().clickable { lore?.invoke(Lore.Skill(code, name, body, hero)) }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(SkillText.title(code), color = if (skill?.curse != null) LifeRed else Rune, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            skill?.let(SkillText::monster)?.forEach { Text(it, color = Parchment, style = MaterialTheme.typography.labelSmall) }
            val facts = listOfNotNull(
                hit?.taken?.let { ui("fight.skill_hits_you", number(it)) },
                skill?.let { ui("lore.cooldown") + " " + ui("lore.seconds", fineNumber(it.cooldown)) },
                skill?.takeIf { it.mana > 0 }?.let { ui("lore.mana") + " " + number(it.mana) },
            )
            if (facts.isNotEmpty()) MutedText(facts.joinToString(" · "), style = MaterialTheme.typography.labelSmall)
            HorizontalDivider(color = Bronze.copy(alpha = .4f), modifier = Modifier.padding(top = 4.dp))
        }
    }
    if (traits.isNotEmpty()) {
        Head(ui("fight.traits", traits.size))
        traits.forEach { trait ->
            Column(Modifier.fillMaxWidth().clickable { lore?.invoke(Lore.Trait(trait, name)) }) {
                Text(trait.title, color = Color(0xFFE8B06A), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(trait.text, color = Parchment, style = MaterialTheme.typography.labelSmall)
                trait.lines.forEach { Text(it, color = ModBlue, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

/** Фазы и тотемы босса: шаблон и пройденные пороги, его тотемы и те, что стоят сейчас. */
@Composable private fun PhasesTab(boss: BossHud, totems: List<String>, name: String) {
    val lore = LocalLore.current
    boss.phase?.let { code ->
        Text(phaseTitle(code), color = Elder, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(phaseText(code), color = Parchment, style = MaterialTheme.typography.bodySmall)
        boss.marks.forEachIndexed { i, at ->
            val passed = boss.passed.getOrElse(i) { false }
            Row2(ui("fight.phase_at", at.roundToInt()), ui(if (passed) "fight.phase_passed" else "fight.phase_ahead"), if (passed) Muted else FrameGoldBright)
        }
    }
    val standing = boss.slots.filterIsInstance<SlotView.Totem>()
    if (standing.isNotEmpty()) {
        Head(ui("fight.totems_now"))
        standing.forEach { t -> Row2(totemTitle(t.code), ui("lore.seconds", fineNumber(t.left)), totemTint(t), note = totemText(t.code)) }
    }
    if (totems.isNotEmpty()) {
        Head(ui("fight.totems_own"))
        totems.forEach { code ->
            Column(Modifier.fillMaxWidth().clickable { lore?.invoke(Lore.Totem(code, name, null)) }) {
                Text(totemTitle(code), color = Elder, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(totemText(code), color = Parchment, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Модификаторы: каждая строка суммой и из чего она сложена - монстр, карта, атлас, сделки. */
@Composable private fun ModsTab(foe: FoeView, shares: List<BuffShare>, index: ContentIndex?) {
    val lines = remember(foe.monster) { monsterLines(foe.monster) }
    if (lines.isEmpty()) MutedText(ui("fight.sheet_no_mods"))
    lines.forEach { line ->
        Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text(monsterLineText(line, index), color = ModBlue, style = MaterialTheme.typography.bodySmall)
            monsterLineParts(line, shares, index)?.let { MutedText(it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}
