package com.sperance.exileforge.core

import com.sperance.exileforge.core.model.combat.*
import com.sperance.exileforge.core.model.combat.world.*
import org.junit.Test
import kotlin.math.abs
import kotlin.test.*

/**
 * The world is a renderer with legs, and these tests are what stops it becoming a second combat engine.
 *
 * Each one pins the same rule from a different side: a number on the ground is a difference between two
 * server snapshots, and walking decides only *when* the client asks — never what the answer is worth.
 */
class WorldContractTest {
    private fun mob(id: String = "zombie", name: String = "Зомби", life: Double = 40.0,
        boss: Boolean = false, element: String = "physical") =
        Monster(id, name, life, 5.0, lootTableId = "common", experience = 10, gold = 3, element = element, boss = boss)

    private fun battle(turn: Int = 0, heroLife: Double = 100.0, enemyLife: Double = 40.0, mana: Double = 20.0,
        shield: Double = 0.0, potions: Int = 2, status: BattleStatus = BattleStatus.ACTIVE,
        log: List<String> = emptyList(), rewards: List<BattleReward> = emptyList(), monster: Monster = mob(),
        id: String = "battle-1") =
        Battle(id, "coast", 1, monster,
            Combatant("Герой", 100.0, heroLife, 20.0, mana, shield),
            Combatant(monster.name, 40.0, enemyLife),
            turn, status, potions, log, rewards, LootTable("common", 1, listOf(LootEntry("NORMAL", 1))))

    /** Steps the world the way a display does, in frames rather than one long jump. */
    private fun WorldSimulation.run(millis: Long) {
        var left = millis
        while(left > 0L) { val step = minOf(16L, left); advance(step); left -= step }
    }

    /** Walks the hero onto the mob the server put in the battle, the way a thumb would. */
    private fun WorldSimulation.closeIn(limit: Long = 8_000) {
        var left = limit
        while(left > 0L && !inStrikeRange) {
            val target = engaged?.position ?: return
            steer((target - (hero?.position ?: return)).direction)
            advance(16L); left -= 16L
        }
        steer(Vec2.ZERO)
    }

    @Test fun `a hit shows the life the server removed and not the number in its log`() {
        val world = WorldSimulation()
        assertTrue(world.observe(battle()))
        world.run(400)
        // The log deliberately disagrees with the snapshot: the world must trust the snapshot.
        val struck = battle(turn = 1, enemyLife = 27.5, mana = 12.0, log = listOf("Герой бьёт Зомби на 999"))
        assertTrue(world.observe(struck, action = BattleAction.ATTACK))
        world.run(WorldTiming.HERO_IMPACT + 48)
        val damage = world.numbers.single { it.tint == WorldTint.HERO_DAMAGE }
        assertEquals("12.5", damage.text)
        assertEquals("−8", world.numbers.single { it.tint == WorldTint.MANA }.text)
        assertEquals(27.5, world.engaged?.life)
        assertEquals(100.0, world.hero?.life)
    }

    @Test fun `a replayed idempotent answer is never animated twice`() {
        val world = WorldSimulation()
        world.observe(battle()); world.run(400)
        val turn = battle(turn = 1, enemyLife = 30.0, heroLife = 94.0, log = listOf("Обмен удара"))
        assertTrue(world.observe(turn, action = BattleAction.ATTACK))
        world.run(WorldTiming.HERO_IMPACT + 48)
        val shown = world.numbers.size
        assertFalse(world.observe(turn, action = BattleAction.ATTACK))
        assertEquals(shown, world.numbers.size)
    }

    @Test fun `a swing that took no life reads as a miss and shows no damage`() {
        val world = WorldSimulation()
        world.observe(battle()); world.run(400)
        world.observe(battle(turn = 1, log = listOf("Зомби уклонился")), action = BattleAction.ATTACK)
        world.run(WorldTiming.HERO_IMPACT + 48)
        assertEquals("Мимо", world.numbers.single().text)
        assertEquals(WorldTint.MISS, world.numbers.single().tint)
        assertEquals(40.0, world.engaged?.life)
    }

