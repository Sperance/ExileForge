package com.sperance.exileforge.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Every field has an icon; unknown custom properties fall back to the exalted sigil. */
fun propertyIcon(key: String): ImageVector {
    val value = key.lowercase()
    return when {
        listOf("life", "health", "жизн", "здоров").any(value::contains) -> Icons.Outlined.FavoriteBorder
        listOf("fire", "огонь", "огнен").any(value::contains) -> Icons.Outlined.LocalFireDepartment
        listOf("cold", "ice", "холод").any(value::contains) -> Icons.Outlined.AcUnit
        listOf("lightning", "speed", "скорост", "молни").any(value::contains) -> Icons.Outlined.Bolt
        listOf("chaos", "хаос").any(value::contains) -> ForgeGlyphs.Skull
        listOf("mana", "energy", "intellect", "мана").any(value::contains) -> Icons.Outlined.WaterDrop
        listOf("flask", "potion", "флакон", "зель").any(value::contains) -> ForgeGlyphs.Flask
        listOf("armor", "armour", "defen", "resist", "защит", "сопротив", "брон").any(value::contains) -> ForgeGlyphs.Kite
        listOf("damage", "attack", "урон", "сил", "бой", "battle").any(value::contains) -> ForgeGlyphs.Swords
        listOf("evasion", "dodge", "уклон", "ловкост").any(value::contains) -> Icons.Outlined.DirectionsRun
        listOf("level", "tier", "уров", "experience", "опыт").any(value::contains) -> Icons.Outlined.TrendingUp
        listOf("price", "money", "currency", "цена", "деньг", "сфер", "золот").any(value::contains) -> ForgeGlyphs.Orb
        listOf("character", "user", "персонаж", "владел", "герой").any(value::contains) -> ForgeGlyphs.Exile
        listOf("gem", "socket", "камен", "самоцвет").any(value::contains) -> ForgeGlyphs.Gem
        listOf("map", "zone", "карт", "зона", "поход").any(value::contains) -> ForgeGlyphs.Atlas
        listOf("recipe", "definition", "рецепт", "определен", "правил").any(value::contains) -> ForgeGlyphs.Tome
        listOf("name", "description", "назван", "описан", "журнал", "log").any(value::contains) -> ForgeGlyphs.Scroll
        listOf("helmet", "шлем").any(value::contains) -> ForgeGlyphs.Helm
        listOf("equipment", "item", "предмет", "экип", "slot", "слот", "инвентар", "арсенал").any(value::contains) -> ForgeGlyphs.Stash
        listOf("craft", "forge", "кузниц", "крафт").any(value::contains) -> ForgeGlyphs.Anvil
        listOf("passive", "node", "навык", "узел", "древ").any(value::contains) -> ForgeGlyphs.Constellation
        listOf("compare", "сравн").any(value::contains) -> ForgeGlyphs.Scales
        listOf("server", "url", "connect", "сервер", "соединен").any(value::contains) -> ForgeGlyphs.Portal
        listOf("image", "изображ").any(value::contains) -> Icons.Outlined.Image
        listOf("condition", "услов").any(value::contains) -> Icons.Outlined.Rule
        listOf("quality", "rarity", "качеств", "редк").any(value::contains) -> Icons.Outlined.AutoAwesome
        listOf("corrupt", "осквер", "mirror", "зеркал").any(value::contains) -> ForgeGlyphs.Sigil
        listOf("ref", "link", "ссылк").any(value::contains) -> ForgeGlyphs.Chain
        value.contains("uuid") || value.endsWith("id") || value.contains("верси") -> Icons.Outlined.Fingerprint
        else -> ForgeGlyphs.Sigil
    }
}
