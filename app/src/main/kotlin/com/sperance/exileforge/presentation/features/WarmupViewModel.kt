package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val mutable = MutableStateFlow<Warmup?>(null)

    /** Прогрев героя на экране (3.80.32: свой поток вместо общего состояния). */
    val state: StateFlow<Warmup?> = mutable.asStateFlow()

    /** Выход из героя: следующий вход прогревается заново. */
    fun clear() {
        job?.cancel()
        mutable.value = null
    }

    fun start() {
        with(runtime) {
            val id = heroId
            if (id.isEmpty() || mutable.value?.heroId == id) return
            job?.cancel()
            mutable.value = Warmup(id)
            job = scope.launch {
                withTimeoutOrNull(LIMIT) {
                    step(id, WarmStep.CONTENT) { ensureContent() }
                    coroutineScope {
                        launch { step(id, WarmStep.LOCALE) { loadLocale(languages.lang.value) } }
                        launch {
                            step(id, WarmStep.ICONS) {
                                coroutineScope {
                                    launch { loadIcons() }
                                    launch { loadPortraits() }
                                }
                            }
                        }
                        launch { step(id, WarmStep.HERO) { if (heroes.state.value.readAt == 0L) heroSync.readHero() } }
                    }
                    step(id, WarmStep.WORLD) {
                        quests.load()
                        crafts.load(silent = true)
                        market.loadMerchant()
                    }
                }
                mutable.update { w -> w?.takeIf { it.heroId == id }?.copy(finished = true) ?: w }
            }
        }
    }

    private suspend fun step(id: String, step: WarmStep, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) { }
        mutable.update { w -> w?.takeIf { it.heroId == id }?.let { it.copy(done = it.done + step) } ?: w }
    }

    private companion object {
        const val LIMIT = 20_000L
    }
}
