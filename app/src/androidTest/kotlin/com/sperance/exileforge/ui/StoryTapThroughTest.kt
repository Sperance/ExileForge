package com.sperance.exileforge.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.hero.HeroHolding
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.quests.StoryAfter
import com.sperance.exileforge.core.quests.StoryOverlay
import com.sperance.exileforge.core.world.World
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.PrologueGate
import com.sperance.exileforge.ui.screens.quests.StoryAfterHost
import com.sperance.exileforge.ui.theme.ForgeTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * Сюжет проходится касаниями до конца: пролог перед созданием героя открывает то, что за ним, а продолжение сданного шага
 * сюжета, дочитанное касаниями, уходит само. Без анимаций абзац виден сразу - касание листает абзацы.
 */
class StoryTapThroughTest {
    @get:Rule val compose = createComposeRule()

    @Before fun dictionary() {
        serverLocale = TestWorld.russian
    }

    @After fun forget() {
        serverLocale = LocaleBundle()
    }

    @Test fun thePrologueIsTappedThrough() {
        compose.setContent {
            CompositionLocalProvider(LocalMotion provides false) {
                ForgeTheme { PrologueGate(always = true) { Text(BEHIND) } }
            }
        }
        check(!shown(BEHIND)) { "the prologue did not cover what is behind it" }
        tapUntil { shown(BEHIND) }
    }

    @Test fun theStepContinuationIsTappedThrough() {
        val index = TestWorld.index
        val step = index.quests.story.first().steps.first()
        GlobalContext.get().get<StoryOverlay>().show(StoryAfter(chapter = 0, step = step.code))
        val game = GameUi(world = World(content = index, contentHash = index.hash), holding = HeroHolding(heroId = "story-tap"))
        compose.setContent {
            CompositionLocalProvider(LocalMotion provides false) {
                ForgeTheme { StoryAfterHost(game) }
            }
        }
        check(scene()) { "the continuation of a turned-in step did not open" }
        tapUntil { !scene() }
    }

    /** Сцена на экране: подсказка «касание - дальше» или «касание - закрыть». */
    private fun scene(): Boolean = shown(ui("story.tap_next")) || shown(ui("story.tap_done"))

    private fun shown(text: String): Boolean = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** Касание по сцене, пока [done] не верно, но не больше [MAX_TAPS]. */
    private fun tapUntil(done: () -> Boolean) {
        repeat(MAX_TAPS) {
            compose.waitForIdle()
            if (done()) return
            compose.onRoot().performClick()
        }
        compose.waitForIdle()
        check(done()) { "the story scene did not end after $MAX_TAPS taps" }
    }

    private companion object {
        const val BEHIND = "behind the prologue"

        /** Касаний с запасом на все абзацы пролога и продолжения. */
        const val MAX_TAPS = 40
    }
}
