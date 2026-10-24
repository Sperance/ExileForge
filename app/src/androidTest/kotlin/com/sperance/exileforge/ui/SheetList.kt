package com.sperance.exileforge.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.StatFigures
import com.sperance.exileforge.core.sheet.StatExplainer
import com.sperance.exileforge.core.sheet.StatSheetModel
import com.sperance.exileforge.ui.screens.hero.rememberStatSheetState
import com.sperance.exileforge.ui.screens.hero.statSheet

/** Сводка листа [figures] героя игры [game] (null - сводка самого героя) ленивым списком высотой [SHEET_HEIGHT] - для снимков. */
@Composable fun SheetList(game: GameUi, figures: StatFigures? = null) {
    val index = game.index ?: return
    val model = figures?.let { StatSheetModel.of(it, index, game.hero?.level, game.lang, emptyList(), null, StatExplainer(game)) } ?: StatSheetModel.hero(game) ?: return
    val state = rememberStatSheetState()
    LazyColumn(Modifier.fillMaxWidth().height(SHEET_HEIGHT)) { statSheet(model, state, null, index) }
}

/** Высота списка сводки на снимке: ленивый список не меряется внутри прокрутки без своей высоты. */
private val SHEET_HEIGHT = 720.dp
