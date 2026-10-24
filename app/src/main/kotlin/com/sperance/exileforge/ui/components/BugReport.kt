package com.sperance.exileforge.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
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
import com.sperance.exileforge.platform.BugShot
import com.sperance.exileforge.platform.BugShots
import com.sperance.exileforge.platform.VolumeChord
import com.sperance.exileforge.ui.nav.Route
import com.sperance.exileforge.ui.nav.reportPlace
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
        val dialog = view.dialogWindow()
        val popup = (view.rootView.layoutParams as? WindowManager.LayoutParams)?.type in WindowManager.LayoutParams.FIRST_SUB_WINDOW..WindowManager.LayoutParams.LAST_SUB_WINDOW
        val detach = when {
            dialog != null -> chord.attach(dialog)
            popup -> chord.attach(view)
            else -> ({})
        }
        onDispose { detach() }
    }
}

/** Окно диалога, в котором стоит этот вид; null - вид в окне Activity или во всплывающем окне. */
private fun View.dialogWindow(): Window? = generateSequence(parent) { it.parent }.filterIsInstance<DialogWindowProvider>().firstOrNull()?.window

/** Окно Activity, к которой ведёт контекст; null - Activity нет. */
private tailrec fun Context.activityWindow(): Window? = when (this) {
    is Activity -> window
    is ContextWrapper -> baseContext.activityWindow()
    else -> null
}

/**
 * Открыть лист отчёта из композиции (строка настроек «Сообщить об ошибке»): сперва снимок окна, где она стоит, - окна Activity и окна
 * диалога поверх, если она в нём ([BugShots.snap]), затем сам лист в [sheets]: в кадре то, о чём жалоба, а не лист.
 */
@Composable fun rememberBugOpener(sheets: SystemSheets): () -> Unit {
    val shots = koinInject<BugShots>()
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    return remember(sheets, shots, view, scope) {
        {
            scope.launch {
                val dialog = view.dialogWindow()
                (view.context.activityWindow() ?: dialog)?.let { shots.snap(it, dialog.takeIf { top -> top !== it }) }
                sheets.bug = true
            }
        }
    }
}

/**
 * The report sheet: where the player is, a field for the words, and the send. Nothing else goes with it —
 * no context of the device and the hero, no tail of the request journal. Снимок экрана [shot] снят до листа: строка «Приложить снимок
 * экрана» (по умолчанию снята) с миниатюрой справа и сноской «Снимок видит только модерация» ([ShotChoice]); снять не вышло - плашка, что
 * отчёт уйдёт без него. Под формой - «Мои отчёты» ([onReports]); подсказки «Похожее уже известно» во время набора нет.
 *
 * Только отчёт об ошибке; черновик возвращается с устройства и пишется по мере набора. У администратора сверху - таблетки темы
 * «Ошибка | Улучшение» ([ReportTopic]), черновик у тем общий. Вошедший игрок может отметить «Добавить логи сервера» ([ServerLogConsent]);
 * отметка не запоминается - у каждого отчёта она снята.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugSheet(
    game: GameUi,
    route: Route,
    run: ExpeditionRun?,
    drafts: DraftStore,
    onDismiss: () -> Unit,
    onReports: () -> Unit,
    shot: BugShot,
    onSend: (BugReportRequest, ByteArray?) -> Unit,
) {
    val screen = remember { bugScreen(route, run) }
    val kind = FeedbackKind.BUG
    var text by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf(ReportTopic.ERROR) }
    var serverLog by remember { mutableStateOf(false) }
    var attachShot by remember { mutableStateOf(false) }
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
                state = ButtonState.of(text.isNotBlank()),
                modifier = Modifier.weight(1f),
                onClick = {
                    val image = (shot as? BugShot.Ready)?.image?.takeIf { attachShot }
                    onSend(BugReportRequest(text.trim().take(limit), screen, bugContext(route, run), kind, topic = topic, serverLog = serverLog), image)
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
                ForgeIconButton(Icons.Outlined.ContentCopy, ui("bug.copy"), onClick = { clipboard.setText(AnnotatedString(text)) }, state = ButtonState.of(text.isNotBlank()), accent = Gold)
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
        )
        ShotChoice(shot, attachShot) { attachShot = it }
        if (game.session.signedIn) {
            ServerLogConsent(serverLog) { serverLog = it }
            ForgeButton(onClick = {
                onDismiss()
                onReports()
            }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) { Text(ui("feedback.open_mine")) }
        }
    }
}

/**
 * Снимок экрана в листе: пока он сжимается или готов - строка «Приложить снимок экрана» ([attach], касание всей строки переключает, пока
 * снимок не готов - погашена) с миниатюрой в пропорциях кадра справа (не приложен - серая; касание - на весь экран) и сноской «Снимок видит
 * только модерация»; снять не вышло - плашка «отчёт уйдёт без него»; снимка нет ([BugShot.Absent]) - ничего.
 */
@Composable private fun ShotChoice(shot: BugShot, attach: Boolean, onAttach: (Boolean) -> Unit) {
    if (shot == BugShot.Absent) return
    if (shot == BugShot.Failed) {
        NoShotPlate()
        return
    }
    val ready = shot as? BugShot.Ready
    val preview = remember(ready) { ready?.preview?.asImageBitmap() }
    var viewing by remember { mutableStateOf(false) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.weight(1f).heightIn(min = 44.dp).toggleable(attach, enabled = ready != null, role = Role.Checkbox, onValueChange = onAttach),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Checkbox(attach, onCheckedChange = null, enabled = ready != null, colors = CheckboxDefaults.colors(checkedColor = Gold, uncheckedColor = Muted, checkmarkColor = Ink))
                Text(ui("bug.shot.attach"), color = Parchment, style = MaterialTheme.typography.bodyMedium)
            }
            ShotThumb(preview, ready?.aspect ?: screenAspect(), SHEET_THUMB, dim = !attach, onOpen = preview?.let { { viewing = true } })
        }
        Text(ui("bug.shot.private"), color = Quiet, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 48.dp))
    }
    if (viewing && preview != null) ShotViewer(preview, ui("shot.title_new"), null) { viewing = false }
}

/** Плашка «Снимок экрана не получился — отчёт уйдёт без него.» на месте строки снимка. */
@Composable private fun NoShotPlate() {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().background(Quiet.copy(alpha = .08f), shape).border(1.dp, Quiet.copy(alpha = .4f), shape).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.HideImage, null, tint = Quiet, modifier = Modifier.size(18.dp))
        Text(ui("bug.shot.failed"), color = Quiet, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Галочка согласия на лог сервера в отчёте ([BugReportRequest.serverLog]): касание всей строки переключает её, ⓘ рядом объясняет,
 * что уйдёт.
 */
@Composable private fun ServerLogConsent(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).toggleable(checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Gold, uncheckedColor = Muted, checkmarkColor = Ink))
        Text(ui("bug.server_log"), color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false))
        InfoMark(ui("bug.server_log"), tip = { Tip(body = ui("bug.server_log_tip"), tint = Muted) })
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
