package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.model.campaign.*
import kotlin.math.hypot
import kotlin.random.Random
import kotlin.test.*

class CampaignTest {
    private val drowned = CampaignMonster("DROWNED", "HUMANOID", mapOf("STOCK_HEALTH" to 18.0, "STOCK_ATTACK_PHYSICAL" to 4.0, "STOCK_ATTACK_SPEED" to 1.0))
    private val map = CampaignMap("C1_TIDAL_SHORE", "CHAPTER_1", 1, "SHORE", 1, listOf(10, 14), listOf(drowned),
        listOf(MonsterModifier("MOB_TOUGH", 100, 1, listOf(MonsterEffect("STOCK_HEALTH", "INCREASED", 60.0))),
            MonsterModifier("MOB_STRONG", 100, 1, listOf(MonsterEffect("STOCK_ATTACK_PHYSICAL", "INCREASED", 40.0))),
            MonsterModifier("MOB_DEEP", 100, 9, listOf(MonsterEffect("STOCK_ARMOR", "ADD", 25.0)))))
    private val rarities = listOf(CampaignRarity("NORMAL", 0), CampaignRarity("MAGIC", 0), CampaignRarity("RARE", 1, listOf(2, 2),
        listOf(MonsterEffect("STOCK_HEALTH", "MORE", 100.0))))

    @Test fun `a monster folds its effects with the server's formula`() {
        val stats = MonsterRoller.fold(drowned, listOf(
            MonsterEffect("STOCK_HEALTH", "ADD", 2.0), MonsterEffect("STOCK_HEALTH", "INCREASED", 50.0),
            MonsterEffect("STOCK_HEALTH", "MORE", 100.0), MonsterEffect("STOCK_ARMOR", "ADD", 10.0)))
        assertEquals((18.0 + 2) * 1.5 * 2, stats.getValue("STOCK_HEALTH"))
        assertEquals(10.0, stats.getValue("STOCK_ARMOR"))
        assertEquals(4.0, stats.getValue("STOCK_ATTACK_PHYSICAL"))
    }

    @Test fun `a rare rolls its count of distinct modifiers the map allows`() {
        repeat(20) { seed ->
            val monster = MonsterRoller.roll(map, rarities, Random(seed))
            assertEquals(MonsterRarity.RARE, monster.rarity)
            assertEquals(2, monster.modifiers.size)
            assertEquals(2, monster.modifiers.map { it.code }.toSet().size)
            // A modifier above the map's level never lands on it.
            assertTrue(monster.modifiers.none { it.code == "MOB_DEEP" })
            assertTrue(monster.stats.getValue("STOCK_HEALTH") >= 36.0)
        }
    }

    @Test fun `a strong hero wins, a weak one falls, and the same seed is the same fight`() {
        val hero = Combatant(mapOf("STOCK_HEALTH" to 60.0, "STOCK_ATTACK_PHYSICAL" to 9.0, "STOCK_ATTACK_SPEED" to 1.4), 1)
        val monster = Combatant(drowned.stats, 1)
        val win = Combat.fight(hero, monster, hero.maxLife, Random(3))
        assertEquals(Outcome.WIN, win.outcome)
        assertTrue(win.heroLife in 1.0..60.0)
        assertEquals(win, Combat.fight(hero, monster, hero.maxLife, Random(3)))
        val brute = Combatant(mapOf("STOCK_HEALTH" to 400.0, "STOCK_ATTACK_PHYSICAL" to 30.0), 5)
        assertEquals(Outcome.LOSS, Combat.fight(hero, brute, hero.maxLife, Random(3)).outcome)
    }

    @Test fun `a hero with no weapon still swings`() {
        val bare = Combatant(mapOf("STOCK_HEALTH" to 50.0), 1)
        assertEquals(Combatant.UNARMED_SPEED, bare.attackSpeed)
        assertEquals(Combatant.UNARMED_DAMAGE, bare.damage.getValue(DamageType.PHYSICAL))
        assertEquals(0.05, bare.critChance)
    }

    @Test fun `chaos goes around energy shield and leech heals the striker`() {
        val warded = Combatant(mapOf("STOCK_HEALTH" to 1000.0, "STOCK_ENERGY_SHIELD" to 1000.0, "STOCK_ATTACK_PHYSICAL" to 0.1, "STOCK_ATTACK_SPEED" to 0.3), 1)
        val poisoner = Combatant(mapOf("STOCK_HEALTH" to 100.0, "STOCK_ATTACK_CHAOS" to 20.0, "STOCK_ATTACK_SPEED" to 2.0, "STOCK_CRITICAL_CHANCE" to 0.0), 1)
        val first = Combat.fight(poisoner, warded, 50.0, Random(1)).events.first { it.attacker == Side.HERO && it.damage > 0 }
        assertEquals(1000.0, first.monsterShield)
        assertTrue(first.monsterLife < 1000.0)
        val vampire = Combatant(mapOf("STOCK_HEALTH" to 100.0, "STOCK_ATTACK_PHYSICAL" to 20.0, "STOCK_LEECH_ALL" to 50.0, "STOCK_CRITICAL_CHANCE" to 0.0), 1)
        val hit = Combat.fight(vampire, warded, 50.0, Random(1)).events.first { it.attacker == Side.HERO && it.damage > 0 }
        assertTrue(hit.healed > 0)
        assertTrue(hit.heroLife > 50.0)
    }

