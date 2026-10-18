package com.sperance.exileforge.core

import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.rules.content.Op
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Строка стата по шаблону сервера не оставляет игроку подстановок: каждая характеристика и сила контента на каждом языке, любой
 * операцией и знаком, в строке стата, глотка фляги и описании умений героя и монстров - без `{`.
 */
class StatLineTextTest {
    private val index = TestContent.index

    /** Характеристики реестра и силы уникалок: всё, что может назвать строка стата. */
    private val stats: Set<String> by lazy { index.stats.stats.map { it.code }.toSet() + index.powers.byStat.keys }

    @Test
    fun noStatLineKeepsAPlaceholder() {
        val left = inEveryLanguage {
            stats.flatMap { stat ->
                Op.entries.flatMap { op -> VALUES.flatMap { v -> listOf(SkillText.statLine(stat, op, v, index), SkillText.draughtLine(stat, op, v, index)) } }
            }
        }
        assertTrue(left.isEmpty(), "подстановки в строках статов: ${left.take(SHOWN)}")
    }

    @Test
    fun noSkillLineKeepsAPlaceholder() {
        val left = inEveryLanguage {
            index.skills.skills.flatMap { SkillText.lines(it, 1, index) } + index.skills.monsterSkills.flatMap { SkillText.monster(it, index) }
        }
        assertTrue(left.isEmpty(), "подстановки в строках умений: ${left.take(SHOWN)}")
    }

    /** Строки [lines] на каждом языке словаря сервера; результат - те, где осталась подстановка, с кодом языка. */
    private fun inEveryLanguage(lines: () -> List<String>): List<String> {
        val locales = TestContent.serverLocales()
        val wasLocale = serverLocale
        val wasLanguage = uiLanguage
        try {
            return Lang.entries.flatMap { lang ->
                serverLocale = LocaleBundle(lang.code, "", locales.getValue(lang.code))
                uiLanguage = lang
                lines().filter { '{' in it }.map { "${lang.code}: $it" }
            }
        } finally {
            serverLocale = wasLocale
            uiLanguage = wasLanguage
        }
    }

    private companion object {
        /** Значения строки: прибавка, доля и минус - у минуса свой шаблон `.negative`. */
        val VALUES = listOf(5.0, 0.25, -5.0)

        /** Сколько строк показать в отчёте о провале. */
        const val SHOWN = 20
    }
}
