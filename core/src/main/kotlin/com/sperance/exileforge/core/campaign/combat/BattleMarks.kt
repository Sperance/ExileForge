package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.KitSkill
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.SkillMark

// ==================== Метки с подрывом (0.14.0, SKILL-02) ====================

/**
 * Метка героя на враге: умение [code] копит [share] (доля, не проценты) урона ударов героя по цели в [pool] до [until], затем
 * взрывается по цели и ещё [neighbours] врагам рядом уроном вида [element].
 */
internal class BattleMark(val code: String, val share: Double, val until: Double, val neighbours: Int, val element: DamageType) {
    var pool = 0.0
}

/**
 * Применение умения с меткой [mark] уровня [level] на доле силы [power] (эхо): метка ложится на основную цель; прежняя метка на ней
 * сперва взрывается. Доля урона растёт на эффект меток листа (`STOCK_MARK_EFFECT`).
 */
internal fun Battle.mark(kitSkill: KitSkill, mark: SkillMark, level: Int, power: Double) {
    val target = target() ?: return
    val code = kitSkill.skill.code
    marks[target.index]?.let { detonate(target, it) }
    if (!target.alive || outcome != null) return
    val share = mark.share.at(level) / 100 * (1 + heroFighter.body[CoreStat.MARK_EFFECT.code] / 100).coerceAtLeast(0.0) * power
    marks[target.index] = BattleMark(code, share, time + mark.duration, mark.neighbours, DamageType.element(mark.element) ?: DamageType.CHAOS)
    lay(target, TimedEffect(EffectKind.MARK, code, emptyList(), time + mark.duration, mark.duration, counter = share * 100))
    record(Side.HERO, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, target.index, code)
}

/** Урон [dealt], что удар героя [me] нанёс цели [target]: её метка копит свою долю. */
internal fun Battle.markDealt(me: Fighter, target: Fighter, dealt: Double) {
    if (marks.isEmpty() || me !== heroFighter || dealt <= 0) return
    marks[target.index]?.let { it.pool += dealt * it.share }
}

/** Метки, чей срок вышел, взрываются. */
internal fun Battle.marksTick() {
    if (marks.isEmpty()) return
    marks.entries.filter { it.value.until <= time }.forEach { (index, mark) -> detonate(foeFighters[index], mark) }
}

/**
 * Подрыв метки [mark] на [target]: накопленное - по цели (если жива) и ещё стольким врагам в досягаемости, сколько у метки соседей,
 * мимо защит (копился уже прошедший урон); метка снимается, журнал пишет подрыв. Павший от подрыва - убийство героя.
 */
internal fun Battle.detonate(target: Fighter, mark: BattleMark) {
    marks.remove(target.index)
    if (target.effects.removeAll { it.kind == EffectKind.MARK && it.source == mark.code } && target.alive) remake(target)
    if (mark.pool <= 0 || outcome != null || !heroFighter.alive) return
    val struck = listOfNotNull(target.takeIf { it.alive }) + foeFighters.filter { it.alive && it !== target && reachable(it.index) }.take(mark.neighbours)
    note(heroFighter, NoteKind.MARK_BURST, mark.code, mark.pool)
    struck.filter { !it.invulnerable }.forEach { foe ->
        val alive = foe.alive
        wound(foe, mark.pool, chaos = mark.element == DamageType.CHAOS)
        record(Side.HERO, Action.SKILL, HitKind.HIT, mark.pool, mark.element, 0.0, false, emptyList(), null, foe.index, mark.code)
        if (alive && !foe.alive) fell(foe, spell = true)
    }
}
