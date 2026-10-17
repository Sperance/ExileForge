package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.rules.content.FightQueue
import com.sperance.exileforge.rules.content.FightRules

/**
 * Как враги боя выходят на поле: раундами собранных стай ([Rounds]) или подкреплением по очереди ([Reinforcements]).
 * Свита (`summonOf`) встаёт только по зову фаз и в выходе не участвует. Выход детерминирован: он зависит лишь от состава
 * боя и того, сколько врагов живо, - тот же сид даёт тот же бой.
 */
sealed interface Muster {
    /** Выход для врагов [foes] боя по правилу [fight]: свежее состояние на один бой. */
    fun field(foes: List<Foe>, fight: FightRules): Field

    /**
     * Раунды: стаи, что бой собрал вокруг вступившей, выходят по [Foe.round]; следующий раунд встаёт, едва пал
     * последний враг на поле, и начинает ярость заново.
     */
    data object Rounds : Muster {
        override fun field(foes: List<Foe>, fight: FightRules): Field = RoundField(foes)
    }

    /**
     * Подкрепления (узел Разлома, этаж башни): все враги боя по порядку - одна очередь [FightQueue] правил боя, на
     * поле не больше `fight.maxFoes`; павшего сразу сменяет следующий, на его место. Каждая смена поля
     * ([FightQueue.waves]) начинает ярость заново.
     */
    data object Reinforcements : Muster {
        override fun field(foes: List<Foe>, fight: FightRules): Field = QueueField(foes, fight)
    }

    /** Состояние выхода одного боя. */
    sealed interface Field {
        /** Идущая смена поля, с 0, из [rounds]. */
        val round: Int
        val rounds: Int

        /** Первые на поле. */
        fun opening(): List<Int>

        /** Враг [index] ещё ждёт выхода: не на поле, цел и не свита. */
        fun waits(index: Int): Boolean

        /** Кто вступает, когда на поле [alive] живых врагов; null - никто. */
        fun join(alive: Int): Join?
    }

    /** Вступающие [entering]; [fresh] - новый раунд: павшие уходят с поля; [rage] - ярость начинается заново. */
    data class Join(val entering: List<Int>, val fresh: Boolean, val rage: Boolean)
}

/** Враги, что встают сами (не свита), сильнейший первым. */
private fun List<Foe>.strongestFirst(indices: List<Int>): List<Int> = indices.sortedByDescending { this[it].rarity }

private class RoundField(private val foes: List<Foe>) : Muster.Field {
    override val rounds: Int = (foes.maxOfOrNull { it.round } ?: 0) + 1
    override var round = 0
        private set

    private fun of(round: Int) = foes.strongestFirst(foes.indices.filter { !foes[it].summoned && foes[it].round == round })

    override fun opening(): List<Int> = of(0)

    override fun waits(index: Int): Boolean = !foes[index].summoned && foes[index].round > round

    override fun join(alive: Int): Muster.Join? {
        if (alive > 0) return null
        while (round + 1 < rounds) {
            round++
            val entering = of(round)
            if (entering.isNotEmpty()) return Muster.Join(entering, fresh = true, rage = true)
        }
        return null
    }
}

private class QueueField(private val foes: List<Foe>, fight: FightRules) : Muster.Field {
    private var queue: FightQueue<Int> = fight.queue(foes.indices.filter { !foes[it].summoned })
    private val maxFoes = fight.maxFoes
    override val rounds: Int = queue.waves
    override val round: Int get() = ((queue.entered - 1).coerceAtLeast(0) / maxFoes).coerceAtMost(rounds - 1)

    override fun opening(): List<Int> = take(queue.opening())

    override fun waits(index: Int): Boolean = !foes[index].summoned && queue.foes.indexOf(index) >= queue.entered

    override fun join(alive: Int): Muster.Join? {
        if (queue.drained) return null
        val was = round
        val entering = take(queue.join(alive)).ifEmpty { return null }
        return Muster.Join(entering, fresh = false, rage = round > was)
    }

    private fun take(join: FightQueue.Join<Int>): List<Int> {
        queue = join.queue
        return foes.strongestFirst(join.joining)
    }
}
