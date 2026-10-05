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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.features.UpdateState
import com.sperance.exileforge.presentation.features.UpdateViewModel
import com.sperance.exileforge.ui.theme.*
import com.sperance.exileforge.update.UpdateInstaller

/** The update model of the activity (3.72.0), for the screens that show the version and check by hand. */
val LocalUpdates = staticCompositionLocalOf<UpdateViewModel?> { null }

/** Where the start stands (3.81.0): the server's content read or on its way, the dictionary and the icons in. */
data class StartStages(val contentLoading: Boolean, val contentReady: Boolean, val dictionaryReady: Boolean, val iconsReady: Boolean)

/**
 * The update gate (3.72.0), over everything: a newer build found is required — its window cannot be dismissed. [busy] - a
 * run or a trial is under way: the window waits for its end. Since 3.73.0 the check itself is unseen, and the first start
 * asks once to allow installing from this game, so the update later goes in without a detour. Since 3.81.0 the start is one
 * window of stages — the version, the content, the dictionary and the icons —, held until the version is checked and the
 * content that is on its way has come; a newer build turns the same window into its update, with «Обновить» there.
 */
@Composable fun UpdateGate(updates: UpdateViewModel, busy: Boolean, stages: StartStages) {
    val s by updates.state.collectAsStateWithLifecycle()
    // Back in the app (3.76.0): the releases are asked again, unless a run is under way.
    val idle by rememberUpdatedState(!busy)
    LifecycleEventEffect(Lifecycle.Event.ON_START) { if (idle) updates.resumed() }
    // The stages are shown once, at the start: a content read later in the game is the banner's.
    var started by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(s.opened, stages.contentLoading) { if (s.opened && !stages.contentLoading) started = true }
    when {
        s.update != null && !busy -> Locked {
            StageList(s, stages)
            UpdateBody(s, updates)
        }

        // The start waits for one check to pass (3.76.0) and, since 3.81.0, for the content on its way.
        // 3.81.1: no longer than the first check's grace when GitHub does not answer.
        !s.opened || (!started && stages.contentLoading) -> Locked { CheckingBody(s, stages, updates) }

        s.askSources -> SourcesPrompt(updates)
    }
}

/** The start's window (3.81.0): the stages one under another, and why the check failed with «Повторить». */
@Composable private fun ColumnScope.CheckingBody(s: UpdateState, stages: StartStages, updates: UpdateViewModel) {
    Text(ui("start.title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
    StageList(s, stages)
    if (!s.checking && s.failure != null) {
        InfoCard(ui("update.gate_failed"), s.failure, failure = true)
        MutedText(ui("update.gate_auto"))
        ForgeButton(onClick = updates::retry, modifier = Modifier.fillMaxWidth()) { Text(ui("update.retry")) }
    } else {
        LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised)
    }
}

/** A stage's state: waiting, under way, done, or failed. */
private enum class Stage { WAIT, RUN, DONE, FAIL }

@Composable private fun StageList(s: UpdateState, stages: StartStages) {
    val version = when {
        s.update != null -> Stage.DONE
        s.verified -> Stage.DONE
        !s.checking && s.failure != null -> Stage.FAIL
        else -> Stage.RUN
    }
    val content = when {
        stages.contentReady && !stages.contentLoading -> Stage.DONE
        stages.contentLoading -> Stage.RUN
        else -> Stage.WAIT
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StageRow(ui("start.version"), version, if (s.update != null) ui("update.versions", BuildConfig.VERSION_NAME, s.update.info.versionName) else null)
        StageRow(ui("start.content"), content)
        StageRow(ui("start.dictionary"), if (stages.dictionaryReady) Stage.DONE else Stage.RUN)
        StageRow(ui("start.icons"), if (stages.iconsReady) Stage.DONE else Stage.RUN)
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
        update.notes.forEach { notes ->
            Text(notes.version, color = GoldBright, style = MaterialTheme.typography.labelLarge)
            if (notes.text.isNotBlank()) MutedText(notes.text)
        }
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
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
