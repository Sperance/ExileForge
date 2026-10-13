package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.run.ExpeditionWorld
import com.sperance.exileforge.core.campaign.run.RoadLife
import com.sperance.exileforge.core.quests.live
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.Counter
import com.sperance.exileforge.rules.content.ExpeditionRules
import com.sperance.exileforge.rules.content.Quest
import com.sperance.exileforge.rules.content.QuestKind
import com.sperance.exileforge.rules.content.Rarity
import kotlin.test.Test
import kotlin.test.assertEquals

/** Карта между боями (4.10.0): здоровье по чистой скорости, шаг после босса, счёт заданий от снимка доски. */
class RoadLifeTest {
    @Test
    fun regeneration_fills_up_to_the_maximum() {
        assertEquals(60.0, RoadLife(10.0).after(50.0, 100.0, 1.0))
        assertEquals(100.0, RoadLife(10.0).after(95.0, 100.0, 1.0))
    }

    @Test
    fun degeneration_off_the_fight_stops_at_one() {
        assertEquals(40.0, RoadLife(-10.0).after(50.0, 100.0, 1.0))
        assertEquals(1.0, RoadLife(-10.0).after(5.0, 100.0, 1.0))
        assertEquals(1.0, RoadLife(-10.0).after(1.0, 100.0, 1.0))
    }

    @Test
    fun boss_slain_speed_lies_over_the_sheet_cap() {
        val rules = ExpeditionRules(heroSpeed = 3.0, bossSlainSpeed = 2.0)
        val fast = mapOf(CoreStat.MOVEMENT_SPEED.code to 400.0)
        assertEquals(3.0 * 2.5, ExpeditionWorld.heroSpeed(rules, fast), 1e-9)
        assertEquals(3.0 * 2.5 * 2.0, ExpeditionWorld.heroSpeed(rules, fast, bossSlain = true), 1e-9)
    }

    @Test
    fun the_run_adds_its_tally_to_the_board_taken_at_entry_once() {
        val atEntry = Quest("q", QuestKind.DAILY, "kill", Counter.KILLS, Rarity.COMMON, target = 10, progress = 3)
        assertEquals(7, atEntry.live(mapOf(Counter.KILLS to 4L)).progress)
        assertEquals(false, atEntry.live(mapOf(Counter.KILLS to 4L)).done)
        assertEquals(10, atEntry.live(mapOf(Counter.KILLS to 40L)).progress)
    }
}
