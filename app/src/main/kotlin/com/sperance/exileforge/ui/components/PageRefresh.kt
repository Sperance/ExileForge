package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * «Потянуть, чтобы обновить» для страницы внутри общей прокрутки экрана (0.14.4, #33): экран-хозяин кладёт свою прокрутку в
 * [RefreshHost], страница, что умеет перечитываться, объявляет это [Refreshable] - пока она на экране. Без объявления потягивание
 * ничего не показывает и не делает: хозяину не нужно знать, какие страницы перечитываются.
 */
@Stable
class PageRefresh internal constructor() {
    internal var action: (() -> Unit)? by mutableStateOf(null)
    internal var busy: Boolean by mutableStateOf(false)
}

private val LocalPageRefresh = staticCompositionLocalOf<PageRefresh?> { null }

@Composable fun rememberPageRefresh(): PageRefresh = remember { PageRefresh() }

/** Прокрутка хозяина [content] под потягиванием [refresh]: индикатор - только у страницы, что объявила [Refreshable]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshHost(refresh: PageRefresh, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val state = rememberPullToRefreshState()
    val action = refresh.action
    CompositionLocalProvider(LocalPageRefresh provides refresh) {
        PullToRefreshBox(
            isRefreshing = refresh.busy,
            onRefresh = { action?.invoke() },
            modifier = modifier,
            state = state,
            indicator = { if (action != null) PullToRefreshDefaults.Indicator(state = state, isRefreshing = refresh.busy, modifier = Modifier.align(Alignment.TopCenter)) },
            content = content,
        )
    }
}

/** Страница перечитывается потягиванием хозяина: [onRefresh] - перечитать, [busy] - чтение идёт. Вне [RefreshHost] - ничего. */
@Composable fun Refreshable(busy: Boolean, onRefresh: () -> Unit) {
    val refresh = LocalPageRefresh.current ?: return
    val latest by rememberUpdatedState(onRefresh)
    DisposableEffect(refresh) {
        refresh.action = { latest() }
        onDispose {
            refresh.action = null
            refresh.busy = false
        }
    }
    SideEffect { refresh.busy = busy }
}
