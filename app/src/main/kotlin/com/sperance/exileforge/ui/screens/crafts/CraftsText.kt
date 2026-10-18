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
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.choiceTitle
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.professionDescription
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.display.roman
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ruleRefusal
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.core.model.crafts.WorkView
import com.sperance.exileforge.core.model.crafts.job
import com.sperance.exileforge.core.model.crafts.running
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.Job
import com.sperance.exileforge.rules.content.JobInput
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.Desecrations
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.Work
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.rules.roll.WorkTally
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.components.rememberNow
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.theme.*
import kotlin.math.ceil

/** Строки и числа ремёсел: названия, итоги, подсказки по уровню и часовой выработке. */
/** The dictionary's name of a work; the tab bar's badge (ForgeApp) names it by this package, so it stays here. */
fun jobTitle(code: String): String = com.sperance.exileforge.core.display.jobTitle(code)

/** The work under way by name, with its choice. */
fun workTitle(work: WorkView): String = com.sperance.exileforge.core.display.workTitle(work.job, work.choice)

/** Всплывашка сбора у полосы цикла: добыча, сделанное и голод сумки, с опытом - «+1 Медная руда · +2 опыта», а без добычи - «пусто». */
fun harvestLine(gains: WorkGains): String = (gainParts(gains) + listOfNotNull(gains.experience.takeIf { it > 0 }?.let { ui("crafts.experience_gain", number(it)) }))
    .joinToString(" · ").ifBlank { ui("crafts.harvest_empty") }

private fun gainParts(gains: WorkGains): List<String> = listOfNotNull(
    gains.items.entries.joinToString { (code, amount) -> ui("crafts.gain", amount, itemTitle(code)) }.ifBlank { null },
    gains.equipment.takeIf { it.isNotEmpty() }?.let { pieces -> ui("crafts.made", pieces.joinToString { equipmentTitle(it.template) }) },
    ui("crafts.starved").takeIf { gains.starved },
)

/** How many of a stack the bag holds, by the item's code — a material, an orb, an essence or a book; 0 before the hero is read. */
fun bagCount(game: GameUi, code: String): Long = game.bagAmount(code) ?: 0L

/**
 * What a work makes, in words: the stack, the smith's range or the cartographer's location.
 *
 * Работа с выбором - выбранный вариант как обычная работа: инструмент кузнеца - база инструмента, ритуал тира - умение, резьба - руна на
 * выходе, Осквернение - та же вещь с запечатанной судьбоносной строкой.
 */
fun jobProduct(view: JobView): String = when (val job = view.job) {
    is Job.Item -> itemTitle(job.output)
    is Job.Equipment -> ui("crafts.kind_equipment", job.band.getOrElse(0) { 1 }, job.band.getOrElse(1) { 1 })
    is Job.Chart -> ui("crafts.kind_map", regionTitle(job.region))
    is Job.Flask -> equipmentTitle(job.output)
    is Job.Book -> ui("crafts.kind_book", job.upTo)
    is Job.Condense -> ui("crafts.kind_condense")
    is Job.Refine -> ui("crafts.kind_refine")
    is Job.Jewel -> ui("crafts.kind_jewel", job.band.getOrElse(0) { 1 }, job.band.getOrElse(1) { 1 })
    is Job.Tool -> if (job.output.isNotEmpty()) equipmentTitle(job.output) else ui("crafts.kind_tool", job.band.getOrElse(0) { 1 }, job.band.getOrElse(1) { 1 })
    is Job.Evolve -> if (job.skill.isNotEmpty()) ui("crafts.kind_evolve_skill", SkillText.title(job.skill), roman(job.tier)) else ui("crafts.kind_evolve", roman(job.tier))
    is Job.Carve -> ui("crafts.kind_carve", job.upTo)
    is Job.Desecrate -> ui("crafts.kind_desecrate")
}

/**
 * Вещи тайника, что примет работа над вещью героя [job] (`Job.targetsItem`): те, что пропустят правила, - та же проверка,
 * что у сервера при запуске; у прочих работ - пусто.
 */
internal fun itemTargets(game: GameUi, job: Job): List<ItemView> {
    val index = game.index ?: return emptyList()
    val check: (ItemView) -> Unit = when (job) {
        is Job.Desecrate -> Desecrations(index).let { rules -> { view -> rules.check(view.item, view.template) } }
        else -> return emptyList()
    }
    return game.hero?.stash.orEmpty().mapNotNull { game.view(it) }.filter { ruleRefusal { check(it) } == null }
}

/** Вещь, что держит идущая работа (Осквернение): её нет среди вещей героя, пока работа идёт. */
internal val GameUi.heldWork: ItemInstance? get() = hero?.crafts?.work?.held

