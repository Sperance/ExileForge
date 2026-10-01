package com.sperance.exileforge.ui.components

import com.sperance.exileforge.core.model.feedback.FeedbackKind
import android.os.Build
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.campaign.ExpeditionRun
import com.sperance.exileforge.core.contract.SERVER_VERSION
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.theme.*

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

/**
 * The report sheet: where the player is — the screen, the hero, the run, the versions and the device, shown as they will
 * be sent — a field for the words, and the send. The tail of the request journal rides along, bodies of sign-ins hidden
 * by the journal itself; no token is ever in it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BugSheet(s: ForgeState, run: ExpeditionRun?, logs: List<RequestLog>, onDismiss: () -> Unit, onSuggestions: () -> Unit,
                         onSend: (BugReportRequest) -> Unit) {
    val screen = remember { bugScreen(s, run) }
    val context = remember { bugContext(s, run) }
    var text by remember { mutableStateOf("") }
    // A bug or a suggestion (3.73.0): the player picks; a suggestion goes into the public list, so it needs an account.
    var kind by remember { mutableStateOf(FeedbackKind.BUG) }
    val limit = if (kind == FeedbackKind.SUGGESTION) s.inputs.suggestion else s.inputs.report
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("bug.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            PillTabs(listOf(kindTitle(FeedbackKind.BUG), kindTitle(FeedbackKind.SUGGESTION)), kind.ordinal, { kind = FeedbackKind.entries[it] }, segmented = true)
            if (kind == FeedbackKind.BUG) Text(ui("bug.where", screen), color = Rune, style = MaterialTheme.typography.labelLarge)
            else MutedText(ui(if (s.account.signedIn) "feedback.suggestion_hint" else "feedback.sign_in_first"))
            OutlinedTextField(text, { text = it.take(limit) }, label = { Text(ui(if (kind == FeedbackKind.BUG) "bug.text" else "feedback.text")) }, minLines = 4,
                supportingText = { Text(ui("bug.count", text.length, limit)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp))
            ForgeButton(enabled = text.isNotBlank() && !s.busy && (kind == FeedbackKind.BUG || s.account.signedIn), modifier = Modifier.fillMaxWidth(),
                onClick = { onSend(BugReportRequest(text.trim().take(limit), screen, context, journalTail(logs), kind)); onDismiss() }) { Text(ui("bug.send")) }
            // Everyone's suggestions, to read and vote on, and one's own reports with how they stand.
            if (s.account.signedIn) ForgeOutlinedButton(onClick = { onDismiss(); onSuggestions() }, modifier = Modifier.fillMaxWidth()) { Text(ui("feedback.open")) }
            if (kind == FeedbackKind.SUGGESTION) return@Column
            Engraved(ui("bug.context"))
            context.forEach { (key, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(key, color = Muted, style = MaterialTheme.typography.labelSmall)
                    Text(value, color = Parchment, style = MaterialTheme.typography.labelSmall)
                }
            }
            MutedText(ui("bug.journal", journalTail(logs).size), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private const val JOURNAL = 20

/** The place, as one line: the phase, the tab or the building, the open sheet of the game. */
private fun bugScreen(s: ForgeState, run: ExpeditionRun?): String = when {
    s.phase != AppPhase.GAME -> s.phase.name
    run != null -> "RUN:${run.zone.code}"
    s.play.atlas != null -> "ATLAS"
    else -> listOfNotNull("TAB:${s.tab}", s.building?.name, s.play.craftsProfession.takeIf { it.isNotBlank() }).joinToString("/")
}

private fun bugContext(s: ForgeState, run: ExpeditionRun?): Map<String, String> = buildMap {
    put("client", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    put("server", "$SERVER_VERSION · API $API_REVISION")
    put("device", "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})")
    put("language", s.lang.name)
    put("phase", s.phase.name)
    put("mode", s.mode.name)
    put("tab", s.tab.toString())
    s.building?.let { put("building", it.name) }
    s.account.profile?.id?.let { put("account", it) }
    s.play.heroId.takeIf { it.isNotBlank() }?.let { put("hero", it) }
    s.hero?.let { put("heroLevel", it.level.toString()); put("heroClass", it.info.heroClass) }
    run?.let { put("zone", it.zone.code); put("zoneLevel", it.zone.level.toString()) }
    s.play.selectedNode.takeIf { it.isNotBlank() }?.let { put("treeNode", it) }
    put("link", if (s.link.offline) "offline" else "online · waiting ${s.link.waiting.size}")
}

private fun journalTail(logs: List<RequestLog>): List<String> = logs.takeLast(JOURNAL).map { log ->
    "${log.method} ${log.path} → ${log.status ?: "—"} (${log.elapsedMs} ms)" + if (log.ok) "" else " · ${log.response.take(200)}"
}
