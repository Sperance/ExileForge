package com.sperance.exileforge.core.model.trade

import com.sperance.exileforge.core.model.hero.EquipmentInstance
import kotlinx.serialization.Serializable

/** One item on the merchant's shelf (since server 0.34.0): already rolled, and its price in gold. */
@Serializable data class MerchantOffer(val id: String, val item: EquipmentInstance, val price: Long = 0)

/**
 * The merchant's shelf for one hero: four to six items the server rolled, new ones every four
 * hours at [refreshAt] (epoch milliseconds). It cannot be renewed sooner.
 */
@Serializable data class MerchantStock(val refreshAt: Long = 0, val offers: List<MerchantOffer> = emptyList())

/** A purchase: the item as it now lies in the stash, and the gold left. */
@Serializable data class MerchantPurchase(val item: EquipmentInstance, val money: Long = 0)
