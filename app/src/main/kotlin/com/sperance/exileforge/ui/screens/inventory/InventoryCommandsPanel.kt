package com.sperance.exileforge.ui.screens.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.command.MAX_ITEM_UNITS
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

@Composable fun InventoryCommandsPanel(s: ForgeState, vm: ForgeViewModel) {
    if(s.equipmentView == null) return
    var expanded by remember(s.characterId) { mutableStateOf(false) }
    val enabled = !s.busy && s.signedIn && s.pending == null && s.inventoryVersion != null
    TextButton(onClick = { expanded = !expanded }) {
        Text(tr("Сумка и рецепты · ${if(expanded) "свернуть" else "показать"}", "Bag and recipes · ${if(expanded) "hide" else "show"}"))
    }
    if(!expanded) return
    ForgePanel {
        BagInventory(s)
        OrnateDivider()
        RecipeCommandForm(s, vm, enabled)
        if(s.adminTools) {
            OrnateDivider()
            Engraved(tr("Администрирование инвентаря", "Inventory administration"))
            var equipmentId by remember(s.characterId) { mutableStateOf("") }
            EntitySpinner(tr("Выдать экипировку", "Grant equipment"), equipmentId, EntitySource.EQUIPMENT, enabled, { equipmentId = it })
            Button(enabled = enabled && equipmentId.isNotBlank(), onClick = { vm.grant(equipmentId) }) { Text(tr("Выдать выбранный предмет", "Grant the chosen item")) }
            var itemId by remember(s.characterId) { mutableStateOf("") }
            var amount by remember(s.characterId) { mutableStateOf("1") }
            EntitySpinner(tr("Предмет или сфера", "Item or orb"), itemId, EntitySource.ITEM, enabled, { itemId = it })
            OutlinedTextField(amount, { amount = it }, label = { Text(tr("Добавить / списать количество", "Add / remove amount")) },
                supportingText = { Text(tr("Отрицательное число списывает предметы. Каждая единица — отдельная запись, за команду не больше $MAX_ITEM_UNITS.", "A negative number removes items. Every unit is a separate record, at most $MAX_ITEM_UNITS per command.")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(enabled = enabled && itemId.isNotBlank() && amount.toLongOrNull()?.let { it != 0L && it in -MAX_ITEM_UNITS..MAX_ITEM_UNITS } == true,
                onClick = { vm.adjustItems(itemId, amount.toLong()) }) { Text(tr("Изменить количество", "Change the amount")) }
        }
    }
}

private data class BagEntry(val itemId: String, val name: String, val kind: String, val amount: Long, val document: JsonObject, val known: Boolean)

/** Bag ledger: a socketed icon, name and kind, the server's unit count struck large on the right. */
@Composable private fun BagInventory(s: ForgeState) {
    var byAmount by remember(s.characterId) { mutableStateOf(true) }
    val entries = remember(s.itemTotals, s.currencies, s.lang) {
        s.itemTotals.map { (itemId, amount) ->
            val currency = s.currencies.firstOrNull { it.text("itemId") == itemId }
            // Currencies rarely carry a category; the emblem fallback still has to draw an orb for them.
            val document = currency?.let { if(it.text("category").isBlank()) JsonObject(it + ("category" to JsonPrimitive("Currency"))) else it } ?: JsonObject(emptyMap())
            BagEntry(itemId, currency?.text("name")?.ifBlank { null } ?: (tr("Предмет", "Item") + " …${itemId.takeLast(6)}"),
                if(currency != null) tr("Валюта", "Currency") else tr("Без описания", "No description"), amount, document, currency != null)
        }
    }
    val sorted = if(byAmount) entries.sortedWith(compareByDescending<BagEntry> { it.amount }.thenBy { it.name }) else entries.sortedBy { it.name.lowercase() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Engraved(tr("Сумка", "Bag"), modifier = Modifier.weight(1f))
        if(entries.size > 1) OutlinedButton(onClick = { byAmount = !byAmount }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp), modifier = Modifier.heightIn(min = 30.dp)) {
            Text(if(byAmount) tr("↓ по количеству", "↓ by amount") else tr("↓ по названию", "↓ by name"), style = MaterialTheme.typography.labelMedium)
        }
    }
    // Stacks are gone: the amount is a count of unit documents the server recomputed for us.
    if(entries.isEmpty()) { Text(tr("Сумка пуста", "The bag is empty"), color = Muted); return }
    Column {
        sorted.forEachIndexed { index, entry ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(38.dp).background(Rune.copy(alpha = .10f), CutCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    ItemIcon(entry.document, if(entry.known) Gold else Muted, Modifier.size(28.dp), framed = false)
                }
                Column(Modifier.weight(1f)) {
                    Text(entry.name, color = if(entry.known) Parchment else Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(entry.kind, color = Muted, style = MaterialTheme.typography.labelMedium)
                }
                Text(entry.amount.toString(), color = GoldBright, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 44.dp))
            }
            if(index < sorted.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable private fun RecipeCommandForm(s: ForgeState, vm: ForgeViewModel, enabled: Boolean) {
    var recipeId by remember(s.characterId) { mutableStateOf("") }
    var recipe by remember(s.characterId) { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var ingredients by remember(recipeId, refresh) { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var amount by remember(recipeId) { mutableStateOf("1") }
    LaunchedEffect(recipeId, refresh) {
        recipe = null; error = null
        if(recipeId.isNotBlank()) {
            loading = true
            try { recipe = vm.recipe(recipeId) }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { error = e.message }
            finally { loading = false }
        }
    }
    EntitySpinner(tr("Рецепт", "Recipe"), recipeId, EntitySource.RECIPE, enabled && !loading, { recipeId = it })
    if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    recipe?.let { selected ->
        val inputs = (selected["arrayIn"] as? JsonArray).orEmpty()
        val supported = selected.text("timeWork").toDoubleOrNull() == 0.0 && (selected["requirement"] as? JsonArray).orEmpty().isEmpty()
        Text(selected.text("name"))
        if(!supported) Text(tr("Сервер пока выполняет только мгновенные рецепты без требований навыков.", "The server only runs instant recipes without skill requirements for now."), color = Muted)
        inputs.forEachIndexed { index, input ->
            val row = input.jsonObject
            val loader = LocalEntityPageLoader.current
            key(recipeId, index, refresh) {
                CompositionLocalProvider(LocalEntityPageLoader provides { source, page, query ->
                    val result = loader(source, page, query)
                    result.copy(items = result.items.filter { item ->
                        val selectorMatches = when {
                            row.text("itemId").isNotBlank() -> item.entityId == row.text("itemId")
                            row.text("category").isNotBlank() -> item.text("category") == row.text("category")
                            else -> item.text("subCategory") == row.text("subCategory")
                        }
                        selectorMatches && (s.itemTotals[item.entityId] ?: 0L) > 0
                    })
                }) {
                    EntitySpinner(tr("Ингредиент ${index + 1} · ${row.text("amount")} за применение", "Ingredient ${index + 1} · ${row.text("amount")} per use"), ingredients[index].orEmpty(), EntitySource.ITEM, enabled && supported, { ingredients = ingredients + (index to it) })
                }
            }
        }
        OutlinedTextField(amount, { amount = it }, label = { Text(tr("Число применений, 1–100", "Number of uses, 1–100")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = enabled && supported && !loading && amount.toLongOrNull()?.let { it in 1L..100L } == true && inputs.indices.all { !ingredients[it].isNullOrBlank() }, onClick = {
            vm.useRecipe(selected, inputs.indices.map { ingredients.getValue(it) }.distinct(), amount.toLong())
        }) { Text(tr("Использовать рецепт", "Use the recipe")) }
        OutlinedButton(enabled = enabled && !loading, onClick = { refresh++ }) { Text(tr("Обновить рецепт", "Refresh the recipe")) }
        Text(tr("Ингредиенты списываются сервером. После применения обновите рецепт перед следующим запуском.", "Ingredients are consumed by the server. Refresh the recipe before running it again."), color = Muted)
    }
}
