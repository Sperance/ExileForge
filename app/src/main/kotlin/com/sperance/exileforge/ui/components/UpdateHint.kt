package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.update.AvailableUpdate
import com.sperance.exileforge.core.update.notesHeadline
import com.sperance.exileforge.ui.nav.Navigator
import com.sperance.exileforge.ui.nav.Route
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Quiet
import com.sperance.exileforge.ui.theme.depthPanel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import kotlin.math.ceil

/**
 * Плашка жёсткого обновления в бою и её кружок - одно состояние на найденную сборку ([LocalUpdateHint]; хозяин заводит новое на другую
 * сборку): плашка раскрыта ([open]) первые [HINT_MS] после показа или касания кружка, затем свёрнута - кружок у кнопки меню забега
 * ([UpdateMark]). Свёрнутая плашка выходит из композиции, раскрытая снова отсчитывает заново.
 */
@Stable
class UpdateHintState {
    var open by mutableStateOf(true)
        private set

    fun expand() {
        open = true
    }

    fun collapse() {
        open = false
    }
}

/** Состояние плашки жёсткого обновления; хозяин - `ForgeApp`. */
val LocalUpdateHint = staticCompositionLocalOf { UpdateHintState() }

/**
 * Плашка жёсткого обновления у низа экрана боя: «Сервер обновлён», что поход продолжится после обновления игры до версии, размер и
 * отсчёт до сворачивания; «Свернуть» и «Обновить» ([onInstall] - окно обновления). Через [HINT_MS] по часам экрана ([rememberNow])
 * сворачивается сама (на стоящих часах снимков остаётся); свёрнутую разворачивает кружок [UpdateMark]. Касания мимо плашки идут в бой.
 */
@Composable internal fun RunUpdateHint(update: AvailableUpdate, onInstall: () -> Unit) {
    val hint = LocalUpdateHint.current
    if (!hint.open) return
    val now by rememberNow(HINT_TICK_MS)
    val since = remember(hint) { now }
    val left = HINT_MS - (now - since)
    if (left <= 0) {
        LaunchedEffect(Unit) { hint.collapse() }
        return
    }
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = RUN_BOTTOM), contentAlignment = Alignment.BottomCenter) {
        UpdateCard(Caution) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UpdateBadge(Caution)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(ui("update.server_ahead"), color = Caution, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Serif)
                    Text(ui("update.server_ahead_note", update.info.versionName), color = Parchment, style = MaterialTheme.typography.bodySmall)
                    val seconds = ceil(left / 1000.0).toInt()
                    val countdown = ui("update.collapse_in", seconds)
                    UpdateMeta(if (update.info.size > 0) "${ui("update.size", megabytes(update.info.size))} · $countdown" else countdown)
                }
            }
            UpdateActions(ui("update.collapse"), hint::collapse, onInstall)
        }
    }
}

/**
 * Кружок свёрнутой плашки жёсткого обновления - сразу за кнопкой меню забега (32 dp в зоне касания 44, цвет осторожности): касание
 * разворачивает плашку заново. Жёсткого обновления нет или плашка раскрыта - ничего. Читает один срез модели - версию ждущей жёсткой
 * сборки, не ход её загрузки.
 */
@Composable fun UpdateMark() {
    val updates = LocalUpdates.current ?: return
    val version by remember(updates) { updates.state.map { it.update?.takeIf { u -> u.hard }?.info?.versionName }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(null)
    val waiting = version ?: return
    val hint = LocalUpdateHint.current
    if (hint.open) return
    ForgeIconButton(
        Icons.Outlined.Upgrade,
        ui("update.waiting", waiting),
        onClick = hint::expand,
        kind = ButtonKind.Secondary,
        size = ButtonSize.Compact,
        accent = Caution,
        shape = IconButtonShape.Circle,
    )
}

/**
 * Тост мягкого обновления снизу над панелью вкладок (есть ли она - у текущего экрана навигатора, [Route.bars]; без панели - у края):
 * «Доступна X», размер, первая строка «Что нового» ([notesHeadline]) в одну строку с многоточием, «Позже» ([onLater] - до холодного
 * запуска) и «Обновить» ([onInstall] - окно обновления). Высота постоянна: длинные заметки его не растят.
 */
@Composable internal fun SoftUpdateToast(update: AvailableUpdate, onLater: () -> Unit, onInstall: () -> Unit) {
    val navigator = koinInject<Navigator>()
    val barShown by remember(navigator) { navigator.current.map { it.bars }.distinctUntilChanged() }.collectAsStateWithLifecycle(navigator.current.value.bars)
    Box(
        Modifier.fillMaxSize().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = if (barShown) ABOVE_BAR else RUN_BOTTOM),
        contentAlignment = Alignment.BottomCenter,
    ) {
        UpdateCard(Gold) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UpdateBadge(Gold)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(ui("update.found", update.info.versionName), color = GoldBright, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Serif)
                    if (update.info.size > 0) UpdateMeta(ui("update.size", megabytes(update.info.size)))
                }
            }
            val headline = notesHeadline(update.info.notes)
            if (headline.isNotEmpty()) {
                Text(ui("update.soft_notes", headline), color = Quiet, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            }
            UpdateActions(ui("update.later"), onLater, onInstall)
        }
    }
}

/** Карточка тоста и плашки: панель «Глубины» с оттенком [accent] и тонкой окантовкой его цвета, не шире окна обновления. */
@Composable private fun UpdateCard(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.widthIn(max = CARD_WIDTH).fillMaxWidth().depthPanel(CardShape, accent = accent, elevation = 14.dp)
            .border(1.dp, accent.copy(alpha = .35f), CardShape).padding(horizontal = 14.dp, vertical = 12.dp),
        content = content,
    )
}

/** Значок обновления в кружке цвета [tint]: тональная заливка и окантовка. */
@Composable private fun UpdateBadge(tint: Color) {
    Box(Modifier.size(40.dp).background(tint.copy(alpha = .14f), CircleShape).border(1.dp, tint.copy(alpha = .55f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Upgrade, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Мелкая строка под заголовком: размер, отсчёт. */
@Composable private fun UpdateMeta(text: String) = Text(text, color = Muted, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"))

/** Кнопки карточки справа: вторая [second] ([onSecond]) и «Обновить» ([onInstall]). */
@Composable private fun UpdateActions(second: String, onSecond: () -> Unit, onInstall: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        ForgeButton(onClick = onSecond, kind = ButtonKind.Secondary, size = ButtonSize.Wide) { Text(second) }
        ForgeButton(onClick = onInstall, size = ButtonSize.Wide) { Text(ui("update.install")) }
    }
}

/** Сколько плашка жёсткого обновления раскрыта, прежде чем свернуться в кружок, мс. */
private const val HINT_MS = 10_000L

/** Такт отсчёта плашки. */
private const val HINT_TICK_MS = 1_000L

/** Тост не шире окна обновления и на планшете. */
private val CARD_WIDTH = 420.dp

private val CardShape = RoundedCornerShape(18.dp)

/** Отступ тоста от низа над панелью вкладок: её высота 68 dp, её отступ 8 dp и зазор 12 dp. */
private val ABOVE_BAR = 88.dp

/** Отступ плашки и тоста от низа, когда панели вкладок нет (бой). */
private val RUN_BOTTOM = 20.dp
