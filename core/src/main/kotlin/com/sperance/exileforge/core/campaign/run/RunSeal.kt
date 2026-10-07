package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.rules.run.Run

/**
 * Печать стража захода (3.93.0): пало [killed] редких из [need] нужных, всего на карте [total]. Пока она не снята, босс
 * не встаёт - сервер не примет его убийство.
 */
data class SealView(val killed: Int, val need: Int, val total: Int) {
    val open: Boolean get() = killed >= need
}

/** Печать стража этого захода; null - её нет (обычная карта, Ваал-зона). */
val ExpeditionRun.seal: SealView?
    get() {
        if (vaal || run.seal <= 0) return null
        val keys = world.agents.filter { it !== world.boss && it.crystal == null }.flatMap { agent -> agent.fallen.map { agent.id * Run.PACK_SLOTS + it } }
        return SealView(run.raresKilled(keys), run.sealNeed, run.rareTotal)
    }

/** Босс ещё запечатан. */
val ExpeditionRun.sealed: Boolean get() = seal?.open == false
