package com.sperance.exileforge.core.sheet

import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.PassiveSource
import com.sperance.exileforge.core.campaign.ReadingPart
import com.sperance.exileforge.core.character.StatPolarity
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.factorChange
import com.sperance.exileforge.core.display.flagMark
import com.sperance.exileforge.core.display.guildLineTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.multiplierText
import com.sperance.exileforge.core.display.nodeTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.shareNumber
import com.sperance.exileforge.core.display.signedNumber
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.statFlag
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statPercent
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.fate.fateTitle
import com.sperance.exileforge.core.fate.reshapeText
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.StatSpread
import com.sperance.exileforge.rules.content.WorldKind
import com.sperance.exileforge.rules.content.WorldPart
import com.sperance.exileforge.rules.sheet.Grant
import com.sperance.exileforge.rules.sheet.ModifierMath
import com.sperance.exileforge.rules.sheet.Share
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.rules.sheet.SourceKind
import com.sperance.exileforge.rules.sheet.StatBreakdown
import com.sperance.exileforge.rules.sheet.StatOperation
import com.sperance.exileforge.rules.sheet.StatSource
import com.sperance.exileforge.rules.text.LocaleKey
import kotlin.math.abs
import kotlin.math.ln

/**
 * The cards of a figure's window, in the order they are read; the colour is the UI's, keyed by the kind. [SKILL] - пассивные
 * умения в бою (входят в итог), [FATE] - строки Предначертания, [RESHAPE] - его преобразования листа, [FIGHT] - что бой читает вместе
 * с цифрой, [LOW_LIFE] - бонусы низкого здоровья (в итог не входят), [APART] - силы добычи против отдельных врагов и шансы
 * уникалок, карт и книг (в итог не входят).
 */
enum class ShareKind { BASE, ATTRIBUTE, CLASS, NODE, ITEM, LAW, SKILL, FATE, OTHER, RESHAPE, AFTER, FIGHT, LOW_LIFE, APART }

/**
 * One source on a card: what it is, what it gives, a grey line under it, and the stat a tap goes to. [better] - к лучшему
 * ли то, что строка даёт, по полярности характеристики ([StatPolarity]), а не по знаку: null - без оценки и без цвета.
 */
data class ShareRow(val title: String, val value: String, val note: String? = null, val link: String? = null, val better: Boolean? = null)

/** A card of one kind of source: its rows and what they give together, one figure per operation; [title] - своё имя карточки (дар). */
data class ShareCard(val kind: ShareKind, val rows: List<ShareRow>, val summary: String, val title: String? = null) {
    /** Итог карточки к лучшему или к худшему: оценка её строк, если они согласны; разные или без оценки - null. */
    val better: Boolean? get() = rows.mapNotNull { it.better }.distinct().singleOrNull()
}

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
 * a node, a worn item by name and slot — a conversion under the stat it takes from.
 */
class StatExplainer(private val game: GameUi) {
    private val hero = game.hero
    private val index = game.index
    private val passives = hero?.combat?.passives.orEmpty().associateBy { it.skill }

