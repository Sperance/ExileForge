package com.sperance.exileforge.core.model.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A promo code as the server stores it.
 *
 * The reward is one list rather than a field per kind, because an administrator writes it as one
 * thought — "some experience, a little gold and three Chaos Orbs" — and because a new kind of
 * reward should cost one enum value rather than a field in the model, the form and the contract.
 */
@Serializable data class RedemptionCode(
    @SerialName("_id") val id: String = "",
    val code: String = "",
    val description: String? = null,
    val treasure: List<RedemptionReward> = emptyList(),
    val used: Long = 0,
    val expiredAt: String? = null,
)

/**
 * One line of a reward.
 *
 * [itemId] is filled for [RedemptionKind.ITEM] and [RedemptionKind.EQUIPMENT] and empty for the
 * two that have no document behind them. For equipment the amount is a number of copies: the
 * server rolls each one separately, so two copies of a template are two different items.
 */
@Serializable data class RedemptionReward(
    val kind: RedemptionKind = RedemptionKind.ITEM,
    val itemId: String = "",
    // No default: the wire leaves out a value equal to its default, and the server has none for it,
    // so an amount of 1 never reached it and the code was refused.
    val amount: Double,
)

@Serializable enum class RedemptionKind { ITEM, EQUIPMENT, EXPERIENCE, GOLD }
