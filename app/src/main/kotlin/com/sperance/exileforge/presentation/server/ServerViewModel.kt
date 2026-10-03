package com.sperance.exileforge.presentation.server

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.session.Session
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.world.World
import com.sperance.exileforge.core.world.WorldRepository
import kotlinx.coroutines.flow.StateFlow

/** Экран «Аккаунт и сервер»: сессия и мир из репозиториев :core; команды пока идут через общую модель. */
class ServerViewModel(sessions: SessionRepository, worlds: WorldRepository) : ViewModel() {
    val session: StateFlow<Session> = sessions.state
    val world: StateFlow<World> = worlds.state
}
