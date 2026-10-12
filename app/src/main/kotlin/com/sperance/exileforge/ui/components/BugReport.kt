package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.data.settings.DraftStore
import com.sperance.exileforge.platform.ScreenCapture
import com.sperance.exileforge.platform.ScreenShot
import com.sperance.exileforge.platform.VolumeChord
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.flow.Flow

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
 * Снимок к листу отчёта (4.8.0): [shot] - кадр экрана до открытия листа, [ready] - снимок снят (или не вышел), лист можно
 * показывать. Аккорд громкости приносит снимок сам ([offer]); жук и меню забега только открывают лист - снимок снимается здесь.
 */
@Stable class BugShot {
    var shot by mutableStateOf<ScreenShot?>(null)
        private set
    var ready by mutableStateOf(false)
        private set
    private var taken = false

    /** Снимок, снятый до просьбы открыть лист. */
    fun offer(shot: ScreenShot?) {
        this.shot = shot
        taken = true
    }

    internal suspend fun open(capture: suspend () -> ScreenShot?) {
        if (!taken) shot = capture()
        ready = true
    }

    internal fun close() {
        shot = null
        taken = false
        ready = false
    }
}

/**
 * Снимок листа отчёта [sheets] (4.8.0): просьба [requests] (аккорд громкости) открывает лист со своим снимком; любое другое
 * открытие ([SystemSheets.bug] - жук, меню забега) снимает окно здесь, дав экрану дорисовать кадр без меню, и лишь затем
 * лист показывается ([BugShot.ready]).
 */
@Composable fun rememberBugShot(sheets: SystemSheets, requests: Flow<ScreenShot?>): BugShot {
    val state = remember { BugShot() }
    val context = LocalContext.current
    LaunchedEffect(requests) {
        requests.collect { shot ->
            if (sheets.bug) return@collect
            state.offer(shot)
            sheets.bug = true
        }
    }
    LaunchedEffect(sheets.bug) {
        if (!sheets.bug) return@LaunchedEffect state.close()
        state.open {
            repeat(SETTLE_FRAMES) { withFrameNanos { } }
            ScreenCapture.capture(context)
        }
    }
    return state
}

/** Кадров, что экран дорисовывает перед снимком: закрывающееся меню забега успевает уйти с экрана. */
private const val SETTLE_FRAMES = 2

/**
 * The report sheet: where the player is, a field for the words, and the send. Since 3.76.0 nothing else goes with it —
 * no context of the device and the hero, no tail of the request journal. Снимок [shot] (4.8.0) - превью и отметка «Приложить
 * снимок» у вида, что его берёт ([FeedbackKind.shots]); [onSend] получает снимок, только если он отмечен.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugSheet(
    game: GameUi,
    route: Route,
    run: ExpeditionRun?,
    drafts: DraftStore,
    onDismiss: () -> Unit,
    onSuggestions: () -> Unit,
    shot: ScreenShot? = null,
    onSend: (BugReportRequest, ScreenShot?) -> Unit,
) {
    val screen = remember { bugScreen(route, run) }
    // A bug or a suggestion (3.73.0): the player picks; a suggestion goes into the public list, so it needs an account.
    // The words of each and the kind last open come back from the device (3.75.0) and are kept as they are typed.
    var kind by remember { mutableStateOf(FeedbackKind.BUG) }
    val texts = remember { mutableStateMapOf<FeedbackKind, String>() }
    var attach by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val draft = drafts.read()
        if (texts.isEmpty()) kind = draft.kind
        draft.texts.forEach { (k, v) -> if (k !in texts) texts[k] = v }
    }
    val text = texts[kind].orEmpty()
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
            if (shot != null && kind.shots) ShotChoice(shot, attach) { attach = it }
            ForgeButton(
                enabled = text.isNotBlank() && (kind == FeedbackKind.BUG || game.session.signedIn),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onSend(BugReportRequest(text.trim().take(limit), screen, emptyMap(), kind), shot?.takeIf { attach && kind.shots })
                    onDismiss()
                },
            ) { Text(ui("bug.send")) }
            // Everyone's suggestions, to read and vote on, and one's own reports with how they stand.
            if (game.session.signedIn) {
                ForgeOutlinedButton(onClick = {
                    onDismiss()
                    onSuggestions()
                }, modifier = Modifier.fillMaxWidth()) { Text(ui("feedback.open")) }
            }
        }
    }
}

/** Превью снимка и отметка «Приложить снимок» (4.8.0). */
@Composable private fun ShotChoice(shot: ScreenShot, attach: Boolean, onAttach: (Boolean) -> Unit) {
    val image = remember(shot) { shot.bitmap.asImageBitmap() }
    Row(
        Modifier.fillMaxWidth().toggleable(attach, role = Role.Checkbox, onValueChange = onAttach),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            image,
            ui("bug.shot"),
            Modifier.height(SHOT_PREVIEW).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit,
            alpha = if (attach) 1f else .4f,
        )
        Checkbox(attach, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Gold, uncheckedColor = Muted))
        Text(ui("bug.attach_shot"), color = if (attach) Parchment else Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** Высота превью снимка в листе отчёта. */
private val SHOT_PREVIEW = 140.dp

/** The place, as one line: the phase, the tab or the building, the open sheet of the game. */
private fun bugScreen(route: Route, run: ExpeditionRun?): String = when {
    route.phase != AppPhase.GAME -> route.phase.name
    run != null -> "RUN:${run.zone.code}"
    route == Route.Atlas -> "ATLAS"
    else -> listOfNotNull("TAB:${route.tab}", route.building?.name).joinToString("/")
}
