package com.sperance.exileforge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.rarityColor
import kotlinx.serialization.json.*

@Composable fun ItemCard(doc: JsonObject, enabled: Boolean = true, onClick: () -> Unit = {}) {
    val color = rarityColor(doc.text("rarity"))
    OutlinedCard(onClick = onClick, enabled = enabled, border = BorderStroke(1.dp, color.copy(alpha = .55f)),
        shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color.copy(alpha = .08f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, color.copy(alpha = .3f))) {
                    Icon(if (doc["type"] == null) Icons.Outlined.Diamond else Icons.Outlined.Shield, null, tint = color, modifier = Modifier.padding(12.dp).size(28.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(doc.text("name"), style = MaterialTheme.typography.titleMedium, color = color, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOf(doc.text("rarity"), doc.text("slot"), doc.text("category")).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 11.sp)
                }
            }
            HorizontalDivider(color = color.copy(alpha = .2f))
            if (doc["itemLevel"] != null) Text("Уровень ${doc.text("itemLevel")}  ·  Цена ${doc.text("price")}", fontSize = 12.sp)
            else if(doc["userId"] != null) Text("Уровень ${doc.text("level")} · Опыт ${doc.text("experience")} · Деньги ${doc.text("money")}", fontSize = 12.sp)
            else Text("Цена ${doc.text("price")}", fontSize = 12.sp)
            ((doc["modifiers"] ?: doc["params"]) as? JsonArray)?.take(6)?.forEach { raw ->
                val mod = raw.jsonObject
                val values = (mod["values"] as? JsonArray).orEmpty().joinToString(" / ") { (it as? JsonObject)?.text("value").orEmpty() }
                Text("$values · ${mod.text("definitionId")} · ${mod.text("source")} [T${mod.text("tier")}]", color = Rune, fontSize = 12.sp)
            }
            if (doc.text("description").isNotBlank()) Text(doc.text("description"), color = Muted, fontFamily = FontFamily.Serif, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(doc.entityId, color = Muted.copy(alpha = .65f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
    }
}