    @Test fun `a kill leaves a corpse and drops exactly the rewards the server granted`() {
        val world = WorldSimulation()
        world.observe(battle()); world.run(400)
        val victory = battle(turn = 4, enemyLife = 0.0, status = BattleStatus.VICTORY, log = listOf("Зомби повержен"),
            rewards = listOf(BattleReward("Опыт", 12), BattleReward("Золото", 5),
                BattleReward("Ржавый меч", 1, equipmentUuid = "uuid-1"), BattleReward("Сфера хаоса", 1, itemId = "chaos")))
        world.observe(victory, action = BattleAction.ATTACK)
        world.run(WorldTiming.AFTERMATH + 4 * WorldTiming.DROP_STRIDE + 80)
        assertEquals(1, world.corpses.size)
        assertEquals(listOf(LootKind.EXPERIENCE, LootKind.GOLD, LootKind.EQUIPMENT, LootKind.CURRENCY),
            world.loot.map { it.kind })
        assertEquals(victory.rewards.map { it.name }, world.loot.map { it.title })
        assertTrue(world.engaged?.gone == true)
    }

    @Test fun `walking over a drop claims it and the same drop is never claimed twice`() {
        val world = WorldSimulation()
        world.observe(battle()); world.run(400)
        world.observe(battle(turn = 2, enemyLife = 0.0, status = BattleStatus.VICTORY,
            rewards = listOf(BattleReward("Опыт", 12), BattleReward("Ржавый меч", 1, equipmentUuid = "uuid-1"))),
            action = BattleAction.ATTACK)
        world.run(WorldTiming.AFTERMATH + 2 * WorldTiming.DROP_STRIDE + 80)
        assertEquals(2, world.loot.size)
        // Walk to each pile in turn; the hero picks them up simply by standing on them.
        repeat(2) {
            val pile = world.loot.firstOrNull() ?: return@repeat
            var left = 12_000L
            while(left > 0L && world.loot.contains(pile)) {
                world.steer((pile.position - (world.hero?.position ?: return)).direction)
                world.advance(16L); left -= 16L
            }
            world.steer(Vec2.ZERO)
        }
        assertTrue(world.loot.isEmpty())
        assertEquals(listOf("Опыт", "Ржавый меч"), world.collected.map { it.title })
        world.run(2_000)
        assertEquals(2, world.collected.size)
    }

    @Test fun `resuming a settled battle shows where it ended instead of replaying it`() {
        val world = WorldSimulation()
        val finished = battle(turn = 7, enemyLife = 0.0, status = BattleStatus.VICTORY,
            log = listOf("Зомби повержен"), rewards = listOf(BattleReward("Опыт", 30)))
        assertTrue(world.observe(finished))
        world.run(1_200)
        assertEquals(1, world.corpses.size)
        // The loot reached the stash before the app was reopened, so nothing falls a second time.
        assertTrue(world.loot.isEmpty())
        assertEquals("Победа", world.banner?.text)
    }

    @Test fun `walking never changes a server number`() {
        val world = WorldSimulation()
        world.observe(battle(heroLife = 61.0, enemyLife = 33.0, shield = 12.0))
        val hero = requireNotNull(world.hero)
        val enemy = requireNotNull(world.engaged)
        world.steer(Vec2(1f, 0f))
        world.run(5_000)
        assertEquals(61.0, hero.life); assertEquals(100.0, hero.maxLife); assertEquals(12.0, hero.shield)
        assertEquals(33.0, enemy.life)
        assertTrue(hero.position != world.field.centre)
    }

    @Test fun `the hero stays inside the walls and out of the stones`() {
        val world = WorldSimulation()
        world.observe(battle())
        val hero = requireNotNull(world.hero)
        listOf(Vec2(1f, 1f), Vec2(-1f, -1f), Vec2(1f, -1f), Vec2(-1f, 1f)).forEach { heading ->
            world.steer(heading.direction)
            world.run(9_000)
            assertTrue(hero.position.x in 0f..world.field.width, "${hero.position}")
            assertTrue(hero.position.y in 0f..world.field.height, "${hero.position}")
            assertTrue(world.field.props.filter { it.blocking }
                .none { hero.position.distanceTo(it.position) < it.radius + hero.radius - Vec2.EPSILON },
                "${hero.position}")
        }
    }

    @Test fun `the mob closes on the hero and stops at its reach`() {
        val world = WorldSimulation()
        world.observe(battle())
        val hero = requireNotNull(world.hero)
        val mob = requireNotNull(world.engaged)
        assertTrue(mob.position.distanceTo(hero.position) > WorldSimulation.STRIKE_RANGE)
        world.run(12_000)
        assertTrue(mob.position.distanceTo(hero.position) <= WorldSimulation.MOB_REACH + .25f,
            "${mob.position.distanceTo(hero.position)}")
    }

