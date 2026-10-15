package com.sperance.exileforge

import android.app.Application
import com.sperance.exileforge.di.appModule
import com.sperance.exileforge.presentation.app.StallWatchdog
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ForgeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidContext(this@ForgeApplication)
            modules(appModule)
        }.koin
        // Падения (0.14.2) - на диск с первой секунды: следующий запуск пришлёт их отчётом
        koin.get<com.sperance.exileforge.presentation.app.CrashKeeper>().install()
        // Сторож главного потока (3.82.0) - с первой секунды: зависание запуска тоже его.
        koin.get<StallWatchdog>().start()
    }
}
