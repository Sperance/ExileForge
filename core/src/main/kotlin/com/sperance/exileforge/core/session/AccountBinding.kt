package com.sperance.exileforge.core.session

import com.sperance.exileforge.rules.content.InputLimits

/**
 * Проверка формы «Создать аккаунт» (3.88.0, server 1.80.0) до запроса: те же пределы, что держит сервер (`rules.inputs`
 * `loginMin`..`accountLogin`, пароль `passwordMin`..`password`, логин - латиница, цифры и `_`). Сервер всё равно проверяет
 * сам; форма лишь не шлёт заведомый отказ.
 */
object AccountBinding {
    /** Что не так с формой; ключ строки словаря и её аргумент. */
    enum class Problem(val key: String) {
        LOGIN_LENGTH("account.bind_login_length"),
        LOGIN_CHARS("account.bind_login_chars"),
        PASSWORD_LENGTH("account.bind_password_length"),
        MISMATCH("account.bind_mismatch"),
    }

    private val LOGIN = Regex("[A-Za-z0-9_]+")

    /** Первая беда формы или null - форму можно слать. Логин сравнивается без пробелов по краям, как его пошлёт клиент. */
    fun problem(login: String, password: String, repeat: String, limits: InputLimits): Problem? {
        val name = login.trim()
        return when {
            name.length !in limits.loginMin..limits.accountLogin -> Problem.LOGIN_LENGTH
            !LOGIN.matches(name) -> Problem.LOGIN_CHARS
            password.length !in limits.passwordMin..limits.password -> Problem.PASSWORD_LENGTH
            password != repeat -> Problem.MISMATCH
            else -> null
        }
    }

    /** Аргумент строки беды: пределы длины словами «от-до». */
    fun argument(problem: Problem, limits: InputLimits): String = when (problem) {
        Problem.LOGIN_LENGTH -> "${limits.loginMin}-${limits.accountLogin}"
        Problem.PASSWORD_LENGTH -> "${limits.passwordMin}-${limits.password}"
        else -> ""
    }
}
