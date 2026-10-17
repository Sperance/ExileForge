package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.ChargeView
import com.sperance.exileforge.core.campaign.HitTrace
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.rules.content.BuffKind
import kotlin.math.ceil
import kotlin.math.roundToInt

/** The hero's charges as their counters draw them. */
fun Battle.chargeViews(): List<ChargeView> = heroCharges.views(time, heroFighter.body.stats)

/**
 * What the hero walks out with: life, mana, the flasks' charges and the seconds each draught still runs. Следующему бою забега
 * испытания и Разлома - и силы: счёт «каждое N-е» и их баффы остатком времени; прочего несомого они не несут.
 */
fun Battle.pools(): HeroPools = HeroPools(
    heroFighter.life,
    heroFighter.mana,
    charges.toList(),
    kit.flasks.indices.map { i -> draughtOf(i)?.let { (it.until - time).coerceAtLeast(0.0) } ?: 0.0 },
    kit.flasks.indices.map { i -> recoveries.firstOrNull { it.slot == i && it.until > time }?.let { DraughtRate(it.life, it.mana, it.toMana, it.toShield) } ?: DraughtRate() },
    HeroCarry(effects = carriedEffects().filter { it.source in kit.powers.byStat }, counts = powers.tally),
)

/**
 * Что герой уносит из боя на карту ([HeroCarry]): недуги, временные баффы и проклятия (не глотки и не носимое весь бой), щит и
 * заряды - остатком своего времени.
 */
fun Battle.carry(): HeroCarry {
    val held = heroFighter.ailments.filter { it.until > time }
    val brought = held.filter { it.foe < 0 }.maxOfOrNull { CarriedAilment.origin(it.foe, 0) } ?: 0
    return HeroCarry(
        held.map { CarriedAilment(it.ailment, it.until - time, it.magnitude, it.duration, it.chaos, it.limit, CarriedAilment.origin(it.foe, brought)) },
        carriedEffects(),
        heroFighter.shield,
        heroCharges.carried(time),
        powers.tally,
    )
}

/** Временные баффы и проклятия героя остатком времени: не глотки, не носимое весь бой и не Разлома. */
private fun Battle.carriedEffects(): List<CarriedEffect> = heroFighter.effects.filter { it.kind != EffectKind.FLASK && it.until > time && it.until - time < Battle.FOREVER / 2 && !riftSourced(it.source) }.map {
    CarriedEffect(it.kind, it.source, it.lines, it.until - time, it.duration, icons[it.source].orEmpty(), it.counter, it.conditional, it.stacks)
}

/** The active slots as their buttons draw them; null where a slot is empty. */
fun Battle.skillViews(): List<SkillView?> = kit.actives.mapIndexed { slot, kitSkill ->
    kitSkill?.let {
        val body = heroFighter.body
        val level = it.level(body)
        val cost = cost(it, level)
        val cooldown = it.skill.cooldown / body.recovery
        val left = ((heroFighter.readyAt[slotKey(slot)] ?: 0.0) - time).coerceAtLeast(0.0)
        val cast = heroFighter.casting?.takeIf { c -> c.key == slotKey(slot) }
        SkillView(
            slot, it.skill.code, it.skill.icon, level, cost.roundToInt(), if (cooldown > 0) (1 - left / cooldown).toFloat().coerceIn(0f, 1f) else 1f,
            skillsFree() || affords(cost), it.condition, left, skillLock(slot), casting = cast?.progress(time)?.toFloat() ?: 0f,
        )
    }
}

/** The belt as its buttons draw it; null where a place is empty. */
fun Battle.flaskViews(): List<FlaskView?> = kit.flasks.mapIndexed { i, flask ->
    flask?.let {
        val running = draughtOf(i)
        FlaskView(
            i,
            it.sheet.code,
            it.sheet.kind,
            charges[i].toInt(),
            it.sheet.maxCharges.toInt(),
            ceil(it.sheet.perUse(heroFighter.body::get) - 1e-9).toInt(),
            running?.let { d -> ((d.until - time) / d.duration).toFloat().coerceIn(0f, 1f) } ?: 0f,
            it.condition,
            flaskLock(i),
        )
    }
}

/**
 * What lies on [fighter], for its tiles; и глотки флаконов - плиткой бутыли на полке с таймером. Стопки Крови вожака
 * ([FATE_STACKS]) - значок ✦ дара ([fateGlows]), не плитка эффекта.
 */
fun Battle.effects(fighter: Fighter): List<EffectView> = fighter.effects.filter { (it.kind != EffectKind.FLASK || it.until > time) && !riftSourced(it.source) && it.source != FATE_STACKS }.map {
    EffectView(
        it.source,
        it.kind,
        ((it.until - time) / it.duration).toFloat().coerceIn(0f, 1f),
        (it.until - time).coerceAtLeast(0.0),
        icons[it.source].orEmpty(),
        BuffKind.of(it.source),
        it.lines,
        flask = if (it.kind == EffectKind.FLASK) kit.flasks.getOrNull(it.slot)?.sheet?.kind else null,
    )
}

/** Пассивные умения героя на весь бой: одни и те же плитки, пока идёт бой. */
fun Battle.passiveViews(): List<EffectView> = passiveViews

/**
 * Удары на экране: каждая строка удара журнала моложе [window] секунд боя - все разом, а не последняя ([Battle.lunge]);
 * [StrikeView.progress] - доля пройденной сцены. С конца журнала, он идёт по времени: обход - только строки окна.
 *
 * Умение без удара (проклятие, помеха) - не выпад: удар умения несёт разбор удара.
 */
fun Battle.strikes(window: Double): List<StrikeView> {
    var from = log.size
    while (from > 0 && time - log[from - 1].time < window) from--
    if (from == log.size) return emptyList()
    val strikes = ArrayList<StrikeView>(log.size - from)
    for (i in from until log.size) {
        val event = log[i]
        if (event.time > time || (event.action != Action.ATTACK && event.action != Action.SKILL) || event.onSelf) continue
        if (event.action == Action.SKILL && event.trace !is HitTrace) continue
        strikes += StrikeView(
            i,
            event.actor,
            event.foe,
            event.kind,
            event.spell,
            event.type,
            event.damage.roundToInt(),
            ((time - event.time) / window).toFloat().coerceIn(0f, 1f),
        )
    }
    return strikes
}

/**
 * Удар на экране: строка журнала [serial], кто бил ([actor]; у врага - [foe]), чем кончилось ([kind]), чары ли ([spell]),
 * ведущая стихия [type] и урон [damage]; [progress] - доля сцены удара.
 */
data class StrikeView(
    val serial: Int,
    val actor: Side,
    val foe: Int,
    val kind: HitKind,
    val spell: Boolean,
    val type: DamageType?,
    val damage: Int,
    val progress: Float,
) {
    val landed: Boolean get() = kind == HitKind.HIT || kind == HitKind.CRIT

    /** Кого бьёт удар. */
    val target: Side get() = actor.other
}
