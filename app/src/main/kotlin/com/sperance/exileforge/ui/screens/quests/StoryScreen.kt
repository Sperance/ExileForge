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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Quest
import com.sperance.exileforge.rules.content.QuestBoard
import com.sperance.exileforge.rules.content.QuestKind
import com.sperance.exileforge.rules.content.StoryBooks
import com.sperance.exileforge.rules.content.StoryChapter
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
 * ([LawChoice]), взятые законы - под главой. Шаг сюжета сдаётся только здесь и строкой главы на доске заданий, по одному; сданные шаги
 * этой и пройденных глав ([DoneChapters]) касанием открывают их текст «после» заново.
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
                if (board == null) item { MutedText(ui("quest.loading")) } else story(game, vm, activity.busy, board)
            }
        }
    }
}

/**
 * Карточка текущей главы в листе кнопки-тома над картой «Похода»: глава, цель шага, прогресс и награда; нажатие - весь сюжет. Сюжет
 * пройден или доска не прочитана - карточки нет. Доска заданий вместо карточки пишет главу строкой ([StoryLine]).
 *
 * Выбор за пройденную главу - закон трона или книга умения - своей плашкой над карточкой ([ChoicePlaque]); шаг сюжета виден всегда.
 */
@Composable fun StoryCard(game: GameUi, board: QuestBoard?, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val index = game.index ?: return
    board ?: return
    val choice = StoryChoice.pending(index, board, game.heroInfo?.laws.orEmpty())
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        choice?.let { ChoicePlaque(it, onOpen) }
        StepCard(index, board, onOpen)
    }
}

/** Карточка шага сюжета: глава и шаг, цель, прогресс и награда. Шага нет - ничего. */
@Composable private fun StepCard(index: ContentIndex, board: QuestBoard, onOpen: () -> Unit) {
    val chapter = index.quests.story.getOrNull(board.chapter) ?: return
    val quest = board.story ?: return
    val shape = RoundedCornerShape(12.dp)
    val ready = quest.done && !quest.claimed
    Column(Modifier.fillMaxWidth().clip(shape).depthPanel(shape, accent = Gold).clickable(role = Role.Button, onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = HEAD_HEIGHT).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(ForgeGlyphs.Tome, null, tint = GoldBright, modifier = Modifier.size(16.dp))
            Text(
                ui("story.card_chapter", loc(LocaleKey.chapterTitle(chapter.region)), board.step + 1, chapter.steps.size),
                color = Muted,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (ready) Text(ui("story.card_ready"), color = Vital, style = MaterialTheme.typography.labelMedium)
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(questTitle(quest), color = GoldBright, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            QuestBar(quest.progress, quest.target, Gold)
            MutedText(ui("quest.progress", number(quest.progress.toDouble()), number(quest.target.toDouble())))
            RewardChips(quest.reward)
        }
    }
}

/**
 * Глава сюжета на доске заданий - одна строка 48dp: том, цель шага и «глава · шаг N из M», прогресс «12 / 30» (готово - кнопка сдачи
 * шага), шеврон и тонкая полоса прогресса по низу; нажатие - весь сюжет ([onOpen]). Ждёт выбор за пройденную главу - та же строка
 * становится плашкой выбора: знак закона или книги, зов выбрать и имя главы. Сюжет пройден и выбора нет - строки нет. Слот [claim] -
 * кнопка сдачи: вызывается только для готового шага (выполнен и не сдан).
 */
@Composable fun StoryLine(game: GameUi, board: QuestBoard?, onOpen: () -> Unit, modifier: Modifier = Modifier, claim: @Composable (Quest) -> Unit) {
    val index = game.index ?: return
    board ?: return
    val laws = game.heroInfo?.laws.orEmpty()
    val choice = StoryChoice.pending(index, board, laws)
    val chapter = index.quests.story.getOrNull(board.chapter)
    val quest = board.story
    val (title, subtitle) = when {
        choice != null -> ui(choice.text) to choiceRegion(index, board, laws)?.let { loc(LocaleKey.chapterTitle(it)) }
        chapter != null && quest != null -> questTitle(quest) to ui("story.card_chapter", loc(LocaleKey.chapterTitle(chapter.region)), board.step + 1, chapter.steps.size)
        else -> return
    }
    val step = quest?.takeIf { choice == null }
    val shape = RoundedCornerShape(12.dp)
    val frame = if (choice != null) {
        Modifier.glow(Gold, radius = 9.dp, shape = shape).clip(shape).background(Brush.verticalGradient(listOf(PLAQUE_TOP, PLAQUE_BOTTOM)), shape)
            .border(1.dp, GoldBright.copy(alpha = .45f), shape)
    } else {
        Modifier.clip(shape).background(Brush.verticalGradient(listOf(LINE_TOP, LINE_BOTTOM)), shape).border(1.dp, Vital.copy(alpha = .35f), shape)
    }
    Box(modifier.fillMaxWidth().heightIn(min = PLAQUE_HEIGHT).then(frame).clickable(role = Role.Button, onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = PLAQUE_HEIGHT).padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(choice?.icon ?: ForgeGlyphs.Tome, null, tint = GoldBright, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = GoldBright,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (choice != null) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (step != null) {
                if (step.awaitsHand) {
                    claim(step)
                } else {
                    MutedText(ui("quest.progress", number(step.progress.toDouble()), number(step.target.toDouble())), style = MaterialTheme.typography.labelSmall)
                }
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = if (choice != null) GoldBright else Gold, modifier = Modifier.size(20.dp))
        }
        if (step != null) Box(Modifier.align(Alignment.BottomStart)) { QuestBar(step.progress, step.target, Caution, thickness = 2.dp) }
    }
}

/** Глава, чей выбор ждёт: трон без закона раньше книги, как в [StoryChoice.pending]; нет - null. */
private fun choiceRegion(index: ContentIndex, board: QuestBoard, laws: List<String>): String? = ThroneLaws(index).pending(board.chapter, laws).firstOrNull()?.region
    ?: StoryBooks(index).pending(board.chapter, board.books).firstOrNull()?.let { index.quests.story.getOrNull(it)?.region }

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

/** Тёмное стекло строки главы на доске: сверху светлее. */
private val LINE_TOP = Color(0xFF121C21)
private val LINE_BOTTOM = Color(0xFF0F161B)

/** Зелёное стекло плашки выбора: сверху светлее. */
private val PLAQUE_TOP = Color(0xFF2A3427)
private val PLAQUE_BOTTOM = Color(0xFF141C17)

/** Кольцо знака плашки: зелёная сердцевина в тёмной кромке. */
private val RING_CORE = Color(0xFF2F4A3A)
private val RING_EDGE = Color(0xFF121A16)

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
    Box(modifier) {
        ForgeIconButton(
            ForgeGlyphs.Tome,
            ui("story.button"),
            onClick = { open = true },
            modifier = Modifier.drawWithContent {
                drawContent()
                val stroke = 2.dp.toPx()
                val inset = 8.dp.toPx()
                val arc = Size(size.width - 2 * inset, size.height - 2 * inset)
                drawArc(Bronze, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                drawArc(Caution, -90f, 360f * share, false, Offset(inset, inset), arc, style = Stroke(stroke))
            },
            kind = ButtonKind.Secondary,
            accent = if (law) GoldBright else null,
        )
        if (ready) Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(8.dp).clip(CircleShape).background(Vital))
    }
    if (open) {
        ForgeSheet(onDismissRequest = { open = false }) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StoryCard(game, board, onOpen = {
                    open = false
                    vm.openStory()
                })
                ForgeButton(onClick = {
                    open = false
                    vm.openStory()
                }, modifier = Modifier.fillMaxWidth()) { Text(ui("story.open_all")) }
            }
        }
    }
}

