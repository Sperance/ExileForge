package com.sperance.exileforge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import com.sperance.exileforge.core.model.passives.*
import com.sperance.exileforge.core.model.command.CalculatedStats
import com.sperance.exileforge.ui.screens.passives.*
import com.sperance.exileforge.ui.theme.ForgeTheme
import com.sperance.exileforge.ui.theme.Ink
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class PassiveTreeTest {
    @get:Rule val compose = createComposeRule()
    private val root = PassiveNode("origin", "Начало пути", "Общий старт", PassiveNodeKind.ORIGIN, 0.0, 0.0, listOf(PassiveEffect("maximum_life", PassiveOperation.FLAT, 5.0)))
    private fun state() = PassiveState(2, 1, emptySet(), 1, 0, 1, setOf("origin"), emptySet(), CalculatedStats(2, emptyMap()))
    @Test fun selectionAndZoomKeepGraphInteractive() {
        val nodes = listOf(root) + (1..6).map { i ->
            val angle = i * Math.PI / 3
            root.copy(id = "node$i", name = "Ветвь $i", x = kotlin.math.cos(angle) * 230, y = kotlin.math.sin(angle) * 230, kind = if(i % 2 == 0) PassiveNodeKind.KEYSTONE else PassiveNodeKind.NOTABLE)
        }
        val tree = PassiveTree(1, "Созвездие изгнанника", "origin", nodes, nodes.drop(1).map { PassiveEdge("origin", it.id) })
        var selected: String? = null
        compose.setContent { ForgeTheme { Column(Modifier.background(Ink).padding(12.dp)) {
            Text("Созвездие изгнанника · свободно 1")
            PassiveGraph(tree, setOf("origin"), setOf("node1", "node2"), "origin", { selected = it })
            PassiveNodeDetails(root, state(), false) { _, _ -> }
        } } }
        compose.onNodeWithContentDescription("Приблизить дерево").performClick()
        compose.onNodeWithTag("passive_graph").performTouchInput { click(center) }
        compose.runOnIdle { assertEquals("origin", selected) }
        compose.onNodeWithText("Центр").performClick()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "design").apply { mkdirs() }
        File(directory, "passives.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
    }
    @Test fun allocationEmitsStableNodeIdAndBattleLockDisablesIt() {
        var locked by mutableStateOf(false)
        var action: Pair<PassiveAction, String>? = null
        compose.setContent { ForgeTheme { PassiveNodeDetails(root, state().copy(lockedReason = if(locked) "Завершите бой" else null), false) { a, id -> action = a to id } } }
        compose.onNodeWithText("Изучить узел").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(PassiveAction.ALLOCATE to "origin", action); locked = true }
        compose.onNodeWithText("Изучить узел").assertIsNotEnabled()
        compose.onNodeWithText("Завершите бой").assertIsDisplayed()
    }
    @Test fun structuralDependenciesDisableRefund() {
        compose.setContent { ForgeTheme { PassiveNodeDetails(root, state().copy(allocated = setOf("origin"), refundable = emptySet()), false) { _, _ -> } } }
        compose.onNodeWithText("Снять узел").assertIsNotEnabled()
    }
}
