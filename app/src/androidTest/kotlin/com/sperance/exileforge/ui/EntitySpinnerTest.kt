package com.sperance.exileforge.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.network.ItemPage
import com.sperance.exileforge.ui.components.EntitySpinner
import com.sperance.exileforge.ui.components.LocalEntityPageLoader
import com.sperance.exileforge.ui.theme.ForgeTheme
import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EntitySpinnerTest {
    @get:Rule val compose = createComposeRule()
    @Test fun selectsNamedRecordFromNextPageAndReturnsFullId() {
        val id = "0123456789abcdef01234567"
        var selected = ""
        compose.setContent {
            ForgeTheme {
                CompositionLocalProvider(LocalEntityPageLoader provides { _, page ->
                    ItemPage(if(page == 0) emptyList() else listOf(buildJsonObject { put("_id", id); put("name", "Изгнанник") }), page, 2, 1)
                }) {
                    EntitySpinner("Персонаж", selected, EntitySource.CHARACTER) { selected = it }
                }
            }
        }
        compose.onNodeWithText("Персонаж: Выбрать ▾").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Загрузить ещё").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Изгнанник · 234567").performClick()
        compose.runOnIdle { assertEquals(id, selected) }
    }
}
