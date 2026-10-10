package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.rules.content.FateLever
import com.sperance.exileforge.rules.run.Run

// ==================== Предначертание в забеге (4.6.0) ====================

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
