package com.sperance.exileforge.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.sperance.exileforge.core.model.combat.*
import com.sperance.exileforge.ui.screens.combat.BattleActions
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class BattleActionsTest {
    @get:Rule val compose = createComposeRule()
    private fun battle() = Battle("encounter", "coast", 1,
        Monster("enemy", "Враг", 20.0, 3.0, lootTableId = "common", experience = 10, gold = 5),
        Combatant("Герой", 60.0, 60.0, mana = 0.0), Combatant("Враг", 20.0, 20.0),
        potions = 0, lootTable = LootTable("common", 1, listOf(LootEntry("NONE", 1))))
    @Test fun unavailableResourcesDisableOnlyRelevantActions() {
        var selected: BattleAction? = null
        compose.setContent { MaterialTheme { BattleActions(battle(), true, { selected = it }, {}) } }
        compose.onNodeWithText("Мощный · 8 MP").assertIsNotEnabled()
        compose.onNodeWithText("Флакон · +40% HP").assertIsNotEnabled()
        compose.onNodeWithText("Защита · +6 MP").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(BattleAction.GUARD, selected) }
    }
    @Test fun completedBattleDisablesAllCommands() {
        compose.setContent { MaterialTheme { BattleActions(battle().copy(status = BattleStatus.VICTORY), true, {}, {}) } }
        compose.onNodeWithText("Удар").assertIsNotEnabled()
        compose.onNodeWithText("Защита · +6 MP").assertIsNotEnabled()
        compose.onNodeWithText("Отступить").assertIsNotEnabled()
    }
}
