package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.run.ExpeditionWorld
import com.sperance.exileforge.core.campaign.run.distances
import kotlin.test.Test
import kotlin.test.assertTrue

/** Тело в скале по нарисованному полю стен: середина каждой доступной клетки пола свободна для героя - подножие не перекрывает проход. */
class WallBodyTest {
    @Test
    fun the_drawn_rock_never_closes_a_floor_cell_to_the_hero() {
        val index = TestContent.index
        val rules = index.campaign.expedition
        index.campaign.zones.filterIndexed { n, _ -> n % 5 == 0 }.forEach { zone ->
            val world = ExpeditionWorld.create(rules, zone, emptyList(), emptyMap(), 11L, null, false)
            world.distances(world.map.start, Int.MAX_VALUE).keys.forEach { cell ->
                assertTrue(world.body.free(cell.x + .5, cell.y + .5, rules.heroRadius), "${zone.code}: the floor cell $cell is closed by the drawn rock")
            }
        }
    }
}
