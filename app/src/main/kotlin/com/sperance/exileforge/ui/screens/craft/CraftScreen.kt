package com.sperance.exileforge.ui.screens.craft

import com.sperance.exileforge.presentation.state.sellPrice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.affixMarks
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.modifierText
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.currency.CurrencyOrb
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** A bench "line" that is not a recipe: taking the crafted modifier back off. */
private const val UNCRAFT = "-"

/**
 * The forge: orbs and the bench over one item, and recipes over the bag.
 *
 * The item is the plate on top — the one the card's «Сфера» or «Верстак» opened the forge with, or
 * the last one worked on — and a tap on it picks another from the stash. Below it the section's
 * ledger: every orb in the bag with what it does and how many are left, or every bench line for
 * the item's slot. What is chosen sits in a bar over the navigation with a button that is held,
 * because an orb is spent the moment it is used; the bar stays, so the same orb can be spent again.
 *
 * Every rule is the server's. The client sends the pair and prints the sentence that comes back
 * under the plate — including a refusal, which costs nothing — and re-reads the item.
 */
@Composable fun CraftScreen(s: ForgeState, vm: ForgeViewModel) {
    // Orbs and ingredients are read off the bag, so the forge opens on a hero that is not stale.
    LaunchedEffect(s.play.characterId, s.account.sessionEpoch) { vm.ensureHero() }
    val hero = s.play.hero
    val section = s.play.forgeSection
    val instance = hero?.inventory?.firstOrNull { it.id == s.play.selectedEquipment }
    val enabled = !s.busy && s.account.signedIn && (s.ownsCharacter || s.isAdmin)
    var picking by remember { mutableStateOf(false) }
    var benchLine by remember(instance?.id) { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader(ui("craft.title"), ui("craft.subtitle"), ForgeGlyphs.Anvil)
            if (hero == null) { InfoCard(ui("tree.no_hero"), ui("craft.hero_first")); return@Column }
            if (section != ForgeSection.RECIPES) ForgeTarget(s, instance) { picking = true }
            TabRow(selectedTabIndex = section.ordinal, containerColor = Abyss) {
                ForgeSection.entries.forEach { entry ->
                    Tab(selected = section == entry, onClick = { vm.forgeSection(entry) },
                        text = { Text(ui(entry.title), style = MaterialTheme.typography.labelLarge) })
                }
            }
            when (section) {
                ForgeSection.ORBS -> OrbLedger(s, vm::selectOrb)
                ForgeSection.BENCH -> instance?.let { BenchLedger(s, it, benchLine) { line -> benchLine = line } }
                ForgeSection.RECIPES -> ForgePanel { RecipeForm(s, vm, enabled) }
            }
        }
        if (hero != null && instance != null) when (section) {
            ForgeSection.ORBS -> OrbBar(s, instance, enabled, vm::applyOrb)
            ForgeSection.BENCH -> BenchBar(s, vm, instance, benchLine, enabled)
            ForgeSection.RECIPES -> Unit
        }
    }
    if (picking) TargetPicker(s, onDismiss = { picking = false }) { vm.selectEquipment(it); picking = false }
}

private val ForgeSection.title get() = when (this) {
    ForgeSection.ORBS -> "forge.section_orbs"
    ForgeSection.BENCH -> "forge.section_bench"
    ForgeSection.RECIPES -> "forge.section_recipes"
}

