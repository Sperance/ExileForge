package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ruleRefusal
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.presentation.forge.ForgeTarget
import com.sperance.exileforge.presentation.forge.OrbChoice
import com.sperance.exileforge.presentation.forge.Smithy
import com.sperance.exileforge.presentation.forge.SmithyViewModel
import com.sperance.exileforge.presentation.forge.forgeTarget
import com.sperance.exileforge.presentation.forge.orbChoices
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.ItemShelf
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.OrbApplier
import com.sperance.exileforge.rules.roll.OrbTarget
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** A bench "line" that is not a recipe: taking the crafted modifier back off. */
internal const val UNCRAFT = "-"

/**
 * The forge as an anvil (the owner's mockup B, 3.x): a rail of the picker's sections beside the anvil (с 4.2.0 - «Тип» общего
 * фильтра выбора вещи), which holds the target, the orb, essence or bench line laid on it and, for an orb, its omen — and shows the
 * target as it stands. Under it the section's tray: a compact grid of only what the bag holds and the target takes, or the bench's
 * lines. What is laid sits in a bar over the navigation with a button that is held, because an orb is spent the moment it is used.
 *
 * С 4.4.x цель - [ForgeTarget] (вещь или питомец, зеркало правил [OrbTarget]): наковальня, лоток и полоса одни на обе, питомец
 * выбирается тем же выбором цели. Every rule is the server's; what a target takes is the rules' own [OrbApplier.accepts].
 */
@Composable fun CraftScreen() {
    val vm = koinViewModel<SmithyViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val smithy by vm.smithy.collectAsStateWithLifecycle()
    // Orbs and ingredients are read off the bag, so the forge opens on a hero that is not stale.
    LaunchedEffect(game.heroId, game.sessionEpoch) { vm.ensure() }
    val hero = game.hero
    val index = game.index
    val target = remember(hero, index, smithy.pet, game.holding.selectedEquipment) { game.forgeTarget(smithy.pet) }
    val enabled = !game.busy && game.session.signedIn && (game.ownsCharacter || game.isAdmin)
    var picking by remember { mutableStateOf<PickerSection?>(null) }
    // Один фильтр выбора вещи на полку у наковальни и шторку (4.2.0).
    val pickFilter = rememberItemFilter(ItemShelf.FORGE)
    var benchLine by remember(target?.id) { mutableStateOf("") }
    // The omens in the bag: an orb that goes on the target only with one of them (a catalyst's Orb of Quality on a ring) is offered too.
    val omens: List<Omen> = remember(index, hero?.bag) { if (hero == null || index == null) emptyList() else heldOmens(index, hero) }
    // Лоток - только сферы, что лягут на цель (4.4.x): не лягут ни сами, ни со знамением сумки - их нет.
    val choices = remember(target, index, omens, hero?.bag) {
        if (index == null || hero == null || target == null) emptyList() else target.orbChoices(index, game.orbs.map { it.code.value }, omens, hero::count)
    }
    // Выбранная сфера перестала ложиться (кончилась, цель сменилась или изменилась после сферы) - выбор снимается.
    LaunchedEffect(target?.id, choices, smithy.orb) {
        if (target != null && smithy.orb.isNotBlank() && choices.none { it.code == smithy.orb }) vm.selectOrb("")
    }
    val gear = target as? ForgeTarget.Gear
    // Эссенции сумки, что на эту вещь не лягут (3.94.1): лоток показывает их серыми, с отказом правил словами игрока.
    val essenceRefusals = remember(gear, index, hero?.bag) {
        if (index == null || gear == null || hero == null) emptyMap() else essenceRefusals(index, hero, gear)
    }
    // Сфера качества над вещью (4.3.0): что поднимет вид качества выбранного знамения и сколько качества другого вида сбросится.
    val quality = remember(target, index, smithy.omen) { index?.let { target?.quality(it, Omen.of(smithy.omen)) } }
    // Что может выйти из сферы удачи (4.3.0): уникалки правил того же семейства и уровня вещи, тот же список, что тянет сервер.
    val chanceUniques = remember(target, index) { index?.let { target?.chanceUniques(it) }.orEmpty() }
    val sections = target?.sections ?: listOf(ForgeSection.ORBS)
    val section = smithy.section.takeIf { it in sections } ?: ForgeSection.ORBS
    val chosen = choices.firstOrNull { it.code == smithy.orb }
    Column(Modifier.fillMaxSize()) {
        // Шапка над прокруткой (3.88.7): уходит при прокрутке вниз и возвращается вверх.
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) { CollapsibleHeader { ScreenHeader(ui("craft.title"), ui("craft.subtitle"), ForgeGlyphs.Anvil) } }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (hero == null || index == null) {
                InfoCard(ui("tree.no_hero"), ui("craft.hero_first"))
                return@Column
            }
            if (sections.size > 1) PillTabs(sections.map { ui(it.title) }, sections.indexOf(section), { vm.section(sections[it]) }, segmented = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetRail(pickFilter, pets = hero.pets.pets.isNotEmpty(), petOn = target is ForgeTarget.Beast) { picking = it }
                Anvil(
                    game,
                    target,
                    toolSocket(game, smithy, section, index, benchLine, chosen, essenceRefusals),
                    omenSocket(game, smithy, section, vm::selectOmen),
                    onPick = { picking = if (target is ForgeTarget.Beast) PickerSection.PETS else PickerSection.ITEMS },
                    modifier = Modifier.weight(1f),
                )
            }
            when (section) {
                ForgeSection.ORBS -> target?.let {
                    LineChoices(game, it, enabled, vm)
                    OmenLedger(game, smithy.orb, smithy.omen, it.orbTarget, omens, vm::selectOmen)
                    OrbTray(game, smithy, choices, ui(it.noOrbsKey), quality, vm::selectOrb)
                }

                ForgeSection.BENCH -> gear?.let { BenchLedger(game, index, hero, it.view, benchLine) { line -> benchLine = line } }

                ForgeSection.ESSENCES -> EssenceTray(game, smithy.essence, essenceRefusals, vm::selectEssence)
            }
        }
        if (hero != null && target != null) {
            when (section) {
                ForgeSection.ORBS -> OrbBar(game, smithy, target, chosen, enabled, quality?.lost ?: 0, chanceUniques) { vm.applyOrb(target, it) }
                ForgeSection.BENCH -> gear?.let { BenchBar(game, vm, it.view, benchLine, enabled) }
                ForgeSection.ESSENCES -> gear?.let { EssenceBar(game, smithy.essence, it.view.item, enabled, { code -> code !in essenceRefusals }, vm::applyEssence) }
            }
        }
    }
    picking?.let { opened ->
        TargetPicker(game, pickFilter, opened, target?.id, onDismiss = { picking = null }) {
            vm.select(it)
            picking = null
        }
    }
}

