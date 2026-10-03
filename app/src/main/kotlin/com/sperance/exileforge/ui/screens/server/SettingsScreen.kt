package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.settings.SettingsViewModel
import com.sperance.exileforge.presentation.state.*
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** The pages behind the settings' rows: the language and the developers' tools, each with «back» to the list. */
private enum class SettingsPage(val title: String) {
    LANGUAGE("account.language"),
    SERVER("account.server"),
    CLIENT("account.client"),
    JOURNAL("account.journal"),
    TESTING("tester.window"),
    TESTERS("tester.accounts"),
    FEEDBACK("feedback.admin"),
    MAIL("mail.compose"),
}

/**
 * «Настройки» (3.77.0, variant A of three mockups, «Список с группами»): every setting on one screen in groups — the general
 * ones, the fight, the interface, the vibration — with its switch in its row; the developers' tools at the foot for a
 * tester or an administrator only. «Back» returns to the tab the settings were opened over.
 */
@Composable internal fun SettingsScreen(s: ForgeState, vm: ForgeViewModel, logs: List<RequestLog> = emptyList()) {
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    val open = page
    Column(Modifier.fillMaxSize()) {
        BackRow(ui(if (open == null) "common.back" else "settings.title")) { if (open == null) vm.closeSettings() else page = null }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(ui(open?.title ?: "settings.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            when (open) {
                null -> SettingsList(s, vm, logs) { page = it }

                SettingsPage.LANGUAGE -> ForgePanel {
                    LanguagePicker(s.lang, s.world.languages, enabled = !s.busy, onLanguage = vm::language)
                    MutedText(ui("account.language_note"))
                }

                SettingsPage.SERVER -> ServerPage(s, vm)

                SettingsPage.CLIENT -> ClientPage(s, vm)

                SettingsPage.JOURNAL -> RequestJournalPanel(vm, logs)

                SettingsPage.TESTING -> TestingPage(s, vm)

                SettingsPage.TESTERS -> TestersPage(s, vm)

                SettingsPage.FEEDBACK -> FeedbackAdminPage(s)

                SettingsPage.MAIL -> MailComposePage(s)
            }
        }
    }
}

