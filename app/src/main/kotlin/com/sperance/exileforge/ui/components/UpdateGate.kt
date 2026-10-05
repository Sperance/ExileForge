package com.sperance.exileforge.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Reach
import com.sperance.exileforge.core.session.StartGate
import com.sperance.exileforge.core.session.StartVerdict
import com.sperance.exileforge.presentation.app.ReachState
import com.sperance.exileforge.presentation.app.StartWindow
import com.sperance.exileforge.presentation.features.UpdateState
import com.sperance.exileforge.presentation.features.UpdateViewModel
import com.sperance.exileforge.ui.theme.*
import com.sperance.exileforge.update.UpdateInstaller
import kotlinx.coroutines.delay

/** The update model of the activity (3.72.0), for the screens that show the version and check by hand. */
val LocalUpdates = staticCompositionLocalOf<UpdateViewModel?> { null }

/** Что из сохранённого на устройстве уже прочитано: мир, словарь, иконки. */
data class StartStages(val contentReady: Boolean, val dictionaryReady: Boolean, val iconsReady: Boolean) {
    val deviceReady: Boolean get() = contentReady && dictionaryReady && iconsReady
}

/**
 * Окно запуска и обновлений (3.86.0). Пока сервер не ответил - несколько строк: связь, версия, данные устройства. Ответил -
 * окна нет ([StartGate]), догрузка видна на экранах. Сервер молчит - так и написано, повтор сам через 10 с, «Повторить
 * сейчас» и «Играть без связи». Любая найденная сборка закрывает игру до установки. Ни одно окно запуска и обновления не
 * закрывается «назад» или касанием мимо. [busy] - идёт поход или испытание: обновление ждёт его конца.
 */
@Composable fun UpdateGate(
    updates: UpdateViewModel,
    busy: Boolean,
    stages: StartStages,
    window: StartWindow,
    onRetry: () -> Unit,
    diagnostics: () -> String,
) {
    // Не привязано к жизненному циклу (3.87.0): окно запуска обязано видеть каждое изменение, пока оно на экране.
    val s by updates.state.collectAsState()
    val reach = window.reach
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Back in the app (3.76.0): the build is asked again, unless a run is under way.
    val idle by rememberUpdatedState(!busy)
    LifecycleEventEffect(Lifecycle.Event.ON_START) { if (idle) updates.resumed() }
    // Back from the settings or the system installer: the permission is asked again, a closed installer stops waiting.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { updates.foreground() }
    // The window is the start's, once: a server lost later in the game is the banner's.
    var released by rememberSaveable { mutableStateOf(false) }
    val verdict = StartGate.verdict(reach.reach, s.mandatory && !busy, window.elapsedMs)
    LaunchedEffect(verdict) { if (verdict == StartVerdict.RELEASE) released = true }
    // Предел окна и на стороне экрана: даже если модель не дошла до него, окно уходит само.
    LaunchedEffect(Unit) {
        delay(StartGate.LIMIT_MS)
        released = true
    }
    var waited by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(CONTINUE_AFTER_MS)
        waited = true
    }
    when {
        verdict == StartVerdict.UPDATE -> Locked { UpdateBody(s, updates) }

        !released -> Locked {
            Text(ui("start.title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            StartRows(s, stages, reach)
            if (verdict == StartVerdict.UNREACHABLE) {
                Text(ui("start.unreachable"), color = Ember, style = MaterialTheme.typography.bodyMedium)
                ForgeButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text(ui("start.retry")) }
            }
            if (verdict == StartVerdict.UNREACHABLE || waited) {
                ForgeOutlinedButton(onClick = { released = true }, modifier = Modifier.fillMaxWidth()) { Text(ui("start.offline")) }
                CopyLog {
                    "--- window\nlifecycle=${lifecycle.currentState} verdict=$verdict released=$released busy=$busy\nseen $window\nseen update checking=${s.checking} " +
                        "update=${s.update?.info?.versionName} failure=${s.failure}\nlive update checking=${updates.state.value.checking}\n$stages\n" + diagnostics()
                }
            }
        }

        s.askSources -> Locked { SourcesBody() }
    }
}

/** Строки окна запуска: что проверяется или качается сейчас, по пункту на строку. */
@Composable private fun StartRows(s: UpdateState, stages: StartStages, reach: ReachState) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(TICK_MS)
            value = System.currentTimeMillis()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (reach.reach) {
            Reach.CONNECTING -> StartRow(ui("start.row.server"), RowState.RUN, ui("start.state.connecting"))
            Reach.ANSWERED -> StartRow(ui("start.row.server"), RowState.DONE, ui("start.state.answered"))
            Reach.UNREACHABLE -> StartRow(ui("start.row.server"), RowState.FAIL, ui("start.state.retry_in", ((reach.retryAt - now + 999) / 1000).coerceAtLeast(0)))
        }
        when {
            s.progress != null -> StartRow(ui("start.row.version"), RowState.RUN, ui("start.state.downloading", (s.progress * 100).toInt()))
            s.checking -> StartRow(ui("start.row.version"), RowState.RUN, ui("start.state.checking"))
            s.update != null -> StartRow(ui("start.row.version"), RowState.DONE, ui("start.state.found", s.update.info.versionName))
            s.failure != null -> StartRow(ui("start.row.version"), RowState.FAIL, ui("start.state.unchecked"))
            else -> StartRow(ui("start.row.version"), RowState.DONE, ui("start.state.latest", BuildConfig.VERSION_NAME))
        }
        StartRow(ui("start.row.device"), if (stages.deviceReady) RowState.DONE else RowState.RUN, ui(if (stages.deviceReady) "start.state.ready" else "start.state.reading"))
    }
}

