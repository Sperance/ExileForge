package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Ally
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.HitKind
import com.sperance.exileforge.core.campaign.combat.SlotHolder
import com.sperance.exileforge.core.campaign.combat.chargeViews
import com.sperance.exileforge.core.campaign.combat.effects
import com.sperance.exileforge.core.campaign.combat.flaskViews
import com.sperance.exileforge.core.campaign.combat.skillViews
import com.sperance.exileforge.core.campaign.run.AilmentView
import com.sperance.exileforge.core.campaign.run.AllyView
import com.sperance.exileforge.core.campaign.run.BossHud
import com.sperance.exileforge.core.campaign.run.BuildupView
import com.sperance.exileforge.core.campaign.run.CastView
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.campaign.run.FloatingHit
import com.sperance.exileforge.core.campaign.run.FoeView
import com.sperance.exileforge.core.campaign.run.LungeView
import com.sperance.exileforge.core.campaign.run.SlotView
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetRole
import com.sperance.exileforge.rules.content.RiftLaw
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.rules.roll.RolledMonster
import kotlin.math.roundToInt

/**
 * The combat pet as a fighter: its sheet at its level, what its role does; a new one each fight stands up whole. Since 3.33.0
 * (server 1.32.0) the hero's sheet reaches it — its levels, damage, life, speed, armour and resistances — made again only
 * when those change. Shared by the map's runs and the trials (3.49.0).
 */
internal class PetAllies(private val index: ContentIndex, private val rules: CombatRules) {
    private var made: Triple<Pet?, Map<String, Double>, Ally?>? = null
    private val menagerie by lazy { Menagerie(index) }

    /** [pet] as a fighter under the hero's [heroStats]; since 3.70.0 the pet is the caller's, so one put to work mid-run joins the next fight. */
    fun of(heroStats: Map<String, Double>, pet: Pet?): Ally? {
        val boons = PetBoons.of(heroStats)
        made?.takeIf { it.first == pet && it.second == boons }?.let { return it.third }
        val ally = pet?.let { own ->
            val kind = menagerie.species(own.species) ?: return@let null
            val levels = (boons[PetBoons.LEVEL] ?: 0.0).toInt()
            val p = if (levels != 0) own.copy(level = (own.level + levels).coerceAtLeast(1)) else own
            Ally(
                p.species,
                Combatant(PetBoons.apply(menagerie.sheet(p), boons), p.level, rules),
                (p.role ?: kind.role) == PetRole.TANK,
                if ((p.role ?: kind.role) == PetRole.SUPPORT) menagerie.supportHeal(p) else 0.0,
                index.pets.drawFire,
            )
        }
        made = Triple(pet, boons, ally)
        return ally
    }
}

/**
 * The fight as the overlay prints it: the [monsters] of the fight as cards in the battle's order, the hero's pools and states,
 * what just landed, and the blows so far, newest first. Shared by the map's runs and the trials (3.49.0).
 */
