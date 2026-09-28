package com.sperance.exileforge.core.model.trade

import com.sperance.exileforge.rules.roll.ItemInstance
import kotlinx.serialization.Serializable

/** One item on the merchant's shelf: already rolled, and its price in gold. */
@Serializable data class MerchantOffer(val id: String, val item: ItemInstance, val price: Long = 0)

/**
 * A lesser orb for gold (3.15.0): the price of the next one, dearer with each bought this window.
 * Since 3.24.0 (server 1.22.0) the shelf holds only [left] more of it this window; null from an older server, which did not count.
 */
@Serializable data class MerchantOrb(val code: String, val price: Long = 0, val bought: Int = 0, val left: Int? = null) {
    val soldOut: Boolean get() = left != null && left <= 0
}

/** The merchant's shelf for one hero: the server's rolls and the orb shelf, new ones at [refreshAt] (epoch millis). */
@Serializable data class MerchantStock(val refreshAt: Long = 0, val offers: List<MerchantOffer> = emptyList(), val orbs: List<MerchantOrb> = emptyList())

/** A purchase: the item as it now lies in the stash, and the gold left. */
@Serializable data class MerchantPurchase(val item: ItemInstance, val money: Long = 0)

/** An orb bought: it is in the bag, the gold left, and what the next one costs. */
@Serializable data class MerchantOrbPurchase(val code: String, val money: Long = 0, val next: Long = 0)
