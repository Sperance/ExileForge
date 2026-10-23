package com.sperance.exileforge.core.market

import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance

/** Почему товар торговца нельзя купить сразу на героя («Купить и надеть»): кнопка гаснет, причина - под ней. */
enum class WearBar {
    /** Не хватает требований - какие, говорит [GameUi.unmetFor]. */
    NEEDS,

    /** Идёт поход по карте: снаряжение там меняет только сам поход (`CH_044` сервера). */
    MAP_RUN,

    /** Идёт забег Разлома: сборка закрыта (`CH_045` сервера). */
    RIFT,

    /** Тайник полон: надетое тоже лежит в нём (`CH_048` сервера). */
    STASH_FULL,

    /** Вторая копия надетой уникалки (`EQ_002` сервера). */
    TWIN,
}

/**
 * Почему вещь [item] нельзя купить и сразу надеть на место [place] (null - по правилам); null - можно. [onMap] - бродячий торговец в
 * походе по карте: там поход сам меняет набор, и замок похода - не помеха (причину боя называет поход). Те же правила, что у сервера
 * (`MerchantService.buyAndWear`, `MapFeatureEvents`); забег Разлома - по открытому забегу снимка героя.
 */
fun GameUi.wearBar(item: ItemInstance, place: Slot?, onMap: Boolean): WearBar? {
    val hero = hero ?: return null
    val rules = index?.rules?.stash
    return when {
        unmetFor(item).isNotEmpty() -> WearBar.NEEDS
        !onMap && hero.campaign.run != null -> WearBar.MAP_RUN
        hero.campaign.trials.rift.run != null -> WearBar.RIFT
        rules != null && hero.items.size >= rules.capacity(hero.info.stashSlots) -> WearBar.STASH_FULL
        index?.let { hero.uniqueWorn(it, item, place) } == true -> WearBar.TWIN
        else -> null
    }
}