@Composable private fun SettingsList(s: ForgeState, vm: ForgeViewModel, logs: List<RequestLog>, onPage: (SettingsPage) -> Unit) {
    val settingsModel = koinViewModel<SettingsViewModel>()
    val set by settingsModel.settings.collectAsStateWithLifecycle()
    fun change(edit: GameSettings.() -> GameSettings) = settingsModel.change(edit)
    RowGroup(ui("settings.general")) {
        AccountRow(Icons.Outlined.Language, ui("account.language"), value = s.lang.title) { onPage(SettingsPage.LANGUAGE) }
        ChoiceRow(Icons.Outlined.LightMode, ui("settings.keep_screen"), KeepScreen.entries, set.keepScreen, { ui("settings.keep.${it.name}") }) {
            change { copy(keepScreen = it) }
        }
        UpdateRow()
        LocalGuideDesk.current?.let { desk ->
            AccountRow(Icons.Outlined.Lightbulb, ui("guide.reset"), chevron = false) {
                desk.reset()
                vm.announce(ui("settings.hints_done"))
            }
        }
    }
    RowGroup(ui("settings.fight")) {
        ChoiceRow(Icons.Outlined.FastForward, ui("settings.speed"), GameSettings.SPEEDS, set.fightSpeed, { "×$it" }) { change { copy(fightSpeed = it) } }
        ChoiceRow(Icons.Outlined.Whatshot, ui("settings.damage"), DamageNumbers.entries, set.damageNumbers, { ui("settings.damage.${it.name}") }) {
            change { copy(damageNumbers = it) }
        }
        ChoiceRow(
            Icons.Outlined.Pause,
            ui("settings.pause"),
            GameSettings.PAUSES,
            set.autoPause,
            { if (it == 0) ui("settings.off") else "$it%" },
            note = ui("settings.pause_note"),
        ) { change { copy(autoPause = it) } }
    }
    RowGroup(ui("settings.interface")) {
        ChoiceRow(Icons.Outlined.FormatSize, ui("settings.text"), TextSize.entries, set.textSize, { it.name }) { change { copy(textSize = it) } }
        SwitchRow(Icons.Outlined.Animation, ui("settings.animations"), ui("settings.animations_note"), set.animations) { change { copy(animations = it) } }
    }
    RowGroup(ui("settings.vibration")) {
        SwitchRow(Icons.Outlined.Vibration, ui("settings.buzz_danger"), ui("settings.buzz_danger_note"), set.buzzDanger) { change { copy(buzzDanger = it) } }
        SwitchRow(Icons.Outlined.TouchApp, ui("settings.buzz_buttons"), ui("settings.buzz_buttons_note"), set.buzzButtons) { change { copy(buzzButtons = it) } }
    }
    // The developers' tools (3.77.0): moved here from the account, seen by a tester or an administrator alone.
    if (s.isTester || s.isAdmin) {
        RowGroup(ui("settings.developers")) {
            AccountRow(
                Icons.Outlined.Dns,
                ui("account.server"),
                value = ui(if (s.link.offline) "account.offline" else "account.online"),
                dot = if (s.link.offline) LifeRed else Vital,
            ) { onPage(SettingsPage.SERVER) }
            AccountRow(Icons.Outlined.Info, ui("account.client"), value = "API $API_REVISION") { onPage(SettingsPage.CLIENT) }
            AccountRow(Icons.AutoMirrored.Outlined.ReceiptLong, ui("account.journal"), value = logs.size.toString()) { onPage(SettingsPage.JOURNAL) }
            if (s.isTester) AccountRow(Icons.Outlined.Science, ui("tester.window"), enabled = !s.busy) { onPage(SettingsPage.TESTING) }
            if (s.isAdmin) AccountRow(Icons.Outlined.Group, ui("tester.accounts"), enabled = !s.busy) { onPage(SettingsPage.TESTERS) }
            if (s.isAdmin) AccountRow(Icons.Outlined.BugReport, ui("feedback.admin"), enabled = !s.busy) { onPage(SettingsPage.FEEDBACK) }
            if (s.isAdmin) AccountRow(Icons.Outlined.Mail, ui("mail.compose"), enabled = !s.busy) { onPage(SettingsPage.MAIL) }
            // Turning the administrator's tools off hides their tab, so the way back cannot live only inside it.
            if (BuildConfig.DEBUG && s.isAdmin && !s.adminTools) {
                AccountRow(Icons.Outlined.AdminPanelSettings, ui("account.tools_back"), enabled = !s.busy, chevron = false) { vm.mode(AppMode.ADMIN) }
            }
        }
    }
}

/** The version, the answer of the last check asked for by hand, and «Проверить». */
@Composable private fun UpdateRow() {
    val updates = LocalUpdates.current ?: return
    val state by updates.state.collectAsStateWithLifecycle()
    val note = when {
        state.checking -> ui("update.checking")
        state.upToDate -> ui("update.up_to_date")
        else -> state.failure
    }
    SettingRow(Icons.Outlined.SystemUpdate, ui("settings.updates"), listOfNotNull(ui("app.version", BuildConfig.VERSION_NAME), note).joinToString(" · ")) {
        ForgeOutlinedButton(enabled = !state.checking, onClick = updates::checkNow, contentPadding = PaddingValues(horizontal = 10.dp)) {
            Text(ui("settings.check"), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** A row of the settings: the glyph, the name with a line under it, and what sets it on the right. */
@Composable private fun SettingRow(icon: ImageVector, label: String, note: String? = null, trailing: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            note?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        trailing()
    }
}

@Composable private fun SwitchRow(icon: ImageVector, label: String, note: String?, on: Boolean, onChange: (Boolean) -> Unit) = SettingRow(icon, label, note) {
    Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Parchment, uncheckedTrackColor = PanelRaised))
}

/** A choice of a few: the options side by side as one segmented strip, the chosen one lit. */
@Composable private fun <T> ChoiceRow(
    icon: ImageVector,
    label: String,
    options: List<T>,
    chosen: T,
    title: (T) -> String,
    note: String? = null,
    onChoose: (T) -> Unit,
) = SettingRow(icon, label, note) {
    val shape = RoundedCornerShape(8.dp)
    Row(Modifier.clip(shape).background(PanelRaised, shape).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEach { option ->
            val on = option == chosen
            Text(
                title(option),
                color = if (on) Ink else Muted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (on) Gold else PanelRaised).clickable { onChoose(option) }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
    }
}
