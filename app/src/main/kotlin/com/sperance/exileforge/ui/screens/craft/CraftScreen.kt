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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.presentation.forge.Smithy
import com.sperance.exileforge.presentation.forge.SmithyViewModel
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
import org.koin.compose.viewmodel.koinViewModel

/** A bench "line" that is not a recipe: taking the crafted modifier back off. */
internal const val UNCRAFT = "-"

/** The bench takes a modifier only from Magic rarity up (2.51.0), same as the rules: a common item has no affix places and a fixed rarity's are closed. */
internal val BENCHABLE = setOf(Rarity.MAGIC, Rarity.RARE)

/** An essence (2.78.0) takes a common item to rare, or rolls a rare anew. */
internal val ESSENTIAL = setOf(Rarity.COMMON, Rarity.RARE)

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
@Composable fun CraftScreen(s: ForgeState) {
    val vm = koinViewModel<SmithyViewModel>()
    val smithy by vm.smithy.collectAsStateWithLifecycle()
    // Orbs and ingredients are read off the bag, so the forge opens on a hero that is not stale.
    LaunchedEffect(s.play.heroId, s.account.sessionEpoch) { vm.ensure() }
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
        if (index == null || gear == null) {
            emptySet<String>() to emptySet<String>()
        } else {
            val applier = OrbApplier(index)
            val bare = Orb.entries.filter { applier.accepts(it, gear) }.toSet()
            val paired = Orb.entries.filter { orb -> orb !in bare && omens.any { it.fits(orb) && applier.accepts(orb, gear, it) } }.toSet()
            (
                Orb.entries.filter { it !in bare && it !in paired }.map { it.name } +
                    index.essences.essences.values.filterNot { applier.accepts(it, gear.item, gear.template) }.map { it.code }
                ).toSet() to paired.map { it.name }.toSet()
        }
    }
    val accepted: (String) -> Boolean = { it !in refused }
    val sections = listOfNotNull(ForgeSection.ORBS, ForgeSection.BENCH.takeIf { !isMap && benchable }, ForgeSection.ESSENCES.takeIf { essential })
    val section = smithy.section.takeIf { it in sections } ?: ForgeSection.ORBS
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader(ui("craft.title"), ui("craft.subtitle"), ForgeGlyphs.Anvil, guide = Guide.FORGE)
            if (hero == null || index == null) {
                InfoCard(ui("tree.no_hero"), ui("craft.hero_first"))
                return@Column
            }
            if (sections.size > 1) PillTabs(sections.map { ui(it.title) }, sections.indexOf(section), { vm.section(sections[it]) }, segmented = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetRail(s, recent.mapNotNull { id -> hero.item(id)?.let { s.view(it) } }, instance?.id, { picking = it }, vm::selectEquipment)
                Anvil(
                    s,
                    view,
                    toolSocket(s, smithy, section, index, benchLine, accepted),
                    omenSocket(s, smithy, section, vm::selectOmen),
                    onPick = { picking = TargetFilter.ALL },
                    modifier = Modifier.weight(1f),
                )
            }
            when (section) {
                ForgeSection.ORBS -> {
                    instance?.let {
                        LineChoice(s, it, it.unveil, "forge.unveil_title", "forge.unveil_hint", enabled, vm::unveil)
                        LineChoice(s, it, it.offer, "forge.choice_title", "forge.choice_hint", enabled, vm::choose)
                    }
                    gear?.let { OmenLedger(s, smithy.orb, smithy.omen, it, omens, vm::selectOmen) }
                    if (instance != null) OrbTray(s, smithy, accepted, { it in omenOnly }, vm::selectOrb)
                }

                ForgeSection.BENCH -> view?.let { BenchLedger(s, index, hero, it, benchLine) { line -> benchLine = line } }

                ForgeSection.ESSENCES -> EssenceTray(s, smithy.essence, accepted, vm::selectEssence)
            }
        }
        if (hero != null && instance != null) {
            when (section) {
                ForgeSection.ORBS -> OrbBar(s, smithy, instance, enabled, accepted, { it in omenOnly }, vm::applyOrb)
                ForgeSection.BENCH -> BenchBar(s, vm, instance, benchLine, enabled)
                ForgeSection.ESSENCES -> EssenceBar(s, smithy.essence, instance, enabled, accepted, vm::applyEssence)
            }
        }
    }
    picking?.let { shelf ->
        TargetPicker(s, recent, shelf, onDismiss = { picking = null }) {
            vm.selectEquipment(it)
            picking = null
        }
    }
}

internal val ForgeSection.title get() = when (this) {
    ForgeSection.ORBS -> "forge.section_orbs"
    ForgeSection.BENCH -> "forge.section_bench"
    ForgeSection.ESSENCES -> "forge.section_essences"
}

/** The anvil's tool socket: the orb, the essence or the bench line the section lays on the item, when one is chosen and fits. */
internal fun toolSocket(s: ForgeState, smithy: Smithy, section: ForgeSection, index: ContentIndex, benchLine: String, accepted: (String) -> Boolean): Socket = when (section) {
    ForgeSection.ORBS -> {
        val orb = Orb.of(smithy.orb)?.takeIf { (s.bagAmount(it.name) ?: 0L) > 0 && accepted(it.name) }
        val glyph: (@Composable () -> Unit)? = if (orb == null) {
            null
        } else {
            { OrbGlyph(orb, Modifier.fillMaxSize()) }
        }
        Socket(orb?.let { itemTitle(it.name) } ?: ui("forge.socket_orb"), Gold, glyph)
    }

    ForgeSection.ESSENCES -> {
        val code = smithy.essence.takeIf { index.essence(it) != null && (s.bagAmount(it) ?: 0L) > 0 && accepted(it) }
        val glyph: (@Composable () -> Unit)? = if (code == null) {
            null
        } else {
            { BagIcon(code, Modifier.fillMaxSize(), tint = Elder) }
        }
        Socket(code?.let(::itemTitle) ?: ui("forge.socket_essence"), Elder, glyph)
    }

    ForgeSection.BENCH -> {
        val glyph: (@Composable () -> Unit)? = if (benchLine.isBlank()) {
            null
        } else {
            { Icon(ForgeGlyphs.Anvil, null, tint = Crafted, modifier = Modifier.fillMaxSize()) }
        }
        Socket(ui(if (benchLine == UNCRAFT) "bench.remove" else "forge.section_bench"), Crafted, glyph)
    }
}

/** The omen's socket beside an orb: the omen laid with it, a tap taking it off; none for the bench or an essence. */
internal fun omenSocket(s: ForgeState, smithy: Smithy, section: ForgeSection, onSelect: (String) -> Unit): Socket? {
    if (section != ForgeSection.ORBS) return null
    val omen = smithy.omen.takeIf { it.isNotBlank() && (s.bagAmount(it) ?: 0L) > 0 }
    val glyph: (@Composable () -> Unit)? = if (omen == null) {
        null
    } else {
        { Icon(ForgeGlyphs.Sigil, null, tint = Rune, modifier = Modifier.fillMaxSize()) }
    }
    return Socket(
        omen?.let(::itemTitle) ?: ui("forge.omen"),
        Rune,
        glyph,
        if (omen == null) {
            null
        } else {
            { onSelect("") }
        },
    )
}
