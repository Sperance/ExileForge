package com.sperance.exileforge.di

import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.data.settings.DraftStore
import com.sperance.exileforge.data.settings.GuideStore
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.features.UpdateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
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
    viewModel { ForgeViewModel(get(), get()) }
    // Проверка обновлений ждёт сервер игровой модели: поток и манифест приходят параметрами из активности.
    viewModel { params -> UpdateViewModel(androidApplication(), params.get(1), params.get(0), get()) }
}
