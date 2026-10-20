package com.sperance.exileforge.ui.screens.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.MAX_CHARACTERS
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.core.model.hero.HeroSummary
import com.sperance.exileforge.core.network.SanctionKind
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.text.NameCharset
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.server.noticeLine
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Choosing who to play, and the only place that choice is ever made.
 *
 * Every screen behind the gate acts on the chosen hero, which is what lets them stop asking
 * which one. Coming back here is the way to swap — a tab never moves the player onto another hero.
 *
 * An account with nothing to choose between does not get a chooser: the creation form opens
 * straight away, because an empty list is not a decision.
 *
 * Язык - до игры: настройки недоступны, пока герой не выбран.
 */
@Composable fun CharacterSelectScreen() {
    val game by koinViewModel<CharactersViewModel>().game.collectAsStateWithLifecycle()
    val vm = koinViewModel<CharactersViewModel>()
    val empty = game.session.charactersRead && game.session.characters.isEmpty()
    var creating by rememberSaveable(empty) { mutableStateOf(empty) }
    var pendingDelete by remember { mutableStateOf<Pair<HeroSummary, HeroDeletion>?>(null) }
    val preview by vm.deletionPreview.collectAsStateWithLifecycle()
    LaunchedEffect(creating) { if (creating) vm.ensureClasses() }
    Scaffold(containerColor = Ink) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Column(Modifier.fillMaxSize().voidBackdrop()) {
                if (game.busy || game.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)
                if (creating) {
                    CreatingColumn(game, vm, onBack = { creating = false }, onSignOut = vm::logout)
                } else {
                    CharacterMenu(
                        game,
                        onPlay = vm::enterCharacter,
                        onDelete = { hero ->
                            pendingDelete = hero to HeroDeletion.MARK
                            vm.previewDeletion(hero.id)
                        },
                        onRestore = { vm.restoreCharacter(it.id) },
                        onErase = { pendingDelete = it to HeroDeletion.ERASE },
                        onCreate = { creating = true },
                        onRefresh = vm::refreshCharacters,
                        onLogout = vm::logout,
                    )
                }
            }
            Row(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                LanguageButton(game.lang, game.world.languages, enabled = !game.busy, onLanguage = vm::language)
            }
        }
    }
    pendingDelete?.let { (doomed, mode) ->
        val close = {
            pendingDelete = null
            vm.closeDeletion()
        }
        HeroDeletionSheet(doomed, mode, preview, game.busy, onDismiss = close) { name ->
            when (mode) {
                HeroDeletion.MARK -> vm.markDeletion(doomed.id, name)
                HeroDeletion.ERASE -> vm.eraseCharacter(doomed.id, name)
            }
        }
    }
}

/**
 * The menu itself, driven by callbacks so it can be shown without a view model.
 *
 * What is worth checking here is which hero a tap plays and how many slots are left — not the
 * wiring behind them.
 *
 * Список перечитывается потягиванием; герои - строками, под ними - создание героя с занятыми местами. Предначертание - лентой над героями.
 */
@Composable internal fun ColumnScope.CharacterMenu(
    game: GameUi,
    onPlay: (String) -> Unit,
    onDelete: (HeroSummary) -> Unit,
    onRestore: (HeroSummary) -> Unit = {},
    onErase: (HeroSummary) -> Unit = {},
    onCreate: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    PullToRefreshBox(isRefreshing = game.refreshing(Reads.CHARACTERS), onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            accountFate(game)?.let { fate -> item(key = "fate") { FateRibbon(fate, ui("fate.ribbon.account"), Modifier.fillMaxWidth()) } }
            items(game.session.characters, key = { it.id }) { character ->
                CharacterCard(
                    game,
                    character,
                    onPlay = { onPlay(character.id) },
                    onDelete = { onDelete(character) },
                    onRestore = { onRestore(character) },
                    onErase = { onErase(character) },
                )
            }
            item { CreateHeroButton(game, onCreate) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(game.session.title, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    ForgeButton(state = ButtonState.of(!game.busy), onClick = onLogout, kind = ButtonKind.Text) { Text(ui("chars.sign_out")) }
                }
            }
        }
    }
}

/**
 * «+ Создать героя» во всю ширину с занятыми местами «N/M» справа; мест нет - кнопка погашена, касание объясняет, как освободить место.
 * Сколько мест держит аккаунт, говорят правила, до их чтения - своё число клиента.
 */
