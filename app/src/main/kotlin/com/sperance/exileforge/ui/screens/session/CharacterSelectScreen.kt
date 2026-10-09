package com.sperance.exileforge.ui.screens.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroSummary
import com.sperance.exileforge.core.network.SanctionKind
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.session.CharactersViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.MAX_CHARACTERS
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.text.NameCharset
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.server.sanctionLine
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
            // The language before the game (3.79.0): the settings are out of reach until a hero is chosen.
            LanguageButton(game.lang, game.world.languages, Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp), enabled = !game.busy, onLanguage = vm::language)
            ToastHost(game, vm::dismissMessage, vm::dismissNotice, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
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
    // A list is refreshed by pulling it, here as everywhere else. The button that used to sit at
    // the bottom of this one said the same thing twice.
    PullToRefreshBox(isRefreshing = game.refreshing(Reads.CHARACTERS), onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                // How many heroes an account holds is the rules' to say; until they are read, the client's own figure stands in.
                ScreenHeader(
                    ui("chars.title"),
                    ui("chars.slots", game.characterSlotsLeft, game.index?.rules?.maxCharacters ?: MAX_CHARACTERS),
                    ForgeGlyphs.Exile,
                )
            }
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
            item {
                ForgeButton(enabled = !game.busy && game.characterSlotsLeft > 0, onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                    Text(ui("editor.create_character"))
                }
                if (game.characterSlotsLeft == 0) MutedText(ui("chars.slots_full"))
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(game.session.title, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    BugAction()
                    ForgeTextButton(enabled = !game.busy, onClick = onLogout) { Text(ui("chars.sign_out")) }
                }
            }
        }
    }
}

/** The creation form on its own page: there is nothing to choose between while it is open. */
@Composable private fun ColumnScope.CreatingColumn(game: GameUi, vm: CharactersViewModel, onBack: () -> Unit, onSignOut: () -> Unit) {
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // The beetle on the creation form too (3.75.0): it is the one page before the game without a way to report.
        item { ScreenHeader(ui("chars.new"), ui("chars.name_and_class"), ForgeGlyphs.Exile) { BugAction() } }
        item { CreateCharacterPanel(game, vm, canGoBack = game.session.characters.isNotEmpty(), onBack = onBack, onSignOut = onSignOut) }
    }
}

/**
 * One hero, as the menu describes them: the name, the class and how far they have come.
 *
 * The row carries the class as a code (3.0.0); the server's dictionary names it, and the portrait
 * is drawn by the code alone, so the menu reads before the content has.
 */
