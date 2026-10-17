package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.CombatEvent
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.HitKind
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.run.PackHit

/**
 * Разбор боя: что нанёс герой ([dealt]), что получил ([taken]) и что
 * сделали защиты ([guard]) - из следов ударов боя ([HitTrace], [TickTrace], [NoteTrace]); бой ничего не считает заново. Бой и весь
 * заход - одна мера: разбор захода - сумма разборов его боёв ([plus]).
 */
data class FightStats(
    val seconds: Double = 0.0,
    val dealt: Dealt = Dealt(),
    val taken: Taken = Taken(),
    val guard: Guard = Guard(),
) {
    /**
     * Урон героя по врагам: по типам и умениям, удары, криты, промахи, заблокированные, сильнейший удар и что срезали защиты врагов.
     * Яд - своей графой [poison], не хаосом; [exposed] - что прибавило Разложение врагов (часть урона, не сверх него).
     */
    data class Dealt(
        val byType: Map<DamageType, Double> = emptyMap(),
        val bySkill: Map<String, Double> = emptyMap(),
        val hits: Int = 0,
        val crits: Int = 0,
        val missed: Int = 0,
        val blocked: Int = 0,
        val strongest: Double = 0.0,
        /** Сколько урона героя по типам срезали броня ([armour]) и сопротивления врагов. */
        val armour: Double = 0.0,
        val resisted: Map<DamageType, Double> = emptyMap(),
        val poison: Double = 0.0,
        val exposed: Double = 0.0,
    ) {
        val total: Double get() = byType.values.sum() + poison

        operator fun plus(other: Dealt) = Dealt(
            byType.merged(other.byType),
            bySkill.merged(other.bySkill),
            hits + other.hits,
            crits + other.crits,
            missed + other.missed,
            blocked + other.blocked,
            maxOf(strongest, other.strongest),
            armour + other.armour,
            resisted.merged(other.resisted),
            poison + other.poison,
            exposed + other.exposed,
        )
    }

    /**
     * Урон врагов по герою: по типам, удары и сильнейший; куда лёг урон ударов - барьер, щит, мана, отложенный, здоровье - и урон
     * недугов отдельно (у тика нет разбора по запасам). Яд - своей графой [poison], [exposed] - что прибавило Разложение на герое.
     */
    data class Taken(
        val byType: Map<DamageType, Double> = emptyMap(),
        val hits: Int = 0,
        val strongest: Double = 0.0,
        val barrier: Double = 0.0,
        val shield: Double = 0.0,
        val mana: Double = 0.0,
        val delayed: Double = 0.0,
        val life: Double = 0.0,
        val ailments: Double = 0.0,
        val poison: Double = 0.0,
        val exposed: Double = 0.0,
    ) {
        val total: Double get() = byType.values.sum() + poison

        operator fun plus(other: Taken) = Taken(
            byType.merged(other.byType),
            hits + other.hits,
            maxOf(strongest, other.strongest),
            barrier + other.barrier,
            shield + other.shield,
            mana + other.mana,
            delayed + other.delayed,
            life + other.life,
            ailments + other.ailments,
            poison + other.poison,
            exposed + other.exposed,
        )
    }

    /**
     * Защита героя: сколько урона врагов срезали броня ([armour]) и сопротивления по типам; уклонённые и заблокированные удары и
     * оценка их урона ([HitTrace.avoided]); что вернулось - фляги, вампиризм и прибавки при ударе, регенерация.
     */
    data class Guard(
        val armour: Double = 0.0,
        val resisted: Map<DamageType, Double> = emptyMap(),
        val evaded: Int = 0,
        val evadedDamage: Double = 0.0,
        val blocked: Int = 0,
        val blockedDamage: Double = 0.0,
        val flasks: Double = 0.0,
        val leech: Double = 0.0,
        val regen: Double = 0.0,
    ) {
        operator fun plus(other: Guard) = Guard(
            armour + other.armour,
            resisted.merged(other.resisted),
            evaded + other.evaded,
            evadedDamage + other.evadedDamage,
            blocked + other.blocked,
            blockedDamage + other.blockedDamage,
            flasks + other.flasks,
            leech + other.leech,
            regen + other.regen,
        )
    }

    /** Урон героя в секунду боя. */
    val dps: Double get() = if (seconds > 0) dealt.total / seconds else 0.0

    /** Получено в секунду боя. */
    val takenPerSecond: Double get() = if (seconds > 0) taken.total / seconds else 0.0

    operator fun plus(other: FightStats) = FightStats(seconds + other.seconds, dealt + other.dealt, taken + other.taken, guard + other.guard)

    companion object {
        val NONE = FightStats()

        /** Разбор боя по его стаям [pack] длиной [duration] секунд. */
        fun of(pack: List<PackHit>, duration: Double): FightStats {
            var stats = FightStats(seconds = duration)
            pack.forEach { hit -> hit.events.forEach { stats = stats.with(it) } }
            return stats
        }
    }

    /** Разбор с ещё одним событием [event]: удар, тик, уклонение и блок по своей стороне, заметки восстановления героя. */
    private fun with(event: CombatEvent): FightStats {
        val trace = event.trace
        if (trace is NoteTrace) return noted(trace)
        if (event.onSelf) return this
        return when (event.actor) {
            Side.HERO -> copy(dealt = dealt.withBlow(event, trace as? HitTrace), guard = guard.copy(leech = guard.leech + ((trace as? HitTrace)?.landing?.let { it.leech + it.onHit } ?: 0.0)))
            Side.MONSTER -> struck(event, trace as? HitTrace)
        }
    }

    /** Заметки боя героя: глоток фляги, что кончился, и секунда регенерации ([NoteKind.LIFE_FLOW]). */
    private fun noted(note: NoteTrace): FightStats {
        if (note.actor.side != Side.HERO) return this
        return when (note.kind) {
            NoteKind.RECOVER_FLASK -> copy(guard = guard.copy(flasks = guard.flasks + note.value))
            NoteKind.LIFE_FLOW -> copy(guard = guard.copy(regen = guard.regen + (note.flow?.regen ?: 0.0)))
            else -> this
        }
    }

    /** Удар врага по герою [event]: уклонённый и заблокированный - в защиту с оценкой, прошедший - по типам, запасам и срезам защит. */
    private fun struck(event: CombatEvent, trace: HitTrace?): FightStats = when (event.kind) {
        HitKind.EVADED -> copy(guard = guard.copy(evaded = guard.evaded + 1, evadedDamage = guard.evadedDamage + (trace?.avoided ?: 0.0)))

        HitKind.BLOCKED -> copy(guard = guard.copy(blocked = guard.blocked + 1, blockedDamage = guard.blockedDamage + (trace?.avoided ?: 0.0)))

        else -> if (event.damage <= 0) {
            this
        } else {
            val landing = trace?.landing
            val tick = event.action == Action.TICK
            val poisoned = poisonTick(event)
            copy(
                taken = taken.copy(
                    byType = if (poisoned) taken.byType else taken.byType.merged(RunStats.split(event)),
                    poison = taken.poison + if (poisoned) event.damage else 0.0,
                    exposed = taken.exposed + exposedBy(event),
                    hits = taken.hits + if (tick) 0 else 1,
                    strongest = if (tick) taken.strongest else maxOf(taken.strongest, event.damage),
                    barrier = taken.barrier + (landing?.barrier ?: 0.0),
                    shield = taken.shield + (landing?.shield ?: 0.0),
                    mana = taken.mana + (landing?.mana ?: 0.0),
                    delayed = taken.delayed + (landing?.delayed ?: 0.0),
                    life = taken.life + (landing?.life ?: 0.0),
                    ailments = taken.ailments + if (tick) event.damage else 0.0,
                ),
                guard = guard.copy(armour = guard.armour + trace.armourCut(), resisted = guard.resisted.merged(trace.resistCut())),
            )
        }
    }
}

