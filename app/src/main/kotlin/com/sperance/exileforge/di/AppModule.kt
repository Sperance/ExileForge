package com.sperance.exileforge.di

import com.sperance.exileforge.core.admin.AdminRepository
import com.sperance.exileforge.core.admin.ModerationModel
import com.sperance.exileforge.core.admin.RedemptionActions
import com.sperance.exileforge.core.admin.SanctionModel
import com.sperance.exileforge.core.app.Actions
import com.sperance.exileforge.core.app.AppStartup
import com.sperance.exileforge.core.app.CharacterActions
import com.sperance.exileforge.core.app.CharactersModel
import com.sperance.exileforge.core.app.ClientDevice
import com.sperance.exileforge.core.app.ConnectionActions
import com.sperance.exileforge.core.app.DEFAULT_SERVER
import com.sperance.exileforge.core.app.DeviceStore
import com.sperance.exileforge.core.app.Foreground
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.HeroCache
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.app.Repositories
import com.sperance.exileforge.core.app.Screens
import com.sperance.exileforge.core.app.ServerReach
import com.sperance.exileforge.core.app.SessionActions
import com.sperance.exileforge.core.app.SettingsStore
import com.sperance.exileforge.core.app.ShellModel
import com.sperance.exileforge.core.app.StartupTrace
import com.sperance.exileforge.core.app.WarmupActions
import com.sperance.exileforge.core.campaign.ExpeditionRepository
import com.sperance.exileforge.core.crafts.CraftsActions
import com.sperance.exileforge.core.crafts.CraftsRepository
import com.sperance.exileforge.core.expedition.ExpeditionActions
import com.sperance.exileforge.core.expedition.RiftActions
import com.sperance.exileforge.core.expedition.TrialActions
import com.sperance.exileforge.core.feedback.BugEvidence
import com.sperance.exileforge.core.feedback.FeedbackRepository
import com.sperance.exileforge.core.feedback.LetterActions
import com.sperance.exileforge.core.feedback.ReportTrail
import com.sperance.exileforge.core.feedback.SeenReplies
import com.sperance.exileforge.core.guild.GuildActions
import com.sperance.exileforge.core.guild.GuildRepository
import com.sperance.exileforge.core.hall.HallFocus
import com.sperance.exileforge.core.hall.HallModel
import com.sperance.exileforge.core.hero.HeroActions
import com.sperance.exileforge.core.hero.HeroPageModel
import com.sperance.exileforge.core.hero.HeroReads
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.hero.HeroSync
import com.sperance.exileforge.core.hero.TreeModel
import com.sperance.exileforge.core.i18n.LanguageRepository
import com.sperance.exileforge.core.market.MarketActions
import com.sperance.exileforge.core.market.MarketRepository
import com.sperance.exileforge.core.network.ForgeHttp
import com.sperance.exileforge.core.network.HttpLink
import com.sperance.exileforge.core.network.LinkRepository
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.core.player.PlayerCardModel
import com.sperance.exileforge.core.questboard.QuestActions
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
import com.sperance.exileforge.core.world.WorldLoader
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.data.settings.DraftStore
import com.sperance.exileforge.data.settings.GuideStore
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.platform.BugDelivery
import com.sperance.exileforge.platform.StallWatchdog
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.FeedbackViewModel
import com.sperance.exileforge.ui.components.PlayerCardViewModel
import com.sperance.exileforge.ui.components.UpdateViewModel
import com.sperance.exileforge.ui.nav.Navigator
import com.sperance.exileforge.ui.screens.admin.AdminViewModel
import com.sperance.exileforge.ui.screens.auction.MarketViewModel
import com.sperance.exileforge.ui.screens.city.HistoryViewModel
import com.sperance.exileforge.ui.screens.craft.SmithyViewModel
import com.sperance.exileforge.ui.screens.crafts.CraftsViewModel
import com.sperance.exileforge.ui.screens.expedition.ExpeditionViewModel
import com.sperance.exileforge.ui.screens.guild.GuildViewModel
import com.sperance.exileforge.ui.screens.hall.HallViewModel
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import com.sperance.exileforge.ui.screens.progress.ProgressViewModel
import com.sperance.exileforge.ui.screens.quests.QuestViewModel
import com.sperance.exileforge.ui.screens.server.ModerationViewModel
import com.sperance.exileforge.ui.screens.server.ServerViewModel
import com.sperance.exileforge.ui.screens.server.SettingsViewModel
import com.sperance.exileforge.ui.screens.session.CharactersViewModel
import com.sperance.exileforge.ui.screens.session.FateViewModel
import com.sperance.exileforge.ui.screens.session.NoticeViewModel
import com.sperance.exileforge.ui.screens.session.SessionViewModel
import com.sperance.exileforge.ui.screens.skills.GrimoireViewModel
import com.sperance.exileforge.ui.screens.tree.TreeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

/** Область корутин приложения: живёт, пока живёт процесс; для сторов, пишущих вне экрана. */
const val APP_SCOPE = "app"