internal fun Battle.hud(
    monsters: List<RolledMonster>,
    leader: RolledMonster,
    speed: Int,
    started: Boolean,
    paused: Boolean,
    heroTaunt: Boolean,
    level: Int,
    round: Int = 1,
    rounds: Int = 1,
): FightHud {
    val battle = this
    val h = heroFighter
    val hits = events.withIndex()
        .filter { (_, event) ->
            event.time <= time && time - event.time < ExpeditionRun.HIT_LIFETIME &&
                (event.damage > 0 || event.healed > 0 || event.kind == HitKind.EVADED || event.kind == HitKind.BLOCKED || event.action == Action.ATTACK)
        }
        .map { (index, event) ->
            FloatingHit(
                index, event.target, event.action, event.kind, event.damage.roundToInt(), time - event.time, event.healed.roundToInt(),
                event.type, event.inflicted, event.stunned, event.foe, event.pet != null,
            )
        }
    fun ailments(f: Battle.Fighter) = f.ailments.groupBy { it.ailment }.map { (ailment, active) ->
        val until = active.maxOf { it.until }
        AilmentView(
            ailment,
            ((until - battle.time) / active.first().duration).toFloat().coerceIn(0f, 1f),
            active.size,
            (until - battle.time).coerceAtLeast(0.0),
            if (ailment.hurts) active.sumOf { it.magnitude } else active.maxOf { it.magnitude },
        )
    }
    fun buildup(f: Battle.Fighter) = rules.buildup?.let {
        val lead = f.leading()
        BuildupView(
            lead?.first,
            (lead?.second ?: 0.0).toFloat().coerceIn(0f, 1f),
            f.buildup.map { it.toFloat().coerceIn(0f, 1f) },
            f.stunnedUntil > battle.time,
            f.shatter,
            f.electrocutedUntil > battle.time,
        )
    }
    val foes = foeFighters.map { f ->
        FoeView(
            f.index, monsters.getOrNull(f.index) ?: checkNotNull(foes[f.index].origin), shownLife(f.life, f.alive), f.body.maxLife.roundToInt(), f.shield.roundToInt(), f.body.maxShield.roundToInt(),
            swing(f), ailments(f), f.held, f.alive, reachable(f.index), f.body.taunt, effects(f),
            f.mana.roundToInt(), f.body.maxMana.roundToInt(), place = place(f.index), buildup = buildup(f),
            barrier = f.barrier.takeIf { f.barrierUntil > time }?.roundToInt() ?: 0,
        )
    }
    return FightHud(
        ally = allyFighter?.let { f -> AllyView(ally!!.code, shownLife(f.life, f.alive), f.body.maxLife.roundToInt(), f.alive) },
        leader = leader, foes = foes,
        heroLife = shownLife(h.life, h.alive), heroShield = h.shield.roundToInt(),
        hits = hits, speed = speed,
        outcome = outcome,
        heroSwing = swing(h), heroAilments = ailments(h), heroHeld = h.held, heroBuildup = buildup(h),
        lunge = lunge()?.let { (event, progress) -> LungeView(event.actor, event.action, event.kind, event.landed, progress.toFloat(), event.foe, event.pet != null) },
        events = events.toList().asReversed(),
        started = started, paused = paused,
        target = target()?.index, focus = focus,
        heroTaunt = heroTaunt,
        heroMana = h.mana.roundToInt(), heroMaxMana = manaCap().roundToInt(), heroReserved = manaReserved().roundToInt(),
        skills = skillViews(), flasks = flaskViews(), heroEffects = effects(h), heroCharges = chargeViews(),
        heroBarrier = h.barrier.roundToInt(),
        level = level,
        round = round, rounds = rounds,
        heroBody = h.body,
        boss = bossHud(),
    )
}

/** Босс боя (3.92.0): первый враг редкости босса или с фазами - его пороги и умение на подходе. */
private fun Battle.bossHud(): BossHud? {
    val i = guardian ?: return null
    val foe = foes[i]
    val fighter = foeFighters[i]
    val cast = (foe.skills + learned[i].orEmpty()).mapNotNull { skill ->
        val ready = fighter.readyAt[skill.code] ?: return@mapNotNull null
        CastView(skill.code, (ready - time).coerceAtLeast(0.0), skill.cooldown / fighter.body.recovery(skill.spell))
    }.minByOrNull { it.left }?.takeIf { fighter.alive }
    val slots = slotHolders.map { holder ->
        when (holder) {
            null -> null
            is SlotHolder.Minion -> SlotView.Minion(holder.index)
            is SlotHolder.Totem -> holder.totem.let { t -> SlotView.Totem(t.serial, t.totem.totem.code, t.totem.totem.kind, (t.until - time).coerceAtLeast(0.0), t.until - t.raised, t.totem.totem.element) }
        }
    }
    val rage = rules.bossEnrage
    val hud = BossHud(i, foe.phase, foe.phases.map { it.step.at }, foe.phases.indices.map { (i to it) in phased }, cast, slots, rage.damage * enrage, (rage.every - time % rage.every).coerceAtLeast(0.0), tainted = foe.tainted)
    return riftHud(hud)
}

/** Полоса стража Разлома (3.96.0): печати, Законы, украденное - поверх полосы босса. */
private fun Battle.riftHud(hud: BossHud): BossHud {
    val fight = riftFight ?: return hud
    return hud.copy(
        rift = fight.guard,
        seals = fight.seals,
        sealsMax = fight.sealsMax,
        sealHits = fight.sealHits,
        sealEvery = fight.rules.warden?.hits ?: 0,
        law = fight.laws,
        lawNext = fight.next,
        lawIn = if (fight.rules.lord != null) (fight.lawAt - time).coerceAtLeast(0.0) else 0.0,
        lawElement = fight.element?.takeIf { RiftLaw.ONE_ELEMENT in fight.laws }?.name,
        lawNextElement = fight.nextElement?.name,
        stolen = fight.stolen.toList(),
        devoured = fight.devoured,
    )
}

/**
 * Здоровье на экране (3.88.8): у живого - вверх до целого и не меньше 1, у павшего - 0. «0» у живого противника читалось
 * как смерть, хотя у него оставалась доля единицы.
 */
fun shownLife(life: Double, alive: Boolean): Int = if (alive) kotlin.math.ceil(life).toInt().coerceAtLeast(1) else 0
