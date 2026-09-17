package com.sperance.exileforge.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.core.model.combat.*
import com.sperance.exileforge.core.model.combat.arena.ArenaSimulation
import com.sperance.exileforge.core.model.combat.arena.ArenaTiming
import com.sperance.exileforge.ui.screens.combat.arena.ArenaHud
import com.sperance.exileforge.ui.screens.combat.arena.ArenaStage
import com.sperance.exileforge.ui.theme.ForgeTheme
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Ink
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Draws the arena on a device with no server icon set loaded, which is the fallback path: the stage
 * has to reach for the bundled glyphs rather than draw nothing.
 */
class ArenaStageTest {
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

    /** Poses the stage just after a hit lands, so the capture shows numbers, sparks and the pack. */
    private fun staged(): Pair<ArenaSimulation, Battle> {
        val arena = ArenaSimulation()
        arena.observe(battle(2, 180.0, 60.0, 60.0, 2), zone.monsters)
        repeat(30) { arena.advance(16L) }
        val struck = battle(3, 164.0, 23.0, 52.0, 2, listOf("Изгнанник наносит критический удар"))
        arena.observe(struck, zone.monsters, action = BattleAction.POWER)
        repeat(((ArenaTiming.HERO_IMPACT + 90L) / 16L).toInt()) { arena.advance(16L) }
        return arena to struck
    }

    @Test fun stageDrawsTheFightAndTheHudReadsTheServerNumbers() {
        val (arena, battle) = staged()
        compose.setContent {
            ForgeTheme {
                Column(Modifier.fillMaxWidth().background(Ink).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("EXILE FORGE · АРЕНА", color = Gold)
                    ArenaStage(arena, { arena.clock }, battle.zoneId, Modifier.fillMaxWidth().height(250.dp))
                    ArenaHud(battle, zone, kills = 2, flasks = battle.potions)
                }
            }
        }
        compose.onNodeWithText("Пепельный громила").assertIsDisplayed()
        compose.onNodeWithText("164/180").assertIsDisplayed()
        compose.onNodeWithText("52/60").assertIsDisplayed()
        compose.onNodeWithText("Ход 3 · стихия fire").assertIsDisplayed()
        compose.onNodeWithText("ЗАЧИСТКА 2/3").assertIsDisplayed()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "design").apply { mkdirs() }
        File(directory, "arena.jpg").outputStream().use {
            Bitmap.createScaledBitmap(bitmap, 540, bitmap.height * 540 / bitmap.width, true)
                .compress(Bitmap.CompressFormat.JPEG, 80, it)
        }
    }

    @Test fun theStageShowsTheLifeTheServerRemovedAndNotTheWordingOfItsLog() {
        val (arena, _) = staged()
        // 60 - 23 straight off the two snapshots; the log line says "критический" and no number at all.
        assertEquals("37", arena.numbers.first { it.crit }.text)
        assertEquals(23.0, arena.engaged!!.life, 0.0)
        assertEquals(2, arena.mobs.size)
    }
}
