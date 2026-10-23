package com.sperance.exileforge.core.model.auction

/** Что герой может сделать с лотом на его странице: один вид для всех лотов рынка, действия - по этому признаку. */
enum class LotStance {
    /** Чужой лот за цену на витрине: только «Купить». */
    BUY,

    /** Чужие торги: ставка и выкуп. */
    BID,

    /** Свой лот за цену: смотреть, снять, продлить в окне продления. */
    OWN_FIXED,

    /** Свои торги: смотреть, снять и продать лучшей ставке. */
    OWN_BID,

    /** Лот закрыт - продан, снят или вернулся продавцу: только смотреть. */
    CLOSED,
}

/** Что герой [heroId] может сделать с этим лотом ([LotStance]): закрытый - только смотреть, свой - снять, чужой - купить или ставить. */
fun AuctionLot.stance(heroId: String): LotStance = when {
    status != LotStatus.ACTIVE -> LotStance.CLOSED
    belongsTo(heroId) -> if (isBidding) LotStance.OWN_BID else LotStance.OWN_FIXED
    isBidding -> LotStance.BID
    else -> LotStance.BUY
}
