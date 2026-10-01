package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.OrbApplier
import com.sperance.exileforge.rules.roll.OrbTarget
import com.sperance.exileforge.rules.roll.Roll
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.theme.*

/** A bench "line" that is not a recipe: taking the crafted modifier back off. */
private const val UNCRAFT = "-"

/** The bench takes a modifier only from Magic rarity up (2.51.0), same as the rules: a common item has no affix places and a fixed rarity's are closed. */
private val BENCHABLE = setOf(Rarity.MAGIC, Rarity.RARE)

/** An essence (2.78.0) takes a common item to rare, or rolls a rare anew. */
private val ESSENTIAL = setOf(Rarity.COMMON, Rarity.RARE)

/**
 * The forge as an anvil (the owner's mockup B, 3.x): a rail of the stash's shelves and the items worked on lately beside the anvil,
 * which holds the item, the orb, essence or bench line laid on it and, for an orb, its omen — and shows the item as it stands.
 * Under it the section's tray: a compact grid of only what the bag holds and the item takes, or the bench's lines. What is laid
 * sits in a bar over the navigation with a button that is held, because an orb is spent the moment it is used; the bar stays,
 * so the same orb can be spent again.
 *
 * Every rule is the server's. The client sends the pair of codes and prints the sentence that comes back on the anvil — including
 * a refusal, which costs nothing — and the hero comes back with it. What an item takes is the rules' own [OrbApplier.accepts].
 */
@Composable fun CraftScreen(s: ForgeState, vm: ForgeViewModel) {
    // Orbs and ingredients are read off the bag, so the forge opens on a hero that is not stale.
    LaunchedEffect(s.play.heroId, s.account.sessionEpoch) { vm.ensureHero() }
    val hero = s.hero
    val index = s.index
    val instance = hero?.item(s.play.selectedEquipment)
    val view = instance?.let { s.view(it) }
    val enabled = !s.busy && s.account.signedIn && (s.ownsCharacter || s.isAdmin)
    var picking by remember { mutableStateOf<TargetFilter?>(null) }
    var benchLine by remember(instance?.id) { mutableStateOf("") }
    // The items worked on lately, newest first: kept while the hero is, so the rail and the picker's «Недавние» lead with them.
    var recentIds by rememberSaveable(s.play.heroId) { mutableStateOf("") }
    LaunchedEffect(instance?.id) {
        instance?.id?.let { id -> recentIds = (listOf(id) + recentIds.split(',')).filter { it.isNotBlank() }.distinct().take(RECENT_TARGETS).joinToString(",") }
    }
    val recent = recentIds.split(',').filter { it.isNotBlank() && hero?.item(it) != null }
    // A map takes no bench line (2.47.0): its forge is the orbs alone, with no tabs to choose between.
    val slot = view?.slot
    val isMap = slot == Slot.MAP
    val benchable = instance != null && instance.rarity in BENCHABLE
    // An essence works on gear alone: not a map, a jewel, a flask or a tool.
    val essential = slot != null && !slot.isJewelLike && !slot.isFlask && !slot.isTool && instance != null && instance.rarity in ESSENTIAL
    // The omens in the bag: an orb that goes on this item only with one of them (a catalyst's Orb of Quality on a ring) is offered too.
    val omens: List<Omen> = remember(index, hero?.bag) { if (hero == null || index == null) emptyList() else heldOmens(index, hero) }
    val gear = remember(instance, view) { if (instance != null && view != null) OrbTarget.Gear(instance, view.template) else null }
    // Only what goes on this item is offered (3.2.0): each orb and essence tried over a copy by the rules, a refusal left out;
    // an orb the item takes only under an omen is marked, so the bar waits for one.
    val (refused, omenOnly) = remember(gear, index, omens) {
        if (index == null || gear == null) emptySet<String>() to emptySet<String>() else {
            val applier = OrbApplier(index)
            val bare = Orb.entries.filter { applier.accepts(it, gear) }.toSet()
            val paired = Orb.entries.filter { orb -> orb !in bare && omens.any { it.fits(orb) && applier.accepts(orb, gear, it) } }.toSet()
            (Orb.entries.filter { it !in bare && it !in paired }.map { it.name } +
                index.essences.essences.values.filterNot { applier.accepts(it, gear.item, gear.template) }.map { it.code }).toSet() to paired.map { it.name }.toSet()
        }
    }
    val accepted: (String) -> Boolean = { it !in refused }
    val sections = listOfNotNull(ForgeSection.ORBS, ForgeSection.BENCH.takeIf { !isMap && benchable }, ForgeSection.ESSENCES.takeIf { essential })
    val section = s.play.forgeSection.takeIf { it in sections } ?: ForgeSection.ORBS
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader(ui("craft.title"), ui("craft.subtitle"), ForgeGlyphs.Anvil, guide = Guide.FORGE)
            if (hero == null || index == null) { InfoCard(ui("tree.no_hero"), ui("craft.hero_first")); return@Column }
            if (sections.size > 1) PillTabs(sections.map { ui(it.title) }, sections.indexOf(section), { vm.forgeSection(sections[it]) }, segmented = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetRail(s, recent.mapNotNull { id -> hero.item(id)?.let { s.view(it) } }, instance?.id, { picking = it }, vm::selectEquipment)
                Anvil(s, view, toolSocket(s, section, index, benchLine, accepted), omenSocket(s, section, vm::selectOmen),
                    onPick = { picking = TargetFilter.ALL }, modifier = Modifier.weight(1f))
            }
            when (section) {
                ForgeSection.ORBS -> {
                    instance?.let {
                        LineChoice(s, it, it.unveil, "forge.unveil_title", "forge.unveil_hint", enabled, vm::unveil)
                        LineChoice(s, it, it.offer, "forge.choice_title", "forge.choice_hint", enabled, vm::choose)
                    }
                    gear?.let { OmenLedger(s, it, omens, vm::selectOmen) }
                    if (instance != null) OrbTray(s, accepted, { it in omenOnly }, vm::selectOrb)
                }
                ForgeSection.BENCH -> view?.let { BenchLedger(s, index, hero, it, benchLine) { line -> benchLine = line } }
                ForgeSection.ESSENCES -> EssenceTray(s, accepted, vm::selectEssence)
            }
        }
        if (hero != null && instance != null) when (section) {
            ForgeSection.ORBS -> OrbBar(s, instance, enabled, accepted, { it in omenOnly }, vm::applyOrb)
            ForgeSection.BENCH -> BenchBar(s, vm, instance, benchLine, enabled)
            ForgeSection.ESSENCES -> EssenceBar(s, instance, enabled, accepted, vm::applyEssence)
        }
    }
    picking?.let { shelf -> TargetPicker(s, recent, shelf, onDismiss = { picking = null }) { vm.selectEquipment(it); picking = null } }
}

