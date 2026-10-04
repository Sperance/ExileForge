package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.ForgeState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * What every feature of the one [ForgeRuntime] shares: the repositories it writes and the hero it acts for.
 * Since 3.80.32 the shared [ForgeState] is a projection nobody writes; a feature reads its sources.
 */
abstract class FeatureViewModel(protected val runtime: ForgeRuntime) {
    protected val sessions get() = runtime.sessions
    protected val world get() = runtime.world
    protected val commands get() = runtime.commands
    protected val links get() = runtime.links

    /** The hero on screen, as every hero route names it. */
    protected val heroId: String get() = runtime.heroes.state.value.heroId.trim()

    /** Whether [id] is the hero on screen: an answer about any other one is not drawn. */
    protected fun onScreen(id: String): Boolean = id == heroId
}
