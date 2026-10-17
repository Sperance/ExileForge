package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.Cell
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.MapGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Каждая форма карты контента на биоме этой формы, размерах зон и многих семенах: встают все стаи захода, стаи достижимы от
 * входа при закрытых воротах, босс - только после открытых (у формы с воротами), у формы без ворот ворот нет.
 */
class MapShapesTest {
    private val expedition get() = TestContent.index.campaign.expedition

    /** Наибольшее число стай захода зоны кампании. */
    private val packs get() = TestContent.index.campaign.zones.maxOf { it.count[1] }

    @Test
    fun every_shape_stands_every_pack_and_reaches_the_boss() {
        expedition.shapes.forEach { rule ->
            val biome = rule.biomes.first()
            listOf(52, 56, 60).forEach { size ->
                (1L..12L).forEach { seed ->
                    val map = MapGenerator.generate(seed, biome, packs, expedition.terrain, size, expedition.minChasm, expedition.minPool, rule.shape)
                    val where = "${rule.shape} $biome size $size seed $seed"
                    assertEquals(packs, map.spawns.size, "$where: not every pack stands")
                    val closed = reach(map)
                    map.spawns.forEach { assertTrue(it in closed, "$where: the pack at $it is out of reach") }
                    assertEquals(rule.shape.gated, map.gate.isNotEmpty(), "$where: an arena gate where the shape has none, or none where it has")
                    if (rule.shape.gated) {
                        assertTrue(map.exit !in closed, "$where: the boss is reachable past a closed gate")
                        map.openGate()
                    }
                    assertTrue(map.exit in reach(map), "$where: the boss is out of reach")
                }
            }
        }
    }

    /** Клетки пола, куда дойти от входа [ExpeditionMap.start] шагами по сторонам. */
    private fun reach(map: ExpeditionMap): Set<Cell> {
        val seen = hashSetOf(map.start)
        val queue = ArrayDeque(listOf(map.start))
        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            listOf(Cell(x + 1, y), Cell(x - 1, y), Cell(x, y + 1), Cell(x, y - 1)).filter { map.walkable(it.x, it.y) && seen.add(it) }.forEach(queue::addLast)
        }
        return seen
    }
}