/** The anvil's tool socket: the orb, the essence or the bench line the section lays on the item, when one is chosen and fits. */
private fun toolSocket(s: ForgeState, section: ForgeSection, index: ContentIndex, benchLine: String, accepted: (String) -> Boolean): Socket = when (section) {
    ForgeSection.ORBS -> {
        val orb = Orb.of(s.play.selectedOrb)?.takeIf { (s.bagAmount(it.name) ?: 0L) > 0 && accepted(it.name) }
        val glyph: (@Composable () -> Unit)? = if (orb == null) null else { { OrbGlyph(orb, Modifier.fillMaxSize()) } }
        Socket(orb?.let { itemTitle(it.name) } ?: ui("forge.socket_orb"), Gold, glyph)
    }
    ForgeSection.ESSENCES -> {
        val code = s.play.selectedEssence.takeIf { index.essence(it) != null && (s.bagAmount(it) ?: 0L) > 0 && accepted(it) }
        val glyph: (@Composable () -> Unit)? = if (code == null) null else { { BagIcon(code, Modifier.fillMaxSize(), tint = Elder) } }
        Socket(code?.let(::itemTitle) ?: ui("forge.socket_essence"), Elder, glyph)
    }
    ForgeSection.BENCH -> {
        val glyph: (@Composable () -> Unit)? = if (benchLine.isBlank()) null else { { Icon(ForgeGlyphs.Anvil, null, tint = Crafted, modifier = Modifier.fillMaxSize()) } }
        Socket(ui(if (benchLine == UNCRAFT) "bench.remove" else "forge.section_bench"), Crafted, glyph)
    }
}

/** The omen's socket beside an orb: the omen laid with it, a tap taking it off; none for the bench or an essence. */
private fun omenSocket(s: ForgeState, section: ForgeSection, onSelect: (String) -> Unit): Socket? {
    if (section != ForgeSection.ORBS) return null
    val omen = s.play.selectedOmen.takeIf { it.isNotBlank() && (s.bagAmount(it) ?: 0L) > 0 }
    val glyph: (@Composable () -> Unit)? = if (omen == null) null else { { Icon(ForgeGlyphs.Sigil, null, tint = Rune, modifier = Modifier.fillMaxSize()) } }
    return Socket(omen?.let(::itemTitle) ?: ui("forge.omen"), Rune, glyph, if (omen == null) null else { { onSelect("") } })
}