@Composable private fun CreateHeroButton(game: GameUi, onCreate: () -> Unit) {
    val total = game.index?.rules?.maxCharacters ?: MAX_CHARACTERS
    val full = game.characterSlotsLeft <= 0
    var explain by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        ForgeButton(
            state = ButtonState.of(!game.busy && !full),
            onClick = onCreate,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Outlined.Add,
            onDisabledClick = if (full) ({ explain = true }) else null,
        ) {
            Text(ui("editor.create_character"), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(10.dp))
            Text("${(total - game.characterSlotsLeft).coerceIn(0, total)}/$total", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, modifier = Modifier.alpha(.75f))
        }
        if (explain) TipCallout(Tip(body = ui("chars.slots_full"), tint = Muted)) { explain = false }
    }
}

/** Предначертание аккаунта карточкой, `UserProfile.fate`; не выбрано или нет в контенте - null. */
private fun accountFate(game: GameUi): FateCard? = game.index?.fates?.of(game.session.profile?.fate)?.let(FateCard::of)

/**
 * The creation form on its own page: there is nothing to choose between while it is open.
 *
 * Пролог перед каждым созданием героя; пропустить можно.
 */
@Composable private fun ColumnScope.CreatingColumn(game: GameUi, vm: CharactersViewModel, onBack: () -> Unit, onSignOut: () -> Unit) {
    com.sperance.exileforge.ui.components.PrologueGate(always = true) { CreatingList(game, vm, onBack, onSignOut) }
}

/**
 * Форма создания героя. У аккаунта с героями сверху - цепочка «← Герои» (и системный «назад») к списку; без героев вернуться некуда,
 * поэтому под выбором класса - вход под другим аккаунтом. «В изгнание» закреплена внизу, форма над ней прокручивается.
 */
@Composable private fun ColumnScope.CreatingList(game: GameUi, vm: CharactersViewModel, onBack: () -> Unit, onSignOut: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val classes = game.index?.classes?.classes.orEmpty()
    val draft by vm.draftClass.collectAsStateWithLifecycle()
    val heroClass = draft.takeIf { code -> classes.any { it.code == code } } ?: classes.firstOrNull()?.code.orEmpty()
    val canGoBack = game.session.characters.isNotEmpty()
    if (canGoBack) {
        BackHandler(onBack = onBack)
        HeroesTrail(onBack)
    }
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CreateCharacterPanel(game, vm, name, { name = it }, classes.map { it.code }, heroClass) }
        if (!canGoBack) {
            item {
                ForgeButton(state = ButtonState.of(!game.busy), onClick = onSignOut, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Text) {
                    Text(ui("chars.other_account"))
                }
            }
        }
    }
    ForgeButton(
        state = ButtonState.of(!game.busy && name.isNotBlank() && heroClass.isNotBlank()),
        onClick = { vm.createCharacter(name, heroClass) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        size = ButtonSize.Wide,
    ) {
        Text(ui("chars.create"))
    }
}

/** Цепочка «← Герои» над формой создания: стрелка и звено ведут к списку героев. */
@Composable private fun HeroesTrail(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(40.dp).padding(start = 4.dp, end = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        ForgeIconButton(Icons.AutoMirrored.Outlined.ArrowBack, ui("common.back"), onClick = onBack, accent = Gold)
        Text(ui("chars.back"), color = Muted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable(role = Role.Button, onClick = onBack))
    }
}

/**
 * One hero, as the menu describes them: the name, the class and how far they have come.
 *
 * The row carries the class as a code; the server's dictionary names it, and the portrait
 * is drawn by the code alone, so the menu reads before the content has.
 *
 * Герой - одной строкой: портрет с уровнем на нём, имя и класс, справа - удаление и малая «Играть». Под баном и в корзине героем не
 * играют - под строкой карточка говорит, кто, за что и до когда, и даёт обжаловать; самоудаление - серая карточка с отсчётом. Значок
 * роли - у администратора и модератора.
 */
