package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.rules.content.MonsterRarity

/**
 * The field of a fight: at most [size] foes of a pack fight the hero at once, whatever the fight — a map's pack, a boss
 * with its adds, a wave of the Abyss, a floor of the trials. The rest wait in line and step in one by one, each into the
 * place of one that fell. A place keeps its fallen until the next steps in, so the field never shows more than [size].
 *
 * It holds indices into the pack only: who fights and how is the [Battle]'s.
 */
class FoeWindow(order: List<Int>, val size: Int = SIZE) {
    init {
        require(size > 0) { "a field holds at least one foe" }
    }

    private val places: MutableList<Int> = order.take(size).toMutableList()
    private val line: ArrayDeque<Int> = ArrayDeque(order.drop(size))

    /** Who stands in each place, fallen or not, by place. */
    val field: List<Int> get() = places

    /** How many still wait their turn. */
    val waiting: Int get() = line.size

    /** The place [index] stands in, or -1 while it waits (or once another took its place). */
    fun place(index: Int): Int = places.indexOf(index)

    fun waits(index: Int): Boolean = index in line

    /**
     * Зов посреди боя (3.92.0): свободное место - у стаи меньше поля они есть - берёт [index] сразу (`true`), иначе он ждёт
     * первым в очереди.
     */
    fun call(index: Int): Boolean {
        if (places.size < size) {
            places += index
            return true
        }
        line.addFirst(index)
        return false
    }

    /** Ждущий [index] уходит из очереди (3.92.0): свита пала вместе с боссом и уже не встанет. */
    fun dismiss(index: Int) {
        line.remove(index)
    }

    /** Every place whose foe is [down] takes the next in line; the ones that stepped in, in the order they did. */
    fun refill(down: (Int) -> Boolean): List<Int> {
        if (line.isEmpty()) return emptyList()
        val entered = mutableListOf<Int>()
        places.indices.forEach { place ->
            if (line.isNotEmpty() && down(places[place])) places[place] = line.removeFirst().also(entered::add)
        }
        return entered
    }

    companion object {
        /** How many foes fight at once. */
        const val SIZE = 3

        /** The order a pack steps in: the strongest first — a boss, the pack's leader — then the rest as the pack stands. */
        fun order(rarities: List<MonsterRarity>): List<Int> = rarities.indices.sortedByDescending { rarities[it] }
    }
}
