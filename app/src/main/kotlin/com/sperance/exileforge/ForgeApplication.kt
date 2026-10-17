package com.sperance.exileforge

import android.app.Application
import com.sperance.exileforge.di.appModule
import com.sperance.exileforge.platform.StallWatchdog
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ForgeApplication : Application() {
    /**
     * Падения пишутся на диск и сторож главного потока встаёт с первой секунды: следующий запуск пришлёт падение отчётом, зависание запуска
     * тоже ловится.
     */
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidContext(this@ForgeApplication)
            modules(appModule)
        }.koin
        koin.get<com.sperance.exileforge.platform.CrashKeeper>().install()
        koin.get<StallWatchdog>().start()
    }
}
