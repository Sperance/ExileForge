package com.sperance.exileforge.ui.screens.craft

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
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.theme.*

/** A bench "line" that is not a recipe: taking the crafted modifier back off. */
private const val UNCRAFT = "-"

/** The bench takes a modifier only from Magic rarity up (2.51.0), same as the rules: a common item has no affix places and a fixed rarity's are closed. */
private val BENCHABLE = setOf(Rarity.UNCOMMON, Rarity.RARE)

/** An essence (2.78.0) takes a common item to rare, or rolls a rare anew. */
private val ESSENTIAL = setOf(Rarity.COMMON, Rarity.RARE)

/**
 * The forge: orbs and the bench over one item — a map gets the orbs alone, and the recipes are gone (2.47.0).
 *
 * The item is the plate on top — the one the card's «Сфера» or «Верстак» opened the forge with, or
 * the last one worked on — and a tap on it picks another from the stash. Below it the section's
 * ledger: every orb in the bag with what it does and how many are left, or every bench line for
 * the item's slot. What is chosen sits in a bar over the navigation with a button that is held,
 * because an orb is spent the moment it is used; the bar stays, so the same orb can be spent again.
 *
 * Every rule is the server's. The client sends the pair of codes and prints the sentence that comes
 * back under the plate — including a refusal, which costs nothing — and the hero comes back with it.
 * Since 3.0.0 the copy is read through its view over the content, and orbs, essences and bench lines
 * are named by code.
 */
@Composable fun CraftScreen(s: ForgeState, vm: ForgeViewModel) {
    // Orbs and ingredients are read off the bag, so the forge opens on a hero that is not stale.
    LaunchedEffect(s.play.heroId, s.account.sessionEpoch) { vm.ensureHero() }
    val hero = s.hero
    val index = s.index
    val instance = hero?.item(s.play.selectedEquipment)
    val view = instance?.let { s.view(it) }
    val enabled = !s.busy && s.account.signedIn && (s.ownsCharacter || s.isAdmin)
    var picking by remember { mutableStateOf(false) }
    var benchLine by remember(instance?.id) { mutableStateOf("") }
    // A map takes no bench line (2.47.0): its forge is the orbs alone, with no tabs to choose between.
    val slot = view?.slot
    val isMap = slot == Slot.MAP
    val benchable = instance != null && instance.rarity in BENCHABLE
    // An essence works on gear alone: not a map, a jewel, a flask or a tool.
    val essential = slot != null && !slot.isJewelLike && !slot.isFlask && !slot.isTool && instance != null && instance.rarity in ESSENTIAL
    val sections = listOfNotNull(ForgeSection.ORBS, ForgeSection.BENCH.takeIf { !isMap && benchable }, ForgeSection.ESSENCES.takeIf { essential })
    val section = s.play.forgeSection.takeIf { it in sections } ?: ForgeSection.ORBS
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader(ui("craft.title"), ui("craft.subtitle"), ForgeGlyphs.Anvil)
            if (hero == null || index == null) { InfoCard(ui("tree.no_hero"), ui("craft.hero_first")); return@Column }
            ForgeTarget(s, view) { picking = true }
            if (sections.size > 1) TabRow(selectedTabIndex = sections.indexOf(section), containerColor = Abyss) {
                sections.forEach { entry ->
                    Tab(selected = section == entry, onClick = { vm.forgeSection(entry) },
                        text = { Text(ui(entry.title), style = MaterialTheme.typography.labelLarge) })
                }
            }
            when (section) {
                ForgeSection.ORBS -> OrbLedger(s, vm::selectOrb)
                ForgeSection.BENCH -> view?.let { BenchLedger(s, index, hero, it, benchLine) { line -> benchLine = line } }
                ForgeSection.ESSENCES -> EssenceLedger(s, vm::selectEssence)
            }
        }
        if (hero != null && instance != null) when (section) {
            ForgeSection.ORBS -> OrbBar(s, instance, enabled, vm::applyOrb)
            ForgeSection.BENCH -> BenchBar(s, vm, instance, benchLine, enabled)
            ForgeSection.ESSENCES -> EssenceBar(s, instance, enabled, vm::applyEssence)
        }
    }
    if (picking) TargetPicker(s, onDismiss = { picking = false }) { vm.selectEquipment(it); picking = false }
}

private val ForgeSection.title get() = when (this) {
    ForgeSection.ORBS -> "forge.section_orbs"
    ForgeSection.BENCH -> "forge.section_bench"
    ForgeSection.ESSENCES -> "forge.section_essences"
}

