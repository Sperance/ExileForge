package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.i18n.locFirst
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MonsterSkill
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.SkillAilment
import com.sperance.exileforge.rules.content.SkillBarrier
import com.sperance.exileforge.rules.content.SkillBuff
import com.sperance.exileforge.rules.content.SkillCast
import com.sperance.exileforge.rules.content.SkillCharges
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillHeal
import com.sperance.exileforge.rules.content.SkillHit
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SkillStat
import com.sperance.exileforge.rules.content.SkillTrigger
import com.sperance.exileforge.rules.sheet.CastTime
import com.sperance.exileforge.rules.sheet.Recovery
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.rules.text.ModifierText
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A skill in the player's words. The server names a skill and nothing more; what it
 * does is read off its numbers at a level, line by line, so a book's next level shows what it will change.
 */
object SkillText {

    /**
     * A skill's name; a unique item's power is named by its stat.
     *
     * Удар фазы босса - источник `<шаблон>#<шаг>`, назван фазой; свойство монстра - источник помехи и проклятия на герое, названо
     * свойством.
     */
    fun title(code: String): String = if ('#' in code) {
        phaseTitle(code.substringBefore('#'))
    } else {
        locFirst("skill.$code.name", LocaleKey.traitName(code), "enum.EnumStatStock.$code")
    }

    /**
     * What [skill] does at [level]: its blow, its poison, its buff or curse, its healing, its lines or its answer. Пороги низкого
     * здоровья строк - из правил боя контента [index], не из словаря; числа строк характеристик - тоже из него ([statLine]).
     *
     * Урон во времени называет своё состояние (яд или свой урон умения); руны - эхо и дары применения.
     */
    fun lines(skill: SkillDefinition, level: Int, index: ContentIndex): List<String> = buildList {
        if (skill.reserve > 0) add(ui("skill.line.reserve.${skill.reserves}", number(skill.reserve)))
        skill.hit?.let { addAll(hit(it, level, index)) }
        skill.dot?.let { dot ->
            add(ui("skill.line.dot", number(dot.min.at(level)), number(dot.max.at(level)), element(dot.element), fineNumber(dot.duration), ailment(dot.ailment)))
            if (dot.targets != 1) add(targets(dot.targets))
            addAll(ailments(dot.ailments, level))
        }
        skill.curse?.let { curse ->
            add(ui("skill.line.curse", fineNumber(curse.duration), if (curse.targets <= 0) ui("skill.line.all_foes") else ui("skill.line.foes", curse.targets)))
            addAll(stats(curse.stats, level, index))
        }
        skill.buff?.let { addAll(buff(it, level, index)) }
        skill.mark?.let { add(ui("skill.line.mark", fineNumber(it.duration), number(it.share.at(level)), it.neighbours, element(it.element))) }
        skill.heal?.let { addAll(heal(it, level)) }
        skill.shield?.let { add(ui("skill.line.shield", number(it.at(level)))) }
        skill.barrier?.let { add(barrier(it, level)) }
        if (skill.stats.isNotEmpty()) {
            if (skill.lowLife) add(ui("skill.line.low_life", number(index.campaign.combat.passiveLowLife * 100)))
            addAll(stats(skill.stats, level, index))
        }
        skill.trigger?.let { addAll(trigger(it, level, index)) }
        skill.charges?.let { addAll(charges(it, level)) }
        skill.echo?.let { add(ui("skill.line.echo", number(it.chance), number(it.power))) }
        skill.cast?.let { addAll(cast(it, level)) }
    }

    /** Дары применения (руны): барьер и заряды. */
    private fun cast(cast: SkillCast, level: Int): List<String> = buildList {
        cast.barrier?.let { add(ui("skill.line.cast_barrier", number(it.life.at(level)), fineNumber(it.duration))) }
        cast.charge?.takeIf { cast.charges > 0 }?.let { add(ui("skill.line.cast_charges", cast.charges, ui("fight.charge.${it.name}"))) }
    }

    /**
     * What a skill does with the hero's charges: gives some of a kind, or spends all for a heavier blow.
     *
     * Предел зарядов траты (`charges.most`): рост урона идёт не больше чем за столько зарядов.
     */
    private fun charges(charges: SkillCharges, level: Int): List<String> = buildList {
        val kind = ui("fight.charge.${charges.kind.name}")
        charges.gain?.let { add(ui("skill.line.charge_gain", it.at(level).roundToInt(), kind)) }
        if (charges.consume) add(ui("skill.line.charge_consume", kind, number(charges.perCharge?.at(level) ?: 0.0)))
        if (charges.consume && charges.most > 0) add(ui("skill.line.charge_most", charges.most))
    }

