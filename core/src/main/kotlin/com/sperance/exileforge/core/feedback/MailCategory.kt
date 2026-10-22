package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.MailKind
import com.sperance.exileforge.rules.content.SystemMail

/**
 * О чём письмо - категория ящика по виду письма и ключу системного письма: её имя (`mail.category.<имя>`), знак и цвет печатают
 * строка письма и плашка отправителя. Письмо героя - [PERSONAL], рассылка гильдии - [GUILD], письмо администратора - [ADMIN];
 * системное - по ключу ([of]), незнакомый ключ - [SYSTEM]. Новый ключ [SystemMail] обязан лечь в свою категорию здесь.
 */
enum class MailCategory {
    MARKET,
    PERSONAL,
    GUILD,
    ADMIN,
    REWARDS,
    REPORTS,
    SYSTEM,
    ;

    companion object {
        /** Категория письма [mail]. */
        fun of(mail: Mail): MailCategory = when (mail.kind) {
            MailKind.PLAYER -> PERSONAL
            MailKind.GUILD -> GUILD
            MailKind.ADMIN -> ADMIN
            MailKind.SYSTEM -> ofKey(mail.key) ?: SYSTEM
        }

        /** Категория системного письма с ключом [key]; ключ, что сюда не разнесён, - null. */
        fun ofKey(key: String): MailCategory? = BY_KEY[key]

        private val BY_KEY: Map<String, MailCategory> = mapOf(
            SystemMail.AUCTION_EXPIRED to MARKET,
            SystemMail.AUCTION_RECALLED to MARKET,
            SystemMail.AUCTION_EXPIRING to MARKET,
            SystemMail.AUCTION_OUTBID to MARKET,
            SystemMail.AUCTION_BID_RETURNED to MARKET,
            SystemMail.AUCTION_SOLD to MARKET,
            SystemMail.AUCTION_WON to MARKET,
            SystemMail.CONTEST_PRIZE to REWARDS,
            SystemMail.CONTEST_PODIUM to REWARDS,
            SystemMail.RIFT_PRIZE to REWARDS,
            SystemMail.FEEDBACK_STATUS to REPORTS,
            SystemMail.FEEDBACK_ISSUE to REPORTS,
            SystemMail.FEEDBACK_REPLY to REPORTS,
            SystemMail.FEEDBACK_COMMENT to REPORTS,
            SystemMail.FATE_REDRAWN to ADMIN,
            SystemMail.HERO_RENAMED to ADMIN,
            SystemMail.ATLAS_RESET to SYSTEM,
            SystemMail.ATLAS_REBUILT to SYSTEM,
            SystemMail.TREE_RESET to SYSTEM,
            SystemMail.KEYSTONE_REFUND to SYSTEM,
            SystemMail.GUILD_TREE_RESET to GUILD,
        )
    }
}
