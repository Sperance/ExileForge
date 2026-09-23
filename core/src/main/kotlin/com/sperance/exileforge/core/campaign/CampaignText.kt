package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.model.campaign.MonsterModifier

/**
 * A monster modifier as a sentence: the server's template with each effect's value in its place.
 * A sentence is never a label with numbers appended — word order differs between languages.
 */
fun monsterModifierText(modifier: MonsterModifier): String =
    modifier.effects.foldIndexed(loc(LocaleKey.monsterModifierName(modifier.code))) { index, text, effect ->
        text.replace("{$index}", statNumber(effect.stat, effect.value))
    }

fun monsterTitle(code: String): String = loc(LocaleKey.monsterName(code))
fun mapTitle(code: String): String = loc(LocaleKey.mapName(code))
fun mapDescription(code: String): String = loc(LocaleKey.mapDescription(code))
fun chapterTitle(code: String): String = loc(LocaleKey.chapterName(code))
