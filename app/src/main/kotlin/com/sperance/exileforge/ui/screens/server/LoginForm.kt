package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import kotlinx.serialization.json.*

@Composable fun LoginForm(s: ForgeState, vm: ForgeViewModel) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    if(s.signedIn) {
        Text("Сессия активна")
        OutlinedButton(enabled = !s.busy, onClick = vm::logout) { Text("Выйти") }
    } else {
        OutlinedTextField(login, { login = it }, enabled = !s.busy, label = { Text("Логин") }, singleLine = true)
        OutlinedTextField(password, { password = it }, enabled = !s.busy, label = { Text("Пароль") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        Button(enabled = !s.busy && login.isNotBlank() && password.isNotEmpty(), onClick = { vm.login(login, password); password = "" }) { Text("Войти") }
    }
}
