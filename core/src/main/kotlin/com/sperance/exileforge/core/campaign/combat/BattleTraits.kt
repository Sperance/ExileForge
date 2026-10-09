package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.FOREVER
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.MonsterTrait
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.TraitAct
import com.sperance.exileforge.rules.content.TraitLine
import com.sperance.exileforge.rules.content.TraitTrigger
import kotlin.math.min

// ==================== Отклики свойств монстров (3.73.0; стратегии - 4.4.1) ====================

/** Строки свойства на силе [power]: всё, кроме [Op.SET], растёт с ней. */
internal fun List<TraitLine>.lines(power: Double): List<StatLine> = map { StatLine(it.stat, it.op, if (it.op == Op.SET) it.value else it.value * power) }

/**
 * Свойство [trait] монстра с откликом [trigger] на силе редкости [power] (4.4.1): число отклика [value] и строки [lines] уже на
 * этой силе, [reaction] - что отклик делает в бою.
 */
internal class TraitAt(val trait: MonsterTrait, val trigger: TraitTrigger, val power: Double) {
    val code: String get() = trait.code
    val value: Double get() = trigger.value * power
    val lines: List<StatLine> by lazy { trigger.lines.lines(power) }
    val reaction: TraitReaction = TraitReaction.of(trigger.act)
}

/** Что замах монстра несёт сверх своего урона (4.4.1): [more] процентов урона и строки [lines] только на этот удар. */
internal data class SwingBoost(val more: Double = 0.0, val lines: List<StatLine> = emptyList()) {
    operator fun plus(other: SwingBoost) = SwingBoost(more + other.more, lines + other.lines)

    companion object {
        val NONE = SwingBoost()
    }
}

/**
 * Отклик свойства монстра (4.4.1): что вид [TraitAct] делает в каждый момент боя - замах, попадание, уклонение, шаг боя,
 * гибель. Момент, в который отклику нечего делать, - ничего. Новый вид - новая реализация и ветка [of], бой не проверяет виды.
 */
internal sealed interface TraitReaction {
    /** Замах [me] оружием: прибавка этого удара; [first] - первый замах монстра в бою. */
    fun swing(battle: Battle, me: Fighter, at: TraitAt, first: Boolean): SwingBoost = SwingBoost.NONE

    /** Удар [me] - замах или умение - попал в [target]. */
    fun landed(battle: Battle, me: Fighter, target: Fighter, at: TraitAt) = Unit

    /** [me] уклонился от удара. */
    fun evaded(battle: Battle, me: Fighter, at: TraitAt) = Unit

    /** Шаг боя, пока [me] стоит. */
    fun stands(battle: Battle, me: Fighter, at: TraitAt) = Unit

    /** [fallen] пал; [pack] - союзники, что ещё стоят. */
    fun fell(battle: Battle, fallen: Fighter, pack: List<Fighter>, at: TraitAt) = Unit

    companion object {
        fun of(act: TraitAct): TraitReaction = when (act) {
            TraitAct.BURST -> Burst
            TraitAct.ENRAGE -> Enrage
            TraitAct.FIRST_STRIKE -> FirstStrike
            TraitAct.RALLY -> Rally
            TraitAct.MEND -> Mend
            TraitAct.HINDER -> Hinder
            TraitAct.HEX -> Hex
            TraitAct.RIPOSTE -> Riposte
            TraitAct.STACK -> Stack
        }
    }
}

/** Первый удар сильнее на число отклика и несёт его строки (4.4.1) - только он. */
private data object FirstStrike : TraitReaction {
    override fun swing(battle: Battle, me: Fighter, at: TraitAt, first: Boolean): SwingBoost {
        if (!first) return SwingBoost.NONE
        battle.note(me, NoteKind.TRAIT, at.code, at.value)
        return SwingBoost(at.value, at.lines)
    }
}

/** Ниже порога здоровья - строки отклика до конца боя, один раз. */
private data object Enrage : TraitReaction {
    override fun stands(battle: Battle, me: Fighter, at: TraitAt) {
        if (me.life >= me.body.maxLife * at.trigger.threshold / 100 || !battle.enraged.add(me.index to at.code)) return
        battle.buff(me, at.code, at.lines, FOREVER)
        battle.note(me, NoteKind.TRAIT, at.code)
    }
}

/** Павший воодушевляет стоящих союзников строками отклика на его срок. */
private data object Rally : TraitReaction {
    override fun fell(battle: Battle, fallen: Fighter, pack: List<Fighter>, at: TraitAt) {
        pack.forEach { battle.buff(it, at.code, at.lines, at.trigger.duration) }
        if (pack.isNotEmpty()) battle.note(fallen, NoteKind.TRAIT, at.code)
    }
}

/** Павший лечит стоящих союзников на долю их здоровья. */
private data object Mend : TraitReaction {
    override fun fell(battle: Battle, fallen: Fighter, pack: List<Fighter>, at: TraitAt) {
        val share = at.value / 100
        pack.forEach { it.life = min(it.body.maxLife, it.life + it.body.maxLife * share) }
        if (pack.isNotEmpty()) battle.note(fallen, NoteKind.TRAIT, at.code, share * 100)
    }
}

/**
 * Павший взрывается долей своего здоровья: ударом стихии отклика или (4.4.1) недугом отклика на герое - тот же вес ложится
 * недугом, а не мгновенным ударом. Взрыв никого не лечит: что бы ни вернул вампиризм, павший остаётся лежать.
 */
