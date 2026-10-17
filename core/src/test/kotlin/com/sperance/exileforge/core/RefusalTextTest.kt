package com.sperance.exileforge.core

import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.locError
import com.sperance.exileforge.core.i18n.serverLocale
import kotlin.test.Test
import kotlin.test.assertEquals

/** Отказ без id: аргумент-id и id в собственном тексте сервера игроку не печатаются. */
class RefusalTextTest {
    @Test
    fun aRefusalPrintsNoId() {
        val was = serverLocale
        try {
            serverLocale = LocaleBundle("ru", "", mapOf("error.X_001" to "Лот {0} не найден", "error.X_002" to "Не хватает: {1} {0}"))
            assertEquals("Лот не найден", locError("X_001", "", listOf("65f1c2a9e4b0a1b2c3d4e5f6")))
            assertEquals("Не хватает: 5 CHAOS", locError("X_002", "", listOf("CHAOS", "5")), "a number and a key stay")
            assertEquals("Предложение ушло", locError(null, "Предложение 65f1c2a9e4b0a1b2c3d4e5f6 ушло"))
        } finally {
            serverLocale = was
        }
    }
}
