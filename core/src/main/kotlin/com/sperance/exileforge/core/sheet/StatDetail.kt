package com.sperance.exileforge.core.sheet

import com.sperance.exileforge.core.display.numberedText
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ModifierCode

/**
 * Пояснение характеристики [stat] для строки, что её двигает: имя [title], короткая фраза [quote] (`stat.desc.<код>`, нет - null) и
 * абзацы подробного описания [paragraphs] - тот же текст, что цитата и «Подробнее» разбора сводки героя.
 */
data class StatNote(val stat: String, val title: String, val quote: String?, val paragraphs: List<String>)

/**
 * Подробное описание характеристики героя (`stat.detail.<код>` словаря клиента): абзацы через перевод строки - первый говорит, что
 * она даёт (его сводка раскрывает под строкой), следующие - где и когда она работает и с чем взаимодействует. Числа механик
 * текст не пишет руками: подстановки правил `{r:…}` заполняет контент ([com.sperance.exileforge.rules.text.numbered]).
 * Короткая фраза `stat.desc.<код>` - цитата шапки разбора - остаётся отдельно.
 */
object StatDetail {
    fun key(stat: String): String = "stat.detail.$stat"

    /** Ключ короткой фразы характеристики [stat] - цитаты шапки разбора. */
    fun quoteKey(stat: String): String = "stat.desc.$stat"

    /** Абзацы описания [stat] на языке [lang] с числами контента [index]; нет описания, контента или числа - пусто. */
    fun paragraphs(stat: String, index: ContentIndex?, lang: Lang = uiLanguage): List<String> {
        val text = uiOr(lang, key(stat), "")
        if (text.isBlank()) return emptyList()
        val filled = index.numberedText(text) ?: return emptyList()
        return filled.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** Характеристики, что двигает модификатор [code], по порядку его эффектов; модификатора нет в контенте - пусто. */
    fun stats(index: ContentIndex, code: ModifierCode): List<String> = index.modifier(code)?.effects?.map { it.stat }.orEmpty()

    /** Есть ли у [stat] на языке [lang] фраза или подробное описание: строка с ней объясняет себя по касанию. */
    fun described(stat: String, lang: Lang = uiLanguage): Boolean = uiOr(lang, quoteKey(stat), "").isNotBlank() || uiOr(lang, key(stat), "").isNotBlank()

    /** Пояснения характеристик [stats] на языке [lang] по порядку, без повторов; характеристика без описания пропускается. */
    fun notes(stats: List<String>, index: ContentIndex?, lang: Lang = uiLanguage): List<StatNote> = stats.distinct().filter { described(it, lang) }.map { stat ->
        val quote = uiOr(lang, quoteKey(stat), "").takeIf { it.isNotBlank() }?.let { index.numberedText(it, stat) }
        StatNote(stat, statTitle(stat, lang), quote, paragraphs(stat, index, lang))
    }
}