/** Высота шапки карточки сюжета: том и «глава · шаг N из M». */
private val HEAD_HEIGHT = 36.dp

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
        item(key = "done") { DoneChapters(chapters, vm::reread) }
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
            index < board.step -> DoneStep(step.code) { vm.reread(board.chapter, step.code) }
            else -> StepLine("· " + loc(LocaleKey.questTitle(QuestKind.STORY, step.code)), Muted)
        }
    }
    item(key = "done") { DoneChapters(chapters.take(board.chapter), vm::reread) }
    item(key = "laws") { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { TakenLaws(index, chosen) } }
    item(key = "codex") { StoryCodex(chapters, board.chapter + 1) }
}

/** Сданный шаг [code]: «✓ имя шага»; касание - перечитать его текст «после» ([onRead]). */
@Composable private fun DoneStep(code: String, onRead: () -> Unit) = StepLine("✓ " + loc(LocaleKey.questTitle(QuestKind.STORY, code)), Vital, onRead)

/** Строка шага главы; сданный шаг [onRead] - касанием перечитать его текст «после». */
@Composable private fun StepLine(text: String, color: Color, onRead: (() -> Unit)? = null) {
    val read = onRead?.let { Modifier.clickable(role = Role.Button, onClick = it) } ?: Modifier
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium, modifier = read.padding(horizontal = 4.dp, vertical = 2.dp))
}

/**
 * Пройденные главы [chapters] (с первой), свёрнутые заголовком, как кодекс: имя главы и её шаги - касание шага перечитывает его текст
 * «после» ([onRead]: номер главы и код шага). Пройденных нет - панели нет.
 */
@Composable private fun DoneChapters(chapters: List<StoryChapter>, onRead: (Int, String) -> Unit) {
    if (chapters.isEmpty()) return
    var open by rememberSaveable { mutableStateOf(false) }
    ForgePanel {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { open = !open }, horizontalArrangement = Arrangement.SpaceBetween) {
            Engraved(ui("story.chapters_done"))
            Text(ui(if (open) "common.collapse" else "common.expand"), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        if (!open) return@ForgePanel
        chapters.forEachIndexed { number, chapter ->
            Text(loc(LocaleKey.chapterTitle(chapter.region)), color = GoldBright, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            chapter.steps.forEach { step -> DoneStep(step.code) { onRead(number, step.code) } }
        }
    }
}
