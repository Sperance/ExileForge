package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.rules.content.ExpeditionRules
import com.sperance.exileforge.rules.roll.Streams

/**
 * Автопроход зоны (3.94.0 - «лента боёв»): без выбора и без карты на экране. Берёт бои и сундуки; алтари кристаллов,
 * трещины Бездны, портал Ваал, фонтаны и торговец пропускаются.
 */
data object AutoPlan

/** Где автопроход, для ленты: волна из скольких и сколько сундуков открыто. */
data class AutoHud(val wave: Int, val waves: Int, val chests: Int = 0)

/** Один шаг автопрохода, между боями. */
sealed interface AutoStep {
    /** Началась новая волна: двигается только счёт. */
    data class Wave(val number: Int) : AutoStep
    data class Fight(val agent: MonsterAgent) : AutoStep
    data class OpenChest(val chest: Chest) : AutoStep
    data object Boss : AutoStep
    data object Exit : AutoStep
}

/**
 * Автопроход зоны (3.2.0, лента боёв с 3.94.0): стаи самой зоны - те же токены, что её сид поставил на карту, чтобы журнал и
 * добыча были как при ходьбе, - бьются от 8 до 15 волнами одна за другой, между волнами открывается сундук; страж последним,
 * если стоит, и выход. Лечения между боями нет. Здесь ничего не катит добычу и не говорит с сервером: каждый шаг - то, что
 * заход и так делает, когда герой к нему подходит.
 */
class AutoPilot(private val steps: ArrayDeque<AutoStep>, val waves: Int) {
    var wave = 0
        private set

    /** Сколько сундуков открыто по пути. */
    var chests = 0
        internal set

    /** Секунды до следующего шага: пауза между боями, чтобы волна читалась целиком. */
    var rest = 0.0

    fun next(): AutoStep? = steps.removeFirstOrNull()?.also { if (it is AutoStep.Wave) wave = it.number }

    companion object {
        /** Шаги автопрохода по [world], число волн - из сида захода [seed]. */
        fun of(rules: ExpeditionRules, world: ExpeditionWorld, seed: Long, bossStands: Boolean): AutoPilot {
            val packs = world.agents.filter { it !== world.boss && it.alive }.sortedBy { it.id }
            val waves = Streams(seed).of("autoWaves").between(rules.autoWavesMin, rules.autoWavesMax).coerceIn(1, packs.size.coerceAtLeast(1))
            val chests = ArrayDeque(world.chests.filterNot { it.opened })
            val steps = ArrayDeque<AutoStep>()
            val size = packs.size / waves
            val extra = packs.size % waves
            var from = 0
            (1..waves).forEach { number ->
                val take = size + if (number <= extra) 1 else 0
                steps += AutoStep.Wave(number)
                packs.subList(from, (from + take).coerceAtMost(packs.size)).forEach { steps += AutoStep.Fight(it) }
                from += take
                // Сундуки - между волнами, по всему пути, а не грудой в конце
                chests.removeFirstOrNull()?.let { steps += AutoStep.OpenChest(it) }
            }
            chests.forEach { steps += AutoStep.OpenChest(it) }
            if (bossStands) steps += AutoStep.Boss
            steps += AutoStep.Exit
            return AutoPilot(steps, waves)
        }
    }
}
