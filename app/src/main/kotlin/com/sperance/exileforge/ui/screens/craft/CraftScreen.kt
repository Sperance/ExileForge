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
import com.sperance.exileforge.core.i18n.ui
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
    LaunchedEffect(s.play.characterId, s.account.sessionEpoch) { vm.ensureHero() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(ui("craft.title"),
            ui("craft.subtitle"), ForgeGlyphs.Tome)
        if (s.play.hero == null) {
            InfoCard(ui("tree.no_hero"),
                ui("craft.hero_first"))
            return@Column
        }
        ForgePanel { RecipeForm(s, vm, enabled = !s.busy && s.account.signedIn && (s.ownsCharacter || s.isAdmin)) }
        InfoCard(ui("craft.future"),
            ui("craft.future_note"))
    }
}

@Composable private fun ColumnScope.RecipeForm(s: ForgeState, vm: ForgeViewModel, enabled: Boolean) {
    var recipeId by remember(s.play.characterId) { mutableStateOf("") }
    var recipe by remember(s.play.characterId) { mutableStateOf<JsonObject?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var ingredients by remember(recipeId, refresh) { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var amount by remember(recipeId) { mutableStateOf("1") }
    val owned = s.play.hero?.bag.orEmpty().associate { it.itemId to it.amount }
    Engraved(ui("craft.recipes"))
    EntitySpinner(ui("craft.recipe"), recipeId, EntitySource.RECIPE, enabled && !loading) { recipeId = it }
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
                    EntitySpinner(ui("craft.ingredient", index + 1, row.text("amount")),
                        ingredients[index].orEmpty(), EntitySource.ITEM, enabled) { ingredients = ingredients + (index to it) }
                }
            }
        }
        OutlinedTextField(amount, { amount = it }, label = { Text(ui("craft.uses")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = enabled && !loading && amount.toLongOrNull()?.let { it in 1L..100L } == true && inputs.indices.all { !ingredients[it].isNullOrBlank() },
            onClick = { vm.useRecipe(recipeId, inputs.indices.map { ingredients.getValue(it) }.distinct(), amount.toLong()) }) { Text(ui("craft.use")) }
        OutlinedButton(enabled = enabled && !loading, onClick = { refresh++ }) { Text(ui("craft.refresh")) }
        // The command re-reads the hero itself, so the bag on the Hero tab is right by the time it opens.
        Text(ui("craft.note"), color = Muted)
    }
}
