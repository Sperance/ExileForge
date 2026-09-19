package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject

/** The bag, the promo codes and the recipes: everything the character owns that is not equipment. */
@Composable fun BagPanel(s: ForgeState, vm: ForgeViewModel) {
    val hero = s.hero ?: return
    var expanded by remember(s.characterId) { mutableStateOf(false) }
    val enabled = !s.busy && s.signedIn && (s.ownsCharacter || s.isAdmin)
    TextButton(onClick = { expanded = !expanded }) {
        Text(tr("Сумка, награды и рецепты · ${if (expanded) "свернуть" else "показать"}", "Bag, rewards and recipes · ${if (expanded) "hide" else "show"}"))
    }
    if (!expanded) return
    ForgePanel {
        Engraved(tr("Сумка", "Bag"))
        if (hero.bag.isEmpty()) Text(tr("Сумка пуста", "The bag is empty"), color = Muted)
        // Currency is named from the catalogue the hero screen already read; anything else is an id.
        hero.bag.forEach { item ->
            val orb = s.orbs.firstOrNull { it.id == item.itemId }
            PropertyRow(orb?.title(s.lang) ?: (tr("Предмет", "Item") + " …${item.itemId.takeLast(6)}"), item.amount.toString(), "item")
        }

        OrnateDivider()
        Engraved(tr("Промокод", "Promo code"))
        var code by remember(s.characterId) { mutableStateOf("") }
        OutlinedTextField(code, { code = it.take(100) }, label = { Text(tr("Промокод", "Promo code")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = enabled && code.isNotBlank(), onClick = { vm.redeem(code) }) { Text(tr("Получить награду", "Claim the reward")) }

        OrnateDivider()
        RecipeForm(s, vm, enabled)
    }
}

@Composable private fun RecipeForm(s: ForgeState, vm: ForgeViewModel, enabled: Boolean) {
    var recipeId by remember(s.characterId) { mutableStateOf("") }
    var recipe by remember(s.characterId) { mutableStateOf<JsonObject?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var ingredients by remember(recipeId, refresh) { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var amount by remember(recipeId) { mutableStateOf("1") }
    val owned = s.hero?.bag.orEmpty().associate { it.itemId to it.amount }
    Engraved(tr("Рецепты", "Recipes"))
    EntitySpinner(tr("Рецепт", "Recipe"), recipeId, EntitySource.RECIPE, enabled && !loading) { recipeId = it }
    LaunchedEffect(recipeId, refresh) {
        recipe = null; failure = null
        if (recipeId.isNotBlank()) {
            loading = true
            try { recipe = vm.recipeDocument(recipeId) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { failure = e.message }
            finally { loading = false }
        }
    }
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    recipe?.let { selected ->
        val inputs = (selected["arrayIn"] as? kotlinx.serialization.json.JsonArray).orEmpty()
        Text(selected.text("name"))
        inputs.forEachIndexed { index, input ->
            val row = input as JsonObject
            val loader = LocalEntityPageLoader.current
            key(recipeId, index, refresh) {
                // Only what the character actually owns can be spent, and only of the required kind.
                CompositionLocalProvider(LocalEntityPageLoader provides { source, page, query ->
                    val result = loader(source, page, query)
                    result.copy(items = result.items.filter { item ->
                        val matches = when {
                            row.text("itemId").isNotBlank() -> item.entityId == row.text("itemId")
                            row.text("category").isNotBlank() -> item.text("category") == row.text("category")
                            else -> item.text("subCategory") == row.text("subCategory")
                        }
                        matches && (owned[item.entityId] ?: 0L) > 0
                    })
                }) {
                    EntitySpinner(tr("Ингредиент ${index + 1} · ${row.text("amount")} за применение", "Ingredient ${index + 1} · ${row.text("amount")} per use"),
                        ingredients[index].orEmpty(), EntitySource.ITEM, enabled) { ingredients = ingredients + (index to it) }
                }
            }
        }
        OutlinedTextField(amount, { amount = it }, label = { Text(tr("Число применений, 1–100", "Number of uses, 1–100")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = enabled && !loading && amount.toLongOrNull()?.let { it in 1L..100L } == true && inputs.indices.all { !ingredients[it].isNullOrBlank() },
            onClick = { vm.useRecipe(recipeId, inputs.indices.map { ingredients.getValue(it) }.distinct(), amount.toLong()) }) { Text(tr("Использовать рецепт", "Use the recipe")) }
        OutlinedButton(enabled = enabled && !loading, onClick = { refresh++ }) { Text(tr("Обновить рецепт", "Refresh the recipe")) }
        Text(tr("Ингредиенты списывает сервер. После применения обновите героя.", "Ingredients are consumed by the server. Refresh the hero afterwards."), color = Muted)
    }
}
