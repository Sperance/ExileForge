package com.sperance.exileforge.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment

/** Высота строки цепочки. */
private val TRAIL_HEIGHT = 40.dp

/** Звено цепочки: подпись уровня (вложенный уровень может её уточнить - [TrailTitle]) и как с него уйти на уровень выше. */
@Stable
private class TrailEntry(label: String, val back: State<() -> Unit>) {
    var label by mutableStateOf(label)
    var title by mutableStateOf<String?>(null)
    val shown: String get() = title ?: label
}

/** Уровни цепочки одного хозяина по глубине: 1 - первый вложенный, корень - сам хозяин. */
@Stable
private class TrailState {
    val levels = mutableStateMapOf<Int, TrailEntry>()
}

/** Где в цепочке стоит содержимое: хозяин и глубина уровня, что его окружает (0 - корень). */
private class TrailScope(val state: TrailState, val depth: Int)

private val LocalTrail = compositionLocalOf<TrailScope?> { null }

/**
 * Хозяин цепочки «назад» (4.4.x) - экран-хаб ([root] - его имя: «Город», «Развитие», «Поход»): одна строка ~40dp
 * «← Город › Гильдия [TAG] › Состав» над [content]. Вложенные уровни встают в неё сами ([TrailLevel]); пока их нет - строки нет.
 * Стрелка и системный «назад» - на уровень вверх, нажатие звена - прыжок к нему (уровни глубже закрываются по очереди).
 */
@Composable fun BackTrailHost(root: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = remember { TrailState() }
    val levels = state.levels.toSortedMap().values.toList()
    val up = levels.lastOrNull()
    BackHandler(enabled = up != null) { up?.back?.value?.invoke() }
    Column(modifier.fillMaxSize()) {
        if (up != null) BackTrail(root, levels)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            CompositionLocalProvider(LocalTrail provides TrailScope(state, 0), content = content)
        }
    }
}

/**
 * Вложенный уровень цепочки: звено [label] на глубине на одну больше окружающей, пока [content] на экране; [onBack] уходит с
 * него на уровень выше. Вне [BackTrailHost] - просто [content].
 */
@Composable fun TrailLevel(label: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    val parent = LocalTrail.current
    if (parent == null) {
        content()
        return
    }
    val depth = parent.depth + 1
    val back = rememberUpdatedState(onBack)
    val entry = remember(parent.state, depth) { TrailEntry(label, back) }
    entry.label = label
    DisposableEffect(parent.state, depth, entry) {
        parent.state.levels[depth] = entry
        onDispose { if (parent.state.levels[depth] === entry) parent.state.levels.remove(depth) }
    }
    CompositionLocalProvider(LocalTrail provides TrailScope(parent.state, depth), content = content)
}

/** Уточнённая подпись окружающего уровня, пока вызов на экране: гильдия называет себя «Гильдия [TAG]». */
@Composable fun TrailTitle(title: String) {
    val scope = LocalTrail.current ?: return
    val entry = scope.state.levels[scope.depth] ?: return
    DisposableEffect(entry, title) {
        entry.title = title
        onDispose { if (entry.title == title) entry.title = null }
    }
}

/** Строка цепочки: стрелка на уровень вверх, корень и звенья через «›»; последнее - где игрок сейчас, не нажимается. */
@Composable private fun BackTrail(root: String, levels: List<TrailEntry>) {
    // Прыжок к звену [keep] (0 - корень): уровни глубже него закрываются, начиная с самого глубокого
    val jump = { keep: Int -> levels.drop(keep).asReversed().forEach { it.back.value() } }
    Row(Modifier.fillMaxWidth().height(TRAIL_HEIGHT).padding(start = 4.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { levels.last().back.value() }, modifier = Modifier.size(TRAIL_HEIGHT)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, ui("common.back"), tint = Gold, modifier = Modifier.size(20.dp))
        }
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            val names = listOf(root) + levels.map { it.shown }
            names.forEachIndexed { i, name ->
                if (i > 0) Text(" › ", color = Muted, style = MaterialTheme.typography.labelLarge)
                val here = i == names.lastIndex
                Text(
                    name,
                    color = if (here) Parchment else Muted,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (here) Modifier else Modifier.clickable(role = Role.Button) { jump(i) },
                )
            }
        }
    }
}
