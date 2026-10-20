package com.sperance.exileforge.core.model.crafts

import com.sperance.exileforge.core.model.trade.Cost
import com.sperance.exileforge.rules.content.CraftingRules
import com.sperance.exileforge.rules.content.CraftsRules
import com.sperance.exileforge.rules.content.Job
import com.sperance.exileforge.rules.content.JobExtra
import com.sperance.exileforge.rules.content.JobInput
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.roll.ActiveWork
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.ProfessionProgress
import com.sperance.exileforge.rules.roll.Work
import com.sperance.exileforge.rules.roll.WorkBonus
import com.sperance.exileforge.rules.roll.WorkBreakdown
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.rules.roll.WorkLayers
import com.sperance.exileforge.rules.roll.WorkTally
import kotlinx.serialization.Serializable

/** The crafts part of the hero snapshot: each profession's progress and the work under way. */
@Serializable data class WorkState(val professions: Map<String, ProfessionProgress> = emptyMap(), val work: ActiveWork? = null)

/** One work of a profession as the hero has it now: the rules' seconds, and the cycle and the «nothing» chance their gear makes of it. */
@Serializable data class JobView(
    /** The work itself: each kind with its own fields — a stack's output, a smith's band, a chart's level band. */
    val job: Job,
    val cycleMillis: Long = 0,
    /** The hero's own odds: the empty cycle's and each find's, after level and gear. */
    val nothing: Double = 0.0,
    val extra: List<JobExtra> = emptyList(),
    /** Whether the hero may take it: a chart needs an open zone in its level band, a choosing work a variant. */
    val open: Boolean = true,
    /** A variant of a choosing work: what was chosen — the item it makes; blank on the work itself. */
    val choice: String = "",
    /** The variants of a choosing work — a condensed essence, a skill book of the hero's class — each a plain work of its own. */
    val options: List<JobView> = emptyList(),
    /** Опыт профессии за удачный цикл - присылает сервер по кривой опыта ремесла с бонусом опыта героя ([Work.experience]). */
    val experience: Double = 0.0,
) {
    val code: String get() = job.code
    val kind: JobKind get() = job.kind
    val level: Int get() = job.level
    val seconds: Double get() = job.seconds
    val output: String get() = job.output
    val inputs: List<JobInput> get() = job.inputs

    /** Расход цикла с добавками [chosen] - тот же, что проверяет сервер при запуске. */
    fun cycleCost(chosen: List<String> = emptyList()): Cost = Cost(Work.perCycle(job, chosen))
}

/** A profession of the hero: its level and experience, the tool in its slot, its bonus and works. */
@Serializable data class ProfessionView(
    val code: String,
    val tool: String = "",
    val level: Int = 1,
    val experience: Double = 0.0,
    val next: Double? = null,
    val equipped: ItemInstance? = null,
    val jobs: List<JobView> = emptyList(),
    /** Бонус ремесла по слоям источников (инструмент, лист героя без дара, гильдия, дар) - сервер считает их одной функцией правил. */
    val layers: WorkLayers = WorkLayers(),
) {
    /** Бонус ремесла профессии - итог слоёв [layers], тот же, что берёт досчёт. */
    val bonus: WorkBonus get() = layers.total

    /** Открыта ли работа [job] герою: профессия доросла до её уровня и сервер её пускает ([JobView.open]). */
    fun opens(job: JobView): Boolean = job.level <= level && job.open

    /** Разбор мер работы [job] по источникам при правилах ремёсел [rules]: итог каждой меры - число, что показывает и досчитывает сервер. */
    fun breakdown(rules: CraftsRules, job: JobView): WorkBreakdown = WorkBreakdown.of(rules, job.job, level, layers)
}

/** The work under way: cycles are counted up to [settledAt]; the next lands at [nextAt] (epoch millis, the server's clock). */
@Serializable data class WorkView(
    val profession: String,
    val job: String,
    val settledAt: Long = 0,
    val cycleMillis: Long = 0,
    val nextAt: Long = 0,
    val additives: List<String> = emptyList(),
    val startedAt: Long = 0,
    val totals: WorkTally = WorkTally(),
    /** The variant chosen, blank for a work without a choice. */
    val choice: String = "",
    /** Когда работа встала: игрока не было `offlineHours` (часы сервера); 0 - идёт. Продолжает её [com.sperance.exileforge.core.network.CraftsClient.resume]. */
    val stoppedAt: Long = 0,
) {
    /** Работа встала по отлучке и ждёт, когда её продолжат: циклы не идут. */
    val stopped: Boolean get() = stoppedAt > 0

    /** Сколько работа шла до остановки по отлучке; у идущей - 0. */
    val ranMillis: Long get() = if (stopped) (stoppedAt - startedAt).coerceAtLeast(0L) else 0L

    /**
     * Сколько прошло текущего цикла на часах сервера [now]: по кругу от [settledAt], так что полоса идёт
     * дальше, даже пока сервер не пересчитал работу, а не стоит на 100%. До [settledAt] и у вставшей работы - ноль.
     */
    fun phase(now: Long): Long = if (cycleMillis > 0 && !stopped) (now - settledAt).coerceAtLeast(0L) % cycleMillis else 0L

    /** Ближайшая граница цикла позже [now] на часах сервера: на ней работа пересчитывается с сервера. */
    fun nextBoundary(now: Long): Long = maxOf(now, settledAt) - phase(now) + cycleMillis

    /**
     * Что эта работа сделала с начала, из вещей героя [items]: метка происхождения сервера ([com.sperance.exileforge.rules.roll.ItemOrigin]) - профессия работы и миг
     * не раньше [startedAt]. Проданное и отданное из списка уходит.
     */
    fun made(items: Iterable<ItemInstance>): List<ItemInstance> = items.filter { item -> item.origin?.let { it.profession == profession && it.at >= startedAt } == true }
}

/** The work [code] as the hero runs it: the chosen variant of a choosing work, else the work itself. */
fun ProfessionView.job(code: String, choice: String = ""): JobView? = jobs.firstOrNull { it.code == code }?.let { job -> job.options.firstOrNull { it.choice == choice } ?: job.takeIf { choice.isEmpty() } }

/** The crafts of a hero, whole: every answer of the crafts routes is this. */
@Serializable data class CraftsState(
    val now: Long = 0,
    val rules: CraftsRules? = null,
    val professions: List<ProfessionView> = emptyList(),
    val work: WorkView? = null,
    val gains: WorkGains = WorkGains(),
    /** Правила ремесла сервера: добавки работ ([CraftingRules.additives]) и сколько разных берёт цикл ([CraftingRules.maxAdditives]). */
    val crafting: CraftingRules,
    /** До какого мига работа была досчитана прежде (часы сервера): [gains] покрывают время от него; ноль - досчёта не было. */
    val from: Long = 0,
)

/** The work under way, as its profession lists it: the chosen variant when it has one. */
val CraftsState.running: JobView? get() = work?.let { w -> professions.firstOrNull { it.code == w.profession }?.job(w.job, w.choice) }
