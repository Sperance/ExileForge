package com.sperance.exileforge.core.model.currency

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The category the server files its currency orbs under inside the shared `items` collection. */
const val CURRENCY_CATEGORY = "CURRENCY"

/**
 * The orbs the server implements, named by the sub-category of their `items` document.
 *
 * This is a display table and nothing else: which orb applies to which item, what it rerolls and
 * what it costs is decided by `CurrencyApplier` on the server. An orb the client does not know
 * still appears in the list under its own document name — the enum only adds a translation.
 */
enum class CurrencyOrb {
    ORB_OF_TRANSMUTATION, ORB_OF_AUGMENTATION, ORB_OF_ALTERATION, ORB_OF_ALCHEMY,
    REGAL_ORB, CHAOS_ORB, EXALTED_ORB, DIVINE_ORB, ORB_OF_ANNULMENT, ORB_OF_SCOURING,
    BLESSED_ORB, VAAL_ORB, ORB_OF_CHANCE, MIRROR_OF_KALANDRA,
    // Since 0.23.0: fracture one affix for good, and the two influences.
    FRACTURING_ORB, SHAPERS_ORB, ELDER_ORB,
    // The one orb that is never applied to an item: the tree spends it, and applyOrb refuses it
    // outright (CR_009). It is in the table because it is still an orb in the bag and a price
    // on the auction, and a bag entry the client cannot name is a bag entry a player cannot read.
    ORB_OF_REGRET;

    fun title(lang: Lang = uiLanguage): String = ui(lang, "enum.orb.$name")
    /** What the server's rule for this orb is, shown so a rejection is expected rather than puzzling. */
    fun rule(lang: Lang = uiLanguage): String = ui(lang, "enum.orb.$name.rule")

    companion object { fun of(subCategory: String): CurrencyOrb? = entries.firstOrNull { it.name == subCategory } }
}

/**
 * One currency document of the `items` collection.
 *
 * It is an ordinary stacking item: the character owns a count of it in the bag and the server
 * spends one of them per application.
 */
@Serializable data class CurrencyItem(
    @SerialName("_id") val id: String,
    val code: String = "",
    val subCategory: String = "",
    val price: Long = 0,
) {
    val orb: CurrencyOrb? get() = CurrencyOrb.of(subCategory)
    /**
     * The orb's name.
     *
     * The server's dictionary is the source since 0.14.0; the enum's own translation is the
     * fallback for a server whose bundle has not been read yet, and the code is the last resort.
     */
    fun title(lang: Lang = uiLanguage): String = locOr(LocaleKey.itemName(code), orb?.title(lang) ?: code)
    /** What the orb does; the dictionary first, the enum's own rule when it is not there yet. */
    fun details(lang: Lang = uiLanguage): String = locOr(LocaleKey.itemDescription(code), orb?.rule(lang).orEmpty())
}
