package com.sperance.exileforge.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.update.AvailableUpdate
import com.sperance.exileforge.core.update.UpdateModel
import com.sperance.exileforge.core.update.UpdateState
import com.sperance.exileforge.ui.theme.*
import com.sperance.exileforge.update.UpdateInstaller

/**
 * Хозяин обязательного обновления - своё окно поверх игры, не состояние заставки запуска: найдена сборка ([UpdateState.mandatory]) -
 * диалог по центру ([UpdateDialog]), игра под ним видна, но не нажимается; закрыть его нельзя ни «назад», ни касанием мимо. [busy] - идёт
 * поход, испытание или Разлом: пока бой, о сборке говорит плашка сверху ([UpdateHint]), диалог - по его концу.
 */
@Composable fun UpdateHost(updates: UpdateModel, busy: Boolean) {
    val s by updates.state.collectAsState()
    val update = s.update ?: return
    if (busy) {
        UpdateHint(update.info.versionName)
        return
    }
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        ChordOverWindow()
        Box(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
            UpdateDialog(update, s, onInstall = updates::install, onPermissionAsked = updates::permissionAsked)
        }
    }
}

/**
 * Окно обязательного обновления: печать, «Нужно обновление», версии «было → станет» чипами, «Что нового» с прокруткой внутри, внизу -
 * «Обновить» и размер; пока качается - тонкая полоса и «64% · 24 из 38 МБ» мелко; установка, разрешение на неё и ошибка загрузки - в этом
 * же окне. Не влезает в экран (крупный шрифт) - прокручивается целиком. [onInstall] - «Обновить» и «Повторить»; [onPermissionAsked] -
 * игрок ушёл в настройки за разрешением.
 *
 * Обход установщика: страница релиза на GitHub, APK руками.
 */
@Composable internal fun UpdateDialog(update: AvailableUpdate, s: UpdateState, onInstall: () -> Unit, onPermissionAsked: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.widthIn(max = DIALOG_WIDTH).fillMaxWidth().background(PanelRaised, DialogShape).border(1.dp, Gold.copy(alpha = .45f), DialogShape)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(56.dp).background(Abyss, CircleShape).border(1.dp, Gold.copy(alpha = .7f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Upgrade, null, tint = Gold, modifier = Modifier.size(28.dp))
        }
        Text(ui("update.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif, textAlign = TextAlign.Center)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VersionChip(BuildConfig.VERSION_NAME, Parchment)
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Muted, modifier = Modifier.size(16.dp))
            VersionChip(update.info.versionName, Vital)
        }
        if (update.info.notes.isNotBlank()) Notes(update.info.notes)
        val progress = s.progress
        when {
            progress != null -> {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(BAR), color = Gold, trackColor = Abyss)
                Caption(downloaded(progress, update.info.size))
                Caption(ui("update.confirm_next"))
            }

            s.installing -> {
                LinearProgressIndicator(Modifier.fillMaxWidth().height(BAR), color = Gold, trackColor = Abyss)
                Caption(ui("update.installing"))
            }

            s.needsPermission -> {
                MutedText(ui("update.permission_note"), style = MaterialTheme.typography.bodySmall)
                ForgeButton(onClick = {
                    context.startActivity(UpdateInstaller.permissionScreen(context))
                    onPermissionAsked()
                }, modifier = Modifier.fillMaxWidth()) { Text(ui("update.open_settings")) }
            }

            else -> {
                s.error?.let { Text(it, color = LifeRed, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }
                ForgeButton(onClick = onInstall, modifier = Modifier.fillMaxWidth()) { Text(ui(if (s.error != null) "update.retry" else "update.install")) }
                if (s.error != null) {
                    ForgeOutlinedButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.pageUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }, modifier = Modifier.fillMaxWidth()) { Text(ui("update.browser")) }
                }
                if (update.info.size > 0) Caption(ui("update.size", megabytes(update.info.size)))
            }
        }
    }
}

/** Версия чипом: номер цвета [tint] в тонкой рамке. */
@Composable private fun VersionChip(version: String, tint: Color) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        version,
        color = tint,
        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.background(Abyss, shape).border(1.dp, tint.copy(alpha = .45f), shape).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** «Что нового» сборки: заголовок и текст заметок, длинные - прокруткой внутри рамки, окно не растёт выше [NOTES_HEIGHT]. */
@Composable private fun Notes(notes: String) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().heightIn(max = NOTES_HEIGHT).background(Abyss, shape).border(1.dp, Bronze, shape).verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(ui("update.notes"), color = Gold, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Serif)
        Text(notes, color = Parchment, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun Caption(text: String) = Text(text, color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)

/** Скачано: «64% · 24 из 38 МБ»; размер сборки неизвестен - только процент. */
private fun downloaded(progress: Float, size: Long): String {
    val percent = (progress * 100).toInt()
    return if (size > 0) ui("update.progress", percent, megabytes((size * progress).toLong()), megabytes(size)) else ui("update.downloading", percent)
}

/** Мегабайты сборки одной цифрой после точки: «38.4». */
private fun megabytes(bytes: Long): String = "%.1f".format(bytes / BYTES_IN_MB)

private const val BYTES_IN_MB = 1_048_576.0

/** Окно обновления не шире этого и на планшете. */
private val DIALOG_WIDTH = 420.dp

/** Наибольшая высота заметок «Что нового»: дальше они прокручиваются внутри. */
private val NOTES_HEIGHT = 220.dp

/** Толщина полосы загрузки: тонкая, не во всю высоту кнопки. */
private val BAR = 5.dp

/** Плашка сверху над боем: найдено обновление, оно встанет по концу похода или испытания; касания идут мимо неё. */
@Composable private fun UpdateHint(version: String) {
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 56.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            ui("update.after_fight", version),
            color = GoldBright,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.background(PanelRaised.copy(alpha = .92f), MaterialTheme.shapes.small).padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
