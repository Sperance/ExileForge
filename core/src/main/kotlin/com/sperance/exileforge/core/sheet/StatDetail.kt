package com.sperance.exileforge.core.sheet

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.rules.content.ContentIndex

/**
 * Подробное описание характеристики героя (`stat.detail.<код>` словаря клиента): абзацы через перевод строки - первый говорит, что
 * она даёт (его сводка раскрывает под строкой), следующие - где и когда она работает и с чем взаимодействует. Числа механик
 * текст не пишет руками: подстановки правил `{r:…}` заполняет контент ([com.sperance.exileforge.rules.text.TextNumbers.text]).
 * Короткая фраза `stat.desc.<код>` - цитата шапки разбора - остаётся отдельно.
 */
object StatDetail {
    fun key(stat: String): String = "stat.detail.$stat"

    /** Абзацы описания [stat] на языке [lang] с числами контента [index]; нет описания - пусто, нет числа - текст как есть. */
    fun paragraphs(stat: String, index: ContentIndex?, lang: Lang = uiLanguage): List<String> {
        val text = uiOr(lang, key(stat), "")
        if (text.isBlank()) return emptyList()
        val filled = index?.textNumbers?.text(text) ?: text
        return filled.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
