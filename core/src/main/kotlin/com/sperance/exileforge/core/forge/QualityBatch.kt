package com.sperance.exileforge.core.forge

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.roll.QualityClimb

/** Что держит пачку сфер; при равенстве - первый по порядку. */
enum class BatchLimit {
    /** Потолок качества вида: больше сфер не нужно. */
    CAP,

    /** Золото на сбор за каждую сферу. */
    GOLD,

    /** Сферы пачки в сумке. */
    ORBS,

    /** Катализатор знамения в сумке: он тратится с каждой сферой. */
    OMENS,
}

/**
 * Пачка сферы [orb] над целью кузницы - одним запросом, как сервер: предел пачки правил ([ForgeTarget.batchLimit]), путь качества до
 * потолка [climb] ([ForgeTarget.qualityClimb]) и сколько сфер позволяет каждый предел ([bounds]: потолок, золото на сбор, сумка). [max] -
 * наибольшая пачка, [limit] - что её держит; [max] 0 - не ляжет ни одна.
 */
class QualityBatch private constructor(val orb: Orb, val climb: QualityClimb, private val fee: Long, private val held: Long, bounds: Map<BatchLimit, Long>) {
    private val least = bounds.entries.minBy { it.value }

    val max: Int = least.value.coerceAtLeast(0).toInt()

    val limit: BatchLimit = least.key

    /** Сколько сфер нужно до потолка. */
    val need: Int get() = climb.orbs

    /** Сбор золотом за пачку из [count] сфер. */
    fun cost(count: Int): Long = fee * count

    /** Остаток сумки и качество после пачки из [count] сфер: «в сумке 25 → 18 · качество 7% → 14%». */
    fun summary(count: Int): String = "${ui("forge.orb_left", held, held - count)} · ${ui("forge.batch_quality", climb.from, climb.after(count))}"

    companion object {
        /**
         * Пачка сферы [orb] над [target] со знамением [omen]: [held] - сколько чего в сумке, [money] - золото героя. Сбор - одна функция
         * правил `craftFee.orb` по уровню предмета копии; null - сфера по правилам ложится только по одной или качество на цель не ляжет.
         */
        fun of(index: ContentIndex, target: ForgeTarget, orb: Orb, omen: Omen?, held: (String) -> Long, money: Long): QualityBatch? {
            val cap = target.batchLimit(index, orb, omen) ?: return null
            val climb = target.qualityClimb(index, omen) ?: return null
            val fee = index.rules.craftFee.orb(orb, target.itemLevel)
            val orbs = held(orb.name)
            val bounds = buildMap {
                put(BatchLimit.CAP, cap.toLong())
                if (fee > 0) put(BatchLimit.GOLD, money / fee)
                put(BatchLimit.ORBS, orbs)
                omen?.let { put(BatchLimit.OMENS, held(it.code)) }
            }
            return QualityBatch(orb, climb, fee, orbs, bounds)
        }
    }
}