/** The chosen essence over the navigation, with the held button: a common item becomes rare, a rare one is rolled anew. */
@Composable private fun EssenceBar(s: ForgeState, instance: ItemInstance, enabled: Boolean, accepted: (String) -> Boolean, onApply: (String, String) -> Unit) {
    val code = s.play.selectedEssence
    val owned = s.bagAmount(code) ?: 0L
    val essence = s.index?.essence(code)?.takeIf { owned > 0 && accepted(code) }
    ForgeBar {
        if (essence == null) { Text(ui("forge.pick_essence"), color = Muted); return@ForgeBar }
        BarTitle(ForgeGlyphs.Shard, Elder, itemTitle(code), stock(owned, 1))
        HoldButton(ui("confirm.hold", ui("forge.apply_essence")), Elder, enabled = enabled && !instance.corrupted, rearm = true) {
            onApply(instance.id, code)
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

/** The omens the bag holds (3.36.0; server 1.65.0: the catalysts are omens of the Orb of Quality), in the content's order. */
internal fun heldOmens(index: ContentIndex, hero: HeroView): List<Omen> =
    index.itemsByCategory[Item.OMEN].orEmpty().mapNotNull { item -> Omen.of(item.code)?.takeIf { hero.count(item.code) > 0 } }

/**
 * A choice of lines waiting on the item: the unveiling's offer (3.36.0) — each modifier the veiled one may become — or the
 * Omen of Choice's (server 1.65.0) — each line an Orb of Alchemy or an Exalted Orb may add. One tap keeps it and the rest are lost.
 */
@Composable private fun LineChoice(s: ForgeState, instance: ItemInstance, options: List<Roll>, title: String, hint: String, enabled: Boolean,
    onChoose: (String, Int) -> Unit) {
    if (options.isEmpty()) return
    ChoiceFrame(title, hint) {
        options.forEachIndexed { i, option ->
            val text = s.view(instance.copy(rolls = listOf(option), unveil = emptyList(), offer = emptyList()))?.lines?.firstOrNull()?.text.orEmpty()
            ChoiceRow(text, "T${option.tier}") { if (enabled) onChoose(instance.id, i) }
        }
    }
}

/** The frame of a choice of lines — an item's or a pet's: the title, how it works, and the options under them. */
@Composable fun ChoiceFrame(title: String, hint: String, options: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().border(1.dp, Rune, MaterialTheme.shapes.small).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ui(title), color = Rune, style = MaterialTheme.typography.titleMedium)
        MutedText(ui(hint))
        options()
    }
}

/** One option of a [ChoiceFrame]: the line as it would read, and a figure beside it. */
@Composable fun ChoiceRow(text: String, figure: String, onClick: () -> Unit) =
    LedgerRow(ForgeGlyphs.Sigil, Rune, text, "", figure, selected = false, ink = ModBlue, onClick = onClick)

/** The chosen orb over the navigation: what it does, what the bag keeps, and the button that is held. */
@Composable internal fun OrbBar(s: ForgeState, instance: ItemInstance, enabled: Boolean, accepted: (String) -> Boolean, needsOmen: (String) -> Boolean,
    onApply: (String, String) -> Unit) {
    val code = s.play.selectedOrb
    val owned = s.bagAmount(code) ?: 0L
    val orb = s.orbs.firstOrNull { it.code == code && owned > 0 && accepted(code) }
    ForgeBar {
        if (orb == null) { Text(ui("forge.pick_orb"), color = Muted); return@ForgeBar }
        // An orb the item takes only under an omen (a catalyst's Orb of Quality) waits for one: alone the server would refuse it.
        val waiting = needsOmen(orb.code) && s.play.selectedOmen.isBlank()
        BarTitle(ForgeGlyphs.Orb, Gold, itemTitle(orb.code) + s.play.selectedOmen.takeIf { it.isNotBlank() }?.let { " + ${itemTitle(it)}" }.orEmpty(),
            if (waiting) ui("forge.needs_omen") to true else stock(owned, 1), orb = Orb.of(orb.code))
        HoldButton(ui("confirm.hold", ui("forge.apply_orb")), Gold, enabled = enabled && !instance.corrupted && !waiting, rearm = true) {
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
