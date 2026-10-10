package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.content.FateLever
import com.sperance.exileforge.rules.run.Run

// ==================== Предначертание в забеге (4.6.0) ====================

/**
 * Обзор Предначертания на карте ([FateLever.FOG_SIGHT]): туман отступает на [radius] клеток дальше, а [throughFog] - вожаки стай
 * и сундуки видны сквозь него. Без рычага - [NONE].
 */
data class FateSight(val radius: Double = 0.0, val throughFog: Boolean = false) {
    companion object {
        val NONE = FateSight()

        /** Обзор замороженного дара захода. */
        fun of(fate: FateEffects): FateSight = fate.of(FateLever.FOG_SIGHT)?.let { FateSight(it.value, throughFog = true) } ?: NONE
    }
}

/**
 * Стаи Следопыта (4.6.0, [FateLever.TRACKER_PACK]): на карте Атласа - [Run.trackers] стай с редким вожаком, жетоны `Run.TRACKER + k`,
 * пак - правилами (`Run.spawn`), место - одна из точек стай карты на потоке `tracker`. Встают до павших: убитые остаются лежать.
 */
internal fun ExpeditionRun.track() {
    val count = run.trackers
    if (count <= 0 || world.map.spawns.isEmpty()) return
    val buffs = MapEffects.buffs(baseEffects)
    repeat(count) { k ->
        val token = Run.TRACKER + k
        val cell = world.map.spawns[run.streams.of("tracker", k).nextInt(world.map.spawns.size)]
        world.summon(token, spawns.summoned(token, buffs), cell)
    }
}
