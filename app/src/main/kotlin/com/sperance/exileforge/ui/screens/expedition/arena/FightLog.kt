package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.campaign.shownLife
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.phaseTitle
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.traitTitle
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.PillTabs
import com.sperance.exileforge.ui.screens.expedition.Caption
import com.sperance.exileforge.ui.screens.hero.petName
import com.sperance.exileforge.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

/** The blows so far, newest first: when, who, what came of it - and the target's life before and after (3.88.8). */
@Composable internal fun FightLog(events: List<CombatEvent>, names: Map<Int, String>, modifier: Modifier = Modifier, onOpen: ((CombatEvent) -> Unit)? = null) {
    val shifts = remember(events) { lifeShifts(events.asReversed()).asReversed() }
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        itemsIndexed(events) { i, event -> EventRow(event, event.time, names[event.foe].orEmpty(), shifts.getOrNull(i), onOpen) }
    }
}

/** A row of a pack's log: a caption naming one of the pack, or one of its lines under that name. */
private sealed interface PackRow {
    data class Head(val text: String) : PackRow

    /** [at] (3.91.1) - секунда боя целиком: этап начинается с [PackHit.start], а время его событий - с нуля. */
    data class Line(val event: CombatEvent, val name: String, val shift: LifeShift?, val at: Double = event.time) : PackRow
}

/** Здоровье цели строки до удара и после (3.88.8), как его показывает экран боя. */
internal data class LifeShift(val before: Int, val after: Int)

/**
 * «ХП было → стало» для каждой строки [events] в порядке времени (3.88.8): «было» - здоровье цели после её прошлой строки
 * в этом бою, у первой - после удара плюс урон. Строки без урона и лечения, заметки и удары по питомцу - без него.
 */
internal fun lifeShifts(events: List<CombatEvent>): List<LifeShift?> {
    val last = HashMap<Pair<Side, Int>, Double>()
    return events.map { event ->
        val target = event.target
        val key = target to if (target == Side.HERO) HERO_KEY else event.foe
        val after = if (target == Side.HERO) event.heroLife else event.monsterLife
        val shift = if (event.action == Action.NOTE || event.pet != null || (event.damage <= 0 && event.healed <= 0)) {
            null
        } else {
            val before = last[key] ?: (after + event.damage - event.healed)
            LifeShift(shownLife(before, before > 0), shownLife(after, after > 0))
        }
        last[Side.HERO to HERO_KEY] = event.heroLife
        last[Side.MONSTER to event.foe] = event.monsterLife
        shift
    }
}

/** Ключ героя среди противников по индексу. */
private const val HERO_KEY = -1

/**
 * The log of a whole pack (since 2.54.0): one list, each foe's blows under its own name — a mixed
 * pack's «they hit» lines would otherwise all say the wrong name. A caption between them names which
 * of the pack it was, only when there was more than one. [toDeath] (3.70.0) opens it on the blow that
 * felled the hero, or at its end.
 */
@Composable internal fun FightLog(
    pack: List<PackHit>,
    modifier: Modifier = Modifier,
    shown: Set<LogKind> = LogKind.DEFAULT,
    toDeath: Boolean = false,
    onOpen: ((CombatEvent, String) -> Unit)? = null,
) {
    // Лента по времени по умолчанию (3.88.8): удары всей стаи по порядку; по противникам - каждый под своим именем.
    var byFoe by rememberSaveable { mutableStateOf(false) }
    val rows = remember(pack, shown, byFoe) {
        val names = pack.map { monsterTitle(it.monster.code) }
        buildList {
            if (byFoe) {
                pack.forEachIndexed { index, hit ->
                    if (pack.size > 1) add(PackRow.Head(ui("expedition.report_pack_enemy", index + 1, pack.size, names[index])))
                    val shifts = lifeShifts(hit.events)
                    hit.events.forEachIndexed { i, event -> if (LogKind.of(event) in shown) add(PackRow.Line(event, names[index], shifts[i])) }
                }
            } else {
                // Этапы боя (3.91.1) - подряд, со сквозным временем: внутри этапа удары стаи по порядку, между ними - подпись.
                val stages = pack.withIndex().groupBy { it.value.start }.entries.sortedBy { it.key }
                stages.forEachIndexed { stage, (start, hits) ->
                    if (stages.size > 1) add(PackRow.Head(ui("expedition.log_stage", stage + 1, hits.joinToString { names[it.index] })))
                    val timeline = hits.flatMap { (index, hit) -> hit.events.map { it to names[index] } }.sortedBy { it.first.time }
                    val shifts = lifeShifts(timeline.map { it.first })
                    timeline.forEachIndexed { i, (event, name) -> if (LogKind.of(event) in shown) add(PackRow.Line(event, name, shifts[i], start + event.time)) }
                }
            }
        }
    }
    val state = rememberLazyListState()
    if (toDeath) {
        LaunchedEffect(rows) {
            val death = rows.indexOfFirst { it is PackRow.Line && it.event.heroLife <= 0 }
            if (rows.isNotEmpty()) state.scrollToItem(if (death >= 0) death else rows.lastIndex)
        }
    }
    LazyColumn(modifier.fillMaxWidth(), state = state, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        if (pack.size > 1) {
            item {
                PillTabs(listOf(ui("expedition.log_by_time"), ui("expedition.log_by_foe")), if (byFoe) 1 else 0, { byFoe = it == 1 }, segmented = true)
            }
        }
        items(rows) { row ->
            when (row) {
                is PackRow.Head -> Caption(row.text)
                is PackRow.Line -> EventRow(row.event, row.at, row.name, row.shift, onOpen?.let { open -> { event: CombatEvent -> open(event, row.name) } })
            }
        }
    }
}

