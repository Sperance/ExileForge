package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.RunPhase
import com.sperance.exileforge.core.campaign.run.reportContext
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.data.settings.DraftStore
import com.sperance.exileforge.platform.VolumeChord
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.nav.reportPlace
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.ui.theme.*

/** The beetle: a bug report from wherever the player is. */
@Composable fun BugButton(modifier: Modifier = Modifier, tint: Color = Muted, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = modifier) { Icon(Icons.Outlined.BugReport, ui("bug.open"), tint = tint, modifier = Modifier.size(22.dp)) }
}

/** Who opens the report sheet: the app hands it down, so a screen's own header carries the beetle instead of a button floating over it. */
val LocalBugReport = staticCompositionLocalOf<(() -> Unit)?> { null }

/** The beetle in a screen's header; nothing where no report can be opened. */
@Composable fun BugAction(modifier: Modifier = Modifier, tint: Color = Muted) {
    LocalBugReport.current?.let { BugButton(modifier, tint, it) }
}

/** Аккорд громкости приложения: его держит Activity; окна диалогов цепляются к нему сами ([ChordOverDialog]). */
val LocalVolumeChord = staticCompositionLocalOf<VolumeChord?> { null }

/**
 * Аккорд громкости поверх окна диалога: композиция внутри `ForgeSheet`, `Dialog` или `AlertDialog` оборачивает callback
 * своего окна ([VolumeChord.attach]) - Громкость↑+↓ открывает отчёт и поверх диалога. Вне окна диалога ничего не делает.
 */
@Composable fun ChordOverDialog() {
    val chord = LocalVolumeChord.current ?: return
    val view = LocalView.current
    DisposableEffect(chord, view) {
        val window = generateSequence(view.parent) { it.parent }.filterIsInstance<DialogWindowProvider>().firstOrNull()?.window
        val detach = window?.let(chord::attach) ?: {}
        onDispose { detach() }
    }
}

/**
 * The report sheet: where the player is, a field for the words, and the send. Nothing else goes with it —
 * no context of the device and the hero, no tail of the request journal; снимков экрана нет. Под формой -
 * «Мои отчёты» и «Известные» ([onReports]); подсказки «Похожее уже известно» во время набора нет.
 *
 * Только отчёт об ошибке; черновик возвращается с устройства и пишется по мере набора.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugSheet(
    game: GameUi,
    route: Route,
    run: ExpeditionRun?,
    drafts: DraftStore,
    onDismiss: () -> Unit,
    onReports: (ReportsTab) -> Unit,
    onSend: (BugReportRequest) -> Unit,
) {
    val screen = remember { bugScreen(route, run) }
    val kind = FeedbackKind.BUG
    var text by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        if (text.isEmpty()) text = drafts.read().texts[kind].orEmpty()
    }
    val clipboard = LocalClipboardManager.current
    val limit = game.inputs.report
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("bug.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            Text(ui("bug.where", screen), color = Rune, style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                text,
                {
                    text = it.take(limit)
                    drafts.save(kind, text)
                },
                label = { Text(ui("bug.text")) },
                minLines = 4,
                supportingText = { Text(ui("bug.count", text.length, limit)) },
                trailingIcon = {
                    IconButton(onClick = { clipboard.setText(AnnotatedString(text)) }, enabled = text.isNotBlank()) {
                        Icon(Icons.Outlined.ContentCopy, ui("bug.copy"), tint = if (text.isNotBlank()) Gold else Muted, modifier = Modifier.size(20.dp))
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            )
            ForgeButton(
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onSend(BugReportRequest(text.trim().take(limit), screen, bugContext(route, run), kind))
                    onDismiss()
                },
            ) { Text(ui("bug.send")) }
            if (game.session.signedIn) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportsTab.entries.forEach { tab ->
                        ForgeOutlinedButton(onClick = {
                            onDismiss()
                            onReports(tab)
                        }, modifier = Modifier.weight(1f)) { Text(ui(if (tab == ReportsTab.MINE) "feedback.open_mine" else "feedback.open_known")) }
                    }
                }
            }
        }
    }
}

/**
 * Где игрок, ключами контекста: место для человека и раздел игры, в забеге - зона, уровень, фаза и семя карты. Сервер
 * выносит их в публичный Issue строками.
 */
private fun bugContext(route: Route, run: ExpeditionRun?): Map<String, String> {
    val place = reportPlace(route)
    val facts = run?.reportContext().orEmpty()
    val area = when {
        run == null -> place.area
        facts["run.phase"] == RunPhase.FIGHT.name -> "combat"
        else -> "run"
    }
    return mapOf("place" to place.name, "area" to area) + facts
}

/** The place, as one line: the phase, the tab or the building, the open sheet of the game. */
private fun bugScreen(route: Route, run: ExpeditionRun?): String = when {
    route.phase != AppPhase.GAME -> route.phase.name
    run != null -> "RUN:${run.zone.code}"
    route == Route.Atlas -> "ATLAS"
    else -> listOfNotNull("TAB:${route.tab}", route.building?.name).joinToString("/")
}
