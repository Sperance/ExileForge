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
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.MAX_CHARACTERS
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.mightText
import com.sperance.exileforge.core.display.purseText
import com.sperance.exileforge.core.i18n.Lang
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
import com.sperance.exileforge.ui.screens.hero.experienceShare
import com.sperance.exileforge.ui.screens.hero.titleName
import com.sperance.exileforge.ui.screens.server.noticeLine
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * Choosing who to play, and the only place that choice is ever made.
 *
 * Every screen behind the gate acts on the chosen hero, which is what lets them stop asking
 * which one. Coming back here is the way to swap — a tab never moves the player onto another hero.
 *
 * An account with nothing to choose between does not get a chooser: the creation form opens
 * straight away, because an empty list is not a decision.
 *
 * Язык - до игры: настройки недоступны, пока герой не выбран; он - в шапке экрана ([HeroesHeader]) и ничего не перекрывает.
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
                val back = { creating = false }.takeIf { creating && game.session.characters.isNotEmpty() }
                HeroesHeader(game, creating, back, onLanguage = vm::language)
                if (creating) {
                    CreatingColumn(game, vm, onSignOut = vm::logout)
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
 * Список перечитывается потягиванием; герои - строками, последний игравший первым ([HeroSummary.lastPlayedFirst]), под ними - создание
 * героя с занятыми местами. Предначертание - лентой над героями, первой строкой: шапка с языком стоит над списком, а не поверх него.
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
            items(HeroSummary.lastPlayedFirst(game.session.characters), key = { it.id }) { character ->
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
 * Шапка экрана героев строкой: «Герои» и занятые места «N/M» (места кончились - цветом осторожности), справа - язык. В форме создания у
 * аккаунта с героями - «← Герои» ([onBack]) и язык; без героев возвращаться некуда - «Герои» и язык.
 */
@Composable private fun HeroesHeader(game: GameUi, creating: Boolean, onBack: (() -> Unit)?, onLanguage: (Lang) -> Unit) {
    val total = game.index?.rules?.maxCharacters ?: MAX_CHARACTERS
    val taken = (total - game.characterSlotsLeft).coerceIn(0, total)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            BackHandler(onBack = onBack)
            ForgeIconButton(Icons.AutoMirrored.Outlined.ArrowBack, ui("common.back"), onClick = onBack, accent = Gold)
            Text(ui("chars.back"), color = Muted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable(role = Role.Button, onClick = onBack))
        } else {
            Text(ui("heroes.title"), style = relicName(19).copy(color = GoldBright), modifier = Modifier.padding(start = 8.dp).semantics { heading() })
            if (!creating) {
                Text(
                    "$taken/$total",
                    color = if (taken >= total) Caution else Muted,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 10.dp).semantics { contentDescription = ui("heroes.slots_said", taken, total) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        LanguageButton(game.lang, game.world.languages, enabled = !game.busy, onLanguage = onLanguage)
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
@Composable private fun ColumnScope.CreatingColumn(game: GameUi, vm: CharactersViewModel, onSignOut: () -> Unit) {
    com.sperance.exileforge.ui.components.PrologueGate(always = true) { CreatingList(game, vm, onSignOut) }
}

/**
 * Форма создания героя. У аккаунта с героями шапка экрана - «← Герои» (и системный «назад») к списку; без героев вернуться некуда,
 * поэтому под выбором класса - вход под другим аккаунтом. «В изгнание» закреплена внизу, форма над ней прокручивается.
 */
@Composable private fun ColumnScope.CreatingList(game: GameUi, vm: CharactersViewModel, onSignOut: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val classes = game.index?.classes?.classes.orEmpty()
    val draft by vm.draftClass.collectAsStateWithLifecycle()
    val heroClass = draft.takeIf { code -> classes.any { it.code == code } } ?: classes.firstOrNull()?.code.orEmpty()
    val canGoBack = game.session.characters.isNotEmpty()
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

/**
 * One hero, as the menu describes them: the name, the class and how far they have come.
 *
 * The row carries the class as a code; the server's dictionary names it, and the portrait
 * is drawn by the code alone, so the menu reads before the content has.
 *
 * Герой - плотной строкой: портрет с уровнем на нём; первая строка - имя (значки роли и тестировщика) и ⚜ Мощь, вторая - класс ·
 * гильдия, третья - титул и золото ([purseText]); опыт - полоса во всю ширину по нижнему краю. Касание строки - играть (для чтения с
 * экрана - кнопка «Играть» с описанием героя), удаление - в «⋯». Под баном и в корзине героем не играют - под строкой карточка говорит,
 * кто, за что и до когда, и даёт обжаловать; самоудаление - серая карточка с отсчётом.
 */
@Composable private fun CharacterCard(game: GameUi, character: HeroSummary, onPlay: () -> Unit, onDelete: () -> Unit, onRestore: () -> Unit, onErase: () -> Unit) {
    val heroClass = character.heroClass.takeIf { it.isNotBlank() }
    val sanction = game.session.sanctions[character.id]
    val deleted = character.deleted || sanction?.kind == SanctionKind.DELETION
    val voluntary = sanction?.voluntary == true
    val playable = sanction == null && !deleted
    val guild = character.guild?.id?.let { game.session.guilds[it] }
    val classes = game.index?.classes
    val share = remember(classes, character.level, character.experience) { classes?.let { experienceShare(it, character.level, character.experience) } }
    val said = remember(character, guild, share, game.lang) { heroSaid(character, guild, share) }
    var appealing by remember(character.id) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().alpha(if (deleted) .6f else 1f).clip(PanelShape).forgePanelSurface(Gold)
            .clickable(enabled = !game.busy && playable, role = Role.Button, onClickLabel = ui("auth.play"), onClick = onPlay)
            .semantics { contentDescription = said },
    ) {
        Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LevelPortrait(heroClass, character.level, game.world.portraits, gray = voluntary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
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
                    Spacer(Modifier.weight(1f))
                    character.might?.let { Text(ui("heroes.might", mightText(it)), color = GoldBright, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false) }
                }
                Text(
                    listOf(heroClass?.let(::classTitle) ?: ui("chars.unknown"), guild ?: ui("heroes.no_guild")).joinToString(" · "),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(character.title.takeIf { it.isNotBlank() }?.let(::titleName).orEmpty(), color = Rune, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Icon(ForgeGlyphs.Coins, null, tint = Gold, modifier = Modifier.size(14.dp))
                    Text(purseText(character.money), color = GoldBright, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                }
            }
            if (playable) HeroMenu(character.name, enabled = !game.busy, onDelete = onDelete)
        }
        if (sanction != null) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (voluntary) {
                    sanction.daysLeft()?.let { Text(ui("chars.marked_in", it), color = Muted, style = MaterialTheme.typography.bodySmall) }
                    MarkedActions(game, onRestore, onErase)
                } else {
                    Text(noticeLine(sanction), color = if (deleted) Muted else LifeRed, style = MaterialTheme.typography.bodySmall)
                    if (sanction.reason.isNotBlank()) MutedText("«${sanction.reason}»")
                    ForgeButton(state = ButtonState.of(!game.busy && !sanction.appealed), onClick = { appealing = true }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) {
                        Text(ui(if (sanction.appealed) "notice.appealed_short" else "notice.appeal"))
                    }
                }
            }
        }
        share?.let { ExperienceEdge(it) }
    }
    if (appealing && sanction != null) {
        val notices = koinViewModel<NoticeViewModel>().model
        AppealDialog(onDismiss = { appealing = false }) { text ->
            appealing = false
            notices.appeal(sanction.id, text)
        }
    }
}

/** Полоса опыта уровня во всю ширину по нижнему краю строки героя: доля [share] золотом. */
@Composable private fun ExperienceEdge(share: Float) {
    Box(Modifier.fillMaxWidth().height(3.dp).background(PanelRaised)) {
        Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(Gold))
    }
}

/** «⋯» строки героя [name]: удаление. Меню - [ForgeMenu]: окна у закрытого меню нет. */
@Composable private fun HeroMenu(name: String, enabled: Boolean, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        ForgeIconButton(
            Icons.Outlined.MoreHoriz,
            ui("heroes.more", name),
            onClick = { open = true },
            kind = if (open) ButtonKind.Secondary else ButtonKind.Text,
            size = ButtonSize.Compact,
            shape = IconButtonShape.Circle,
            state = ButtonState.of(enabled),
        )
        ForgeMenu(expanded = open, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            DropdownMenuItem(
                text = { Text(ui("chars.release_do"), color = LifeRed) },
                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = LifeRed) },
                onClick = {
                    open = false
                    onDelete()
                },
            )
        }
    }
}

/** Герой строки словами для чтения с экрана: имя, класс, уровень, опыт, Мощь, гильдия, титул и золото. */
private fun heroSaid(hero: HeroSummary, guild: String?, share: Float?): String = listOfNotNull(
    hero.name,
    hero.heroClass.takeIf { it.isNotBlank() }?.let(::classTitle),
    ui("heroes.said.level", hero.level),
    share?.let { ui("heroes.said.xp", (it * 100).roundToInt()) },
    hero.might?.let { ui("heroes.said.might", mightText(it)) },
    guild?.let { ui("heroes.said.guild", it) } ?: ui("heroes.no_guild"),
    hero.title.takeIf { it.isNotBlank() }?.let { ui("heroes.said.title", titleName(it)) },
    ui("heroes.said.gold", purseText(hero.money)),
).joinToString(", ")

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
