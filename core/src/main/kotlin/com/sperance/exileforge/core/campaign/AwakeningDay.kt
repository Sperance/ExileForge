package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.rules.content.AwakenedWins
import com.sperance.exileforge.rules.content.AwakeningStanding
import com.sperance.exileforge.rules.content.Awakenings
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.roll.ItemInstance

/**
 * Пробуждения суток [day] глазами героя: пробуждение каждой зоны дня [marks] в порядке броска и смена [changeAt] (мс эпохи сервера).
 * Клиент считает их сам правилом [Awakenings] по часам сервера - тем же, что сервер на входе в заход.
 */
data class AwakeningDay(val day: Long, val changeAt: Long, val marks: Map<MapCode, AwakeningStanding>) {
    /** Пробуждение зоны [zone]; null - зона не из дня. */
    operator fun get(zone: MapCode): AwakeningStanding? = marks[zone]

    companion object {
        /** Пробуждения суток [day] по победам героя [wins]. */
        fun of(awakenings: Awakenings, day: Long, wins: AwakenedWins): AwakeningDay = AwakeningDay(day, awakenings.changeAt(day), awakenings.standings(day, wins))

        /** Карт тайника [stash], что будят стража ([Awakenings.wakes]), по зонам. */
        fun wakingMaps(awakenings: Awakenings, stash: List<ItemInstance>): Map<MapCode, Int> = stash.filter { it.mapZone.isNotEmpty() && awakenings.wakes(it.rarity) }.groupingBy { it.mapZone }.eachCount()
    }
}
