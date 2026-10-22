package com.sperance.exileforge.core.hero

import com.sperance.exileforge.core.app.Building
import com.sperance.exileforge.core.app.Feature
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.unlocked
import com.sperance.exileforge.core.campaign.FateKit
import com.sperance.exileforge.core.display.FateCounter
import com.sperance.exileforge.core.display.FateSource
import com.sperance.exileforge.core.expedition.ExpeditionActions
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.content.FateLever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Кнопка к зданию [building], где тратят счёт дара, с подписью [label]. */
data class FateVisit(val building: Building, val label: String)

/**
 * Дар играемого героя для показа: карточка [card], рычаги на силе героя с переносом Разгона [effects] (лента: «Сейчас
 * действует» и сила), подпись счёта [tally] (null - дар ничего не копит) и кнопка к зданию счёта [visit] - только если здание герою
 * открыто.
 */
data class HeroFate(val card: FateCard, val effects: FateEffects, val tally: String?, val visit: FateVisit?)

/**
 * Дар героя одним потоком на приложение: лента листа героя читает его, а не считает сама. Пересчёт - когда меняются
 * герой, контент, язык или открытость зданий, а в заходе с Разгоном - когда меняется число выигранных боёв снимка захода
 * (`RunHud.fateWon`), не на каждый снимок.
 */
class HeroFateModel(slice: GameSlice, expedition: ExpeditionActions, scope: CoroutineScope) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val streak: Flow<RunStreak?> = expedition.run.flatMapLatest { run ->
        run?.takeIf { it.run.fate.has(FateLever.WIN_STREAK) }?.let { r -> r.hud.map { RunStreak(r.run.fate, it.fateWon) } } ?: flowOf(null)
    }.distinctUntilChanged()

    val fate: StateFlow<HeroFate?> = combine(slice.ui.map { Inputs(it) }.distinctUntilChanged(), streak) { inputs, run -> resolve(inputs, run) }
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_MS), null)

    /** Что из среза игры нужно дару: герой, контент, язык подписей и открытые герою здания. */
    private data class Inputs(val hero: HeroView?, val index: ContentIndex?, val lang: Lang, val open: Set<Building>) {
        constructor(ui: GameUi) : this(ui.hero, ui.index, ui.lang, Building.entries.filterTo(mutableSetOf()) { ui.unlocked(Feature.ofBuilding(it)) })
    }

    /** Разгон идущего захода: замороженные рычаги захода [fate] и его выигранные бои [won]. */
    private data class RunStreak(val fate: FateEffects, val won: Int)

    private fun resolve(inputs: Inputs, streak: RunStreak?): HeroFate? {
        val hero = inputs.hero ?: return null
        val index = inputs.index ?: return null
        val fate = index.fates.of(hero.info.fate) ?: return null
        val effects = FateKit.of(index, fate.code, hero.stats, hero.equipped.values, hero.info.fateCarry).effects
        val source = streak?.let { FateSource(hero.info, it.fate, it.won) } ?: FateSource(hero.info, effects)
        val tally = FateCounter.of(source)
        val visit = tally?.let { FateCounter.of(source.effects)?.visit() }?.takeIf { it.building in inputs.open }
        return HeroFate(FateCard.of(fate), effects, tally, visit)
    }

    /** Где тратят счёт дара: трофеи - у торговца; прочие счета тратить негде. */
    private fun FateCounter.visit(): FateVisit? = when (this) {
        FateCounter.TROPHIES -> FateVisit(Building.MERCHANT, ui("fate.visit.merchant"))
        FateCounter.BOUNTY, FateCounter.STREAK -> null
    }

    private companion object {
        /** Поток живёт столько после последнего читателя: поворот экрана не пересобирает дар. */
        const val STOP_MS = 5_000L
    }
}
