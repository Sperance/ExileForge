package com.sperance.exileforge.ui.screens.quests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.quests.QuestRepository
import com.sperance.exileforge.core.quests.StoryAfter
import com.sperance.exileforge.rules.content.QuestKind
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.ui.components.StoryScene
import com.sperance.exileforge.ui.components.storyParagraphs
import org.koin.compose.koinInject

/**
 * Продолжение сюжета поверх игры: шаг, который игрок сдал или перечитывает из «Всего сюжета» ([QuestRepository.story]), - сюжетный
 * экран с его текстом «после» (`quest.story.<шаг>.after`). У финала главы за ним второй страницей - эпилог главы
 * (`quest.chapter_outro.<регион>`); «Пропустить» закрывает обе. Окно, ушедшее с экрана любым путём (дочитано, пропущено, заход,
 * выход), считается показанным: на устройстве ничего не хранится. Тосты видны и поверх сцены - их слой общий
 * ([com.sperance.exileforge.ui.components.ToastLayer]).
 */
@Composable fun StoryAfterHost(game: GameUi) {
    val quests = koinInject<QuestRepository>()
    val after by quests.story.collectAsStateWithLifecycle()
    val shown = after ?: return
    DisposableEffect(shown) { onDispose { quests.told(shown) } }
    val pages = remember(shown, game.index) { storyPages(game, shown) }
    if (pages.isEmpty()) {
        LaunchedEffect(shown) { quests.told(shown) }
        return
    }
    var page by remember(shown) { mutableIntStateOf(0) }
    val current = pages[page.coerceAtMost(pages.lastIndex)]
    StoryScene(
        kicker = current.kicker,
        title = current.title,
        paragraphs = current.paragraphs,
        skippable = true,
        onDone = { if (page < pages.lastIndex) page++ else quests.told(shown) },
        onSkip = { quests.told(shown) },
    )
}

/** Страница окна сюжета: строка над заголовком [kicker], заголовок [title] и абзацы [paragraphs]. */
private data class StoryPage(val kicker: String, val title: String, val paragraphs: List<String>)

/** Страницы окна шага [after]: текст «после» шага и, у финала главы, эпилог главы; страницы без текста пропускаются. */
private fun storyPages(game: GameUi, after: StoryAfter): List<StoryPage> {
    val chapter = game.index?.quests?.story?.getOrNull(after.chapter) ?: return emptyList()
    val kicker = loc(LocaleKey.chapterTitle(chapter.region))
    val step = StoryPage(kicker, loc(LocaleKey.questTitle(QuestKind.STORY, after.step)), storyParagraphs(loc(LocaleKey.storyAfter(after.step))))
    val outro = StoryPage(kicker, loc(LocaleKey.regionName(chapter.region)), storyParagraphs(loc(LocaleKey.chapterOutro(chapter.region))))
    val finale = chapter.steps.lastOrNull()?.code == after.step
    return listOfNotNull(step, outro.takeIf { finale }).filter { it.paragraphs.isNotEmpty() }
}
