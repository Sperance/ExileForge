package com.sperance.exileforge.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

@Composable fun InventoryCommandsPanel(s: ForgeState, vm: ForgeViewModel) {
    if(s.equipmentView == null) return
    var expanded by remember(s.characterId) { mutableStateOf(false) }
    val enabled = !s.busy && s.signedIn && s.pending == null && s.inventoryVersion != null
    TextButton(onClick = { expanded = !expanded }) {
        Text(tr("Предметы, награды и рецепты · ${if(expanded) "свернуть" else "показать"}", "Items, rewards and recipes · ${if(expanded) "hide" else "show"}"))
    }
    if(!expanded) return
    ForgePanel {
        Engraved(tr("Сумка", "Bag"))
        s.equipmentView.items.forEach { stack ->
            val currency = s.currencies.firstOrNull { it.text("itemId") == stack.itemId }
            PropertyRow(currency?.text("name") ?: tr("Предмет", "Item") + " …${stack.itemId.takeLast(6)}", stack.amount.toString(), "currency")
        }
        var code by remember(s.characterId) { mutableStateOf("") }
        OutlinedTextField(code, { code = it.take(100) }, label = { Text(tr("Промокод", "Promo code")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = enabled && code.isNotBlank(), onClick = { vm.redeem(code) }) { Text(tr("Получить награду", "Claim the reward")) }
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
                supportingText = { Text(tr("Отрицательное число списывает предметы", "A negative number removes items")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(enabled = enabled && itemId.isNotBlank() && amount.toLongOrNull()?.let { it != 0L && it in -1_000_000_000L..1_000_000_000L } == true,
                onClick = { vm.adjustItems(itemId, amount.toLong()) }) { Text(tr("Изменить количество", "Change the amount")) }
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
                        selectorMatches && s.equipmentView?.items.orEmpty().any { it.itemId == item.entityId && it.amount > 0 }
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
