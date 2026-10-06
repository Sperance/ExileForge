package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.MonsterEffect
import com.sperance.exileforge.rules.roll.MonsterRoller
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.run.Run

/**
 * The monsters of a run as the client stands them on the map. The rules roll every pack from the run's
 * seed — the same roll the server checks a kill against; what only the fight cares about is the client's:
 * a map's buffs on the stats, the skills a monster casts, a boss's lines at this encounter, a crystal's guardian.
 */
class Spawns(private val index: ContentIndex, private val run: Run) {
    private val monsters = MonsterRoller(index)
    private val campaign get() = index.campaign
    private val book get() = index.skills

    /** Every token of the zone — or of its Vaal zone — as the seed rolls it, with [buffs] of the map on each. */
    fun packs(vaal: Boolean, buffs: List<MonsterEffect>): List<List<RolledMonster>> {
        val casting = run.streams.of(if (vaal) "vaalCasting" else "casting")
        return List(if (vaal) run.vaalCount else run.count) { i -> run.spawn(i, vaal).pack.map { skilled(buffed(it, buffs), casting) } }
    }

    /**
     * Стая, что встаёт посреди захода (3.89.1, сервер 1.81.3): подкрепление алтаря (`Run.REINFORCEMENT + k`) или страж сокровищ
     * (`Run.GUARD + номер объекта`) - жетон [token], вожак редкий; [buffs] карты на каждом, навыки - на своём потоке.
     */
    fun summoned(token: Int, buffs: List<MonsterEffect>): List<RolledMonster> {
        val casting = run.streams.of("summonedCasting", token)
        return run.spawn(token).pack.map { skilled(buffed(it, buffs), casting) }
    }

    /** The zone's boss at this encounter: its signature lines and a few of its table, [extra] what the map does to it alone. */
    fun boss(zone: Zone, buffs: List<MonsterEffect>, extra: List<MonsterEffect>): RolledMonster? = guardian(zone.boss, zone.level, buffs, extra, run.streams.of("bossRoll"))

    /** The guardian of the Vaal zone behind the portal, sealing its exit. */
    fun corrupted(zone: Zone, buffs: List<MonsterEffect>): RolledMonster? = guardian(zone.corrupted, zone.level, buffs, emptyList(), run.streams.of("corruptRoll"))

    private fun guardian(code: MonsterCode, level: Int, buffs: List<MonsterEffect>, extra: List<MonsterEffect>, dice: Dice): RolledMonster? {
        if (code.value.isBlank() || index.monster(code) == null) return null
        return skilled(buffed(monsters.boss(monsters.guardian(code, level), dice, extra), buffs), dice)
    }

    /**
     * A crystal's guardian: the zone's monster standing up rare — its rule's lines drawn as any rare's — with
     * the line of every kind of essence it guards, and [extra], what a Vaal orb and the atlas add to it.
     */
    fun crystalGuardian(zone: Zone, crystal: Crystal, place: Int, buffs: List<MonsterEffect>, extra: List<MonsterEffect>): RolledMonster? {
        val monster = index.monster(crystal.guardian) ?: zone.monsters.firstOrNull()?.let(index::monster) ?: return null
        val dice = run.streams.of("crystalGuardian", place)
        val rule = campaign.rarity(MonsterRarity.RARE)
        val picked = monsters.draw(run.pool, zone.level, rule, dice.between(rule.modifiers), dice)
        val essences = crystal.essences.mapNotNull { index.essence(it)?.kind?.monster }.distinct().mapNotNull(index::modifier)
            .map { monsters.rolled(monsters.raise(it, 1, zone.level), 1.0, dice) }
        return skilled(buffed(monsters.build(monster, zone.level, rule, picked + essences, dice, extra), buffs), dice)
    }

    /** The map's [buffs] folded into the monster's stats and kept apart, so the arena can say which is which. */
    fun buffed(monster: RolledMonster, buffs: List<MonsterEffect>): RolledMonster = if (buffs.isEmpty()) monster else monster.copy(stats = monsters.fold(monster.stats, buffs), mapBuffs = monster.mapBuffs + buffs)

    /**
     * What a monster casts: a boss its own skills; a monster with mana and none of its own, the casters' spell
     * of its leading element; one with a mad essence, one more of any monster's, and mana to cast it with.
     */
    fun skilled(monster: RolledMonster, dice: Dice): RolledMonster {
        val mana = monster.stats[CoreStat.MANA.code] ?: 0.0
        val own = monster.skills.ifEmpty {
            if (mana > 0) listOfNotNull(book.rules.casterSpells[DamageType.entries.maxBy { monster.stats[it.attack] ?: 0.0 }.name]) else emptyList()
        }
        val borrows = (monster.stats[CoreStat.BORROW_SKILLS.code] ?: 0.0) > 0 && book.monsterSkills.isNotEmpty()
        val skills = (own + if (borrows) listOf(dice.pick(book.monsterSkills).code) else emptyList()).distinct()
        if (skills == monster.skills) return monster
        val stats = if (borrows && mana <= 0) monster.stats + (CoreStat.MANA.code to BORROWED_MANA) else monster.stats
        return monster.copy(skills = skills, stats = stats)
    }

    /** Whether this run hides a Vaal portal: [chance] of it, on a zone with a guardian of corruption to stand at the zone's end. */

    companion object {
        private const val BORROWED_MANA = 40.0
    }
}