    /**
     * Темп активного умения у героя [hero]: цена маны умения уровня [level] ([SkillDefinition.manaAt], у всех видов - по уровню умения) со снижением цены листа, время применения ([CastTime]) и перезарядка
     * на его листе в темпе боя правил (`combat.tempo`), регенерация маны; у пассивного - null. Им гримуар и лист боя печатают «применение · перезарядка» и «цена в секунду
     * / ваша регенерация».
     */
    fun pace(skill: SkillDefinition, level: Int, hero: Combatant): SkillPace? {
        if (skill.kind != SkillKind.ACTIVE) return null
        val mana = skill.manaAt(level) * hero.skillCost
        val tempo = hero.rules.tempo
        return SkillPace(mana, CastTime.of(skill, hero, hero.rules) / tempo, skill.cooldown / hero.recovery / tempo, Recovery.manaRegen(hero, hero.rules.mana))
    }

    /** Строка цены маны в секунду цикла против регенерации героя. */
    fun manaRate(pace: SkillPace): String = ui("skill.line.mana_rate", Rate.perSecond(pace.manaRate), Rate.perSecond(pace.regen))

    /** What a monster's skill does, for the scouting panel and the log: one sentence; числа строк характеристик - из контента [index]. */
    fun monster(skill: MonsterSkill, index: ContentIndex): List<String> = buildList {
        skill.hit?.let { addAll(hit(it, 1, index)) }
        skill.buff?.let { addAll(buff(it, 1, index)) }
        skill.curse?.let { curse ->
            add(ui("skill.line.curse", fineNumber(curse.duration), ui("skill.line.foes", 1)))
            addAll(stats(curse.stats, 1, index))
        }
        skill.heal?.let { addAll(heal(it, 1)) }
        if (skill.manaBurn > 0) add(ui("skill.line.mana_burn", number(skill.manaBurn)))
    }

    private fun hit(hit: SkillHit, level: Int, index: ContentIndex): List<String> = buildList {
        hit.weapon?.let { add(ui("skill.line.weapon", number(it.at(level)))) }
        hit.spell?.let { add(ui("skill.line.spell", number(it.min.at(level)), number(it.max.at(level)), element(it.element))) }
        if (hit.targets != 1) add(targets(hit.targets))
        if (hit.hits > 1) add(ui("skill.line.hits", hit.hits))
        if (hit.chain > 0) add(ui("skill.line.chain", hit.chain, number(hit.falloff)))
        hit.convert?.let { convert ->
            add(
                if (hit.element == "RANDOM") {
                    ui("skill.line.convert_random", number(convert.at(level)))
                } else {
                    ui("skill.line.convert", number(convert.at(level)), element(hit.element.orEmpty()))
                },
            )
        }
        hit.finisher?.let { add(ui("skill.line.finisher", number(it.at(level)))) }
        hit.stun?.let { add(ui("skill.line.stun", number(it.at(level)))) }
        addAll(ailments(hit.ailments, level))
        addAll(stats(hit.stats, level, index))
    }

    private fun buff(buff: SkillBuff, level: Int, index: ContentIndex): List<String> = buildList {
        add(ui("skill.line.buff", fineNumber(buff.duration)))
        addAll(stats(buff.stats, level, index))
        buff.counter?.let { add(ui("skill.line.counter", number(it.at(level)))) }
        if (buff.nextCrit) add(ui("skill.line.next_crit"))
    }

    private fun heal(heal: SkillHeal, level: Int): List<String> = buildList {
        heal.life?.let { add(ui("skill.line.heal_life", number(it.at(level)))) }
        heal.mana?.let { add(ui("skill.line.heal_mana", number(it.at(level)))) }
        if (heal.cleanse) add(ui("skill.line.cleanse"))
    }

    private fun barrier(barrier: SkillBarrier, level: Int) = ui("skill.line.barrier", number(barrier.life.at(level)), fineNumber(barrier.duration))

    /**
     * Срабатывание: условие и шанс одной фразой, и первое следствие в той же строке - «С шансом 20%, убив врага: +1 заряд
     * всем флягам»; прочие следствия и перезарядка - строками ниже. Строка без следствия не висит на двоеточии.
     *
     * Событие низкого здоровья называет свой порог правил `combat.lowLife` контента [index]; прочим числа не нужны.
     */
    private fun trigger(trigger: SkillTrigger, level: Int, index: ContentIndex): List<String> {
        val effects = buildList {
            trigger.heal?.let { addAll(heal(it, level)) }
            trigger.shield?.let { add(ui("skill.line.shield", number(it.at(level)))) }
            trigger.barrier?.let { add(barrier(it, level)) }
            trigger.buff?.let { addAll(buff(it, level, index)) }
            trigger.hit?.let { addAll(hit(it, level, index)) }
            if (trigger.flaskCharges > 0) add(ui("skill.line.flask_charges", trigger.flaskCharges))
            trigger.ailment?.let { add(ui(if (trigger.twice) "skill.line.inflict_twice" else "skill.line.inflict", ailment(it))) }
            if (trigger.refund) add(ui("skill.line.refund"))
        }
        val event = ui("skill.on.${trigger.on.name}", number(index.campaign.combat.lowLife * 100))
        val head = trigger.chance?.let { ui("skill.line.chance_on", number(it.at(level)), event.replaceFirstChar { c -> c.lowercase() }) } ?: event
        return buildList {
            add(listOfNotNull(head, effects.firstOrNull()).joinToString(": "))
            addAll(effects.drop(1))
            if (trigger.cooldown > 0) add(ui("skill.line.trigger_cooldown", fineNumber(trigger.cooldown)))
        }
    }

