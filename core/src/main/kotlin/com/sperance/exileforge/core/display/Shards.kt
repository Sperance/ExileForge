package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.Received
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ShardYield

/**
 * Осколки продажи словами: по виду - «+3 Осколок хаоса (7/10)», склеенные из них сферы - «+1 Сфера хаоса». Одна строка
 * на тост продажи, кнопку продажи и лист вещи; ничего не дала - null.
 */
fun shardsText(yields: List<ShardYield>, perOrb: Int): String? = yields.flatMap { gain ->
    listOfNotNull(
        ui("sell.shards", gain.gained, itemTitle(gain.shard), gain.held, perOrb),
        gain.orbs.takeIf { it > 0 }?.let { ui("sell.shards_orb", it, itemTitle(gain.orb.name)) },
    )
}.takeIf { it.isNotEmpty() }?.joinToString(" · ")

/** Тост продажи [sold] с её осколками отдельной строкой, если они были. */
fun withShards(sold: String, yields: List<ShardYield>, perOrb: Int): String = shardsText(yields, perOrb)?.let { "$sold\n$it" } ?: sold

/** Сколько осколков склеиваются в сферу по правилам контента; контента ещё нет - 0 (у правил нет значений по умолчанию). */
val ContentIndex?.shardsPerOrb: Int get() = this?.rules?.sell?.shardsPerOrb ?: 0

/**
 * Тост добычи, не легшей в тайник: переполнение и продажа из-за полного тайника, автопродажа фильтром героя - частями
 * через « · », с осколками всего проданного строкой ниже; всё легло в тайник - null.
 */
fun receivedText(received: Received, perOrb: Int): String? {
    val sold = received.overflowSold
    val auto = received.autoSold
    val parts = listOfNotNull(
        ui("stash.received", received.overflowed, sold.count, sold.gold).takeIf { received.overflowed > 0 || sold.count > 0 },
        ui("stash.auto_sold", auto.count, auto.gold).takeIf { auto.count > 0 },
    )
    if (parts.isEmpty()) return null
    return withShards(parts.joinToString(" · "), ShardYield.merged(sold.shards + auto.shards), perOrb)
}
