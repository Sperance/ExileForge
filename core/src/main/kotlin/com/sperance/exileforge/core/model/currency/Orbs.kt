package com.sperance.exileforge.core.model.currency

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.pick
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
enum class CurrencyOrb(private val ru: String, private val en: String, private val ruRule: String, private val enRule: String) {
    ORB_OF_TRANSMUTATION("Сфера превращения", "Orb of Transmutation",
        "Обычный предмет становится магическим и получает аффиксы", "Upgrades a common item to magic and rolls its affixes"),
    ORB_OF_AUGMENTATION("Сфера улучшения", "Orb of Augmentation",
        "Добавляет магическому предмету ещё один аффикс", "Adds one more affix to a magic item"),
    ORB_OF_ALTERATION("Сфера изменения", "Orb of Alteration",
        "Перекатывает аффиксы магического предмета", "Rerolls the affixes of a magic item"),
    ORB_OF_ALCHEMY("Сфера алхимии", "Orb of Alchemy",
        "Обычный предмет становится редким и получает аффиксы", "Upgrades a common item to rare and rolls its affixes"),
    REGAL_ORB("Царская сфера", "Regal Orb",
        "Магический предмет становится редким, сохраняя аффиксы и получая ещё один", "Upgrades a magic item to rare, keeping its affixes and adding one"),
    CHAOS_ORB("Сфера хаоса", "Chaos Orb",
        "Перекатывает аффиксы редкого предмета", "Rerolls the affixes of a rare item"),
    EXALTED_ORB("Высшая сфера", "Exalted Orb",
        "Добавляет редкому предмету ещё один аффикс", "Adds one more affix to a rare item"),
    DIVINE_ORB("Божественная сфера", "Divine Orb",
        "Перекатывает значения аффиксов, сохраняя сами аффиксы и их тиры", "Rerolls affix values, keeping the affixes and their tiers"),
    ORB_OF_ANNULMENT("Сфера аннулирования", "Orb of Annulment",
        "Убирает с предмета случайный аффикс", "Removes one random affix"),
    ORB_OF_SCOURING("Сфера очищения", "Orb of Scouring",
        "Снимает все аффиксы и возвращает предмет к обычной редкости", "Strips every affix and returns the item to common"),
    BLESSED_ORB("Священная сфера", "Blessed Orb",
        "Перекатывает значения implicit-модификаторов", "Rerolls the values of the implicit modifiers"),
    VAAL_ORB("Сфера ваал", "Vaal Orb",
        "Портит предмет: вешает модификатор порчи, после чего предмет неизменяем", "Corrupts the item: adds a corruption modifier and locks it forever"),
    ORB_OF_CHANCE("Сфера удачи", "Orb of Chance",
        "Делает из обычного предмета предмет случайной редкости, изредка — уникальный", "Turns a common item into a random rarity, rarely a unique"),
    MIRROR_OF_KALANDRA("Зеркало Каландры", "Mirror of Kalandra",
        "Создаёт неизменяемую копию предмета", "Creates an unalterable copy of the item");

    fun title(lang: Lang = uiLanguage): String = lang.pick(ru, en)
    /** What the server's rule for this orb is, shown so a rejection is expected rather than puzzling. */
    fun rule(lang: Lang = uiLanguage): String = lang.pick(ruRule, enRule)

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
