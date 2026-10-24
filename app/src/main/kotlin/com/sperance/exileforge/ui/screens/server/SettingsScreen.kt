package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Redeem
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.app.AccountUi
import com.sperance.exileforge.core.app.DamageNumbers
import com.sperance.exileforge.core.app.GameSettings
import com.sperance.exileforge.core.app.KeepScreen
import com.sperance.exileforge.core.app.TextSize
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.session.Buzz
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.session.SessionViewModel
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The pages behind the settings' rows: the language and the developers' tools, each with «back» to the list. [paged] - страница
 * сама - серверный список страницами ([PagedColumn]): общей прокрутки у неё нет, её строки листает её ленивый список.
 */
internal enum class SettingsPage(val title: String, val paged: Boolean = false) {
    LANGUAGE("account.language"),
    SERVER("account.server"),
    CLIENT("account.client"),
    TESTING("tester.window"),
    FEEDBACK("feedback.admin", paged = true),
    MODERATION("moderation.title", paged = true),
    MAIL("mail.compose"),
    REPORTS("settings.reports", paged = true),
    WHATS_NEW("settings.whats_new"),
}

/**
 * Настройки поверх забега: тот же экран во весь экран над походом, Разломом или испытанием - стек экранов под ними
 * не трогается; корень цепочки «назад» - меню забега, «назад» с настроек закрывает их.
 */
@Composable internal fun SettingsOverlay(onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        ChordOverWindow()
        Box(Modifier.fillMaxSize().background(Ink).systemBarsPadding()) { SettingsScreen(root = ui("run.menu"), onClose = onClose) }
    }
}

/**
 * «Настройки» (variant A of three mockups, «Список с группами»): every setting on one screen in groups — the general
 * ones, the fight, the interface, the vibration — with its switch in its row; the developers' tools at the foot for a
 * tester or an administrator only.
 *
 * Путь назад - одна цепочка [BackTrailHost] от [root] (экран, над которым открыты настройки): «Настройки», страница, досье - её
 * уровни, своих стрелок у страниц нет. Страница, открытая прямо с экрана «Аккаунт», - сама первый уровень и закрывает настройки
 * целиком; страница, что умеет перечитываться, объявляет это сама.
 */
@Composable internal fun SettingsScreen(direct: String? = null, root: String = ui("account.title"), onClose: (() -> Unit)? = null) {
    val account by koinViewModel<ServerViewModel>().ui.collectAsStateWithLifecycle()
    val shell: ShellViewModel = koinViewModel()
    val opened = remember(direct) { SettingsPage.entries.firstOrNull { it.name == direct } }
    var page by rememberSaveable(direct) { mutableStateOf(opened) }
    val open = page
    val close = { onClose?.invoke() ?: shell.closeSettings() }
    BackTrailHost(root) {
        if (opened != null) {
            TrailLevel(ui(opened.title), onBack = close) { SettingsBody(account, opened) { page = it } }
        } else {
            TrailLevel(ui("settings.title"), onBack = close) {
                if (open == null) {
                    SettingsBody(account, null) { page = it }
                } else {
                    TrailLevel(ui(open.title), onBack = { page = null }) { SettingsBody(account, open) { page = it } }
                }
            }
        }
    }
}

/**
 * Тело уровня настроек: список ([open] null) или страница; своего заголовка нет - уровень называет цепочка. Страница со списком
 * страницами ([SettingsPage.paged]) листается своим списком, остальные - общей прокруткой ([SettingsScroll]).
 */
@Composable private fun SettingsBody(account: AccountUi, open: SettingsPage?, onPage: (SettingsPage) -> Unit) {
    val sessionModel: SessionViewModel = koinViewModel()
    val page: @Composable () -> Unit = {
        when (open) {
            null -> SettingsList(account, onPage)

            SettingsPage.LANGUAGE -> ForgePanel {
                LanguagePicker(account.lang, account.world.languages, enabled = !account.busy, onLanguage = sessionModel::language)
            }

            SettingsPage.SERVER -> ServerPage(account)

            SettingsPage.CLIENT -> ClientPage(account)

            SettingsPage.TESTING -> TestingPage(account)

            SettingsPage.FEEDBACK -> FeedbackAdminPage(account)

            SettingsPage.MODERATION -> ModerationPage(account)

            SettingsPage.MAIL -> MailComposePage(account)

            SettingsPage.REPORTS -> OwnReportsPage(account.seesIssues, account.inputs)

            SettingsPage.WHATS_NEW -> WhatsNewPage()
        }
    }
    RefreshHost(rememberPageRefresh(), Modifier.fillMaxSize()) {
        if (open?.paged == true) page() else SettingsScroll { page() }
    }
}

