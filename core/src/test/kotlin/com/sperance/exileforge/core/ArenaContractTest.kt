package com.sperance.exileforge.core

import com.sperance.exileforge.core.model.combat.*
import com.sperance.exileforge.core.model.combat.arena.*
import org.junit.Test
import kotlin.test.*

/**
 * The arena is a renderer, and these tests are what stops it becoming a second combat engine.
 *
 * Each one pins the same rule from a different side: a number on the stage is a difference between two
 * server snapshots, never something the client worked out.
 */
class ArenaContractTest {
    private fun mob(id: String = "zombie", name: String = "Зомби", life: Double = 40.0,
        boss: Boolean = false, element: String = "physical") =
        Monster(id, name, life, 5.0, lootTableId = "common", experience = 10, gold = 3, element = element, boss = boss)

    private fun battle(turn: Int = 0, heroLife: Double = 100.0, enemyLife: Double = 40.0, mana: Double = 20.0,
        shield: Double = 0.0, potions: Int = 2, status: BattleStatus = BattleStatus.ACTIVE,
        log: List<String> = emptyList(), rewards: List<BattleReward> = emptyList(), monster: Monster = mob()) =
        Battle("battle-1", "coast", 1, monster,
            Combatant("Герой", 100.0, heroLife, 20.0, mana, shield),
            Combatant(monster.name, 40.0, enemyLife),
            turn, status, potions, log, rewards, LootTable("common", 1, listOf(LootEntry("NORMAL", 1))))

    /** Steps the stage the way a display does, in frames rather than one long jump. */
    private fun ArenaSimulation.run(millis: Long) {
        var left = millis
        while(left > 0L) { val step = minOf(16L, left); advance(step); left -= step }
    }

    @Test fun `a hit shows the life the server removed and not the number in its log`() {
        val arena = ArenaSimulation()
        assertTrue(arena.observe(battle()))
        arena.run(400)
        // The log deliberately disagrees with the snapshot: the stage must trust the snapshot.
        val struck = battle(turn = 1, enemyLife = 27.5, mana = 12.0, log = listOf("Герой бьёт Зомби на 999"))
        assertTrue(arena.observe(struck, action = BattleAction.ATTACK))
        arena.run(ArenaTiming.HERO_IMPACT + 48)
        val damage = arena.numbers.single { it.tint == ArenaTint.HERO_DAMAGE }
        assertEquals("12.5", damage.text)
        assertEquals("−8", arena.numbers.single { it.tint == ArenaTint.MANA }.text)
        assertEquals(27.5, arena.engaged?.life)
        assertEquals(100.0, arena.hero?.life)
    }

    @Test fun `a replayed idempotent answer is never animated twice`() {
        val arena = ArenaSimulation()
        arena.observe(battle()); arena.run(400)
        val turn = battle(turn = 1, enemyLife = 30.0, heroLife = 94.0, log = listOf("Обмен удара"))
        assertTrue(arena.observe(turn, action = BattleAction.ATTACK))
        arena.run(ArenaTiming.HERO_IMPACT + 48)
        val shown = arena.numbers.size
        assertFalse(arena.observe(turn, action = BattleAction.ATTACK))
        assertEquals(shown, arena.numbers.size)
    }

    @Test fun `a swing that took no life reads as a miss and shows no damage`() {
        val arena = ArenaSimulation()
        arena.observe(battle()); arena.run(400)
        arena.observe(battle(turn = 1, log = listOf("Зомби уклонился")), action = BattleAction.ATTACK)
        arena.run(ArenaTiming.HERO_IMPACT + 48)
        assertEquals("Мимо", arena.numbers.single().text)
        assertEquals(ArenaTint.MISS, arena.numbers.single().tint)
        assertEquals(40.0, arena.engaged?.life)
    }

    @Test fun `a kill leaves a corpse and drifts exactly the rewards the server granted`() {
        val arena = ArenaSimulation()
        arena.observe(battle()); arena.run(400)
        val victory = battle(turn = 4, enemyLife = 0.0, status = BattleStatus.VICTORY, log = listOf("Зомби повержен"),
            rewards = listOf(BattleReward("Опыт", 12), BattleReward("Золото", 5),
                BattleReward("Ржавый меч", 1, equipmentUuid = "uuid-1"), BattleReward("Сфера хаоса", 1, itemId = "chaos")))
        arena.observe(victory, action = BattleAction.ATTACK)
        arena.run(ArenaTiming.AFTERMATH + 4 * ArenaTiming.LOOT_STRIDE + 80)
        assertEquals(1, arena.corpses.size)
        assertEquals(listOf(LootKind.EXPERIENCE, LootKind.GOLD, LootKind.EQUIPMENT, LootKind.CURRENCY),
            arena.loot.map { it.kind })
        assertEquals(victory.rewards.map { it.name }, arena.loot.map { it.name })
        assertTrue(arena.engaged?.gone == true)
    }

    @Test fun `resuming a settled battle shows where it ended instead of replaying it`() {
        val arena = ArenaSimulation()
        val finished = battle(turn = 7, enemyLife = 0.0, status = BattleStatus.VICTORY,
            log = listOf("Зомби повержен"), rewards = listOf(BattleReward("Опыт", 30)))
        assertTrue(arena.observe(finished))
        arena.run(1_200)
        assertEquals(1, arena.corpses.size)
        // The loot reached the stash before the app was reopened, so nothing drifts a second time.
        assertTrue(arena.loot.isEmpty())
        assertEquals("Победа", arena.banner?.text)
    }

