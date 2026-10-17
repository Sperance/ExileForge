package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.hero.UniqueFind
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ItemTemplate

/** Найденный уникальный шаблон: он сам и его находка. */
data class FoundUnique(val template: ItemTemplate, val find: UniqueFind)

/** Пул уникалок «Истории»: тег таблицы шаблонов, её уникальные и мифические шаблоны и название. */
data class UniquePool(val tag: String, val title: String, val templates: List<ItemTemplate>) {
    /** Найденные из пула, свежие первыми. */
    fun found(finds: Map<String, UniqueFind>): List<FoundUnique> = templates.mapNotNull { t -> finds[t.code]?.let { FoundUnique(t, it) } }.sortedByDescending { it.find.at }

    /** Сколько шаблонов пула найдено. */
    fun count(finds: Map<String, UniqueFind>): Int = templates.count { it.code in finds }
}

/**
 * Альбом уникальных вещей: что вообще бывает и откуда. Категории правил ([ContentIndex.album],
 * `rules.json` → `album`): каждая уникальная и мифическая вещь лежит ровно в одной категории - первой по порядку старшинства
 * контента (боссы, мифические, механики, мир), пустая категория не показывается, пул Сферы удачи - способ получить вещь, а не её
 * источник, - в альбом не входит. Категории группы (`boss:*`) - отдельная группа, по пулу на босса; прочие - пулы в порядке
 * контента. Новая категория - запись контента без правки кода; название - словарь клиента `history.pool.<тег>`, босс - его имя.
 */
class UniqueAlbum(index: ContentIndex, lang: Lang = uiLanguage) {
    private val album = index.album

    /** Все уникальные и мифические шаблоны альбома - каждый в одной категории. */
    val all: List<ItemTemplate> = album.categories.flatMap { it.templates }

    /** Категории по порядку контента. */
    val pools: List<UniquePool> = album.categories.filterNot { it.grouped }.map { UniquePool(it.tag, ui(lang, "history.pool.${it.tag}"), it.templates) }

    /** Боссы - по пулу на босса, по имени. */
    val bosses: List<UniquePool> = album.categories.filter { it.grouped }.map { UniquePool(it.tag, monsterTitle(it.tag.removePrefix(BOSS)), it.templates) }.sortedBy { it.title }

    /** Сколько шаблонов контента найдено: шаблон, которого контент больше не знает, не в счёт. */
    fun count(finds: Map<String, UniqueFind>): Int = all.count { it.code in finds }

    /** Боссы, с которых найдена хоть одна вещь: без находок босс в Истории не показывается. */
    fun bossesFound(finds: Map<String, UniqueFind>): List<UniquePool> = bosses.filter { it.count(finds) > 0 }

    /** Все найденные, свежие первыми; шаблон, которого контент больше не знает, пропускается. */
    fun found(finds: Map<String, UniqueFind>): List<FoundUnique> = all.mapNotNull { t -> finds[t.code]?.let { FoundUnique(t, it) } }.sortedByDescending { it.find.at }

    private companion object {
        const val BOSS = "boss:"
    }
}