    @Test fun `nothing is asked for out of reach and a swing is asked for within it`() {
        val world = WorldSimulation()
        world.observe(battle())
        // The mob spawns well out of reach, so distance is the only thing gating the first ask.
        world.run(200)
        assertFalse(world.inStrikeRange)
        assertNull(world.takeAction())
        world.closeIn()
        assertTrue(world.inStrikeRange)
        assertEquals(BattleAction.ATTACK, world.takeAction())
        // The cadence holds the next ask back rather than letting a frame loop spam the server.
        assertNull(world.takeAction())
        world.run(WorldSimulation.CADENCE + 32)
        assertEquals(BattleAction.ATTACK, world.takeAction())
    }

    @Test fun `the auto pilot drinks before it swings and never while the battle is over`() {
        val hurt = battle(heroLife = 20.0, potions = 2)
        assertEquals(BattleAction.POTION, AutoPilot.choose(hurt, inRange = true, swings = 0))
        assertEquals(BattleAction.POTION, AutoPilot.choose(hurt, inRange = false, swings = 0))
        // Out of flasks and nearly out of life: brace instead of trading blows.
        assertEquals(BattleAction.GUARD, AutoPilot.choose(battle(heroLife = 12.0, potions = 0), true, 0))
        assertNull(AutoPilot.choose(battle(status = BattleStatus.VICTORY), true, 0))
        assertNull(AutoPilot.choose(battle(), inRange = false, swings = 0))
    }

    @Test fun `the heavy strike is spent on the rhythm the mana allows`() {
        val ready = battle(mana = 20.0)
        assertEquals(BattleAction.ATTACK, AutoPilot.choose(ready, true, 0))
        assertEquals(BattleAction.ATTACK, AutoPilot.choose(ready, true, 1))
        assertEquals(BattleAction.POWER, AutoPilot.choose(ready, true, 2))
        // The server charges eight mana for it, so an empty pool swings plain steel instead.
        assertEquals(BattleAction.ATTACK, AutoPilot.choose(battle(mana = 4.0), true, 2))
    }

    @Test fun `a turn still playing out holds the next ask back`() {
        val world = WorldSimulation()
        world.observe(battle()); world.closeIn()
        assertEquals(BattleAction.ATTACK, world.takeAction())
        world.observe(battle(turn = 1, enemyLife = 28.0), action = BattleAction.ATTACK)
        // The cadence has elapsed, but the swing it answered is still being drawn.
        world.run(WorldSimulation.CADENCE + 32)
        assertTrue(world.playing)
        assertNull(world.takeAction())
        world.run(WorldTiming.AFTERMATH + 64)
        assertFalse(world.playing)
        assertEquals(BattleAction.ATTACK, world.takeAction())
    }

    @Test fun `every drop lands where the hero can stand`() {
        val world = WorldSimulation()
        world.observe(battle()); world.run(400)
        world.observe(battle(turn = 2, enemyLife = 0.0, status = BattleStatus.VICTORY,
            rewards = (1..4).map { BattleReward("Награда $it", it.toLong()) }), action = BattleAction.ATTACK)
        world.run(WorldTiming.AFTERMATH + 4 * WorldTiming.DROP_STRIDE + 80)
        val hero = requireNotNull(world.hero)
        assertEquals(4, world.loot.size)
        world.loot.forEach { pile ->
            assertEquals(pile.position, world.field.resolve(pile.position, hero.radius))
        }
    }

    @Test fun `switching the auto pilot off leaves the client silent`() {
        val world = WorldSimulation()
        world.observe(battle()); world.closeIn()
        world.auto = false
        assertNull(world.takeAction())
        world.auto = true
        assertEquals(BattleAction.ATTACK, world.takeAction())
    }

    @Test fun `a fresh encounter announces the monster and leaves the rest of the zone milling about`() {
        val world = WorldSimulation()
        val roster = listOf(mob("a", "А"), mob("b", "Б"), mob("c", "В"), mob("d", "Г"), mob("e", "Д"))
        world.observe(battle(monster = mob("a", "А")), roster)
        world.run(240)
        assertEquals(listOf("a", "b", "c", "d", "e"), world.mobs.map { it.id })
        assertEquals(1, world.mobs.count { !it.ambient })
        assertEquals("А", world.banner?.text)
    }

