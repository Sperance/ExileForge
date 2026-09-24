package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.model.campaign.*
import kotlin.math.hypot
import kotlin.random.Random
import kotlin.test.*

class CampaignTest {
    private val drowned = CampaignMonster("DROWNED", "HUMANOID", mapOf("STOCK_HEALTH" to 18.0, "STOCK_ATTACK_PHYSICAL" to 4.0, "STOCK_ATTACK_SPEED" to 1.0))
    private val map = CampaignMap("C1_TIDAL_SHORE", "CHAPTER_1", 1, "SHORE", 1, listOf(10, 14), listOf(drowned),
        listOf(MonsterModifier("MOB_TOUGH", 100, 1, effects = listOf(MonsterEffect("STOCK_HEALTH", "INCREASED", 60.0))),
            MonsterModifier("MOB_STRONG", 100, 1, effects = listOf(MonsterEffect("STOCK_ATTACK_PHYSICAL", "INCREASED", 40.0))),
            MonsterModifier("MOB_DEEP", 100, 9, effects = listOf(MonsterEffect("STOCK_ARMOR", "ADD", 25.0)))))
    private val rarities = listOf(CampaignRarity("NORMAL", 0), CampaignRarity("MAGIC", 0), CampaignRarity("RARE", 1, listOf(2, 2),
        effects = listOf(MonsterEffect("STOCK_HEALTH", "MORE", 100.0))))

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

    @Test fun `a hero with no weapon still swings, by the server's unarmed rule`() {
        val rules = CombatRules(unarmed = UnarmedRule(damage = 6.0, speed = 1.5), critical = CriticalRule(chance = 7.0, multiplier = 150.0))
        val bare = Combatant(mapOf("STOCK_HEALTH" to 50.0), 1, rules)
        assertEquals(1.5, bare.attackSpeed)
        assertEquals(6.0, bare.damage.getValue(DamageType.PHYSICAL))
        assertEquals(0.07, bare.critChance)
        // No spell damage, no mana: nothing to cast. A hero with mana knows the innate spell.
        assertFalse(bare.casts)
        val exile = Combatant(mapOf("STOCK_HEALTH" to 50.0, "STOCK_MANA" to 40.0), 3, rules, innateSpell = true)
        assertTrue(exile.casts)
        assertEquals(rules.spell.innateDamage + rules.spell.innatePerLevel * 2, exile.spellDamage)
        assertEquals(rules.spell.castSpeed, exile.castSpeed)
    }

