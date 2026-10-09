package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted

/**
 * Присутствие игрока (4.5.1) - по флагу сервера и времени последней команды: в сети, заходил в последние сутки или давно.
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

/** Сколько назад игрок был в сети: «сейчас», «12 мин назад», «3 ч назад», «5 д назад», «давно» - если время неизвестно. */
fun presenceText(online: Boolean, lastSeenAt: Long, now: Long = System.currentTimeMillis()): String {
    if (online) return ui("presence.now")
    if (lastSeenAt <= 0) return ui("presence.unknown")
    val minutes = ((now - lastSeenAt) / MINUTE_MS).coerceAtLeast(0)
    return when {
        minutes < 60 -> ui("presence.minutes", minutes.coerceAtLeast(1))
        minutes < 24 * 60 -> ui("presence.hours", minutes / 60)
        else -> ui("presence.days", minutes / (24 * 60))
    }
}

/** «был N назад» строкой состава и карточки; в сети - «в сети». */
fun seenAgoText(online: Boolean, lastSeenAt: Long, now: Long = System.currentTimeMillis()): String = if (online) ui("presence.online") else ui("presence.was", presenceText(false, lastSeenAt, now))

private const val MINUTE_MS = 60_000L
private const val DAY_MS = 86_400_000L