@Composable private fun CharacterCard(game: GameUi, character: HeroSummary, onPlay: () -> Unit, onDelete: () -> Unit, onRestore: () -> Unit, onErase: () -> Unit) {
    val heroClass = character.heroClass.takeIf { it.isNotBlank() }
    val sanction = game.session.sanctions[character.id]
    val deleted = character.deleted || sanction?.kind == SanctionKind.DELETION
    val voluntary = sanction?.voluntary == true
    val playable = sanction == null && !deleted
    var appealing by remember(character.id) { mutableStateOf(false) }
    ForgePanel(modifier = Modifier.alpha(if (deleted) .6f else 1f).clickable(enabled = !game.busy && playable, onClick = onPlay)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LevelPortrait(heroClass, character.level, game.world.portraits, gray = voluntary)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        character.name,
                        color = nameColor(sanction != null, voluntary),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    RoleMark(accountRole(game.session.profile?.role))
                    TesterMark(character.tester)
                }
                Text(heroClass?.let(::classTitle) ?: ui("chars.unknown"), color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (playable) {
                ForgeIconButton(Icons.Outlined.DeleteOutline, ui("chars.release_do"), onClick = onDelete, kind = ButtonKind.Danger, size = ButtonSize.Small, state = ButtonState.of(!game.busy))
                ForgeButton(state = ButtonState.of(!game.busy), onClick = onPlay, size = ButtonSize.Small) { Text(ui("auth.play")) }
            }
        }
        when {
            voluntary -> sanction?.daysLeft()?.let { Text(ui("chars.marked_in", it), color = Muted, style = MaterialTheme.typography.bodySmall) }

            sanction != null -> {
                Text(noticeLine(sanction), color = if (deleted) Muted else LifeRed, style = MaterialTheme.typography.bodySmall)
                if (sanction.reason.isNotBlank()) MutedText("«${sanction.reason}»")
            }
        }
        when {
            voluntary -> MarkedActions(game, onRestore, onErase)

            sanction != null -> ForgeButton(state = ButtonState.of(!game.busy && !sanction.appealed), onClick = { appealing = true }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) {
                Text(ui(if (sanction.appealed) "notice.appealed_short" else "notice.appeal"))
            }
        }
    }
    if (appealing && sanction != null) {
        val notices = koinViewModel<NoticeViewModel>().model
        AppealDialog(onDismiss = { appealing = false }) { text ->
            appealing = false
            notices.appeal(sanction.id, text)
        }
    }
}

/** Круглый портрет класса 48 dp с уровнем героя [level] плашкой на нижней кромке; [gray] - обесцвечен (герой в своей корзине). */
@Composable private fun LevelPortrait(heroClass: String?, level: Int, portraits: Int, gray: Boolean) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.BottomCenter) {
        ClassPortrait(heroClass, portraits, Modifier.fillMaxSize().then(if (gray) Modifier.grayscale() else Modifier), round = true)
        Text(
            level.toString(),
            color = GoldBright,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = 4.dp).background(Ink, LevelShape).border(1.dp, Gold.copy(alpha = .6f), LevelShape).padding(horizontal = 5.dp),
        )
    }
}

private val LevelShape = RoundedCornerShape(6.dp)

/** Имя героя: под санкцией модерации - красным, в своей корзине - приглушённым, иначе - золотом. */
private fun nameColor(sanctioned: Boolean, voluntary: Boolean) = when {
    voluntary -> Muted
    sanctioned -> LifeRed
    else -> GoldBright
}

/** Кнопки героя в своей корзине: вернуть - если есть свободное место, иначе погашено, касание называет причину; стереть навсегда. */
@Composable private fun MarkedActions(game: GameUi, onRestore: () -> Unit, onErase: () -> Unit) {
    val slot = game.characterSlotsLeft > 0
    var explain by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        ForgeButton(
            state = ButtonState.of(!game.busy && slot),
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth(),
            onDisabledClick = if (slot) null else ({ explain = true }),
        ) { Text(ui("chars.restore")) }
        if (explain) TipCallout(Tip(body = ui("chars.restore_no_slot"), tint = Muted)) { explain = false }
    }
    ForgeButton(state = ButtonState.of(!game.busy), onClick = onErase, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Danger) {
        Text(ui("chars.erase_do"))
    }
}

/**
 * The creation form.
 *
 * The class is chosen here or nowhere: it is the whole stat base and the root of the tree, and the
 * server has no route that moves a hero to another one. The classes come with the content, and the one picked is the play state's draft, so it survives the form being redrawn.
 * Each is shown by the picker, whole, between the name and the button that seals the choice.
 *
 * Имя - поле со счётчиком знаков внутри и ⓘ правил имени. Классы приходят с контентом после открытия формы: пустой список - ошибка,
 * только когда чтение кончилось.
 */
@Composable private fun CreateCharacterPanel(game: GameUi, vm: CharactersViewModel, name: String, onName: (String) -> Unit, classes: List<String>, heroClass: String) {
    val index = game.index
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val rules = ui("chars.name_unique")
        OutlinedTextField(
            name,
            { onName(NameCharset.filter(it).take(game.inputs.heroName)) },
            enabled = !game.busy,
            label = { Text(ui("common.name")) },
            suffix = { Text("${name.length}/${game.inputs.heroName}", color = Muted, style = MaterialTheme.typography.labelMedium) },
            trailingIcon = { InfoMark(rules, tip = { Tip(rules, ui("common.name_charset"), Muted) }) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (classes.isEmpty() && index != null && Reads.CONTENT !in game.loading) {
            Text(ui("editor.no_classes"), color = MaterialTheme.colorScheme.error)
        }
        if (index != null && heroClass.isNotBlank()) {
            ClassPicker(index, classes, heroClass, game.world.portraits, enabled = !game.busy, onChoose = vm::draftClass)
        }
    }
}
