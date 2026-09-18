package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Gold
import kotlinx.serialization.json.*

@Composable fun LoginForm(s: ForgeState, vm: ForgeViewModel) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    if(s.signedIn) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(ForgeGlyphs.Exile, null, tint = Gold, modifier = Modifier.size(20.dp))
            Text("${s.profile?.name.orEmpty()} · ${if(s.isAdmin) tr("Администратор", "Administrator") else tr("Игрок", "Player")}")
        }
        var change by remember { mutableStateOf(false) }
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        TextButton(onClick = { change = !change; oldPassword = ""; newPassword = "" }) { Text(tr("Изменить пароль", "Change password")) }
        if(change) {
            OutlinedTextField(oldPassword, { oldPassword = it }, label = { Text(tr("Текущий пароль", "Current password")) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(newPassword, { newPassword = it }, label = { Text(tr("Новый пароль, 6–64 символа, цифра и заглавная", "New password, 6–64 characters, a digit and a capital")) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(enabled = !s.busy && oldPassword.isNotEmpty() && newPassword.length in 6..64, onClick = { vm.changePassword(oldPassword, newPassword); oldPassword = ""; newPassword = ""; change = false }) { Text(tr("Сменить пароль", "Change password")) }
        }
        OutlinedButton(enabled = !s.busy, onClick = vm::logout) { Text(tr("Выйти", "Sign out")) }
    } else {
        OutlinedTextField(login, { login = it }, enabled = !s.busy, label = { Text(tr("Логин", "Login")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, enabled = !s.busy, label = { Text(tr("Пароль", "Password")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Button(enabled = !s.busy && login.isNotBlank() && password.isNotEmpty(), onClick = { vm.login(login, password); password = "" }, modifier = Modifier.fillMaxWidth()) { Text(tr("Войти", "Sign in")) }
    }
}