/**
 * Граф приложения: хранилища устройства, журнал запросов, HTTP-клиент процесса, репозитории и сервисы ядра (`core/app` и
 * фичи), службы платформы и модели экранов; то, что ядро просит у приложения (хранилище, навигация, устройство), - его интерфейсами.
 *
 * Один HTTP-клиент на процесс: пул соединений, потоки и TLS-сессии общие у всех серверов и обновлений. Сервисы приложения (сессия, связь,
 * герои аккаунта, прогрев, коды наград, запуск) зовут друг друга и поэтому ссылаются лениво. Проверка обновлений ждёт сервер игровой
 * модели: поток и манифест приходят параметрами из активности.
 */
val appModule = module {
    single<CoroutineScope>(named(APP_SCOPE)) { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    single { RequestJournal() }
    single { ServerStore(androidContext()) } binds arrayOf(DeviceStore::class, SettingsStore::class, HeroCache::class)
    single { GuideStore(androidContext()) }
    single { DraftStore(androidContext(), get(named(APP_SCOPE))) }
    single { com.sperance.exileforge.data.settings.RepliesSeen(androidContext(), get(named(APP_SCOPE))) } bind SeenReplies::class
    single { BugDelivery(androidContext(), get(), get(), get(named(APP_SCOPE))) }
    single { PreferencesRepository(get(), get(named(APP_SCOPE))) }
    single { SessionRepository(DEFAULT_SERVER) }
    single { WorldRepository() }
    single { ServerConnection() }
    single { Foreground() }
    single { ServerReach(get(), lazy { get<ConnectionActions>() }, get(named(APP_SCOPE)), get(), get()) }
    single<HttpLink> { ForgeHttp.link }
    singleOf(::Navigator) { bind<Screens>() }
    single { ConnectionEventsHub() }
    single { CommandRunner(get(named(APP_SCOPE)), get<ConnectionEventsHub>(), StallReports(get(), get(named(APP_SCOPE)), get())) }
    single { Notices() }
    single { GameEvents() }
    single { FeedbackRepository() }
    single { HeroRepository() }
    single { QuestRepository() }
    single { MarketRepository() }
    singleOf(::MarketActions)
    single { GuildRepository() }
    singleOf(::GuildActions)
    singleOf(::LetterActions)
    singleOf(::HeroReads)
    single { Buzzes() }
    single { HeroSync(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    singleOf(::HeroActions)
    single { ExpeditionRepository() }
    single { ExpeditionActions(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), lazy { get<HeroActions>() }, get(named(APP_SCOPE))) }
    single { TrialActions(get(), get(), get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    single { RiftActions(get(), get(), get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    single { CraftsRepository() }
    single { CraftsActions(get(), get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE)), get()) }
    single { ContentLoader() }
    single { StartupTrace() }
    single { StallWatchdog(androidContext(), get(), get()) }
    single { com.sperance.exileforge.platform.CrashKeeper(androidContext()) }
    single { ReportTrail() }
    single { com.sperance.exileforge.platform.ReportEvidence(androidContext(), get(), get(), get(named(APP_SCOPE))) } bind ClientDevice::class
    single { WorldLoader(get(), get(), get(), get(), get(), get(named(APP_SCOPE)), get(), get()) }
    single { LanguageRepository() }
    single { LinkRepository() }
    single { AdminRepository() }
    single { GameSlice(get(), get(), get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    single { QuestActions(get(), get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    singleOf(::Repositories)
    singleOf(::Actions)
    single { SessionActions(get(), get(), get(), get(), get(), get(named(APP_SCOPE)), get(), get(), get(), get(), lazy { get<ConnectionActions>() }, lazy { get<CharacterActions>() }, lazy { get<WarmupActions>() }, get(), get(), get(), get()) }
    single { CharacterActions(get(), get(), get(), get(), get(), get(named(APP_SCOPE)), get(), get(), lazy { get<WarmupActions>() }, get(), get()) }
    single { ConnectionActions(get(), get(), get(), get(), get(), get(named(APP_SCOPE)), get(), get(), lazy { get<SessionActions>() }, lazy { get<CharacterActions>() }, get()) }
    single { WarmupActions(get(), get(), get(), get(), get(), get(named(APP_SCOPE)), get()) }
    single { RedemptionActions(get(), get(), get(), get(), get(), get(named(APP_SCOPE))) }
    single { AppStartup(get(), get(), get(), get(), get(), get(named(APP_SCOPE)), get(), get(), get(), get(), get(), get(), get(), get(), get(), get<StallWatchdog>(), get<com.sperance.exileforge.platform.CrashKeeper>(), get()) }
    single { BugEvidence(get(), get(), get<com.sperance.exileforge.platform.CrashKeeper>()) }
    factoryOf(::ShellModel)
    factoryOf(::ModerationModel)
    factoryOf(::SanctionModel)
    singleOf(::HallFocus)
    factoryOf(::HallModel)
    factoryOf(::PlayerCardModel)
    factoryOf(::HeroPageModel)
    factoryOf(::CharactersModel)
    factoryOf(::TreeModel)
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
    viewModel { SettingsViewModel(get(), get()) }
    viewModelOf(::ServerViewModel)
    viewModel { params ->
        val playing = combine(get<ExpeditionActions>().run, get<TrialActions>().arena, get<RiftActions>().state) { run, arena, rift -> run != null || arena != null || rift.arena != null }
        UpdateViewModel(androidApplication(), get(), params.get(0), get(), get(), get<WorldLoader>(), playing, params.get(1))
    }
}
