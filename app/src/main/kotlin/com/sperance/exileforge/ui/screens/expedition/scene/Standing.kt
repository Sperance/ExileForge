package com.sperance.exileforge.ui.screens.expedition.scene

/**
 * Очередь стоящего на карте по глубине (скала, ориентиры, объекты, жетоны): запись - глубина, вид, ссылка и число, без лямбд и
 * пар на кадр. Массивы растут один раз и живут со сценой; порядок - устойчивая вставка: при равной глубине первым рисуется
 * поставленный раньше, как у `sortBy`.
 */
internal class Standing {
    var size = 0
        private set
    private var depths = DoubleArray(CAPACITY)
    private var kinds = IntArray(CAPACITY)
    private var parts = IntArray(CAPACITY)
    private var refs = arrayOfNulls<Any>(CAPACITY)

    /** Порядок рисования: номера записей по глубине. */
    private var order = IntArray(CAPACITY)

    fun add(depth: Double, kind: Int, ref: Any? = null, part: Int = 0) {
        if (size == depths.size) grow()
        depths[size] = depth
        kinds[size] = kind
        parts[size] = part
        refs[size] = ref
        order[size] = size
        size++
    }

    /** Порядок по глубине вставкой: очередь почти упорядочена (скала идёт по глубине), так что это почти линейный проход. */
    fun sort() {
        for (i in 1 until size) {
            val at = order[i]
            val depth = depths[at]
            var j = i - 1
            while (j >= 0 && depths[order[j]] > depth) {
                order[j + 1] = order[j]
                j--
            }
            order[j + 1] = at
        }
    }

    fun kind(i: Int): Int = kinds[order[i]]
    fun part(i: Int): Int = parts[order[i]]
    fun ref(i: Int): Any? = refs[order[i]]

    fun clear() {
        java.util.Arrays.fill(refs, 0, size, null)
        size = 0
    }

    private fun grow() {
        val next = depths.size * 2
        depths = depths.copyOf(next)
        kinds = kinds.copyOf(next)
        parts = parts.copyOf(next)
        refs = refs.copyOf(next)
        order = order.copyOf(next)
    }

    companion object {
        private const val CAPACITY = 256

        /** Кусок скалы - слой [MapLayers] номер `part`. */
        const val WALLS = 0
        const val EXIT = 1
        const val VAAL_PORTAL = 2
        const val FOUNTAIN = 3
        const val CHEST = 4
        const val CRYSTAL = 5
        const val CRACK = 6

        /** Объект карты: часть `part` ([standFeature]). */
        const val FEATURE = 7
        const val AGENT = 8
        const val HERO = 9
    }
}