/** The item being worked on, as the stash draws it, with the server's last word about it underneath. */
@Composable fun ForgeTarget(s: ForgeState, instance: EquipmentInstance?, onPick: () -> Unit) {
    if (instance == null) {
        Column(Modifier.fillMaxWidth().border(1.dp, Bronze.copy(alpha = .6f), MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onPick).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ui("forge.pick_item"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            Text(ui("forge.pick_item_hint"), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    val document = inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId])
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ItemRow(document, s.world.definitions, price = s.sellPrice(instance), onClick = onPick)
        if (instance.corrupted) Text(ui("orb.corrupted"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
        s.play.forgeLine.takeIf { it.isNotBlank() }?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(ForgeGlyphs.Sigil, null, tint = Rune, modifier = Modifier.size(14.dp))
                Text(it, color = Rune, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Every orb the bag holds, one line each: what it does and how many there are. */
@Composable fun OrbLedger(s: ForgeState, onSelect: (String) -> Unit) {
    val owned = s.play.hero?.bag.orEmpty().associate { it.itemId to it.amount }
    // Regret is spent on the tree, never on an item, so it has no line here.
    val orbs = s.world.orbs.filter { (owned[it.id] ?: 0L) > 0 && it.orb != CurrencyOrb.ORB_OF_REGRET }
    if (orbs.isEmpty()) { Text(ui("forge.no_orbs"), color = Muted); return }
    Column {
        orbs.forEach { orb ->
            LedgerRow(ForgeGlyphs.Orb, Gold, orb.title(s.lang), orb.details(s.lang), (owned[orb.id] ?: 0L).toString(),
                selected = orb.id == s.play.selectedOrb) { onSelect(orb.id) }
        }
    }
    Text(ui("orb.note"), color = Muted, style = MaterialTheme.typography.bodySmall)
}

/** The bench lines for this item's slot, and the crafted modifier it already carries, if any. */
@Composable private fun BenchLedger(s: ForgeState, instance: EquipmentInstance, chosen: String, onChoose: (String) -> Unit) {
    val document = inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId])
    val owned = s.play.hero?.bag.orEmpty().associate { it.itemId to it.amount }
    val recipes = s.world.bench.filter { it.fits(document.text("slot")) }
        .sortedWith(compareBy({ it.source }, { it.modifierCode }, { -it.tier }))
    val crafted = craftedModifier(s, document)
    Column {
        crafted?.let {
            val scouring = s.world.orbs.firstOrNull { orb -> orb.orb == CurrencyOrb.ORB_OF_SCOURING }
            LedgerRow(ForgeGlyphs.Anvil, Crafted, modifierText(it, s.world.definitions),
                ui("bench.current") + " · " + ui("bench.cost_line", orbTitle(s, CurrencyOrb.ORB_OF_SCOURING.name), 1, owned[scouring?.id] ?: 0L),
                "×", selected = chosen == UNCRAFT) { onChoose(UNCRAFT) }
        }
        recipes.forEach { recipe ->
            LedgerRow(ForgeGlyphs.Anvil, Crafted, recipeText(recipe, s.world.definitions),
                ui("bench.cost_line", orbTitle(s, recipe.orb), recipe.amount, owned[recipe.orbItemId] ?: 0L),
                "T${recipe.tier}", selected = chosen == recipe.code) { onChoose(recipe.code) }
        }
    }
    if (recipes.isEmpty()) Text(ui("bench.none"), color = Muted)
    Text(ui("bench.note"), color = Muted, style = MaterialTheme.typography.bodySmall)
}

/** One line of a forge ledger: a spine lit when chosen, a drawing, a name over what it means, and a figure. */
@Composable private fun LedgerRow(icon: ImageVector, accent: Color, title: String, subtitle: String, figure: String,
    selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable(role = Role.Button, onClick = onClick)
        .background(if (selected) Panel else Color.Transparent),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        RaritySpine(if (selected) GoldBright else accent.copy(alpha = .35f), 3.dp)
        Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f).padding(vertical = 9.dp)) {
            Text(title, color = if (selected) GoldBright else Parchment, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(figure, color = GoldBright, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 4.dp))
    }
    HorizontalDivider(color = PanelRaised)
}

/** The chosen orb over the navigation: what it does, what the bag keeps, and the button that is held. */
@Composable fun OrbBar(s: ForgeState, instance: EquipmentInstance, enabled: Boolean, onApply: (String, String) -> Unit) {
    val owned = s.play.hero?.bag.orEmpty().associate { it.itemId to it.amount }
    val orb = s.world.orbs.firstOrNull { it.id == s.play.selectedOrb && (owned[it.id] ?: 0L) > 0 }
    ForgeBar {
        if (orb == null) { Text(ui("forge.pick_orb"), color = Muted); return@ForgeBar }
        BarTitle(ForgeGlyphs.Orb, Gold, orb.title(s.lang), stock(owned[orb.id] ?: 0L, 1))
        HoldButton(ui("confirm.hold", ui("forge.apply_orb")), Gold, enabled = enabled && !instance.corrupted, rearm = true) {
            onApply(instance.id, orb.id)
        }
    }
}

/** The chosen bench line over the navigation, priced, with the same held button. */
@Composable private fun BenchBar(s: ForgeState, vm: ForgeViewModel, instance: EquipmentInstance, chosen: String, enabled: Boolean) {
    val owned = s.play.hero?.bag.orEmpty().associate { it.itemId to it.amount }
    val recipe = s.world.bench.firstOrNull { it.code == chosen }
    ForgeBar {
        when {
            chosen == UNCRAFT -> {
                val scouring = s.world.orbs.firstOrNull { it.orb == CurrencyOrb.ORB_OF_SCOURING }
                BarTitle(ForgeGlyphs.Anvil, Crafted, ui("bench.remove"), stock(owned[scouring?.id] ?: 0L, 1))
                HoldButton(ui("confirm.hold", ui("forge.remove_bench")), Crafted, enabled = enabled && (owned[scouring?.id] ?: 0L) >= 1, rearm = true) { vm.uncraft(instance.id) }
            }
            recipe != null -> {
                BarTitle(ForgeGlyphs.Anvil, Crafted, recipeText(recipe, s.world.definitions), stock(owned[recipe.orbItemId] ?: 0L, recipe.amount))
                HoldButton(ui("confirm.hold", ui("forge.apply_bench")), Crafted, enabled = enabled && (owned[recipe.orbItemId] ?: 0L) >= recipe.amount, rearm = true) { vm.craft(instance.id, recipe.code) }
            }
            else -> Text(ui("forge.pick_line"), color = Muted)
        }
    }
}

/**
 * What the bag keeps after one use. It is printed only when the bag can pay; when it cannot, the
 * bar says so in red and the button stays off (2.46.0).
 */
private fun stock(have: Long, need: Long): Pair<String, Boolean> =
    if (have >= need) ui("forge.orb_left", have, have - need) to false else ui("forge.short", have) to true

@Composable private fun ForgeBar(content: @Composable ColumnScope.() -> Unit) {
    HorizontalDivider(color = Bronze)
    Column(Modifier.fillMaxWidth().background(Abyss).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable private fun BarTitle(icon: ImageVector, accent: Color, title: String, stock: Pair<String, Boolean>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(30.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = GoldBright, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stock.first, color = if (stock.second) LifeRed else Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** The stash, whole, to pick what the forge works on; worn items included, since an orb does not care. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TargetPicker(s: ForgeState, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val inventory = s.play.hero?.inventory.orEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.85f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("forge.pick_item")) }
            if (inventory.isEmpty()) item { Text(ui("forge.stash_empty"), color = Muted) }
            items(inventory, key = { it.id }) { instance ->
                ItemRow(inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId]), s.world.definitions,
                    selected = instance.id == s.play.selectedEquipment,
                    facts = if (instance.equipped || instance.socketed) listOf(ui("hero.equipped")) else emptyList(), price = s.sellPrice(instance)) { onPick(instance.id) }
            }
        }
    }
}

private fun craftedModifier(s: ForgeState, document: JsonObject): JsonObject? =
    (document["params"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        .firstOrNull { affixMarks(it, s.world.definitions).crafted }

private fun orbTitle(s: ForgeState, code: String) = s.world.orbs.firstOrNull { it.subCategory == code }?.title(s.lang)
    ?: CurrencyOrb.of(code)?.title(s.lang) ?: code

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
        val inputs = (selected["arrayIn"] as? JsonArray).orEmpty()
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
