package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.rules.content.StatRegistry
import com.sperance.exileforge.rules.content.StatSection

/** Имя раздела сводки героя. */
fun StatSection.title(lang: Lang = uiLanguage): String = ui(lang, "enum.stat_group.$name")

/**
 * Сводка, разложенная по разделам (4.6.5): раздел строки - `section` характеристики в контенте ([StatRegistry.section]), разделы
 * по своему порядку, пустые пропущены, внутри - порядок реестра. Без раздела строки нет: часть строки (`partOf`) читается в ней,
 * силы уникальных - блоком «Силы снаряжения», счётчики надетого (`NONE`) не показываются.
 */
fun groupedStats(stats: Map<String, Double>, registry: StatRegistry): List<Pair<StatSection, List<Pair<String, Double>>>> = stats.entries
    .filter { registry[it.key]?.partOf == null }
    .mapNotNull { entry -> registry.section(entry.key)?.let { it to entry } }
    .groupBy({ it.first }, { it.second })
    .toSortedMap()
    .map { (section, entries) -> section to entries.sortedWith(compareBy({ registry.order(it.key) }, { it.key })).map { it.key to it.value } }
