package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.run.Reward

/**
 * Добыча сундука или объекта карты: событие [n] и что сервер за него дал или скатал в запас сундуков захода; null - ни ответа, ни
 * запаса, объект ждёт. Добычу катит только сервер: здесь она лишь раскладывается для показа.
 */
data class MapDrop(val n: Int, val reward: Reward?) {
    val waiting: Boolean get() = reward == null

    /** Сколько вещей легло в сумку - «+N» у плашки: вещи экипировки и штуки стопок; золото и опыт не в счёт. */
    val pieces: Long get() = reward?.pieces() ?: 0L

    /** Лучшая вещь добычи - строка у сумки: самая редкая экипировка; null - вещей нет. */
    val best: ItemInstance? get() = reward?.equipment?.maxByOrNull { it.rarity.ordinal }

    /**
     * Фишки веера: по одной на вещь, на стопку и на золото - каждая летит в сумку. Порядок - от лучшей вещи к золоту, как
     * веер слева направо.
     */
    val chips: List<DropChip>
        get() = reward?.let { r ->
            r.equipment.sortedByDescending { it.rarity.ordinal }.map { DropChip.Piece(it) } +
                r.items.entries.sortedByDescending { it.value }.map { (code, amount) -> DropChip.Stack(code, amount) } +
                listOfNotNull(r.gold.takeIf { it > 0 }?.let(DropChip::Gold))
        }.orEmpty()
}

/** Фишка веера добычи. */
sealed interface DropChip {
    data class Piece(val item: ItemInstance) : DropChip

    data class Stack(val code: String, val amount: Long) : DropChip

    data class Gold(val amount: Long) : DropChip
}

/** Сколько штук добычи легло в сумку: вещи экипировки и штуки стопок; золото и опыт не в счёт. */
fun Reward.pieces(): Long = equipment.size + items.values.sum()
