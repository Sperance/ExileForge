package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.SERVER_COMMIT
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.InfoCard
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.*

@Composable internal fun ServerScreen(s: ForgeState, vm: ForgeViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Врата мира", style = MaterialTheme.typography.headlineLarge)
        Text("Подключение к ktor-bestgame", color = Muted)
        LoginForm(s, vm)
        OutlinedTextField(s.serverDraft, vm::serverDraft, enabled = !s.busy, label = { Text("Адрес сервера") }, supportingText = { Text("Без /api/v1: https://example.com/") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(enabled = !s.busy && !s.editorOpen, onClick = vm::connect, modifier = Modifier.fillMaxWidth()) { Text("Сохранить и подключиться") }
        if (s.editorOpen) Text("Перед сменой сервера закройте редактор в кузнице.", color = Muted)
        OutlinedButton(enabled = !s.busy, onClick = vm::health, modifier = Modifier.fillMaxWidth()) { Text("Проверить /system/health") }
        InfoCard("Состояние сервера", s.health)
        InfoCard("Локальная разработка", "Эмулятор: http://10.0.2.2:8080/\nТелефон: IP компьютера в вашей Wi-Fi сети. HTTP разрешён в debug-сборке; release использует HTTPS.")
        InfoCard("Контракт сервера", "feature/poe-catalog-crafting · ${SERVER_COMMIT.take(12)}\nПредметы, экипировка и персонажи. Выпадение и крафт выполняются сервером.")
    }
}