    private fun targets(count: Int) = if (count <= 0) ui("skill.line.all_foes") else ui("skill.line.foes", count)

    private fun ailments(list: List<SkillAilment>, level: Int) = list.map { ui("skill.line.ailment", number(it.chance.at(level)), ailment(it.ailment)) }

    private fun stats(list: List<SkillStat>, level: Int, index: ContentIndex) = list.map { statLine(it.stat, it.op, it.value.at(level), index) }

    /** An element as the skill names it: `FIRE` … `CHAOS`, or `RANDOM` — any of the three elements. */
    fun element(name: String): String = ui("skill.element.$name")

    /** An ailment as a skill names it: `IGNITE` … `BLEED`, or `ELEMENT` — the one of the element that struck. */
    fun ailment(word: String): String = ui("skill.ailment.$word")

    /**
     * One stat line, a skill's or a flask's: the server's own sentence for the stat and operation — its
     * `.negative` one for a minus — or the stat's title and the number where the server has none.
     *
     * Шаблон «+{v}» с отрицательным числом печатается «−5»: знак ставит fillTemplate. Числа силы и правил шаблона
     * (`{p:…}`, `{r:…}`) подставляет контент [index] ([com.sperance.exileforge.rules.text.numbered]), как в строках
     * модификаторов; подстановка без своего числа (или без контента) игроку не печатается: строка выходит общей - «Имя: +N».
     */
    fun statLine(stat: String, operation: Op, value: Double, index: ContentIndex?): String {
        val key = "stat.template.$stat.${operation.name}"
        val negative = locOr("$key.negative", "").takeIf { value < 0 && it.isNotBlank() }
        val template = (negative ?: locOr(key, "")).takeIf(String::isNotBlank)
        val shown = if (negative != null) abs(value) else value
        val number = if (operation == Op.INCREASED) shareNumber(stat, shown) else statNumber(stat, shown)
        val numbered = template?.let { index.numberedText(it, stat) }
        return numbered?.let { fillTemplate(it.replace("{v}", "{0}"), listOf(number)) } ?: plainStatLine(stat, operation, value)
    }

    /**
     * Доля [value] прибавки к характеристике [stat] коротким числом со знаком и единицей ([statValue]) - так, как её читает фраза сервера:
     * шаблон с минусом у числа («Сопротивления героя стихиям: −{v}%») ставит минус у прибавки; без шаблона - знак самого числа.
     */
    fun statShare(stat: String, value: Double, index: ContentIndex?): String {
        val key = "stat.template.$stat.${Op.ADD.name}"
        val negative = locOr("$key.negative", "").takeIf { value < 0 && it.isNotBlank() }
        val shown = if (negative != null) abs(value) else value
        val flips = (negative ?: locOr(key, "")).let { it.contains(ModifierText.MINUS + "{v}") || it.contains("-{v}") }
        return signedNumber(if (flips) -shown else shown) { statValue(stat, it, index) }
    }

    /** Строка стата без шаблона сервера: имя характеристики и число по операции. */
    private fun plainStatLine(stat: String, operation: Op, value: Double): String {
        val size = statNumber(stat, abs(value))
        return when (operation) {
            Op.ADD -> ui("skill.stat_add", statTitle(stat), signedNumber(value) { statNumber(stat, it) })
            Op.INCREASED -> ui(if (value < 0) "skill.stat_reduced" else "skill.stat_increased", statTitle(stat), size)
            Op.MORE -> ui(if (value < 0) "skill.stat_less" else "skill.stat_more", statTitle(stat), size)
            Op.SET -> ui("skill.stat_set", statTitle(stat), size)
        }
    }

    /**
     * Строка, что фляга кладёт на героя на время глотка: [statLine] и хвост словаря сервера [ModifierText.DRAUGHT]
     * («во время действия») - тот же, что печатает строку фляги на карточке.
     */
    fun draughtLine(stat: String, operation: Op, value: Double, index: ContentIndex?): String = listOf(statLine(stat, operation, value, index), locOr(ModifierText.DRAUGHT, ""))
        .filter(String::isNotBlank).joinToString(" ")
}

/**
 * Темп активного умения у героя: [mana] - цена применения, [cast] - секунды применения, [cooldown] -
 * перезарядка после него, [regen] - регенерация маны героя в секунду.
 */
data class SkillPace(val mana: Double, val cast: Double, val cooldown: Double, val regen: Double) {
    /** Секунды цикла: применение и перезарядка. */
    val period: Double get() = cast + cooldown

    /** Цена маны в секунду цикла. */
    val manaRate: Double get() = if (period > 0) mana / period else 0.0
}
