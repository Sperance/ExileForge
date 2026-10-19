package com.sperance.exileforge.ui.screens.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.admin.AdminRepository
import com.sperance.exileforge.core.app.AccountSlice
import com.sperance.exileforge.core.app.AccountUi
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.LanguageRepository
import com.sperance.exileforge.core.network.LinkRepository
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Session
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.world.World
import com.sperance.exileforge.core.world.WorldRepository
import kotlinx.coroutines.flow.StateFlow

/** Экраны «Аккаунт» и «Настройки»: их срез [AccountUi] ядра в области экрана; команды - у моделей сессии и героя. */
class ServerViewModel(
    sessions: SessionRepository,
    worlds: WorldRepository,
    commands: CommandRunner,
    languages: LanguageRepository,
    links: LinkRepository,
    admins: AdminRepository,
    heroes: HeroRepository,
) : ViewModel() {
    val session: StateFlow<Session> = sessions.state
    val world: StateFlow<World> = worlds.state

    val ui: StateFlow<AccountUi> = AccountSlice(sessions, worlds, commands, languages, links, admins, heroes, viewModelScope).ui
}
