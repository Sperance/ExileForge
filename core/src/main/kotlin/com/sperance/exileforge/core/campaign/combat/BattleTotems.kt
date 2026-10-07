package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.campaign.lines
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.BossTotem
import com.sperance.exileforge.rules.content.MonsterSkill
import com.sperance.exileforge.rules.content.PowerEvent
import com.sperance.exileforge.rules.content.TotemKind

/** Тотем правил в бою (3.93.0): сам тотем и его проклятие, уже найденное среди умений монстров. */
data class FoeTotem(val totem: BossTotem, val curse: MonsterSkill? = null)

/**
 * Тотем, что стоит (3.93.0): поставлен боссом [owner] в слот [slot] в [raised] и рассыпается в [until] - или раньше, со смертью
 * босса или вытесненный новым. [serial] - его номер в бою: источник его строк на босе, свите и герое.
 */
class StandingTotem internal constructor(val serial: Int, val totem: FoeTotem, val owner: Int, val slot: Int, val raised: Double, val until: Double) {
    internal var nextBurst: Double = raised + totem.totem.interval
    val source: String get() = "$TOTEM_SOURCE$serial"
}

/** Кто занял слот вокруг босса (3.93.0): приспешник свиты или тотем. */
sealed interface SlotHolder {
    data class Minion(val index: Int) : SlotHolder

    data class Totem(val totem: StandingTotem) : SlotHolder
}

/** Источник строк тотема: префикс и номер тотема в бою. */
internal const val TOTEM_SOURCE = "totem#"

/** Свободный слот вокруг босса или null - все заняты. */
internal fun Battle.freeSlot(): Int? = slotHolders.indices.firstOrNull { slotHolders[it] == null }

/** Враги на поле для условий героя (3.93.0): стоящие враги и тотемы - тотем бить нельзя, но он враг. */
internal fun Battle.enemies(): Int = foeFighters.count { it.alive } + standingTotems.size

/**
 * Босс [boss] ставит тотем (3.93.0): один из [choices] наугад. Свободный слот берёт сразу, иначе вытесняет самый старый тотем;
 * все слоты у свиты - тотема нет.
 */
internal fun Battle.raiseTotem(boss: Fighter, choices: List<FoeTotem>) {
    if (choices.isEmpty() || !boss.alive) return
    val slot = freeSlot() ?: standingTotems.minByOrNull { it.raised }?.let { oldest ->
        fallTotem(oldest)
        oldest.slot
    } ?: return
    val pick = choices[random.nextInt(choices.size)]
    val standing = StandingTotem(++totemSerial, pick, boss.index, slot, time, time + pick.totem.duration)
    standingTotems += standing
    slotHolders[slot] = SlotHolder.Totem(standing)
    bless(standing)
    note(boss, NoteKind.TOTEM, pick.totem.code)
    powers.fire(PowerEvent.TOTEM_RAISED)
    remake(heroFighter)
}

/** Что тотем кладёт, встав: строки на босса или свиту, проклятие на героя - до своего конца. */
private fun Battle.bless(standing: StandingTotem) {
    val totem = standing.totem.totem
    val left = standing.until - time
    val lines = totem.lines.map { StatLine(it.stat, it.op, it.value) }
    when (totem.kind) {
        TotemKind.POWER -> buff(foeFighters[standing.owner], standing.source, lines, left)

        TotemKind.RETINUE -> retinueOf(standing.owner).forEach { buff(it, standing.source, lines, left) }

        TotemKind.CURSE -> standing.totem.curse?.curse?.takeIf { !heroFighter.body.immuneCurse }?.let { curse ->
            lay(heroFighter, TimedEffect(EffectKind.CURSE, standing.source, curse.stats.lines(1), standing.until, left))
        }

        TotemKind.ELEMENT -> Unit
    }
}

/** Приспешник [minion] встал, пока стоят знамёна его босса: их строки ложатся и на него. */
internal fun Battle.bannerFor(minion: Fighter) {
    val owner = foes[minion.index].summonOf ?: return
    standingTotems.filter { it.owner == owner && it.totem.totem.kind == TotemKind.RETINUE }.forEach { standing ->
        buff(minion, standing.source, standing.totem.totem.lines.map { StatLine(it.stat, it.op, it.value) }, standing.until - time)
    }
}

private fun Battle.retinueOf(boss: Int): List<Fighter> = foes.indices.filter { foes[it].summonOf == boss && foeFighters[it].alive }.map { foeFighters[it] }

/**
 * Тотемы за срез (3.93.0): стихийный бьёт героя раз в свой период, отстоявший рассыпается; босс со своими тотемами ставит
 * новый раз в свой срок.
 */
internal fun Battle.totemTick() {
    standingTotems.toList().forEach { standing ->
        val boss = foeFighters[standing.owner]
        when {
            time >= standing.until - 1e-9 || !boss.alive -> fallTotem(standing)

            standing.totem.totem.kind == TotemKind.ELEMENT && time >= standing.nextBurst - 1e-9 -> {
                standing.nextBurst += standing.totem.totem.interval
                if (heroFighter.alive) totemBurst(boss, standing)
            }
        }
    }
    foeFighters.forEach { boss ->
        val own = foes[boss.index].totems
        if (own.isEmpty() || !boss.alive) return@forEach
        val next = totemAt.getOrPut(boss.index) { time + foes[boss.index].totemFirst }
        if (time >= next - 1e-9) {
            totemAt[boss.index] = time + foes[boss.index].totemEvery
            raiseTotem(boss, own)
        }
    }
}

/** Удар стихийного тотема: доля урона оружия босса его стихией. */
private fun Battle.totemBurst(boss: Fighter, standing: StandingTotem) {
    val totem = standing.totem.totem
    val element = DamageType.element(totem.element) ?: return
    val total = boss.body.damage.values.sum() * totem.burst / 100
    strike(boss, foeTarget(), Blow(mapOf(element to total), Action.SKILL, spell = true, skill = totem.code, spread = false))
}

/** Тотем рассыпался: слот свободен, его строки сняты со всех, силы героя слышат об этом. */
internal fun Battle.fallTotem(standing: StandingTotem) {
    if (!standingTotems.remove(standing)) return
    if ((slotHolders[standing.slot] as? SlotHolder.Totem)?.totem === standing) slotHolders[standing.slot] = null
    (foeFighters + heroFighter).forEach { fighter -> if (fighter.effects.removeAll { it.source == standing.source }) remake(fighter) }
    note(foeFighters[standing.owner], NoteKind.TOTEM_FALL, standing.totem.totem.code)
    powers.fire(PowerEvent.TOTEM_FALLS)
    remake(heroFighter)
}

/** Босс пал: все его тотемы рассыпаются с ним. */
internal fun Battle.fellTotems(boss: Fighter) {
    standingTotems.filter { it.owner == boss.index }.forEach(::fallTotem)
}
