package com.sperance.exileforge.core

import com.sperance.exileforge.core.model.combat.*
import com.sperance.exileforge.core.model.combat.world.*
import org.junit.Test
import kotlin.math.abs
import kotlin.test.*

/**
 * The world is a renderer with legs, and these tests are what stops it becoming a second combat engine.
 *
 * Each one pins the same rule from a different side: a number on the map is a difference between two
 * server snapshots, and walking decides only *when* the client asks — never what the answer is worth.
 */
class WorldContractTest {
    private fun mob(id: String = "zombie", name: String = "Зомби", life: Double = 40.0,
        boss: Boolean = false, element: String = "physical") =
        Monster(id, name, life, 5.0, lootTableId = "common", experience = 10, gold = 3, element = element, boss = boss)

    private fun battle(turn: Int = 0, heroLife: Double = 100.0, enemyLife: Double = 40.0, mana: Double = 20.0,
        shield: Double = 0.0, potions: Int = 2, status: BattleStatus = BattleStatus.ACTIVE,
        log: List<String> = emptyList(), rewards: List<BattleReward> = emptyList(), monster: Monster = mob(),
        id: String = "battle-1", zone: String = "coast") =
        Battle(id, zone, 1, monster,
            Combatant("Герой", 100.0, heroLife, 20.0, mana, shield),
            Combatant(monster.name, 40.0, enemyLife),
            turn, status, potions, log, rewards, LootTable("common", 1, listOf(LootEntry("NORMAL", 1))))

    private val roster = listOf(mob("zombie", "Зомби"), mob("hound", "Гончая"), mob("wraith", "Призрак"))

    /** Steps the world the way a display does, in frames rather than one long jump. */
    private fun WorldSimulation.run(millis: Long) {
        var left = millis
        while(left > 0L) { val step = minOf(16L, left); advance(step); left -= step }
    }

    /** Lets the world walk itself until it asks for something, the way the screen's pump does. */
    private fun WorldSimulation.until(limit: Long = 40_000): WorldIntent? {
        var left = limit
        while(left > 0L) {
            advance(16L); left -= 16L
            takeIntent()?.let { return it }
        }
        return null
    }

    /** Walks the exile onto the mob the server put in the battle, the way a thumb would. */
    private fun WorldSimulation.closeIn(limit: Long = 30_000) {
        mode = WorldMode.AUTO_RUN
        var left = limit
        while(left > 0L && !inStrikeRange) { advance(16L); left -= 16L }
        mode = WorldMode.AUTO_STRIKE
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
        val dropped = world.loot + world.collected
        assertEquals(setOf(LootKind.EXPERIENCE, LootKind.GOLD, LootKind.EQUIPMENT, LootKind.CURRENCY),
            dropped.map { it.kind }.toSet())
        assertEquals(victory.rewards.map { it.name }.toSet(), dropped.map { it.title }.toSet())
        // The drop falls by the corpse even though the body itself has already finished dying.
        val corpse = world.corpses.single().position
        assertTrue(dropped.all { it.position.distanceTo(corpse) < 3f }, "${dropped.map { it.position }}")
    }

    @Test fun `the exile collects what it walked over and never collects it twice`() {
        val world = WorldSimulation()
        world.observe(battle()); world.run(400)
        world.observe(battle(turn = 2, enemyLife = 0.0, status = BattleStatus.VICTORY,
            rewards = listOf(BattleReward("Опыт", 12), BattleReward("Ржавый меч", 1, equipmentUuid = "uuid-1"))),
            action = BattleAction.ATTACK)
        world.run(WorldTiming.AFTERMATH + 2 * WorldTiming.DROP_STRIDE + 80)
        assertEquals(2, world.loot.size + world.collected.size)
        // Nothing to fight and nothing to summon, so the errand left is the reward on the floor.
        world.mode = WorldMode.AUTO_RUN
        world.rules = WorldRules(engage = false, boss = false)
        world.run(30_000)
        assertTrue(world.loot.isEmpty())
        assertEquals(setOf("Опыт", "Ржавый меч"), world.collected.map { it.title }.toSet())
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
        val from = hero.position
        world.steer(Vec2(1f, 1f).direction)
        world.run(4_000)
        assertEquals(61.0, hero.life); assertEquals(100.0, hero.maxLife); assertEquals(12.0, hero.shield)
        assertEquals(33.0, enemy.life)
        assertTrue(hero.position.distanceTo(from) > 1f, "${hero.position}")
    }

