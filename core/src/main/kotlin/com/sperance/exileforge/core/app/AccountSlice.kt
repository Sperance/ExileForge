package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.admin.AdminRepository
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.LanguageRepository
import com.sperance.exileforge.core.network.LinkRepository
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.world.WorldRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Срез «аккаунт» экранов «Аккаунт» и «Настройки» ([AccountUi]) из репозиториев ядра, в области экрана. */
class AccountSlice(
    sessions: SessionRepository,
    worlds: WorldRepository,
    commands: CommandRunner,
    languages: LanguageRepository,
    links: LinkRepository,
    admins: AdminRepository,
    heroes: HeroRepository,
    scope: CoroutineScope,
) {
    val ui: StateFlow<AccountUi> = combine(
        combine(sessions.state, commands.state, languages.lang, worlds.state) { session, activity, lang, world -> AccountUi(session, activity, lang, world) },
        combine(links.state, admins.state, heroes.state) { link, admin, hero -> AccountUi(sessions.state.value, link = link, admin = admin, hero = hero) },
    ) { base, rest -> base.copy(link = rest.link, admin = rest.admin, hero = rest.hero) }
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            AccountUi(sessions.state.value, commands.state.value, languages.lang.value, worlds.state.value, links.state.value, admins.state.value, heroes.state.value),
        )
}