private enum class RowState { RUN, DONE, FAIL }

/** Пункт: значок состояния, что это и коротко - где оно. */
@Composable private fun StartRow(title: String, state: RowState, status: String) {
    val color: Color = when (state) {
        RowState.RUN -> Gold
        RowState.DONE -> Vital
        RowState.FAIL -> Ember
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            when (state) {
                RowState.RUN -> CircularProgressIndicator(Modifier.size(16.dp), color = color, strokeWidth = 2.dp)
                RowState.DONE -> Text("✓", color = color, style = MaterialTheme.typography.titleSmall)
                RowState.FAIL -> Text("✕", color = color, style = MaterialTheme.typography.titleSmall)
            }
        }
        Text(title, color = Parchment, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(status, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

/** Журнал запуска в буфер обмена: метка «скопировано» держится пару секунд. */
@Composable private fun CopyLog(diagnostics: () -> String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_MS)
            copied = false
        }
    }
    TextButton(onClick = {
        clipboard.setText(AnnotatedString(diagnostics()))
        copied = true
    }, modifier = Modifier.fillMaxWidth()) { Text(ui(if (copied) "start.copied" else "start.copy_log"), color = Gold) }
}

private const val COPIED_MS = 2_000L

/** Как часто окно пересчитывает отсчёт до повтора. */
private const val TICK_MS = 500L

/** Сколько окно ждёт, прежде чем предложить войти, не дожидаясь. */
private const val CONTINUE_AFTER_MS = 15_000L

/** «Установка неизвестных приложений» не разрешена: только путь в настройки; вернувшись, игра спрашивает снова. */
@Composable private fun ColumnScope.SourcesBody() {
    val context = LocalContext.current
    Text(ui("update.sources_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
    MutedText(ui("update.sources_note"))
    ForgeButton(onClick = { context.startActivity(UpdateInstaller.permissionScreen(context)) }, modifier = Modifier.fillMaxWidth()) {
        Text(ui("update.open_settings"))
    }
}

/** A window nothing outside closes: no back, no tap beside it. */
@Composable private fun Locked(content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp).background(PanelRaised, DialogShape).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** The build to take: from what to what, what is new, and the download into the installer. */
@Composable private fun ColumnScope.UpdateBody(s: UpdateState, updates: UpdateViewModel) {
    val update = s.update ?: return
    val context = LocalContext.current
    Text(ui("update.title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
    if (update.info.size > 0) MutedText(ui("update.size", "%.1f".format(update.info.size / 1_048_576.0)))
    Text(ui("update.notes"), color = Parchment, style = MaterialTheme.typography.labelLarge)
    Column(Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(update.info.versionName, color = GoldBright, style = MaterialTheme.typography.labelLarge)
        if (update.info.notes.isNotBlank()) MutedText(update.info.notes)
    }
    val progress = s.progress
    when {
        progress != null -> {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised)
            MutedText(ui("update.downloading", (progress * 100).toInt()))
        }

        s.installing -> {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised)
            MutedText(ui("update.installing"))
        }

        s.needsPermission -> {
            InfoCard(ui("update.permission_title"), ui("update.permission_note"))
            ForgeButton(onClick = {
                context.startActivity(UpdateInstaller.permissionScreen(context))
                updates.permissionAsked()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("update.open_settings"))
            }
        }

        else -> {
            s.error?.let { InfoCard(ui("update.failed_title"), it, failure = true) }
            ForgeButton(onClick = updates::install, modifier = Modifier.fillMaxWidth()) { Text(ui(if (s.error != null) "update.retry" else "update.install")) }
            // The way around the installer: the release page on GitHub, the APK by hand.
            if (s.error != null) {
                ForgeOutlinedButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.pageUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }, modifier = Modifier.fillMaxWidth()) { Text(ui("update.browser")) }
            }
        }
    }
}

/** This build's version, dim: on the way in and in the account (3.72.0). */
@Composable fun VersionLabel(modifier: Modifier = Modifier) {
    Text(ui("app.version", BuildConfig.VERSION_NAME), modifier = modifier, color = Muted.copy(alpha = .55f), style = MaterialTheme.typography.labelSmall)
}

/** The account's version row and «Проверить обновления» (3.72.0). */
@Composable fun UpdateCard() {
    val updates = LocalUpdates.current ?: return
    val s by updates.state.collectAsStateWithLifecycle()
    InfoCard(
        ui("update.card_title"),
        ui("update.version", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE) +
            if (s.upToDate) "\n" + ui("update.up_to_date") else s.failure?.let { "\n$it" }.orEmpty(),
    )
    ForgeOutlinedButton(enabled = !s.checking, onClick = updates::checkNow, modifier = Modifier.fillMaxWidth()) {
        Text(ui(if (s.checking) "update.checking" else "update.check"))
    }
}
