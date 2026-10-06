package com.sperance.exileforge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * Сворачиваемые шапки (3.88.7): прокрутка списка вниз сворачивает шапки экрана в компактную строку, прокрутка вверх -
 * возвращает. Одно состояние на открытый экран: оболочка ставит [connection] на контейнер экранов, так что любой список
 * внутри сообщает направление сам, а шапки читают [collapsed] через [LocalHeaderCollapse].
 */
@Stable
class HeaderCollapse {
    var collapsed by mutableStateOf(false)
        private set

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < -THRESHOLD) {
                collapsed = true
            } else if (available.y > THRESHOLD) {
                collapsed = false
            }
            return Offset.Zero
        }
    }

    private companion object {
        /** Сдвиг пальца за кадр, px, что считается намерением, а не дрожью. */
        const val THRESHOLD = 6f
    }
}

/** Состояние шапок открытого экрана; вне оболочки - своё, всегда развёрнутое. */
val LocalHeaderCollapse = staticCompositionLocalOf { HeaderCollapse() }

/**
 * Шапка, что уходит при прокрутке вниз: развёрнутая [full] видна, пока список не листают вниз; [compact] - строка,
 * что стоит на её месте свёрнутой (её может не быть: тогда шапка просто уходит, а вкладки под ней прилипают).
 */
@Composable fun CollapsibleHeader(compact: (@Composable () -> Unit)? = null, full: @Composable () -> Unit) {
    val collapsed = LocalHeaderCollapse.current.collapsed
    AnimatedVisibility(!collapsed, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) { full() }
    if (compact != null) AnimatedVisibility(collapsed, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) { compact() }
}
