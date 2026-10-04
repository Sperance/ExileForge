package com.sperance.exileforge.core.model.command

import com.sperance.exileforge.rules.reward.RedemptionKind
import com.sperance.exileforge.rules.reward.RedemptionReward
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A promo code as the server stores it; the reward is one list of lines of any kind (server 1.75.0: each kind with its own fields). */
@Serializable data class RedemptionCode(
    @SerialName("_id") val id: String = "",
    val code: String = "",
    val description: String? = null,
    val treasure: List<RedemptionReward> = emptyList(),
    val used: Long = 0,
    val expiredAt: String? = null,
)

/** A line of [kind] as the administrator typed it: the stack or template [code] it names, if any, and how many. */
fun redemptionReward(kind: RedemptionKind, code: String, amount: Double): RedemptionReward = when (kind) {
    RedemptionKind.ITEM -> RedemptionReward.Item(code, amount.toLong())
    RedemptionKind.EQUIPMENT -> RedemptionReward.Equipment(code, amount.toInt())
    RedemptionKind.EXPERIENCE -> RedemptionReward.Experience(amount)
    RedemptionKind.GOLD -> RedemptionReward.Gold(amount.toLong())
}

/** The stack or template a line names; null for experience and gold. */
val RedemptionReward.named: String?
    get() = when (this) {
        is RedemptionReward.Item -> code
        is RedemptionReward.Equipment -> template
        is RedemptionReward.Experience, is RedemptionReward.Gold -> null
    }
