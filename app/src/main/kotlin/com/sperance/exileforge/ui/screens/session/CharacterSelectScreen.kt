package com.sperance.exileforge.ui.screens.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroSummary
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.MAX_CHARACTERS
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * Choosing who to play, and the only place that choice is ever made.
 *
 * Every screen behind the gate acts on the chosen hero, which is what lets them stop asking
 * which one. Coming back here is the way to swap — a tab never moves the player onto another hero.
 *
 * An account with nothing to choose between does not get a chooser: the creation form opens
 * straight away, because an empty list is not a decision.
 */
@Composable fun CharacterSelectScreen(s: ForgeState, vm: ForgeViewModel) {
    val empty = s.account.charactersRead && s.account.characters.isEmpty()
    var creating by rememberSaveable(empty) { mutableStateOf(empty) }
    var pendingDelete by remember { mutableStateOf<HeroSummary?>(null) }
    LaunchedEffect(creating) { if (creating) vm.ensureClasses() }
    Scaffold(containerColor = Ink) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
        Column(Modifier.fillMaxSize().voidBackdrop()) {
            if (s.busy || s.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)
            if (creating) CreatingColumn(s, vm, onBack = { creating = false }, onSignOut = vm::logout)
            else CharacterMenu(s, onPlay = vm::enterCharacter, onDelete = { pendingDelete = it },
                onCreate = { creating = true }, onRefresh = vm::refreshCharacters, onLogout = vm::logout)
        }
        ToastHost(s, vm::dismissMessage, vm::dismissNotice, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
        }
    }
    pendingDelete?.let { doomed ->
        ConfirmSheet(
            title = ui("chars.release_q"), subtitle = doomed.name, danger = true,
            icon = { Icon(ForgeGlyphs.Exile, null, tint = LifeRed, modifier = Modifier.size(40.dp)) },
            note = ui("chars.release_text"),
            confirm = ui("chars.release_do"),
            onDismiss = { pendingDelete = null }) { vm.deleteCharacter(doomed.id) }
    }
}

/**
 * The menu itself, driven by callbacks so it can be shown without a view model.
 *
 * What is worth checking here is which hero a tap plays and how many slots are left — not the
 * wiring behind them.
 */
@Composable internal fun ColumnScope.CharacterMenu(s: ForgeState, onPlay: (String) -> Unit, onDelete: (HeroSummary) -> Unit,
    onCreate: () -> Unit = {}, onRefresh: () -> Unit = {}, onLogout: () -> Unit = {}) {
    // A list is refreshed by pulling it, here as everywhere else. The button that used to sit at
    // the bottom of this one said the same thing twice.
    PullToRefreshBox(isRefreshing = s.refreshing(Reads.CHARACTERS), onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            // How many heroes an account holds is the rules' to say; until they are read, the client's own figure stands in.
            ScreenHeader(ui("chars.title"),
                ui("chars.slots", s.characterSlotsLeft, s.index?.rules?.maxCharacters ?: MAX_CHARACTERS),
                ForgeGlyphs.Exile)
        }
        items(s.account.characters, key = { it.id }) { character ->
            CharacterCard(s, character, onPlay = { onPlay(character.id) }, onDelete = { onDelete(character) })
        }
        item {
            ForgeButton(enabled = !s.busy && s.characterSlotsLeft > 0, onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                Text(ui("editor.create_character"))
            }
            if (s.characterSlotsLeft == 0) MutedText(ui("chars.slots_full"))
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(s.accountTitle, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                BugAction()
                ForgeTextButton(enabled = !s.busy, onClick = onLogout) { Text(ui("chars.sign_out")) }
            }
        }
    }
    }
}

/** The creation form on its own page: there is nothing to choose between while it is open. */
@Composable private fun ColumnScope.CreatingColumn(s: ForgeState, vm: ForgeViewModel, onBack: () -> Unit, onSignOut: () -> Unit) {
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader(ui("chars.new"), ui("chars.name_and_class"), ForgeGlyphs.Exile) }
        item { CreateCharacterPanel(s, vm, canGoBack = s.account.characters.isNotEmpty(), onBack = onBack, onSignOut = onSignOut) }
    }
}

/**
 * One hero, as the menu describes them: the name, the class and how far they have come.
 *
 * The row carries the class as a code (3.0.0); the server's dictionary names it, and the portrait
 * is drawn by the code alone, so the menu reads before the content has.
 */
@Composable private fun CharacterCard(s: ForgeState, character: HeroSummary, onPlay: () -> Unit, onDelete: () -> Unit) {
    val heroClass = character.heroClass.takeIf { it.isNotBlank() }
    ForgePanel(modifier = Modifier.clickable(enabled = !s.busy, onClick = onPlay)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ClassPortrait(heroClass, s.world.portraits, Modifier.size(64.dp), round = true)
            Column(Modifier.weight(1f)) {
                Text(character.name, color = GoldBright, style = MaterialTheme.typography.titleMedium)
                PropertyRow(ui("common.class"), heroClass?.let(::classTitle) ?: ui("chars.unknown"), Glyph.CHARACTER)
                PropertyRow(ui("common.level"), character.level.toString(), Glyph.LEVEL)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeButton(enabled = !s.busy, onClick = onPlay, modifier = Modifier.weight(1f)) { Text(ui("auth.play")) }
            ForgeOutlinedButton(enabled = !s.busy, onClick = onDelete) { Text(ui("chars.release_do"), color = MaterialTheme.colorScheme.error) }
        }
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
@Composable private fun CreateCharacterPanel(s: ForgeState, vm: ForgeViewModel, canGoBack: Boolean,
    onBack: () -> Unit, onSignOut: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val index = s.index
    val classes = index?.classes?.classes.orEmpty()
    val heroClass = s.play.draftClass.takeIf { code -> classes.any { it.code == code } } ?: classes.firstOrNull()?.code.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ForgePanel {
            OutlinedTextField(name, { name = it }, enabled = !s.busy, label = { Text(ui("common.name")) },
                supportingText = { Text(ui("chars.name_unique")) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            if (classes.isEmpty()) Text(ui("editor.no_classes"),
                color = MaterialTheme.colorScheme.error)
        }
        if (index != null && heroClass.isNotBlank())
            ClassCarousel(index, classes.map { it.code }, heroClass, s.world.portraits, enabled = !s.busy, onChoose = vm::draftClass)
        ForgePanel {
            ForgeButton(enabled = !s.busy && name.isNotBlank() && heroClass.isNotBlank(),
                onClick = { vm.createCharacter(name, heroClass) }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("chars.create"))
            }
            if (canGoBack) ForgeTextButton(enabled = !s.busy, onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(ui("chars.back"))
            }
            // An account with no heroes has no list to go back to, and a device registration is
            // silent — so without this the first screen a new player sees is also the only one, with
            // no way to sign in as someone who already has an exile.
            ForgeTextButton(enabled = !s.busy, onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text(ui("chars.other_account"))
            }
        }
    }
}
