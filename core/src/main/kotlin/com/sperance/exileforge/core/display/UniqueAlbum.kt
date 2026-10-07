package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.core.model.hero.UniqueFind
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ItemTemplate
import com.sperance.exileforge.rules.table.TableKind

/** Найденный уникальный шаблон: он сам и его находка. */
data class FoundUnique(val template: ItemTemplate, val find: UniqueFind)

/** Пул уникалок «Истории» (3.90.2): тег таблицы шаблонов, её уникальные и мифические шаблоны и название. */
data class UniquePool(val tag: String, val title: String, val templates: List<ItemTemplate>) {
    /** Найденные из пула, свежие первыми. */
    fun found(finds: Map<String, UniqueFind>): List<FoundUnique> = templates.mapNotNull { t -> finds[t.code]?.let { FoundUnique(t, it) } }.sortedByDescending { it.find.at }

    /** Сколько шаблонов пула найдено. */
    fun count(finds: Map<String, UniqueFind>): Int = templates.count { it.code in finds }
}

/**
 * Альбом уникальных вещей (3.90.2): что вообще бывает и откуда. Пул - таблица шаблонов контента, целиком из уникальных и
 * мифических (мир, шанс, кузня, Бездна, мифические, механики); таблицы боссов (`boss:*`) - отдельная группа, по пулу на
 * босса. Смешанные таблицы добычи пулом не считаются: их уникалки и так лежат в пулах выше. Новая таблица в контенте -
 * новый пул без правки кода; название - словарь клиента `history.pool.<тег>`, босс - его имя.
 */
class UniqueAlbum(index: ContentIndex, lang: Lang = uiLanguage) {
    /** Все уникальные и мифические шаблоны контента. */
    val all: List<ItemTemplate> = index.templates.values.filter { it.unique }

    val pools: List<UniquePool>
    val bosses: List<UniquePool>

    init {
        val tables = index.tables.tags.filter { index.tables.kind(it) == TableKind.TEMPLATE }.mapNotNull { tag ->
            val members = index.templatePool(listOf(tag)).map { it.value }.distinctBy { it.code }
            members.takeIf { it.isNotEmpty() }?.let { tag to it }
        }
        val (boss, rest) = tables.partition { (tag, _) -> tag.startsWith(BOSS) }
        pools = rest.filter { (_, members) -> members.all { it.unique } }
            .map { (tag, members) -> UniquePool(tag, uiOr(lang, "history.pool.$tag", displayName(tag.substringAfter(':'), lang)), members) }
            .sortedByDescending { it.templates.size }
        bosses = boss.mapNotNull { (tag, members) -> members.filter { it.unique }.takeIf { it.isNotEmpty() }?.let { UniquePool(tag, monsterTitle(tag.removePrefix(BOSS)), it) } }
            .sortedBy { it.title }
    }

    /** Все найденные, свежие первыми; шаблон, которого контент больше не знает, пропускается. */
    fun found(finds: Map<String, UniqueFind>): List<FoundUnique> = all.mapNotNull { t -> finds[t.code]?.let { FoundUnique(t, it) } }.sortedByDescending { it.find.at }

    private companion object {
        const val BOSS = "boss:"
    }
}
