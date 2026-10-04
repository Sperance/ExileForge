package com.sperance.exileforge.presentation.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.admin.AdminRepository
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.LanguageRepository
import com.sperance.exileforge.core.network.LinkRepository
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Session
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.world.World
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.presentation.state.AppModes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Экраны «Аккаунт» и «Настройки» (3.80.33): их срез [AccountUi] из репозиториев :core; команды - у моделей сессии и героя. */
class ServerViewModel(
    sessions: SessionRepository,
    worlds: WorldRepository,
    commands: CommandRunner,
    languages: LanguageRepository,
    links: LinkRepository,
    admins: AdminRepository,
    heroes: HeroRepository,
    modes: AppModes,
) : ViewModel() {
    val session: StateFlow<Session> = sessions.state
    val world: StateFlow<World> = worlds.state

    val ui: StateFlow<AccountUi> = combine(
        combine(sessions.state, commands.state, languages.lang, worlds.state) { session, activity, lang, world -> AccountUi(session, activity, lang, world) },
        combine(links.state, admins.state, heroes.state, modes.mode) { link, admin, hero, mode -> AccountUi(sessions.state.value, link = link, admin = admin, hero = hero, mode = mode) },
    ) { base, rest -> base.copy(link = rest.link, admin = rest.admin, hero = rest.hero, mode = rest.mode) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AccountUi(sessions.state.value, commands.state.value, languages.lang.value, worlds.state.value, links.state.value, admins.state.value, heroes.state.value, modes.mode.value),
        )
}
