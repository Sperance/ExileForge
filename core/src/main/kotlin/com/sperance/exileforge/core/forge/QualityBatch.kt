package com.sperance.exileforge.core.forge

import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.roll.QualityClimb

/** Что держит пачку сфер качества; при равенстве - первый по порядку. */
enum class BatchLimit {
    /** Потолок качества вида: больше сфер не нужно. */
    CAP,

    /** Золото на сбор за каждую сферу. */
    GOLD,

    /** Сферы качества в сумке. */
    ORBS,

    /** Катализатор знамения в сумке: он тратится с каждой сферой. */
    OMENS,
}

/**
 * Пачка сфер качества над целью кузницы - одним запросом, как сервер: путь до потолка [climb] (правило [ForgeTarget.qualityClimb])
 * и сколько сфер позволяет каждый предел ([bounds]: потолок, золото на сбор, сумка). [max] - наибольшая пачка, [limit] - что её
 * держит; [max] 0 - не ляжет ни одна.
 */
class QualityBatch private constructor(val climb: QualityClimb, bounds: Map<BatchLimit, Long>) {
    private val least = bounds.entries.minBy { it.value }

    val max: Int = least.value.coerceAtLeast(0).toInt()

    val limit: BatchLimit = least.key

    /** Сколько сфер нужно до потолка. */
    val need: Int get() = climb.orbs

    companion object {
        /**
         * Пачка над [target] со знамением [omen]: [held] - сколько чего в сумке, [money] - золото героя. Сбор - одна функция
         * правил `craftFee.orb` по уровню предмета копии; null - сфера качества на цель не ляжет.
         */
        fun of(index: ContentIndex, target: ForgeTarget, omen: Omen?, held: (String) -> Long, money: Long): QualityBatch? {
            val climb = target.qualityClimb(index, omen) ?: return null
            val fee = index.rules.craftFee.orb(Orb.QUALITY_ORB, target.itemLevel)
            val bounds = buildMap {
                put(BatchLimit.CAP, climb.orbs.toLong())
                if (fee > 0) put(BatchLimit.GOLD, money / fee)
                put(BatchLimit.ORBS, held(Orb.QUALITY_ORB.name))
                omen?.let { put(BatchLimit.OMENS, held(it.code)) }
            }
            return QualityBatch(climb, bounds)
        }
    }
}
