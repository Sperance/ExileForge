package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.nodeTitle
import com.sperance.exileforge.core.display.shareNumber
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statPercent
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.sheet.Grant
import com.sperance.exileforge.rules.sheet.Share
import com.sperance.exileforge.rules.sheet.SourceKind
import com.sperance.exileforge.rules.sheet.StatBreakdown
import com.sperance.exileforge.rules.sheet.StatOperation
import com.sperance.exileforge.rules.sheet.StatSource
import kotlin.math.abs
import kotlin.math.ln

/** The cards of a figure's window, in the order they are read; the colour is the UI's, keyed by the kind. */
enum class ShareKind { BASE, ATTRIBUTE, CLASS, NODE, ITEM, PET, SKILL, OTHER, AFTER }

/** One source on a card: what it is, what it gives, a grey line under it, and the stat a tap goes to. */
data class ShareRow(val title: String, val value: String, val note: String? = null, val link: String? = null)

/**
 * A passive skill's [lines] on a figure and what they add to it: the passives are laid on in a fight, not on the
 * sheet, so their [gain] is the sheet's figure with them less without; a [lowLife] bonus holds only at low life.
 */
data class PassiveShare(val skill: String, val lines: List<StatLine>, val gain: Double, val lowLife: Boolean)

/**
 * The passives on one figure: a row each, and the [total] they give together outside low life — the lines laid on at
 * once, since an increase multiplies what the others add and a sum of each one's own gain would miss it.
 */
data class PassiveShares(val rows: List<PassiveShare> = emptyList(), val total: Double = 0.0)

/** A card of one kind of source: its rows and what they give together, one figure per operation. */
data class ShareCard(val kind: ShareKind, val rows: List<ShareRow>, val summary: String)

/**
 * A figure's window as it is read: the cards, each kind's share of the total for the bar (positive
 * only), the formula in one line, and what the figure gives other stats right now.
 */
data class StatExplanation(
    val stat: String,
    val total: String,
    val cards: List<ShareCard>,
    val weights: Map<ShareKind, Double>,
    val formula: String,
    val grants: List<ShareRow>,
)

/**
 * Takes a [StatBreakdown] apart into what the window prints: sources named from the hero — a class,
 * a node, a worn item by name and slot, a pet — a conversion under the stat it takes from.
 */
class StatExplainer(private val game: GameUi) {
    private val hero = game.hero
    private val index = game.index

    fun explain(breakdown: StatBreakdown, grants: List<Grant>, holders: (String) -> List<StatSource>, passives: PassiveShares = PassiveShares()): StatExplanation {
        val stat = breakdown.stat
        val cards = mutableListOf<ShareCard>()
        if (breakdown.base != 0.0) {
            cards += ShareCard(
                ShareKind.BASE,
                listOf(ShareRow(hero?.let { ui("stat.src.base", classTitle(it.heroClass), it.level) } ?: ui("stat.kind.BASE"), fmt(stat, breakdown.base, Op.ADD, breakdown.percent, sign = false))),
                fmt(stat, breakdown.base, Op.ADD, breakdown.percent, sign = false),
            )
        }
        breakdown.shares.groupBy(::kindOf).toSortedMap().forEach { (kind, shares) ->
            cards += ShareCard(kind, merged(shares).map { row(stat, it, breakdown.percent) }, summary(stat, shares, breakdown.percent))
        }
        // Every passive that names the figure has its row, an increase with nothing to increase too: «+10% · в бою: +0».
        if (passives.rows.isNotEmpty()) {
            cards += ShareCard(
                ShareKind.SKILL,
                passives.rows.map { ShareRow(SkillText.title(it.skill), it.lines.joinToString(", ") { line -> fmt(stat, line.value, line.op, breakdown.percent) }, passiveNote(stat, it, breakdown)) },
                signed(stat, passives.total, breakdown.percent),
            )
        }
        if (breakdown.shifts.isNotEmpty()) {
            cards += ShareCard(
                ShareKind.AFTER,
                breakdown.shifts.map { ShareRow(shiftTitle(it.source, holders), signed(stat, it.delta, breakdown.percent), it.from?.let { from -> ui("stat.from", statTitle(from)) }) },
                signed(stat, breakdown.shifts.sumOf { it.delta }, breakdown.percent),
            )
        }
        return StatExplanation(
            stat,
            fmt(stat, breakdown.total, Op.ADD, breakdown.percent, sign = false),
            cards,
            weights(breakdown),
            formula(breakdown),
            grants.map { ShareRow(statTitle(it.stat), fmt(it.stat, it.value, it.op, index?.stats?.isPercent(it.stat) == true), link = it.stat) },
        )
    }