/**
 * Every essence the bag holds (2.78.0), one line each — what it makes of a common item and of a rare one,
 * in the server's words, and how many there are; the special ones in gold.
 */
@Composable private fun EssenceLedger(s: ForgeState, onSelect: (String) -> Unit) {
    val hero = s.hero ?: return
    val index = s.index ?: return
    val essences = index.itemsByCategory[Item.ESSENCE].orEmpty().filter { hero.count(it.code) > 0 }
        .sortedWith(compareBy({ index.essence(it.code)?.special == true }, { -(index.essence(it.code)?.tier ?: 0) }))
    if (essences.isEmpty()) { Text(ui("forge.no_essences"), color = Muted); return }
    Column {
        essences.forEach { essence ->
            val special = index.essence(essence.code)?.special == true
            LedgerRow(ForgeGlyphs.Shard, if (special) GoldBright else Elder, itemTitle(essence.code), itemDescription(essence.code),
                hero.count(essence.code).toString(), selected = essence.code == s.play.selectedEssence) { onSelect(essence.code) }
        }
    }
    MutedText(ui("essence.note"))
}

/** The chosen essence over the navigation, with the held button: a common item becomes rare, a rare one is rolled anew. */
@Composable private fun EssenceBar(s: ForgeState, instance: ItemInstance, enabled: Boolean, onApply: (String, String) -> Unit) {
    val code = s.play.selectedEssence
    val owned = s.bagAmount(code) ?: 0L
    val essence = s.index?.essence(code)?.takeIf { owned > 0 }
    ForgeBar {
        if (essence == null) { Text(ui("forge.pick_essence"), color = Muted); return@ForgeBar }
        BarTitle(ForgeGlyphs.Shard, Elder, itemTitle(code), stock(owned, 1))
        HoldButton(ui("confirm.hold", ui("forge.apply_essence")), Elder, enabled = enabled && !instance.corrupted, rearm = true) {
            onApply(instance.id, code)
        }
    }
}

