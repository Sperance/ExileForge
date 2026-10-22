package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.atlas.AtlasFog
import com.sperance.exileforge.core.display.ItemSearch
import com.sperance.exileforge.core.display.atlasBranchTitle
import com.sperance.exileforge.core.display.atlasNodeTitle
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.AtlasNode
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MapCode

/**
 * Место, что находит поиск по карте («Поход» - зона, Атлас - узел): ключ [key] для выбора, имя [title], строка сведений [facts],
 * уровень [level] (у узла - нет) и слова поиска [words] - имя и прочие имена места (страж зоны, ветвь узла), уже свёрнутые.
 */
class Place<K>(val key: K, val title: String, val facts: String, val level: Int?, val words: List<String>)

/**
 * Запрос поиска мест: пусто - ничего не ищется ([Blank]); число - уровень ±[LEVEL_SPREAD], «20-30» - вилка уровней ([Levels]); прочее -
 * слова имени ([Words], без регистра и «ё», как поиск предметов).
 */
sealed interface PlaceQuery {
    data object Blank : PlaceQuery

    data class Levels(val range: IntRange) : PlaceQuery

    data class Words(val folded: String) : PlaceQuery

    companion object {
        /** На сколько уровней вокруг набранного числа ищет запрос-число. */
        const val LEVEL_SPREAD = 2

        private val RANGE = Regex("""(\d{1,3})\s*[-–]\s*(\d{1,3})""")

        fun of(text: String): PlaceQuery {
            val folded = ItemSearch.fold(text)
            if (folded.isEmpty()) return Blank
            folded.toIntOrNull()?.let { return Levels(it - LEVEL_SPREAD..it + LEVEL_SPREAD) }
            RANGE.matchEntire(folded)?.destructured?.let { (from, to) -> return Levels(minOf(from.toInt(), to.toInt())..maxOf(from.toInt(), to.toInt())) }
            return Words(folded)
        }
    }
}

/** Места [this], что подходят под [query]: по уровню - только места с уровнем, по словам - любое слово места содержит запрос. */
fun <K> List<Place<K>>.matching(query: PlaceQuery): List<Place<K>> = when (query) {
    PlaceQuery.Blank -> emptyList()
    is PlaceQuery.Levels -> filter { place -> place.level?.let { it in query.range } == true }
    is PlaceQuery.Words -> filter { place -> place.words.any { it.contains(query.folded) } }
}

/** Места поиска на языке экрана: зоны кампании и узлы Атласа. */
object Places {
    /**
     * Открытые зоны кампании [unlocked] - неизведанных в поиске нет: имя, «Ур. · страж · убит · акт», слова - имя зоны и её стража.
     * Убит ли страж - по кампании героя [campaign] на миг [now] часов сервера.
     */
    fun zones(index: ContentIndex, unlocked: Collection<MapCode>, campaign: CampaignState?, now: Long): List<Place<MapCode>> = unlocked.mapNotNull { code ->
        val zone = index.zone(code) ?: return@mapNotNull null
        val title = mapTitle(code)
        val guardian = monsterTitle(zone.boss)
        val slain = campaign?.bossDown(code, now) == true
        val act = index.campaign.act(code)
        val facts = if (slain) ui("expedition.search_zone_slain", zone.level, guardian, act) else ui("expedition.search_zone", zone.level, guardian, act)
        Place(code, title, facts, zone.level, listOf(ItemSearch.fold(title), ItemSearch.fold(guardian)))
    }.sortedWith(compareBy({ it.level }, { it.title }))

    /** Узлы Атласа [nodes]: имя, ветвь и «взят» по взятым [taken]; слова - имя узла и ветви. */
    fun atlas(nodes: List<AtlasNode>, taken: Set<String>): List<Place<String>> = nodes.map { node ->
        val title = atlasNodeTitle(node.code)
        val group = atlasBranchTitle(AtlasFog.branch(node.code))
        Place(node.code, title, if (node.code in taken) ui("atlas.search_taken", group) else group, null, listOf(ItemSearch.fold(title), ItemSearch.fold(group)))
    }.sortedBy { it.title }
}
