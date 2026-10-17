package com.sperance.exileforge

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.sperance.exileforge.platform.FrameRateCap
import com.sperance.exileforge.platform.NetworkWatch
import com.sperance.exileforge.platform.VolumeChord
import com.sperance.exileforge.ui.ForgeApp
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.LocalVolumeChord
import com.sperance.exileforge.ui.components.UpdateViewModel
import com.sperance.exileforge.ui.theme.ForgeTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    /**
     * The shell's model, owned by the activity so the lifecycle reaches it too; the composition gets the very same
     * instance, the activity being its store. The runtime it starts lives with the process, not with the activity.
     */
    private val shell: ShellViewModel by viewModel()

    /** Updates from GitHub Releases: checked against the server the game model is connected to. */
    private val updates: UpdateViewModel by viewModel { parametersOf(shell.newerServer, shell.serverFailed) }

    /** Сеть устройства: вернувшаяся будит связь сразу. */
    private val network by lazy { NetworkWatch(applicationContext, shell::networkBack) }

    /**
     * Громкость↑ и Громкость↓ вместе: лист отчёта. Уровня приложения: окна диалогов цепляются к нему через
     * [LocalVolumeChord]. Снимков экрана нет.
     */
    private val chord = VolumeChord { shell.requestBug() }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = chord.onKey(event) || super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean = chord.onKey(event) || super.onKeyUp(keyCode, event)

    /** Окно без фокуса (диалог поверх, шторка системы) - аккорд начинается заново. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) chord.reset()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FrameRateCap.apply(this)
        setContent { CompositionLocalProvider(LocalVolumeChord provides chord) { ForgeTheme { ForgeApp(updates) } } }
    }

    /** Back in the foreground — unlocked or switched to: the connection is restored without a tap. */
    override fun onStart() {
        super.onStart()
        shell.reconnect()
        network.start()
    }

    /**
     * Leaving the foreground sends the run's journal at once: a process killed in the background must not take it along.
     * The moment is noted, so a long absence refreshes the screen on return.
     */
    override fun onStop() {
        network.stop()
        shell.away()
        shell.flushRun()
        super.onStop()
    }
}
