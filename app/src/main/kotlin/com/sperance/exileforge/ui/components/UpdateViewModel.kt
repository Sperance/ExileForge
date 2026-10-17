package com.sperance.exileforge.ui.components

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.app.ServerReach
import com.sperance.exileforge.core.app.StartupTrace
import com.sperance.exileforge.core.network.HttpLink
import com.sperance.exileforge.core.update.UpdateBuild
import com.sperance.exileforge.core.update.UpdateModel
import com.sperance.exileforge.core.world.GameResources
import com.sperance.exileforge.update.SystemInstaller
import kotlinx.coroutines.flow.Flow

/** Обновления сборки: модель ядра [model] в области окна запуска с установщиком системы, сборкой и часами устройства. */
class UpdateViewModel(
    app: Application,
    reach: ServerReach,
    newerServer: Flow<Unit>,
    http: HttpLink,
    trace: StartupTrace,
    resources: GameResources,
    playing: Flow<Boolean>,
    serverFailed: Flow<Unit>,
) : AndroidViewModel(app) {
    val model = UpdateModel(
        reach, newerServer, http, trace, resources, playing, serverFailed,
        installer = SystemInstaller(app),
        build = UpdateBuild(BuildConfig.VERSION_CODE, BuildConfig.UPDATES),
        clock = SystemClock::elapsedRealtime,
        scope = viewModelScope,
    )
}
