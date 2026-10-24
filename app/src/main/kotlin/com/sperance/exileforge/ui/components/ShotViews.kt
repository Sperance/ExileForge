package com.sperance.exileforge.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sperance.exileforge.core.feedback.ReportShots
import com.sperance.exileforge.core.feedback.ShotLoad
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.ReportShot
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Quiet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Ширина миниатюры снимка в листе отчёта и в тикете. */
internal val SHEET_THUMB = 56.dp
internal val TICKET_THUMB = 72.dp

/** Пропорции экрана устройства (ширина к высоте): у миниатюры, пока кадр не пришёл. */
@Composable internal fun screenAspect(): Float {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) { configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.coerceAtLeast(1) }
}

/**
 * Миниатюра снимка экрана шириной [width] в пропорциях [aspect]: кадр [image] (null - ещё не готов, пустая плитка), [dim] - погашена (снимок
 * не приложен); значок лупы в углу. Касание - [onOpen] (null - не касается); для чтеца - кнопка «Снимок экрана, открыть на весь экран».
 */
@Composable internal fun ShotThumb(image: ImageBitmap?, aspect: Float, width: Dp, dim: Boolean, onOpen: (() -> Unit)?) {
    val shape = RoundedCornerShape(8.dp)
    val description = ui("shot.open")
    Box(
        Modifier.size(width, width / aspect).clip(shape).background(Panel).border(1.dp, Bronze, shape)
            .then(onOpen?.let { Modifier.clickable(role = Role.Button, onClick = it).semantics { contentDescription = description } } ?: Modifier),
    ) {
        image?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = if (dim) DIM_ALPHA else 1f, colorFilter = if (dim) GRAYSCALE else null) }
        if (image != null && onOpen != null) {
            Box(Modifier.align(Alignment.BottomEnd).padding(3.dp).size(22.dp).clip(RoundedCornerShape(6.dp)).background(Ink.copy(alpha = .8f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ZoomIn, null, tint = Parchment, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** Плитка на месте миниатюры без кадра: значок [icon] и слова [text]; касание - [onClick] (null - не касается). */
@Composable private fun ShotPlate(text: String, aspect: Float, width: Dp, icon: ImageVector, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier.size(width, width / aspect).clip(shape).background(Panel).border(1.dp, Quiet.copy(alpha = .45f), shape)
            .then(onClick?.let { Modifier.clickable(role = Role.Button, onClick = it) } ?: Modifier).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = Quiet, modifier = Modifier.size(18.dp))
        Text(text, color = Quiet, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

/**
 * Снимок экрана на весь экран: тёмный слой поверх всего, сверху - закрыть и [title], кадр [image] целиком с приближением щипком и сдвигом,
 * снизу - [caption] (у хранимого снимка - когда он удалится; null - строки нет).
 */
@Composable internal fun ShotViewer(image: ImageBitmap, title: String, caption: String?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ChordOverWindow()
        Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(52.dp).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                ForgeIconButton(Icons.Outlined.Close, ui("shot.close"), onClick = onDismiss, accent = Parchment)
                Text(title, color = Parchment, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
            ZoomableShot(image, Modifier.weight(1f).fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp))
            caption?.let {
                Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, tint = Quiet, modifier = Modifier.size(14.dp))
                    Text(it, color = Quiet, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

/** Кадр [image] целиком, приближение щипком от 1× до [MAX_ZOOM]× и сдвиг приближенного: состояние живёт, пока открыт просмотр. */
@Composable private fun ZoomableShot(image: ImageBitmap, modifier: Modifier) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier.clip(RoundedCornerShape(6.dp)).pointerInput(Unit) {
            detectTransformGestures { _, move, gesture, _ ->
                zoom = (zoom * gesture).coerceIn(1f, MAX_ZOOM)
                pan = if (zoom == 1f) Offset.Zero else pan + move
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            image,
            null,
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = zoom
                scaleY = zoom
                translationX = pan.x
                translationY = pan.y
            },
            contentScale = ContentScale.Fit,
        )
    }
}

/** Снимок раскрытого тикета - на каком он шаге. */
private sealed interface TicketShot {
    data object Loading : TicketShot

    class Shown(val image: ImageBitmap) : TicketShot

    data object Gone : TicketShot

    data object Failed : TicketShot
}

/**
 * Снимок экрана справа от текста раскрытого тикета (у автора и модерации): кадр [shot] отчёта, созданного [createdAt], читается [shots] с
 * сервера, пока тикет раскрыт; касание - на весь экран с подписью «Хранится до …». Срок вышел (по часам сервера) или сервера снимка уже нет -
 * плитка «Снимок удалён через N дней» (`feedback.shotDays` контента); не загрузился - плитка с повтором касанием. Снимка не было - пусто.
 */
@Composable internal fun ReportShotAside(shot: ReportShot, createdAt: String, shots: ReportShots) {
    if (!shot.attached) return
    val shotDays = LocalContentIndex.current?.rules?.feedback?.shotDays ?: 0
    val now = rememberServerNow(MINUTE_MS)
    val aspect = screenAspect()
    var attempt by remember(shot.reportId) { mutableIntStateOf(0) }
    val kept = shot.kept(now)
    val state by produceState<TicketShot>(TicketShot.Loading, shot.reportId, attempt, kept) {
        value = if (!kept) {
            TicketShot.Gone
        } else {
            when (val load = shots.load(shot.reportId)) {
                is ShotLoad.Ready -> withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(load.image, 0, load.image.size)?.asImageBitmap() }?.let(TicketShot::Shown) ?: TicketShot.Failed
                ShotLoad.Gone -> TicketShot.Gone
                ShotLoad.Failed -> TicketShot.Failed
            }
        }
    }
    var open by remember(shot.reportId) { mutableStateOf(false) }
    when (val current = state) {
        TicketShot.Loading -> ShotThumb(null, aspect, TICKET_THUMB, dim = false, onOpen = null)
        TicketShot.Gone -> ShotPlate(ui(pluralKey("shot.gone", shotDays), shotDays), aspect, TICKET_THUMB, Icons.Outlined.HideImage)
        TicketShot.Failed -> ShotPlate(ui("shot.failed"), aspect, TICKET_THUMB, Icons.Outlined.Refresh) { attempt++ }
        is TicketShot.Shown -> {
            val ratio = current.image.width.toFloat() / current.image.height
            ShotThumb(current.image, ratio, TICKET_THUMB, dim = false) { open = true }
            if (open) ShotViewer(current.image, ui("shot.title", reportDate(createdAt)), ui("shot.kept_until", shotDay(shot.until))) { open = false }
        }
    }
}

/** День, до которого хранится снимок, - «31.10» по часам устройства. */
private fun shotDay(until: Long): String = Instant.ofEpochMilli(until).atZone(ZoneId.systemDefault()).format(DAY_MONTH)

private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")

/** Приближение просмотра снимка самое большее. */
private const val MAX_ZOOM = 5f

/** Такт проверки срока снимка: минута. */
private const val MINUTE_MS = 60_000L

/** Погашенная миниатюра неприложенного снимка: серая и приглушённая. */
private val GRAYSCALE = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
private const val DIM_ALPHA = .55f