/** Страница настроек, что листается целиком: общие поля и промежутки страниц ([SettingsPadding]). */
@Composable internal fun SettingsScroll(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(SettingsPadding), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
}

/** Поля страницы настроек - и у листающейся целиком ([SettingsScroll]), и у списка страницами ([PagedColumn]). */
internal val SettingsPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)

@Composable private fun SettingsList(account: AccountUi, onPage: (SettingsPage) -> Unit) {
    val shell: ShellViewModel = koinViewModel()
    val sessionModel: SessionViewModel = koinViewModel()
    val settingsModel = koinViewModel<SettingsViewModel>()
    val set by settingsModel.settings.collectAsStateWithLifecycle()
    val speeds by settingsModel.speeds.collectAsStateWithLifecycle()
    fun change(edit: GameSettings.() -> GameSettings) = settingsModel.change(edit)
    RowGroup(ui("settings.general")) {
        val shell: ShellViewModel = koinViewModel()
        val sessionModel: SessionViewModel = koinViewModel()
        AccountRow(Icons.Outlined.Language, ui("account.language"), value = account.lang.title) { onPage(SettingsPage.LANGUAGE) }
        ChoiceRow(Icons.Outlined.LightMode, ui("settings.keep_screen"), KeepScreen.entries, set.keepScreen, { ui("settings.keep.${it.name}") }) {
            change { copy(keepScreen = it) }
        }
        UpdateRow()
        AccountRow(Icons.Outlined.History, ui("settings.whats_new")) { onPage(SettingsPage.WHATS_NEW) }
        ReportRows(onPage)
    }
    RowGroup(ui("settings.fight")) {
        ChoiceRow(
            Icons.Outlined.FastForward,
            ui("settings.speed"),
            speeds,
            set.fightSpeed,
            { ui("settings.speed.option", it) },
            note = ui("settings.speed_note"),
        ) { change { copy(fightSpeed = it) } }
        SwitchRow(Icons.Outlined.DirectionsRun, ui("settings.swift"), ui("settings.swift_note"), set.swiftAfterBoss) { change { copy(swiftAfterBoss = it) } }
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
        SwitchRow(Icons.Outlined.AutoAwesome, ui("settings.simple_effects"), ui("settings.simple_effects_note"), set.simpleEffects) { change { copy(simpleEffects = it) } }
    }
    RowGroup(ui("settings.vibration")) {
        val buzzes = LocalBuzzes.current
        SwitchRow(Icons.Outlined.Vibration, ui("settings.buzz_danger"), ui("settings.buzz_danger_note"), set.buzzDanger) { on ->
            change { copy(buzzDanger = on) }
            if (on) buzzes?.probe(Buzz.DANGER)
        }
        SwitchRow(Icons.Outlined.TouchApp, ui("settings.buzz_buttons"), ui("settings.buzz_buttons_note"), set.buzzButtons) { on ->
            change { copy(buzzButtons = on) }
            if (on) buzzes?.probe(Buzz.BUTTON)
        }
    }
}

/**
 * Разделы персонала: модерация модератору и администратору, инструменты разработчика тестировщику и администратору.
 * Живут на экране «Аккаунт», а не в настройках; каждая строка открывает свою страницу настроек напрямую, коды наград
 * администратора - свой экран ([onRedemptions]).
 */
@Composable internal fun StaffGroups(account: AccountUi, onRedemptions: () -> Unit, onPage: (SettingsPage) -> Unit) {
    if (account.isModerator) {
        RowGroup(ui("moderation.group")) {
            AccountRow(Icons.Outlined.Gavel, ui("moderation.title"), enabled = !account.busy) { onPage(SettingsPage.MODERATION) }
            AccountRow(Icons.Outlined.BugReport, ui("feedback.admin"), enabled = !account.busy) { onPage(SettingsPage.FEEDBACK) }
        }
    }
    if (account.isTester || account.isAdmin) {
        RowGroup(ui("settings.developers")) {
            AccountRow(
                Icons.Outlined.Dns,
                ui("account.server"),
                value = ui(if (account.link.offline) "account.offline" else "account.online"),
                dot = if (account.link.offline) LifeRed else Vital,
            ) { onPage(SettingsPage.SERVER) }
            AccountRow(Icons.Outlined.Info, ui("account.client"), value = "API $API_REVISION") { onPage(SettingsPage.CLIENT) }
            if (account.grants) AccountRow(Icons.Outlined.Science, ui("tester.window"), enabled = !account.busy) { onPage(SettingsPage.TESTING) }
            if (account.isAdmin) AccountRow(Icons.Outlined.Mail, ui("mail.compose"), enabled = !account.busy) { onPage(SettingsPage.MAIL) }
            if (account.isAdmin) AccountRow(Icons.Outlined.Redeem, ui("redemption.title"), enabled = !account.busy, onClick = onRedemptions)
        }
    }
}

