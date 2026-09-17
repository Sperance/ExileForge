package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.SERVER_COMMIT
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ForgeIcon
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.*

@Composable internal fun ServerScreen(s: ForgeState, vm: ForgeViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader(tr("Врата мира", "Gateway"), tr("Подключение к ktor-bestgame", "Connection to ktor-bestgame"), ForgeGlyphs.Portal)
        ForgePanel {
            Engraved(tr("Изгнанник", "Exile"))
            LoginForm(s, vm)
        }
        ForgePanel {
            Engraved(tr("Язык интерфейса", "Interface language"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Lang.entries.forEach { option ->
                    FilterChip(selected = s.lang == option, onClick = { vm.language(option) }, label = { Text(option.title) })
                }
            }
            Text(tr("Русский и английский переключаются мгновенно, выбор сохраняется на устройстве.", "Russian and English switch instantly; the choice is stored on this device."), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        ForgePanel {
            Engraved(tr("Сервер", "Server"))
            OutlinedTextField(s.serverDraft, vm::serverDraft, enabled = !s.busy, label = { Text(tr("Адрес сервера", "Server address")) },
                supportingText = { Text(tr("Без /api/v1: https://example.com/", "Without /api/v1: https://example.com/")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(enabled = !s.busy && !s.editorOpen, onClick = vm::connect, modifier = Modifier.fillMaxWidth()) { Text(tr("Сохранить и подключиться", "Save and connect")) }
            if (s.editorOpen) Text(tr("Перед сменой сервера закройте редактор в кузнице.", "Close the forge editor before switching servers."), color = Muted)
            OutlinedButton(enabled = !s.busy, onClick = vm::health, modifier = Modifier.fillMaxWidth()) { Text(tr("Проверить /system/health", "Check /system/health")) }
        }
        InfoCard(tr("Состояние сервера", "Server state"), s.health)
        InfoCard(tr("Локальная разработка", "Local development"),
            tr("Эмулятор: http://10.0.2.2:8080/\nТелефон: IP компьютера в вашей Wi-Fi сети. HTTP разрешён в debug-сборке; release использует HTTPS.",
               "Emulator: http://10.0.2.2:8080/\nPhone: your computer's IP on the same Wi-Fi. Cleartext HTTP is debug-only; release builds require HTTPS."))
        InfoCard(tr("Контракт сервера", "Server contract"),
            "0.13.0 · master · ${SERVER_COMMIT.take(12)}\n" +
            tr("Предметы, экипировка и персонажи. Выпадение и крафт выполняются сервером.", "Items, equipment and characters. Drops and crafting are resolved by the server."))
        ForgePanel {
            Engraved(tr("Иконки сервера", "Server icons"))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ForgeIcon("ui-catalog", Modifier.size(48.dp)) {
                    Icon(ForgeGlyphs.Sigil, null, tint = Muted, modifier = Modifier.size(24.dp))
                }
                Text(if(s.icons.ready) tr("Набор ${s.icons.manifest?.set}: ${s.icons.drawings.size} иконок · версия ${s.icons.version.take(12)}",
                        "Set ${s.icons.manifest?.set}: ${s.icons.drawings.size} icons · version ${s.icons.version.take(12)}")
                    else tr("Набор не загружен: используются встроенные эмблемы.", "The set is not loaded: bundled emblems are used."),
                    color = if(s.icons.ready) Gold else Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
            OutlinedButton(enabled = !s.busy, onClick = vm::reloadIcons, modifier = Modifier.fillMaxWidth()) { Text(tr("Обновить набор иконок", "Refresh the icon set")) }
            Text(tr("Картинки рисует сервер и отдаёт одним спрайтом; клиент хранит их до смены версии набора.", "The server draws the pictures and serves them as one sprite; the client keeps them until the set version changes."), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
