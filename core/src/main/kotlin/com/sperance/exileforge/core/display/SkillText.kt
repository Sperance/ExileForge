package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.modifier.ModifierOperation
import com.sperance.exileforge.core.model.skills.MonsterSkill
import com.sperance.exileforge.core.model.skills.Scale
import com.sperance.exileforge.core.model.skills.SkillAilment
import com.sperance.exileforge.core.model.skills.SkillBarrier
import com.sperance.exileforge.core.model.skills.SkillBuff
import com.sperance.exileforge.core.model.skills.SkillDefinition
import com.sperance.exileforge.core.model.skills.SkillHeal
import com.sperance.exileforge.core.model.skills.SkillHit
import com.sperance.exileforge.core.model.skills.SkillStat
import com.sperance.exileforge.core.model.skills.SkillTrigger
import kotlin.math.abs

/**
 * A skill in the player's words (2.78.0, server 0.69.0). The server names a skill and nothing more; what it
 * does is read off its numbers at a level, line by line, so a book's next level shows what it will change.
 */
object SkillText {

    /** A skill's name; a unique item's power (2.79.0) is named by its stat. */
    fun title(code: String): String = locOr("skill.$code.name", locOr("enum.EnumStatStock.$code", displayName(code)))

    /** What [skill] does at [level]: its blow, its poison, its buff or curse, its healing, its lines or its answer. */
    fun lines(skill: SkillDefinition, level: Int): List<String> = buildList {
        if (skill.reserve > 0) add(ui("skill.line.reserve", number(skill.reserve)))
        skill.hit?.let { addAll(hit(it, level)) }
        skill.dot?.let { dot ->
            add(ui("skill.line.dot", number(dot.min.at(level)), number(dot.max.at(level)), element(dot.element), fineNumber(dot.duration)))
            if (dot.targets != 1) add(targets(dot.targets))
        }
        skill.curse?.let { curse ->
            add(ui("skill.line.curse", fineNumber(curse.duration), if (curse.targets <= 0) ui("skill.line.all_foes") else ui("skill.line.foes", curse.targets)))
            addAll(stats(curse.stats, level))
        }
        skill.buff?.let { addAll(buff(it, level)) }
        skill.heal?.let { addAll(heal(it, level)) }
        skill.shield?.let { add(ui("skill.line.shield", number(it.at(level)))) }
        skill.barrier?.let { add(barrier(it, level)) }
        if (skill.stats.isNotEmpty()) {
            if (skill.lowLife) add(ui("skill.line.low_life"))
            addAll(stats(skill.stats, level))
        }
        skill.trigger?.let { addAll(trigger(it, level)) }
    }

    /** What a monster's skill does, for the scouting panel and the log: one sentence. */
    fun monster(skill: MonsterSkill): List<String> = buildList {
        skill.hit?.let { addAll(hit(it, 1)) }
        skill.buff?.let { addAll(buff(it, 1)) }
        skill.curse?.let { curse -> add(ui("skill.line.curse", fineNumber(curse.duration), ui("skill.line.foes", 1))); addAll(stats(curse.stats, 1)) }
        skill.heal?.let { addAll(heal(it, 1)) }
        if (skill.manaBurn > 0) add(ui("skill.line.mana_burn", number(skill.manaBurn)))
    }

    private fun hit(hit: SkillHit, level: Int): List<String> = buildList {
        hit.weapon?.let { add(ui("skill.line.weapon", number(it.at(level)))) }
        hit.spell?.let { add(ui("skill.line.spell", number(it.min.at(level)), number(it.max.at(level)), element(it.element))) }
        if (hit.targets != 1) add(targets(hit.targets))
        if (hit.hits > 1) add(ui("skill.line.hits", hit.hits))
        hit.convert?.let { convert ->
            add(if (hit.element == "RANDOM") ui("skill.line.convert_random", number(convert.at(level)))
                else ui("skill.line.convert", number(convert.at(level)), element(hit.element.orEmpty())))
        }
        hit.finisher?.let { add(ui("skill.line.finisher", number(it.at(level)))) }
        hit.stun?.let { add(ui("skill.line.stun", number(it.at(level)))) }
        addAll(ailments(hit.ailments, level))
        addAll(stats(hit.stats, level))
    }

