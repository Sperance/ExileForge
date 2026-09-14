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
        Text("${s.profile?.name.orEmpty()} · ${if(s.isAdmin) "Администратор" else "Игрок"}")
        var change by remember { mutableStateOf(false) }
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        TextButton(onClick = { change = !change; oldPassword = ""; newPassword = "" }) { Text("Изменить пароль") }
        if(change) {
            OutlinedTextField(oldPassword, { oldPassword = it }, label = { Text("Текущий пароль") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            OutlinedTextField(newPassword, { newPassword = it }, label = { Text("Новый пароль, 12–128 символов") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            Button(enabled = !s.busy && oldPassword.isNotEmpty() && newPassword.length in 12..128, onClick = { vm.changePassword(oldPassword, newPassword); oldPassword = ""; newPassword = ""; change = false }) { Text("Сменить пароль") }
        }
        OutlinedButton(enabled = !s.busy, onClick = vm::logout) { Text("Выйти") }
    } else {
        OutlinedTextField(login, { login = it }, enabled = !s.busy, label = { Text("Логин") }, singleLine = true)
        OutlinedTextField(password, { password = it }, enabled = !s.busy, label = { Text("Пароль") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        Button(enabled = !s.busy && login.isNotBlank() && password.isNotEmpty(), onClick = { vm.login(login, password); password = "" }) { Text("Войти") }
    }
}
