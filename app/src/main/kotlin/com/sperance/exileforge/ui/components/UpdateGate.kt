package com.sperance.exileforge.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.app.StartStage
import com.sperance.exileforge.presentation.app.StartStep
import com.sperance.exileforge.presentation.app.StepState
import com.sperance.exileforge.presentation.features.UpdateState
import com.sperance.exileforge.presentation.features.UpdateViewModel
import com.sperance.exileforge.ui.theme.*
import com.sperance.exileforge.update.UpdateInstaller
import kotlinx.coroutines.delay

/** The update model of the activity (3.72.0), for the screens that show the version and check by hand. */
val LocalUpdates = staticCompositionLocalOf<UpdateViewModel?> { null }

/**
 * Where the start stands (3.81.0): the server's content read or on its way, the dictionary and the icons in. 3.82.0:
 * [sessionBusy] - a command (the sign-in, the session's check) is under way, [startupDone] - the start's own steps are over.
 */
data class StartStages(
    val contentLoading: Boolean,
    val contentReady: Boolean,
    val dictionaryReady: Boolean,
    val iconsReady: Boolean,
    val sessionBusy: Boolean = false,
    val startupDone: Boolean = true,
) {
    /** The start no longer holds the window: its steps are over, nothing signs in, no content is on its way. */
    val settled: Boolean get() = startupDone && !sessionBusy && !contentLoading
}

/**
 * The update gate (3.72.0), over everything: a newer build found is required — its window cannot be dismissed. [busy] - a
 * run or a trial is under way: the window waits for its end. Since 3.73.0 the check itself is unseen, and the first start
 * asks once to allow installing from this game. Since 3.81.0 the start is one window of stages; since 3.82.0 it holds only
 * for the start itself — the sign-in and the content on its way, never the version check — shows under every active stage
 * its steps with their time, so a stall is seen where it is, and after [CONTINUE_AFTER_MS] offers to go on without waiting.
 * С 3.84.0 там же [diagnostics] - журнал запуска со стеками потоков: долгий запуск копируется и уходит разработчику.
 */
