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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.data.settings.DraftStore
import com.sperance.exileforge.platform.VolumeChord
import com.sperance.exileforge.presentation.feedback.FeedbackViewModel
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/** The beetle (3.48.0): a bug report from wherever the player is. */
@Composable fun BugButton(modifier: Modifier = Modifier, tint: Color = Muted, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = modifier) { Icon(Icons.Outlined.BugReport, ui("bug.open"), tint = tint, modifier = Modifier.size(22.dp)) }
}

/** Who opens the report sheet (3.57.0): the app hands it down, so a screen's own header carries the beetle instead of a button floating over it. */
val LocalBugReport = staticCompositionLocalOf<(() -> Unit)?> { null }

/** The beetle in a screen's header; nothing where no report can be opened. */
@Composable fun BugAction(modifier: Modifier = Modifier, tint: Color = Muted) {
    LocalBugReport.current?.let { BugButton(modifier, tint, it) }
}

/** Аккорд громкости приложения (4.9.0): его держит Activity; окна диалогов цепляются к нему сами ([ChordOverDialog]). */
val LocalVolumeChord = staticCompositionLocalOf<VolumeChord?> { null }

/**
 * Аккорд громкости поверх окна диалога (4.9.0): композиция внутри `ForgeSheet`, `Dialog` или `AlertDialog` оборачивает callback
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
 * The report sheet: where the player is, a field for the words, and the send. Since 3.76.0 nothing else goes with it —
 * no context of the device and the hero, no tail of the request journal; снимков экрана нет (0.14.1, GH-ISSUES). Под формой -
 * «Похожее уже известно» ([SimilarKnown]) по черновику, ниже - «Мои отчёты» и «Известные» ([onReports], макет A).
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
    val model = koinViewModel<FeedbackViewModel>()
    val feedback by model.feedback.collectAsStateWithLifecycle()
    val activity by model.activity.collectAsStateWithLifecycle()
    // A bug or a suggestion (3.73.0): the player picks; a suggestion goes into the public list, so it needs an account.
    // The words of each and the kind last open come back from the device (3.75.0) and are kept as they are typed.
    var kind by remember { mutableStateOf(FeedbackKind.BUG) }
    val texts = remember { mutableStateMapOf<FeedbackKind, String>() }
    LaunchedEffect(Unit) {
        val draft = drafts.read()
        if (texts.isEmpty()) kind = draft.kind
        draft.texts.forEach { (k, v) -> if (k !in texts) texts[k] = v }
    }
    val text = texts[kind].orEmpty()
    // Похожее (0.14.1) ищется, когда игрок перестал печатать
    LaunchedEffect(text, kind) {
        delay(SIMILAR_PAUSE_MS)
        model.findSimilar(text)
    }
    val clipboard = LocalClipboardManager.current
    val limit = if (kind == FeedbackKind.SUGGESTION) game.inputs.suggestion else game.inputs.report
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("bug.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            PillTabs(listOf(kindTitle(FeedbackKind.BUG), kindTitle(FeedbackKind.SUGGESTION)), kind.ordinal, {
                kind = FeedbackKind.entries[it]
                drafts.save(kind, texts[kind].orEmpty())
            }, segmented = true)
            if (kind == FeedbackKind.BUG) {
                Text(ui("bug.where", screen), color = Rune, style = MaterialTheme.typography.labelLarge)
            } else {
                MutedText(ui(if (game.session.signedIn) "feedback.suggestion_hint" else "feedback.sign_in_first"))
            }
            OutlinedTextField(
                text,
                {
                    texts[kind] = it.take(limit)
                    drafts.save(kind, texts[kind].orEmpty())
                },
                label = { Text(ui(if (kind == FeedbackKind.BUG) "bug.text" else "feedback.text")) },
                minLines = 4,
                supportingText = { Text(ui("bug.count", text.length, limit)) },
                // The words alone to the clipboard (3.75.0), to keep or pass on elsewhere.
                trailingIcon = {
                    IconButton(onClick = { clipboard.setText(AnnotatedString(text)) }, enabled = text.isNotBlank()) {
                        Icon(Icons.Outlined.ContentCopy, ui("bug.copy"), tint = if (text.isNotBlank()) Gold else Muted, modifier = Modifier.size(20.dp))
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            )
            ForgeButton(
                enabled = text.isNotBlank() && (kind == FeedbackKind.BUG || game.session.signedIn),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onSend(BugReportRequest(text.trim().take(limit), screen, emptyMap(), kind))
                    onDismiss()
                },
            ) { Text(ui("bug.send")) }
            if (game.session.signedIn) {
                SimilarKnown(feedback.similar, !activity.busy) { id, vote -> model.vote(id, vote) }
                // Свои отчёты с их судьбой и известные проблемы с голосованием (0.14.1, макет A): две кнопки - на свою вкладку
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

/** Пауза после последней буквы, после которой ищется похожее. */
private const val SIMILAR_PAUSE_MS = 700L

/** The place, as one line: the phase, the tab or the building, the open sheet of the game. */
private fun bugScreen(route: Route, run: ExpeditionRun?): String = when {
    route.phase != AppPhase.GAME -> route.phase.name
    run != null -> "RUN:${run.zone.code}"
    route == Route.Atlas -> "ATLAS"
    else -> listOfNotNull("TAB:${route.tab}", route.building?.name).joinToString("/")
}
