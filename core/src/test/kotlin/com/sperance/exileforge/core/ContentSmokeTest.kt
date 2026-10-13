package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.rules.content.ContentFiles
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.ItemFactory
import com.sperance.exileforge.rules.run.RewardDraws
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import com.sperance.exileforge.rules.run.RunEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The client against the pinned server's content, with no server running: the chunks load, the
 * sheet stands, a seeded run rolls the same twice (the server replays the journal with the same
 * seed, so a divergence here is a divergence in what the player is paid), and the standing rule
 * that a magic or rare item never leaves the roll empty.
 */
class ContentSmokeTest {
    private val index get() = TestContent.index

    @Test
    fun the_content_loads_and_every_class_stands() {
        assertTrue(index.hash.isNotBlank())
        assertTrue(index.templates.isNotEmpty() && index.campaign.zones.isNotEmpty() && ContentFiles.ALL.size == 18)
        index.classes.classes.forEach { heroClass ->
            val sheet = Sheets.calculate(index, 1, heroClass.code, emptyList(), emptyList())
            assertTrue((sheet.stats["STOCK_HEALTH"] ?: 0.0) > 0.0, "${heroClass.code} has no life at level 1")
        }
    }

    @Test
    fun a_seeded_run_rolls_the_same_twice() {
        val zone = index.campaign.zones.first()
        val context = RunContext(index.classes.classes.first().code, zone.level)
        val first = Run(index, zone, 42L, context)
        val second = Run(index, zone, 42L, context)
        assertEquals(first.count, second.count)
        assertEquals(first.spawn(0), second.spawn(0))
        // Rewards roll on the server's own stream (1.30.0): the same draws pay the same, whatever the client holds.
        assertEquals(first.kill(0, 0, false, RewardDraws(7L, 0)), second.kill(0, 0, false, RewardDraws(7L, 0)))
        assertEquals(first.chest(RewardDraws(7L, 1)), second.chest(RewardDraws(7L, 1)))
        assertEquals(first.boss(RewardDraws(7L, 2)), second.boss(RewardDraws(7L, 2)))
        assertNotNull(first.kill(0, 0, false, RewardDraws(7L, 0)), "a kill pays nothing at all")
    }

    @Test
    fun a_magic_or_rare_item_never_rolls_empty() {
        val factory = ItemFactory(index)
        var checked = 0
        index.templates.values.filterNot { it.unique }.forEach { template ->
            listOf(Rarity.MAGIC, Rarity.RARE).forEach { rarity ->
                (1L..3L).forEach { seed ->
                    val item = factory.create("item", template, rarity, Dice(seed))
                    val affixes = item.rolls.count { index.modifier(it.code)?.affix == true }
                    val floor = index.limits(item.rarity, template.slot).floor.coerceAtLeast(1)
                    assertTrue(item.rarity != Rarity.COMMON && affixes >= floor, "${template.code} ${item.rarity} seed $seed: $affixes affixes, floor $floor")
                    checked++
                }
            }
        }
        assertTrue(checked > 0)
    }

    @Test
    fun the_journal_survives_a_restart() {
        val journal = RunJournal("run", "hero", "zone")
        journal.record { RunEvent.Kill(it, i = 0, m = 1) }
        journal.record { RunEvent.Chest(it, index = 0) }
        journal.confirm(1)
        val back = assertNotNull(RunJournal.decode(journal.encode()))
        assertEquals(journal.all, back.all)
        assertEquals(journal.pending, back.pending)
        assertEquals(1, back.applied)
        // A batch whose answer was lost goes again under its own key, even after a restart; an answer frees it.
        val sent = assertNotNull(back.outgoing { "first" }).first
        back.record { RunEvent.Kill(it, i = 1, m = 0) }
        val again = assertNotNull(assertNotNull(RunJournal.decode(back.encode())).outgoing { "second" })
        assertEquals(sent, again.first)
        assertEquals(1, again.second.size)
        back.confirm(2)
        assertEquals("second", assertNotNull(back.outgoing { "second" }).first.key)
    }
}
