package com.sperance.exileforge

import android.app.Application
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.data.settings.deviceId

class ForgeApplication : Application() {
    val journal = RequestJournal()
    val serverStore by lazy { ServerStore(this) }
    /** The account's name on this device; it is derived, not stored, so it cannot drift. */
    val deviceId by lazy { deviceId(this) }
}
