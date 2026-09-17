package com.sperance.exileforge.core.model.icons

import kotlinx.serialization.Serializable

/** Icon the server signs everything it has no drawing for. */
const val FALLBACK_ICON = "ui-unknown"

/** Groups of the set. The client uses them for filters and previews; drawing never depends on them. */
@Serializable enum class IconCategory { WEAPON, ARMOUR, JEWELLERY, CURRENCY, STAT, AFFIX, RARITY, PASSIVE, COMBAT, ITEM, UI }

/** One icon: where its picture lives and the palette it was drawn with. */
@Serializable data class IconDescriptor(val id: String, val title: String = "", val category: IconCategory = IconCategory.UI,
    val tint: String = "", val deep: String = "", val url: String = "", val keywords: List<String> = emptyList())

/** `GET /api/v1/icons`. While [version] is unchanged nothing in the set has changed, so the cache stands. */
@Serializable data class IconManifest(val set: String = "", val revision: Int = 0, val version: String = "",
    val format: String = "svg", val viewBox: Int = 64, val sprite: String = "/api/v1/icons/sprite.svg",
    val fallback: String = FALLBACK_ICON, val total: Int = 0,
    val categories: Map<IconCategory, Int> = emptyMap(), val icons: List<IconDescriptor> = emptyList())

/**
 * `GET /api/v1/icons/bindings`: the tables that let the client pick an icon itself.
 *
 * Keys are exactly the values the rest of the API already returns, so a payload written before the
 * set existed still resolves without a request per entity.
 */
@Serializable data class IconBindingTables(val version: String = "", val fallback: String = FALLBACK_ICON,
    val stats: Map<String, String> = emptyMap(), val tags: Map<String, String> = emptyMap(),
    val modifierSources: Map<String, String> = emptyMap(), val itemClasses: Map<String, String> = emptyMap(),
    val weapons: Map<String, String> = emptyMap(), val slots: Map<String, String> = emptyMap(),
    val rarities: Map<String, String> = emptyMap(), val poeRarities: Map<String, String> = emptyMap(),
    val currencies: Map<String, String> = emptyMap(), val passiveKinds: Map<String, String> = emptyMap(),
    val battleActions: Map<String, String> = emptyMap(), val battleStatuses: Map<String, String> = emptyMap(),
    val combatElements: Map<String, String> = emptyMap(), val modifiers: Map<String, String> = emptyMap(),
    val bases: Map<String, String> = emptyMap())