@Composable fun UpdateGate(updates: UpdateViewModel, busy: Boolean, stages: StartStages, steps: List<StartStep>, diagnostics: () -> String) {
    val s by updates.state.collectAsStateWithLifecycle()
    // Back in the app (3.76.0): the server is asked again, unless a run is under way.
    val idle by rememberUpdatedState(!busy)
    LifecycleEventEffect(Lifecycle.Event.ON_START) { if (idle) updates.resumed() }
    // The window is the start's, once: a content read later in the game is the banner's.
    var released by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(stages.settled) { if (stages.settled) released = true }
    var waited by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(CONTINUE_AFTER_MS)
        waited = true
    }
    when {
        s.update != null && !busy -> Locked {
            StageList(s, stages, steps)
            UpdateBody(s, updates)
        }

        !released -> Locked {
            Text(ui("start.title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            StageList(s, stages, steps)
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised)
            if (waited) {
                MutedText(ui("start.slow"))
                ForgeOutlinedButton(onClick = { released = true }, modifier = Modifier.fillMaxWidth()) { Text(ui("start.continue")) }
                CopyLog { diagnostics() + "\n--- window\n$stages settled=${stages.settled} released=$released update=${s.update != null} busy=$busy" }
            }
        }

        s.askSources -> SourcesPrompt(updates)
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

/** How long the start's window waits before it offers to go on without the rest. */
private const val CONTINUE_AFTER_MS = 15_000L

/** A stage's state: waiting, under way, done, or failed. */
private enum class Stage { WAIT, RUN, DONE, FAIL }

/** A stage by its own steps: one running - under way, the last one failed - failed, any - done. */
private fun List<StartStep>.state(): Stage? = when {
    isEmpty() -> null
    any { it.state == StepState.RUN } -> Stage.RUN
    last().state == StepState.FAIL -> Stage.FAIL
    else -> Stage.DONE
}

@Composable private fun StageList(s: UpdateState, stages: StartStages, steps: List<StartStep>) {
    val of = steps.groupBy { it.stage }
    fun traced(stage: StartStage) = of[stage].orEmpty()
    val rows = StartStage.entries.associateWith { stage ->
        val own = traced(stage).state()
        when (stage) {
            StartStage.VERSION -> when {
                s.update != null -> Stage.DONE
                s.checking -> Stage.RUN
                s.failure != null -> Stage.FAIL
                else -> Stage.DONE
            }

            StartStage.SESSION -> when {
                stages.sessionBusy || own == Stage.RUN || !stages.startupDone -> Stage.RUN
                own == Stage.FAIL -> Stage.FAIL
                else -> Stage.DONE
            }

            StartStage.CONTENT -> when {
                stages.contentLoading || own == Stage.RUN -> Stage.RUN
                stages.contentReady -> Stage.DONE
                own == Stage.FAIL -> Stage.FAIL
                else -> Stage.WAIT
            }

            StartStage.DICTIONARY -> if (stages.dictionaryReady && own != Stage.RUN) Stage.DONE else own ?: Stage.RUN

            StartStage.ICONS -> if (stages.iconsReady && own != Stage.RUN) Stage.DONE else own ?: Stage.RUN
        }
    }
    // The clock of the running steps: their seconds tick while the window stands.
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(250)
            value = System.currentTimeMillis()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { (stage, state) ->
            val note = if (stage == StartStage.VERSION) s.update?.let { ui("update.versions", BuildConfig.VERSION_NAME, it.info.versionName) } else null
            StageRow(ui(stage.title), state, note)
            // The steps of an active stage (3.82.0): where it stands now, and how long each took.
            if (state == Stage.RUN || state == Stage.FAIL) traced(stage).takeLast(STEPS_SHOWN).forEach { StepRow(it, now) }
        }
    }
}

/** Steps shown under an active stage: the latest. */
private const val STEPS_SHOWN = 5

@Composable private fun StepRow(step: StartStep, now: Long) {
    val color = when (step.state) {
        StepState.RUN -> Parchment
        StepState.DONE -> Muted
        StepState.FAIL -> LifeRed
    }
    Row(Modifier.padding(start = 26.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            when (step.state) {
                StepState.RUN -> "…"
                StepState.DONE -> "✓"
                StepState.FAIL -> "✕"
            },
            color = color,
            style = MaterialTheme.typography.labelMedium,
        )
        Column(Modifier.weight(1f)) {
            Text(ui(step.key, *step.args.toTypedArray()), color = color, style = MaterialTheme.typography.labelMedium)
            step.error?.let { Text(it, color = LifeRed, style = MaterialTheme.typography.labelSmall) }
        }
        Text(ui("start.seconds", "%.1f".format(step.millis(now) / 1000.0)), color = color, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun StageRow(title: String, stage: Stage, note: String? = null) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when (stage) {
            Stage.RUN -> CircularProgressIndicator(Modifier.size(16.dp), color = Gold, strokeWidth = 2.dp)

            else -> Text(
                when (stage) {
                    Stage.DONE -> "✓"
                    Stage.FAIL -> "✕"
                    else -> "•"
                },
                color = when (stage) {
                    Stage.DONE -> Vital
                    Stage.FAIL -> LifeRed
                    else -> Muted
                },
                modifier = Modifier.width(16.dp),
            )
        }
        Text(title, color = if (stage == Stage.WAIT) Muted else Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        note?.let { Text(it, color = Gold, style = MaterialTheme.typography.labelMedium) }
    }
}

/** The first start (3.73.0): why the game wants «install unknown apps», the way to the setting, and «later». */
@Composable private fun SourcesPrompt(updates: UpdateViewModel) {
    val context = LocalContext.current
    Dialog(onDismissRequest = updates::sourcesAsked) {
        Column(
            Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).border(1.dp, Gold.copy(alpha = .4f), RoundedCornerShape(12.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(ui("update.sources_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            MutedText(ui("update.sources_note"))
            ForgeButton(onClick = {
                context.startActivity(UpdateInstaller.permissionScreen(context))
                updates.sourcesAsked()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("update.open_settings"))
            }
            ForgeOutlinedButton(onClick = updates::sourcesAsked, modifier = Modifier.fillMaxWidth()) { Text(ui("update.later")) }
        }
    }
}

/** A window nothing outside closes: no back, no tap beside it. */
@Composable private fun Locked(content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp).background(Panel, RoundedCornerShape(12.dp))
                .border(1.dp, Gold.copy(alpha = .4f), RoundedCornerShape(12.dp)).padding(20.dp),
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
        // Заметки только выложенной сборки (3.82.0): сервер хранит одну.
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
            // The way around the installer: the release page, the APK by hand.
            if (s.error != null) {
                ForgeOutlinedButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.apkUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
