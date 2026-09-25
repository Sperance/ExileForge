package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.ForgeState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * What every feature of the one [ForgeRuntime] shares: the state it reads, the one way it changes
 * it, and the character it acts for. A feature keeps nothing a screen draws; that all lives in
 * [ForgeState], and commands and reads go through the runtime's `task` and `read`.
 */
abstract class FeatureViewModel(protected val runtime: ForgeRuntime) {
    protected val state: StateFlow<ForgeState> get() = runtime.state

    /** The character on screen, as every character route names it. */
    protected val characterId: String get() = state.value.play.characterId.trim()

    /** Whether [id] is the character on screen: an answer about any other one is not drawn. */
    protected fun onScreen(id: String): Boolean = id == characterId

    protected fun update(transform: (ForgeState) -> ForgeState) = runtime.mutable.update(transform)
}
