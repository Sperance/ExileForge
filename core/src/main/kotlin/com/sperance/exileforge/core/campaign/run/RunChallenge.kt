package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.PhaseFoes
import com.sperance.exileforge.core.campaign.combat.Foe

/**
 * Экран-вызов («Досье»): страж зоны или Ваал-зоны встречен вручную - мир ждёт слова игрока «В бой». Автозабег
 * идёт в бой сразу, без экрана. Вызов прерывает идущий сбор узла ([breakGather]), как начало боя.
 */
internal fun ExpeditionRun.challenge(agent: MonsterAgent) {
    breakGather()
    challenge = agent
    phase = RunPhase.CHALLENGE
}

/** «В бой» с экрана-вызова. */
internal fun ExpeditionRun.accept() {
    val agent = challenge?.takeIf { phase == RunPhase.CHALLENGE } ?: return
    challenge = null
    engage(agent)
}

/** Враги вызова, как их поставит бой: сделки алтаря на них, фазы босса, без свиты. */
internal fun ExpeditionRun.challengeFoes(agent: MonsterAgent): List<Foe> {
    val phases = PhaseFoes(index, rules)
    return agent.standing.map { i ->
        val monster = pactFoe(FightMember(agent, i))
        phases.foe(monster, monster.level.takeIf { it > 0 } ?: run.levelOf(zone))
    }
}

/** Вид вызова для экрана: босс, его уровень и здоровье, шаблон фаз и пороги. */
internal fun ExpeditionRun.challengeView(): ChallengeView? {
    val agent = challenge ?: return null
    val boss = challengeFoes(agent).maxByOrNull { it.rarity.ordinal } ?: return null
    val origin = boss.origin ?: return null
    return ChallengeView(origin, boss.level, boss.body.maxLife, boss.phase, boss.phases.map { it.step.at }, vaal)
}
