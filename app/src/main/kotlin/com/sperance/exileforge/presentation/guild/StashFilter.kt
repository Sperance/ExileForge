package com.sperance.exileforge.presentation.guild

import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.itemTitle

/** Что лежит в записи хранилища: копия вещи или стопка сумки. */
enum class StashKind { ITEM, STACK }

/** Поле отбора хранилища: каждое заданное - чип с крестиком. */
enum class StashField { KIND, SLOT, RARITY, MIN_LEVEL, MAX_LEVEL }

/**
 * Отбор хранилища гильдии: поля отбора аукциона, но считается на клиенте - вкладка целиком уже пришла с
 * `GET /guild/stash`, лишних запросов к серверу нет. Пустое поле - «любое».
 */
data class StashFilter(
    val title: String = "",
    val kind: StashKind? = null,
    val slot: String = "",
    val rarity: String = "",
    val minLevel: String = "",
    val maxLevel: String = "",
) {
    /** Заданные поля, без имени - оно в строке поиска. */
    fun active(): List<StashField> = buildList {
        if (kind != null) add(StashField.KIND)
        if (slot.isNotBlank()) add(StashField.SLOT)
        if (rarity.isNotBlank()) add(StashField.RARITY)
        if (minLevel.isNotBlank()) add(StashField.MIN_LEVEL)
        if (maxLevel.isNotBlank()) add(StashField.MAX_LEVEL)
    }

    fun without(field: StashField): StashFilter = when (field) {
        StashField.KIND -> copy(kind = null)
        StashField.SLOT -> copy(slot = "")
        StashField.RARITY -> copy(rarity = "")
        StashField.MIN_LEVEL -> copy(minLevel = "")
        StashField.MAX_LEVEL -> copy(maxLevel = "")
    }

    /** Всё сброшено, кроме набранного имени. */
    fun cleared(): StashFilter = StashFilter(title = title)

    /**
     * Подходит ли запись: копия [view] - по имени, месту, редкости и уровню вещи; стопка [code] - по имени и виду, поля вещи
     * её отсеивают.
     */
    fun matches(view: ItemView?, code: String): Boolean {
        val name = view?.title ?: itemTitle(code)
        if (title.isNotBlank() && !name.contains(title.trim(), ignoreCase = true)) return false
        if (kind != null && kind != (if (view != null) StashKind.ITEM else StashKind.STACK)) return false
        val itemOnly = slot.isNotBlank() || rarity.isNotBlank() || minLevel.isNotBlank() || maxLevel.isNotBlank()
        if (view == null) return !itemOnly
        if (slot.isNotBlank() && view.slot.name != slot) return false
        if (rarity.isNotBlank() && view.rarity.name != rarity) return false
        minLevel.toIntOrNull()?.let { if (view.level < it) return false }
        maxLevel.toIntOrNull()?.let { if (view.level > it) return false }
        return true
    }
}
