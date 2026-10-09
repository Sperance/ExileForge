package com.sperance.exileforge.ui.screens.quests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.sperance.exileforge.presentation.state.StoryFold
import com.sperance.exileforge.rules.content.QuestBoard
import com.sperance.exileforge.rules.content.ThroneLaws
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/*
 * Сюжет (4.2.0): не раздел доски заданий, а свой экран «Похода», открытый с первого уровня. На карте мира - карточка текущей
 * главы (цель, прогресс, награда); нажатие открывает все главы. «Девять тронов»: у главы - вступление, за её финал - эпилог и
 * выбор закона трона ([LawChoice]), взятые законы - под главой.
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
 * Карточка текущей главы над картой мира и сверху экрана «Задания» (4.3.0) - одна на оба места: глава, цель шага, прогресс и
 * награда; нажатие - весь сюжет. Сюжет пройден или доска не прочитана - карточки нет. С 4.4.x шеврон сворачивает её в строку
 * «глава · задание» с тонкой полосой прогресса ([fold] - из настроек устройства); новый шаг сюжета или награда, что стала ждать,
 * раскрывают её сами ([onSeen] сообщает, какой шаг карточка показала).
 */
@Composable fun StoryCard(
    game: GameUi,
    board: QuestBoard?,
    fold: StoryFold,
    modifier: Modifier = Modifier,
    onFold: (Boolean) -> Unit,
    onSeen: (String, Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    val index = game.index ?: return
    board ?: return
    val shape = RoundedCornerShape(12.dp)
    // Трон пройденной главы ждёт выбора закона - карточка зовёт к нему раньше шага, даже когда сюжет уже пройден
    if (ThroneLaws(index).pending(board.chapter, game.heroInfo?.laws.orEmpty()).isNotEmpty()) {
        Row(
            modifier.fillMaxWidth().clip(shape).depthPanel(shape, accent = GoldBright).clickable(role = Role.Button, onClick = onOpen).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(ForgeGlyphs.Tome, null, tint = GoldBright, modifier = Modifier.size(16.dp))
            Text(ui("story.card_law"), color = GoldBright, style = MaterialTheme.typography.titleSmall)
        }
        return
    }
    val chapter = index.quests.story.getOrNull(board.chapter) ?: return
    val quest = board.story ?: return
    val ready = quest.done && !quest.claimed
    LaunchedEffect(quest.id, ready) { onSeen(quest.id, ready) }
    val collapsed = fold.collapsed && fold.quest == quest.id
    val chapterTitle = loc(LocaleKey.chapterTitle(chapter.region))
    Column(modifier.fillMaxWidth().clip(shape).depthPanel(shape, accent = Gold).clickable(role = Role.Button, onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = FOLDED_HEIGHT).padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(ForgeGlyphs.Tome, null, tint = GoldBright, modifier = Modifier.size(16.dp))
            Text(
                if (collapsed) "$chapterTitle · ${questTitle(quest)}" else ui("story.card_chapter", chapterTitle, board.step + 1, chapter.steps.size),
                color = if (collapsed) GoldBright else Muted,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (ready) Text(ui("story.card_ready"), color = Vital, style = MaterialTheme.typography.labelMedium)
            IconButton(onClick = { onFold(!collapsed) }, modifier = Modifier.size(FOLDED_HEIGHT)) {
                Icon(
                    if (collapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                    ui(if (collapsed) "common.expand" else "common.collapse"),
                    tint = Gold,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (collapsed) {
            QuestBar(quest.progress, quest.target, Gold, thickness = 2.dp)
        } else {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(questTitle(quest), color = GoldBright, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                QuestBar(quest.progress, quest.target, Gold)
                MutedText(ui("quest.progress", number(quest.progress.toDouble()), number(quest.target.toDouble())))
                RewardChips(quest.reward)
            }
        }
    }
}

/** Карточка сюжета над моделью заданий: свёрнутость из настроек, нажатие - весь сюжет. */
@Composable fun StoryCard(game: GameUi, board: QuestBoard?, vm: QuestViewModel, modifier: Modifier = Modifier) {
    val fold by vm.storyFold.collectAsStateWithLifecycle()
    StoryCard(game, board, fold, modifier, onFold = vm::foldStory, onSeen = vm::seeStory, onOpen = vm::openStory)
}

/** Высота свёрнутой карточки сюжета: одна строка с шевроном. */
private val FOLDED_HEIGHT = 36.dp

/**
 * The story: a throne waiting for its law first (the chapter's epilogue and the two laws), then the chapter - its name, its
 * introduction, its steps behind and ahead, the step at hand as a quest, - and the laws already taken.
 */
private fun LazyListScope.story(game: GameUi, vm: QuestViewModel, busy: Boolean, board: QuestBoard) {
    val index = game.index ?: return
    val chapters = index.quests.story
    val laws = ThroneLaws(index)
    val taken = game.heroInfo?.laws.orEmpty()
    laws.pending(board.chapter, taken).forEach { throne ->
        item(key = "law-${throne.region}") { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { LawChoice(index, throne, busy, vm::law) } }
    }
    val chosen = taken.mapNotNull(laws::law)
    val chapter = chapters.getOrNull(board.chapter)
    if (chapter == null) {
        item {
            ForgePanel {
                Engraved(ui("quest.story_done"))
                MutedText(ui("quest.story_done_text"))
            }
        }
        item(key = "laws") { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { TakenLaws(index, chosen) } }
        return
    }
    item {
        ForgePanel {
            Engraved(loc(LocaleKey.chapterTitle(chapter.region)))
            MutedText(ui("quest.chapter_line", loc("region.${chapter.region}.name"), board.step + 1, chapter.steps.size))
            Spacer(Modifier.height(6.dp))
            Text(loc(LocaleKey.chapterIntro(chapter.region)), color = Parchment, style = MaterialTheme.typography.bodyMedium)
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
    item(key = "laws") { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { TakenLaws(index, chosen) } }
}

@Composable private fun StepLine(text: String, color: Color) {
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
}
