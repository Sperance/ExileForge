package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** The steps of the warm-up, in the order the loading screen lists them. */
enum class WarmStep { CONTENT, LOCALE, ICONS, HERO, WORLD }

/** The warm-up of one hero: which steps are done, and whether it is over (done, failed or timed out alike). */
data class Warmup(val heroId: String, val done: Set<WarmStep> = emptySet(), val finished: Boolean = false) {
    val progress: Float get() = done.size.toFloat() / WarmStep.entries.size
}

/**
 * The warm-up (3.54.0): entering a hero reads, before the game opens, everything the screens would otherwise ask for one
 * by one — the content, the dictionary, the icons, the hero whole, and the boards the tabs open on (quests, crafts, the
 * merchant) — so the play itself sends few requests and short ones. Every step is kept from the device when it can be;
 * a step that fails does not hold the game back, and after [LIMIT] ms the game opens whatever is left.
 */
class WarmupViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {
    private var job: Job? = null

    fun start() {
        with(runtime) {
            val id = heroId
            if (id.isEmpty() || state.value.play.warmup?.heroId == id) return
            job?.cancel()
            update { it.copy(play = it.play.copy(warmup = Warmup(id))) }
            job = scope.launch {
                withTimeoutOrNull(LIMIT) {
                    step(id, WarmStep.CONTENT) { ensureContent() }
                    coroutineScope {
                        launch { step(id, WarmStep.LOCALE) { loadLocale(state.value.lang) } }
                        launch {
                            step(id, WarmStep.ICONS) {
                                coroutineScope {
                                    launch { loadIcons() }
                                    launch { loadPortraits() }
                                }
                            }
                        }
                        launch { step(id, WarmStep.HERO) { if (state.value.play.heroReadAt == 0L) heroViewModel.readHero() } }
                    }
                    step(id, WarmStep.WORLD) {
                        quests.load()
                        craftsViewModel.load(silent = true)
                        market.loadMerchant()
                    }
                }
                update { s -> s.play.warmup?.takeIf { it.heroId == id }?.let { s.copy(play = s.play.copy(warmup = it.copy(finished = true))) } ?: s }
            }
        }
    }

    private suspend fun step(id: String, step: WarmStep, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) { }
        update { s -> s.play.warmup?.takeIf { it.heroId == id }?.let { s.copy(play = s.play.copy(warmup = it.copy(done = it.done + step))) } ?: s }
    }

    private companion object {
        const val LIMIT = 20_000L
    }
}