    private fun kindOf(share: Share): ShareKind = when {
        share.perStat != null -> ShareKind.ATTRIBUTE

        else -> when (share.source?.kind) {
            SourceKind.CLASS -> ShareKind.CLASS
            SourceKind.NODE -> ShareKind.NODE
            SourceKind.ITEM -> ShareKind.ITEM
            SourceKind.PET -> ShareKind.PET
            else -> ShareKind.OTHER
        }
    }

    /**
     * Sources of one name and one operation as one row (3.81.0): ten tree nodes «Здоровье» or two rings of the same name
     * read as their sum, not ten rows. A conversion keeps its own row - its rule is told on it.
     */
    private fun merged(shares: List<Share>): List<Share> = shares.groupBy { if (it.perStat != null) it else sourceTitle(it.source) to it.op }.values.map { same ->
        if (same.size == 1) {
            same.single()
        } else {
            val value = if (same.first().op == Op.MORE) (same.fold(1.0) { acc, it -> acc * (1 + it.value / 100) } - 1) * 100 else same.sumOf { it.value }
            same.first().copy(value = value, local = same.flatMap { it.local })
        }
    }

    private fun row(stat: String, share: Share, percent: Boolean): ShareRow {
        val value = fmt(stat, share.value, share.op, percent)
        val per = share.perStat ?: return ShareRow(sourceTitle(share.source), value, localNote(share.local))
        val rule = ui("stat.per", fmt(stat, share.each, share.op, percent), statNumber(per, share.per), statTitle(per))
        val by = share.source?.takeIf { it.kind != SourceKind.CLASS }?.let { "${sourceTitle(it)}: " }.orEmpty()
        return ShareRow("${statTitle(per)} ${statNumber(per, share.perValue)}", value, by + rule, link = per)
    }

    /** What a passive adds in a fight; an increase or a «more» over no base says why it adds nothing. */
    private fun passiveNote(stat: String, passive: PassiveShare, b: StatBreakdown): String {
        val idle = abs(passive.gain) < 0.05 && b.flatSum == 0.0 && passive.lines.any { it.op == Op.INCREASED || it.op == Op.MORE }
        val gain = ui(if (idle) "stat.passive.idle" else "stat.passive.gain", signed(stat, passive.gain, b.percent))
        return if (passive.lowLife) "${ui("stat.passive.low_life")} · $gain" else gain
    }

    /** The local lines an item folded into this figure: «локально: +200, +40%». */
    private fun localNote(local: List<StatOperation>): String? = local.takeIf { it.isNotEmpty() }?.let { ops ->
        ui("stat.local", ops.joinToString(", ") { fmt(it.stat, it.value, it.op, index?.stats?.isPercent(it.stat) == true) })
    }

    fun sourceTitle(source: StatSource?): String = if (source == null) {
        ui("stat.kind.OTHER")
    } else {
        when (source.kind) {
            SourceKind.CLASS -> ui("stat.src.class", classTitle(source.ref))
            SourceKind.NODE -> nodeTitle(source.ref)
            SourceKind.ITEM -> hero?.item(source.ref)?.let { item -> game.view(item)?.let { view -> "${view.title} · ${slotTitle(item.slot ?: view.slot)}" } } ?: ui("stat.kind.ITEM")
            SourceKind.PET -> hero?.pets?.pet(source.ref)?.let { locOr("pet.${it.species}", it.species) } ?: ui("stat.kind.PET")
            SourceKind.POWER, SourceKind.MAP, SourceKind.ATLAS -> statTitle(source.ref)
        }
    }

    /** A power's shift is named by the worn items that carry it, the power's own name when none is found. */
    private fun shiftTitle(source: StatSource, holders: (String) -> List<StatSource>): String {
        val prefix = when (source.kind) {
            SourceKind.MAP -> ui("stat.src.map")
            SourceKind.ATLAS -> ui("stat.src.atlas")
            else -> null
        }
        if (source.kind != SourceKind.POWER) return listOfNotNull(prefix, statTitle(source.ref)).joinToString(": ")
        val items = holders(source.ref).filter { it.kind == SourceKind.ITEM }.map(::sourceTitle)
        return if (items.isEmpty()) statTitle(source.ref) else "${items.joinToString(", ")} — ${statTitle(source.ref)}"
    }

