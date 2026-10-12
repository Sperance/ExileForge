package com.sperance.exileforge.ui.screens.quests

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.quests.QuestRepository
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.settings.SettingsViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.QuestKind
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.ui.components.StoryScene
import com.sperance.exileforge.ui.components.ToastHost
import com.sperance.exileforge.ui.components.storyParagraphs
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Продолжение сюжета (4.7.0): шаг сюжета героя выполнен - один раз на устройстве поверх игры встаёт сюжетный экран с его
 * продолжением (`quest.story.<шаг>.after`), у финала главы - и с эпилогом главы. Показанное отмечается `герой:шаг`
 * ([com.sperance.exileforge.presentation.state.GameSettings.storySeen]); запросов к серверу нет - доска заданий уже на клиенте.
 * Тосты (4.9.0: «отчёт отправлен») видны и поверх сцены.
 */
@Composable fun StoryAfterHost(game: GameUi) {
    val board = koinInject<QuestRepository>().state.collectAsStateWithLifecycle().value.board ?: return
    val settings = koinViewModel<SettingsViewModel>()
    val set by settings.settings.collectAsStateWithLifecycle()
    val story = board.story?.takeIf { it.done } ?: return
    val seen = "${game.heroId}:${story.goal}"
    if (game.heroId.isBlank() || seen in set.storySeen) return
    val chapter = game.index?.quests?.story?.getOrNull(board.chapter) ?: return
    val finale = chapter.steps.lastOrNull()?.code == story.goal
    val paragraphs = storyParagraphs(loc(LocaleKey.storyAfter(story.goal))) + if (finale) storyParagraphs(loc(LocaleKey.chapterOutro(chapter.region))) else emptyList()
    if (paragraphs.isEmpty()) return
    StoryScene(
        kicker = loc(LocaleKey.chapterTitle(chapter.region)),
        title = loc(LocaleKey.questTitle(QuestKind.STORY, story.goal)),
        paragraphs = paragraphs,
        skippable = true,
        onDone = { settings.change { copy(storySeen = storySeen + seen) } },
        overlay = {
            val shell = koinViewModel<ShellViewModel>()
            ToastHost(game, shell::dismissMessage, shell::dismissNotice, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp))
        },
    )
}
