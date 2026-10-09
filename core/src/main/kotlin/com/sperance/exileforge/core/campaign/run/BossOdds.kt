package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.HeroModel
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.PhaseFoes
import com.sperance.exileforge.core.campaign.combat.Ally
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.HeroPools
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.FightKind
import com.sperance.exileforge.rules.content.FightRules
import com.sperance.exileforge.rules.roll.Dice
import kotlin.random.Random

/**
 * Почему герой проиграл бой прогона (4.2.0). Расширяемо: новая причина - новая реализация и правило в [LossCauses].
 */
sealed interface LossCause {
    /** Враги боя разъярились ко времени гибели героя (4.3.0: ярость - в любом бою). */
    data object Enrage : LossCause

    /** Бой не кончился за лимит прогона. */
    data object Timeout : LossCause

    /** Добил урон [type]. */
    data class Damage(val type: DamageType) : LossCause
}

/** Правила, что называют причину поражения по бою, по порядку: первое, что ответило, и есть причина. */
object LossCauses {
    private val rules: List<(Battle) -> LossCause?> = listOf(
        { battle -> LossCause.Timeout.takeIf { battle.outcome == null } },
        { battle -> LossCause.Enrage.takeIf { battle.outcome == Outcome.LOSS && battle.furious } },
        { battle -> battle.killingType()?.let(LossCause::Damage) },
    )

    /** Причина поражения [battle]; null - бой выигран. */
    fun of(battle: Battle): LossCause? = if (battle.outcome == Outcome.WIN) null else rules.firstNotNullOfOrNull { it(battle) }
}

/**
 * Исход прогона боёв со стражем (3.92.0; 4.2.0 - «Весы»): побед из [fights], средняя длина боя в секундах, средний остаток
 * здоровья героя в победах (доля, null - побед нет) и поражения по причинам [causes] (4.3.0).
 */
data class BossOdds(val wins: Int, val fights: Int, val seconds: Double, val lifeLeft: Double?, val causes: Map<LossCause, Int> = emptyMap()) {
    val share: Double get() = if (fights > 0) wins.toDouble() / fights else 0.0

    /** Самая частая причина поражения; null - поражений нет. */
    val cause: LossCause? get() = causes.maxByOrNull { it.value }?.key
}

/**
 * Неизменяемый снимок боя со стражем для прогона (4.2.0): герой, враги, запасы и питомец на миг снимка. Снимок делают там, где
 * живёт арена или поход, а [run] - тяжёлый, его зовут вне главного потока: он не читает живого состояния.
 */
class OddsPlan internal constructor(
    private val hero: Combatant,
    private val foes: List<Foe>,
    private val rules: CombatRules,
    private val fight: FightRules,
    private val pools: HeroPools,
    private val stance: HeroStance,
    private val kit: Loadout,
    private val model: HeroModel,
    private val percent: Set<String>,
    private val ally: Ally?,
    private val phases: PhaseFoes,
    /** Прошлый бой выигран: силы `FIGHT_CLEAR` открывают и бои прогона. */
    private val cleared: Boolean,
    /** Вид предсказанного боя (4.3.0): прогон катит его правила - своей ярости у «Весов» нет. */
    private val kind: FightKind,
) {
    /** [fights] боёв, каждый на своих костях, не дольше [cap] секунд (недоигранный - не победа). */
    fun run(fights: Int = FIGHTS, cap: Double = CAP): BossOdds {
        var wins = 0
        var seconds = 0.0
        var lifeLeft = 0.0
        val causes = mutableMapOf<LossCause, Int>()
        repeat(fights) { i ->
            val battle = Battle(
                hero, phases.withRetinue(foes, Dice(SEED + i)), rules, fight, pools.life, Random(SEED + i), stance,
                kit = kit, model = model, pools = pools, percent = percent, ally = ally, cleared = cleared, kind = kind,
            )
            while (battle.outcome == null && battle.time < cap) battle.advance(1.0)
            seconds += battle.time
            if (battle.outcome == Outcome.WIN) {
                wins++
                lifeLeft += battle.heroLife / battle.heroFighter.body.maxLife.coerceAtLeast(1.0)
            }
            LossCauses.of(battle)?.let { causes.merge(it, 1, Int::plus) }
        }
        return BossOdds(
            wins,
            fights,
            if (fights > 0) seconds / fights else 0.0,
            if (wins > 0) lifeLeft / wins else null,
            causes,
        )
    }

    private companion object {
        const val FIGHTS = 40
        const val CAP = 180.0
        const val SEED = 9_173L
    }
}

/** Тип урона, что добил героя: последний удар или тик врага по нему; null - не от врага. */
private fun Battle.killingType(): DamageType? = events.lastOrNull { it.actor == Side.MONSTER && it.damage > 0 && !it.atPet }?.let { it.type ?: it.ailment?.let { ailment -> ruleOf[ailment]?.second } }
