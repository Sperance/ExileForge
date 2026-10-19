package com.sperance.exileforge.ui.components

import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sperance.exileforge.core.app.AppPhase
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.RunPhase
import com.sperance.exileforge.core.campaign.run.reportContext
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.ReportTopic
import com.sperance.exileforge.data.settings.DraftStore
import com.sperance.exileforge.platform.VolumeChord
import com.sperance.exileforge.ui.nav.Route
import com.sperance.exileforge.ui.nav.reportPlace
import com.sperance.exileforge.ui.theme.*

/** Аккорд громкости приложения: его держит Activity; окна диалогов и попапов цепляются к нему сами ([ChordOverWindow]). */
val LocalVolumeChord = staticCompositionLocalOf<VolumeChord?> { null }

/**
 * Аккорд громкости поверх своего окна: композиция внутри `ForgeSheet`, `Dialog` или `AlertDialog` оборачивает callback окна
 * диалога, внутри всплывающего меню или попапа ([ForgeMenu], фокусный `Popup`) - слушает необработанные клавиши окна попапа
 * ([VolumeChord.attach]); Громкость↑+↓ открывает отчёт и поверх них. В окне Activity ничего не делает - аккорд там её.
 */
@Composable fun ChordOverWindow() {
    val chord = LocalVolumeChord.current ?: return
    val view = LocalView.current
    DisposableEffect(chord, view) {
        val dialog = generateSequence(view.parent) { it.parent }.filterIsInstance<DialogWindowProvider>().firstOrNull()?.window
        val popup = (view.rootView.layoutParams as? WindowManager.LayoutParams)?.type in WindowManager.LayoutParams.FIRST_SUB_WINDOW..WindowManager.LayoutParams.LAST_SUB_WINDOW
        val detach = when {
            dialog != null -> chord.attach(dialog)
            popup -> chord.attach(view)
            else -> ({})
        }
        onDispose { detach() }
    }
}

/**
 * The report sheet: where the player is, a field for the words, and the send. Nothing else goes with it —
 * no context of the device and the hero, no tail of the request journal; снимков экрана нет. Под формой -
 * «Мои отчёты» и «Известные» ([onReports]); подсказки «Похожее уже известно» во время набора нет.
 *
 * Только отчёт об ошибке; черновик возвращается с устройства и пишется по мере набора. У администратора сверху - таблетки темы
 * «Ошибка | Улучшение» ([ReportTopic]), черновик у тем общий.
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
    var topic by remember { mutableStateOf(ReportTopic.ERROR) }
    LaunchedEffect(Unit) {
        if (text.isEmpty()) text = drafts.read().texts[kind].orEmpty()
    }
    val clipboard = LocalClipboardManager.current
    val limit = game.inputs.report
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(spacing = 8.dp),
        footer = {
            ForgeButton(
                enabled = text.isNotBlank(),
                modifier = Modifier.weight(1f),
                onClick = {
                    onSend(BugReportRequest(text.trim().take(limit), screen, bugContext(route, run), kind, topic = topic))
                    onDismiss()
                },
            ) { Text(ui("bug.send")) }
        },
    ) {
        if (game.isAdmin) PillTabs(ReportTopic.entries.map { ui("bug.topic.${it.name}") }, ReportTopic.entries.indexOf(topic), { topic = ReportTopic.entries[it] }, segmented = true)
        Text(ui("bug.title.${topic.name}"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
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
                ForgeIconButton(Icons.Outlined.ContentCopy, ui("bug.copy"), onClick = { clipboard.setText(AnnotatedString(text)) }, enabled = text.isNotBlank(), tint = Gold)
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
        )
        if (game.session.signedIn) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportsTab.entries.forEach { tab ->
                    ForgeButton(onClick = {
                        onDismiss()
                        onReports(tab)
                    }, modifier = Modifier.weight(1f), kind = ButtonKind.Secondary) { Text(ui(if (tab == ReportsTab.MINE) "feedback.open_mine" else "feedback.open_known")) }
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
