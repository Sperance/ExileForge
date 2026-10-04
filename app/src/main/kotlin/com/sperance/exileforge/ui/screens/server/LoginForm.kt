package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.server.AccountUi
import com.sperance.exileforge.presentation.session.SessionViewModel
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeOutlinedButton
import com.sperance.exileforge.ui.components.ForgeTextButton
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Gold
import kotlinx.serialization.json.*
import org.koin.compose.viewmodel.koinViewModel

@Composable fun LoginForm(account: AccountUi) {
    val sessionModel: SessionViewModel = koinViewModel()
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    if (account.session.signedIn) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(ForgeGlyphs.Exile, null, tint = Gold, modifier = Modifier.size(20.dp))
            Text("${account.session.title} · ${if (account.isAdmin) ui("account.administrator") else ui("account.player")}")
        }
        var change by remember { mutableStateOf(false) }
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        ForgeTextButton(onClick = {
            change = !change
            oldPassword = ""
            newPassword = ""
        }) { Text(ui("account.change_password")) }
        if (change) {
            OutlinedTextField(oldPassword, { oldPassword = it.take(account.inputs.password) }, label = { Text(ui("account.current_password")) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(newPassword, { newPassword = it.take(account.inputs.password) }, label = { Text(ui("account.new_password")) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            ForgeButton(enabled = !account.busy && oldPassword.isNotEmpty() && newPassword.isNotEmpty(), onClick = {
                sessionModel.changePassword(oldPassword, newPassword)
                oldPassword = ""
                newPassword = ""
                change = false
            }) { Text(ui("account.do_change_password")) }
        }
        ForgeOutlinedButton(enabled = !account.busy, onClick = sessionModel::logout) { Text(ui("account.sign_out")) }
    } else {
        OutlinedTextField(login, { login = it.take(account.inputs.login) }, enabled = !account.busy, label = { Text(ui("account.login")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it.take(account.inputs.password) }, enabled = !account.busy, label = { Text(ui("account.password")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        ForgeButton(enabled = !account.busy && login.isNotBlank() && password.isNotEmpty(), onClick = {
            sessionModel.login(login, password)
            password = ""
        }, modifier = Modifier.fillMaxWidth()) { Text(ui("account.sign_in")) }
    }
}
