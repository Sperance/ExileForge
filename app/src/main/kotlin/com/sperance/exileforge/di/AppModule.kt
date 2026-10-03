package com.sperance.exileforge.di

import com.sperance.exileforge.core.crafts.CraftsRepository
import com.sperance.exileforge.core.feedback.FeedbackRepository
import com.sperance.exileforge.core.guild.GuildRepository
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.market.MarketRepository
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.core.quests.QuestRepository
import com.sperance.exileforge.core.session.Buzzes
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.ConnectionEventsHub
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.data.settings.DEFAULT_SERVER
import com.sperance.exileforge.data.settings.DraftStore
import com.sperance.exileforge.data.settings.GuideStore
import com.sperance.exileforge.data.settings.PreferencesRepository
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.Actions
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.Repositories
import com.sperance.exileforge.presentation.crafts.CraftsActions
import com.sperance.exileforge.presentation.crafts.CraftsViewModel
import com.sperance.exileforge.presentation.features.UpdateViewModel
import com.sperance.exileforge.presentation.feedback.FeedbackViewModel
import com.sperance.exileforge.presentation.forge.SmithyViewModel
import com.sperance.exileforge.presentation.guild.GuildActions
import com.sperance.exileforge.presentation.guild.GuildViewModel
import com.sperance.exileforge.presentation.hero.HeroActions
import com.sperance.exileforge.presentation.hero.HeroSync
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.market.MarketActions
import com.sperance.exileforge.presentation.market.MarketViewModel
import com.sperance.exileforge.presentation.quests.QuestActions
import com.sperance.exileforge.presentation.quests.QuestViewModel
import com.sperance.exileforge.presentation.server.ServerViewModel
import com.sperance.exileforge.presentation.settings.SettingsViewModel
import com.sperance.exileforge.presentation.skills.GrimoireViewModel
import com.sperance.exileforge.presentation.tree.TreeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Область корутин приложения: живёт, пока живёт процесс; для сторов, пишущих вне экрана. */
const val APP_SCOPE = "app"

/**
 * Граф приложения (3.81.0): хранилища устройства, журнал запросов и модели экранов. Сеть и репозитории
 * героя переезжают сюда следующими этапами (`REFACTORING.md`).
 */
val appModule = module {
    single<CoroutineScope>(named(APP_SCOPE)) { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    single { RequestJournal() }
    single { ServerStore(androidContext()) }
    single { GuideStore(androidContext()) }
    single { DraftStore(androidContext(), get(named(APP_SCOPE))) }
    single { PreferencesRepository(get(), get(named(APP_SCOPE))) }
    single { SessionRepository(DEFAULT_SERVER) }
    single { WorldRepository() }
    single { ServerConnection() }
    single { ConnectionEventsHub() }
    single { CommandRunner(get(named(APP_SCOPE)), get<ConnectionEventsHub>()) }
    single { Notices() }
    single { GameEvents() }
    single { FeedbackRepository() }
    single { HeroRepository() }
    single { QuestRepository() }
    single { MarketRepository() }
    singleOf(::MarketActions)
    single { GuildRepository() }
    singleOf(::GuildActions)
    single { Buzzes() }
    single { HeroSync(get(), get(), get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    singleOf(::HeroActions)
    single { CraftsRepository() }
    single { CraftsActions(get(), get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    single { ContentLoader() }
    single { QuestActions(get(), get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    singleOf(::Repositories)
    singleOf(::Actions)
    viewModelOf(::ForgeViewModel)
    viewModelOf(::FeedbackViewModel)
    viewModelOf(::QuestViewModel)
    viewModelOf(::MarketViewModel)
    viewModelOf(::GuildViewModel)
    viewModelOf(::CraftsViewModel)
    viewModelOf(::TreeViewModel)
    viewModelOf(::HeroViewModel)
    viewModelOf(::SmithyViewModel)
    viewModelOf(::GrimoireViewModel)
    viewModel { SettingsViewModel(get()) }
    viewModel { ServerViewModel(get(), get()) }
    // Проверка обновлений ждёт сервер игровой модели: поток и манифест приходят параметрами из активности.
    viewModel { params -> UpdateViewModel(androidApplication(), params.get(1), params.get(0), get()) }
}
