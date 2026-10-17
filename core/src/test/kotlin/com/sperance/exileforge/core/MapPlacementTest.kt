package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.Cell
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.run.BlightSpot
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.ExpeditionWorld
import com.sperance.exileforge.core.campaign.run.RoomSpot
import com.sperance.exileforge.core.campaign.run.distances
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.ChestWindow
import com.sperance.exileforge.rules.run.FeatureKind
import com.sperance.exileforge.rules.run.MapMechanic
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Расстановка объектов карты на выборке семян: чистая зона у входа, выхода и портала Ваал и рычаг у каждой запертой комнаты. */
class MapPlacementTest {
    private val index get() = TestContent.index

    /** Заход в [zone] с семенем [seed] и всеми механиками насильно: на карте стоит всё, что умеет стоять. */
    private fun expedition(zone: Int, seed: Long): ExpeditionRun {
        val heroClass = index.classes.classes.first()
        val level = 40
        val place = index.campaign.zones[zone % index.campaign.zones.size]
        val sheet = Sheets.calculate(index, level, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList())
        val gear = HeroGear(sheet.stats, level, sheet.model, HeroStance.of(heroClass.code), Loadout.of(HeroSkills(), index, heroClass.code, emptyList(), sheet.stats, index.powers, index.rules.charges), index.stats.percent)
        val run = Run(index, place, seed, RunContext(heroClass.code, level, forced = MapMechanic.entries.toSet()))
        val journal = RunJournal("run-$seed", "hero", place.code.value)
        val campaign = CampaignState(chests = mapOf(place.code.value to ChestWindow(Long.MAX_VALUE, 3)))
        return ExpeditionRun.start(index, place, run, journal, gear, campaign, 0L, 0.0, level, { 0L })
    }

    private fun world(zone: Int, seed: Long): ExpeditionWorld = expedition(zone, seed).world

    private val samples = (0 until 6).flatMap { zone -> listOf(3L, 41L, 977L, 20_261L).map { zone * 7 to it } }

    @Test
    fun no_map_object_stands_within_the_clear_zone() {
        val steps = index.campaign.expedition.clearSteps
        samples.forEach { (zone, seed) ->
            val world = world(zone, seed)
            val clear = listOfNotNull(world.map.start, world.map.exit, world.portal).flatMap { world.distances(it, steps - 1).keys }.toSet()
            val portalClear = listOf(world.map.start, world.map.exit).flatMap { world.distances(it, steps - 1).keys }.toSet()
            val objects: List<Cell> = world.chests.map { it.cell } + world.fountains.map { it.cell } + world.crystals.map { it.cell } +
                world.cracks.map { it.cell } + world.features.flatMap { spot ->
                    when (spot) {
                        is RoomSpot -> listOfNotNull(spot.lever)
                        is BlightSpot -> spot.cells
                        else -> listOf(spot.cell)
                    }
                }
            objects.forEach { cell -> assertTrue(cell !in clear, "zone $zone seed $seed: an object at $cell inside the clear zone") }
            world.portal?.let { assertTrue(it !in portalClear, "zone $zone seed $seed: the Vaal portal inside the clear zone") }
        }
    }

    @Test
    fun every_locked_room_has_its_lever() {
        var vaults = 0
        samples.forEach { (zone, seed) ->
            val world = world(zone, seed)
            val reach = world.distances(world.map.start, Int.MAX_VALUE).keys
            world.features.filterIsInstance<RoomSpot>().filter { it.kind == FeatureKind.VAULT }.forEach { room ->
                vaults++
                val lever = assertNotNull(room.lever, "zone $zone seed $seed: a locked room without a lever")
                assertTrue(lever in reach, "zone $zone seed $seed: the lever at $lever is out of the hero's reach")
            }
        }
        assertTrue(vaults > 0, "the sample placed no locked room")
    }
}
