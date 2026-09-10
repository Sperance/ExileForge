package com.sperance.exileforge

import android.app.Application
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.data.settings.ServerStore

class ForgeApplication : Application() {
    val journal = RequestJournal()
    val serverStore by lazy { ServerStore(this) }
}