    @Test fun `the exile never walks into the rock a map is cut from`() {
        MapKind.entries.forEach { kind ->
            val zone = kind.name.lowercase()
            val world = WorldSimulation()
            world.observe(battle(zone = zone), roster)
            val hero = requireNotNull(world.hero)
            listOf(Vec2(1f, 1f), Vec2(-1f, -1f), Vec2(1f, -1f), Vec2(-1f, 1f), Vec2(0f, 1f), Vec2(1f, 0f))
                .forEach { heading ->
                    world.steer(heading.direction)
                    world.run(6_000)
                    assertFalse(world.map.blocked(hero.position, hero.radius), "$zone ${hero.position}")
                    assertTrue(hero.position.x in 0f..world.map.width.toFloat())
                    assertTrue(hero.position.y in 0f..world.map.height.toFloat())
                }
        }
    }

    @Test fun `the mob the server chose finds its way to the exile`() {
        val world = WorldSimulation()
        world.observe(battle(zone = "crypt"))
        val hero = requireNotNull(world.hero)
        val mob = requireNotNull(world.engaged)
        assertTrue(mob.position.distanceTo(hero.position) > WorldSimulation.STRIKE_RANGE)
        world.run(30_000)
        assertTrue(mob.position.distanceTo(hero.position) <= WorldSimulation.MOB_REACH + .4f,
            "${mob.position.distanceTo(hero.position)}")
    }

    @Test fun `nothing is asked for out of reach and a swing is asked for within it`() {
        val world = WorldSimulation()
        world.observe(battle())
        world.run(200)
        assertFalse(world.inStrikeRange)
        assertNull(world.takeIntent())
        world.closeIn()
        assertTrue(world.inStrikeRange)
        assertEquals(WorldIntent.Strike(BattleAction.ATTACK), world.takeIntent())
        // The cadence holds the next ask back rather than letting a frame loop spam the server.
        assertNull(world.takeIntent())
        world.run(WorldSimulation.CADENCE + 32)
        assertEquals(WorldIntent.Strike(BattleAction.ATTACK), world.takeIntent())
    }

    @Test fun `a turn still playing out holds the next ask back`() {
        val world = WorldSimulation()
        world.observe(battle()); world.closeIn()
        assertEquals(WorldIntent.Strike(BattleAction.ATTACK), world.takeIntent())
        world.observe(battle(turn = 1, enemyLife = 28.0), action = BattleAction.ATTACK)
        // The cadence has elapsed, but the swing it answered is still being drawn.
        world.run(WorldSimulation.CADENCE + 32)
        assertTrue(world.playing)
        assertNull(world.takeIntent())
        world.run(WorldTiming.AFTERMATH + 64)
        assertFalse(world.playing)
        assertEquals(WorldIntent.Strike(BattleAction.ATTACK), world.takeIntent())
    }

    @Test fun `driving by hand asks the server for nothing at all`() {
        val world = WorldSimulation()
        world.observe(battle()); world.closeIn()
        world.mode = WorldMode.MANUAL
        world.rules = WorldRules(engage = true, boss = true)
        assertNull(world.takeIntent())
        world.mode = WorldMode.AUTO_STRIKE
        assertEquals(WorldIntent.Strike(BattleAction.ATTACK), world.takeIntent())
    }

    @Test fun `a zone walks a whole pack, not one monster at a time`() {
        val world = WorldSimulation()
        world.observe(battle(), roster)
        world.run(240)
        assertEquals(WorldSimulation.PACK_SIZE, world.roaming)
        assertEquals(1, world.mobs.count { it.engaged })
        // Every body of the pack stands somewhere it could have walked to.
        world.mobs.forEach { assertFalse(world.map.blocked(it.position, it.radius), "${it.id} ${it.position}") }
    }

    @Test fun `the server's monster is bound to the nearest body of its kind`() {
        val world = WorldSimulation()
        world.observe(battle(), listOf(mob("hound", "Гончая"), mob("hound", "Гончая"), mob("zombie", "Зомби")))
        world.run(240)
        val engaged = requireNotNull(world.engaged)
        assertEquals("zombie", engaged.id)
        val hero = requireNotNull(world.hero).position
        val zombies = world.mobs.filter { it.id == "zombie" }
        assertEquals(engaged, zombies.minByOrNull { it.position.distanceTo(hero) })
    }

    @Test fun `walking into a roaming monster opens a battle with the zone`() {
        val world = WorldSimulation()
        world.observe(battle(turn = 3, enemyLife = 0.0, status = BattleStatus.VICTORY), roster)
        world.run(2_000)
        world.mode = WorldMode.AUTO_RUN
        world.rules = WorldRules(engage = true, boss = false)
        assertEquals(WorldIntent.Engage(boss = false), world.until())
    }

