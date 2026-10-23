package com.sperance.exileforge.core

import com.sperance.exileforge.core.expedition.refusedKey
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.UiStrings
import com.sperance.exileforge.core.i18n.locError
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.rules.content.TrialEventKind
import com.sperance.exileforge.rules.run.RefusalReason
import com.sperance.exileforge.rules.run.RunEventKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Отказ без id: аргумент-id и id в собственном тексте сервера игроку не печатаются; у каждого вида события и причины есть имя в тосте отказа. */
class RefusalTextTest {
    /** Ключ [refusedKey] каждого вида события захода и испытания и каждой причины отказа есть в словаре: вид или код, что сервер добавил без строки, ловится здесь. */
    @Test
    fun everyEventKindIsNamedInTheRefusalToast() {
        val known = UiStrings.keys(Lang.RU)
        val keys = RunEventKind.entries.map { it.refusedKey() } + TrialEventKind.entries.map { it.refusedKey() } + RefusalReason.entries.map { it.refusedKey() }
        assertEquals(emptyList(), keys.filterNot { it in known }, "нет в словаре")
        assertTrue(keys.isNotEmpty())
    }

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