    /**
     * [lowLife] - бонусы низкого здоровья на цифре с тем, что прибавят ([CombatSheet.lowLife]): в итог не входят.
     *
     * Сдвиги поверх свода: «больше» умений - к их карточке, шаги дара - «Перестройка», чтение боя - своя карточка; множитель - сдвигом
     * доли, карточка - их произведением. Бонус низкого здоровья - строкой с тем, что прибавит, в итог не входит. Силы добычи - строкой под
     * силой.
     */
    fun explain(
        breakdown: StatBreakdown,
        grants: List<Grant>,
        holders: (String) -> List<StatSource>,
        lowLife: List<Pair<PassiveSource, Double>> = emptyList(),
        apart: List<Pair<WorldPart, WorldKind>> = emptyList(),
    ): StatExplanation {
        val stat = breakdown.stat
        val cards = mutableListOf<ShareCard>()
        if (breakdown.base != 0.0) {
            cards += ShareCard(
                ShareKind.BASE,
                baseRows(breakdown),
                fmt(stat, breakdown.base, Op.ADD, breakdown.percent, sign = false),
            )
        }
        breakdown.shares.groupBy(::kindOf).toSortedMap().forEach { (kind, shares) ->
            cards += ShareCard(kind, merged(shares).map { row(stat, it, breakdown.percent) }, summary(stat, shares, breakdown.percent), cardTitle(kind, shares.first().source))
        }
        val factors = shiftFactors(breakdown)
        breakdown.shifts.groupBy(::shiftKind).toSortedMap().forEach { (kind, shifts) ->
            val rows = shifts.map { shift -> shiftRow(stat, shift, holders, breakdown.percent).let { row -> factors[shift]?.let { row.copy(value = factorChange(it)) } ?: row } }
            val sum = if (factors.isEmpty()) signed(stat, shifts.sumOf { it.delta }, breakdown.percent) else factorChange(shifts.fold(1.0) { acc, it -> acc * (factors[it] ?: 1.0) })
            val at = cards.indexOfFirst { it.kind == kind }
            if (at >= 0) cards[at] = cards[at].let { it.copy(rows = it.rows + rows, summary = "${it.summary} · $sum") } else cards += ShareCard(kind, rows, sum, cardTitle(kind, shifts.first().source))
        }
        if (lowLife.isNotEmpty()) {
            cards += ShareCard(
                ShareKind.LOW_LIFE,
                lowLife.map { (passive, gain) -> ShareRow(skillTitle(passive.skill), passive.lines.filter { stat in StatSpread.targets(it.stat, it.op) }.joinToString(", ") { line -> fmt(stat, line.value, line.op, breakdown.percent) }, lowLifeNote(stat, passive, gain, breakdown), better = judge(stat, gain)) },
                "",
            )
        }
        if (apart.isNotEmpty()) {
            cards += ShareCard(ShareKind.APART, apart.map { (part, kind) -> ShareRow(statTitle(part.stat), signedNumber(part.value) + "%", apartNote(part, kind), better = judge(part.stat, part.value)) }, "")
        }
        return StatExplanation(
            stat,
            fmt(stat, breakdown.total, Op.ADD, breakdown.percent, sign = false),
            cards,
            weights(breakdown),
            formula(breakdown),
            grants.map { ShareRow(statTitle(it.stat), fmt(it.stat, it.value, it.op, index?.stats?.isPercent(it.stat) == true), link = it.stat, better = judge(it.stat, it.value, it.op)) },
        )
    }

    private fun kindOf(share: Share): ShareKind = when {
        share.perStat != null -> ShareKind.ATTRIBUTE

        else -> when (share.source?.kind) {
            SourceKind.CLASS -> ShareKind.CLASS
            SourceKind.NODE -> ShareKind.NODE
            SourceKind.ITEM -> ShareKind.ITEM
            SourceKind.LAW -> ShareKind.LAW
            SourceKind.SKILL -> ShareKind.SKILL
            SourceKind.FATE -> ShareKind.FATE
            else -> ShareKind.OTHER
        }
    }

    /** Карточка сдвига: умение, шаг дара, чтение боя; сила уникалки, карта и Атлас - «После расчёта». */
    private fun shiftKind(shift: Shift): ShareKind = when (shift.source.kind) {
        SourceKind.SKILL -> ShareKind.SKILL
        SourceKind.FATE -> ShareKind.FATE
        SourceKind.RESHAPE -> ShareKind.RESHAPE
        SourceKind.FIGHT -> ShareKind.FIGHT
        else -> ShareKind.AFTER
    }

    /** Карточка дара называется им: «Предначертание: Живая крепость»; прочие - своим видом. */
    private fun cardTitle(kind: ShareKind, source: StatSource?): String? = if (kind == ShareKind.FATE) sourceTitle(source) else null

