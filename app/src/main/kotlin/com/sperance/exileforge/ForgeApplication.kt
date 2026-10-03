package com.sperance.exileforge

import android.app.Application
import com.sperance.exileforge.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ForgeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ForgeApplication)
            modules(appModule)
        }
    }
}
