package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.ChargeView
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.rules.content.BuffKind
import kotlin.math.ceil
import kotlin.math.roundToInt

// ==================== What the screen reads (2.78.0) ====================

/** The hero's charges as their counters draw them (3.33.0). */
fun Battle.chargeViews(): List<ChargeView> = heroCharges.views(time, heroFighter.body.stats)

/** What the hero walks out with: life, mana, the flasks' charges and the seconds each draught still runs. */
fun Battle.pools(): HeroPools = HeroPools(
    heroFighter.life,
    heroFighter.mana,
    charges.toList(),
    kit.flasks.indices.map { i -> draughtOf(i)?.let { (it.until - time).coerceAtLeast(0.0) } ?: 0.0 },
    kit.flasks.indices.map { i -> recoveries.firstOrNull { it.slot == i && it.until > time }?.let { DraughtRate(it.life, it.mana) } ?: DraughtRate() },
)

/** The active slots as their buttons draw them; null where a slot is empty. */
fun Battle.skillViews(): List<SkillView?> = kit.actives.mapIndexed { slot, kitSkill ->
    kitSkill?.let {
        val body = heroFighter.body
        val level = it.level(body)
        val cost = cost(it, level)
        val cooldown = it.skill.cooldown / body.recovery(it.skill.spell)
        val left = ((heroFighter.readyAt[slotKey(slot)] ?: 0.0) - time).coerceAtLeast(0.0)
        SkillView(
            slot, it.skill.code, it.skill.icon, level, cost.roundToInt(), if (cooldown > 0) (1 - left / cooldown).toFloat().coerceIn(0f, 1f) else 1f,
            skillsFree() || heroFighter.mana + fateSpare() + 1e-9 >= cost, it.condition, left, skillLock(slot),
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

/** What lies on [fighter], for its tiles. */
fun Battle.effects(fighter: Fighter): List<EffectView> = fighter.effects.filter { it.kind != EffectKind.FLASK && !riftSourced(it.source) }.map {
    EffectView(
        it.source,
        it.kind,
        ((it.until - time) / it.duration).toFloat().coerceIn(0f, 1f),
        (it.until - time).coerceAtLeast(0.0),
        icons[it.source].orEmpty(),
        BuffKind.of(it.source),
    )
}
