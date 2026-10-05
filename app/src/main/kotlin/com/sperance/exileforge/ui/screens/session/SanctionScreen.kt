package com.sperance.exileforge.ui.screens.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.SanctionKind
import com.sperance.exileforge.core.network.SanctionView
import com.sperance.exileforge.presentation.admin.NoticeViewModel
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.auction.listedAt
import com.sperance.exileforge.ui.screens.server.RolePill
import com.sperance.exileforge.ui.screens.server.categoryTitle
import com.sperance.exileforge.ui.screens.server.epochOf
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/**
 * Экран санкции (3.88.5, вариант B): кольцо с остатком срока, что закрыто и до когда, кто, когда и за что - логином и ролью, -
 * комментарий модератора, номер санкции; «Обжаловать» один раз и «Сменить аккаунт». [sanctionId] пустой - аккаунт удалён и
 * уже стирается: подробностей нет, остаётся только выйти.
 */
@Composable fun SanctionScreen(sanctionId: String) {
    val vm = koinViewModel<NoticeViewModel>()
    val sanction by vm.sanction.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    LaunchedEffect(sanctionId) { vm.load(sanctionId) }
    var appealing by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val current = sanction
        TermRing(current)
        Text(
            ui(if (current == null) "notice.closed" else "notice.title.${current.kind.name}.${current.target.name}"),
            color = LifeRed,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        current?.let { notice ->
            MutedText(untilLine(notice))
            ForgePanel(Modifier.fillMaxWidth(), accent = Rune) {
                Text(ui("notice.details", notice.number), color = Rune, style = MaterialTheme.typography.labelLarge)
                Detail(ui("notice.by")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(notice.byLogin, color = Parchment, style = MaterialTheme.typography.bodySmall)
                        RolePill(notice.byRole)
                    }
                }
                Detail(ui("notice.when")) { Value(listedAt(notice.at).orEmpty()) }
                Detail(ui("notice.what")) { Value(ui("moderation.target.${notice.target.name}") + " · " + notice.label) }
                Detail(ui("notice.reason")) { Value(categoryTitle(notice.category)) }
            }
            if (notice.comment.isNotBlank()) {
                ForgePanel(Modifier.fillMaxWidth(), accent = Rune) {
                    Text(ui("notice.comment"), color = Rune, style = MaterialTheme.typography.labelLarge)
                    Text("«${notice.comment}»", color = Parchment, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (notice.appealed) {
                MutedText(ui("notice.appealed"))
            } else {
                ForgeButton(enabled = !activity.held, onClick = { appealing = true }, modifier = Modifier.fillMaxWidth()) { Text(ui("notice.appeal")) }
            }
        }
        ForgeOutlinedButton(onClick = vm::dismiss, modifier = Modifier.fillMaxWidth()) { Text(ui("notice.switch")) }
    }
    sanction?.takeIf { appealing }?.let { notice ->
        AppealDialog(notice, onDismiss = { appealing = false }) { text ->
            appealing = false
            vm.appeal(notice.id, text)
        }
    }
}

/** Кольцо срока: доля оставшегося времени и сам остаток; бессрочный бан - полное кольцо со знаком «∞». */
@Composable private fun TermRing(sanction: SanctionView?) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sanction?.id) {
        while (true) {
            delay(MINUTE_MS)
            now = System.currentTimeMillis()
        }
    }
    val from = epochOf(sanction?.at)
    val to = epochOf(sanction?.until)
    val share = if (from == null || to == null || to <= from) 1f else ((to - now).toFloat() / (to - from)).coerceIn(0f, 1f)
    Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Bronze, 0f, 360f, false, style = stroke)
            drawArc(LifeRed, -90f, 360f * share, false, style = stroke)
        }
        Text(
            when {
                sanction == null -> "—"
                to == null -> "∞"
                else -> left(to - now)
            },
            color = GoldBright,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable private fun Detail(label: String, value: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
        value()
    }
}

@Composable private fun Value(text: String) = Text(text, color = Parchment, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)

/**
 * Апелляция (3.88.5): одна на санкцию, текст до `rules.inputs.appeal`. Уходит без входа - по id санкции, - и попадает
 * модераторам в отчёты.
 */
@Composable internal fun AppealDialog(sanction: SanctionView, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    ForgeDialog(ui("notice.appeal_title", sanction.number), onDismiss) {
        MutedText(ui("notice.appeal_note"))
        OutlinedTextField(text, { text = it.take(APPEAL) }, minLines = 3, supportingText = { LengthCounter(text, APPEAL) }, modifier = Modifier.fillMaxWidth())
        ForgeButton(enabled = text.isNotBlank(), onClick = { onSend(text) }, modifier = Modifier.fillMaxWidth()) { Text(ui("notice.appeal_send")) }
    }
}

private fun untilLine(sanction: SanctionView): String {
    val until = sanction.until
    return when {
        sanction.kind == SanctionKind.DELETION -> ui("notice.purge_at", listedAt(until.orEmpty()).orEmpty())
        until == null -> ui("moderation.forever")
        else -> ui("moderation.until", listedAt(until).orEmpty())
    }
}

/** Остаток словами: «6 д 4 ч», «3 ч 12 мин», «12 мин». */
private fun left(ms: Long): String {
    val minutes = (ms.coerceAtLeast(0) / MINUTE_MS)
    val days = minutes / (60 * 24)
    val hours = minutes / 60 % 24
    return when {
        days > 0 -> ui("notice.left_dh", days, hours)
        hours > 0 -> ui("notice.left_hm", hours, minutes % 60)
        else -> ui("notice.left_m", minutes)
    }
}

private const val MINUTE_MS = 60_000L
private const val APPEAL = 1000