@Composable private fun CharacterCard(game: GameUi, character: HeroSummary, onPlay: () -> Unit, onDelete: () -> Unit, onRestore: () -> Unit, onErase: () -> Unit) {
    val heroClass = character.heroClass.takeIf { it.isNotBlank() }
    // Санкция героя (3.88.5): под баном и в корзине им не играют; карточка говорит, кто, за что и до когда, и даёт обжаловать
    val sanction = game.session.sanctions[character.id]
    val deleted = character.deleted || sanction?.kind == SanctionKind.DELETION
    // Самоудаление (4.5.1): серая карточка с отсчётом; вернуть или стереть сразу - сам владелец, обжаловать нечего
    val voluntary = sanction?.voluntary == true
    val playable = sanction == null && !deleted
    var appealing by remember(character.id) { mutableStateOf(false) }
    ForgePanel(modifier = Modifier.alpha(if (deleted) .6f else 1f).clickable(enabled = !game.busy && playable, onClick = onPlay)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ClassPortrait(heroClass, game.world.portraits, Modifier.size(64.dp).then(if (voluntary) Modifier.grayscale() else Modifier), round = true)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(character.name, color = nameColor(sanction != null, voluntary), style = MaterialTheme.typography.titleMedium)
                    // Роль аккаунта (3.88.7): значок у администратора, модератора и тестировщика.
                    RoleMark(accountRole(game.session.profile?.role))
                }
                PropertyRow(ui("common.class"), heroClass?.let(::classTitle) ?: ui("chars.unknown"), Glyph.CHARACTER)
                PropertyRow(ui("common.level"), character.level.toString(), Glyph.LEVEL)
            }
        }
        when {
            voluntary -> sanction?.daysLeft()?.let { Text(ui("chars.marked_in", it), color = Muted, style = MaterialTheme.typography.bodySmall) }

            sanction != null -> {
                Text(sanctionLine(sanction), color = if (deleted) Muted else LifeRed, style = MaterialTheme.typography.bodySmall)
                if (sanction.comment.isNotBlank()) MutedText("«${sanction.comment}»")
            }
        }
        when {
            voluntary -> MarkedActions(game, onRestore, onErase)

            sanction != null -> ForgeOutlinedButton(enabled = !game.busy && !sanction.appealed, onClick = { appealing = true }, modifier = Modifier.fillMaxWidth()) {
                Text(ui(if (sanction.appealed) "notice.appealed_short" else "notice.appeal"))
            }

            else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForgeButton(enabled = !game.busy, onClick = onPlay, modifier = Modifier.weight(1f)) { Text(ui("auth.play")) }
                ForgeOutlinedButton(enabled = !game.busy, onClick = onDelete) { Text(ui("chars.release_do"), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (appealing && sanction != null) {
        val notices = koinViewModel<com.sperance.exileforge.presentation.admin.NoticeViewModel>()
        AppealDialog(sanction, onDismiss = { appealing = false }) { text ->
            appealing = false
            notices.appeal(sanction.id, text)
        }
    }
}

/** Имя героя: под санкцией модерации - красным, в своей корзине - приглушённым, иначе - золотом. */
private fun nameColor(sanctioned: Boolean, voluntary: Boolean) = when {
    voluntary -> Muted
    sanctioned -> LifeRed
    else -> GoldBright
}

/** Кнопки героя в своей корзине (4.5.1): вернуть - если есть свободное место, иначе погашено с причиной; стереть навсегда. */
@Composable private fun MarkedActions(game: GameUi, onRestore: () -> Unit, onErase: () -> Unit) {
    val slot = game.characterSlotsLeft > 0
    ForgeButton(enabled = !game.busy && slot, onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text(ui("chars.restore")) }
    if (!slot) Text(ui("chars.restore_no_slot"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
    ForgeOutlinedButton(enabled = !game.busy, onClick = onErase, modifier = Modifier.fillMaxWidth()) {
        Text(ui("chars.erase_do"), color = MaterialTheme.colorScheme.error)
    }
}

/**
 * The creation form.
 *
 * The class is chosen here or nowhere: it is the whole stat base and the root of the tree, and the
 * server has no route that moves a hero to another one. The classes come with the content
 * (3.0.0), and the one picked is the play state's draft, so it survives the form being redrawn.
 * Each is shown by the carousel (3.13.0), whole, between the name and the button that seals the choice.
 */
@Composable private fun CreateCharacterPanel(
    game: GameUi,
    vm: CharactersViewModel,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    val index = game.index
    val classes = index?.classes?.classes.orEmpty()
    val draft by vm.draftClass.collectAsStateWithLifecycle()
    val heroClass = draft.takeIf { code -> classes.any { it.code == code } } ?: classes.firstOrNull()?.code.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ForgePanel {
            OutlinedTextField(
                name,
                { name = NameCharset.filter(it).take(game.inputs.heroName) },
                enabled = !game.busy,
                label = { Text(ui("common.name")) },
                supportingText = { Text(ui("chars.name_unique") + " · ${name.length}/${game.inputs.heroName}\n" + ui("common.name_charset")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            // The classes arrive with the content, which is read after the form opens: an empty
            // list is only an error once that read has finished (the app bar shows its progress).
            if (classes.isEmpty() && index != null && Reads.CONTENT !in game.loading) {
                Text(
                    ui("editor.no_classes"),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (index != null && heroClass.isNotBlank()) {
            ClassCarousel(index, classes.map { it.code }, heroClass, game.world.portraits, enabled = !game.busy, onChoose = vm::draftClass)
        }
        ForgePanel {
            ForgeButton(
                enabled = !game.busy && name.isNotBlank() && heroClass.isNotBlank(),
                onClick = { vm.createCharacter(name, heroClass) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(ui("chars.create"))
            }
            if (canGoBack) {
                ForgeTextButton(enabled = !game.busy, onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(ui("chars.back"))
                }
            }
            // An account with no heroes has no list to go back to, and a device registration is
            // silent — so without this the first screen a new player sees is also the only one, with
            // no way to sign in as someone who already has an exile.
            ForgeTextButton(enabled = !game.busy, onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text(ui("chars.other_account"))
            }
        }
    }
}
