package com.sperance.exileforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.features.UpdateViewModel
import com.sperance.exileforge.ui.ForgeApp
import com.sperance.exileforge.ui.theme.ForgeTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    /** The one model of the activity, owned here rather than by the composition so the lifecycle can reach it too. */
    private val viewModel: ForgeViewModel by viewModel()

    /** Updates from GitHub Releases (3.72.0): checked against the server the game model is connected to. */
    private val updates: UpdateViewModel by viewModel { parametersOf(viewModel.newerServer, suspend { viewModel.serverManifest() }) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ForgeTheme { ForgeApp(updates) } }
    }

    /** Back in the foreground — unlocked or switched to: the connection is restored without a tap. */
    override fun onStart() {
        super.onStart()
        viewModel.reconnect()
    }

    /**
     * Leaving the foreground sends the run's journal at once: a process killed in the background must not take it along.
     * The moment is noted, so a long absence refreshes the screen on return.
     */
    override fun onStop() {
        viewModel.away()
        viewModel.flushRun()
        super.onStop()
    }
}
