package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.MapGenerator
import com.sperance.exileforge.core.campaign.SceneSight
import kotlin.test.Test
import kotlin.test.assertTrue

/** Выход стоит там, где скала перед ним не закрывает его овал (3.95.3), - на всех биомах и многих семенах. */
class SceneSightTest {
    private val biomes = listOf("CRYPT", "RUINS", "MINES", "TEMPLE", "CITADEL", "MIRE", "CAVE", "VOLCANO", "FROST")

    @Test
    fun the_exit_stands_in_view_of_the_camera() {
        var checked = 0
        var hidden = 0
        biomes.forEach { biome ->
            (1L..40L).forEach { seed ->
                val map = MapGenerator.generate(seed, biome, monsters = 12, size = 56)
                checked++
                if (!SceneSight.inView(map.exit.x, map.exit.y) { x, y -> !map.clear(x, y) }) hidden++
            }
        }
        // Запасной путь (нет ни одной открытой клетки) допустим, но не как правило
        assertTrue(hidden * 20 <= checked, "exit hidden behind rock on $hidden of $checked maps")
    }
}