/** The item being worked on, as the stash draws it, with the server's last word about it underneath. */
@Composable fun ForgeTarget(s: ForgeState, item: ItemView?, onPick: () -> Unit) {
    if (item == null) {
        Column(Modifier.fillMaxWidth().border(1.dp, Bronze.copy(alpha = .6f), MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onPick).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ui("forge.pick_item"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            MutedText(ui("forge.pick_item_hint"))
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ItemRow(item, price = s.sellPrice(item.item), onClick = onPick)
        if (item.corrupted) Text(ui("orb.corrupted"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
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
    val hero = s.hero ?: return
    // Regret is spent on the tree, never on an item, so it has no line here.
    val orbs = s.orbs.filter { hero.count(it.code) > 0 && it.code != Orb.ORB_OF_REGRET.name }
    if (orbs.isEmpty()) { Text(ui("forge.no_orbs"), color = Muted); return }
    Column {
        orbs.forEach { orb ->
            LedgerRow(ForgeGlyphs.Orb, Gold, itemTitle(orb.code), itemDescription(orb.code), hero.count(orb.code).toString(),
                selected = orb.code == s.play.selectedOrb, orb = Orb.of(orb.code)) { onSelect(orb.code) }
        }
    }
}

/**
 * The bench lines for this item's slot, and the crafted modifier it already carries, if any.
 * Only the recipes the hero has found are offered (3.0.0); the rest of the bench stays hidden.
 */
@Composable private fun BenchLedger(s: ForgeState, index: ContentIndex, hero: HeroView, item: ItemView, chosen: String, onChoose: (String) -> Unit) {
    val recipes = s.bench.filter { it.fits(item.slot) }.sortedWith(compareBy({ it.source }, { it.modifier }, { -it.tier }))
    val crafted = item.lines.firstOrNull { it.marks.crafted }
    val scouring = index.rules.bench.uncraftOrb
    Column {
        crafted?.let {
            LedgerRow(ForgeGlyphs.Anvil, Crafted, it.text,
                ui("bench.current") + " · " + ui("bench.cost_line", itemTitle(scouring.name), 1, hero.count(scouring.name)),
                "×", selected = chosen == UNCRAFT, ink = ModBlue) { onChoose(UNCRAFT) }
        }
        recipes.forEach { recipe ->
            LedgerRow(ForgeGlyphs.Anvil, Crafted, recipeText(index, recipe),
                ui("bench.cost_line", itemTitle(recipe.orb.name), recipe.amount, hero.count(recipe.orb.name)),
                "T${recipe.tier}", selected = chosen == recipe.code, ink = ModBlue) { onChoose(recipe.code) }
        }
    }
    if (recipes.isEmpty()) Text(ui("bench.none"), color = Muted)
}

/** One line of a forge ledger: a spine lit when chosen, a drawing, a name over what it means, and a figure. */
@Composable private fun LedgerRow(icon: ImageVector, accent: Color, title: String, subtitle: String, figure: String,
    selected: Boolean, orb: Orb? = null, ink: Color = Parchment, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable(role = Role.Button, onClick = onClick)
        .background(if (selected) Panel else Color.Transparent),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        RaritySpine(if (selected) GoldBright else accent.copy(alpha = .35f), 3.dp)
        // An orb's line wears its stained glass (2.69.0); the bench and the rest keep their glyph.
        if (orb != null) OrbGlyph(orb, Modifier.size(28.dp))
        else Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f).padding(vertical = 9.dp)) {
            Text(title, color = if (selected) GoldBright else ink, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(figure, color = GoldBright, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 4.dp))
    }
    HorizontalDivider(color = PanelRaised)
}

/** The chosen orb over the navigation: what it does, what the bag keeps, and the button that is held. */
@Composable fun OrbBar(s: ForgeState, instance: ItemInstance, enabled: Boolean, onApply: (String, String) -> Unit) {
    val code = s.play.selectedOrb
    val owned = s.bagAmount(code) ?: 0L
    val orb = s.orbs.firstOrNull { it.code == code && owned > 0 }
    ForgeBar {
        if (orb == null) { Text(ui("forge.pick_orb"), color = Muted); return@ForgeBar }
        BarTitle(ForgeGlyphs.Orb, Gold, itemTitle(orb.code), stock(owned, 1), orb = Orb.of(orb.code))
        HoldButton(ui("confirm.hold", ui("forge.apply_orb")), Gold, enabled = enabled && !instance.corrupted, rearm = true) {
            onApply(instance.id, orb.code)
        }
    }
}

/** The chosen bench line over the navigation, priced, with the same held button. */
@Composable private fun BenchBar(s: ForgeState, vm: ForgeViewModel, instance: ItemInstance, chosen: String, enabled: Boolean) {
    val index = s.index ?: return
    val recipe = s.bench.firstOrNull { it.code == chosen }
    ForgeBar {
        when {
            chosen == UNCRAFT -> {
                val scouring = index.rules.bench.uncraftOrb
                val owned = s.bagAmount(scouring.name) ?: 0L
                BarTitle(ForgeGlyphs.Anvil, Crafted, ui("bench.remove"), stock(owned, 1))
                HoldButton(ui("confirm.hold", ui("forge.remove_bench")), Crafted, enabled = enabled && owned >= 1, rearm = true) { vm.uncraft(instance.id) }
            }
            recipe != null -> {
                val owned = s.bagAmount(recipe.orb.name) ?: 0L
                BarTitle(ForgeGlyphs.Anvil, Crafted, recipeText(index, recipe), stock(owned, recipe.amount))
                HoldButton(ui("confirm.hold", ui("forge.apply_bench")), Crafted, enabled = enabled && owned >= recipe.amount, rearm = true) { vm.craft(instance.id, recipe.code) }
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

@Composable private fun BarTitle(icon: ImageVector, accent: Color, title: String, stock: Pair<String, Boolean>, orb: Orb? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (orb != null) OrbGlyph(orb, Modifier.size(34.dp))
        else Icon(icon, null, tint = accent, modifier = Modifier.size(30.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = GoldBright, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stock.first, color = if (stock.second) LifeRed else Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** The stash, whole, to pick what the forge works on; worn items included, since an orb does not care. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TargetPicker(s: ForgeState, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val stash = s.hero?.items.orEmpty().mapNotNull { s.view(it) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.85f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("forge.pick_item")) }
            if (stash.isEmpty()) item { Text(ui("forge.stash_empty"), color = Muted) }
            items(stash, key = { it.id }) { piece ->
                ItemRow(piece, selected = piece.id == s.play.selectedEquipment,
                    facts = if (piece.equipped || piece.socketed) listOf(ui("hero.equipped")) else emptyList(), price = s.sellPrice(piece.item)) { onPick(piece.id) }
            }
        }
    }
}