    @Test fun `standing on the summoning circle calls the boss once the zone opens it`() {
        val world = WorldSimulation()
        world.observe(battle(turn = 3, enemyLife = 0.0, status = BattleStatus.VICTORY), roster)
        world.run(2_000)
        world.mode = WorldMode.AUTO_RUN
        // The zone is cleared: the errand left is the altar, and the exile walks to it on its own.
        world.rules = WorldRules(engage = false, boss = true)
        assertEquals(WorldIntent.Engage(boss = true), world.until())
        assertTrue(world.onAltar)
    }

    @Test fun `a cleared zone is never summoned from before the server opens the boss`() {
        val world = WorldSimulation()
        world.observe(battle(turn = 3, enemyLife = 0.0, status = BattleStatus.VICTORY), roster)
        world.mode = WorldMode.AUTO_RUN
        world.rules = WorldRules(engage = false, boss = false)
        assertNull(world.until(6_000))
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

    @Test fun `a fresh encounter announces the monster`() {
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
        world.observe(battle(), roster); world.run(400)
        world.reset()
        assertNull(world.hero); assertNull(world.engaged); assertNull(world.banner)
        assertTrue(world.mobs.isEmpty() && world.numbers.isEmpty() && world.corpses.isEmpty())
        assertTrue(world.loot.isEmpty() && world.collected.isEmpty())
        assertFalse(world.playing)
        // The same battle is a fresh encounter again, not a replay that has already been seen.
        assertTrue(world.observe(battle()))
    }

    @Test fun `the next encounter of a zone keeps the map and the exile where they stood`() {
        val world = WorldSimulation()
        world.observe(battle(), roster); world.run(400)
        world.steer(Vec2(1f, 0f)); world.run(1_200); world.steer(Vec2.ZERO)
        val stood = requireNotNull(world.hero).position
        val ground = world.map
        assertTrue(world.observe(battle(id = "battle-2", turn = 0), roster))
        assertEquals(stood, requireNotNull(world.hero).position)
        assertSame(ground, world.map)
        assertEquals(WorldSimulation.PACK_SIZE, world.roaming)
    }

    @Test fun `another zone is another map`() {
        val world = WorldSimulation()
        world.observe(battle(zone = "coast"), roster)
        val coast = world.map
        assertTrue(world.observe(battle(id = "battle-9", zone = "crypt"), roster))
        assertNotSame(coast, world.map)
        assertEquals(MapKind.VAULT, world.map.kind)
    }

    @Test fun `the three zones get three different maps and always the same one`() {
        assertEquals(MapKind.SHORE, Battlefield.kindOf("coast"))
        assertEquals(MapKind.VAULT, Battlefield.kindOf("crypt"))
        assertEquals(MapKind.BASTION, Battlefield.kindOf("citadel"))
        listOf("coast", "crypt", "citadel").forEach { zone ->
            val first = Battlefield.of(zone)
            val second = Battlefield.of(zone)
            assertEquals(first.kind, second.kind)
            assertEquals(first.spawn, second.spawn)
            assertEquals(first.altar, second.altar)
            assertEquals(first.camps, second.camps)
            assertEquals(tiles(first), tiles(second), zone)
        }
        assertNotEquals(tiles(Battlefield.of("coast")), tiles(Battlefield.of("crypt")))
    }

    @Test fun `every map opens a way from the spawn to its camps, its altar and its scenery`() {
        listOf("coast", "crypt", "citadel").forEach { zone ->
            val map = Battlefield.of(zone)
            assertFalse(map.blocked(map.spawn, Battlefield.CLEARANCE), zone)
            assertTrue(map.camps.isNotEmpty(), zone)
            (map.camps + map.altar + map.decor.map { it.position }).forEach { spot ->
                assertFalse(map.blocked(spot, Battlefield.CLEARANCE), "$zone $spot")
                assertTrue(map.path(map.spawn, spot, Battlefield.CLEARANCE).isNotEmpty(), "$zone $spot")
            }
            // The altar is a walk away rather than underfoot, and the map is mostly somewhere to go.
            assertTrue(map.altar.distanceTo(map.spawn) > 6f, zone)
            assertTrue(map.reachable(map.spawn, Battlefield.CLEARANCE).size > 80, zone)
        }
    }

    @Test fun `a route goes round a wall instead of into it`() {
        val map = Battlefield.of("crypt")
        val walk = map.path(map.spawn, map.altar, Battlefield.CLEARANCE)
        assertTrue(walk.isNotEmpty())
        walk.forEach { assertFalse(map.blocked(it, Battlefield.CLEARANCE), "$it") }
        // A crypt is rooms and corridors, so the way there is longer than the line to it.
        assertTrue(walk.size.toFloat() >= map.spawn.distanceTo(map.altar))
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

    private fun tiles(map: Battlefield) =
        (0 until map.height).joinToString("\n") { y -> (0 until map.width).map { x -> map.terrain(x, y).ordinal }.joinToString("") }
}