/**
 * «Что нового»: заметки пяти последних версий с GitHub - одинаковые карточки [ReleaseCard],
 * свежая раскрыта, прочие раскрываются касанием.
 */
@Composable private fun WhatsNewPage() {
    val updates = LocalUpdates.current ?: return
    val notes by updates.notes.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { updates.loadNotes() }
    val list = notes
    when {
        list == null -> MutedText(ui("settings.whats_new_loading"))

        list.isEmpty() -> MutedText(ui("settings.whats_new_failed"))

        else -> list.forEachIndexed { i, release ->
            var open by rememberSaveable(release.tag) { mutableStateOf(i == 0) }
            ReleaseCard(release, open) { open = !open }
        }
    }
}

/**
 * Отчёты в настройках: «Сообщить об ошибке» с подсказкой аккорда громкости (касание снимает экран и открывает лист отчёта,
 * [com.sperance.exileforge.ui.components.rememberBugOpener]) и «Отчёты» - страница
 * своих отчётов ([onPage]); без системных листов приложения ([LocalSystemSheets]) строк нет.
 */
@Composable private fun ReportRows(onPage: (SettingsPage) -> Unit) {
    val sheets = LocalSystemSheets.current ?: return
    val openBug = com.sperance.exileforge.ui.components.rememberBugOpener(sheets)
    SettingRow(Icons.Outlined.BugReport, ui("bug.open"), ui("settings.bug_chord"), onClick = openBug) {
        Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
    AccountRow(Icons.Outlined.Forum, ui("settings.reports")) { onPage(SettingsPage.REPORTS) }
}

/** Версия сборки: проверка идёт сама после загрузки и возврата в игру; «Обновить» - только когда есть новая. */
@Composable private fun UpdateRow() {
    val updates = LocalUpdates.current ?: return
    val state by updates.state.collectAsStateWithLifecycle()
    val found = state.update
    SettingRow(Icons.Outlined.SystemUpdate, ui("settings.updates"), listOfNotNull(ui("app.version", BuildConfig.VERSION_NAME), updateNote(state)).joinToString(" · ")) {
        if (found != null) {
            ForgeButton(state = ButtonState.of(!state.installing), onClick = updates::install, contentPadding = PaddingValues(horizontal = 10.dp), kind = ButtonKind.Secondary) {
                Text(ui("update.install"), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * A row of the settings: the glyph, the name with a line under it, and what sets it on the right; [onClick] - касание всей строки.
 * Краски - [LocalMenuPalette]: в меню забега - краски режима.
 */
@Composable internal fun SettingRow(icon: ImageVector, label: String, note: String? = null, onClick: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit) {
    val palette = LocalMenuPalette.current
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = palette.accent, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = palette.text, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            note?.let { Text(it, color = palette.muted, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        trailing()
    }
}

@Composable internal fun SwitchRow(icon: ImageVector, label: String, note: String?, on: Boolean, onChange: (Boolean) -> Unit) = SettingRow(icon, label, note) {
    val palette = LocalMenuPalette.current
    Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = palette.accent, checkedThumbColor = palette.text, uncheckedTrackColor = palette.well))
}

/** A choice of a few: the options side by side as one segmented strip, the chosen one lit. */
@Composable internal fun <T> ChoiceRow(
    icon: ImageVector,
    label: String,
    options: List<T>,
    chosen: T,
    title: (T) -> String,
    note: String? = null,
    onChoose: (T) -> Unit,
) = SettingRow(icon, label, note) {
    val shape = RoundedCornerShape(8.dp)
    val palette = LocalMenuPalette.current
    Row(Modifier.clip(shape).background(palette.well, shape).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEach { option ->
            val on = option == chosen
            Text(
                title(option),
                color = if (on) palette.onAccent else palette.muted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (on) palette.accent else palette.well).clickable { onChoose(option) }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
    }
}
