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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.features.UpdateState
import com.sperance.exileforge.presentation.features.UpdateViewModel
import com.sperance.exileforge.update.UpdateInstaller
import com.sperance.exileforge.ui.theme.*

/** The update model of the activity (3.72.0), for the screens that show the version and check by hand. */
val LocalUpdates = staticCompositionLocalOf<UpdateViewModel?> { null }

/**
 * The update gate (3.72.0), over everything: until a check succeeds the game is closed, and a newer build found is
 * required — its window cannot be dismissed. [busy] - a run or a trial is under way: the window waits for its end.
 */
@Composable fun UpdateGate(updates: UpdateViewModel, busy: Boolean) {
    val s by updates.state.collectAsStateWithLifecycle()
    when {
        !s.verified -> Locked { UnverifiedBody(s, updates::retry) }
        s.update != null && !busy -> Locked { UpdateBody(s, updates) }
    }
}

/** A window nothing outside closes: no back, no tap beside it. */
@Composable private fun Locked(content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth().padding(20.dp).background(Panel, RoundedCornerShape(12.dp))
            .border(1.dp, Gold.copy(alpha = .4f), RoundedCornerShape(12.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** The check at start has not succeeded yet: it runs, or it failed and is tried again by hand. */
@Composable private fun ColumnScope.UnverifiedBody(s: UpdateState, onRetry: () -> Unit) {
    Text(ui("update.checking_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
    if (s.checking) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised)
    s.failure?.let { InfoCard(ui("update.check_failed_title"), it, failure = true) }
    if (!s.checking) ForgeButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text(ui("update.retry")) }
    VersionLabel(Modifier.align(Alignment.CenterHorizontally))
}

/** The build to take: from what to what, what is new, and the download into the installer. */
@Composable private fun ColumnScope.UpdateBody(s: UpdateState, updates: UpdateViewModel) {
    val update = s.update ?: return
    val context = LocalContext.current
    Text(ui("update.title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
    Text(ui("update.versions", BuildConfig.VERSION_NAME, update.info.versionName), color = Gold, style = MaterialTheme.typography.bodyMedium)
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
            ForgeButton(onClick = { context.startActivity(UpdateInstaller.permissionScreen(context)); updates.permissionAsked() }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("update.open_settings"))
            }
        }
        else -> {
            s.error?.let { InfoCard(ui("update.failed_title"), it, failure = true) }
            ForgeButton(onClick = updates::install, modifier = Modifier.fillMaxWidth()) { Text(ui(if (s.error != null) "update.retry" else "update.install")) }
            // The way around the installer: the release page, the APK by hand.
            if (s.error != null) ForgeOutlinedButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }, modifier = Modifier.fillMaxWidth()) { Text(ui("update.browser")) }
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
    InfoCard(ui("update.card_title"), ui("update.version", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE) +
        if (s.upToDate) "\n" + ui("update.up_to_date") else s.failure?.let { "\n$it" }.orEmpty())
    ForgeOutlinedButton(enabled = !s.checking, onClick = updates::checkNow, modifier = Modifier.fillMaxWidth()) {
        Text(ui(if (s.checking) "update.checking" else "update.check"))
    }
}
