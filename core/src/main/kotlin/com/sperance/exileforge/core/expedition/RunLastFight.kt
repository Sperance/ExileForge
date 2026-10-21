package com.sperance.exileforge.core.expedition

import com.sperance.exileforge.core.campaign.run.FightReport
import com.sperance.exileforge.core.feedback.LastFight

/** Последний бой забега на экране: похода, испытания или открытого Разлома - что идёт, тот и спрошен; забега нет - null. */
class RunLastFight(private val expedition: ExpeditionActions, private val trials: TrialActions, private val rift: RiftActions) : LastFight {
    override fun report(): FightReport? = expedition.run.value?.hud?.value?.lastFight
        ?: trials.arena.value?.hud?.value?.report
        ?: rift.state.value.takeIf { it.open }?.let { it.arena?.report ?: it.lastFight }
}
