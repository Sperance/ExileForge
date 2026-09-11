package com.sperance.exileforge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.*
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.*

@Composable fun ItemCard(doc: JsonObject, enabled: Boolean = true, selected: Boolean = false,
    detailed: Boolean = false, definitions: List<JsonObject> = emptyList(), actionLabel: String = "Открыть", onClick: () -> Unit = {}) {
    val color = rarityColor(doc.text("rarity"))
    OutlinedCard(onClick = onClick, enabled = enabled, border = BorderStroke(if(selected) 2.dp else 1.dp, if(selected) Gold else color.copy(alpha = .36f)),
        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.background(Brush.linearGradient(listOf(color.copy(alpha=.07f), MaterialTheme.colorScheme.surface))).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ItemEmblem(itemVisualKind(doc), color)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(doc.text("rarity").takeIf { it.isNotBlank() }?.let(::rarityTitle) ?: if(doc["userId"] != null) "ПЕРСОНАЖ" else "ПРЕДМЕТ", color = color, style = MaterialTheme.typography.labelSmall)
                    Text(doc.text("name").ifBlank { "Предмет экипировки" }, style = MaterialTheme.typography.titleMedium, maxLines = if(detailed) 5 else 2, overflow = TextOverflow.Ellipsis)
                    Text(doc.text("slot").takeIf { it.isNotBlank() }?.let(::slotTitle) ?: doc.text("category"), color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                if(selected) Icon(Icons.Outlined.CheckCircle, "Выбран", tint = Gold, modifier = Modifier.size(22.dp))
            }
            HorizontalDivider(color = color.copy(alpha = .2f))
            if(doc["itemLevel"] != null) PropertyRow("Уровень предмета", doc.text("itemLevel"), "level")
            if(doc["level"] != null) PropertyRow("Уровень персонажа", doc.text("level"), "level")
            if(doc["damage_min"] != null) PropertyRow("Урон", "${doc.text("damage_min")}–${doc.text("damage_max")}", "damage")
            if(doc["defense"] != null) PropertyRow("Защита", doc.text("defense"), "defense")
            if(doc["quality"] != null && (detailed || doc.text("quality") != "0")) PropertyRow("Качество", "${doc.text("quality")}%", "quality")
            if(doc["price"] != null) PropertyRow("Цена", doc.text("price"), "price")
            if(doc["userId"] != null) PropertyRow("Экипировка", (doc["equipments"] as? JsonArray).orEmpty().size.toString(), "equipment")
            val mods = (doc["modifiers"] as? JsonArray) ?: (doc["params"] as? JsonArray) ?: JsonArray(emptyList())
            mods.take(if(detailed) mods.size else 3).forEach { raw ->
                val mod = raw as? JsonObject ?: return@forEach
                PropertyRow(modifierTitle(mod, definitions), modifierValues(mod), mod.text("definitionId"))
            }
            if(!detailed && mods.size > 3) Text("Ещё ${mods.size - 3} свойств", color = Rune, style = MaterialTheme.typography.labelMedium)
            if(doc.text("corrupted") == "true") PropertyRow("Осквернён", "Да", "corruption")
            if(doc.text("mirrored") == "true") PropertyRow("Зеркальная копия", "Да", "mirror")
            if(detailed && doc.text("description").isNotBlank()) Text(doc.text("description"), color = Muted, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(actionLabel, color = color, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Outlined.ChevronRight, null, tint = color)
            }
        }
    }
}
