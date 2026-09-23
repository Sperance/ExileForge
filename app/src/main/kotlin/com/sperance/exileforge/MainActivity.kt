package com.sperance.exileforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.ui.ForgeApp
import com.sperance.exileforge.ui.theme.ForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: ForgeViewModel = viewModel(factory = ForgeViewModel.Factory(application as ForgeApplication))
            ForgeTheme { ForgeApp(vm) }
        }
    }
}
