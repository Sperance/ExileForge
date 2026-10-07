package com.sperance.exileforge.core.model.feedback

import com.sperance.exileforge.rules.content.Rarity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What a report is (3.73.0, server 1.69.0): a bug or a player's suggestion. */
@Serializable enum class FeedbackKind { BUG, SUGGESTION, APPEAL }

/** Where a report stands: new, in progress, implemented, closed — a closed suggestion leaves the public list. */
@Serializable enum class ReportStatus {
    NEW,
    IN_PROGRESS,
    DONE,
    WONTFIX,
    ;

    val open: Boolean get() = this == NEW || this == IN_PROGRESS
}

/** A player's vote on a suggestion: one per account, switched by tapping again. */
@Serializable enum class Vote { LIKE, DISLIKE, NONE }

/** A suggestion in the public list: no author — only the administrator sees who wrote it; [mine] marks the viewer's own. */
@Serializable data class Suggestion(
    val id: String,
    val text: String,
    val status: ReportStatus,
    val likes: Int,
    val dislikes: Int,
    val vote: Vote,
    val mine: Boolean,
    val createdAt: String = "",
) {
    val rating: Int get() = likes - dislikes
    val votable: Boolean get() = !mine && status.open
}

/** One of the viewer's own reports, with the administrator's word. */
@Serializable data class OwnReport(
    val id: String,
    val kind: FeedbackKind,
    val text: String,
    val status: ReportStatus,
    val reason: String = "",
    val likes: Int = 0,
    val dislikes: Int = 0,
    val createdAt: String = "",
)

/** A report whole, as the administrator reads it. */
@Serializable data class FullReport(
    @SerialName("_id") val id: String,
    val text: String,
    val screen: String = "",
    val context: Map<String, String> = emptyMap(),
    val userId: String? = null,
    val status: ReportStatus = ReportStatus.NEW,
    val kind: FeedbackKind = FeedbackKind.BUG,
    val likes: List<String> = emptyList(),
    val dislikes: List<String> = emptyList(),
    val reason: String = "",
    val createdAt: String = "",
    /** The task in Asana once the administrator exported it (3.75.0, server 1.70.0); blank before. */
    val asanaUrl: String = "",
    /** Код последнего отказа Asana (3.88.7, сервер 1.80.9): выгрузка или закрытие не удались; пусто - всё ушло. */
    val asanaError: String = "",
)

/** Отчёт для администратора: с логином и ролью автора (3.88.7). */
@Serializable data class AdminReport(val report: FullReport, val login: String? = null, val role: com.sperance.exileforge.core.network.AccountRole? = null)

/** A letter's thing to take (3.73.0): a template and its rarity, rolled at the hero's level when taken. */
@Serializable data class MailEquipment(val template: String, val rarity: Rarity? = null)

/** What a letter carries: gold, stacks of the bag by code, things; one hero of the account takes it, once. */
@Serializable data class MailAttachment(
    val gold: Long = 0,
    val items: Map<String, Long> = emptyMap(),
    val equipment: List<MailEquipment> = emptyList(),
    /** Items as they are (server 1.74.0): an expired lot's goods come back by mail with their rolls. */
    val instances: List<com.sperance.exileforge.rules.roll.ItemInstance> = emptyList(),
    /** Питомцы как есть (3.91.0, сервер 1.81.10): питомец истёкшего лота; забрать - только в зверинец с местом. */
    val pets: List<com.sperance.exileforge.rules.content.Pet> = emptyList(),
) {
    val empty: Boolean get() = gold <= 0 && items.isEmpty() && equipment.isEmpty() && instances.isEmpty() && pets.isEmpty()
}

@Serializable enum class MailKind { SYSTEM, ADMIN }

/**
 * A letter of the account (3.73.0): a system one names a dictionary [key] with its [args], the administrator's one its
 * [subject] and [body] as written. [claimedBy] — the hero that took the attachment; [expiresAt] — epoch millis.
 */
@Serializable data class Mail(
    @SerialName("_id") val id: String,
    val kind: MailKind = MailKind.ADMIN,
    val key: String = "",
    val args: List<String> = emptyList(),
    val subject: String = "",
    val body: String = "",
    val attachment: MailAttachment = MailAttachment(),
    val read: Boolean = false,
    val claimedBy: String? = null,
    val expiresAt: Long = 0,
    val createdAt: String = "",
) {
    val claimable: Boolean get() = claimedBy == null && !attachment.empty
}

/** The administrator's letter: [login] one account, blank every account. */
@Serializable data class MailRequest(val login: String = "", val subject: String, val body: String, val attachment: MailAttachment = MailAttachment())
