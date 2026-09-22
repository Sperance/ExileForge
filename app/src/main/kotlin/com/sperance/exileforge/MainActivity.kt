package com.sperance.exileforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.ui.ForgeApp
import com.sperance.exileforge.ui.theme.ForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The view model is built above the theme because the theme reads the language: Chinese
            // is drawn with the carved letter spacing closed up, and that is a property of the
            // typography, not of any one screen.
            val vm: ForgeViewModel = viewModel(factory = ForgeViewModel.Factory(application as ForgeApplication))
            val state by vm.state.collectAsStateWithLifecycle()
            ForgeTheme(state.lang) { ForgeApp(vm) }
        }
    }
}
