package com.sperance.exileforge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.*
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.icons.propertyIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.*

/**
 * One modifier, as a whole sentence.
 *
 * The server's dictionary holds the phrasing — "+{0} to armour" — and the rolled values fill it,
 * so there is no label to put on the left of a number any more.
 */
@Composable fun ModifierLine(modifier: JsonObject, definitions: List<ModifierDefinition>) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(propertyIcon(modifier.text("modifierId")), null, tint = Rune, modifier = Modifier.size(16.dp))
        Text(modifierText(modifier, definitions), color = Parchment, style = MaterialTheme.typography.bodyMedium)
    }
}

/** What a character must reach before the item works, printed only where the template asks for it. */
private fun requirements(doc: JsonObject): List<String> = listOf(
    "requiredLevel" to tr("ур.", "lvl"), "requiredStrength" to tr("сил", "str"),
    "requiredDexterity" to tr("лов", "dex"), "requiredIntelligence" to tr("инт", "int"),
).mapNotNull { (key, short) ->
    val value = (doc[key] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
    if (value <= if (key == "requiredLevel") 1 else 0) null else "$value $short"
}

/** Path of Exile item frame: rarity border, engraved name band, then rolled properties. */
@Composable fun ItemCard(doc: JsonObject, enabled: Boolean = true, selected: Boolean = false,
    detailed: Boolean = false, definitions: List<ModifierDefinition> = emptyList(),
    actionLabel: String = tr("Открыть", "Open"), onClick: () -> Unit = {}) {
    val color = rarityColor(doc.text("rarity"))
    val shape = CutCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomEnd = 14.dp, bottomStart = 4.dp)
    OutlinedCard(onClick = onClick, enabled = enabled, border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) GoldBright else color.copy(alpha = .45f)),
        shape = shape, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = Panel, disabledContainerColor = Panel, disabledContentColor = MaterialTheme.colorScheme.onSurface)) {
        Column(Modifier.background(Brush.verticalGradient(listOf(color.copy(alpha = .12f), Panel, Abyss)))) {
            // Name band: the plate an item's title sits on in the stash tooltip.
            Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(color.copy(alpha = .22f), Color.Transparent)))
                .drawBehind { drawLine(color.copy(alpha = .45f), Offset(0f, size.height), Offset(size.width, size.height), 1f) }
                .padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(doc.text("rarity").takeIf { it.isNotBlank() }?.let(::rarityTitle)?.uppercase()
                    ?: if (doc["userId"] != null) tr("ПЕРСОНАЖ", "CHARACTER") else tr("ПРЕДМЕТ", "ITEM"), color = color, style = MaterialTheme.typography.labelSmall)
                Text(doc.text("name").ifBlank { documentTitle(doc) }, style = MaterialTheme.typography.titleMedium,
                    color = color, maxLines = if (detailed) 5 else 2, overflow = TextOverflow.Ellipsis)
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ItemIcon(doc, color, Modifier.size(64.dp))
                    Text(doc.text("slot").takeIf { it.isNotBlank() }?.let(::slotTitle) ?: doc.text("category"),
                        color = Muted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    if (selected) Icon(Icons.Outlined.CheckCircle, tr("Выбран", "Selected"), tint = GoldBright, modifier = Modifier.size(22.dp))
                }
                OrnateDivider(color.copy(alpha = .7f))
                if (doc["itemLevel"] != null) PropertyRow(tr("Уровень предмета", "Item level"), doc.text("itemLevel"), "level")
                if (doc["level"] != null) PropertyRow(tr("Уровень персонажа", "Character level"), doc.text("level"), "level")
                if (doc["weaponType"] != null) PropertyRow(tr("Тип оружия", "Weapon type"), weaponTitle(doc.text("weaponType")), "weapon")
                if (doc["durability"] != null) PropertyRow(tr("Прочность", "Durability"), doc.text("durability"), "durability")
                if (doc["price"] != null) PropertyRow(tr("Цена", "Price"), doc.text("price"), "price")
                // Requirements decide whether a worn item counts at all; the server does the checking.
                requirements(doc).takeIf { it.isNotEmpty() }?.let { PropertyRow(tr("Требования", "Requirements"), it.joinToString(" · "), "level") }
                // The base — armour, damage, attack speed — is fixed modifiers rather than item fields.
                (doc["baseParams"] as? JsonArray).orEmpty().forEach { raw ->
                    val modifier = raw as? JsonObject ?: return@forEach
                    ModifierLine(modifier, definitions)
                }
                if (doc["money"] != null) PropertyRow(tr("Золото", "Gold"), doc.text("money"), "money")
                // Corruption is the one state that closes an item: no orb touches it again.
                if ((doc["corrupted"] as? JsonPrimitive)?.booleanOrNull == true)
                    PropertyRow(tr("Состояние", "State"), tr("Порченый", "Corrupted"), "corrupted")
                // The pool is not printed: how many definitions a template may roll from says nothing
                // about the item in front of you, and the administrator who owns it edits it in the
                // editor. What a copy actually rolled is below.
                val rolled = (doc["params"] as? JsonArray).orEmpty()
                rolled.take(if (detailed) rolled.size else 3).forEach { raw ->
                    val modifier = raw as? JsonObject ?: return@forEach
                    ModifierLine(modifier, definitions)
                }
                if (!detailed && rolled.size > 3) Text(tr("Ещё ${rolled.size - 3} свойств", "${rolled.size - 3} more properties"), color = Rune, style = MaterialTheme.typography.labelMedium)
                if (detailed) documentDescription(doc).takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(actionLabel.uppercase(), color = color, style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Outlined.ChevronRight, null, tint = color)
                }
            }
        }
    }
}
