package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.campaign.FighterShot
import com.sperance.exileforge.core.campaign.LineKind
import com.sperance.exileforge.core.campaign.LineSource
import com.sperance.exileforge.core.campaign.MonsterBreakdown
import com.sperance.exileforge.core.campaign.MonsterShareKind
import com.sperance.exileforge.core.campaign.TraceOrigin
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.modifierLine
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statValue
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.BuffKind
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.roll.MonsterRoller

/** One source of a stat on a log line's card (3.37.0): who gave it and how much, with a grey note under it. */
data class TraceRow(val title: String, val value: String, val note: String? = null)

/** A stat of one side at the moment of a line: its figure then and every source of it. */
data class StatTrace(val stat: String, val title: String, val total: String, val rows: List<TraceRow>)

/**
 * Lays a fighter's stat at the moment of a log line out by source (3.37.0): the hero's by the sheet's own breakdown, the
 * map's shifts and the passives; a monster's by the roller's fold — base, rarity, each modifier, the map; then for both
 * what the fight laid on at that moment. The figure itself is the fight's.
 */
class TraceExplainer(private val game: GameUi) {
    private val index = game.index
    private val sheet = StatExplainer(game)
    private val roller = index?.let(::MonsterRoller)

    fun stat(shot: FighterShot, stat: String, origin: TraceOrigin): StatTrace {
        val percent = index?.stats?.isPercent(stat) == true
        val rows = mutableListOf<TraceRow>()
        when {
            shot.side == Side.HERO && shot.index != Battle.ALLY -> hero(stat, origin, rows)
            shot.side == Side.MONSTER -> monster(shot.index, stat, origin, percent, rows)
        }
        shot.lines.filter { it.line.stat == stat }.forEach { rows += TraceRow(lineTitle(it), sheet.fmt(stat, it.line.value, it.line.op, percent), ui("trace.src.fight")) }
        return StatTrace(stat, statTitle(stat, game.lang), statValue(stat, shot.stats[stat] ?: 0.0, index), rows)
    }

    private fun hero(stat: String, origin: TraceOrigin, rows: MutableList<TraceRow>) {
        val explainer = origin.hero.explainer
        if (explainer != null) {
            sheet.explain(explainer.explain(stat).shifted(origin.hero.shifts()[stat].orEmpty()), emptyList(), explainer::holders).cards
                .forEach { card -> card.rows.forEach { rows += TraceRow(it.title, it.value, it.note) } }
        }
        val percent = index?.stats?.isPercent(stat) == true
        origin.hero.passiveLines.filter { it.stat == stat }.forEach { rows += TraceRow(ui("trace.src.passive"), sheet.fmt(stat, it.value, it.op, percent)) }
    }

    private fun monster(foe: Int, stat: String, origin: TraceOrigin, percent: Boolean, rows: MutableList<TraceRow>) {
        val index = index ?: return
        val roller = roller ?: return
        val entry = origin.foes.getOrNull(foe) ?: return
        val monster = entry.origin ?: return
        val rarity = { r: com.sperance.exileforge.rules.content.MonsterRarity -> roller.rarityEffects(index.campaign.rarity(r)) }
        MonsterBreakdown.explain(roller, rarity, index.monster(monster.code), monster, entry.level, stat).forEach { share ->
            val title = when (share.kind) {
                MonsterShareKind.BASE -> ui("trace.src.monster_base", monsterTitle(share.ref), entry.level)

                MonsterShareKind.RARITY -> ui("trace.src.rarity", ui("trace.rarity.${share.ref}"))

                MonsterShareKind.MODIFIER -> index.modifier(ModifierCode(share.ref))?.let { def ->
                    monster.modifiers.firstOrNull { it.code.value == share.ref }?.let { mod -> modifierLine(index, def, mod.effects.map { it.value }) }
                } ?: share.ref

                MonsterShareKind.MAP -> ui("stat.src.map")
            }
            rows += TraceRow(title, sheet.fmt(stat, share.value, share.op, percent))
        }
    }

    /** A line's figure with its operation, as the sheet's window prints it. */
    fun fmt(stat: String, value: Double, op: com.sperance.exileforge.rules.content.Op): String = sheet.fmt(stat, value, op, index?.stats?.isPercent(stat) == true)

    fun lineTitle(source: LineSource): String = when (source.kind) {
        LineKind.BUFF -> BuffKind.of(source.ref)?.let { ui("fight.buff.${it.name}") } ?: SkillText.title(source.ref)
        LineKind.CURSE, LineKind.SKILL -> SkillText.title(source.ref)
        LineKind.FLASK -> equipmentTitle(source.ref)
        LineKind.CHARGE -> ui("trace.src.charge")
        LineKind.CONDITION -> locOr("condition.${source.ref}", source.ref)
        LineKind.LOW_LIFE -> ui("trace.src.low_life")
        LineKind.POWER -> ui("trace.src.power")
        LineKind.AURA -> statTitle(source.ref, game.lang)
    }
}
