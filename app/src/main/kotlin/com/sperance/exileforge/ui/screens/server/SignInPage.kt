package com.sperance.exileforge.ui.screens.server

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sperance.exileforge.core.app.AccountSeal
import com.sperance.exileforge.core.app.AccountUi
import com.sperance.exileforge.core.app.MAX_CHARACTERS
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.screens.session.SessionViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Страница «Вход» Врат мира: печать аккаунта ([AccountSealCard]), под ней строки-действия ([SignInAction]) - каждая открывает свой
 * лист: у аккаунта с логином «Сменить пароль» и «Выйти», у гостя «Зарегистрироваться», «Войти» и «Выйти»; в подвале - версия и сервер.
 * Не вошёл - только «Войти».
 */
@Composable internal fun SignInPage(account: AccountUi) {
    val sessionModel: SessionViewModel = koinViewModel()
    val slots = account.index?.rules?.maxCharacters ?: MAX_CHARACTERS
    val seal = remember(account.session, slots) { AccountSeal.of(account.session, slots) }
    var open by remember { mutableStateOf<SignInAction?>(null) }
    seal?.let { AccountSealCard(it, ui("account.signin_note")) }
    RowGroup(ui("signin.actions")) {
        (seal?.let(SignInAction::of) ?: listOf(SignInAction.SIGN_IN)).forEach { action -> SignInActionRow(action, enabled = !account.busy) { open = action } }
    }
    SignInFooter(account.session.server, account.isAdmin)
    val refusal = account.activity.refusal?.read()
    val close = { open = null }
    when (open) {
        SignInAction.CHANGE_PASSWORD -> PasswordSheet(account.inputs, account.busy, refusal, sessionModel::changePassword, close)
        SignInAction.REGISTER -> RegisterSheet(account.inputs, account.busy, refusal, sessionModel::bind, close)
        SignInAction.SIGN_IN -> LoginSheet(account.inputs, account.busy, refusal, sessionModel::login, close)
        SignInAction.SIGN_OUT -> SignOutSheet(seal?.guest == true, account.busy, sessionModel::logout, close)
        null -> Unit
    }
}
