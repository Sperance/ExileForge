package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.SERVER_BRANCH
import com.sperance.exileforge.core.contract.SERVER_COMMIT
import com.sperance.exileforge.core.contract.SERVER_VERSION
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.checks.LogCard
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel

@Composable internal fun ServerScreen(s: ForgeState, vm: ForgeViewModel, logs: List<RequestLog> = emptyList()) {
    var promoOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader(tr("Врата мира", "Gateway"), tr("Подключение к ktor-bestgame", "Connection to ktor-bestgame"), ForgeGlyphs.Portal)
        ForgePanel {
            Engraved(tr("Изгнанник", "Exile"))
            // A character is swapped by leaving the game, never from inside a tab: every button
            // below is bound to the one chosen in the menu, and this is the way back to it.
            s.character?.let { hero ->
                PropertyRow(tr("Персонаж", "Character"), hero.name + tr(" · ур. ${hero.level}", " · lvl ${hero.level}"), "character")
            }
            OutlinedButton(enabled = !s.busy && !s.editorOpen, onClick = vm::leaveGame, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Сменить персонажа", "Change character"))
            }
            if (s.editorOpen) Text(tr("Сначала закройте редактор.", "Close the editor first."), color = Muted, style = MaterialTheme.typography.bodySmall)
            // A reward is paid to a character, not to an account, so the code is asked for where
            // the character being played is already named — and the dialog names them again.
            OutlinedButton(enabled = !s.busy && s.characterId.isNotBlank(), onClick = { promoOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Ввести промокод", "Enter a promo code"))
            }
            LoginForm(s, vm)
        }
        // Every administrator tool moved to its own tab in 2.3.0. What stays here is the way back
        // into it: turning the tools off hides that tab, so the switch cannot live only inside it.
        if (s.isAdmin && !s.adminTools) ForgePanel {
            Engraved(tr("Администратор", "Administrator"))
            Text(tr("Инструменты сейчас скрыты — приложение выглядит так, как его видит игрок.",
                    "The tools are hidden — the app looks the way a player sees it."),
                color = Muted, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = !s.busy, onClick = { vm.mode(AppMode.ADMIN) }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Вернуть инструменты", "Bring the tools back"))
            }
        }
        ForgePanel {
            Engraved(tr("Язык интерфейса", "Interface language"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Lang.entries.forEach { option ->
                    FilterChip(selected = s.lang == option, onClick = { vm.language(option) }, label = { Text(option.title) })
                }
            }
            Text(tr("Русский и английский переключаются мгновенно, выбор сохраняется на устройстве.", "Russian and English switch instantly; the choice is stored on this device."), color = Muted, style = MaterialTheme.typography.bodySmall)
            // Names of things belong to the server since 0.14.0: without its dictionary the screens
            // print codes, so how much of it arrived is worth saying out loud.
            if (s.localeStrings > 0) PropertyRow(tr("Словарь сервера", "Server dictionary"),
                "${s.localeLanguage.uppercase()} · " + tr("строк: ${s.localeStrings}", "${s.localeStrings} strings"), "description")
            else Text(tr("Словарь сервера не загружен: названия предметов будут показаны кодами.",
                         "The server dictionary was not loaded: items will be shown by their codes."), color = Muted, style = MaterialTheme.typography.bodySmall)
            // Drawings come from the server too, and a missing set is invisible by design: every
            // hole falls back to a bundled emblem, so the count is the only way to notice one.
            if (s.iconKeys > 0) PropertyRow(tr("Иконки сервера", "Server icons"),
                tr("кодов: ${s.iconKeys}, рисунков: ${s.iconSprites}", "${s.iconKeys} codes, ${s.iconSprites} drawings"), "image")
            else Text(tr("Набор иконок не загружен: предметы рисуются встроенными эмблемами.",
                         "The icon set was not loaded: items are drawn with the bundled emblems."), color = Muted, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = !s.busy, onClick = { vm.refreshLocale(); vm.refreshIcons() }, modifier = Modifier.fillMaxWidth()) { Text(tr("Перечитать словарь и иконки", "Re-read the dictionary and icons")) }
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
            "$SERVER_VERSION · $SERVER_BRANCH · ${SERVER_COMMIT.take(12)}\n" +
            tr("Предметы, экипировка, инвентарь и персонажи. Роллы модификаторов выполняет сервер.",
               "Items, equipment, inventory and characters. Modifier rolls are resolved by the server."))
        InfoCard(tr("Вход", "Sign-in"),
            tr("Сервер не выдаёт токен: логин отвечает документом учётной записи, который живёт только в памяти приложения. Пароль уходит параметром запроса — используйте HTTPS.",
               "The server issues no token: a login answers with the account document, which lives in memory only. The password travels as a query parameter — use HTTPS."))
        RequestJournalPanel(vm, logs)
    }
    if (promoOpen) PromoCodeDialog(s, onDismiss = { promoOpen = false }) { code -> promoOpen = false; vm.redeem(code) }
}

/**
 * One promo code, for the character currently being played.
 *
 * The account may hold three exiles and the server pays the reward to exactly one of them, so the
 * dialog says which before anything is typed rather than after the goods have landed.
 */
@Composable private fun PromoCodeDialog(s: ForgeState, onDismiss: () -> Unit, onRedeem: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Panel, titleContentColor = Gold,
        title = { Text(tr("Промокод", "Promo code")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Награда придёт персонажу: ${s.character?.name.orEmpty()}",
                        "The reward goes to: ${s.character?.name.orEmpty()}"), color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(code, { code = it.take(100) }, label = { Text(tr("Код", "Code")) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = !s.busy && code.isNotBlank(), onClick = { onRedeem(code) }) {
            Text(tr("Получить награду", "Claim the reward")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Отмена", "Cancel")) } })
}

/**
 * The request journal, right where a connection is set up.
 *
 * The Checks tab needs an administrator, which is exactly what you do not have when the connection
 * itself is broken, so every failed attempt is readable here: method, path, status and both bodies.
 */
@Composable private fun RequestJournalPanel(vm: ForgeViewModel, logs: List<RequestLog>) {
    var expanded by remember { mutableStateOf(false) }
    ForgePanel {
        Engraved(tr("Журнал запросов", "Request journal"))
        Text(tr("Записей: ${logs.size}. Здесь виден точный адрес, статус и ответ сервера — журнал доступен и без входа.",
                "${logs.size} entries. The exact address, status and server answer are here, sign-in or not."),
            color = Muted, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) tr("Свернуть", "Hide") else tr("Показать", "Show")) }
            TextButton(enabled = logs.isNotEmpty(), onClick = vm::clearLogs) { Text(tr("Очистить", "Clear")) }
        }
        if (expanded) {
            if (logs.isEmpty()) Text(tr("Здесь появятся запросы и ответы сервера.", "Requests and server responses will appear here."), color = Muted)
            logs.take(12).forEach { LogCard(it) }
        }
    }
}