    @Test fun `a map is one reachable region with the exit far from the start`() {
        listOf("SHORE", "CRYPT").forEach { biome ->
            val layout = MapGenerator.generate(42, biome, 12)
            val cells = (0 until layout.height).flatMap { y -> (0 until layout.width).map { x -> Cell(x, y) } }.filter { layout.walkable(it.x, it.y) }
            val tiles = Array(layout.width * layout.height) { i -> if (layout.walkable(i % layout.width, i / layout.width)) Tile.FLOOR else Tile.WALL }
            val reach = MapGenerator.distances(tiles, layout.width, layout.start)
            assertTrue(cells.all { reach[it.y * layout.width + it.x] >= 0 }, "$biome: sealed ground")
            assertTrue(reach[layout.exit.y * layout.width + layout.exit.x] >= 20, "$biome: the exit is too close")
            assertEquals(12, layout.spawns.size, "$biome: spawns")
            assertTrue(layout.spawns.all { layout.walkable(it.x, it.y) && reach[it.y * layout.width + it.x] >= 8 })
            assertEquals(layout.floor, MapGenerator.generate(42, biome, 12).floor, "$biome: the same seed carves the same map")
        }
        assertNotEquals(MapGenerator.generate(1, "SHORE", 12).exit, MapGenerator.generate(2, "SHORE", 12).exit)
    }

    @Test fun `up on the screen is toward the top corner`() {
        val (x, y) = ExpeditionWorld.screenToWorld(0.0, -1.0)
        assertTrue(x < 0 && y < 0)
        assertEquals(1.0, hypot(x, y), 1e-9)
        assertEquals(0.0 to 0.0, ExpeditionWorld.screenToWorld(0.0, 0.0))
    }

    @Test fun `walking into a monster starts a fight and the hero never enters rock`() {
        val world = ExpeditionWorld.create(map, rarities, emptyMap(), 5)
        val agent = world.agents.first()
        var event: WorldEvent? = null
        repeat(4000) {
            if (event != null) return@repeat
            val dx = agent.x - world.heroX
            val dy = agent.y - world.heroY
            val length = hypot(dx, dy)
            event = world.step(0.016, dx / length, dy / length)
            assertTrue(world.map.walkable(world.heroX.toInt(), world.heroY.toInt()))
            // A wall in the way: the monster comes the rest of the way itself once the hero is close.
            if (event == null && length > ExpeditionWorld.AGGRO) { world.heroX = agent.x - 1.0.coerceAtMost(length); world.heroY = agent.y }
            if (!world.map.walkable(world.heroX.toInt(), world.heroY.toInt())) { world.heroX = agent.x; world.heroY = agent.y }
        }
        assertIs<WorldEvent.Encounter>(event)
        world.retreatFrom(agent)
        assertNull(world.step(0.016, 0.0, 0.0).takeIf { it is WorldEvent.Encounter && it.agent == agent })
    }

    @Test fun `standing on the exit ends the map`() {
        val world = ExpeditionWorld.create(map, rarities, emptyMap(), 9)
        world.agents.forEach { it.alive = false }
        world.heroX = world.map.exit.x + 0.5
        world.heroY = world.map.exit.y + 0.5
        assertEquals(WorldEvent.Exit, world.step(0.016, 0.0, 0.0))
    }

    @Test fun `a won fight waits for its loot, and the run goes on after it`() {
        val killed = mutableListOf<String>()
        val run = ExpeditionRun.start(map, rarities, mapOf("STOCK_HEALTH" to 500.0, "STOCK_ATTACK_PHYSICAL" to 60.0, "STOCK_ATTACK_SPEED" to 2.0), 10, 7,
            onKill = { killed += it.code }, onCleared = {})
        val agent = run.world.agents.first()
        run.world.heroX = agent.x
        run.world.heroY = agent.y
        run.update(0.016)
        assertEquals(RunPhase.FIGHT, run.hud.value.phase)
        repeat(3000) { if (run.hud.value.phase == RunPhase.FIGHT) run.update(0.05) }
        assertEquals(RunPhase.LOOT, run.hud.value.phase)
        assertEquals(listOf("DROWNED"), killed)
        assertTrue(run.hud.value.rewardPending)
        // Nothing moves on while the server has not answered.
        run.send(RunCommand.Continue); run.update(0.016)
        assertEquals(RunPhase.LOOT, run.hud.value.phase)
        run.send(RunCommand.Reward(CampaignReward(experience = 20.0, gold = 5)))
        run.send(RunCommand.Continue); run.update(0.016)
        assertEquals(RunPhase.MAP, run.hud.value.phase)
        assertEquals(5L, run.hud.value.gold)
        assertEquals(run.world.agents.size - 1, run.hud.value.alive)
        run.send(RunCommand.Leave); run.update(0.016)
        assertEquals(RunPhase.LEFT, run.hud.value.phase)
    }

    @Test fun `a lost fight ends the run`() {
        val run = ExpeditionRun.start(map, rarities, mapOf("STOCK_HEALTH" to 1.0, "STOCK_ATTACK_PHYSICAL" to 0.1), 1, 3, onKill = {}, onCleared = {})
        val agent = run.world.agents.first()
        run.world.heroX = agent.x
        run.world.heroY = agent.y
        repeat(3000) { if (run.hud.value.phase != RunPhase.DEAD) run.update(0.05) }
        assertEquals(RunPhase.DEAD, run.hud.value.phase)
        assertEquals(0, run.hud.value.heroLife)
        run.send(RunCommand.Continue); run.update(0.016)
        assertEquals(RunPhase.LEFT, run.hud.value.phase)
    }
}