    @Test fun `chaos goes around energy shield and leech heals the striker`() {
        val warded = Combatant(mapOf("STOCK_HEALTH" to 1000.0, "STOCK_ENERGY_SHIELD" to 1000.0, "STOCK_ATTACK_PHYSICAL" to 0.1, "STOCK_ATTACK_SPEED" to 0.3), 1)
        val poisoner = Combatant(mapOf("STOCK_HEALTH" to 100.0, "STOCK_ATTACK_CHAOS" to 20.0, "STOCK_ATTACK_SPEED" to 2.0, "STOCK_CRITICAL_CHANCE" to 0.0), 1)
        val first = Combat.fight(poisoner, warded, 50.0, Random(1)).events.first { it.actor == Side.HERO && it.damage > 0 }
        assertEquals(1000.0, first.monsterShield)
        assertTrue(first.monsterLife < 1000.0)
        val vampire = Combatant(mapOf("STOCK_HEALTH" to 100.0, "STOCK_ATTACK_PHYSICAL" to 20.0, "STOCK_LEECH_ALL" to 50.0, "STOCK_CRITICAL_CHANCE" to 0.0), 1)
        val hit = Combat.fight(vampire, warded, 50.0, Random(1)).events.first { it.actor == Side.HERO && it.damage > 0 }
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

    @Test fun `touching a monster starts a fight, and one the hero ran from lets them go`() {
        val world = ExpeditionWorld.create(map, rarities, emptyMap(), 5)
        val agent = world.agents.first()
        world.heroX = agent.x + 0.4
        world.heroY = agent.y
        assertEquals(WorldEvent.Encounter(agent), world.step(0.016, 0.0, 0.0))
        world.retreatFrom(agent)
        assertNull(world.step(0.016, 0.0, 0.0).takeIf { it is WorldEvent.Encounter && it.agent == agent })
        assertFalse(agent.chasing, "a monster the hero ran from goes home")
    }

    /** A small map drawn by hand: `#` rock, `.` floor, `M` the monster, `H` the hero, `E` the exit. */
    private fun sketch(vararg rows: String, rule: BehaviourRule = BehaviourRule(), light: Double = 5.0): ExpeditionWorld {
        val height = rows.size
        val width = rows.first().length
        fun find(c: Char) = rows.withIndex().firstNotNullOf { (y, row) -> row.indexOf(c).takeIf { it >= 0 }?.let { Cell(it, y) } }
        val tiles = Array(width * height) { i -> if (rows[i / width][i % width] == '#') Tile.WALL else Tile.FLOOR }
        val layout = ExpeditionMap(width, height, tiles, IntArray(width * height), find('H'), find('E'), listOf(find('M')))
        val monster = RolledMonster("DROWNED", "HUMANOID", MonsterRarity.NORMAL, emptyList(), drowned.stats, rule)
        return ExpeditionWorld(layout, listOf(monster), 3.2, 1, light)
    }

    @Test fun `a chase goes round the rock instead of into it`() {
        val world = sketch(
            "#########",
            "#M......#",
            "#######.#",
            "#.......#",
            "#H....E.#",
            "#########", rule = BehaviourRule(sight = 20.0, giveUp = 30.0))
        val route = world.path(Cell(1, 1), Cell(1, 4))
        assertEquals(Cell(1, 1), route.first())
        assertEquals(Cell(1, 4), route.last())
        assertTrue(route.all { world.map.walkable(it.x, it.y) })
        assertTrue(route.any { it.x == 7 && it.y == 2 }, "the only way down is the gap")
        // The monster is told where the hero is and has to walk the long way there.
        val agent = world.agents.first()
        agent.mode = AgentMode.HUNTING
        agent.lastX = world.heroX
        agent.lastY = world.heroY
        var event: WorldEvent? = null
        repeat(2000) { if (event == null) event = world.step(0.016, 0.0, 0.0) }
        assertEquals(WorldEvent.Encounter(agent), event)
    }

    @Test fun `a monster behind rock does not see the hero, and an ambusher waits until they are close`() {
        val hidden = sketch(
            "#######",
            "#M....#",
            "#######",
            "#H...E#",
            "#######", rule = BehaviourRule(sight = 10.0))
        repeat(60) { hidden.step(0.016, 0.0, 0.0) }
        assertFalse(hidden.agents.first().chasing, "rock stands between them")

        val ambush = sketch(
            "#########",
            "#M......#",
            "#......H#",
            "#.....E.#",
            "#########", rule = BehaviourRule(type = BehaviourRule.AMBUSH, sight = 10.0, wake = 2.0))
        val lurker = ambush.agents.first()
        repeat(60) { ambush.step(0.016, 0.0, 0.0) }
        assertEquals(AgentMode.LURKING, lurker.mode)
        assertEquals(1.5, lurker.x, 1e-9)
        ambush.heroX = lurker.x + 1.5
        ambush.heroY = lurker.y
        ambush.step(0.016, 0.0, 0.0)
        assertTrue(lurker.chasing)
    }

    @Test fun `the hero sees their light radius through open ground and remembers what they saw`() {
        val world = sketch(
            "############",
            "#H.........#",
            "#.##########",
            "#M........E#",
            "############", light = 3.0)
        assertTrue(world.lit(2, 1) && world.lit(4, 1))
        assertFalse(world.lit(6, 1), "beyond the light")
        assertTrue(world.lit(2, 2), "a rock face in reach is seen")
        assertFalse(world.lit(3, 3), "behind the rock is not")
        world.heroX = 5.5
        world.step(0.016, 0.0, 0.0)
        assertTrue(world.lit(8, 1))
        assertFalse(world.lit(1, 1))
        assertTrue(world.explored(1, 1), "what was seen stays on the map")
        assertEquals(5.0 * 0.7, ExpeditionWorld.lightRadius(emptyMap(), 0.7), 1e-9)
        assertEquals(8.0 * 1.2, ExpeditionWorld.lightRadius(mapOf("STOCK_LIGHT_RADIUS" to 8.0), 1.2), 1e-9)
    }

    @Test fun `chests stand where the seed says, as many as the server says, and open once`() {
        val world = ExpeditionWorld.create(map, rarities, emptyMap(), 11)
        world.placeChests(2)
        assertEquals(2, world.chests.size)
        world.placeChests(5)
        assertEquals(2, world.chests.size, "chests are placed once")
        val again = ExpeditionWorld.create(map, rarities, emptyMap(), 11).also { it.placeChests(2) }
        assertEquals(world.chests.map { it.cell }, again.chests.map { it.cell })
        world.chests.forEach { chest ->
            assertTrue(world.map.walkable(chest.cell.x, chest.cell.y))
            assertTrue(chest.cell != world.map.exit && chest.cell !in world.map.spawns)
        }
        world.agents.forEach { it.alive = false }
        val chest = world.chests.first()
        world.heroX = chest.cell.x + 0.5
        world.heroY = chest.cell.y + 0.5
        assertEquals(WorldEvent.Opened(chest), world.step(0.016, 0.0, 0.0))
        assertNull(world.step(0.016, 0.0, 0.0))
    }

    @Test fun `a run places the server's chests and waits for what one brought`() {
        var opened = 0
        val run = ExpeditionRun.start(map, rarities, emptyMap(), 1, 11, onKill = {}, onCleared = {}, onChest = { opened++ })
        run.world.agents.forEach { it.alive = false }
        run.send(RunCommand.Chests(1))
        run.update(0.016)
        assertEquals(1, run.hud.value.chestsLeft)
        val chest = run.world.chests.single()
        run.world.heroX = chest.cell.x + 0.5
        run.world.heroY = chest.cell.y + 0.5
        run.update(0.016)
        assertEquals(1, opened)
        assertTrue(run.hud.value.chestPending)
        assertEquals(0, run.hud.value.chestsLeft)
        run.send(RunCommand.ChestReward(CampaignReward(gold = 40)))
        run.update(0.016)
        assertEquals(40L, run.hud.value.chest?.gold)
        assertEquals(RunPhase.MAP, run.hud.value.phase)
        run.send(RunCommand.DismissChest)
        run.update(0.016)
        assertNull(run.hud.value.chest)
    }

    @Test fun `a boss guards the exit, rolls nothing and seals it until it falls`() {
        val guardian = CampaignBoss("BOSS_TIDECALLER", "HUMANOID", mapOf("STOCK_HEALTH" to 40.0, "STOCK_ATTACK_PHYSICAL" to 6.0),
            BehaviourRule(type = BehaviourRule.AMBUSH, wake = 3.0), listOf(MonsterModifier("MOB_TOUGH", 100, effects = listOf(MonsterEffect("STOCK_HEALTH", "INCREASED", 60.0)))))
        val withBoss = map.copy(boss = guardian)
        val unique = CampaignRarity("UNIQUE", 0, modifierPower = 2.0, effects = listOf(MonsterEffect("STOCK_HEALTH", "MORE", 250.0)))
        val rolled = assertNotNull(MonsterRoller.boss(withBoss, rarities + unique))
        assertEquals(MonsterRarity.UNIQUE, rolled.rarity)
        assertEquals(120.0, rolled.modifiers.single().effects.single().value, "the tier's power doubles the fixed modifier")
        assertEquals(40.0 * 2.2 * 3.5, rolled.stats.getValue("STOCK_HEALTH"), 1e-9)
        assertNull(MonsterRoller.boss(map, rarities), "an older server sends no boss")

        val world = ExpeditionWorld.create(withBoss, rarities + unique, emptyMap(), 9)
        val boss = assertNotNull(world.boss)
        assertTrue(world.sealed)
        assertTrue(hypot(boss.x - (world.map.exit.x + 0.5), boss.y - (world.map.exit.y + 0.5)) <= 3.0)
        assertEquals(world.agents.size - 1, world.total, "the boss is the seal, not one of the monsters")
        world.agents.filter { it !== boss }.forEach { it.alive = false }
        boss.calm = 100.0
        world.heroX = world.map.exit.x + 0.5
        world.heroY = world.map.exit.y + 0.5
        assertNull(world.step(0.016, 0.0, 0.0), "the exit is sealed")
        world.bossAbsent()
        assertEquals(WorldEvent.Exit, world.step(0.016, 0.0, 0.0))
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
        // The screen after the fight has the whole log and what it came to.
        val report = assertNotNull(run.hud.value.report)
        assertEquals(Outcome.WIN, report.outcome)
        assertTrue(report.events.isNotEmpty() && report.dealt > 0)
        // Nothing moves on while the server has not answered.
        run.send(RunCommand.Continue); run.update(0.016)
        assertEquals(RunPhase.LOOT, run.hud.value.phase)
        run.send(RunCommand.Reward(CampaignReward(experience = 20.0, gold = 5)))
        run.send(RunCommand.Continue); run.update(0.016)
        assertEquals(RunPhase.MAP, run.hud.value.phase)
        assertEquals(5L, run.hud.value.gold)
        assertNull(run.hud.value.report)
        assertEquals(run.world.agents.size - 1, run.hud.value.alive)
        run.send(RunCommand.Leave); run.update(0.016)
        assertEquals(RunPhase.LEFT, run.hud.value.phase)
    }

    @Test fun `a lost fight ends the run once the server has priced the death`() {
        var fallen = 0
        val run = ExpeditionRun.start(map, rarities, mapOf("STOCK_HEALTH" to 1.0, "STOCK_ATTACK_PHYSICAL" to 0.1), 1, 3, onKill = {}, onCleared = {}, onFallen = { fallen++ })
        val agent = run.world.agents.first()
        run.world.heroX = agent.x
        run.world.heroY = agent.y
        repeat(3000) { if (run.hud.value.phase != RunPhase.DEAD) run.update(0.05) }
        assertEquals(RunPhase.DEAD, run.hud.value.phase)
        assertEquals(0, run.hud.value.heroLife)
        assertEquals(1, fallen)
        assertTrue(run.hud.value.fallPending)
        // Nothing moves on while the server has not said what the death cost.
        run.send(RunCommand.Continue); run.update(0.016)
        assertEquals(RunPhase.DEAD, run.hud.value.phase)
        run.send(RunCommand.Fallen(CampaignFall(lost = 50.0, level = 1, totalExperience = 0.0)))
        run.send(RunCommand.Continue); run.update(0.016)
        assertEquals(50.0, run.hud.value.fall?.lost)
        assertEquals(RunPhase.LEFT, run.hud.value.phase)
    }

    @Test fun `nothing returns while walking, but a flask can be drunk on the map`() {
        val rules = CombatRules(flask = FlaskRule(charges = 2, perKill = 1, heal = 10.0, duration = 1.0))
        // Slow enough to take a couple of blows, so the fight leaves a mark; regeneration only counts inside it.
        val run = ExpeditionRun.start(map, rarities, mapOf("STOCK_HEALTH" to 300.0, "STOCK_MANA" to 40.0, "STOCK_ATTACK_PHYSICAL" to 10.0, "STOCK_ATTACK_SPEED" to 2.0, "STOCK_CRITICAL_CHANCE" to 0.0), 10, 7,
            onKill = {}, onCleared = {}, rules = rules)
        // A fight leaves the hero hurt; walking away from it heals nothing.
        val agent = run.world.agents.first()
        run.world.heroX = agent.x; run.world.heroY = agent.y
        repeat(3000) { if (run.hud.value.phase != RunPhase.LOOT) run.update(0.05) }
        run.send(RunCommand.Reward(CampaignReward())); run.send(RunCommand.Continue); run.update(0.016)
        assertEquals(RunPhase.MAP, run.hud.value.phase)
        val hurt = run.hud.value.heroLife
        assertTrue(hurt < 300, "the fight should have cost something")
        run.world.agents.forEach { it.alive = false }
        repeat(100) { run.update(0.05) }
        assertEquals(hurt, run.hud.value.heroLife, "life came back while walking")
        // The flask works on the map too: a tenth of the life over its second, one charge spent.
        run.send(RunCommand.Flask); run.update(0.016)
        assertTrue(run.hud.value.flaskActive)
        assertEquals(1, run.hud.value.flasks)
        repeat(25) { run.update(0.05) }
        assertFalse(run.hud.value.flaskActive)
        val expected = minOf(300, hurt + 30)
        assertTrue(run.hud.value.heroLife in (expected - 2)..expected, "${run.hud.value.heroLife} after $hurt")
    }

    @Test fun `the flask starts full, is drunk in a fight and earns a charge back per kill`() {
        val rules = CombatRules(flask = FlaskRule(charges = 2, perKill = 1, heal = 40.0, duration = 3.0))
        val run = ExpeditionRun.start(map, rarities, mapOf("STOCK_HEALTH" to 500.0, "STOCK_ATTACK_PHYSICAL" to 60.0, "STOCK_ATTACK_SPEED" to 2.0), 10, 7,
            onKill = {}, onCleared = {}, rules = rules)
        assertEquals(2, run.hud.value.flasks)
        assertEquals(2, run.hud.value.maxFlasks)
        val agent = run.world.agents.first()
        run.world.heroX = agent.x
        run.world.heroY = agent.y
        run.update(0.016)
        assertEquals(RunPhase.FIGHT, run.hud.value.phase)
        run.send(RunCommand.Flask); run.update(0.016)
        assertEquals(1, run.hud.value.flasks)
        assertTrue(run.hud.value.fight!!.flaskActive)
        repeat(3000) { if (run.hud.value.phase == RunPhase.FIGHT) run.update(0.05) }
        assertEquals(RunPhase.LOOT, run.hud.value.phase)
        assertEquals(2, run.hud.value.flasks, "a kill gives a charge back, up to the rule's count")
        assertEquals(1, run.hud.value.report?.flasks)
    }

    @Test fun `a higher tier draws from a wider pool and rolls stronger values`() {
        val pool = listOf(
            MonsterModifier("MOB_TOUGH", 100, 1, "MAGIC", listOf(MonsterEffect("STOCK_HEALTH", "INCREASED", 60.0))),
            MonsterModifier("MOB_BERSERK", 100, 1, "RARE", listOf(MonsterEffect("STOCK_ATTACK_PHYSICAL", "MORE", 30.0))))
        val tiered = map.copy(modifiers = pool)
        val magic = listOf(CampaignRarity("MAGIC", 1, listOf(2, 2), modifierPower = 1.5))
        repeat(10) { seed ->
            val monster = MonsterRoller.roll(tiered, magic, Random(seed))
            assertEquals(listOf("MOB_TOUGH"), monster.modifiers.map { it.code }, "a magic monster rolled a rare-only modifier")
            assertEquals(90.0, monster.modifiers.single().effects.single().value)
        }
        val rare = listOf(CampaignRarity("RARE", 1, listOf(2, 2), modifierPower = 2.0))
        val monster = MonsterRoller.roll(tiered, rare, Random(1))
        assertEquals(setOf("MOB_TOUGH", "MOB_BERSERK"), monster.modifiers.map { it.code }.toSet())
        assertEquals(drowned.stats.getValue("STOCK_HEALTH") * 2.2, monster.stats.getValue("STOCK_HEALTH"), 1e-9)
    }

    @Test fun `each side's swing bar fills at its own attack speed`() {
        val rules = CombatRules()
        val battle = Battle(Combatant(mapOf("STOCK_HEALTH" to 500.0, "STOCK_ATTACK_SPEED" to 2.0), 1, rules),
            Combatant(mapOf("STOCK_HEALTH" to 500.0, "STOCK_ATTACK_SPEED" to 0.5), 1, rules), rules, 500.0, 0.0, 0, Random(2))
        battle.advance(0.36)
        val first = battle.events.first { it.actor == Side.HERO }
        assertTrue(battle.swing(Side.HERO) < 0.1f)
        battle.advance(0.25)
        assertEquals(0.5f, battle.swing(Side.HERO), 0.05f)
        // The monster swings once in two seconds, so a quarter second in it has barely begun.
        assertTrue(battle.swing(Side.MONSTER) < 0.35f)
        assertEquals(first, battle.events.first { it.actor == Side.HERO })
    }

    @Test fun `a fire hit can ignite, and the burn is logged once a second`() {
        val rules = CombatRules(ailments = listOf(AilmentRule("BURNING", "STOCK_ATTACK_FIRE", 100.0, 100.0, 2.0)))
        val torch = Combatant(mapOf("STOCK_HEALTH" to 100.0, "STOCK_ATTACK_FIRE" to 10.0, "STOCK_ATTACK_SPEED" to 0.5, "STOCK_CRITICAL_CHANCE" to 0.0), 1, rules)
        val dummy = Combatant(mapOf("STOCK_HEALTH" to 10000.0, "STOCK_ATTACK_PHYSICAL" to 0.1, "STOCK_ATTACK_SPEED" to 0.3), 1, rules)
        val log = Combat.fight(torch, dummy, 100.0, Random(4), rules)
        val hit = log.events.first { it.actor == Side.HERO && it.action == Action.ATTACK }
        assertEquals(listOf(Ailment.BURNING), hit.inflicted)
        assertEquals(DamageType.FIRE, hit.type)
        val ticks = log.events.filter { it.action == Action.TICK && it.actor == Side.HERO }
        assertTrue(ticks.isNotEmpty())
        assertTrue(ticks.all { it.ailment == Ailment.BURNING && it.type == DamageType.FIRE && it.damage > 0 })
        // A two-second burn of 100% is the hit again, in two ticks of about half each.
        val burn = ticks.take(2).sumOf { it.damage }
        assertEquals(hit.damage, burn, hit.damage * 0.1)
        assertEquals(1.0, ticks[1].time - ticks[0].time, 0.05)
    }

    @Test fun `cold chills and a heavy cold hit freezes, so the frozen side misses its turn`() {
        val rules = CombatRules(ailments = listOf(
            AilmentRule("CHILLED", "STOCK_ATTACK_COLD", 100.0, 50.0, 3.0),
            AilmentRule("FROZEN", "STOCK_ATTACK_COLD", 100.0, 0.0, 1.5, threshold = 10.0)))
        // One heavy cold swing every two and a half seconds: the freeze has time to pass before the next.
        val frost = Combatant(mapOf("STOCK_HEALTH" to 1000.0, "STOCK_ATTACK_COLD" to 30.0, "STOCK_ATTACK_SPEED" to 0.4, "STOCK_CRITICAL_CHANCE" to 0.0), 1, rules)
        val victim = Combatant(mapOf("STOCK_HEALTH" to 200.0, "STOCK_ATTACK_PHYSICAL" to 1.0, "STOCK_ATTACK_SPEED" to 1.0, "STOCK_CRITICAL_CHANCE" to 0.0), 1, rules)
        val battle = Battle(frost, victim, rules, 1000.0, 0.0, 0, Random(5))
        battle.advance(0.4)
        val hit = battle.events.first { it.actor == Side.HERO }
        assertEquals(setOf(Ailment.CHILLED, Ailment.FROZEN), hit.inflicted.toSet())
        assertTrue(battle.fighter(Side.MONSTER).held)
        // Frozen for a second and a half from 0.35: the monster's swing at 0.55 never comes.
        battle.advance(1.0)
        assertTrue(battle.events.none { it.actor == Side.MONSTER })
        battle.advance(1.0)
        assertTrue(battle.events.any { it.actor == Side.MONSTER })
        // Chill slows: the next swing after the thaw is scheduled at 1.5 times the interval.
        assertEquals(1.5, battle.fighter(Side.MONSTER).slow())
    }

    @Test fun `the innate spell is cast beside the swings and costs mana`() {
        val rules = CombatRules(spell = SpellRule(innateDamage = 5.0, innatePerLevel = 1.0, castSpeed = 1.0, manaCost = 50.0, manaRegenShare = 0.0))
        val exile = Combatant(mapOf("STOCK_HEALTH" to 500.0, "STOCK_MANA" to 40.0, "STOCK_ATTACK_PHYSICAL" to 1.0, "STOCK_ATTACK_SPEED" to 1.0, "STOCK_CRITICAL_CHANCE" to 0.0), 2, rules, innateSpell = true)
        val wall = Combatant(mapOf("STOCK_HEALTH" to 100000.0, "STOCK_ARMOR" to 100000.0, "STOCK_EVASION" to 100000.0, "STOCK_ATTACK_PHYSICAL" to 0.1, "STOCK_ATTACK_SPEED" to 0.3), 1, rules)
        val log = Combat.fight(exile, wall, 500.0, Random(6), rules)
        val spells = log.events.filter { it.actor == Side.HERO && it.action == Action.SPELL }
        // Two casts empty a 40-mana pool at half each; no regeneration, so no third.
        assertEquals(2, spells.size)
        assertTrue(spells.all { it.landed && it.type == DamageType.MAGICAL })
        // A spell goes around armour and evasion: it lands for about its base.
        assertEquals(6.0, spells.first().damage, 6.0 * rules.variance / 100 + 1e-9)
        assertEquals(0.0, log.heroMana, 1e-9)
        // Every swing at the wall is evaded or lands for nothing; the spell is what hurt it.
        assertTrue(log.events.filter { it.actor == Side.HERO && it.action == Action.ATTACK }.all { it.kind == HitKind.EVADED || it.damage < 0.2 })
    }

    @Test fun `a flask heals over its duration and a retreat gives the monster its free swings`() {
        val rules = CombatRules(flask = FlaskRule(charges = 2, perKill = 1, heal = 50.0, duration = 1.0), retreat = RetreatRule(delay = 1.0))
        val hero = Combatant(mapOf("STOCK_HEALTH" to 100.0, "STOCK_ATTACK_PHYSICAL" to 0.1, "STOCK_ATTACK_SPEED" to 1.0), 1, rules)
        val monster = Combatant(mapOf("STOCK_HEALTH" to 1000.0, "STOCK_ATTACK_PHYSICAL" to 0.1, "STOCK_ATTACK_SPEED" to 1.0, "STOCK_CRITICAL_CHANCE" to 0.0), 1, rules)
        val battle = Battle(hero, monster, rules, 20.0, 0.0, 2, Random(7))
        assertTrue(battle.useFlask())
        assertFalse(battle.useFlask(), "one flask at a time")
        assertEquals(1, battle.flasks)
        battle.advance(1.05)
        assertTrue(battle.heroLife in 65.0..71.0, "${battle.heroLife}")
        assertEquals(Action.FLASK, battle.events.first().action)
        assertTrue(battle.retreat())
        val before = battle.events.count { it.actor == Side.MONSTER && it.action == Action.ATTACK }
        battle.advance(1.05)
        assertEquals(Outcome.RETREAT, battle.outcome)
        assertTrue(battle.events.count { it.actor == Side.MONSTER && it.action == Action.ATTACK } > before, "the monster kept swinging")
        assertTrue(battle.events.none { it.actor == Side.HERO && it.action == Action.ATTACK && it.time > battle.events.first { e -> e.action == Action.RETREAT }.time })
    }

    @Test fun `energy shield recharges once left alone`() {
        val rules = CombatRules(shield = ShieldRule(rechargeDelay = 1.0, rechargePerSecond = 50.0))
        val warded = Combatant(mapOf("STOCK_HEALTH" to 1000.0, "STOCK_ENERGY_SHIELD" to 100.0, "STOCK_ATTACK_PHYSICAL" to 0.1, "STOCK_ATTACK_SPEED" to 0.3), 1, rules)
        val hitter = Combatant(mapOf("STOCK_HEALTH" to 1000.0, "STOCK_ATTACK_PHYSICAL" to 40.0, "STOCK_ATTACK_SPEED" to 0.3, "STOCK_CRITICAL_CHANCE" to 0.0, "STOCK_EVASION" to 0.0), 1, rules)
        val battle = Battle(warded, hitter, rules, 1000.0, 0.0, 0, Random(8))
        battle.advance(0.6)
        val hit = battle.events.first { it.actor == Side.MONSTER && it.landed }
        assertTrue(hit.heroShield < 100.0)
        // A second untouched, then half the shield a second: whole again well before the next swing.
        battle.advance(2.0)
        assertTrue(battle.fighter(Side.HERO).shield > hit.heroShield)
    }
}
