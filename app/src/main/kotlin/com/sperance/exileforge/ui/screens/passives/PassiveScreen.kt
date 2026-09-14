package com.sperance.exileforge.ui.screens.passives

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.passives.*
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.EntitySpinner
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.coroutines.launch

@Composable fun PassiveScreen(s: ForgeState, vm: ForgeViewModel) {
    var selected by rememberSaveable(s.characterId) { mutableStateOf("origin") }
    var query by rememberSaveable { mutableStateOf("") }
    var onlyAvailable by rememberSaveable { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<Pair<PassiveAction, String?>?>(null) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val tree = s.passiveTree
    val state = s.passiveState.takeIf { s.passiveCharacterId == s.characterId }
    val blocked = s.busy || s.passivePending != null
    LaunchedEffect(s.characterId, s.sessionEpoch) {
        if(s.signedIn && s.characterId.isNotBlank() && !s.busy) vm.loadPassives()
    }
    LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Древо навыков", style = MaterialTheme.typography.headlineLarge, color = Gold)
            Text("Общее дерево · личный путь каждого героя", color = Muted)
            EntitySpinner("Персонаж", s.characterId, EntitySource.CHARACTER, !s.busy && s.pending == null, vm::characterId)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = s.signedIn && !s.busy && s.characterId.isNotBlank(), onClick = vm::loadPassives) { Text("Обновить дерево") }
                TextButton(enabled = !s.busy, onClick = { vm.tab(4) }) { Text("К герою") }
            }
            if(!s.signedIn) Text("Войдите во вкладке «Аккаунт».")
            if(s.passivePending != null) {
                Text("Ответ не подтверждён. Исходное действие сохранено.", color = Gold)
                Button(enabled = !s.busy && s.signedIn, onClick = vm::retryPassive) { Text("Подтвердить действие") }
            }
        }
        if(tree != null && state != null) {
            item {
                Text(tree.name, style = MaterialTheme.typography.titleLarge)
                Text("Свободно ${state.availablePoints} · потрачено ${state.spentPoints} / ${state.totalPoints}", color = Gold)
                Text("Жесты: двигайте и масштабируйте. Узлы также доступны в списке ниже.", style = MaterialTheme.typography.bodySmall)
                state.lockedReason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            item { PassiveGraph(tree, state.allocated, state.allocatable, selected) { selected = it } }
            item {
                tree.nodes.firstOrNull { it.id == selected }?.let { node ->
                    PassiveNodeDetails(node, state, blocked) { action, id ->
                        if(action == PassiveAction.ALLOCATE) vm.changePassive(action, id) else confirm = action to id
                    }
                }
            }
            item {
                OutlinedButton(enabled = !blocked && state.allocated.isNotEmpty() && state.lockedReason == null, onClick = { confirm = PassiveAction.RESET to null }) { Text("Сбросить все навыки") }
                Text("Сброс бесплатный. Если уменьшатся нужные предметам характеристики, сначала снимите эти предметы.", color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Найти узел или характеристику") }, singleLine = true)
                FilterChip(onlyAvailable, onClick = { onlyAvailable = !onlyAvailable }, label = { Text("Только доступные") })
            }
            val visible = tree.nodes.filter { node ->
                (!onlyAvailable || node.id in state.allocatable) && (query.isBlank() || node.name.contains(query, true) || node.effects.any { passiveStatName(it.stat).contains(query, true) })
            }
            if(visible.isEmpty()) item { Text("Подходящих узлов нет.") }
            items(visible, key = { it.id }) { node ->
                Card(onClick = { selected = node.id; scope.launch { list.animateScrollToItem(3) } }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(node.name, color = if(node.id in state.allocated) Gold else MaterialTheme.colorScheme.onSurface)
                        Text("${passiveKind(node.kind)} · ${if(node.id in state.allocated) "изучен" else if(node.id in state.allocatable) "доступен" else "закрыт"}", style = MaterialTheme.typography.labelMedium)
                        node.effects.forEach { Text(passiveEffectText(it), style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
    confirm?.let { (action, nodeId) ->
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text(if(action == PassiveAction.RESET) "Сбросить всё дерево?" else "Снять навык?") },
            text = { Text("Очки вернутся герою. Сервер проверит связность дерева и требования экипировки.") },
            confirmButton = { TextButton(enabled = !blocked, onClick = { confirm = null; vm.changePassive(action, nodeId) }) { Text("Подтвердить") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Отмена") } })
    }
}
