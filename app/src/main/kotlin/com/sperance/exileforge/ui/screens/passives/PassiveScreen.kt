package com.sperance.exileforge.ui.screens.passives

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.passives.*
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ForgeIcon
import com.sperance.exileforge.ui.icons.LocalForgeIcons
import com.sperance.exileforge.ui.theme.*
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
            ScreenHeader(tr("Древо навыков", "Passive tree"), tr("Общее дерево · личный путь каждого героя", "One tree · a personal path for every hero"), ForgeGlyphs.Constellation)
            EntitySpinner(tr("Персонаж", "Character"), s.characterId, EntitySource.CHARACTER, !s.busy && s.pending == null, vm::characterId)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = s.signedIn && !s.busy && s.characterId.isNotBlank(), onClick = vm::loadPassives) { Text(tr("Обновить дерево", "Refresh the tree")) }
                TextButton(enabled = !s.busy, onClick = { vm.tab(4) }) { Text(tr("К герою", "Back to the hero")) }
            }
            if(!s.signedIn) Text(tr("Войдите во вкладке «Аккаунт».", "Sign in on the Account tab."))
            if(s.passivePending != null) {
                Text(tr("Ответ не подтверждён. Исходное действие сохранено.", "The response was not confirmed. The original action is stored."), color = Gold)
                Button(enabled = !s.busy && s.signedIn, onClick = vm::retryPassive) { Text(tr("Подтвердить действие", "Confirm the action")) }
            }
        }
        if(tree != null && state != null) {
            item {
                ForgePanel {
                    Text(tree.name, style = MaterialTheme.typography.titleLarge, color = GoldBright)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatGlobe(tr("Свободно", "Free"), state.availablePoints.toString(), 1f, Rune)
                        StatGlobe(tr("Потрачено", "Spent"), "${state.spentPoints} / ${state.totalPoints}",
                            if(state.totalPoints > 0) state.spentPoints.toFloat() / state.totalPoints else 0f, Gold)
                    }
                    Text(tr("Жесты: двигайте и масштабируйте. Узлы также доступны в списке ниже.", "Gestures: drag and pinch. The nodes are also listed below."), style = MaterialTheme.typography.bodySmall, color = Muted)
                    state.lockedReason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
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
                OutlinedButton(enabled = !blocked && state.allocated.isNotEmpty() && state.lockedReason == null, onClick = { confirm = PassiveAction.RESET to null }) { Text(tr("Сбросить все навыки", "Refund every node")) }
                Text(tr("Сброс бесплатный. Если уменьшатся нужные предметам характеристики, сначала снимите эти предметы.", "Refunding is free. If stats required by your gear drop, unequip those items first."), color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Найти узел или характеристику", "Find a node or a stat")) }, singleLine = true)
                FilterChip(onlyAvailable, onClick = { onlyAvailable = !onlyAvailable }, label = { Text(tr("Только доступные", "Available only")) })
            }
            val visible = tree.nodes.filter { node ->
                (!onlyAvailable || node.id in state.allocatable) && (query.isBlank() || node.name.contains(query, true) || node.effects.any { passiveStatName(it.stat).contains(query, true) })
            }
            if(visible.isEmpty()) item { Text(tr("Подходящих узлов нет.", "No matching nodes.")) }
            items(visible, key = { it.id }) { node ->
                val allocated = node.id in state.allocated
                ForgePanel(Modifier.clickable { selected = node.id; scope.launch { list.animateScrollToItem(3) } }, accent = if(allocated) Gold else Muted) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ForgeIcon(LocalForgeIcons.current.forNode(node), Modifier.size(34.dp), description = node.name)
                        Column {
                            Text(node.name, color = if(allocated) GoldBright else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                            Text("${passiveKind(node.kind)} · " + when {
                                allocated -> tr("изучен", "allocated")
                                node.id in state.allocatable -> tr("доступен", "available")
                                else -> tr("закрыт", "locked")
                            }, style = MaterialTheme.typography.labelMedium, color = Muted)
                        }
                    }
                    node.effects.forEach { Text(passiveEffectText(it), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
    confirm?.let { (action, nodeId) ->
        AlertDialog(onDismissRequest = { confirm = null }, containerColor = Panel, titleContentColor = Gold,
            title = { Text(if(action == PassiveAction.RESET) tr("Сбросить всё дерево?", "Refund the whole tree?") else tr("Снять навык?", "Refund the node?")) },
            text = { Text(tr("Очки вернутся герою. Сервер проверит связность дерева и требования экипировки.", "The points return to the hero. The server checks tree connectivity and gear requirements.")) },
            confirmButton = { TextButton(enabled = !blocked, onClick = { confirm = null; vm.changePassive(action, nodeId) }) { Text(tr("Подтвердить", "Confirm")) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(tr("Отмена", "Cancel")) } })
    }
}
