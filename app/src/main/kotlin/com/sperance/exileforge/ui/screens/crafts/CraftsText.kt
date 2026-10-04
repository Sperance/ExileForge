package com.sperance.exileforge.ui.screens.crafts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.choiceTitle
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.professionDescription
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.core.model.crafts.WorkView
import com.sperance.exileforge.core.model.crafts.job
import com.sperance.exileforge.core.model.crafts.running
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.JobInput
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.rules.roll.WorkTally
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.ceil

/** Строки и числа ремёсел (3.80.14): названия, итоги, подсказки по уровню и часовой выработке. */
/** The dictionary's name of a work; the tab bar's badge (ForgeApp) names it by this package, so it stays here. */
fun jobTitle(code: String): String = com.sperance.exileforge.core.display.jobTitle(code)

/** The work under way by name, with its choice. */
fun workTitle(work: WorkView): String = com.sperance.exileforge.core.display.workTitle(work.job, work.choice)

/**
 * What an answer brought, as one line: «+2 Iron Ore, +1 Bark», the pieces a smith or a cartographer
 * made, that the bag ran dry, or that the cycles came up empty.
 */
fun gainsLine(gains: WorkGains): String = listOfNotNull(
    gains.items.entries.joinToString { (code, amount) -> ui("crafts.gain", amount, itemTitle(code)) }.ifBlank { null },
    gains.equipment.takeIf { it.isNotEmpty() }?.let { made -> ui("crafts.made", made.joinToString { equipmentTitle(it.template) }) },
    ui("crafts.starved").takeIf { gains.starved },
).joinToString(" · ").ifBlank { ui("crafts.gain_nothing", gains.cycles) }

/** How many of a stack the bag holds, by the item's code — a material, an orb, an essence or a book; 0 before the hero is read. */
fun bagCount(s: ForgeState, code: String): Long = s.bagAmount(code) ?: 0L

/** What a work makes, in words: the stack, the smith's range or the cartographer's location. */
fun jobProduct(job: JobView): String = when (job.kind) {
    JobKind.ITEM -> itemTitle(job.output)

    JobKind.EQUIPMENT -> ui("crafts.kind_equipment", job.band.getOrElse(0) { 1 }, job.band.getOrElse(1) { 1 })

    JobKind.MAP -> ui("crafts.kind_map", regionTitle(job.region))

    JobKind.FLASK -> equipmentTitle(job.output)

    // Choosing works (3.45.0): the variant picked in the sheet is a plain ITEM work.
    JobKind.BOOK -> ui("crafts.kind_book", job.band.getOrElse(0) { 1 })

    JobKind.CONDENSE -> ui("crafts.kind_condense")

    JobKind.REFINE -> ui("crafts.kind_refine")

    JobKind.JEWEL -> ui("crafts.kind_jewel", job.band.getOrElse(0) { 1 }, job.band.getOrElse(1) { 1 })
}

/** A crafting profession spends materials; a gathering one only brings them. The works say which, not a list of codes. */
val ProfessionView.crafting get() = jobs.any { it.inputs.isNotEmpty() }

/**
 * How long the bag keeps the work going: the input that runs out first, what the bag holds of it and
 * how many cycles that pays for — every additive the work was started with is spent each cycle too.
 * Display only: the server stops the work when a cycle cannot be paid.
 */
fun stockLine(s: ForgeState, work: WorkView, job: JobView): String? {
    val inputs = job.inputs + work.additives.map { JobInput(it, 1) }
    val scarce = inputs.filter { it.amount > 0 }.minByOrNull { bagCount(s, it.item) / it.amount } ?: return null
    val have = bagCount(s, scarce.item)
    val cycles = (have / scarce.amount).toInt()
    return if (cycles == 0) {
        ui("crafts.stock_empty", itemTitle(scarce.item), have)
    } else {
        ui("crafts.stock", itemTitle(scarce.item), have, cycles, plural("crafts.cycles", cycles), eta(cycles * work.cycleMillis))
    }
}

/**
 * How long the work still needs to lift its profession a level (3.2.0), counted here: the experience a
 * cycle brings past its «nothing» chance, the cycles that leaves, less what the running cycle has done.
 */
