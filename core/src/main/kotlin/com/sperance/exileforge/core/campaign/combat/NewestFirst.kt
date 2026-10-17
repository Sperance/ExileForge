package com.sperance.exileforge.core.campaign.combat

/**
 * Журнал боя новыми сверху без копии: первые [size] строк [log], перевёрнутые. Журнал боя только дописывается, так
 * что окно неизменно, сколько бы строк ни пришло после. Равенство окон одного журнала - за O(1): экран боя сверяет снимок
 * каждый кадр, и полный обход журнала ради этого не нужен.
 */
internal class NewestFirst(private val log: List<CombatEvent>, override val size: Int) :
    AbstractList<CombatEvent>(),
    RandomAccess {
    override fun get(index: Int): CombatEvent {
        if (index !in 0 until size) throw IndexOutOfBoundsException("index $index, size $size")
        return log[size - 1 - index]
    }

    override fun equals(other: Any?): Boolean = other === this || (other is NewestFirst && other.log === log && other.size == size) || super.equals(other)

    override fun hashCode(): Int = super.hashCode()
}