/** Имя идущей работы [work]: у работы над вещью - с именем этой вещи, иначе с выбранным вариантом. */
internal fun workTitle(game: GameUi, work: WorkView): String = game.heldWork?.let { held -> "${jobTitle(work.job)} · ${game.view(held)?.title ?: equipmentTitle(held.template)}" } ?: workTitle(work)

/** A crafting profession spends materials; a gathering one only brings them. The works say which, not a list of codes. */
val ProfessionView.crafting get() = jobs.any { it.inputs.isNotEmpty() }

/**
 * Сколько сумка ещё держит работу: вход, что кончится первым, сколько его в сумке и на сколько циклов хватит - каждая примесь,
 * с которой работу начали, тоже уходит каждый цикл. Пара - код этого входа и строка; работа без входа - null. Только показ:
 * сервер сам остановит работу, когда цикл нечем оплатить.
 */
fun stock(game: GameUi, work: WorkView, job: JobView): Pair<String, String>? {
    val inputs = job.inputs + work.additives.map { JobInput(it, 1) }
    val scarce = inputs.filter { it.amount > 0 }.minByOrNull { bagCount(game, it.item) / it.amount } ?: return null
    val have = bagCount(game, scarce.item)
    val cycles = (have / scarce.amount).toInt()
    return scarce.item to if (cycles == 0) {
        ui("crafts.stock_empty", itemTitle(scarce.item), have)
    } else {
        ui("crafts.stock", itemTitle(scarce.item), have, cycles, plural("crafts.cycles", cycles), eta(cycles * work.cycleMillis))
    }
}

/** Запас входа идущей работы строкой: иконка входа, что кончится первым, и [stock] цветом предупреждения. */
@Composable internal fun StockRow(game: GameUi, work: WorkView, job: JobView) {
    val (code, line) = stock(game, work, job) ?: return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BagIcon(code, Modifier.size(18.dp))
        Text(line, color = Caution, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Строка уровня профессии под полосой цикла: «Ур. 14», тонкая полоса опыта и сколько работа ещё идёт до следующего - «до 15 · 42 мин»,
 * считая здесь: опыт цикла сверх шанса «впустую», сколько циклов это оставляет, минус пройденное идущим циклом. На последнем уровне -
 * «макс.». Опыт удачного цикла - [JobView.experience] по кривой сервера, не поле работы.
 */
@Composable internal fun LevelRow(profession: ProfessionView, job: JobView, work: WorkView, offset: Long) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(ui("crafts.level", profession.level), color = Rune, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
        LinearProgressIndicator(progress = { share(profession) }, modifier = Modifier.weight(1f).height(3.dp), color = Vital, trackColor = PanelRaised)
        val next = profession.next
        val perCycle = job.experience * (1 + profession.bonus.experience.coerceAtLeast(0.0) / 100) * (1 - job.nothing / 100)
        when {
            next == null -> LevelNote(ui("crafts.level_top_short"))

            perCycle > 0 && work.cycleMillis > 0 -> {
                val cycles = ceil((next - profession.experience).coerceAtLeast(0.0) / perCycle).toLong()
                LevelEta(profession.level + 1, cycles * work.cycleMillis, work, offset)
            }
        }
    }
}

/**
 * Сколько до уровня [level] живьём: свои часы раз в секунду - перерисовывается только эта подпись, не плашка работы вокруг
 * (часы в листовом элементе, вкладка ремёсел не теряет кадров).
 */
@Composable private fun LevelEta(level: Int, total: Long, work: WorkView, offset: Long) {
    val now by rememberNow(1000L)
    LevelNote(ui("crafts.level_eta", level, eta((total - work.phase(now + offset)).coerceAtLeast(0L))))
}

@Composable private fun LevelNote(text: String) = Text(text, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)

/**
 * Подсказка листа работы при нехватке на цикл (с примесями [additives]): первый недостающий материал, сколько его есть и нужно, и где он
 * добывается - самая ранняя работа-стопка, что его даёт; хватает - null.
 */
internal fun shortNote(game: GameUi, job: JobView, additives: List<String>): String? {
    val (code, need) = Work.perCycle(job.job, additives).entries.firstOrNull { (code, amount) -> bagCount(game, code) < amount } ?: return null
    val line = ui("crafts.short_note", itemTitle(code), bagCount(game, code), need)
    val source = game.index?.professions?.professions.orEmpty()
        .flatMap { profession -> profession.jobs.filterIsInstance<Job.Item>().filter { it.output == code }.map { profession to it } }
        .minByOrNull { it.second.level } ?: return line
    return line + " " + ui("crafts.short_source", professionTitle(source.first.code), jobTitle(source.second.code), source.second.level)
}

/** Время часами: «3:41», «1:05:09». */
internal fun clock(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds % 3600 / 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60)
}

