package com.sperance.exileforge.ui.screens.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.tr
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
@Composable fun CharacterSelectScreen(s: ForgeState, vm: ForgeViewModel, snackbar: SnackbarHostState) {
    val empty = s.charactersRead && s.characters.isEmpty()
    var creating by rememberSaveable(empty) { mutableStateOf(empty) }
    var pendingDelete by remember { mutableStateOf<CharacterSummary?>(null) }
    LaunchedEffect(creating) { if (creating) vm.ensureClasses() }
    Scaffold(containerColor = Ink,
        snackbarHost = { SnackbarHost(snackbar) { data -> Snackbar(data, containerColor = PanelRaised, contentColor = Parchment, actionColor = Gold, shape = MaterialTheme.shapes.small) } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().voidBackdrop()) {
            if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)
            if (creating) CreatingColumn(s, vm, onBack = { creating = false }, onSignOut = vm::logout)
            else CharacterMenu(s, onPlay = vm::enterCharacter, onDelete = { pendingDelete = it },
                onCreate = { creating = true }, onRefresh = vm::refreshCharacters, onLogout = vm::logout)
        }
    }
    pendingDelete?.let { doomed ->
        AlertDialog(onDismissRequest = { pendingDelete = null }, containerColor = Panel, titleContentColor = Gold,
            title = { Text(tr("Отпустить персонажа?", "Give the character up?")) },
            text = { Text(doomed.name + "\n" + tr("Снаряжение, дерево и лоты уйдут вместе с ним. Это не отменить.",
                                                  "Their gear, tree and lots go with them. This cannot be undone.")) },
            confirmButton = { TextButton(enabled = !s.busy, onClick = { vm.deleteCharacter(doomed.id); pendingDelete = null }) {
                Text(tr("Отпустить", "Give up"), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(tr("Отмена", "Cancel")) } })
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
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ScreenHeader(tr("Кем играем", "Who are we playing"),
                tr("Слотов свободно: ${s.characterSlotsLeft} из $MAX_CHARACTERS", "${s.characterSlotsLeft} of $MAX_CHARACTERS slots free"),
                ForgeGlyphs.Exile)
        }
        items(s.characters, key = { it.id }) { character ->
            CharacterCard(s, character, onPlay = { onPlay(character.id) }, onDelete = { onDelete(character) })
        }
        item {
            Button(enabled = !s.busy && s.characterSlotsLeft > 0, onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Создать персонажа", "Create a character"))
            }
            if (s.characterSlotsLeft == 0) Text(
                tr("Все слоты заняты. Освободить можно, только отпустив персонажа.",
                   "Every slot is taken. One is freed only by giving a character up."),
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            OutlinedButton(enabled = !s.busy, onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Обновить список", "Refresh the list"))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(s.accountTitle, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                TextButton(enabled = !s.busy, onClick = onLogout) { Text(tr("Выйти из аккаунта", "Sign out")) }
            }
        }
    }
}

/** The creation form on its own page: there is nothing to choose between while it is open. */
@Composable private fun ColumnScope.CreatingColumn(s: ForgeState, vm: ForgeViewModel, onBack: () -> Unit, onSignOut: () -> Unit) {
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader(tr("Новый изгнанник", "A new exile"), tr("Имя и класс", "A name and a class"), ForgeGlyphs.Exile) }
        item { CreateCharacterPanel(s, vm, canGoBack = s.characters.isNotEmpty(), onBack = onBack, onSignOut = onSignOut) }
    }
}

/** One character, as the menu describes them: the name, the class and how far they have come. */
@Composable private fun CharacterCard(s: ForgeState, character: CharacterSummary, onPlay: () -> Unit, onDelete: () -> Unit) {
    val characterClass = s.classes.firstOrNull { it.id == character.classId }
    ForgePanel(modifier = Modifier.clickable(enabled = !s.busy, onClick = onPlay)) {
        Text(character.name, color = GoldBright, style = MaterialTheme.typography.titleMedium)
        PropertyRow(tr("Класс", "Class"), characterClass?.title ?: tr("неизвестен", "unknown"), "character")
        PropertyRow(tr("Уровень", "Level"), character.level.toString(), "level")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !s.busy, onClick = onPlay, modifier = Modifier.weight(1f)) { Text(tr("Играть", "Play")) }
            OutlinedButton(enabled = !s.busy, onClick = onDelete) { Text(tr("Отпустить", "Give up"), color = MaterialTheme.colorScheme.error) }
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
    var classId by rememberSaveable(s.classes.size) { mutableStateOf(s.classes.firstOrNull()?.id.orEmpty()) }
    val chosen = s.classes.firstOrNull { it.id == classId }
    ForgePanel {
        OutlinedTextField(name, { name = it }, enabled = !s.busy, label = { Text(tr("Имя", "Name")) },
            supportingText = { Text(tr("Имя уникально на всём сервере", "The name is unique across the server")) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        if (s.classes.isEmpty()) Text(tr("Сервер не вернул ни одного класса — персонажа создать нельзя.",
                                         "The server served no classes — a character cannot be created."),
            color = MaterialTheme.colorScheme.error)
        else Spinner(tr("Класс", "Class"), classId, s.classes.associate { it.id to it.title }, !s.busy) { classId = it }
        chosen?.let { option ->
            if (option.details.isNotBlank()) Text(option.details, color = Muted, style = MaterialTheme.typography.bodySmall)
            Text(tr("База 1 уровня: ", "Level 1 base: ") + option.baseStats.joinToString(" · ") {
                "${statTitle(it.stat, s.lang)} ${it.value.toInt()}" },
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Button(enabled = !s.busy && name.isNotBlank() && classId.isNotBlank(),
            onClick = { vm.createCharacter(name, classId) }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("В изгнание", "Into exile"))
        }
        Text(tr("Класс выбирается один раз: он задаёт характеристики и корень дерева, и сменить его сервер не даст.",
                "The class is chosen once: it sets the stats and the root of the tree, and the server will not move it."),
            color = Muted, style = MaterialTheme.typography.bodySmall)
        if (canGoBack) TextButton(enabled = !s.busy, onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Назад к списку", "Back to the list"))
        }
        // An account with no characters has no list to go back to, and a device registration is
        // silent — so without this the first screen a new player sees is also the only one, with
        // no way to sign in as someone who already has an exile.
        TextButton(enabled = !s.busy, onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Войти под другим аккаунтом", "Sign in as somebody else"))
        }
    }
}
