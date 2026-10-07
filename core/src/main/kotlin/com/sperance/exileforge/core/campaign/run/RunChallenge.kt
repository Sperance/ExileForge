package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.PhaseFoes
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.rules.roll.Dice
import kotlin.random.Random

/**
 * Экран-вызов (3.92.0, макет B «Досье»): страж зоны или Ваал-зоны встречен вручную - мир ждёт слова игрока «В бой». Автозабег
 * идёт в бой сразу, без экрана.
 */
internal fun ExpeditionRun.challenge(agent: MonsterAgent) {
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
        phases.foe(monster, monster.level.takeIf { it > 0 } ?: zone.level)
    }
}

/** Вид вызова для экрана: босс, его уровень и здоровье, шаблон фаз и пороги. */
internal fun ExpeditionRun.challengeView(): ChallengeView? {
    val agent = challenge ?: return null
    val boss = challengeFoes(agent).maxByOrNull { it.rarity.ordinal } ?: return null
    val origin = boss.origin ?: return null
    return ChallengeView(origin, boss.level, boss.body.maxLife, boss.phase, boss.phases.map { it.step.at }, vaal)
}

/** Исход прогона боёв со стражем (3.92.0): побед из [fights] и средняя длина боя в секундах. */
data class BossOdds(val wins: Int, val fights: Int, val seconds: Double) {
    val share: Double get() = if (fights > 0) wins.toDouble() / fights else 0.0
}

/**
 * Прогон (3.92.0): [fights] боёв героем, каков он сейчас, - листом, снаряжением, запасами и питомцем - против стража вызова,
 * каждый на своих костях, не дольше [cap] секунд (недоигранный - не победа). Тяжёлый: звать вне главного потока.
 */
fun ExpeditionRun.bossOdds(fights: Int = ODDS_FIGHTS, cap: Double = ODDS_CAP): BossOdds? {
    val agent = challenge ?: return null
    val phases = PhaseFoes(index, rules)
    val foes = challengeFoes(agent)
    val ally = allies.of(hero.stats, pet())
    var wins = 0
    var seconds = 0.0
    repeat(fights) { i ->
        val battle = Battle(
            hero, phases.withRetinue(foes, Dice(ODDS_SEED + i)), rules, life, Random(ODDS_SEED + i), stance,
            kit = kit, model = build, pools = pools, percent = build.gear.percent, ally = ally,
        )
        while (battle.outcome == null && battle.time < cap) battle.advance(1.0)
        if (battle.outcome == Outcome.WIN) wins++
        seconds += battle.time
    }
    return BossOdds(wins, fights, seconds / fights)
}

private const val ODDS_FIGHTS = 40
private const val ODDS_CAP = 180.0
private const val ODDS_SEED = 9_173L