internal fun eta(millis: Long): String {
    val seconds = (millis + 999) / 1000
    return when {
        seconds < 60 -> ui("crafts.eta_seconds", seconds)
        seconds < 3600 -> ui("crafts.eta_minutes", (seconds + 59) / 60)
        else -> ui("crafts.eta_hours", seconds / 3600, seconds % 3600 / 60)
    }
}

/** Время в работе для плитки итога: с часа - часами «1:05», меньше - «≈ 12 мин», «≈ 42 с». */
internal fun workAge(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    return when {
        seconds >= 3600 -> "%d:%02d".format(seconds / 3600, seconds % 3600 / 60)
        seconds >= 60 -> ui("crafts.eta_minutes", seconds / 60)
        else -> ui("crafts.eta_seconds", seconds)
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

/** Выход в час одной фишкой: стопка [code] (работа делает не стопку - null, фишка несёт иконку работы) и сколько её в среднем за час. */
internal class HourYield(val code: String?, val amount: Double)

/**
 * What the work under way brings in an hour on average, counted from the job as the server
 * sends it — the cycle [cycleMillis], the «nothing» chance and each find's chance already made of the hero's gear —
 * and the yield bonus's extra units: the cycles an hour holds, less the empty ones, times what one brings.
 * Display only: the server's dice decide. Первым - что работа делает, за ним находки; нулевые не пишутся.
 */
internal fun hourYields(profession: ProfessionView, job: JobView, cycleMillis: Long): List<HourYield> {
    if (cycleMillis <= 0) return emptyList()
    val landed = landed(cycleMillis, job)
    val made = HourYield(job.output.takeIf { it.isNotBlank() && job.kind in STACK_WORKS }, landed * yieldFactor(profession))
    val finds = job.extra.map { HourYield(it.item, landed * it.chance / 100) }
    return (listOf(made) + finds).filter { it.amount > 0 }
}

/** Работы, чей выход - стопка сумки: её фишка открывает карточку материала. */
private val STACK_WORKS = setOf(JobKind.ITEM, JobKind.CONDENSE, JobKind.REFINE)

/** The cycles an hour of [cycleMillis] brings that do not come back empty. */
internal fun landed(cycleMillis: Long, job: JobView): Double = HOUR_MILLIS / cycleMillis.toDouble() * (1 - job.nothing / 100).coerceAtLeast(0.0)

/** The profession's chance to double a cycle's yield, as the factor it multiplies the average by. */
internal fun yieldFactor(profession: ProfessionView): Double = 1 + profession.bonus.yield.coerceAtLeast(0.0) / 100

/** About how many of its product a work brings an hour, by its own cycle; null for one that has no cycle yet. */
internal fun perHour(job: JobView, profession: ProfessionView): Double? = job.cycleMillis.takeIf { it > 0 }?.let { landed(it, job) * yieldFactor(profession) }

internal const val HOUR_MILLIS = 3_600_000.0

/** A cycle's price against the bag: each material as have/need, short ones in red; «бесплатно» for a work that spends nothing. */
internal fun cycleCost(game: GameUi, job: JobView): AnnotatedString = buildAnnotatedString {
    if (job.inputs.isEmpty()) {
        append(ui("crafts.free"))
        return@buildAnnotatedString
    }
    job.inputs.forEachIndexed { i, input ->
        if (i > 0) append(" + ")
        val have = bagCount(game, input.item)
        withStyle(SpanStyle(color = if (have >= input.amount) Parchment else LifeRed)) {
            append("${itemTitle(input.item)} ${ui("crafts.ratio", have, input.amount)}")
        }
    }
}

internal const val TILES = 3

/**
 * The variants a choosing work offers: every one, whatever the profession's level (a locked one shows its lock and
 * the sheet names the level) — but for the condensing only the essences the bag can feed a cycle of, else a hundred and
 * forty chips would bury the few that can run.
 *
 * Ритуал тира - только изученные умения; почему вариант закрыт, называет низ листа.
 */
internal fun choices(game: GameUi, work: JobView): List<JobView> = work.options.filter { option ->
    when (work.kind) {
        JobKind.CONDENSE -> option.inputs.all { bagCount(game, it.item) >= it.amount }
        JobKind.EVOLVE -> (game.hero?.skills?.level(option.choice) ?: 0) > 0
        else -> true
    }
}
