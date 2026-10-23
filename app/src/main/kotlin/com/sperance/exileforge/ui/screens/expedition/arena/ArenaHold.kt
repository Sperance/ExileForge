package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Удержание боя арены окном поверх неё: `true` берёт удержание, `false` отдаёт ([com.sperance.exileforge.core.campaign.run.RunCommand.Hold]).
 * Его даёт арена ([ArenaOverlay]); вне арены - null, окно бой не держит.
 */
internal val LocalArenaHold = staticCompositionLocalOf<((Boolean) -> Unit)?> { null }

/**
 * Держит бой арены, пока окно в композиции: бой, что допускает удержание, стоит; бой без удержания (вне арены) идёт дальше, окно
 * живёт поверх него.
 */
@Composable internal fun ArenaHold() {
    val hold = LocalArenaHold.current ?: return
    DisposableEffect(hold) {
        hold(true)
        onDispose { hold(false) }
    }
}
