package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject

/**
 * The forge: what a character can make out of what they carry.
 *
 * A screen of its own rather than a fold inside the bag, because a recipe is a whole task — pick
 * one, choose what to spend on it, decide how many times — and because it is the half of the game
 * most likely to grow. It keeps no place in the bottom bar: five destinations are what a player
 * needs at hand, and the forge is somewhere you go on purpose.
 *
 * Every rule is the server's. It owns the recipe, it takes the ingredients, and it decides what
 * comes out; the client names a recipe and a list of item ids.
 */
@Composable fun CraftScreen(s: ForgeState, vm: ForgeViewModel) {
    // Ingredients are read off the bag, so the forge opens on a hero that is not stale.
    LaunchedEffect(s.characterId, s.sessionEpoch) { vm.ensureHero() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(tr("Кузница", "The forge"),
            tr("Рецепты и превращения", "Recipes and transmutations"), ForgeGlyphs.Tome)
        if (s.hero == null) {
            InfoCard(tr("Герой не загружен", "The hero is not loaded"),
                tr("Откройте вкладку «Герой» — рецепты тратят то, что лежит в сумке.",
                   "Open the Hero tab — a recipe spends what is in the bag."))
            return@Column
        }
        ForgePanel { RecipeForm(s, vm, enabled = !s.busy && s.signedIn && (s.ownsCharacter || s.isAdmin)) }
        InfoCard(tr("Что здесь будет", "What goes here"),
            tr("Пока кузница умеет только рецепты. Всё, что появится потом — разборка, улучшение, зачарование — встанет рядом, а не поверх.",
               "For now the forge only knows recipes. Whatever comes later — salvage, upgrades, enchantment — will stand beside them, not on top of them."))
    }
}

@Composable private fun ColumnScope.RecipeForm(s: ForgeState, vm: ForgeViewModel, enabled: Boolean) {
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
        // The command re-reads the hero itself, so the bag on the Hero tab is right by the time it opens.
        Text(tr("Ингредиенты списывает сервер.", "Ingredients are consumed by the server."), color = Muted)
    }
}
