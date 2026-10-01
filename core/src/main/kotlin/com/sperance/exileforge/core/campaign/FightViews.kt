package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetRole
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.rules.roll.RolledMonster
import kotlin.math.roundToInt

/**
 * The combat pet as a fighter: its sheet at its level, what its role does; a new one each fight stands up whole. Since 3.33.0
 * (server 1.32.0) the hero's sheet reaches it — its levels, damage, life, speed, armour and resistances — made again only
 * when those change. Shared by the map's runs and the trials (3.49.0).
 */
internal class PetAllies(private val index: ContentIndex, private val pet: Pet?, private val rules: CombatRules) {
    private var made: Pair<Map<String, Double>, Ally?>? = null
    private val menagerie by lazy { Menagerie(index) }

    fun of(heroStats: Map<String, Double>): Ally? {
        val boons = PetBoons.of(heroStats)
        made?.takeIf { it.first == boons }?.let { return it.second }
        val ally = pet?.let { own ->
            val kind = menagerie.species(own.species) ?: return@let null
            val levels = (boons[PetBoons.LEVEL] ?: 0.0).toInt()
            val p = if (levels != 0) own.copy(level = (own.level + levels).coerceAtLeast(1)) else own
            Ally(p.species, Combatant(PetBoons.apply(menagerie.sheet(p), boons), p.level, rules), kind.role == PetRole.TANK,
                if (kind.role == PetRole.SUPPORT) menagerie.supportHeal(p) else 0.0, index.pets.drawFire)
        }
        made = boons to ally
        return ally
    }
}

/**
 * The fight as the overlay prints it: the [monsters] of the stage as cards in the battle's order, the hero's pools and states,
 * what just landed, and the blows so far, newest first. Shared by the map's runs and the trials (3.49.0).
 */
internal fun Battle.hud(
    monsters: List<RolledMonster>, leader: RolledMonster, speed: Int, started: Boolean, paused: Boolean, heroTaunt: Boolean,
    level: Int, escape: Boolean, stage: Int, stages: Int, interlude: Double?,
): FightHud {
    val battle = this
    val h = heroFighter
    val hits = events.withIndex()
        .filter { (_, event) -> event.time <= time && time - event.time < ExpeditionRun.HIT_LIFETIME && event.action != Action.RETREAT &&
            (event.damage > 0 || event.healed > 0 || event.kind == HitKind.EVADED || event.kind == HitKind.BLOCKED || event.action == Action.ATTACK) }
        .map { (index, event) ->
            FloatingHit(index, event.target, event.action, event.kind, event.damage.roundToInt(), time - event.time, event.healed.roundToInt(),
                event.type, event.inflicted, event.stunned, event.foe)
        }
    fun ailments(f: Battle.Fighter) = f.ailments.groupBy { it.ailment }.map { (ailment, active) ->
        val until = active.maxOf { it.until }
        AilmentView(ailment, ((until - battle.time) / active.first().duration).toFloat().coerceIn(0f, 1f), active.size,
            (until - battle.time).coerceAtLeast(0.0), if (ailment.hurts) active.sumOf { it.magnitude } else active.maxOf { it.magnitude })
    }
    val foes = foeFighters.map { f ->
        FoeView(f.index, monsters[f.index], f.life.roundToInt(), f.body.maxLife.roundToInt(), f.shield.roundToInt(), f.body.maxShield.roundToInt(),
            swing(f), ailments(f), f.held, f.alive, reachable(f.index), f.body.taunt, effects(f),
            f.mana.roundToInt(), f.body.maxMana.roundToInt(), back = f.ranged, place = window.place(f.index), waiting = window.waits(f.index))
    }
    return FightHud(
        ally = allyFighter?.let { f -> AllyView(ally!!.code, f.life.roundToInt(), f.body.maxLife.roundToInt(), f.alive) },
        leader = leader, foes = foes,
        heroLife = h.life.roundToInt(), heroShield = h.shield.roundToInt(),
        hits = hits, speed = speed,
        outcome = outcome,
        heroSwing = swing(h), heroAilments = ailments(h), heroHeld = h.held,
        retreating = retreating,
        lunge = lunge()?.let { (event, progress) -> LungeView(event.actor, event.action, event.kind, event.landed, progress.toFloat(), event.foe) },
        events = events.toList().asReversed(),
        started = started, paused = paused,
        target = target()?.index, focus = focus,
        loneWolf = rules.loneWolf.takeIf { loneWolf }, heroTaunt = heroTaunt,
        heroMana = h.mana.roundToInt(), heroMaxMana = manaCap().roundToInt(), heroReserved = manaReserved().roundToInt(),
        skills = skillViews(), flasks = flaskViews(), heroEffects = effects(h), heroCharges = chargeViews(),
        heroBarrier = h.barrier.roundToInt(),
        level = level, escape = escape,
        stage = stage, stages = stages, interlude = interlude,
    )
}
