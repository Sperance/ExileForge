package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * What every feature of the one [ForgeRuntime] shares: the repositories it writes and the hero it acts for.
 * Since 3.80.41 there is no shared state: a feature reads and writes its repositories.
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