/** Почему каждая эссенция сумки не ляжет на вещь [gear] (3.94.1): отказ правил словами игрока; эссенции, что ляжет, в карте нет. */
private fun essenceRefusals(index: ContentIndex, hero: HeroView, gear: ForgeTarget.Gear): Map<String, String> {
    val applier = OrbApplier(index)
    val view = gear.view
    return index.essences.essences.values.filter { hero.count(it.code) > 0 && !applier.accepts(it, view.item, view.template) }.associate { essence ->
        essence.code to (ruleRefusal { applier.applyEssence(essence, view.item.copy(), view.template, Dice(0)) } ?: ui("forge.essence_refused"))
    }
}

/** Строки на выбор, что ждут решения над целью: раскрытие и знамение выбора у вещи, знамение выбора у питомца. */
@Composable private fun LineChoices(game: GameUi, target: ForgeTarget, enabled: Boolean, vm: SmithyViewModel) {
    when (target) {
        is ForgeTarget.Gear -> {
            val item = target.view.item
            LineChoice(game, item, item.unveil, "forge.unveil_title", "forge.unveil_hint", enabled, vm::unveil)
            LineChoice(game, item, item.offer, "forge.choice_title", "forge.choice_hint", enabled, vm::choose)
        }

        is ForgeTarget.Beast -> PetChoices(game, target.pet, enabled, vm::choosePetLine)
    }
}

internal val ForgeSection.title get() = when (this) {
    ForgeSection.ORBS -> "forge.section_orbs"
    ForgeSection.BENCH -> "forge.section_bench"
    ForgeSection.ESSENCES -> "forge.section_essences"
}

/** The anvil's tool socket: the orb ([chosen] из лотка), the essence or the bench line the section lays on the target, when one is chosen and fits. */
internal fun toolSocket(game: GameUi, smithy: Smithy, section: ForgeSection, index: ContentIndex, benchLine: String, chosen: OrbChoice?, essenceRefusals: Map<String, String>): Socket = when (section) {
    ForgeSection.ORBS -> {
        val glyph: (@Composable () -> Unit)? = chosen?.let { { BagIcon(it.code, Modifier.fillMaxSize(), kind = ItemVisualKind.CURRENCY) } }
        Socket(chosen?.let { itemTitle(it.code) } ?: ui("forge.socket_orb"), Gold, glyph)
    }

    ForgeSection.ESSENCES -> {
        val code = smithy.essence.takeIf { index.essence(it) != null && (game.bagAmount(it) ?: 0L) > 0 && it !in essenceRefusals }
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
internal fun omenSocket(game: GameUi, smithy: Smithy, section: ForgeSection, onSelect: (String) -> Unit): Socket? {
    if (section != ForgeSection.ORBS) return null
    val omen = smithy.omen.takeIf { it.isNotBlank() && (game.bagAmount(it) ?: 0L) > 0 }
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
