package com.sperance.exileforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.features.UpdateViewModel
import com.sperance.exileforge.ui.ForgeApp
import com.sperance.exileforge.ui.theme.ForgeTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    /**
     * The shell's model, owned by the activity so the lifecycle reaches it too; the composition gets the very same
     * instance, the activity being its store. The runtime it starts lives with the process, not with the activity.
     */
    private val shell: ShellViewModel by viewModel()

    /** Updates from GitHub Releases (3.72.0): checked against the server the game model is connected to. */
    private val updates: UpdateViewModel by viewModel { parametersOf(shell.newerServer) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ForgeTheme { ForgeApp(updates) } }
    }

    /** Back in the foreground — unlocked or switched to: the connection is restored without a tap. */
    override fun onStart() {
        super.onStart()
        shell.reconnect()
    }

    /**
     * Leaving the foreground sends the run's journal at once: a process killed in the background must not take it along.
     * The moment is noted, so a long absence refreshes the screen on return.
     */
    override fun onStop() {
        shell.away()
        shell.flushRun()
        super.onStop()
    }
}