@Composable private fun EventRow(event: CombatEvent, at: Double, monster: String, shift: LifeShift?, onOpen: ((CombatEvent) -> Unit)? = null) {
    // Умение врага и удар фазы (3.92.0) открывают свой лист; иная строка с разбором - его карточку (3.37.0)
    val lore = LocalLore.current
    val skill = event.skill?.takeIf { event.actor == Side.MONSTER && event.action != Action.NOTE }
    val tap = when {
        lore != null && skill != null && '#' in skill -> Modifier.clickable { lore(Lore.Phase(skill.substringBefore('#'), monster)) }
        lore != null && skill != null -> Modifier.clickable { lore(Lore.Skill(skill, monster, null)) }
        onOpen != null && event.trace != null -> Modifier.clickable { onOpen(event) }
        else -> Modifier
    }
    Row(tap.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            ui("expedition.log_time", String.format(Locale.ROOT, "%.1f", at)),
            color = Muted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(40.dp),
        )
        Text(
            logLine(event, monster),
            color = logColour(event),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (event.kind == HitKind.CRIT) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (event.action == Action.TICK) FontStyle.Italic else FontStyle.Normal,
            modifier = Modifier.weight(1f),
        )
        shift?.let { Text(ui("expedition.log_life", it.before, it.after), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
    }
}

private fun logLine(event: CombatEvent, monster: String): String {
    val damage = event.damage.roundToInt()
    val hero = event.actor == Side.HERO
    val line = event.pet?.let { petLine(event, monster, petName(it), damage) } ?: when (event.action) {
        // Thorns and reflect (2.75.0): the one who was struck gives a blow back.
        Action.REFLECT -> if (hero) ui("expedition.log_reflect_you", monster, damage) else ui("expedition.log_reflect_they", monster, damage)

        Action.TICK -> {
            val ailment = event.ailment?.let { ui(it.key()) }.orEmpty()
            if (hero) ui("expedition.log_tick_they", monster, damage, ailment) else ui("expedition.log_tick_you", damage, ailment)
        }

        Action.ATTACK -> when (event.kind) {
            HitKind.HIT -> if (hero) ui("expedition.log_you_hit", damage) else ui("expedition.log_they_hit", monster, damage)

            HitKind.CRIT -> if (hero) ui("expedition.log_you_crit", damage) else ui("expedition.log_they_crit", monster, damage)

            // An evasion or a block belongs to the one who was struck at.
            HitKind.EVADED -> if (hero) ui("expedition.log_they_evade", monster) else ui("expedition.log_you_evade")

            HitKind.BLOCKED -> if (hero) ui("expedition.log_they_block", monster) else ui("expedition.log_you_block")
        }

        // A skill (2.78.0): a blow names it, anything else says who used it and on whom.
        Action.SKILL -> {
            val skill = SkillText.title(event.skill.orEmpty())
            when {
                event.kind == HitKind.EVADED -> if (hero) ui("expedition.log_skill_evaded", skill, monster) else ui("expedition.log_skill_you_evade", monster, skill)

                event.kind == HitKind.BLOCKED -> if (hero) ui("expedition.log_skill_blocked", skill, monster) else ui("expedition.log_skill_you_block", monster, skill)

                event.damage > 0 -> if (hero) {
                    ui(if (event.kind == HitKind.CRIT) "expedition.log_skill_crit" else "expedition.log_skill_hit", skill, monster, damage)
                } else {
                    ui(if (event.kind == HitKind.CRIT) "expedition.log_they_skill_crit" else "expedition.log_they_skill_hit", monster, skill, damage)
                }

                event.onSelf -> (if (hero) ui("expedition.log_skill_self", skill) else ui("expedition.log_they_skill_self", monster, skill)) +
                    (if (event.healed >= 1) " · +${event.healed.roundToInt()}" else "")

                else -> if (hero) ui("expedition.log_skill_on", skill, monster) else ui("expedition.log_they_skill_on", monster, skill)
            }
        }

        Action.FLASK -> ui("expedition.log_flask", equipmentTitle(event.skill.orEmpty())) +
            (if (event.healed >= 1) " · +${event.healed.roundToInt()}" else "")

        // A note (3.37.0): what happened without a blow, by the trace that carries it.
        Action.NOTE -> noteLine(event, monster)
    }
    // The blow's leading element and what it left behind, as words after the sentence.
    val marks = buildList {
        event.type?.takeIf { event.action.strikes && event.landed && event.damage > 0 && it != DamageType.PHYSICAL }?.let { add(ui(it.key())) }
        if (event.stunned) add(ui("expedition.stunned"))
        event.inflicted.forEach { add(ui(it.key())) }
    }
    return if (marks.isEmpty()) line else "$line · ${marks.joinToString(" · ")}"
}

/**
 * A line of the combat pet (3.70.0), named: its blow at [monster], the blow it took from one, the healing it gave the hero;
 * null for anything the hero's own sentences say well enough.
 */
private fun petLine(event: CombatEvent, monster: String, pet: String, damage: Int): String? {
    val (doer, done) = if (event.actor == Side.HERO) pet to monster else monster to pet
    return when {
        event.actor == Side.HERO && event.onSelf -> event.healed.takeIf { it >= 1 }?.let { ui("expedition.log_pet_heal", pet, it.roundToInt()) }
        event.action == Action.TICK -> ui("expedition.log_tick_they", pet, damage, event.ailment?.let { ui(it.key()) }.orEmpty())
        event.action == Action.REFLECT -> ui("expedition.log_returns", doer, done, damage)
        event.action != Action.ATTACK && event.action != Action.SKILL -> null
        event.kind == HitKind.EVADED -> ui("expedition.log_evades", done, doer)
        event.kind == HitKind.BLOCKED -> ui("expedition.log_blocks", done, doer)
        event.damage > 0 -> ui(if (event.kind == HitKind.CRIT) "expedition.log_strikes_crit" else "expedition.log_strikes", doer, done, damage)
        else -> null
    }
}

internal fun noteLine(event: CombatEvent, monster: String): String {
    val note = event.trace as? NoteTrace ?: return ""
    return when (note.kind) {
        NoteKind.BUFF -> ui("expedition.log_note_buff", ui("fight.buff.${note.ref}"), fineNumber(note.value))
        NoteKind.CHARGE -> ui("expedition.log_note_charge", ui("fight.charge.${note.ref}"), note.value.roundToInt())
        NoteKind.POWER -> ui("expedition.log_note_power", statTitle(note.ref))
        NoteKind.CONDITION_ON -> ui("expedition.log_note_condition_on", locOr("condition.${note.ref}", note.ref))
        NoteKind.CONDITION_OFF -> ui("expedition.log_note_condition_off", locOr("condition.${note.ref}", note.ref))
        NoteKind.KILL -> ui("expedition.log_note_kill", monster) + (if (note.value >= 1) " · +${note.value.roundToInt()}" else "")
        NoteKind.TRAIT -> ui("expedition.log_note_trait", monster, traitTitle(note.ref))
        NoteKind.PHASE -> ui("expedition.log_note_phase", monster, phaseTitle(note.ref), note.value.roundToInt())
        NoteKind.TOTEM -> ui("expedition.log_note_totem", monster, com.sperance.exileforge.core.display.totemTitle(note.ref))
        NoteKind.TOTEM_FALL -> ui("expedition.log_note_totem_fall", com.sperance.exileforge.core.display.totemTitle(note.ref))
        NoteKind.RECOVER_FLASK -> ui("expedition.log_note_recover_flask", equipmentTitle(note.ref), note.value.roundToInt())
        NoteKind.RECOVER_RECOUP -> ui("expedition.log_note_recover_recoup", note.value.roundToInt())
        NoteKind.RECOVER_WASTE -> ui("expedition.log_note_recover_waste", note.value.roundToInt())
        NoteKind.REGEN -> ui("expedition.log_note_regen", note.value.roundToInt())
    }
}

private fun logColour(event: CombatEvent): Color = when {
    event.pet != null -> Vital
    (event.trace as? NoteTrace)?.kind?.recovery == true -> Vital
    event.action == Action.NOTE -> Rune
    event.action == Action.FLASK -> Vital
    event.action == Action.SKILL && event.damage <= 0 && event.landed -> if (event.actor == Side.HERO) Gold else Color(0xFFE9A0A0)
    event.action == Action.TICK || event.action == Action.REFLECT -> damageTint(event.type).copy(alpha = .85f)
    event.kind == HitKind.CRIT -> Color(0xFFFFD34A)
    event.kind == HitKind.HIT -> if (event.actor == Side.HERO) Parchment else Color(0xFFE9A0A0)
    else -> Muted
}
