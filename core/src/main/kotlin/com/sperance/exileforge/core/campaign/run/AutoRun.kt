package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.rules.content.ExpeditionRules
import com.sperance.exileforge.rules.roll.Streams

/**
 * Автопроход зоны (3.94.0 - «лента боёв»): без выбора и без карты на экране. Берёт бои и сундуки; алтари кристаллов,
 * трещины Бездны, портал Ваал, фонтаны и торговец пропускаются.
 */
data object AutoPlan

/**
 * Где автопроход, для ленты: волна из скольких, сколько сундуков открыто, завершается ли он после идущего боя (3.95.2),
 * стоит ли без связи с сервером [offline] (4.0.0) и отдыхает ли герой перед волной [resting] (MOB-BAL, В-15).
 */
data class AutoHud(val wave: Int, val waves: Int, val chests: Int = 0, val finishing: Boolean = false, val offline: Boolean = false, val resting: Boolean = false)

/** Один шаг автопрохода, между боями. */
sealed interface AutoStep {
    /** Началась новая волна: двигается только счёт. */
    data class Wave(val number: Int) : AutoStep
    data class Fight(val agent: MonsterAgent) : AutoStep
    data class OpenChest(val chest: Chest) : AutoStep
    data object Boss : AutoStep
    data object Exit : AutoStep

    /** «Завершить» (3.95.2): заход кончается здесь же, как уходом порталом, - с тем, что уже собрано. */
    data object Leave : AutoStep
}

/**
 * Автопроход зоны (3.2.0, лента боёв с 3.94.0): стаи самой зоны - те же токены, что её сид поставил на карту, чтобы журнал и
 * добыча были как при ходьбе, - бьются от 8 до 15 волнами одна за другой, между волнами открывается сундук; страж последним,
 * если стоит, и выход. Перед каждой волной, кроме первой, и перед стражем герой отдыхает, как в ручном заходе (MOB-BAL, В-15:
 * `expedition.autoRest`): пьёт фляги здоровья, чьё лечение не пропадёт, и стоит вне боя на скорости здоровья карты. Здесь ничего не
 * катит добычу и не говорит с сервером: каждый шаг - то, что заход и так делает, когда герой к нему подходит.
 */
class AutoPilot(private val steps: ArrayDeque<AutoStep>, val waves: Int) {
    var wave = 0
        private set

    /** Сколько сундуков открыто по пути. */
    var chests = 0
        internal set

    /** Секунды до следующего шага: пауза между боями, чтобы волна читалась целиком. */
    var rest = 0.0

    /** Игрок завершил автопроход (3.95.2): идущий бой доигрывается, следующим шагом - уход. */
    var finishing = false
        private set

    /** Секунды идущего отдыха (MOB-BAL, В-15); null - герой не отдыхает. */
    var resting: Double? = null

    /** Шаг, перед которым отдых уже был: после отдыха он берётся сразу. */
    private var rested: AutoStep? = null

    fun next(): AutoStep? = if (finishing) AutoStep.Leave else steps.removeFirstOrNull()?.also { if (it is AutoStep.Wave) wave = it.number }

    /**
     * Шаг, перед которым герою пора отдохнуть: новая волна (кроме первой) или страж, что встанет на бой ([guarded]), - если отдыха
     * перед ним ещё не было. Отдых этим и начинается: второй раз тот же шаг его не просит.
     */
    fun restBefore(guarded: Boolean): Boolean {
        val step = steps.firstOrNull()?.takeIf { !finishing && it != rested } ?: return false
        val due = step is AutoStep.Wave && step.number > 1 || step == AutoStep.Boss && guarded
        if (due) rested = step
        return due
    }

    fun finish() {
        finishing = true
    }

    companion object {
        /** Шаги автопрохода по [world], число волн - из сида захода [seed]. */
        fun of(rules: ExpeditionRules, world: ExpeditionWorld, seed: Long): AutoPilot {
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
            // Страж - шагом всегда (3.95.2): отдыхающий может вернуться по ходу захода; кого на посту нет, шаг пропускает
            steps += AutoStep.Boss
            steps += AutoStep.Exit
            return AutoPilot(steps, waves)
        }
    }
}
