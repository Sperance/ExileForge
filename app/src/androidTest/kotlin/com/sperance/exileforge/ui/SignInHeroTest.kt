package com.sperance.exileforge.ui

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.MainActivity
import com.sperance.exileforge.ui.nav.Navigator
import com.sperance.exileforge.ui.nav.Route
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * Вход открывает экран героя без падения: стек навигатора уходит с экрана без оболочки (вход) на экран в оболочке (герой) - как
 * после входа игрока, - и памяти вкладок ключ экрана героя достаётся один раз. Повтор гоняет смену туда и обратно.
 */
class SignInHeroTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun signInOpensTheHeroScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val navigator = GlobalContext.get().get<Navigator>()
            repeat(SWAPS) {
                onMain { navigator.reset(Route.Auth) }
                compose.waitForIdle()
                onMain { navigator.reset(Route.Hero) }
                compose.waitForIdle()
                check(navigator.stack.toList() == listOf(Route.Hero)) { "the hero screen is not on top after sign-in" }
            }
            onMain { navigator.reset(Route.Auth) }
            compose.waitForIdle()
        }
    }

    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    private companion object {
        /** Смен «вход → герой» с запасом: шов между кадрами стека и оболочки ловится не с первой. */
        const val SWAPS = 3
    }
}
