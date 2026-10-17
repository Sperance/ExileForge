package com.sperance.exileforge.ui.screens.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.quests.QuestViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.StoryFold
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.QuestBoard
import com.sperance.exileforge.rules.content.StoryBooks
import com.sperance.exileforge.rules.content.ThroneLaws
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Весь сюжет: глава, её шаги - пройденные, текущий заданием и будущие; «назад» - на карту «Похода».
 *
 * Сюжет - свой экран «Похода», открытый с первого уровня. «Девять тронов»: у главы вступление, за её финал - эпилог и выбор закона трона
 * ([LawChoice]), взятые законы - под главой.
 */
@Composable fun StoryScreen() {
    val vm = koinViewModel<QuestViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val quests by vm.quests.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId, game.sessionEpoch) { if (game.heroId.isNotBlank()) vm.open() }
    val board = quests.board
    BackTrailHost(ui("expedition.title")) {
        TrailLevel(ui("story.title"), onBack = vm::back) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { ScreenHeader(ui("story.title"), ui("story.subtitle"), ForgeGlyphs.Tome) }
                if (board == null) item { MutedText(ui("quest.loading")) } else story(game, vm, activity.busy, board)
            }
        }
    }
}

/**
 * Карточка текущей главы над картой мира и сверху экрана «Задания» - одна на оба места: глава, цель шага, прогресс и
 * награда; нажатие - весь сюжет. Сюжет пройден или доска не прочитана - карточки нет. Шеврон сворачивает её в строку
 * «глава · задание» с тонкой полосой прогресса ([fold] - из настроек устройства); новый шаг сюжета или награда, что стала ждать,
 * раскрывают её сами ([onSeen] сообщает, какой шаг карточка показала).
 *
 * Выбор за пройденную главу - закон трона или книга умения - своей плашкой над карточкой ([ChoicePlaque]); шаг сюжета виден всегда.
 */
@Composable fun StoryCard(
    game: GameUi,
    board: QuestBoard?,
    fold: StoryFold,
    modifier: Modifier = Modifier,
    onFold: (Boolean) -> Unit,
    onSeen: (String, Boolean) -> Unit,
    onOpen: () -> Unit,
    folding: Boolean = true,
) {
    val index = game.index ?: return
    board ?: return
    val choice = StoryChoice.pending(index, board, game.heroInfo?.laws.orEmpty())
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        choice?.let { ChoicePlaque(it, onOpen) }
        StepCard(index, board, fold, onFold, onSeen, onOpen, folding)
    }
}