/** Удар героя [event] в разбор нанесённого: исход, урон по типам и умению, что срезали защиты врага. */
private fun FightStats.Dealt.withBlow(event: CombatEvent, trace: HitTrace?): FightStats.Dealt = when (event.kind) {
    HitKind.EVADED -> copy(missed = missed + 1)

    HitKind.BLOCKED -> copy(blocked = blocked + 1)

    else -> if (event.damage <= 0) {
        this
    } else {
        val blow = event.action != Action.TICK && event.action != Action.REFLECT
        val poisoned = poisonTick(event)
        copy(
            byType = if (poisoned) byType else byType.merged(RunStats.split(event)),
            poison = poison + if (poisoned) event.damage else 0.0,
            exposed = exposed + exposedBy(event),
            bySkill = bySkill.merged(mapOf(RunStats.skillKey(event) to event.damage)),
            hits = hits + if (blow) 1 else 0,
            crits = crits + if (event.kind == HitKind.CRIT) 1 else 0,
            strongest = if (blow) maxOf(strongest, event.damage) else strongest,
            armour = armour + trace.armourCut(),
            resisted = resisted.merged(trace.resistCut()),
        )
    }
}

/** Тик яда: в разборе - своя графа «Яд», а не хаос. */
private fun poisonTick(event: CombatEvent): Boolean = event.action == Action.TICK && event.ailment == Ailment.POISONED

/** Что прибавило событию [event] Разложение цели: из следа удара или тика. */
private fun exposedBy(event: CombatEvent): Double = when (val trace = event.trace) {
    is HitTrace -> trace.exposed
    is TickTrace -> trace.exposed
    else -> 0.0
}

/** Сколько срезала броня цели удара: доля брони от урона типа до защит. */
private fun HitTrace?.armourCut(): Double = this?.types?.sumOf { it.raw * it.armour } ?: 0.0

/** Сколько срезали сопротивления цели по типам: доля сопротивления от урона, прошедшего броню. */
private fun HitTrace?.resistCut(): Map<DamageType, Double> = this?.types?.filter { it.resist > 0 }?.associate { it.type to it.raw * (1 - it.armour) * it.resist }.orEmpty()

private fun <K> Map<K, Double>.merged(other: Map<K, Double>): Map<K, Double> = if (other.isEmpty()) this else (keys + other.keys).associateWith { (this[it] ?: 0.0) + (other[it] ?: 0.0) }