    /** Строка сдвига: шаг дара - его правилом, чтение боя - характеристикой, откуда прибавка (касание открывает её). */
    private fun shiftRow(stat: String, shift: Shift, holders: (String) -> List<StatSource>, percent: Boolean): ShareRow {
        val value = signed(stat, shift.delta, percent)
        val better = judge(stat, shift.delta)
        return when (shift.source.kind) {
            SourceKind.FIGHT -> ReadingPart.of(shift.source.ref)?.let { part -> partRow(stat, part, value, better) }
                ?: ShareRow(statTitle(shift.source.ref), value, ui("stat.fight.note"), link = shift.source.ref, better = better)

            SourceKind.SKILL, SourceKind.RESHAPE -> ShareRow(sourceTitle(shift.source), value, skillNote(shift.source), better = better)

            else -> ShareRow(shiftTitle(shift.source, holders), value, shift.from?.let { from -> ui("stat.from", statTitle(from)) }, better = better)
        }
    }

    /**
     * Часть чтения боя: своё имя, у множителя - он сам под именем («×0.50»), касание - характеристика, что её задаёт.
     */
    private fun partRow(stat: String, part: ReadingPart, value: String, better: Boolean?): ShareRow {
        val times = factorOf(stat, part)
        return ShareRow(ui("stat.flow.${part.name}"), value, times?.let { "×${factor(it)} · " + ui("stat.fight.note") } ?: ui("stat.fight.note"), link = part.stat, better = better)
    }

    /** Множитель части [part] чтения боя цифры [stat] на листе героя; прибавка - null. */
    private fun factorOf(stat: String, part: ReadingPart): Double? = hero?.combat?.steps(stat)?.firstOrNull { it.part == part }?.factor

    /**
     * Что даёт строка разбора характеристике [stat] - к лучшему ли: одна точка оценки [StatPolarity]; замена
     * значения ([Op.SET]) - без оценки.
     */
    private fun judge(stat: String, value: Double, op: Op = Op.ADD): Boolean? = if (op == Op.SET) null else StatPolarity.better(index, stat, value)

    /** Пассивное умение с уровнем, на котором действует: «Решимость ур. 12». */
    private fun skillTitle(code: String): String = passives[code]?.let { ui("stat.src.skill", SkillText.title(code), it.level) } ?: SkillText.title(code)

    /** Резерв маны ауры под её строкой: «резерв 25% маны». */
    private fun skillNote(source: StatSource?): String? = source?.takeIf { it.kind == SourceKind.SKILL }?.let { passives[it.ref] }?.reserve?.takeIf { it > 0 }?.let { ui("stat.src.reserve", number(it)) }

    /**
     * Sources of one name and one operation as one row: ten tree nodes «Здоровье» or two rings of the same name
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
        val better = judge(stat, share.value, share.op)
        val per = share.perStat ?: return ShareRow(rowTitle(share.source), value, localNote(share.local) ?: skillNote(share.source), better = better)
        val rule = ui("stat.per", fmt(stat, share.each, share.op, percent), statNumber(per, share.per), statTitle(per))
        val by = share.source?.takeIf { it.kind != SourceKind.CLASS }?.let { "${sourceTitle(it)}: " }.orEmpty()
        return ShareRow("${statTitle(per)} ${statNumber(per, share.perValue)}", value, by + rule, link = per, better = better)
    }

    /** Строка в карточке дара - его именем: карточка уже называется «Предначертание: …». */
    private fun rowTitle(source: StatSource?): String = if (source?.kind == SourceKind.FATE) fateTitle(source.ref) else sourceTitle(source)

    /** What a low-life bonus adds in a fight; an increase or a «more» over no base says why it adds nothing. */
    private fun apartNote(part: WorldPart, kind: WorldKind): String = if (kind.stock == null) {
        ui("stat.apart.kind.${kind.name}")
    } else {
        ui("stat.apart.against", part.against.joinToString(", ") { ui("enum.monster_rarity.$it") })
    }