    private fun summary(stat: String, shares: List<Share>, percent: Boolean): String = shares.groupBy { if (percent && it.op == Op.INCREASED) Op.ADD else it.op }
        .toSortedMap().map { (op, group) ->
            when (op) {
                Op.MORE -> fmt(stat, (group.fold(1.0) { acc, it -> acc * (1 + it.value / 100) } - 1) * 100, op, percent)
                Op.SET -> fmt(stat, group.last().value, op, percent)
                else -> fmt(stat, group.sumOf { it.value }, op, percent)
            }
        }.joinToString(" · ")

    /**
     * What each kind adds to the total, for the bar: a flat part through the increases and the «more»s,
     * an increase on the flat sum, the «more»s' gain split by their logarithms, a shift as it is.
     */
    private fun weights(b: StatBreakdown): Map<ShareKind, Double> {
        if (b.set != null) return emptyMap()
        val increase = if (b.percent) 1.0 else 1 + b.increasedSum / 100
        val more = b.moreFactor
        val gain = b.flatSum * increase * (more - 1)
        val logMore = ln(more)
        val weights = LinkedHashMap<ShareKind, Double>()
        fun add(kind: ShareKind, value: Double) {
            if (value > 0) weights.merge(kind, value, Double::plus)
        }
        add(ShareKind.BASE, b.base * increase * more)
        b.shares.forEach { share ->
            val kind = kindOf(share)
            when {
                share.op == Op.ADD || b.percent && share.op == Op.INCREASED -> add(kind, share.value * increase * more)
                share.op == Op.INCREASED -> add(kind, b.flatSum * share.value / 100 * more)
                share.op == Op.MORE && logMore != 0.0 -> add(kind, gain * ln(1 + share.value / 100) / logMore)
                else -> Unit
            }
        }
        b.shifts.forEach { add(ShareKind.AFTER, it.delta) }
        return weights
    }

    /** «135 × 1.30 × 1.10 = 193 − 13 = 180»: the flat sum, the multipliers that are there, the set value, the shifts. */
    private fun formula(b: StatBreakdown): String {
        val stat = b.stat
        val parts = mutableListOf(fmt(stat, b.flatSum, Op.ADD, b.percent, sign = false))
        if (!b.percent && b.increasedSum != 0.0) parts += "× ${factor(1 + b.increasedSum / 100)}"
        if (b.moreFactor != 1.0) parts += "× ${factor(b.moreFactor)}"
        var text = parts.joinToString(" ")
        b.set?.let { text += " → ${fmt(stat, it.value, Op.SET, b.percent)}" }
        val formed = fmt(stat, b.formed, Op.ADD, b.percent, sign = false)
        if (parts.size > 1 && b.set == null) text += " = $formed"
        val shifted = b.shifts.sumOf { it.delta }
        if (abs(shifted) >= 0.05) text += " ${signed(stat, shifted, b.percent).replaceFirst("+", "+ ").replaceFirst("−", "− ")} = ${fmt(stat, b.total, Op.ADD, b.percent, sign = false)}"
        return text
    }

    private fun factor(value: Double): String = String.format(java.util.Locale.ROOT, "%.2f", value)

    private fun signed(stat: String, value: Double, percent: Boolean): String = fmt(stat, value, Op.ADD, percent)

    /** A figure with its operation: «+20», «+10%», «×1.10», «= 1»; a percent stat carries its sign on the flat too. */
    fun fmt(stat: String, value: Double, op: Op, percent: Boolean, sign: Boolean = true): String {
        val unit = if (percent || statPercent(stat, index)) "%" else ""
        val size = if (op == Op.INCREASED) shareNumber(stat, abs(value)) else statNumber(stat, abs(value))
        val mark = if (!sign && value >= 0) {
            ""
        } else if (value < 0) {
            "−"
        } else {
            "+"
        }
        return when (op) {
            Op.ADD -> "$mark$size$unit"
            Op.INCREASED -> if (percent) "$mark$size$unit" else "$mark$size%"
            Op.MORE -> "×${factor(1 + value / 100)}"
            Op.SET -> "= ${statNumber(stat, value)}$unit"
        }
    }
}
