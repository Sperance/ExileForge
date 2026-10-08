package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.FactorKey
import com.sperance.exileforge.core.campaign.HitTrace
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.NoteTrace
import com.sperance.exileforge.core.campaign.PhaseFoes
import com.sperance.exileforge.core.campaign.Spawns
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.FoeTotem
import com.sperance.exileforge.core.campaign.combat.HitKind
import com.sperance.exileforge.core.campaign.combat.RiftBoonLines
import com.sperance.exileforge.core.campaign.combat.RiftCombat
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.combat.sealStruck
import com.sperance.exileforge.core.campaign.combat.sealed
import com.sperance.exileforge.core.campaign.combat.stolenSource
import com.sperance.exileforge.core.campaign.combat.withShades
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.RiftGuardians
import com.sperance.exileforge.rules.content.RiftLaw
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Стражи Разлома в бою (3.96.0): печати Стража, кража Поглотителя, Закон Владыки; без Разлома бой прежний. */
class RiftCombatTest {
    private val index get() = TestContent.index
    private val rules get() = index.campaign.combat
    private val guardians: RiftGuardians get() = checkNotNull(index.campaign.trials?.rift).guardians

    /** Герой, которого не убить, и [extra] поверх его листа. */
    private fun hero(extra: Map<String, Double> = emptyMap()): Combatant {
        val heroClass = index.classes.classes.first()
        val sheet = Sheets.calculate(index, 40, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList())
        return Combatant(sheet.stats + mapOf(CoreStat.HEALTH.code to 1_000_000.0, "STOCK_ATTACK_PHYSICAL" to 40.0) + extra, 40, rules)
    }

    /** Страж зоны, которого не убить за время теста. */
    private fun boss(): Foe {
        val zone = index.campaign.zones.first()
        val run = Run(index, zone, 7L, RunContext(index.classes.classes.first().code, 40))
        val rolled = checkNotNull(Spawns(index, run).boss(zone, MapEffects.buffs(emptyMap()), emptyList()))
        val foe = PhaseFoes(index, rules).foe(rolled, zone.level)
        return foe.copy(body = Combatant(foe.body.stats + (CoreStat.HEALTH.code to 1e9), foe.body.level, rules), rarity = MonsterRarity.UNIQUE)
    }

    private fun Battle.run(seconds: Double) {
        var left = seconds
        while (left > 0 && outcome == null) {
            advance(0.1)
            left -= 0.1
        }
    }

    private fun Battle.notes(kind: NoteKind) = events.filter { (it.trace as? NoteTrace)?.kind == kind }

    @Test
    fun seals_cut_the_damage_break_by_hits_and_crits_and_grow_back() {
        val warden = guardians.warden
        val idol = FoeTotem(index.campaign.totems.byCode.getValue(warden.idols))
        // Засада на весь тест: печати снимает только сам тест
        val quiet = Battle(hero(), listOf(boss()), rules, index.rules.fight, 1_000_000.0, Random(3L), rift = RiftCombat(warden = warden, ambush = 1e6, idol = idol))
        val boss = quiet.foe(0)
        assertEquals(1 + warden.taken / 100, quiet.sealed(boss), 1e-9)
        quiet.run(0.2)
        assertEquals(warden.idolCount, quiet.totems.size, "the idols stand")
        quiet.sealStruck(boss, crit = true)
        assertEquals(warden.seals - 1, quiet.boss(), "a crit breaks a seal at once")
        repeat(warden.hits - 1) { quiet.sealStruck(boss, crit = false) }
        assertEquals(warden.seals - 1, quiet.boss(), "short of the hits, the seal holds")
        quiet.sealStruck(boss, crit = false)
        assertEquals(warden.seals - 2, quiet.boss(), "the hits break the next seal")
        quiet.run(warden.regrow + 0.2)
        assertEquals(warden.seals - 1, quiet.boss(), "a seal grows back after the pause")
        quiet.run(warden.every + 0.2)
        assertEquals(warden.seals, quiet.boss(), "and the rest one by one")
        assertTrue(quiet.notes(NoteKind.SEAL_BROKEN).size == 2 && quiet.notes(NoteKind.SEALS_RETURNED).size == 2)

        // В настоящем бою удар по Стражу несёт множитель печатей в своём следе
        val fight = Battle(hero(), listOf(boss()), rules, index.rules.fight, 1_000_000.0, Random(3L), rift = RiftCombat(warden = warden, idol = idol))
        fight.run(10.0)
        val sealedHit = fight.events.firstNotNullOfOrNull { e ->
            (e.trace as? HitTrace)?.takeIf { e.actor == Side.HERO && e.action == Action.ATTACK }?.factors?.firstOrNull { it.key == FactorKey.SEALS }
        }
        assertNotNull(sealedHit, "the hit shows the seals")
        assertEquals(1 + warden.taken / 100, sealedHit.value, 1e-9)
        assertTrue(fight.notes(NoteKind.SEAL_BROKEN).isNotEmpty(), "the hero's blows break seals")
    }

