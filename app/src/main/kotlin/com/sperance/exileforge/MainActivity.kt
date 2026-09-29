package com.sperance.exileforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.ui.ForgeApp
import com.sperance.exileforge.ui.theme.ForgeTheme

class MainActivity : ComponentActivity() {
    /** The one model of the activity, owned here rather than by the composition so the lifecycle can reach it too. */
    private val viewModel: ForgeViewModel by viewModels { ForgeViewModel.Factory(application as ForgeApplication) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ForgeTheme { ForgeApp(viewModel) } }
    }

    /** Back in the foreground — unlocked or switched to: the connection is restored without a tap. */
    override fun onStart() {
        super.onStart()
        viewModel.reconnect()
    }

    /** Leaving the foreground sends the run's journal at once: a process killed in the background must not take it along. */
    override fun onStop() {
        viewModel.flushRun()
        super.onStop()
    }
}
