package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.nodeTitle
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
enum class ShareKind { BASE, ATTRIBUTE, CLASS, NODE, ITEM, PET, OTHER, AFTER }

/** One source on a card: what it is, what it gives, a grey line under it, and the stat a tap goes to. */
data class ShareRow(val title: String, val value: String, val note: String? = null, val link: String? = null)

/** A card of one kind of source: its rows and what they give together, one figure per operation. */
data class ShareCard(val kind: ShareKind, val rows: List<ShareRow>, val summary: String)

/**
 * A figure's window as it is read: the cards, each kind's share of the total for the bar (positive
 * only), the formula in one line, and what the figure gives other stats right now.
 */
data class StatExplanation(
    val stat: String, val total: String, val cards: List<ShareCard>, val weights: Map<ShareKind, Double>,
    val formula: String, val grants: List<ShareRow>,
)

/**
 * Takes a [StatBreakdown] apart into what the window prints: sources named from the hero — a class,
 * a node, a worn item by name and slot, a pet — a conversion under the stat it takes from.
 */
class StatExplainer(private val s: ForgeState) {
    private val hero = s.hero
    private val index = s.index

    fun explain(breakdown: StatBreakdown, grants: List<Grant>, holders: (String) -> List<StatSource>): StatExplanation {
        val stat = breakdown.stat
        val cards = mutableListOf<ShareCard>()
        if (breakdown.base != 0.0) cards += ShareCard(ShareKind.BASE,
            listOf(ShareRow(hero?.let { ui("stat.src.base", classTitle(it.heroClass), it.level) } ?: ui("stat.kind.BASE"), fmt(stat, breakdown.base, Op.ADD, breakdown.percent, sign = false))),
            fmt(stat, breakdown.base, Op.ADD, breakdown.percent, sign = false))
        breakdown.shares.groupBy(::kindOf).toSortedMap().forEach { (kind, shares) ->
            cards += ShareCard(kind, shares.map { row(stat, it, breakdown.percent) }, summary(stat, shares, breakdown.percent))
        }
        if (breakdown.shifts.isNotEmpty()) cards += ShareCard(ShareKind.AFTER,
            breakdown.shifts.map { ShareRow(shiftTitle(it.source, holders), signed(stat, it.delta, breakdown.percent), it.from?.let { from -> ui("stat.from", statTitle(from)) }) },
            signed(stat, breakdown.shifts.sumOf { it.delta }, breakdown.percent))
        return StatExplanation(stat, fmt(stat, breakdown.total, Op.ADD, breakdown.percent, sign = false), cards, weights(breakdown),
            formula(breakdown), grants.map { ShareRow(statTitle(it.stat), fmt(it.stat, it.value, it.op, index?.stats?.isPercent(it.stat) == true), link = it.stat) })
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

    private fun row(stat: String, share: Share, percent: Boolean): ShareRow {
        val value = fmt(stat, share.value, share.op, percent)
        val per = share.perStat ?: return ShareRow(sourceTitle(share.source), value, localNote(share.local))
        val rule = ui("stat.per", fmt(stat, share.each, share.op, percent), statNumber(per, share.per), statTitle(per))
        val by = share.source?.takeIf { it.kind != SourceKind.CLASS }?.let { "${sourceTitle(it)}: " }.orEmpty()
        return ShareRow("${statTitle(per)} ${statNumber(per, share.perValue)}", value, by + rule, link = per)
    }

    /** The local lines an item folded into this figure: «локально: +200, +40%». */
    private fun localNote(local: List<StatOperation>): String? = local.takeIf { it.isNotEmpty() }?.let { ops ->
        ui("stat.local", ops.joinToString(", ") { fmt(it.stat, it.value, it.op, index?.stats?.isPercent(it.stat) == true) })
    }

    fun sourceTitle(source: StatSource?): String = if (source == null) ui("stat.kind.OTHER") else when (source.kind) {
        SourceKind.CLASS -> ui("stat.src.class", classTitle(source.ref))
        SourceKind.NODE -> nodeTitle(source.ref)
        SourceKind.ITEM -> hero?.item(source.ref)?.let { item -> s.view(item)?.let { view -> "${view.title} · ${slotTitle(item.slot ?: view.slot)}" } } ?: ui("stat.kind.ITEM")
        SourceKind.PET -> hero?.pets?.pet(source.ref)?.let { locOr("pet.${it.species}", it.species) } ?: ui("stat.kind.PET")
        SourceKind.POWER, SourceKind.MAP, SourceKind.ATLAS -> statTitle(source.ref)
    }

    /** A power's shift is named by the worn items that carry it, the power's own name when none is found. */
    private fun shiftTitle(source: StatSource, holders: (String) -> List<StatSource>): String {
        val prefix = when (source.kind) { SourceKind.MAP -> ui("stat.src.map"); SourceKind.ATLAS -> ui("stat.src.atlas"); else -> null }
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
        fun add(kind: ShareKind, value: Double) { if (value > 0) weights.merge(kind, value, Double::plus) }
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
    private fun fmt(stat: String, value: Double, op: Op, percent: Boolean, sign: Boolean = true): String {
        val unit = if (percent || statPercent(stat, index)) "%" else ""
        val size = statNumber(stat, abs(value))
        val mark = if (!sign && value >= 0) "" else if (value < 0) "−" else "+"
        return when (op) {
            Op.ADD -> "$mark$size$unit"
            Op.INCREASED -> if (percent) "$mark$size$unit" else "$mark$size%"
            Op.MORE -> "×${factor(1 + value / 100)}"
            Op.SET -> "= ${statNumber(stat, value)}$unit"
        }
    }
}
