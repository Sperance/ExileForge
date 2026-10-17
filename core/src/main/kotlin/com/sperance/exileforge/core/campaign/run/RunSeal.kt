package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.rules.run.Run

/**
 * Печать стража захода: пало [killed] редких из [need] нужных, всего на карте [total]. Пока она не снята, босс
 * не встаёт - сервер не примет его убийство.
 */
data class SealView(val killed: Int, val need: Int, val total: Int) {
    val open: Boolean get() = killed >= need
}

/**
 * Ворота арены кольца: зачищено [cleared] стай из [need] нужных. Пока они закрыты, проход к арене - скала и
 * босс не встаёт: сервер не примет его раньше (`Run.bossOpen`).
 */
data class GateView(val cleared: Int, val need: Int) {
    val open: Boolean get() = cleared >= need
}

/** Павшие члены стай карты ключами `жетон × PACK_SLOTS + член`, как их считает сервер. */
private val ExpeditionRun.fallenKeys: List<Int>
    get() = world.agents.filter { it !== world.boss && it.crystal == null }.flatMap { agent -> agent.fallen.map { agent.id * Run.PACK_SLOTS + it } }

/** Печать стража этого захода; null - её нет (обычная карта, Ваал-зона). */
val ExpeditionRun.seal: SealView?
    get() {
        if (vaal || run.seal <= 0) return null
        return SealView(run.raresKilled(fallenKeys), run.sealNeed, run.rareTotal)
    }

/** Ворота арены этого захода; null - их нет (не кольцо, Ваал-зона). */
val ExpeditionRun.arenaGate: GateView?
    get() {
        if (vaal || run.gate <= 0) return null
        return GateView(run.packsCleared(fallenKeys), run.gateNeed)
    }

/** Босс ещё запечатан: печать стража или ворота арены. */
val ExpeditionRun.sealed: Boolean get() = seal?.open == false || arenaGate?.open == false

/** Ворота открылись - проход к арене становится полом (раз за заход; при возврате в заход - на первом шаге). */
internal fun ExpeditionRun.openArena() {
    if (vaal || world.map.gate.isEmpty() || arenaGate?.open == false) return
    world.map.openGate()
}