    private fun buff(buff: SkillBuff, level: Int): List<String> = buildList {
        add(ui("skill.line.buff", fineNumber(buff.duration)))
        addAll(stats(buff.stats, level))
        buff.counter?.let { add(ui("skill.line.counter", number(it.at(level)))) }
        if (buff.nextCrit) add(ui("skill.line.next_crit"))
    }

    private fun heal(heal: SkillHeal, level: Int): List<String> = buildList {
        heal.life?.let { add(ui("skill.line.heal_life", number(it.at(level)))) }
        heal.mana?.let { add(ui("skill.line.heal_mana", number(it.at(level)))) }
        if (heal.cleanse) add(ui("skill.line.cleanse"))
    }

    private fun barrier(barrier: SkillBarrier, level: Int) = ui("skill.line.barrier", number(barrier.life.at(level)), fineNumber(barrier.duration))

    private fun trigger(trigger: SkillTrigger, level: Int): List<String> = buildList {
        val chance = trigger.chance?.let { ui("skill.line.chance", number(it.at(level))) }.orEmpty()
        add(ui("skill.on.${trigger.on.name}") + chance + ":")
        trigger.heal?.let { addAll(heal(it, level)) }
        trigger.shield?.let { add(ui("skill.line.shield", number(it.at(level)))) }
        trigger.barrier?.let { add(barrier(it, level)) }
        trigger.buff?.let { addAll(buff(it, level)) }
        trigger.hit?.let { addAll(hit(it, level)) }
        if (trigger.flaskCharges > 0) add(ui("skill.line.flask_charges", trigger.flaskCharges))
        trigger.ailment?.let { add(ui(if (trigger.twice) "skill.line.inflict_twice" else "skill.line.inflict", ailment(it))) }
        if (trigger.refund) add(ui("skill.line.refund"))
        if (trigger.cooldown > 0) add(ui("skill.line.trigger_cooldown", fineNumber(trigger.cooldown)))
    }

    private fun targets(count: Int) = if (count <= 0) ui("skill.line.all_foes") else ui("skill.line.foes", count)

    private fun ailments(list: List<SkillAilment>, level: Int) = list.map { ui("skill.line.ailment", number(it.chance.at(level)), ailment(it.ailment)) }

    private fun stats(list: List<SkillStat>, level: Int) = list.map { statLine(it.stat, it.operation, it.value.at(level)) }

    /** An element as the skill names it: `FIRE` … `CHAOS`, or `RANDOM` — any of the three elements. */
    fun element(name: String): String = ui("skill.element.$name")

    /** An ailment as a skill names it: `IGNITE` … `BLEED`, or `ELEMENT` — the one of the element that struck. */
    fun ailment(word: String): String = ui("skill.ailment.$word")

    /**
     * One stat line, a skill's or a flask's: the server's own sentence for the stat and operation — its
     * `.negative` one for a minus — or the stat's title and the number where the server has none.
     */
    fun statLine(stat: String, operation: ModifierOperation, value: Double): String {
        val key = "stat.template.$stat.${operation.name}"
        val negative = locOr("$key.negative", "").takeIf { value < 0 && it.isNotBlank() }
        val template = negative ?: locOr(key, "")
        if (template.isNotBlank()) return template.replace("{v}", statNumber(stat, if (negative != null) abs(value) else value))
        val size = statNumber(stat, abs(value))
        return when (operation) {
            ModifierOperation.ADD -> ui("skill.stat_add", statTitle(stat), (if (value < 0) "−" else "+") + size)
            ModifierOperation.INCREASED -> ui(if (value < 0) "skill.stat_reduced" else "skill.stat_increased", statTitle(stat), size)
            ModifierOperation.MORE -> ui(if (value < 0) "skill.stat_less" else "skill.stat_more", statTitle(stat), size)
            ModifierOperation.SET -> ui("skill.stat_set", statTitle(stat), size)
        }
    }

    /** A skill's price in mana at [level], as the book prints it. */
    fun cost(mana: Scale?, level: Int): String? = mana?.let { number(it.at(level)) }
}
