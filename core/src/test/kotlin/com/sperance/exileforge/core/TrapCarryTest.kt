package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.CarriedAilment
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.ExpeditionWorld
import com.sperance.exileforge.core.campaign.run.FeatureAction
import com.sperance.exileforge.core.campaign.run.TrapSpot
import com.sperance.exileforge.core.campaign.run.afflict
import com.sperance.exileforge.core.campaign.run.engage
import com.sperance.exileforge.core.campaign.run.spring
import com.sperance.exileforge.core.campaign.run.stride
import com.sperance.exileforge.core.campaign.run.walk
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.content.TrapKind
import com.sperance.exileforge.rules.content.TrapShape
import com.sperance.exileforge.rules.roll.ChestWindow
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import com.sperance.exileforge.rules.run.TrapAxis
import com.sperance.exileforge.rules.run.TrapLayout
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Ловушки на карте: недуг входит в следующий бой и тикает в нём, удар бьёт ритмом и не убивает разом. */
class TrapCarryTest {
    private val index get() = TestContent.index
    private val zone get() = index.campaign.zones.first()

    private fun expedition(): ExpeditionRun {
        val heroClass = index.classes.classes.first()
        val level = 12
        val sheet = Sheets.calculate(index, level, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList())
        val gear = HeroGear(sheet.stats, level, sheet.model, HeroStance.of(heroClass.code), Loadout.of(HeroSkills(), index, heroClass.code, emptyList(), sheet.stats, index.powers, index.rules.charges), index.stats.percent)
        val run = Run(index, zone, 7L, RunContext(heroClass.code, level))
        val journal = RunJournal("run-7", "hero", zone.code.value)
        val campaign = CampaignState(chests = mapOf(zone.code.value to ChestWindow(Long.MAX_VALUE, 0)))
        return ExpeditionRun.start(index, zone, run, journal, gear, campaign, NOW, 0.0, level, { 0L })
    }

    /** Шипы с кровотечением: недуг ложится на героя на карте, бой принимает его с тем же остатком и силой и тикает им. */
    @Test
    fun a_trap_ailment_rides_into_the_next_fight_and_ticks_there() {
        val run = expedition()
        run.spring(MapFeature.Trap(1, TrapKind("SPIKES", TrapShape.LINE, "PHYSICAL", hit = 5.0, dot = 2.0, seconds = 10.0)))
        val carried = run.carry.ailments.single { it.ailment == Ailment.BLEEDING }
        assertTrue(carried.magnitude > 0, "the trap leaves a bleeding that hurts")
        run.engage(run.world.agents.first())
        val battle = assertNotNull(run.fight, "the fight starts")
        val laid = battle.heroFighter.ailments.single { it.ailment == Ailment.BLEEDING }
        assertEquals(carried.magnitude, laid.magnitude, 1e-9, "the fight takes the trap's strength")
        assertEquals(carried.left, laid.until - battle.time, 1e-9, "and what is left of its time")
        battle.advance(1.5)
        assertTrue((battle.heroFighter.tickedAt[Ailment.BLEEDING] ?: 0.0) > 0.0, "the carried bleeding ticks in the fight")
    }

    /**
     * Каждая ловушка контента бьёт с последствием: оставляет длящийся недуг (его несёт и следующий бой) или отбрасывает; пульсирующая
     * зона бьёт видами, что его оставляют.
     */
    @Test
    fun every_trap_of_the_content_leaves_a_lasting_ailment_or_knocks_back() {
        val traps = index.campaign.features?.traps ?: return
        traps.kinds.filter { it.shape != TrapShape.PULSE }.forEach { kind -> assertTrue(kind.knock > 0 || (kind.dot + kind.hold > 0 && kind.seconds > 0), "${kind.code} leaves an ailment or knocks back") }
        traps.kinds.filter { it.shape == TrapShape.PULSE }.flatMap { it.mimics }.forEach { code -> assertTrue(traps.kind(code)?.seconds ?: 0.0 > 0, "the pulse strikes as $code with an ailment") }
    }

