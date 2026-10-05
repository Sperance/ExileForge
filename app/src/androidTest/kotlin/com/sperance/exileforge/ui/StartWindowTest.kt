package com.sperance.exileforge.ui

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import com.sperance.exileforge.MainActivity
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.StartGate
import org.junit.Rule
import org.junit.Test

/**
 * Окно запуска не висит (3.87.0): при первом и при повторном запуске в том же процессе - как у игрока, вернувшегося в
 * игру, - оно за [SETTLE_MS] либо уходит (сервер ответил), либо говорит, что сервер недоступен. Повторный запуск
 * держал окно на «подключение…» бесконечно, хотя сервер ответил за полсекунды.
 */
class StartWindowTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun startWindowSettlesOnFirstAndSecondLaunch() {
        repeat(2) { launch ->
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.waitUntil(SETTLE_MS) { settled() }
                check(settled()) { "launch ${launch + 1}: the start window is still waiting" }
            }
        }
    }

    /** Окна нет, или оно сказало, что сервер молчит, - но не «подключение…». */
    private fun settled(): Boolean = compose.onAllNodesWithText(ui("start.title")).fetchSemanticsNodes().isEmpty() ||
        compose.onAllNodesWithText(ui("start.retry")).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        /** Меньше предела окна: тест ловит застывшее окно, а не предел, что снимает его. */
        val SETTLE_MS = StartGate.LIMIT_MS - 5_000
    }
}
