package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.TraitView
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.monsterSkillDamage
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.phaseText
import com.sperance.exileforge.core.display.phaseTitle
import com.sperance.exileforge.core.display.totemText
import com.sperance.exileforge.core.display.totemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.ui.theme.*

/**
 * Лист-справка (3.92.0): заголовок в цвете [tint], кто его владелец, разделы строками, пары «что - сколько» и примечание. Один
 * для умения монстра, проклятия, свойства и фазы - где бы их ни коснулись: в бою, в разведке, на экране босса, в журнале.
 */
@Composable fun LoreSheet(
    title: String,
    subtitle: String?,
    tint: Color,
    sections: List<Pair<String, List<String>>>,
    facts: List<Pair<String, String>> = emptyList(),
    note: String? = null,
    onDismiss: () -> Unit,
) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 16.dp).heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, color = tint, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            subtitle?.let { MutedText(it, style = MaterialTheme.typography.labelMedium) }
            sections.filter { it.second.isNotEmpty() }.forEach { (head, lines) ->
                Text(head.uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall)
                lines.forEach { Text(it, color = Parchment, style = MaterialTheme.typography.bodyMedium) }
            }
            if (facts.isNotEmpty()) {
                Column {
                    facts.forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(value, color = GoldBright, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = Bronze.copy(alpha = .5f))
                    }
                }
            }
            note?.let { MutedText(it, style = MaterialTheme.typography.labelSmall) }
            ForgeOutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(ui("lore.ok")) }
        }
    }
}

/**
 * Умение монстра (3.92.0): суть словами, урон его листом [foe] по типам и сколько дойдёт до героя [hero] после защиты,
 * перезарядка и мана; [owner] - чьё оно.
 */
@Composable fun MonsterSkillSheet(index: ContentIndex, code: String, owner: String?, foe: Combatant?, hero: Combatant?, onDismiss: () -> Unit) {
    val skill = index.skills.monsterByCode[code]
    if (skill == null) {
        LoreSheet(SkillText.title(code), owner, GoldBright, emptyList(), onDismiss = onDismiss)
        return
    }
    val hit = remember(skill, foe, hero) { foe?.let { monsterSkillDamage(skill, it, hero) } }
    val kind = ui(if (skill.spell) "lore.skill_spell" else "lore.skill_attack")
    val facts = buildList {
        hit?.raw?.forEach { (type, amount) -> add(ui("lore.damage_of", ui("skill.element.${type.name}")) to number(amount)) }
        hit?.taken?.let { add(ui("lore.damage_taken") to number(it)) }
        add(ui("lore.cooldown") to ui("lore.seconds", fineNumber(skill.cooldown)))
        add(ui("lore.mana") to number(skill.mana))
    }
    LoreSheet(
        SkillText.title(code),
        listOfNotNull(owner?.let { ui("lore.skill_of", it) }, kind).joinToString(" · "),
        if (skill.curse != null) LifeRed else GoldBright,
        listOf(ui("lore.essence") to SkillText.monster(skill)),
        facts,
        onDismiss = onDismiss,
    )
}

/**
 * Проклятие (3.92.0): что именно меняет - строки умения, что его наложило, или строка сделки алтаря, - сколько ещё
 * действует и чем его снять.
 */
@Composable fun CurseSheet(title: String, owner: String?, lines: List<String>, left: Double?, total: Double?, note: String?, onDismiss: () -> Unit) {
    val facts = buildList {
        if (left != null && total != null) add(ui("lore.curse_left") to ui("lore.seconds_of", fineNumber(left), fineNumber(total)))
    }
    LoreSheet(
        ui("lore.curse_title", title),
        owner?.let { ui("lore.cursed_by", it) },
        LifeRed,
        listOf(ui("lore.changes") to lines),
        facts,
        note ?: ui("lore.curse_remove"),
        onDismiss,
    )
}

/** Проклятие умения [code] (монстра или героя) по его строкам: что меняет и сколько ещё. */
@Composable fun SkillCurseSheet(index: ContentIndex, code: String, owner: String?, left: Double?, onDismiss: () -> Unit) {
    val monster = index.skills.monsterByCode[code]
    val hero = index.skills.byCode[code]
    val lines = monster?.let(SkillText::monster) ?: hero?.let { SkillText.lines(it, 1) }.orEmpty()
    val total = monster?.curse?.duration ?: hero?.curse?.duration
    CurseSheet(SkillText.title(code), owner, lines, left, total, null, onDismiss)
}