@Composable internal fun levelLine(crafts: Crafts, work: WorkView, offset: Long): String? {
    val profession = crafts.state?.professions?.firstOrNull { it.code == work.profession } ?: return null
    val next = profession.next ?: return ui("crafts.level_top")
    val job = profession.job(work.job, work.choice) ?: return null
    val perCycle = job.experience * (1 + profession.bonus.experience.coerceAtLeast(0.0) / 100) * (1 - job.nothing / 100)
    if (perCycle <= 0 || work.cycleMillis <= 0) return null
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }
    val cycles = ceil((next - profession.experience).coerceAtLeast(0.0) / perCycle).toLong()
    val running = (now + offset - work.settledAt).coerceIn(0L, work.cycleMillis)
    return ui("crafts.level_eta", profession.level + 1, eta((cycles * work.cycleMillis - running).coerceAtLeast(0L)))
}

internal fun eta(millis: Long): String {
    val seconds = (millis + 999) / 1000
    return when {
        seconds < 60 -> ui("crafts.eta_seconds", seconds)
        seconds < 3600 -> ui("crafts.eta_minutes", (seconds + 59) / 60)
        else -> ui("crafts.eta_hours", seconds / 3600, seconds % 3600 / 60)
    }
}

/** How long a work runs: «2 ч 05 мин», «14 мин 03 с», «42 с». */
internal fun duration(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    return when {
        seconds >= 3600 -> ui("crafts.duration_hours", seconds / 3600, "%02d".format(seconds % 3600 / 60))
        seconds >= 60 -> ui("crafts.duration_minutes", seconds / 60, "%02d".format(seconds % 60))
        else -> ui("crafts.duration_seconds", seconds)
    }
}

/** How often the cycle's figures are redrawn. */
internal const val CLOCK_TICK = 200L

/**
 * What the work under way brings in an hour on average (3.24.0), counted from the job as the server
 * sends it — the cycle, the «nothing» chance and each find's chance already made of the hero's gear —
 * and the yield bonus's extra units: the cycles an hour holds, less the empty ones, times what one brings.
 * Display only: the server's dice decide.
 */
internal fun hourlyLine(crafts: Crafts, work: WorkView): String? {
    val profession = crafts.state?.professions?.firstOrNull { it.code == work.profession } ?: return null
    val job = profession.job(work.job, work.choice) ?: return null
    if (work.cycleMillis <= 0) return null
    val landed = landed(work.cycleMillis, job)
    val made = jobProduct(job) to landed * yieldFactor(profession)
    val finds = job.extra.map { itemTitle(it.item) to landed * it.chance / 100 }
    val figures = (listOf(made) + finds).filter { it.second > 0 }.takeIf { it.isNotEmpty() } ?: return null
    return ui("crafts.per_hour", figures.joinToString(" · ") { (name, amount) -> ui("crafts.per_hour_item", fineNumber(amount), name) })
}

/** The cycles an hour of [cycleMillis] brings that do not come back empty. */
internal fun landed(cycleMillis: Long, job: JobView): Double = HOUR_MILLIS / cycleMillis.toDouble() * (1 - job.nothing / 100).coerceAtLeast(0.0)

/** The profession's chance to double a cycle's yield, as the factor it multiplies the average by. */
internal fun yieldFactor(profession: ProfessionView): Double = 1 + profession.bonus.yield.coerceAtLeast(0.0) / 100

/** About how many of its product a work brings an hour, by its own cycle; null for one that has no cycle yet. */
internal fun perHour(job: JobView, profession: ProfessionView): Double? = job.cycleMillis.takeIf { it > 0 }?.let { landed(it, job) * yieldFactor(profession) }

internal const val HOUR_MILLIS = 3_600_000.0

/** A cycle's price against the bag: each material as have/need, short ones in red; «бесплатно» for a work that spends nothing. */
internal fun cycleCost(s: ForgeState, job: JobView): AnnotatedString = buildAnnotatedString {
    if (job.inputs.isEmpty()) {
        append(ui("crafts.free"))
        return@buildAnnotatedString
    }
    job.inputs.forEachIndexed { i, input ->
        if (i > 0) append(" + ")
        val have = bagCount(s, input.item)
        withStyle(SpanStyle(color = if (have >= input.amount) Parchment else LifeRed)) {
            append("${itemTitle(input.item)} ${ui("crafts.ratio", have, input.amount)}")
        }
    }
}

internal const val TILES = 3

/**
 * The variants a choosing work offers now: those the profession's level reaches — and, for the condensing,
 * only the essences the bag can feed a cycle of, else a hundred and forty chips would bury the few that can run.
 */
internal fun choices(s: ForgeState, profession: ProfessionView, work: JobView): List<JobView> = work.options.filter { option -> option.level <= profession.level && (work.kind != JobKind.CONDENSE || option.inputs.all { bagCount(s, it.item) >= it.amount }) }