private data object Burst : TraitReaction {
    override fun fell(battle: Battle, fallen: Fighter, pack: List<Fighter>, at: TraitAt) {
        val hero = battle.heroFighter
        val amount = fallen.body.maxLife * at.value / 100
        val ailment = at.trigger.ailment?.let { Ailment.of(it) }
        if (hero.alive) {
            if (ailment != null) {
                if (battle.burden(fallen, hero, ailment, amount)) battle.note(fallen, NoteKind.TRAIT, at.code, amount)
            } else {
                val type = DamageType.element(at.trigger.element) ?: DamageType.PHYSICAL
                battle.strike(fallen, battle.foeTarget(), Blow(mapOf(type to amount), Action.SKILL, spell = true, skill = at.code, spread = false, primary = false))
            }
        }
        fallen.life = 0.0
        fallen.shield = 0.0
    }
}

/** Попавший по герою удар вешает строки отклика на его срок (4.4.1) - не проклятие; повтор обновляет срок, не складывается. */
private data object Hinder : TraitReaction {
    override fun landed(battle: Battle, me: Fighter, target: Fighter, at: TraitAt) {
        if (target !== battle.heroFighter) return
        val fresh = target.effects.none { it.kind == EffectKind.HINDER && it.source == at.code }
        val duration = at.trigger.duration
        battle.lay(target, TimedEffect(EffectKind.HINDER, at.code, at.lines, battle.time + duration, duration))
        if (fresh) battle.note(me, NoteKind.TRAIT, at.code)
    }
}

/** Попавший по герою удар с шансом отклика проклинает его строками отклика на срок (4.4.1); иммунитет к проклятиям снимает. */
private data object Hex : TraitReaction {
    override fun landed(battle: Battle, me: Fighter, target: Fighter, at: TraitAt) {
        if (target !== battle.heroFighter || target.body.immuneCurse) return
        if (battle.random.nextDouble() * 100 >= at.value.coerceAtMost(100.0)) return
        val duration = at.trigger.duration
        battle.lay(target, TimedEffect(EffectKind.CURSE, at.code, at.lines, battle.time + duration, duration))
        battle.note(me, NoteKind.TRAIT, at.code, at.value)
    }
}

/** Уклонившись, монстр бьёт следующим замахом сильнее на число отклика (4.4.1), затем снова как обычно. */
private data object Riposte : TraitReaction {
    override fun evaded(battle: Battle, me: Fighter, at: TraitAt) {
        battle.riposting += me.index to at.code
    }

    override fun swing(battle: Battle, me: Fighter, at: TraitAt, first: Boolean): SwingBoost {
        if (!battle.riposting.remove(me.index to at.code)) return SwingBoost.NONE
        battle.note(me, NoteKind.TRAIT, at.code, at.value)
        return SwingBoost(at.value)
    }
}

/** Каждый свой попавший удар даёт строки отклика ещё раз (4.4.1), до потолка складываний за бой. */
private data object Stack : TraitReaction {
    override fun landed(battle: Battle, me: Fighter, target: Fighter, at: TraitAt) {
        val key = me.index to at.code
        val count = (battle.stacked[key] ?: 0) + 1
        if (count > at.trigger.stacks) return
        battle.stacked[key] = count
        battle.buff(me, at.code, at.lines.map { if (it.op == Op.SET) it else it.copy(value = it.value * count) }, FOREVER)
        if (count == at.trigger.stacks) battle.note(me, NoteKind.TRAIT, at.code, count.toDouble())
    }
}

// ==================== Моменты боя ====================

/** Отклики свойств [fighter]; у героя и питомца - ни одного. */
private fun Battle.traitsOf(fighter: Fighter): List<TraitAt> = if (fighter.side == Side.MONSTER) traitsAt[fighter.index] else emptyList()

/**
 * Замах [me] оружием: у монстра - с прибавками его откликов ([TraitReaction.swing]); строки замаха ложатся на лист только
 * этого удара.
 */
internal fun Battle.swingOf(me: Fighter): Blow {
    if (me.side != Side.MONSTER) return Blow(me.body.damage)
    val first = swung.add(me.index)
    val boost = traitsOf(me).fold(SwingBoost.NONE) { sum, at -> sum + at.reaction.swing(this, me, at, first) }
    if (boost == SwingBoost.NONE) return Blow(me.body.damage)
    val body = if (boost.lines.isEmpty()) me.body else me.model.body(foeLines(me) + boost.lines)
    return Blow(body.damage.mapValues { it.value * (1 + boost.more / 100) }, body = body.takeIf { boost.lines.isNotEmpty() })
}

/** Удар [me] попал в [target]: отклики попадания, пока [me] стоит. */
internal fun Battle.traitsLanded(me: Fighter, target: Fighter) {
    if (me.alive) traitsOf(me).forEach { it.reaction.landed(this, me, target, it) }
}

/** [me] уклонился от удара. */
internal fun Battle.traitsEvaded(me: Fighter) = traitsOf(me).forEach { it.reaction.evaded(this, me, it) }

/** Шаг боя: отклики каждого стоящего врага. */
internal fun Battle.traitsStand() = foeFighters.forEach { foe -> if (foe.alive) traitsOf(foe).forEach { it.reaction.stands(this, foe, it) } }

/** Что свойства павшего делают, пока он падает: взрыв по герою, воодушевление и лечение стаи. */
internal fun Battle.lastWords(fallen: Fighter) {
    if (outcome != null) return
    val pack = foeFighters.filter { it.alive && it !== fallen }
    traitsOf(fallen).forEach { it.reaction.fell(this, fallen, pack, it) }
}