/** Свойство монстра (3.92.0): его название, что делает и строки. */
@Composable fun TraitSheet(trait: TraitView, owner: String?, onDismiss: () -> Unit) {
    LoreSheet(trait.title, owner, Handcrafted, listOf(ui("lore.essence") to listOf(trait.text), ui("lore.lines") to trait.lines), onDismiss = onDismiss)
}

/** Фаза босса (3.92.0): шаблон и что происходит на каждом пороге. */
@Composable fun PhaseSheet(code: String, owner: String?, onDismiss: () -> Unit) {
    LoreSheet(phaseTitle(code), owner?.let { ui("lore.phase_of", it) }, Elder, listOf(ui("lore.essence") to listOf(phaseText(code))), note = ui("lore.phase_note"), onDismiss = onDismiss)
}

/** Тотем босса (3.93.0): что делает, сколько ещё стоит; бить его нельзя - он рассыпается сам или со смертью босса. */
@Composable fun TotemSheet(code: String, owner: String?, left: Double?, onDismiss: () -> Unit) {
    val facts = listOfNotNull(left?.let { ui("lore.totem_left") to ui("lore.seconds", fineNumber(it)) })
    LoreSheet(totemTitle(code), owner?.let { ui("lore.totem_of", it) }, Elder, listOf(ui("lore.essence") to listOf(totemText(code))), facts, ui("lore.totem_note"), onDismiss)
}

/** Что открыть в листе-справке (3.92.0): кто бы ни коснулся - плитка, печать, чип, строка журнала. */
sealed interface Lore {
    /** Умение монстра [code] врага [owner]; [foe] - его лист, [hero] - лист героя (null - по герою из игры). */
    data class Skill(val code: String, val owner: String?, val foe: Combatant?, val hero: Combatant? = null) : Lore

    /** Проклятие умения [code] от [owner], ещё [left] секунд. */
    data class Curse(val code: String, val owner: String?, val left: Double?) : Lore

    /** Проклятие строкой (сделка алтаря): [title] и что меняет [lines], [note] - сколько действует. */
    data class CurseText(val title: String, val lines: List<String>, val note: String?) : Lore
    data class Trait(val view: TraitView, val owner: String?) : Lore
    data class Phase(val code: String, val owner: String?) : Lore

    /** Тотем [code] босса [owner], ещё [left] секунд (3.93.0). */
    data class Totem(val code: String, val owner: String?, val left: Double?) : Lore
}

/** Как открыть лист-справку отсюда; null - некому показать, плитка остаётся подсказкой. */
val LocalLore = androidx.compose.runtime.staticCompositionLocalOf<((Lore) -> Unit)?> { null }

/** Хозяин листов-справок (3.92.0): всё, что внутри, открывает их через [LocalLore]; лист один, поверх. */
@Composable fun LoreHost(game: com.sperance.exileforge.presentation.state.GameUi, content: @Composable () -> Unit) {
    var open by remember { androidx.compose.runtime.mutableStateOf<Lore?>(null) }
    androidx.compose.runtime.CompositionLocalProvider(LocalLore provides { open = it }) { content() }
    val index = game.index ?: return
    val close = { open = null }
    when (val lore = open) {
        null -> Unit

        is Lore.Skill -> {
            val hero = lore.hero ?: remember(game.hero, index) { game.hero?.let { Combatant(it.stats, it.level, index.campaign.combat) } }
            MonsterSkillSheet(index, lore.code, lore.owner, lore.foe, hero, close)
        }

        is Lore.Curse -> SkillCurseSheet(index, lore.code, lore.owner, lore.left, close)

        is Lore.CurseText -> CurseSheet(lore.title, null, lore.lines, null, null, lore.note, close)

        is Lore.Trait -> TraitSheet(lore.view, lore.owner, close)

        is Lore.Phase -> PhaseSheet(lore.code, lore.owner, close)

        is Lore.Totem -> TotemSheet(lore.code, lore.owner, lore.left, close)
    }
}

/** Чип, что открывает лист-справку (3.92.0): название в цвете [tint] и стрелка. */
@Composable fun LoreChip(label: String, tint: Color, onClick: () -> Unit) {
    val pill = androidx.compose.foundation.shape.RoundedCornerShape(50)
    Text(
        "$label ›",
        color = tint,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.clip(pill).background(PanelRaised).border(1.dp, tint.copy(alpha = .4f), pill)
            .clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 4.dp),
    )
}