    @Test fun `walking and idling never change a server number`() {
        val arena = ArenaSimulation()
        arena.observe(battle(heroLife = 61.0, enemyLife = 33.0, shield = 12.0))
        val hero = requireNotNull(arena.hero)
        val enemy = requireNotNull(arena.engaged)
        arena.run(5_000)
        assertEquals(61.0, hero.life); assertEquals(100.0, hero.maxLife); assertEquals(12.0, hero.shield)
        assertEquals(33.0, enemy.life)
        // The mob walked in from off stage and stopped exactly on its mark.
        assertEquals(ArenaStageLayout.mobMelee, enemy.position)
        assertEquals(ArenaStageLayout.heroHome, hero.position)
    }

    @Test fun `a fresh encounter announces the monster and stands the rest of the zone by`() {
        val arena = ArenaSimulation()
        val roster = listOf(mob("a", "А"), mob("b", "Б"), mob("c", "В"), mob("d", "Г"), mob("e", "Д"))
        arena.observe(battle(monster = mob("a", "А")), roster)
        arena.run(240)
        assertEquals(listOf("a", "b", "c", "d"), arena.mobs.map { it.id })
        assertEquals(1, arena.mobs.count { !it.queued })
        assertEquals("А", arena.banner?.text)
        assertTrue(arena.mobs.filter { it.queued }.all { it.position == it.home })
    }

    @Test fun `a boss encounter is announced as one`() {
        val arena = ArenaSimulation()
        arena.observe(battle(monster = mob("king", "Король", boss = true, element = "fire")))
        arena.run(240)
        assertEquals(true, arena.banner?.boss)
        assertEquals("fire", arena.engaged?.element)
    }

    @Test fun `the delta is subtraction of server values and nothing else`() {
        val before = battle(turn = 2, heroLife = 80.0, enemyLife = 40.0, mana = 20.0, shield = 10.0, potions = 2)
        val after = battle(turn = 3, heroLife = 66.0, enemyLife = 21.0, mana = 12.0, shield = 4.0, potions = 1,
            log = listOf("Критический удар героя"))
        val delta = battleDelta(before, after, BattleAction.POWER)
        assertEquals(19.0, delta.heroDamage)
        assertEquals(14.0, delta.monsterDamage)
        assertEquals(8.0, delta.manaSpent)
        assertEquals(6.0, delta.shieldAbsorbed)
        assertEquals(1, delta.potionsSpent)
        assertTrue(delta.turnAdvanced)
        assertFalse(delta.fresh)
        assertFalse(delta.silent)
        assertEquals(listOf("Критический удар героя"), delta.log)
    }

    @Test fun `an unchanged snapshot is silent while a turn of two misses is not`() {
        val resting = battle(turn = 3)
        assertTrue(battleDelta(resting, resting, BattleAction.GUARD).silent)
        assertFalse(battleDelta(resting, battle(turn = 4), BattleAction.ATTACK).silent)
    }

    @Test fun `the flask shows the life the server restored and the heavy strike casts`() {
        val before = battle(turn = 1, heroLife = 30.0)
        val quaffed = battle(turn = 2, heroLife = 70.0, potions = 1)
        val flask = arenaScript(battleDelta(before, quaffed, BattleAction.POTION), "hero", "zombie", "Зомби",
            false, "physical") { "" }
        assertEquals(40.0, flask.single { it.kind == ArenaBeatKind.QUAFF }.amount)
        assertTrue(flask.none { it.kind == ArenaBeatKind.REGEN })
        val heavy = arenaScript(battleDelta(before, battle(turn = 2, heroLife = 30.0, enemyLife = 20.0, mana = 12.0),
            BattleAction.POWER), "hero", "zombie", "Зомби", false, "physical") { "" }
        assertTrue(heavy.any { it.kind == ArenaBeatKind.CAST })
        assertEquals(-8.0, heavy.single { it.kind == ArenaBeatKind.MANA }.amount)
        assertEquals(heavy.map { it.at }.sorted(), heavy.map { it.at })
    }

    @Test fun `guard keeps the hero home and a retreat ends the script`() {
        val before = battle(turn = 1)
        val guarded = arenaScript(battleDelta(before, battle(turn = 2, heroLife = 96.0, mana = 26.0), BattleAction.GUARD),
            "hero", "zombie", "Зомби", false, "physical") { "" }
        assertTrue(guarded.any { it.kind == ArenaBeatKind.GUARD })
        assertTrue(guarded.none { it.kind == ArenaBeatKind.ADVANCE })
        assertEquals(ArenaTiming.DEFENSIVE_IMPACT, guarded.single { it.kind == ArenaBeatKind.IMPACT }.at)
        val fled = arenaScript(battleDelta(before, battle(turn = 2, status = BattleStatus.FLED), BattleAction.FLEE),
            "hero", "zombie", "Зомби", false, "physical") { "" }
        assertEquals(listOf(ArenaBeatKind.RETREAT, ArenaBeatKind.BANNER), fled.map { it.kind })
    }

    @Test fun `arena time only moves forward`() {
        assertFailsWith<IllegalArgumentException> { ArenaSimulation().advance(-1L) }
    }

    @Test fun `a reset stage forgets the fight it was drawing`() {
        val arena = ArenaSimulation()
        arena.observe(battle()); arena.run(400)
        arena.reset()
        assertNull(arena.hero); assertNull(arena.engaged); assertNull(arena.banner)
        assertTrue(arena.mobs.isEmpty() && arena.numbers.isEmpty() && arena.corpses.isEmpty())
        assertFalse(arena.playing)
        // The same battle is a fresh encounter again, not a replay that has already been seen.
        assertTrue(arena.observe(battle()))
    }
}
