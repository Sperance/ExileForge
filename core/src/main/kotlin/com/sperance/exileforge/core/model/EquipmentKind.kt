package com.sperance.exileforge.core.model

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage

/** The three subclasses of the server's sealed `Equipment`; the wire discriminator is the FQCN. */
enum class EquipmentKind {
    Weapon, Armor, Accessory;
    val type: String get() = "$EQUIPMENT_PACKAGE.$name"
    fun title(lang: Lang = uiLanguage) = ui(lang, "enum.kind.$name")
    companion object {
        const val EQUIPMENT_PACKAGE = "features.data.equipment.equipment_data"
        fun of(type: String): EquipmentKind? = entries.firstOrNull { it.name == type.substringAfterLast('.') }
    }
}
