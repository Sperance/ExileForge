package com.sperance.exileforge.core

import com.sperance.exileforge.core.display.rollQuality
import com.sperance.exileforge.core.display.rollSummary
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierSource
import com.sperance.exileforge.core.model.modifier.ModifierTier
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The trade table (2.60.0) reads a roll against its tier's range, and sums the item up. */
class RollQualityTest {
    private val life = ModifierDefinition(id = "life", code = "LIFE", source = ModifierSource.PREFIX,
        tiers = listOf(ModifierTier(80, listOf(listOf(70.0, 80.0))), ModifierTier(40, listOf(listOf(50.0, 50.0)))))

    private fun line(tier: Int, value: Double) = buildJsonObject {
        put("modifierCode", "LIFE"); put("tier", tier); put("values", buildJsonArray { add(JsonPrimitive(value)) })
    }

    @Test fun a_value_is_placed_inside_its_tier() {
        assertEquals(.75, rollQuality(line(1, 77.5), listOf(life))!!, 1e-9)
        assertEquals(1.0, rollQuality(line(2, 50.0), listOf(life)))
        assertNull(rollQuality(line(0, 77.5), listOf(life)), "a line with no tier has no quality")
    }

    @Test fun a_magic_item_counts_its_open_places_and_best_tier() {
        val summary = rollSummary(buildJsonObject { put("rarity", "UNCOMMON") }, listOf(line(2, 50.0)), listOf(life))
        assertEquals(1, summary.openSlots)
        assertEquals(2, summary.bestTier)
        assertEquals(100, summary.quality)
    }
}
