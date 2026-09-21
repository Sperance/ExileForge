package com.sperance.exileforge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.sperance.exileforge.core.display.IconBundle
import com.sperance.exileforge.core.display.IconKey
import com.sperance.exileforge.core.display.IconPath
import com.sperance.exileforge.core.display.IconSprite
import com.sperance.exileforge.core.display.icon
import com.sperance.exileforge.core.display.serverIcons
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.components.PropertyRow
import com.sperance.exileforge.ui.icons.spriteVector
import com.sperance.exileforge.ui.theme.ForgeTheme
import com.sperance.exileforge.ui.theme.Ink
import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Drawings that arrive from the server rather than from the build.
 *
 * What matters is not that a helmet looks like a helmet — a test cannot see that — but that a card
 * reaches for the server's outline when there is one and survives when there is not. An icon is
 * decoration, and decoration must never be able to take a screen down with it.
 */
class ServerIconTest {
    @get:Rule val compose = createComposeRule()

    @After fun forget() { serverIcons = IconBundle() }

    private val set = """{
        "sprites": {"helmet": {"viewBox": 24, "paths": [{"d": "M4 11a8 8 0 0 1 16 0v6h-4v-5h-8v5H4z", "alpha": 1.0}]},
                    "broken": {"viewBox": 24, "paths": [{"d": "nonsense, not a path at all", "alpha": 1.0}]}},
        "icons": {"equipment.IRON_SKULLCAP": "helmet", "stat.STOCK_ARMOR": "helmet", "item.BAD_ORB": "broken"}
    }"""

    @Test fun anOutlineFromTheServerBecomesSomethingComposeCanPaint() {
        serverIcons = IconBundle.parse("sha", set)
        val helmet = spriteVector(requireNotNull(icon(IconKey.equipment("IRON_SKULLCAP"))))
        assertNotNull("the server's outline did not parse", helmet)
        assertEquals(24f, requireNotNull(helmet).viewportWidth)
        // Parsing is memoised by the sprite, so a stash full of helmets parses one outline.
        assertEquals(helmet, spriteVector(requireNotNull(icon(IconKey.equipment("IRON_SKULLCAP")))))
    }

    @Test fun aMalformedOutlineCostsItsOwnIconAndNothingElse() {
        serverIcons = IconBundle.parse("sha", set)
        // One typo in a hand-written file must not blank the app: the caller falls back instead.
        assertNull(spriteVector(requireNotNull(icon(IconKey.item("BAD_ORB")))))
        assertNotNull(spriteVector(requireNotNull(icon(IconKey.stat("STOCK_ARMOR")))))
        assertNull(spriteVector(IconSprite(24f, emptyList())))
        assertNull(spriteVector(IconSprite(0f, listOf(IconPath("M1 1h2v2h-2z", 1f)))))
    }

    @Test fun aCardAndAStatRowBothDrawWithoutASetAndWithOne() {
        val helmet = buildJsonObject {
            put("_id", "x"); put("code", "IRON_SKULLCAP"); put("name", "Железный шишак")
            put("slot", "HELMET"); put("rarity", "RARE"); put("itemLevel", 30)
        }
        // No set: the bundled emblem stands in and the card still renders in full.
        compose.setContent { ForgeTheme { Column(Modifier.fillMaxSize().background(Ink)) {
            ItemCard(helmet, actionLabel = "Открыть")
            PropertyRow("Броня", "120", "STOCK_ARMOR")
        } } }
        compose.onNodeWithText("Железный шишак").assertIsDisplayed()
        compose.onNodeWithText("120").assertIsDisplayed()
    }
}
