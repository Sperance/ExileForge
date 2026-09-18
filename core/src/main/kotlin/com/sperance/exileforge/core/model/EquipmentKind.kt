package com.sperance.exileforge.core.model

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.pick
import com.sperance.exileforge.core.i18n.uiLanguage

/** The three subclasses of the server's sealed `Equipment`; the wire discriminator is the FQCN. */
enum class EquipmentKind(private val ru: String, private val en: String) {
    Weapon("Оружие", "Weapon"), Armor("Броня", "Armour"), Accessory("Аксессуар", "Accessory");
    val type: String get() = "$EQUIPMENT_PACKAGE.$name"
    fun title(lang: Lang = uiLanguage) = lang.pick(ru, en)
    companion object {
        const val EQUIPMENT_PACKAGE = "features.data.equipment.equipment_data"
        fun of(type: String): EquipmentKind? = entries.firstOrNull { it.name == type.substringAfterLast('.') }
        /** Which kind the server puts in a slot, so a template can be created for it. */
        fun forSlot(slot: String): EquipmentKind = when (slot) {
            "WEAPON_1H", "WEAPON_2H" -> Weapon
            "RING", "AMULET", "BELT", "QUIVER" -> Accessory
            else -> Armor
        }
    }
}
