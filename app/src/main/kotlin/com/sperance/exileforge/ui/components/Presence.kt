package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.presenceText
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted

/**
 * Присутствие игрока - по флагу сервера и времени последней команды: в сети, заходил в последние сутки или давно.
 * Цвет точки - [color]: зелёный, жёлтый, серый.
 */
enum class Presence(val color: Color) {
    ONLINE(Gold),
    RECENT(Caution),
    AWAY(Muted.copy(alpha = .5f)),
    ;

    companion object {
        /** Присутствие по флагу [online] сервера и последней команде [lastSeenAt] (мс эпохи, 0 - неизвестно). */
        fun of(online: Boolean, lastSeenAt: Long, now: Long = System.currentTimeMillis()): Presence = when {
            online -> ONLINE
            lastSeenAt > 0 && now - lastSeenAt < DAY_MS -> RECENT
            else -> AWAY
        }
    }
}

/** Точка присутствия игрока - зелёная, жёлтая или серая. */
@Composable fun PresenceDot(online: Boolean, lastSeenAt: Long, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(Presence.of(online, lastSeenAt).color, CircleShape))
}

/** Присутствие на живой миг: [online] - в сети, [text] - подпись давности ядра ([presenceText]). */
@Immutable
data class LivePresence(val online: Boolean, val text: String)

/**
 * Живое присутствие по последней команде [lastSeenAt] (мс эпохи, 0 - неизвестно): «сейчас» - по часам сервера с тактом минуты
 * ([rememberServerNow]), в сети ли - решает [online] на этот миг (флаг сервера или окно `rules.online`). Одна точка для всех экранов.
 */
@Composable fun rememberPresence(lastSeenAt: Long, online: (now: Long) -> Boolean): LivePresence {
    val now = rememberServerNow(PRESENCE_TICK_MS)
    val live = online(now)
    return LivePresence(live, presenceText(lastSeenAt, now, live))
}

private const val PRESENCE_TICK_MS = 60_000L
private const val DAY_MS = 86_400_000L
