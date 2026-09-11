package com.sperance.exileforge.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Every field has an icon; unknown custom properties use a neutral rune. */
fun propertyIcon(key: String): ImageVector {
    val value = key.lowercase()
    return when {
        listOf("life", "health", "жизн", "здоров").any(value::contains) -> Icons.Outlined.FavoriteBorder
        listOf("fire", "огонь", "огнен").any(value::contains) -> Icons.Outlined.LocalFireDepartment
        listOf("cold", "ice", "холод").any(value::contains) -> Icons.Outlined.AcUnit
        listOf("lightning", "speed", "скорост", "молни").any(value::contains) -> Icons.Outlined.Bolt
        listOf("mana", "energy", "intellect", "мана").any(value::contains) -> Icons.Outlined.WaterDrop
        listOf("armor", "defen", "resist", "защит", "сопротив").any(value::contains) -> Icons.Outlined.Shield
        listOf("damage", "attack", "урон", "сил").any(value::contains) -> Icons.Outlined.FlashOn
        listOf("level", "tier", "уров", "experience", "опыт").any(value::contains) -> Icons.Outlined.TrendingUp
        listOf("price", "money", "currency", "цена", "деньг", "сфер").any(value::contains) -> Icons.Outlined.Toll
        listOf("character", "user", "персонаж", "владел").any(value::contains) -> Icons.Outlined.PersonOutline
        listOf("name", "description", "назван", "описан").any(value::contains) -> Icons.Outlined.AutoStories
        listOf("equipment", "item", "предмет", "экип", "slot", "слот").any(value::contains) -> Icons.Outlined.Inventory2
        listOf("image", "изображ").any(value::contains) -> Icons.Outlined.Image
        listOf("condition", "услов").any(value::contains) -> Icons.Outlined.Rule
        listOf("quality", "rarity", "качеств", "редк").any(value::contains) -> Icons.Outlined.AutoAwesome
        value.contains("uuid") || value.endsWith("id") || value.contains("верси") -> Icons.Outlined.Fingerprint
        else -> Icons.Outlined.Diamond
    }
}
