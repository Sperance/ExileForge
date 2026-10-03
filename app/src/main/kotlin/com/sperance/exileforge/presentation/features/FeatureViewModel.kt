package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.ForgeState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * What every feature of the one [ForgeRuntime] shares: the state it reads, the one way it changes it,
 * and the hero it acts for. A feature keeps nothing a screen draws; that all lives in [ForgeState].
 */
abstract class FeatureViewModel(protected val runtime: ForgeRuntime) {
    protected val state: StateFlow<ForgeState> get() = runtime.state
    protected val sessions get() = runtime.sessions
    protected val world get() = runtime.world
    protected val commands get() = runtime.commands

    /** The hero on screen, as every hero route names it. */
    protected val heroId: String get() = state.value.play.heroId.trim()

    /** Whether [id] is the hero on screen: an answer about any other one is not drawn. */
    protected fun onScreen(id: String): Boolean = id == heroId

    protected fun update(transform: (ForgeState) -> ForgeState) = runtime.mutable.update(transform)
}