    private fun lowLifeNote(stat: String, passive: PassiveSource, gain: Double, b: StatBreakdown): String {
        val idle = abs(gain) < 0.05 && b.flatSum == 0.0 && passive.lines.any { it.op == Op.INCREASED || it.op == Op.MORE }
        return "${ui("stat.passive.low_life")} · ${ui(if (idle) "stat.passive.idle" else "stat.passive.gain", signed(stat, gain, b.percent))}"
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
            SourceKind.LAW -> loc(LocaleKey.lawName(source.ref))
            SourceKind.FATE -> ui("stat.src.fate", loc(LocaleKey.fateName(source.ref)))
            SourceKind.POWER, SourceKind.MAP, SourceKind.ATLAS, SourceKind.FIGHT -> statTitle(source.ref)
            SourceKind.SKILL -> skillTitle(source.ref)
            SourceKind.RESHAPE -> hero?.combat?.fate?.let { fate -> index?.let { reshapeText(it, fate, source.ref) } } ?: source.ref
            SourceKind.GUILD -> guildLineTitle(source.ref)
            SourceKind.POTION -> ui("stat.src.potion", itemTitle(source.ref))
        }
    }

    /** A power's shift is named by the worn items that carry it, the power's own name when none is found. */
    private fun shiftTitle(source: StatSource, holders: (String) -> List<StatSource>): String {
        val prefix = when (source.kind) {
            SourceKind.MAP -> ui("stat.src.map")
            SourceKind.ATLAS -> ui("stat.src.atlas")
            else -> null
        }
        if (source.kind == SourceKind.GUILD || source.kind == SourceKind.POTION) return sourceTitle(source)
        if (source.kind != SourceKind.POWER) return listOfNotNull(prefix, statTitle(source.ref)).joinToString(": ")
        val items = holders(source.ref).filter { it.kind == SourceKind.ITEM }.map(::sourceTitle)
        return if (items.isEmpty()) statTitle(source.ref) else "${items.joinToString(", ")} — ${statTitle(source.ref)}"
    }

    private fun summary(stat: String, shares: List<Share>, percent: Boolean): String = shares.groupBy { if (percent && it.op == Op.INCREASED) Op.ADD else it.op }
        .toSortedMap().map { (op, group) ->
            when (op) {
                Op.MORE -> fmt(stat, (group.fold(1.0) { acc, it -> acc * (1 + it.value / 100) } - 1) * 100, op, percent)
                Op.SET -> fmt(stat, ModifierMath.settled(group.map { it.value }) ?: 0.0, op, percent)
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
        b.shifts.forEach { add(shiftKind(it), it.delta) }
        return weights
    }

    /**
     * Строки базы [b]: класс героя на первом уровне и прирост за уровни после него - «Класс «Мародёр» +20», «Уровень 24 +23»
     * (`HeroClass.baseOn`). Без героя или если база не сходится с классом - одной строкой.
     */
    private fun baseRows(b: StatBreakdown): List<ShareRow> {
        val stat = b.stat
        val whole = listOf(ShareRow(hero?.let { ui("stat.src.base", classTitle(it.heroClass), it.level) } ?: ui("stat.kind.BASE"), fmt(stat, b.base, Op.ADD, b.percent, sign = false), better = judge(stat, b.base)))
        val owner = hero ?: return whole
        val heroClass = index?.heroClass(owner.heroClass) ?: return whole
        val start = heroClass.base[stat] ?: 0.0
        val grown = (heroClass.perLevel[stat] ?: 0.0) * (owner.level - 1).coerceAtLeast(0)
        if (grown == 0.0 || abs(start + grown - b.base) >= 0.05) return whole
        return listOfNotNull(
            ShareRow(ui("stat.src.class", classTitle(owner.heroClass)), fmt(stat, start, Op.ADD, b.percent), better = judge(stat, start)).takeIf { start != 0.0 },
            ShareRow(ui("stat.src.level", owner.level), fmt(stat, grown, Op.ADD, b.percent), better = judge(stat, grown)),
        )
    }

    /** Характеристика читается множителем (`StatDef.multiplier`). */
    private fun multiplier(stat: String): Boolean = index?.stats?.get(stat)?.multiplier == true

    /**
     * Множитель каждого сдвига множителя [b]: сдвиги ложатся по порядку, `(100 + после) / (100 + до)` - «Невесомость −40%»,
     * а не «−40%». Не множитель - пусто.
     */
    private fun shiftFactors(b: StatBreakdown): Map<Shift, Double> {
        if (!multiplier(b.stat)) return emptyMap()
        var at = b.formed
        return b.shifts.associateWith { shift -> ((100 + at + shift.delta) / (100 + at)).also { at += shift.delta } }
    }

    /**
     * «135 × 1.30 × 1.10 = 193 − 13 = 180»: the flat sum, the multipliers that are there, the set value, the shifts.
     *
     * Множитель: «100% · −40% = 60%»; поток восстановления - по шагам, как его читает бой: «26.4 + 9.0 = 35.4 × 0.50 = 17.7 − 2.0 = 15.7».
     */
    private fun formula(b: StatBreakdown): String {
        val stat = b.stat
        if (multiplier(stat)) return (listOf(multiplierText(b.formed)) + shiftFactors(b).values.map(::factorChange)).joinToString(" · ") + " = ${multiplierText(b.total)}"
        val parts = mutableListOf(fmt(stat, b.flatSum, Op.ADD, b.percent, sign = false))
        if (!b.percent && b.increasedSum != 0.0) parts += "× ${factor(1 + b.increasedSum / 100)}"
        if (b.moreFactor != 1.0) parts += "× ${factor(b.moreFactor)}"
        var text = parts.joinToString(" ")
        b.set?.let { text += " → ${fmt(stat, it.value, Op.SET, b.percent)}" }
        val formed = fmt(stat, b.formed, Op.ADD, b.percent, sign = false)
        if (parts.size > 1 && b.set == null) text += " = $formed"
        if (b.shifts.any { it.source.kind == SourceKind.FIGHT && ReadingPart.of(it.source.ref) != null }) return text + steps(b)
        val shifted = b.shifts.sumOf { it.delta }
        if (abs(shifted) >= 0.05) text += " ${signedStep(stat, shifted, b.percent)} = ${fmt(stat, b.total, Op.ADD, b.percent, sign = false)}"
        return text
    }

    /** Сдвиги [b] по одному: множитель части чтения боя - «× f», прочее - прибавкой; после каждого - итог. */
    private fun steps(b: StatBreakdown): String = buildString {
        var at = b.formed
        b.shifts.filter { abs(it.delta) >= 0.005 }.forEach { shift ->
            val times = ReadingPart.of(shift.source.ref)?.takeIf { shift.source.kind == SourceKind.FIGHT }?.let { factorOf(b.stat, it) }
            append(if (times != null) " × ${factor(times)}" else " ${signedStep(b.stat, shift.delta, b.percent)}")
            at += shift.delta
            append(" = ${fmt(b.stat, at, Op.ADD, b.percent, sign = false)}")
        }
    }

    /** Сдвиг со знаком отдельным словом формулы: «+ 9.0», «− 2.0». */
    private fun signedStep(stat: String, value: Double, percent: Boolean): String = signed(stat, value, percent).replaceFirst("+", "+ ").replaceFirst("−", "− ")

    private fun factor(value: Double): String = String.format(java.util.Locale.ROOT, "%.2f", value)

    private fun signed(stat: String, value: Double, percent: Boolean): String = fmt(stat, value, Op.ADD, percent)

    /**
     * A figure with its operation: «+20», «+10%», «×1.10», «= 1»; a percent stat carries its sign on the flat too.
     *
     * Итог множителя - доля «60%», доли свода - прибавки процентов; флаг (`StatDef.unit`) - отметка без числа.
     */
    fun fmt(stat: String, value: Double, op: Op, percent: Boolean, sign: Boolean = true): String {
        if (!sign && op == Op.ADD && multiplier(stat)) return multiplierText(value)
        if (op != Op.MORE && statFlag(stat, index)) return flagMark(value)
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