/** Карточка шага сюжета: глава и шаг, цель, прогресс и награда; свёрнутая - строкой с тонкой полосой. Шага нет - ничего. */
@Composable private fun StepCard(
    index: ContentIndex,
    board: QuestBoard,
    fold: StoryFold,
    onFold: (Boolean) -> Unit,
    onSeen: (String, Boolean) -> Unit,
    onOpen: () -> Unit,
    folding: Boolean,
) {
    val chapter = index.quests.story.getOrNull(board.chapter) ?: return
    val quest = board.story ?: return
    val shape = RoundedCornerShape(12.dp)
    val ready = quest.done && !quest.claimed
    LaunchedEffect(quest.id, ready) { onSeen(quest.id, ready) }
    val collapsed = fold.collapsed && fold.quest == quest.id
    val chapterTitle = loc(LocaleKey.chapterTitle(chapter.region))
    Column(Modifier.fillMaxWidth().clip(shape).depthPanel(shape, accent = Gold).clickable(role = Role.Button, onClick = onOpen)) {
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
            if (collapsed) MutedText(ui("quest.progress", number(quest.progress.toDouble()), number(quest.target.toDouble())), style = MaterialTheme.typography.labelSmall)
            if (folding) {
                IconButton(onClick = { onFold(!collapsed) }, modifier = Modifier.size(FOLDED_HEIGHT)) {
                    Icon(
                        if (collapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                        ui(if (collapsed) "common.expand" else "common.collapse"),
                        tint = Gold,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        if (collapsed) {
            QuestBar(quest.progress, quest.target, Caution, thickness = 2.dp)
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

/**
 * Выбор, что ждёт героя за пройденную главу: закон трона ([LAW]) или книга умения ([BOOK]); [text] - строка плашки, [icon] - знак в
 * кольце. Закон - раньше книги, как на экране сюжета.
 */
internal enum class StoryChoice(val text: String, val icon: ImageVector) {
    LAW("story.card_law", ForgeGlyphs.Keep),
    BOOK("story.card_book", ForgeGlyphs.Grimoire),
    ;

    companion object {
        /** Что ждёт выбора на главе [board] при взятых законах [laws]; ничего - null. */
        fun pending(index: ContentIndex, board: QuestBoard, laws: List<String>): StoryChoice? = when {
            ThroneLaws(index).pending(board.chapter, laws).isNotEmpty() -> LAW
            StoryBooks(index).pending(board.chapter, board.books).isNotEmpty() -> BOOK
            else -> null
        }
    }
}

/**
 * Плашка выбора над шагом сюжета: знак в светлом кольце, строка «Трон взят - выберите его закон» (или книги) и шеврон; касание -
 * экран сюжета с выбором. Зелёное стекло с светлой кромкой и ореолом - зовёт раньше шага, но шаг не прячет.
 */
@Composable private fun ChoicePlaque(choice: StoryChoice, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = PLAQUE_HEIGHT).glow(Gold, radius = 9.dp, shape = shape).clip(shape)
            .background(Brush.verticalGradient(listOf(PLAQUE_TOP, PLAQUE_BOTTOM)), shape).border(1.dp, GoldBright.copy(alpha = .45f), shape)
            .clickable(role = Role.Button, onClick = onOpen).padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(Brush.radialGradient(listOf(RING_CORE, RING_EDGE))).border(1.dp, GoldBright.copy(alpha = .5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(choice.icon, null, tint = GoldBright, modifier = Modifier.size(18.dp)) }
        Text(ui(choice.text), color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null, tint = GoldBright, modifier = Modifier.size(20.dp))
    }
}

/** Высота плашки выбора: строка и кольцо. */
private val PLAQUE_HEIGHT = 48.dp

/** Зелёное стекло плашки выбора: сверху светлее. */
private val PLAQUE_TOP = Color(0xFF2A3427)
private val PLAQUE_BOTTOM = Color(0xFF141C17)

/** Кольцо знака плашки: зелёная сердцевина в тёмной кромке. */
private val RING_CORE = Color(0xFF2F4A3A)
private val RING_EDGE = Color(0xFF121A16)

/** Карточка сюжета над моделью заданий: свёрнутость из настроек, нажатие - весь сюжет. */
@Composable fun StoryCard(game: GameUi, board: QuestBoard?, vm: QuestViewModel, modifier: Modifier = Modifier) {
    val fold by vm.storyFold.collectAsStateWithLifecycle()
    StoryCard(game, board, fold, modifier, onFold = vm::foldStory, onSeen = vm::seeStory, onOpen = vm::openStory)
}

/**
 * Кнопка сюжета над картой «Похода»: том в кольце прогресса шага, точка `Vital` -
 * награда ждёт, свечение - трон ждёт закона. Нажатие - лист с текущим шагом: глава, цель, прогресс, награда и «Весь сюжет».
 * Сюжет пройден и закон выбран - кнопки нет.
 */
@Composable fun StoryButton(game: GameUi, board: QuestBoard?, vm: QuestViewModel, modifier: Modifier = Modifier) {
    val index = game.index ?: return
    board ?: return
    val law = StoryChoice.pending(index, board, game.heroInfo?.laws.orEmpty()) != null
    val quest = board.story
    if (!law && (quest == null || index.quests.story.getOrNull(board.chapter) == null)) return
    var open by remember { mutableStateOf(false) }
    val ready = law || quest?.let { it.done && !it.claimed } == true
    val share = quest?.let { (it.progress.toFloat() / it.target.coerceAtLeast(1)).coerceIn(0f, 1f) } ?: 1f
    val shape = RoundedCornerShape(12.dp)
    Box(modifier.size(40.dp)) {
        Box(
            Modifier.fillMaxSize().clip(shape).background(Abyss.copy(alpha = .88f)).border(1.dp, (if (law) GoldBright else Gold).copy(alpha = .5f), shape)
                .clickable(role = Role.Button, onClickLabel = ui("story.button")) { open = true },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(progress = { share }, Modifier.size(32.dp), color = Caution, trackColor = Bronze, strokeWidth = 2.dp)
            Icon(ForgeGlyphs.Tome, ui("story.button"), tint = GoldBright, modifier = Modifier.size(16.dp))
        }
        if (ready) Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(8.dp).clip(CircleShape).background(Vital))
    }
    if (open) {
        ForgeSheet(onDismissRequest = { open = false }) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StoryCard(game, board, StoryFold(), onFold = {}, onSeen = vm::seeStory, onOpen = {
                    open = false
                    vm.openStory()
                }, folding = false)
                ForgeButton(onClick = {
                    open = false
                    vm.openStory()
                }, modifier = Modifier.fillMaxWidth()) { Text(ui("story.open_all")) }
            }
        }
    }
}

/** Высота свёрнутой карточки сюжета: одна строка с шевроном. */
private val FOLDED_HEIGHT = 36.dp

/**
 * The story: a throne waiting for its law first (the chapter's epilogue and the two laws), then the chapter - its name, its
 * introduction, its steps behind and ahead, the step at hand as a quest, - and the laws already taken.
 *
 * Книга за пройденную главу - после закона трона, по одной на главу.
 */
private fun LazyListScope.story(game: GameUi, vm: QuestViewModel, busy: Boolean, board: QuestBoard) {
    val index = game.index ?: return
    val chapters = index.quests.story
    val laws = ThroneLaws(index)
    val taken = game.heroInfo?.laws.orEmpty()
    laws.pending(board.chapter, taken).forEach { throne ->
        item(key = "law-${throne.region}") { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { LawChoice(index, throne, busy, vm::law) } }
    }
    val hero = game.hero
    if (hero != null) {
        StoryBooks(index).pending(board.chapter, board.books).forEach { chapter ->
            item(key = "book-$chapter") { BookChoice(index, chapter, hero.heroClass, hero.skills, busy, vm::book) }
        }
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
        item(key = "codex") { StoryCodex(chapters, chapters.size) }
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
    item(key = "codex") { StoryCodex(chapters, board.chapter + 1) }
}

@Composable private fun StepLine(text: String, color: Color) {
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
}
