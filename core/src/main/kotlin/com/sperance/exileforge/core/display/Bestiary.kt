package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.Stat

/** Зона кампании в Бестиарии: её монстры по порядку контента и босс зоны. */
data class BestiaryZone(val code: MapCode, val level: Int, val monsters: List<MonsterCode>, val boss: MonsterCode) {
    /** Все виды зоны: монстры, затем босс. */
    val species: List<MonsterCode> = (monsters + boss).distinct()
}

/** Глава кампании (регион) со своими зонами по порядку кампании. */
data class BestiaryChapter(val region: String, val zones: List<BestiaryZone>)

/**
 * Бестиарий героя по контенту [index]: главы кампании с зонами, где каждый вид встречается, и «Иное» - монстры контента вне зон
 * кампании (стражи порчи, испытания, Разлом). Строится раз на контент; счёт героя - [BestiaryTally].
 */
class Bestiary(index: ContentIndex) {
    val chapters: List<BestiaryChapter> = index.content.campaign.regions.map { region ->
        BestiaryChapter(region.code, region.zones.map { BestiaryZone(it.code, it.level, it.monsters.distinct(), it.boss) })
    }

    private val zoned: Map<MonsterCode, List<MapCode>> = chapters.flatMap { it.zones }.flatMap { zone -> zone.species.map { it to zone.code } }
        .groupBy({ it.first }, { it.second })

    /** Виды вне зон кампании - по порядку контента. */
    val other: List<MonsterCode> = index.content.campaign.monsters.map { it.code }.filter { it !in zoned }

    /** Все виды Бестиария без повторов. */
    val all: List<MonsterCode> = zoned.keys.toList() + other

    private val bosses: Set<MonsterCode> = index.content.campaign.monsters.filter { it.boss }.mapTo(HashSet()) { it.code }

    /** Зоны, где встречается вид [code]; вне кампании - пусто. */
    fun zones(code: MonsterCode): List<MapCode> = zoned[code].orEmpty()

    /** Вид - босс. */
    fun boss(code: MonsterCode): Boolean = code in bosses

    /** Глава вида [code] - первая, где он встречается; вне кампании - null. */
    fun chapter(code: MonsterCode): BestiaryChapter? = chapters.firstOrNull { chapter -> chapter.zones.any { code in it.species } }

    companion object {
        /** Ступени вида по числу убийств: бронза, серебро, золото. */
        val STEPS: List<Long> = listOf(10L, 100L, 1_000L)

        /** Ступень вида при [kills] убийствах: 0 - ни одной, 1..3 - бронза, серебро, золото. */
        fun tier(kills: Long): Int = STEPS.count { kills >= it }
    }
}

/**
 * Счёт Бестиария героя по его статистике [stats] (`hero/stats`): убийства вида - группы `KILL` и `BOSS`, гибели от него - `KILLER`.
 * Вид открыт, когда убит хоть раз.
 */
class BestiaryTally(private val stats: Map<String, Long>) {
    fun kills(code: MonsterCode): Long = (stats[Stat.of(Stat.KILL, code.value)] ?: 0L) + (stats[Stat.of(Stat.BOSS, code.value)] ?: 0L)

    fun deaths(code: MonsterCode): Long = stats[Stat.of(Stat.KILLER, code.value)] ?: 0L

    fun open(code: MonsterCode): Boolean = kills(code) > 0

    /** Сколько видов из [codes] открыто. */
    fun opened(codes: Collection<MonsterCode>): Int = codes.count(::open)

    /** Сколько видов из [codes] на золотой ступени. */
    fun golden(codes: Collection<MonsterCode>): Int = codes.count { Bestiary.tier(kills(it)) == Bestiary.STEPS.size }

    /** Кто убивал героя: виды и число гибелей, чаще первыми; только известные [known]. */
    fun killers(known: Set<MonsterCode>): List<Pair<MonsterCode, Long>> = stats.mapNotNull { (key, value) ->
        if (value <= 0 || !key.startsWith(KILLER_PREFIX)) return@mapNotNull null
        MonsterCode(key.substring(KILLER_PREFIX.length)).takeIf { it in known }?.let { it to value }
    }.sortedByDescending { it.second }

    /** Всего гибелей героя от монстров. */
    val deaths: Long get() = stats.entries.sumOf { (key, value) -> if (key.startsWith(KILLER_PREFIX)) value else 0L }

    private companion object {
        val KILLER_PREFIX = Stat.of(Stat.KILLER, "")
    }
}
