package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Низ экрана карты (макет «Сумка и нижние панели», вариант B): любая нижняя панель - фонтан, объект карты, вопрос об уходе - занимает
 * его, пока стоит на экране ([DockPanel]), и сумка захода ([MapBag]) на это время прячется. Одно состояние на все панели: новая
 * панель просто встаёт в [DockPanel], веток по её виду нет.
 */
@Stable class MapDock {
    private var claims by mutableIntStateOf(0)

    /** Низ занят хотя бы одной панелью. */
    val occupied: Boolean get() = claims > 0

    internal fun claim() {
        claims++
    }

    internal fun release() {
        claims--
    }
}

/** Низ карты экрана забега; вне карты - ничей (панель ничего не занимает). */
internal val LocalMapDock = staticCompositionLocalOf<MapDock?> { null }

/** Низ карты для [content] экрана забега: панели внутри занимают его через [DockPanel]. */
@Composable internal fun MapDockHost(content: @Composable (MapDock) -> Unit) {
    val dock = remember { MapDock() }
    CompositionLocalProvider(LocalMapDock provides dock) { content(dock) }
}

/** Нижняя панель карты: пока [content] на экране, низ занят. */
@Composable internal fun DockPanel(content: @Composable () -> Unit) {
    val dock = LocalMapDock.current
    DisposableEffect(dock) {
        dock?.claim()
        onDispose { dock?.release() }
    }
    content()
}
