package com.sperance.exileforge.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.sperance.exileforge.presentation.server.AccountUi
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.InputLimits
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted

/** The rules' input limits (3.73.0): every field cuts what is typed to them; until the content is read - [DefaultInputs]. */
val GameUi.inputs: InputLimits get() = index?.rules?.inputs ?: DefaultInputs
val AccountUi.inputs: InputLimits get() = index?.rules?.inputs ?: DefaultInputs

/**
 * Пределы, пока контент не прочитан (4.12.x: у правил нет значений по умолчанию): поля не режутся и не требуют длины - пределы
 * контента проверит сервер. Где контент под рукой, поле берёт его пределы ([GameUi.inputs], [AccountUi.inputs]).
 */
val DefaultInputs = Int.MAX_VALUE.let { open ->
    InputLimits(
        heroName = open, heroNameMin = 0, heroDescription = open, login = open, loginMin = 0, accountLogin = open, password = open,
        passwordMin = 0, search = open, server = open, code = open, number = open, report = open, mailSubject = open,
        mailBody = open, sanctionComment = open, appeal = open, gameServerCode = open, gameServerName = open,
    )
}

/** «12/24» under a field: how much of its limit is used, red once it is reached. */
@Composable fun LengthCounter(value: String, limit: Int) = Text("${value.length}/$limit", color = if (value.length >= limit) LifeRed else Muted, style = MaterialTheme.typography.labelSmall)
