package com.sperance.exileforge.core.guild

import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.filter.LevelRange
import com.sperance.exileforge.core.filter.Narrowing
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui

/** Что лежит в записи хранилища: копия вещи или стопка сумки. */
enum class StashKind {
    ITEM,
    STACK,
    ;

    val title: String get() = ui("guild.stash_kind.$name")
}

/** Поле отбора хранилища: каждое заданное - чип с крестиком. */
enum class StashField { KIND, SLOT, RARITY, LEVEL }

/**
 * Отбор хранилища гильдии: поля отбора аукциона, но считается на клиенте - вкладка целиком уже пришла с
 * `GET /guild/stash`, лишних запросов к серверу нет. Пустое поле - «любое»; уровень - та же вилка, что у фильтра предметов ([LevelRange]).
 */
data class StashFilter(
    val title: String = "",
    val kind: StashKind? = null,
    val slot: String = "",
    val rarity: String = "",
    val level: LevelRange = LevelRange(),
) {
    /** Заданные поля, без имени - оно в строке поиска. */
    fun active(): List<StashField> = buildList {
        if (kind != null) add(StashField.KIND)
        if (slot.isNotBlank()) add(StashField.SLOT)
        if (rarity.isNotBlank()) add(StashField.RARITY)
        if (!level.isAny) add(StashField.LEVEL)
    }

    fun without(field: StashField): StashFilter = when (field) {
        StashField.KIND -> copy(kind = null)
        StashField.SLOT -> copy(slot = "")
        StashField.RARITY -> copy(rarity = "")
        StashField.LEVEL -> copy(level = LevelRange())
    }

    /** Заданное поле [field] словами для чипа: вид, место, редкость или вилка уровня («Ур. 27–37»). */
    fun fieldTitle(field: StashField, lang: Lang): String = when (field) {
        StashField.KIND -> kind?.title.orEmpty()
        StashField.SLOT -> slotTitle(slot, lang)
        StashField.RARITY -> rarityTitle(rarity, lang)
        StashField.LEVEL -> level.title(lang).orEmpty()
    }

    /** Заданные поля сужениями под строкой поиска, каждое - с отбором без него. */
    fun narrowings(lang: Lang): List<Narrowing<StashFilter>> = active().map { Narrowing(fieldTitle(it, lang), without(it)) }

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
        val itemOnly = slot.isNotBlank() || rarity.isNotBlank()
        if (view == null) return !itemOnly && level.admits(null)
        if (slot.isNotBlank() && view.slot.name != slot) return false
        if (rarity.isNotBlank() && view.rarity.name != rarity) return false
        return level.admits(view.level)
    }
}
