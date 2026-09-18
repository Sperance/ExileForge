package com.sperance.exileforge.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.core.model.combat.*
import com.sperance.exileforge.core.model.combat.world.Vec2
import com.sperance.exileforge.core.model.combat.world.WorldMode
import com.sperance.exileforge.core.model.combat.world.WorldRules
import com.sperance.exileforge.core.model.combat.world.WorldSimulation
import com.sperance.exileforge.core.model.combat.world.WorldTiming
import com.sperance.exileforge.ui.screens.combat.world.LootFeed
import com.sperance.exileforge.ui.screens.combat.world.WorldBottomPlate
import com.sperance.exileforge.ui.screens.combat.world.WorldTopPlate
import com.sperance.exileforge.ui.screens.combat.world.WorldView
import com.sperance.exileforge.ui.theme.ForgeTheme
import com.sperance.exileforge.ui.theme.Ink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Draws the expedition on a device with no server icon set loaded, which is the fallback path: the
 * map has to reach for the bundled glyphs rather than draw nothing.
 */
class WorldViewTest {
    @get:Rule val compose = createComposeRule()

    private val brute = Monster("brute", "Пепельный громила", 60.0, 7.0, element = "fire",
        lootTableId = "common", experience = 24, gold = 9)
    private val zone = Zone("coast", "Пепельный берег", "Выжженная полоса прибоя", 1,
        listOf(brute, Monster("hound", "Пепельный гончий", 30.0, 4.0, lootTableId = "common", experience = 8, gold = 3)),
        Monster("tide", "Владыка прилива", 200.0, 18.0, lootTableId = "boss", experience = 90, gold = 40, boss = true))

    private fun battle(turn: Int, heroLife: Double, enemyLife: Double, mana: Double, potions: Int,
        log: List<String> = emptyList(), rewards: List<BattleReward> = emptyList()) =
        Battle("battle-1", "coast", 1, brute,
            Combatant("Изгнанник", 180.0, heroLife, 60.0, mana, 24.0),
            Combatant(brute.name, 60.0, enemyLife),
            turn, BattleStatus.ACTIVE, potions, log, rewards,
            LootTable("common", 1, listOf(LootEntry("NORMAL", 3), LootEntry("MAGIC", 1))))

    /** Poses the world just after a hit lands, with the exile walked into reach of the mob. */
    private fun staged(): Pair<WorldSimulation, Battle> {
        val world = WorldSimulation()
        world.observe(battle(2, 180.0, 60.0, 60.0, 2), zone.monsters)
        world.mode = WorldMode.AUTO_RUN
        world.rules = WorldRules(engage = true, boss = false)
        var left = 30_000L
        while(left > 0L && !world.inStrikeRange) { world.advance(16L); left -= 16L }
        world.mode = WorldMode.AUTO_STRIKE
        val struck = battle(3, 164.0, 23.0, 52.0, 2, listOf("Изгнанник наносит критический удар"))
        world.observe(struck, zone.monsters, action = BattleAction.POWER)
        repeat(((WorldTiming.HERO_IMPACT + 90L) / 16L).toInt()) { world.advance(16L) }
        return world to struck
    }

    @Test fun worldDrawsTheFightAndTheHudReadsTheServerNumbers() {
        val (world, battle) = staged()
        compose.setContent {
            ForgeTheme {
                Box(Modifier.fillMaxWidth().height(620.dp).background(Ink)) {
                    WorldView(world, { world.clock }, Vec2(112f, 56f), { null }, Modifier.fillMaxSize())
                    WorldTopPlate(battle, zone, kills = 2, roaming = world.roaming, bossReady = false,
                        modifier = Modifier.align(Alignment.TopCenter))
                    LootFeed(world.collected, {}, Modifier.align(Alignment.TopEnd).padding(top = 70.dp, end = 10.dp))
                    WorldBottomPlate(battle, controls = true, mode = WorldMode.AUTO_STRIKE, onMode = {},
                        onAction = {}, onFlee = {}, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
        compose.onNodeWithText("Пепельный громила").assertIsDisplayed()
        compose.onNodeWithText("23/60").assertIsDisplayed()
        compose.onNodeWithText("164/180").assertIsDisplayed()
        compose.onNodeWithText("52/60").assertIsDisplayed()
        compose.onNodeWithText("2/3").assertIsDisplayed()
        compose.onNodeWithText("Авто-удар").assertIsDisplayed()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "design").apply { mkdirs() }
        File(directory, "arena.jpg").outputStream().use {
            Bitmap.createScaledBitmap(bitmap, 540, bitmap.height * 540 / bitmap.width, true)
                .compress(Bitmap.CompressFormat.JPEG, 80, it)
        }
    }

    @Test fun theWorldShowsTheLifeTheServerRemovedAndNotTheWordingOfItsLog() {
        val (world, _) = staged()
        // 60 - 23 straight off the two snapshots; the log line says "критический" and no number at all.
        assertEquals("37", world.numbers.first { it.crit }.text)
        assertEquals(23.0, world.engaged!!.life, 0.0)
        // The zone walks a whole pack, and the exile closed on the one the server chose out of it.
        assertEquals(WorldSimulation.PACK_SIZE, world.roaming)
        assertTrue(world.inStrikeRange)
    }
}
