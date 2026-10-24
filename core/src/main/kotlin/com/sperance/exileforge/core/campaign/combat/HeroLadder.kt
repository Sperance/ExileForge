package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.HeroModel
import com.sperance.exileforge.core.character.StatLine

/**
 * Часть тела героя в бою - в порядке разбора характеристики: эффекты на герое (каждый своей ступенью), заряды, силы вещей, условия
 * боя, низкое здоровье, последними - ауры врагов.
 */
enum class FightPart { EFFECT, CHARGES, POWERS, CONDITIONS, LOW_LIFE, AURAS }

/**
 * Ступень тела героя в бою: часть [part] и строки, что она кладёт ([lines]); у эффекта - его плитка [effect] без срока (ступень не
 * меняется, пока идёт время), у прочих - null.
 */
data class FightRung(val part: FightPart, val lines: List<StatLine>, val effect: EffectView? = null)

/**
 * Тело героя в бою по частям ([Battle.heroLadder]): [rungs] - строки на герое в порядке свода тела боя, [auras] - ауры врагов поверх.
 * Ступени собираются ([start], [bodies]) без памяти тел боя ([HeroModel.freshBody]) - вне потока боя и только по запросу: снимок боя
 * несёт лишь строки. Равные снимки (те же строки, ауры и герой) - равные лестницы: по равенству читатель решает, собирать ли заново.
 */
data class HeroLadder internal constructor(private val model: HeroModel, val rungs: List<FightRung>, val auras: Map<String, Double>) {
    /** Тело до строк боя: лист захода с пассивными умениями. */
    fun start(): Combatant = model.freshBody(emptyList())

    /**
     * Тело после каждой ступени - в порядке разбора ([FightPart]): эффекты по одному, затем прочие части, последней - ауры врагов (без
     * аур её нет). Строки взятых ступеней ложатся в порядке свода тела боя, так что тело последней ступени - тело боя.
     */
    fun bodies(): List<Pair<FightRung, Combatant>> {
        val taken = BooleanArray(rungs.size)
        val steps = rungs.indices.sortedBy { rungs[it].part }.map { i ->
            taken[i] = true
            rungs[i] to model.freshBody(rungs.filterIndexed { j, _ -> taken[j] }.flatMap { it.lines })
        }
        if (auras.isEmpty()) return steps
        return steps + (FightRung(FightPart.AURAS, emptyList()) to (steps.lastOrNull()?.second ?: start()).under(auras))
    }
}