    @Test
    fun the_devourer_steals_a_boon_and_a_shade_rises_with_it() {
        val devourer = guardians.devourer
        val boon = RiftBoonLines("RIFT_TEST", listOf(StatLine(CoreStat.HEALTH.code, Op.ADD, 5_000.0)))
        val shade = boss().copy(rarity = MonsterRarity.NORMAL)
        val foes = PhaseFoes(index, rules).withShades(listOf(boss()), shade, devourer.shades)
        assertEquals(1 + devourer.shades, foes.size)
        val hero = hero()
        val battle = Battle(hero, foes, rules, index.rules.fight, 1_000_000.0, Random(5L), rift = RiftCombat(devourer = devourer, echo = 30, boons = listOf(boon), ambush = 1e6))
        val fed = Battle(hero, listOf(boss()), rules, index.rules.fight, 1_000_000.0, Random(5L))
        assertEquals(fed.foe(0).body.maxLife * (1 + minOf(30 * devourer.perEcho, devourer.maxPower) / 100), battle.foe(0).body.maxLife, 1.0)
        val before = battle.heroFighter.body.maxLife
        battle.run(devourer.stealEvery + 0.3)
        assertEquals(listOf("RIFT_TEST"), battle.notes(NoteKind.BOON_STOLEN).map { (it.trace as NoteTrace).ref })
        assertTrue(battle.heroFighter.body.maxLife < before, "the hero lost the boon's lines")
        val risen = foes.indices.drop(1).filter { battle.foe(it).alive }
        assertEquals(1, risen.size, "one shade rose")
        assertTrue(battle.foe(risen.single()).effects.any { it.source == stolenSource("RIFT_TEST") }, "the shade carries the boon")
    }

    @Test
    fun no_crits_law_turns_crits_into_hits() {
        val lord = guardians.lord.copy(every = 3.0, warn = 1.0, laws = listOf(RiftLaw.NO_CRITS))
        val critical = hero(mapOf(CoreStat.CRITICAL_CHANCE.code to 70.0))
        val free = Battle(critical, listOf(boss()), rules, index.rules.fight, 1_000_000.0, Random(9L))
        free.run(20.0)
        assertTrue(free.events.any { it.actor == Side.HERO && it.kind == HitKind.CRIT }, "the hero crits without the law")
        val ruled = Battle(critical, listOf(boss()), rules, index.rules.fight, 1_000_000.0, Random(9L), rift = RiftCombat(lord = lord))
        ruled.run(20.0)
        val proclaimed = ruled.notes(NoteKind.LAW).first().time
        assertTrue(ruled.events.none { it.actor == Side.HERO && it.kind == HitKind.CRIT && it.time > proclaimed }, "no crit under the law")
    }

    @Test
    fun a_rift_without_mechanics_is_the_same_fight() {
        val plain = Battle(hero(), listOf(boss()), rules, index.rules.fight, 1_000_000.0, Random(13L))
        val empty = Battle(hero(), listOf(boss()), rules, index.rules.fight, 1_000_000.0, Random(13L), rift = RiftCombat())
        plain.run(30.0)
        empty.run(30.0)
        assertTrue(plain.events.size > 10, "the fight went on")
        assertEquals(plain.events, empty.events)
    }

    /** Сколько печатей стоит у Стража сейчас - по полосе стража. */
    private fun Battle.boss(): Int = checkNotNull(riftFight).seals
}
