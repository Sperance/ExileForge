package com.sperance.exileforge.core.pick

/** Что можно отметить в выборе пачкой (продажа, вклад в хранилище гильдии): отметка держится по [id]. */
interface Pickable {
    val id: String
}

/** Быстрый набор выбора: какие лоты [T] он берёт. */
fun interface PickPreset<in T> {
    fun takes(lot: T): Boolean
}

/**
 * Выбор пачкой - отмеченные id поверх текущих лотов, общий для продажи и вклада в хранилище гильдии. Ушедшее из лотов
 * само выпадает из выбора: счёт читается только по живым лотам.
 */
data class PickSelection(val chosen: Set<String> = emptySet()) {
    fun <T : Pickable> picked(lots: List<T>): List<T> = lots.filter { it.id in chosen }

    fun toggle(id: String) = copy(chosen = if (id in chosen) chosen - id else chosen + id)

    /**
     * Отмечает [ids], которых нет в [seen], - пометки к продаже и отложенная продажа добычи захода, что пришли позже. Уже виденный id
     * не отмечается снова: снятая отметка не возвращается.
     */
    fun mark(ids: Collection<String>, seen: Set<String>) = copy(chosen = chosen + ids.filter { it !in seen })

    /** Набор включён, когда в нём есть лоты и все они отмечены. */
    fun <T : Pickable> covers(lots: List<T>, preset: PickPreset<T>): Boolean = lots.filter(preset::takes).let { set -> set.isNotEmpty() && set.all { it.id in chosen } }

    /** Включённый набор снимается целиком, иначе добавляется к выбору. */
    fun <T : Pickable> flip(lots: List<T>, preset: PickPreset<T>): PickSelection {
        val ids = lots.filter(preset::takes).mapTo(HashSet()) { it.id }
        return copy(chosen = if (covers(lots, preset)) chosen - ids else chosen + ids)
    }
}
