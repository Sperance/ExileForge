package com.sperance.exileforge.core.sheet

import com.sperance.exileforge.core.display.numberedText
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.text.LocaleKey

/**
 * Пояснение характеристики [stat] для строки, что её двигает: имя [title], короткая фраза [quote] ([StatDetail.quote], нет - null) и
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

    /**
     * Короткая фраза [stat] без чисел: словарь клиента на языке [lang] (характеристики героя), иначе словарь сервера (Атлас и карты,
     * `StatGroup.described`); нет нигде - пусто.
     */
    fun quote(stat: String, lang: Lang = uiLanguage): String = uiOr(lang, quoteKey(stat), "").ifBlank { locOr(LocaleKey.statDescription(stat), "") }

    /** Абзацы описания [stat] на языке [lang] с числами контента [index]; нет описания, контента или числа - пусто. */
    fun paragraphs(stat: String, index: ContentIndex?, lang: Lang = uiLanguage): List<String> {
        val text = uiOr(lang, key(stat), "")
        if (text.isBlank()) return emptyList()
        val filled = index.numberedText(text) ?: return emptyList()
        return filled.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** Есть ли у [stat] фраза ([quote]) или подробное описание на языке [lang]: строка с ней объясняет себя по касанию. */
    fun described(stat: String, lang: Lang = uiLanguage): Boolean = quote(stat, lang).isNotBlank() || uiOr(lang, key(stat), "").isNotBlank()

    /** Пояснения характеристик [stats] на языке [lang] по порядку, без повторов; характеристика без описания пропускается. */
    fun notes(stats: List<String>, index: ContentIndex?, lang: Lang = uiLanguage): List<StatNote> = stats.distinct().filter { described(it, lang) }.map { stat ->
        val quote = quote(stat, lang).takeIf { it.isNotBlank() }?.let { index.numberedText(it, stat) }
        StatNote(stat, statTitle(stat, lang), quote, paragraphs(stat, index, lang))
    }
}
