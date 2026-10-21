package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
import com.sperance.exileforge.core.display.flowParts
import com.sperance.exileforge.core.display.flowRate
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.phaseTitle
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.traitTitle
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.PillTabs
import com.sperance.exileforge.ui.screens.expedition.Caption
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The blows so far, newest first: when, who, what came of it - and the target's life before and after.
 * Живой журнал: пока игрок наверху, новая строка держит список на ней; отлистал вниз - список стоит, а кнопка
 * «↑ новое (N)» считает пришедшие строки; касание кнопки, возврат наверх или [LOG_RETURN_MS] без касаний списка снова держат
 * список на новой.
 *
 * Игрок листает - следование снято; наверху или после паузы без касаний - снова следим. Ключ строки - её место от начала боя: новая строка сверху не сдвигает ключи
 * прочих.
 */
@Composable internal fun FightLog(events: List<CombatEvent>, names: Map<Int, String>, modifier: Modifier = Modifier, onOpen: ((CombatEvent) -> Unit)? = null) {
    val shifts = remember(events) { lifeShifts(events.asReversed()).asReversed() }
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var follow by remember { mutableStateOf(true) }
    var seen by remember { mutableIntStateOf(events.size) }
    var touches by remember { mutableIntStateOf(0) }
    var held by remember { mutableStateOf(false) }
    fun atTop() = state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
    LaunchedEffect(state) {
        launch {
            state.interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is DragInteraction.Start -> follow = false
                    is DragInteraction.Stop, is DragInteraction.Cancel -> if (atTop()) follow = true
                }
            }
        }
        snapshotFlow { atTop() }.collect { top -> if (top) follow = true }
    }
    LaunchedEffect(follow, held, touches, state.isScrollInProgress) {
        if (follow || held || state.isScrollInProgress) return@LaunchedEffect
        delay(LOG_RETURN_MS)
        follow = true
    }
    LaunchedEffect(events.size, follow) {
        if (follow) {
            seen = events.size
            if (!atTop()) state.scrollToItem(0)
        }
    }
    Box(
        modifier.fillMaxWidth().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    held = awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed }
                    touches++
                }
            }
        },
    ) {
        LazyColumn(Modifier.fillMaxWidth(), state = state, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            itemsIndexed(events, key = { i, _ -> events.size - i }) { i, event -> EventRow(event, event.time, names[event.foe].orEmpty(), shifts.getOrNull(i), onOpen) }
        }
        val fresh = events.size - seen
        if (!follow && fresh > 0) {
            Text(
                ui("fight.log_new", fresh),
                color = Ink,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp).clip(RoundedCornerShape(12.dp)).background(Gold).clickable {
                    follow = true
                    scope.launch { state.scrollToItem(0) }
                }.padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

/** Сколько миллисекунд без касаний журнал, отлистанный вниз, ждёт, прежде чем сам вернуться к новой строке. */
private const val LOG_RETURN_MS = 3_000L

/**
 * A row of a pack's log: a caption naming one of the pack, or one of its lines under that name. [key] - место строки в стае
 * (противник и номер события): не меняется ни с фильтром, ни с новыми событиями живого боя.
 */
private sealed interface PackRow {
    val key: String

    data class Head(override val key: String, val text: String) : PackRow

    /** [at] - секунда шага целиком: бой начинается с [PackHit.start], а время его событий - с нуля. */
    data class Line(override val key: String, val event: CombatEvent, val name: String, val shift: LifeShift?, val at: Double = event.time) : PackRow
}

/** Здоровье цели строки до удара и после, как его показывает экран боя. */
internal data class LifeShift(val before: Int, val after: Int)

/**
 * «ХП было → стало» для каждой строки [events] в порядке времени: «было» - здоровье цели после её прошлой строки
 * в этом бою, у первой - после удара плюс урон. Строки без урона и лечения и заметки - без него.
 */
internal fun lifeShifts(events: List<CombatEvent>): List<LifeShift?> {
    val last = HashMap<Pair<Side, Int>, Double>()
    return events.map { event ->
        val target = event.target
        val key = target to if (target == Side.HERO) HERO_KEY else event.foe
        val after = if (target == Side.HERO) event.heroLife else event.monsterLife
        val shift = if (event.action == Action.NOTE || (event.damage <= 0 && event.healed <= 0)) {
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
 * The log of a whole pack: one list, each foe's blows under its own name — a mixed
 * pack's «they hit» lines would otherwise all say the wrong name. A caption between them names which
 * of the pack it was, only when there was more than one. [toDeath] opens it on the blow that
 * felled the hero, or at its end.
 *
 * Лента по времени по умолчанию; бои шага испытания - подряд со сквозным временем.
 */
@Composable internal fun FightLog(
    pack: List<PackHit>,
    modifier: Modifier = Modifier,
    shown: Set<LogKind> = LogKind.DEFAULT,
    toDeath: Boolean = false,
    onOpen: ((CombatEvent, String) -> Unit)? = null,
) {
    var byFoe by rememberSaveable { mutableStateOf(false) }
    val rows = remember(pack, shown, byFoe) {
        val names = pack.map { monsterTitle(it.monster.code) }
        buildList {
            if (byFoe) {
                pack.forEachIndexed { index, hit ->
                    if (pack.size > 1) add(PackRow.Head("foe:$index", ui("expedition.report_pack_enemy", index + 1, pack.size, names[index])))
                    val shifts = lifeShifts(hit.events)
                    hit.events.forEachIndexed { i, event -> if (LogKind.of(event) in shown) add(PackRow.Line("$index:$i", event, names[index], shifts[i])) }
                }
            } else {
                val stages = pack.withIndex().groupBy { it.value.start }.entries.sortedBy { it.key }
                stages.forEachIndexed { stage, (start, hits) ->
                    if (stages.size > 1) add(PackRow.Head("stage:$stage", ui("expedition.log_stage", stage + 1, hits.joinToString { names[it.index] })))
                    val timeline = hits.flatMap { (index, hit) -> hit.events.mapIndexed { i, event -> Triple("$index:$i", event, names[index]) } }.sortedBy { it.second.time }
                    val shifts = lifeShifts(timeline.map { it.second })
                    timeline.forEachIndexed { i, (key, event, name) -> if (LogKind.of(event) in shown) add(PackRow.Line(key, event, name, shifts[i], start + event.time)) }
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
        items(rows, key = { it.key }, contentType = { it is PackRow.Head }) { row ->
            when (row) {
                is PackRow.Head -> Caption(row.text)
                is PackRow.Line -> EventRow(row.event, row.at, row.name, row.shift, onOpen?.let { open -> { event: CombatEvent -> open(event, row.name) } })
            }
        }
    }
}

@Composable private fun EventRow(event: CombatEvent, at: Double, monster: String, shift: LifeShift?, onOpen: ((CombatEvent) -> Unit)? = null) {
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

/**
 * Шипы и отражение - удар назад от ударенного; уклонение и блок - у того, по кому били; удар по неуязвимому - своей строкой «без урона»;
 * умение называет удар или кто и на кого; заметка - что случилось без удара.
 */
private fun logLine(event: CombatEvent, monster: String): String {
    val damage = event.damage.roundToInt()
    val hero = event.actor == Side.HERO
    val line = when (event.action) {
        Action.REFLECT -> if (hero) ui("expedition.log_reflect_you", monster, damage) else ui("expedition.log_reflect_they", monster, damage)

        Action.TICK -> {
            val ailment = event.ailment?.let { ui(it.key()) }.orEmpty()
            if (hero) ui("expedition.log_tick_they", monster, damage, ailment) else ui("expedition.log_tick_you", damage, ailment)
        }

        Action.ATTACK -> when (event.kind) {
            HitKind.HIT -> if (hero) ui("expedition.log_you_hit", damage) else ui("expedition.log_they_hit", monster, damage)
            HitKind.CRIT -> if (hero) ui("expedition.log_you_crit", damage) else ui("expedition.log_they_crit", monster, damage)
            HitKind.EVADED -> if (hero) ui("expedition.log_they_evade", monster) else ui("expedition.log_you_evade")
            HitKind.BLOCKED -> if (hero) ui("expedition.log_they_block", monster) else ui("expedition.log_you_block")
            HitKind.INVULNERABLE -> if (hero) ui("expedition.log_you_hit_invulnerable") else ui("expedition.log_they_hit_invulnerable", monster)
        }

        Action.SKILL -> {
            val skill = SkillText.title(event.skill.orEmpty())
            when {
                event.kind == HitKind.EVADED -> if (hero) ui("expedition.log_skill_evaded", skill, monster) else ui("expedition.log_skill_you_evade", monster, skill)

                event.kind == HitKind.BLOCKED -> if (hero) ui("expedition.log_skill_blocked", skill, monster) else ui("expedition.log_skill_you_block", monster, skill)

                event.kind == HitKind.INVULNERABLE -> if (hero) ui("expedition.log_skill_invulnerable", skill) else ui("expedition.log_they_skill_invulnerable", monster, skill)

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

        Action.NOTE -> noteLine(event, monster)
    }
    val marks = buildList {
        event.type?.takeIf { event.action.strikes && event.landed && event.damage > 0 && it != DamageType.PHYSICAL }?.let { add(ui(it.key())) }
        if (event.stunned) add(ui("expedition.stunned"))
        event.inflicted.forEach { add(ui(it.key())) }
    }
    return if (marks.isEmpty()) line else "$line · ${marks.joinToString(" · ")}"
}

internal fun noteLine(event: CombatEvent, monster: String): String {
    (event.trace as? FateTrace)?.let { return fateLine(it) }
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
        NoteKind.RAGE -> ui("expedition.log_note_rage", monster, note.ref)
        NoteKind.SEAL_BROKEN -> ui("expedition.log_note_seal_broken", monster, note.value.roundToInt())
        NoteKind.SEALS_RETURNED -> ui("expedition.log_note_seals_returned", monster, note.value.roundToInt())
        NoteKind.SEAL_LOCK_SKILL -> ui("expedition.log_note_seal_lock", monster, SkillText.title(note.ref), fineNumber(note.value))
        NoteKind.SEAL_LOCK_FLASK -> ui("expedition.log_note_seal_lock", monster, equipmentTitle(note.ref), fineNumber(note.value))
        NoteKind.BOON_STOLEN -> ui("expedition.log_note_boon_stolen", monster, locOr("rift.boon.${note.ref}.name", note.ref))
        NoteKind.LAW -> ui("expedition.log_note_law", monster, locOr("rift.law.${note.ref}.name", note.ref))
        NoteKind.LAW_REWOUND -> ui("expedition.log_note_law_rewound", monster, note.value.roundToInt())
        NoteKind.RECOVER_FLASK -> ui("expedition.log_note_recover_flask", equipmentTitle(note.ref), note.value.roundToInt())
        NoteKind.RECOVER_RECOUP -> ui("expedition.log_note_recover_recoup", note.value.roundToInt())
        NoteKind.RECOVER_WASTE -> ui("expedition.log_note_recover_waste", note.value.roundToInt())
        NoteKind.RECOVER_HALT -> ui("expedition.log_note_recover_halt", equipmentTitle(note.ref))
        NoteKind.LIFE_FLOW -> note.flow?.let(::lifeFlowLine).orEmpty()
        NoteKind.CAST -> castLine("expedition.log_note_cast", event, monster, SkillText.title(note.ref), fineNumber(note.value))
        NoteKind.CAST_BROKEN -> castLine("expedition.log_note_cast_broken", event, monster, SkillText.title(note.ref), fineNumber(note.value))
        NoteKind.CAST_NO_MANA -> ui("expedition.log_note_cast_no_mana", SkillText.title(note.ref), fineNumber(note.value))
        NoteKind.PROVOKED -> ui("expedition.log_note_provoked", monster, fineNumber(note.value))
        NoteKind.MARK_BURST -> ui("expedition.log_note_mark_burst", SkillText.title(note.ref), note.value.roundToInt())
    }
}

/** Строка применения умения: своя у героя, у врага - с его именем ([key] с `_foe`). */
private fun castLine(key: String, event: CombatEvent, monster: String, vararg args: Any): String = if (event.actor == Side.HERO) {
    ui(key, *args)
} else {
    ui("${key}_foe", monster, *args)
}

/** Итог здоровья за секунду: «Здоровье −12/с · реген +8 · деген 2%». */
private fun lifeFlowLine(flow: LifeFlow): String = ui("expedition.log_note_life_flow", flowRate(flow.net), flowParts(flow))

/**
 * Срабатывание Предначертания: «✦ дар: что сделал рычаг» словами `expedition.fate_beat.<срабатывание>` - числа по порядку,
 * предмет (недуг, умение) последним; свёртка частого - «ещё ×N» и итог.
 */
internal fun fateLine(trace: FateTrace): String {
    val subject = when (trace.beat.ref) {
        FateRef.NONE -> null
        FateRef.AILMENT -> Ailment.entries.firstOrNull { it.name == trace.ref }?.let { ui(it.key()) }
        FateRef.SKILL -> SkillText.title(trace.ref)
    }
    val text = ui("expedition.fate_beat.${trace.beat.name}", *(trace.args.map(::fineNumber) + listOfNotNull(subject)).toTypedArray())
    val fate = locOr("fate.${trace.fate}.name", trace.fate)
    return if (trace.repeats > 0) ui("expedition.log_fate_fold", fate, trace.repeats, text) else ui("expedition.log_fate", fate, text)
}

/** Цвет строки журнала; итог здоровья - убыль красным, прибыль зелёным. */
private fun logColour(event: CombatEvent): Color = when {
    (event.trace as? NoteTrace)?.kind == NoteKind.LIFE_FLOW -> flowTint((event.trace as NoteTrace).value)
    (event.trace as? NoteTrace)?.kind?.recovery == true -> Vital
    event.trace is FateTrace -> FateInk.GoldLight
    event.action == Action.NOTE -> Rune
    event.action == Action.FLASK -> Vital
    event.action == Action.SKILL && event.damage <= 0 && event.landed -> if (event.actor == Side.HERO) Gold else Color(0xFFE9A0A0)
    event.action == Action.TICK || event.action == Action.REFLECT -> tickTint(event.ailment, event.type).copy(alpha = .85f)
    event.kind == HitKind.CRIT -> Color(0xFFFFD34A)
    event.kind == HitKind.HIT -> if (event.actor == Side.HERO) Parchment else Color(0xFFE9A0A0)
    event.kind == HitKind.INVULNERABLE -> FrameGoldBright
    else -> Muted
}
