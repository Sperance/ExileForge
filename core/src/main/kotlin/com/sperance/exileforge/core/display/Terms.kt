package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Line

/**
 * A word of the game's own that a line uses without saying what it is (3.81.0): a buff, a kind of charges, an ailment, a stun
 * or a condition. Each is told by its name and a short rule; a line names one when a stat of its effects carries one of the
 * term's [marks].
 */
enum class Term(vararg val marks: String) {
    ARCANE_SURGE("ARCANE_SURGE"),
    ONSLAUGHT("ONSLAUGHT"),
    UNHOLY_MIGHT("UNHOLY_MIGHT"),
    FORTIFY("FORTIFY"),
    FRENZY_CHARGE("FRENZY"),
    POWER_CHARGE("POWER_CHARGE"),
    ENDURANCE_CHARGE("ENDURANCE"),
    IGNITE("IGNITE", "BURN"),
    CHILL("CHILL"),
    FREEZE("FREEZE", "FROZEN"),
    SHOCK("SHOCK"),
    POISON("POISON"),
    BLEED("BLEED"),
    STUN("STUN"),
    LOW_LIFE("LOW_LIFE"),
    CURSE("CURSE"),

    // Защита и урон, что её обходит (3.95.2): щит принимает удары первым, хаос и яд идут мимо него
    ENERGY_SHIELD("ENERGY_SHIELD", "SHIELD_RECHARGE", "ENERGY_REGEN", "SHIELD_OF_LIFE"),
    CHAOS("CHAOS"),

    // Правило знамений-катализаторов (4.2.0) - одно на все: описание каждого называет лишь свой вид модификаторов
    CATALYST,
    ;

    val title: String get() = ui("term.$name")
    val text: String get() = ui("term.$name.text")

    companion object {
        /** The terms the stats name, in the order of this list. */
        fun ofStats(stats: Collection<String>): List<Term> = entries.filter { term -> stats.any { stat -> term.marks.any { it in stat } } }

        /** Термины копии (4.2.0): что называют её строки, и катализатор, если её качество - катализатора. */
        fun ofItem(item: ItemView): List<Term> = ofStats(item.lines.flatMap { line -> line.definition?.effects.orEmpty().map { it.stat } }) +
            listOfNotNull(CATALYST.takeIf { item.item.catalyst != null })

        /** The terms the lines' effects name. */
        fun ofLines(index: ContentIndex, lines: Collection<Line>): List<Term> = ofStats(lines.flatMap { line -> index.modifier(line.code)?.effects.orEmpty().map { it.stat } })
    }
}