    @Test fun `a boss encounter is announced as one`() {
        val world = WorldSimulation()
        world.observe(battle(monster = mob("king", "Король", boss = true, element = "fire")))
        world.run(240)
        assertEquals(true, world.banner?.boss)
        assertEquals("fire", world.engaged?.element)
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
        val flask = worldScript(battleDelta(before, quaffed, BattleAction.POTION), "hero", "zombie", "Зомби",
            false, "physical", WorldArt())
        assertEquals(40.0, flask.single { it.kind == WorldBeatKind.QUAFF }.amount)
        assertTrue(flask.none { it.kind == WorldBeatKind.REGEN })
        val heavy = worldScript(battleDelta(before, battle(turn = 2, heroLife = 30.0, enemyLife = 20.0, mana = 12.0),
            BattleAction.POWER), "hero", "zombie", "Зомби", false, "physical", WorldArt())
        assertTrue(heavy.any { it.kind == WorldBeatKind.CAST })
        assertEquals(-8.0, heavy.single { it.kind == WorldBeatKind.MANA }.amount)
        assertEquals(heavy.map { it.at }.sorted(), heavy.map { it.at })
    }

    @Test fun `guard answers sooner and a retreat ends the script`() {
        val before = battle(turn = 1)
        val guarded = worldScript(battleDelta(before, battle(turn = 2, heroLife = 96.0, mana = 26.0), BattleAction.GUARD),
            "hero", "zombie", "Зомби", false, "physical", WorldArt())
        assertTrue(guarded.any { it.kind == WorldBeatKind.GUARD })
        assertEquals(WorldTiming.DEFENSIVE_IMPACT, guarded.single { it.kind == WorldBeatKind.IMPACT }.at)
        val fled = worldScript(battleDelta(before, battle(turn = 2, status = BattleStatus.FLED), BattleAction.FLEE),
            "hero", "zombie", "Зомби", false, "physical", WorldArt())
        assertEquals(listOf(WorldBeatKind.RETREAT, WorldBeatKind.BANNER), fled.map { it.kind })
    }

    @Test fun `world time only moves forward`() {
        assertFailsWith<IllegalArgumentException> { WorldSimulation().advance(-1L) }
    }

    @Test fun `a reset world forgets the fight it was drawing`() {
        val world = WorldSimulation()
        world.observe(battle()); world.run(400)
        world.reset()
        assertNull(world.hero); assertNull(world.engaged); assertNull(world.banner)
        assertTrue(world.mobs.isEmpty() && world.numbers.isEmpty() && world.corpses.isEmpty())
        assertTrue(world.loot.isEmpty() && world.collected.isEmpty())
        assertFalse(world.playing)
        // The same battle is a fresh encounter again, not a replay that has already been seen.
        assertTrue(world.observe(battle()))
    }

    @Test fun `the next encounter of a zone keeps the ground and the hero where they stood`() {
        val world = WorldSimulation()
        world.observe(battle()); world.run(400)
        world.steer(Vec2(1f, 0f)); world.run(1_200); world.steer(Vec2.ZERO)
        val stood = requireNotNull(world.hero).position
        assertTrue(world.observe(battle(id = "battle-2", turn = 0)))
        assertEquals(stood, requireNotNull(world.hero).position)
        assertEquals(1, world.mobs.size)
    }

    @Test fun `the projection and its inverse agree, and a swipe up walks up the screen`() {
        val camera = IsoCamera(Vec2(13f, 13f), Vec2(1080f, 1920f))
        listOf(Vec2(13f, 13f), Vec2(0f, 0f), Vec2(25.5f, 3.25f)).forEach { point ->
            val round = camera.toWorld(camera.toScreen(point))
            assertTrue(abs(round.x - point.x) < .01f && abs(round.y - point.y) < .01f, "$round")
        }
        // Up the screen is up-left and up-right in equal measure; right is south-east minus south-west.
        val up = camera.toGround(Vec2(0f, -100f))
        assertTrue(up.x < 0f && up.y < 0f && abs(up.x - up.y) < .01f, "$up")
        val right = camera.toGround(Vec2(100f, 0f))
        assertTrue(right.x > 0f && right.y < 0f, "$right")
        assertTrue(camera.toScreen(up).y < camera.toScreen(Vec2.ZERO).y)
    }

    @Test fun `a zone always lays its ground out the same way`() {
        val first = Battlefield.of("coast")
        val second = Battlefield.of("coast")
        assertEquals(first.props.map { it.position }, second.props.map { it.position })
        assertNotEquals(first.props.map { it.position }, Battlefield.of("crypt").props.map { it.position })
        // The middle is swept: the hero is put down there and must not spawn inside a stone.
        assertTrue(first.props.none { it.position.distanceTo(first.centre) <= Battlefield.CLEARING })
    }
}
