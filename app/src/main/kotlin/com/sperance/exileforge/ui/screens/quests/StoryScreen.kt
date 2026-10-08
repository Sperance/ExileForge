package com.sperance.exileforge.ui.screens.quests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.quests.QuestViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.QuestBoard
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/*
 * Сюжет (4.2.0): не раздел доски заданий, а свой экран «Похода», открытый с первого уровня. На карте мира - карточка текущей
 * главы (цель, прогресс, награда); нажатие открывает все главы.
 */

/** Весь сюжет: глава, её шаги - пройденные, текущий заданием и будущие; «назад» - на карту «Похода». */
@Composable fun StoryScreen() {
    val vm = koinViewModel<QuestViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val quests by vm.quests.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId, game.sessionEpoch) { if (game.heroId.isNotBlank()) vm.open() }
    val board = quests.board
    Column(Modifier.fillMaxSize()) {
        BackRow(ui("expedition.title"), vm::back)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { ScreenHeader(ui("story.title"), ui("story.subtitle"), ForgeGlyphs.Tome) }
            if (board == null) item { MutedText(ui("quest.loading")) } else story(game, vm, activity.busy, board)
        }
    }
}

/**
 * Карточка текущей главы над картой мира: глава, цель шага, прогресс и награда; нажатие - весь сюжет. Сюжет пройден или
 * доска не прочитана - карточки нет.
 */
@Composable fun StoryCard(game: GameUi, board: QuestBoard?, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val chapter = board?.let { game.index?.quests?.story?.getOrNull(it.chapter) } ?: return
    val quest = board.story ?: return
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).depthPanel(shape, accent = Gold).clickable(role = Role.Button, onClick = onOpen).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Icon(ForgeGlyphs.Tome, null, tint = GoldBright, modifier = Modifier.size(16.dp))
            Text(
                ui("story.card_chapter", loc("quest.chapter.${chapter.region}"), board.step + 1, chapter.steps.size),
                color = Muted,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (quest.done && !quest.claimed) Text(ui("story.card_ready"), color = Vital, style = MaterialTheme.typography.labelMedium)
        }
        Text(questTitle(quest), color = GoldBright, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        QuestBar(quest.progress, quest.target, Gold)
        MutedText(ui("quest.progress", number(quest.progress.toDouble()), number(quest.target.toDouble())))
        RewardChips(quest.reward)
    }
}

/** The story: the chapter's name, its steps behind and ahead, and the step at hand as a quest. */
private fun LazyListScope.story(game: GameUi, vm: QuestViewModel, busy: Boolean, board: QuestBoard) {
    val chapters = game.index?.quests?.story.orEmpty()
    val chapter = chapters.getOrNull(board.chapter)
    if (chapter == null) {
        item {
            ForgePanel {
                Engraved(ui("quest.story_done"))
                MutedText(ui("quest.story_done_text"))
            }
        }
        return
    }
    item {
        ForgePanel {
            Engraved(loc("quest.chapter.${chapter.region}"))
            MutedText(ui("quest.chapter_line", loc("region.${chapter.region}.name"), board.step + 1, chapter.steps.size))
        }
    }
    items(chapter.steps.withIndex().toList(), key = { it.value.code }) { (index, step) ->
        val current = board.story?.takeIf { index == board.step && it.goal == step.code }
        when {
            current != null -> QuestRow(current) { ClaimButton(vm, busy, current) }
            index < board.step -> StepLine("✓ " + loc("quest.story.${step.code}.name"), Vital)
            else -> StepLine("· " + loc("quest.story.${step.code}.name"), Muted)
        }
    }
}

@Composable private fun StepLine(text: String, color: Color) {
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
}
