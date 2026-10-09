package com.sperance.exileforge.core.campaign.run

/**
 * Раунды собранного боя (4.4.1, решение владельца): стаи по порядку, раунд берёт стаи целиком, пока врагов в нём не больше
 * [most] (`fight.maxFoes`); стая не делится, а не влезшая открывает следующий раунд: 2+2+3 → [2] [2] [3], 1+2 → [1+2].
 */
internal object FightRounds {
    fun <T> of(packs: List<T>, most: Int, size: (T) -> Int): List<List<T>> = packs.fold(mutableListOf<MutableList<T>>()) { rounds, pack ->
        val last = rounds.lastOrNull()
        if (last != null && last.sumOf(size) + size(pack) <= most) last += pack else rounds += mutableListOf(pack)
        rounds
    }
}
