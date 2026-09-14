package com.sperance.exileforge.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.*
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
    TextButton(onClick = { expanded = !expanded }) { Text("Предметы, награды и рецепты · ${if(expanded) "свернуть" else "показать"}") }
    if(!expanded) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        s.equipmentView.items.forEach { stack ->
            val currency = s.currencies.firstOrNull { it.text("itemId") == stack.itemId }
            PropertyRow(currency?.text("name") ?: "Предмет …${stack.itemId.takeLast(6)}", stack.amount.toString(), "currency")
        }
        var code by remember(s.characterId) { mutableStateOf("") }
        OutlinedTextField(code, { code = it.take(100) }, label = { Text("Промокод") }, singleLine = true)
        Button(enabled = enabled && code.isNotBlank(), onClick = { vm.redeem(code) }) { Text("Получить награду") }
        HorizontalDivider()
        RecipeCommandForm(s, vm, enabled)
        if(s.adminTools) {
            HorizontalDivider()
            Text("Администрирование инвентаря")
            var equipmentId by remember(s.characterId) { mutableStateOf("") }
            EntitySpinner("Выдать экипировку", equipmentId, EntitySource.EQUIPMENT, enabled, { equipmentId = it })
            Button(enabled = enabled && equipmentId.isNotBlank(), onClick = { vm.grant(equipmentId) }) { Text("Выдать выбранный предмет") }
            var itemId by remember(s.characterId) { mutableStateOf("") }
            var amount by remember(s.characterId) { mutableStateOf("1") }
            EntitySpinner("Предмет или сфера", itemId, EntitySource.ITEM, enabled, { itemId = it })
            OutlinedTextField(amount, { amount = it }, label = { Text("Добавить / списать количество") }, supportingText = { Text("Отрицательное число списывает предметы") }, singleLine = true)
            Button(enabled = enabled && itemId.isNotBlank() && amount.toLongOrNull()?.let { it != 0L && it in -1_000_000_000L..1_000_000_000L } == true,
                onClick = { vm.adjustItems(itemId, amount.toLong()) }) { Text("Изменить количество") }
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
    EntitySpinner("Рецепт", recipeId, EntitySource.RECIPE, enabled && !loading, { recipeId = it })
    if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    recipe?.let { selected ->
        val inputs = (selected["arrayIn"] as? JsonArray).orEmpty()
        val supported = selected.text("timeWork").toDoubleOrNull() == 0.0 && (selected["requirement"] as? JsonArray).orEmpty().isEmpty()
        Text(selected.text("name"))
        if(!supported) Text("Сервер пока выполняет только мгновенные рецепты без требований навыков.", color = Muted)
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
                    EntitySpinner("Ингредиент ${index + 1} · ${row.text("amount")} за применение", ingredients[index].orEmpty(), EntitySource.ITEM, enabled && supported, { ingredients = ingredients + (index to it) })
                }
            }
        }
        OutlinedTextField(amount, { amount = it }, label = { Text("Число применений, 1–100") }, singleLine = true)
        Button(enabled = enabled && supported && !loading && amount.toLongOrNull()?.let { it in 1L..100L } == true && inputs.indices.all { !ingredients[it].isNullOrBlank() }, onClick = {
            vm.useRecipe(selected, inputs.indices.map { ingredients.getValue(it) }.distinct(), amount.toLong())
        }) { Text("Использовать рецепт") }
        OutlinedButton(enabled = enabled && !loading, onClick = { refresh++ }) { Text("Обновить рецепт") }
        Text("Ингредиенты списываются сервером. После применения обновите рецепт перед следующим запуском.", color = Muted)
    }
}
