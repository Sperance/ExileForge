package com.sperance.exileforge.core

import com.sperance.exileforge.core.feedback.MailCategory
import com.sperance.exileforge.rules.content.SystemMail
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Каждый ключ системного письма сервера ([SystemMail]) разнесён по категории ящика ([MailCategory]): новый ключ не уходит в «Систему» молча. */
class MailCategoryTest {
    @Test
    fun every_system_mail_key_has_a_category() {
        val keys = SystemMail::class.java.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { it.get(null) as String }
        assertTrue(keys.isNotEmpty(), "ключи SystemMail не прочитаны")
        assertEquals(emptyList(), keys.filter { MailCategory.ofKey(it) == null }, "ключи письма без категории")
    }

    /** Письма о решениях Администрации - смена Предначертания и имени героя - лежат в «Администрации», а не в «Системе». */
    @Test
    fun administration_decisions_are_in_the_administration() {
        listOf(SystemMail.FATE_REDRAWN, SystemMail.HERO_RENAMED).forEach { assertEquals(MailCategory.ADMIN, MailCategory.ofKey(it), it) }
    }
}
