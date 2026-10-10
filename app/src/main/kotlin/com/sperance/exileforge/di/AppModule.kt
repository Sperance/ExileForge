package com.sperance.exileforge.di

import com.sperance.exileforge.core.admin.AdminRepository
import com.sperance.exileforge.core.campaign.ExpeditionRepository
import com.sperance.exileforge.core.crafts.CraftsRepository
import com.sperance.exileforge.core.feedback.FeedbackRepository
import com.sperance.exileforge.core.guild.GuildRepository
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.LanguageRepository
import com.sperance.exileforge.core.market.MarketRepository
import com.sperance.exileforge.core.network.ForgeHttp
import com.sperance.exileforge.core.network.LinkRepository
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.core.quests.QuestRepository
import com.sperance.exileforge.core.session.Buzzes
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.ConnectionEventsHub
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.session.StallReports
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.data.settings.DEFAULT_SERVER
import com.sperance.exileforge.data.settings.DraftStore
import com.sperance.exileforge.data.settings.GuideStore
import com.sperance.exileforge.data.settings.PreferencesRepository
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.Actions
import com.sperance.exileforge.presentation.Repositories
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.admin.AdminViewModel
import com.sperance.exileforge.presentation.admin.ModerationViewModel
import com.sperance.exileforge.presentation.admin.NoticeViewModel
import com.sperance.exileforge.presentation.app.AppStartup
import com.sperance.exileforge.presentation.app.CharacterActions
import com.sperance.exileforge.presentation.app.ConnectionActions
import com.sperance.exileforge.presentation.app.RedemptionActions
import com.sperance.exileforge.presentation.app.ServerReach
import com.sperance.exileforge.presentation.app.SessionActions
import com.sperance.exileforge.presentation.app.StallWatchdog
import com.sperance.exileforge.presentation.app.StartupTrace
import com.sperance.exileforge.presentation.app.WarmupActions
import com.sperance.exileforge.presentation.crafts.CraftsActions
import com.sperance.exileforge.presentation.crafts.CraftsViewModel
import com.sperance.exileforge.presentation.expedition.ExpeditionActions
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.expedition.RiftActions
import com.sperance.exileforge.presentation.expedition.TrialActions
import com.sperance.exileforge.presentation.features.UpdateViewModel
import com.sperance.exileforge.presentation.feedback.FeedbackViewModel
import com.sperance.exileforge.presentation.forge.SmithyViewModel
import com.sperance.exileforge.presentation.guild.GuildActions
import com.sperance.exileforge.presentation.guild.GuildViewModel
import com.sperance.exileforge.presentation.hall.HallViewModel
import com.sperance.exileforge.presentation.hero.HeroActions
import com.sperance.exileforge.presentation.hero.HeroReads
import com.sperance.exileforge.presentation.hero.HeroSync
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.history.HistoryViewModel
import com.sperance.exileforge.presentation.market.MarketActions
import com.sperance.exileforge.presentation.market.MarketViewModel
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.player.PlayerCardViewModel
import com.sperance.exileforge.presentation.progress.ProgressViewModel
import com.sperance.exileforge.presentation.quests.QuestActions
import com.sperance.exileforge.presentation.quests.QuestViewModel
import com.sperance.exileforge.presentation.server.ServerViewModel
import com.sperance.exileforge.presentation.session.CharactersViewModel
import com.sperance.exileforge.presentation.session.FateViewModel
import com.sperance.exileforge.presentation.session.SessionViewModel
import com.sperance.exileforge.presentation.settings.SettingsViewModel
import com.sperance.exileforge.presentation.skills.GrimoireViewModel
import com.sperance.exileforge.presentation.state.AppModes
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.tree.TreeViewModel
import com.sperance.exileforge.presentation.world.WorldLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import okhttp3.OkHttpClient
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
 * Граф приложения (3.81.0): хранилища устройства, журнал запросов, HTTP-клиент процесса, репозитории ядра, сервисы
 * `presentation/app` и модели экранов.
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
    single { ServerReach(get(), lazy { get<ConnectionActions>() }, get(named(APP_SCOPE)), get()) }
    // Один HTTP-клиент на процесс (3.80.45): пул соединений, потоки и TLS-сессии общие у всех серверов и обновлений.
    single<OkHttpClient> { ForgeHttp.client }
    singleOf(::Navigator)
    single { ConnectionEventsHub() }
    single { CommandRunner(get(named(APP_SCOPE)), get<ConnectionEventsHub>(), StallReports(get(), get(named(APP_SCOPE)))) }
    single { Notices() }
    single { GameEvents() }
    single { FeedbackRepository() }
    single { HeroRepository() }
    single { QuestRepository() }
    single { MarketRepository() }
    singleOf(::MarketActions)
    single { GuildRepository() }
    singleOf(::GuildActions)
    singleOf(::HeroReads)
    single { Buzzes() }
    single { HeroSync(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    singleOf(::HeroActions)
    single { ExpeditionRepository() }
    single { ExpeditionActions(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    single { TrialActions(get(), get(), get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    single { RiftActions(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { CraftsRepository() }
    single { CraftsActions(get(), get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    single { ContentLoader() }
    single { StartupTrace() }
    single { StallWatchdog(androidContext(), get(), get()) }
    single { WorldLoader(get(), get(), get(), get(), get(), get(named(APP_SCOPE)), get()) }
    single { LanguageRepository() }
    single { LinkRepository() }
    single { AdminRepository() }
    single { AppModes() }
    single { GameSlice(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    single { QuestActions(get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    singleOf(::Repositories)
    singleOf(::Actions)
    // Сервисы приложения (3.80.44, вместо `ForgeRuntime`): сессия, связь, герои аккаунта, прогрев, коды наград и запуск.
    // Сессия, связь и герои зовут друг друга, поэтому ссылаются лениво.
    single { SessionActions(get(), get(), get(), get(), get(), get(named(APP_SCOPE)), get(), get(), get(), get(), lazy { get<ConnectionActions>() }, lazy { get<CharacterActions>() }, lazy { get<WarmupActions>() }, get(), get()) }
    single { CharacterActions(get(), get(), get(), get(), get(), get(named(APP_SCOPE)), get(), get(), lazy { get<WarmupActions>() }, get()) }
    single { ConnectionActions(get(), get(), get(), get(), get(), get(named(APP_SCOPE)), get(), get(), lazy { get<SessionActions>() }, lazy { get<CharacterActions>() }) }
    single { WarmupActions(get(), get(), get(), get(), get(), get(named(APP_SCOPE)), get()) }
    single { RedemptionActions(get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    single { AppStartup(get(), get(), get(), get(), get(), get(named(APP_SCOPE)), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModelOf(::ShellViewModel)
    viewModelOf(::AdminViewModel)
    viewModelOf(::ModerationViewModel)
    viewModelOf(::NoticeViewModel)
    viewModelOf(::SessionViewModel)
    viewModelOf(::CharactersViewModel)
    viewModelOf(::FateViewModel)
    viewModelOf(::FeedbackViewModel)
    viewModelOf(::QuestViewModel)
    viewModelOf(::MarketViewModel)
    viewModelOf(::GuildViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::HallViewModel)
    viewModelOf(::PlayerCardViewModel)
    viewModelOf(::CraftsViewModel)
    viewModelOf(::ProgressViewModel)
    viewModelOf(::TreeViewModel)
    viewModelOf(::HeroViewModel)
    viewModelOf(::SmithyViewModel)
    viewModelOf(::GrimoireViewModel)
    viewModelOf(::ExpeditionViewModel)
    viewModel { SettingsViewModel(get()) }
    viewModelOf(::ServerViewModel)
    // Проверка обновлений ждёт сервер игровой модели: поток и манифест приходят параметрами из активности.
    // 3.90.3: и ресурсы игры, которых ждёт первая проверка, и поход с испытанием, во время которых проверок нет.
    viewModel { params ->
        val playing = combine(get<ExpeditionActions>().run, get<TrialActions>().arena, get<RiftActions>().state) { run, arena, rift -> run != null || arena != null || rift.arena != null }
        UpdateViewModel(androidApplication(), get(), params.get(0), get(), get(), get<WorldLoader>(), playing, params.get(1))
    }
}