    /** Холод и шок ловушки ложатся силой, а не уроном в секунду; один удар не снимает больше потолка правил. */
    @Test
    fun cold_and_lightning_traps_chill_and_shock_and_no_strike_kills_alone() {
        val run = expedition()
        val traps = index.campaign.features!!.traps
        run.spring(MapFeature.Trap(1, traps.kind("FROST")!!))
        run.spring(MapFeature.Trap(2, traps.kind("STORM")!!))
        assertTrue(run.carry.ailments.any { it.ailment == Ailment.CHILLED && it.magnitude > 0 }, "the frost cross chills")
        assertTrue(run.carry.ailments.any { it.ailment == Ailment.SHOCKED && it.magnitude > 0 }, "the storm pillars shock")
        val brutal = expedition()
        val full = brutal.heroLife
        brutal.spring(MapFeature.Trap(3, TrapKind("SPIKES", TrapShape.LINE, "PHYSICAL", hit = 500.0), power = 10.0))
        assertTrue(brutal.heroLife > 0 && full - brutal.heroLife <= full * traps.maxHit / 100 + 1e-6, "one strike takes no more than the ceiling")
    }

    /** Ловушка на карте бьёт своим ритмом, пока герой в зоне, и молчит, когда он вышел. */
    @Test
    fun a_trap_strikes_on_its_beat_while_the_hero_stands_in_it() {
        val run = expedition()
        val world = run.world
        val kind = index.campaign.features!!.traps.kind("FIRE")!!
        val cell = world.map.start
        val spot = TrapSpot(MapFeature.Trap(9, kind, size = 2, offset = .4), TrapLayout(kind, 2, cell.x, cell.y, TrapAxis.EAST), index.campaign.features!!.traps)
        val dt = 1.0 / 30
        fun beat(): FeatureAction? {
            world.step(dt, 0.0, 0.0)
            return spot.touch(world, dt)
        }
        val inside = (0 until 150).count { beat() is FeatureAction.Strike }
        assertEquals(kotlin.math.floor((5.0 - .4) / kind.every).toInt() + 1, inside, "one strike per beat over five seconds in the zone")
        world.heroX += 6
        assertEquals(0, (0 until 150).count { beat() != null }, "no strike once the hero has left")
    }

    /** Холод морозной ловушки укорачивает шаг по карте тем же растяжением, что действия в бою; самый сильный холод - не ниже пола. */
    @Test
    fun chill_shortens_the_step_on_the_map_but_not_below_the_floor() {
        val run = expedition()
        run.stickX = 1.0
        assertEquals(1.0, run.stride(), 1e-9, "no chill - a full step")
        val plain = run.stepLength()
        run.spring(MapFeature.Trap(1, index.campaign.features!!.traps.kind("FROST")!!))
        val chill = run.carry.ailments.single { it.ailment == Ailment.CHILLED }.magnitude
        assertEquals(1 / (1 + chill / 100), run.stride(), 1e-9, "the step divides by the fight's chill stretch")
        val chilled = run.stepLength()
        assertTrue(chilled < plain, "the chilled step is shorter")
        assertEquals(plain * run.stride(), chilled, 1e-6, "shorter by the stride")
        run.afflict(CarriedAilment(Ailment.CHILLED, 10.0, 1_000.0, 10.0))
        assertEquals(ExpeditionWorld.CHILL_FLOOR, run.stride(), 1e-9, "the strongest chill stops at the floor")
        assertEquals(plain * ExpeditionWorld.CHILL_FLOOR, run.stepLength(), 1e-6, "and so does the step")
    }

    /** Длина одного кадра шага героя с клетки посреди пола, вдали от стен и монстров: ни стена, ни встреча шаг не режут. */
    private fun ExpeditionRun.stepLength(): Double {
        val map = world.map
        val cell = (2 until map.width - 2).flatMap { x -> (2 until map.height - 2).map { y -> x to y } }.first { (x, y) ->
            (-2..2).all { dx -> (-2..2).all { dy -> map.walkable(x + dx, y + dy) } } &&
                world.agents.all { hypot(it.x - x - .5, it.y - y - .5) > world.rules.contact + 2 }
        }
        world.heroX = cell.first + .5
        world.heroY = cell.second + .5
        walk(1.0 / 30)
        return hypot(world.heroX - cell.first - .5, world.heroY - cell.second - .5)
    }

    private companion object {
        const val NOW = 1_000_000_000_000L
    }
}
