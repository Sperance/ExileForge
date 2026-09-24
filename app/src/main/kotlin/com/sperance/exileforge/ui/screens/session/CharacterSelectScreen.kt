package com.sperance.exileforge.ui.screens.session

import com.sperance.exileforge.core.display.Glyph

import androidx.compose.foundation.clickable
import com.sperance.exileforge.presentation.state.Reads
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.CharacterSummary
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.MAX_CHARACTERS
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * Choosing who to play, and the only place that choice is ever made.
 *
 * Every screen behind the gate acts on the chosen character, which is what lets them stop asking
 * which one. Coming back here is the way to swap — a tab never moves the player onto another hero.
 *
 * An account with nothing to choose between does not get a chooser: the creation form opens
 * straight away, because an empty list is not a decision.
 */
@Composable fun CharacterSelectScreen(s: ForgeState, vm: ForgeViewModel) {
    val empty = s.account.charactersRead && s.account.characters.isEmpty()
    var creating by rememberSaveable(empty) { mutableStateOf(empty) }
    var pendingDelete by remember { mutableStateOf<CharacterSummary?>(null) }
    LaunchedEffect(creating) { if (creating) vm.ensureClasses() }
    Scaffold(containerColor = Ink) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().voidBackdrop()) {
            if (s.busy || s.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)
            RefusalLine(s.refusal, vm::dismissMessage)
            if (creating) CreatingColumn(s, vm, onBack = { creating = false }, onSignOut = vm::logout)
            else CharacterMenu(s, onPlay = vm::enterCharacter, onDelete = { pendingDelete = it },
                onCreate = { creating = true }, onRefresh = vm::refreshCharacters, onLogout = vm::logout)
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
 * What is worth checking here is which character a tap plays and how many slots are left — not the
 * wiring behind them.
 */
@Composable internal fun ColumnScope.CharacterMenu(s: ForgeState, onPlay: (String) -> Unit, onDelete: (CharacterSummary) -> Unit,
    onCreate: () -> Unit = {}, onRefresh: () -> Unit = {}, onLogout: () -> Unit = {}) {
    // A list is refreshed by pulling it, here as everywhere else. The button that used to sit at
    // the bottom of this one said the same thing twice.
    PullToRefreshBox(isRefreshing = s.refreshing(Reads.CHARACTERS), onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ScreenHeader(ui("chars.title"),
                ui("chars.slots", s.characterSlotsLeft, MAX_CHARACTERS),
                ForgeGlyphs.Exile)
        }
        items(s.account.characters, key = { it.id }) { character ->
            CharacterCard(s, character, onPlay = { onPlay(character.id) }, onDelete = { onDelete(character) })
        }
        item {
            Button(enabled = !s.busy && s.characterSlotsLeft > 0, onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                Text(ui("editor.create_character"))
            }
            if (s.characterSlotsLeft == 0) Text(
                ui("chars.slots_full"),
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(s.accountTitle, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                TextButton(enabled = !s.busy, onClick = onLogout) { Text(ui("chars.sign_out")) }
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

/** One character, as the menu describes them: the name, the class and how far they have come. */
@Composable private fun CharacterCard(s: ForgeState, character: CharacterSummary, onPlay: () -> Unit, onDelete: () -> Unit) {
    val characterClass = s.world.classes.firstOrNull { it.id == character.classId }
    ForgePanel(modifier = Modifier.clickable(enabled = !s.busy, onClick = onPlay)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ClassPortrait(characterClass?.code, s.world.portraits, Modifier.size(64.dp), round = true)
            Column(Modifier.weight(1f)) {
                Text(character.name, color = GoldBright, style = MaterialTheme.typography.titleMedium)
                PropertyRow(ui("common.class"), characterClass?.title ?: ui("chars.unknown"), Glyph.CHARACTER)
                PropertyRow(ui("common.level"), character.level.toString(), Glyph.LEVEL)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !s.busy, onClick = onPlay, modifier = Modifier.weight(1f)) { Text(ui("auth.play")) }
            OutlinedButton(enabled = !s.busy, onClick = onDelete) { Text(ui("chars.release_do"), color = MaterialTheme.colorScheme.error) }
        }
    }
}

/**
 * The creation form.
 *
 * The class is chosen here or nowhere: it is the whole stat base and the root of the tree, and the
 * server has no route that moves a character to another one.
 */
@Composable private fun CreateCharacterPanel(s: ForgeState, vm: ForgeViewModel, canGoBack: Boolean,
    onBack: () -> Unit, onSignOut: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var classId by rememberSaveable(s.world.classes.size) { mutableStateOf(s.world.classes.firstOrNull()?.id.orEmpty()) }
    val chosen = s.world.classes.firstOrNull { it.id == classId }
    ForgePanel {
        OutlinedTextField(name, { name = it }, enabled = !s.busy, label = { Text(ui("common.name")) },
            supportingText = { Text(ui("chars.name_unique")) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        if (s.world.classes.isEmpty()) Text(ui("editor.no_classes"),
            color = MaterialTheme.colorScheme.error)
        // Every class as its portrait, three by four, side by side: the one chosen is framed in gold.
        else LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(s.world.classes, key = { it.id }) { option ->
                val picked = option.id == classId
                val shape = CutCornerShape(8.dp)
                Column(Modifier.width(96.dp).border(if (picked) 2.dp else 1.dp, if (picked) GoldBright else Bronze.copy(alpha = .5f), shape)
                    .clip(shape).clickable(enabled = !s.busy) { classId = option.id }, horizontalAlignment = Alignment.CenterHorizontally) {
                    ClassPortrait(option.code, s.world.portraits, Modifier.fillMaxWidth())
                    Text(option.title, color = if (picked) GoldBright else Parchment, style = MaterialTheme.typography.labelMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(4.dp))
                }
            }
        }
        chosen?.let { option ->
            if (option.details.isNotBlank()) Text(option.details, color = Muted, style = MaterialTheme.typography.bodySmall)
            Text(ui("editor.level1_base") + option.baseStats.joinToString(" · ") {
                "${statTitle(it.stat, s.lang)} ${statNumber(it.stat, it.value)}" },
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Button(enabled = !s.busy && name.isNotBlank() && classId.isNotBlank(),
            onClick = { vm.createCharacter(name, classId) }, modifier = Modifier.fillMaxWidth()) {
            Text(ui("chars.create"))
        }
        Text(ui("chars.class_note"),
            color = Muted, style = MaterialTheme.typography.bodySmall)
        if (canGoBack) TextButton(enabled = !s.busy, onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(ui("chars.back"))
        }
        // An account with no characters has no list to go back to, and a device registration is
        // silent — so without this the first screen a new player sees is also the only one, with
        // no way to sign in as someone who already has an exile.
        TextButton(enabled = !s.busy, onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text(ui("chars.other_account"))
        }
    }
}
