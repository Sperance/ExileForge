package com.sperance.exileforge.core.model.trade

import com.sperance.exileforge.rules.roll.ItemInstance
import kotlinx.serialization.Serializable

/** One item on the merchant's shelf: already rolled, and its price in gold. */
@Serializable data class MerchantOffer(val id: String, val item: ItemInstance, val price: Long = 0)

/** The merchant's shelf for one hero: the server's rolls, new ones at [refreshAt] (epoch millis). */
@Serializable data class MerchantStock(val refreshAt: Long = 0, val offers: List<MerchantOffer> = emptyList())

/** A purchase: the item as it now lies in the stash, and the gold left. */
@Serializable data class MerchantPurchase(val item: ItemInstance, val money: Long = 0)
