package com.sperance.exileforge.core.model.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A promo code as the server stores it; the reward is one list so a new kind costs one enum value. */
@Serializable data class RedemptionCode(
    @SerialName("_id") val id: String = "",
    val code: String = "",
    val description: String? = null,
    val treasure: List<RedemptionReward> = emptyList(),
    val used: Long = 0,
    val expiredAt: String? = null,
)

/** One line of a reward: a stack or a template by code, or an amount of experience or gold. */
@Serializable data class RedemptionReward(val kind: RedemptionKind = RedemptionKind.ITEM, val item: String = "", val amount: Double = 1.0)

@Serializable enum class RedemptionKind { ITEM, EQUIPMENT, EXPERIENCE, GOLD }
