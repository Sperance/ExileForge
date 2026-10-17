package com.sperance.exileforge.core.expedition

import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.rules.content.BulkSale
import com.sperance.exileforge.rules.roll.ItemInstance

/**
 * Где выпавшая вещь сейчас у героя - одно правило для всех списков лута: окно боя и сундука, вкладка лута,
 * итог карты, отчёт забега, испытание. Снимок награды не меняется - по id вещи меняется только её показ.
 */
enum class LootPresence {
    /** Лежит в тайнике: обычная строка с действиями. */
    HELD,

    /** Надета или в гнезде: строка с меткой «Надето», без действий. */
    WORN,

    /** Ещё не дошла до тайника с ответом журнала: строка ждёт, кнопка «в пути». */
    ARRIVING,

    /** У героя её нет - продана: строки нет. */
    GONE,
    ;

    val shown: Boolean get() = this != GONE
    val worn: Boolean get() = this == WORN

    /** Строка открывает карточку, надевает и продаёт вещь. */
    val actionable: Boolean get() = this == HELD || this == ARRIVING
}

/**
 * Положение вещи [id] у героя по тайнику и надетому. [arriving] - ответ журнала с ней ещё не пришёл: вещи, которой нет
 * у героя, ждут, а не прячут. Герой ещё не прочитан - вещь показывается как есть.
 */
fun GameUi.lootPresence(id: String, arriving: Boolean = false): LootPresence {
    val hero = hero ?: return LootPresence.HELD
    val held = hero.item(id) ?: return if (arriving) LootPresence.ARRIVING else LootPresence.GONE
    return if (held.equipped || held.socketed) LootPresence.WORN else LootPresence.HELD
}

/** Вещи снимка награды [equipment] с их положением у героя, без пропавших, в порядке снимка. */
fun GameUi.presentLoot(equipment: List<ItemInstance>, arriving: Boolean = false): List<Pair<ItemInstance, LootPresence>> = equipment.map { it to lootPresence(it.id, arriving) }.filter { (_, presence) -> presence.shown }

/**
 * Можно ли пометить вещь [id] к продаже посреди захода - то же правило, что лот продажи в итогах ([BulkSale.pickable]): вещь в
 * тайнике - если правила её отмечают, вещь в пути ([arriving]) - да, надетая и проданная - нет.
 */
fun GameUi.saleMarkable(id: String, arriving: Boolean = false): Boolean = when (lootPresence(id, arriving)) {
    LootPresence.HELD -> hero?.item(id)?.let(BulkSale::pickable) != false
    LootPresence.ARRIVING -> true
    LootPresence.WORN, LootPresence.GONE -> false
}
