package com.sperance.exileforge

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.ui.ForgeApp
import com.sperance.exileforge.ui.theme.ForgeTheme

/**
 * The one activity. It is a [FragmentActivity] since 2.24.0 because the campaign's scene is a
 * libGDX fragment inside the Compose tree, and libGDX asks its host for [AndroidFragmentApplication.Callbacks].
 */
class MainActivity : FragmentActivity(), AndroidFragmentApplication.Callbacks {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: ForgeViewModel = viewModel(factory = ForgeViewModel.Factory(application as ForgeApplication))
            ForgeTheme { ForgeApp(vm) }
        }
    }

    /** libGDX's "quit" — the scene never ends the app; leaving a run is the overlay's button. */
    override fun exit() = Unit
}
