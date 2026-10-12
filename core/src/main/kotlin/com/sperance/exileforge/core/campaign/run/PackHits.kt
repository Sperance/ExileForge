package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.rules.roll.RolledMonster

/**
 * Журнал законченного боя по врагам (4.8.0): каждый вступивший враг - свой [PackHit] с его событиями; [monsters] - враги
 * боя по номеру, свита фаз - вслед за ними по своему облику. Враг, что так и не вышел на поле (бой кончился раньше его
 * очереди), в журнал не входит. [start] - начало боя от начала шага испытания.
 */
fun Battle.packHits(monsters: List<RolledMonster>, start: Double = 0.0): List<PackHit> = foes.indices.mapNotNull { i ->
    val monster = monsters.getOrNull(i) ?: foes[i].origin ?: return@mapNotNull null
    PackHit(monster, events.filter { it.foe == i }, duration, start).takeIf { foeFighters[i].engaged }
}

/** Отчёт законченного боя (4.8.0): вожак - сильнейший из [monsters], журнал - [packHits]. Null - бой не кончился или пуст. */
fun Battle.report(monsters: List<RolledMonster>): FightReport? {
    val outcome = outcome ?: return null
    val leader = monsters.maxByOrNull { it.rarity.ordinal } ?: return null
    return FightReport(leader, outcome, packHits(monsters), duration)
}
