package com.sperance.exileforge.core.hero

import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.sheet.StatSheetModel
import com.sperance.exileforge.rules.content.ContentIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/** Сводка героя [heroId] ([StatSheetModel]): экран сверяет героя, чтобы не показать на миг лист прежнего. */
data class HeroSheet(val heroId: String, val sheet: StatSheetModel)

/**
 * Сводка вкладки «Герой» одним потоком на приложение: считается на фоновом потоке, когда меняются герой, контент или язык, - не в
 * кадре и не на каждый выпуск среза игры. Последняя сводка держится и после ухода с вкладки: возврат на неё рисует строки сразу.
 */
class HeroSheetModel(slice: GameSlice, scope: CoroutineScope) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val sheet: StateFlow<HeroSheet?> = slice.ui
        .distinctUntilChangedBy(::Inputs)
        .mapLatest { game -> StatSheetModel.hero(game)?.let { HeroSheet(game.heroId, it) } }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_MS), null)

    /** Что из среза игры нужно сводке: герой, контент и язык подписей. */
    private data class Inputs(val hero: HeroView?, val index: ContentIndex?, val lang: Lang) {
        constructor(ui: GameUi) : this(ui.hero, ui.index, ui.lang)
    }

    private companion object {
        /** Поток живёт столько после последнего читателя: поворот экрана и быстрый возврат на вкладку не пересчитывают сводку. */
        const val STOP_MS = 5_000L
    }
}
