package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.ForgeSection
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemShelf
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.forge.ForgeTarget
import com.sperance.exileforge.core.forge.OrbChoice
import com.sperance.exileforge.core.forge.forgeTarget
import com.sperance.exileforge.core.forge.hiddenOrbs
import com.sperance.exileforge.core.forge.orbChoices
import com.sperance.exileforge.core.i18n.ruleRefusal
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.content.Orb
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
 * The forge as an anvil: a rail of the picker's sections beside the anvil («Тип» общего
 * фильтра выбора вещи), which holds the target, the orb, essence or bench line laid on it and, for an orb, its omen — and shows the
 * target as it stands. Under it the section's tray: a compact grid of only what the bag holds and the target takes, or the bench's
 * lines. What is laid sits in a bar over the navigation with a button that is held, because an orb is spent the moment it is used.
 *
 * Цель - [ForgeTarget] (зеркало правил [OrbTarget]). Every rule is the server's; what a target takes is the rules' own [OrbApplier.accepts].
 *
 * Сферы и материалы читаются из сумки - кузня открывается на свежем герое. Лоток - только сферы, что лягут на цель сами или со знамением
 * сумки; переставшая ложиться выбранная сфера снимается. Печати Атласа - только на карту. Эссенции, что не лягут, - серыми с отказом правил
 * словами игрока. Сфера качества говорит, что поднимет и сколько качества другого вида сбросится; сфера удачи - какие уникалки может дать
 * вещь.
 */
@Composable fun CraftScreen() {
    val vm = koinViewModel<SmithyViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val smithy by vm.smithy.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId, game.sessionEpoch) { vm.ensure() }
    val hero = game.hero
    val index = game.index
    val target = remember(hero, index, game.holding.selectedEquipment) { game.forgeTarget() }
    val enabled = !game.busy && game.session.signedIn && (game.ownsCharacter || game.isAdmin)
    var picking by remember { mutableStateOf(false) }
    val pickFilter = rememberItemFilter(ItemShelf.FORGE)
    var benchLine by remember(target?.id) { mutableStateOf("") }
    var engraving by remember(target?.id) { mutableStateOf(false) }
    val omens: List<Omen> = remember(index, hero?.bag) { if (hero == null || index == null) emptyList() else heldOmens(index, hero) }
    val choices = remember(target, index, omens, hero?.bag) {
        if (index == null || hero == null || target == null) emptyList() else target.orbChoices(index, game.orbs.map { it.code.value }, omens, hero::count)
    }
    val hidden = remember(target, index, omens, hero?.bag) {
        if (index == null || hero == null || target == null) emptyList() else target.hiddenOrbs(index, game.orbs.map { it.code.value }, omens, hero::count)
    }
    LaunchedEffect(target?.id, choices, smithy.orb) {
        if (target != null && smithy.orb.isNotBlank() && choices.none { it.code == smithy.orb }) vm.selectOrb("")
    }
    val seals = remember(target, index, omens, hero?.bag) {
        if (index == null || hero == null || target == null) emptyList() else target.seals(index, omens, hero::count)
    }
    LaunchedEffect(target?.id, seals, smithy.seal) {
        if (target != null && smithy.seal.isNotBlank() && seals.none { it.code == smithy.seal }) vm.selectSeal("")
    }
    val gear = target as? ForgeTarget.Gear
    val essenceRefusals = remember(gear, index, hero?.bag) {
        if (index == null || gear == null || hero == null) emptyMap() else essenceRefusals(index, hero, gear)
    }
    val quality = remember(target, index, smithy.omen) { index?.let { target?.quality(it, Omen.of(smithy.omen)) } }
    val chanceUniques = remember(target, index) { index?.let { target?.chanceUniques(it) }.orEmpty() }
    val sections = target?.sections ?: listOf(ForgeSection.ORBS)
    val section = smithy.section.takeIf { it in sections } ?: ForgeSection.ORBS
    val chosen = choices.firstOrNull { it.code == smithy.orb }
    val sealChosen = seals.firstOrNull { it.code == smithy.seal }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) { CollapsibleHeader { ScreenHeader(ui("craft.title"), ui("craft.subtitle"), ForgeGlyphs.Anvil) } }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (hero == null || index == null) {
                InfoCard(ui("tree.no_hero"), ui("craft.hero_first"))
                return@Column
            }
            if (sections.size > 1) PillTabs(sections.map { ui(it.title) }, sections.indexOf(section), { vm.section(sections[it]) }, segmented = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetRail(pickFilter) { picking = true }
                Anvil(
                    game,
                    target,
                    toolSocket(game, smithy, section, index, benchLine, if (section == ForgeSection.SEALS) sealChosen else chosen, essenceRefusals),
                    omenSocket(game, smithy, section, vm::selectOmen),
                    onPick = { picking = true },
                    modifier = Modifier.weight(1f),
                )
            }
            when (section) {
                ForgeSection.ORBS -> target?.let {
                    LineChoices(game, it, enabled, vm)
                    OmenLedger(game, smithy.orb, smithy.omen, it.orbTarget, omens, vm::selectOmen)
                    OrbTray(game, smithy, choices, hidden, ui(it.noOrbsKey), quality) { code ->
                        vm.selectOrb(code)
                        engraving = code == Orb.ENGRAVERS_ORB.name
                    }
                }

                ForgeSection.SEALS -> gear?.let {
                    LineChoices(game, it, enabled, vm)
                    OmenList(game, it.sealOmens(index, smithy.seal, omens), smithy.omen, vm::selectOmen)
                    SealTray(game, smithy, seals, vm::selectSeal)
                }

                ForgeSection.BENCH -> gear?.let { BenchLedger(game, index, hero, it.view, benchLine) { line -> benchLine = line } }

                ForgeSection.ESSENCES -> EssenceTray(game, smithy.essence, essenceRefusals, vm::selectEssence)
            }
        }
        if (hero != null && target != null) {
            when (section) {
                ForgeSection.ORBS -> OrbBar(game, smithy, target, chosen, enabled, quality?.lost ?: 0, chanceUniques, { engraving = true }) { vm.applyOrb(target, it) }
                ForgeSection.SEALS -> gear?.let { SealBar(game, smithy, it, sealChosen, enabled) { code -> vm.applySeal(it.id, code) } }
                ForgeSection.BENCH -> gear?.let { BenchBar(game, vm, it.view, benchLine, enabled) }
                ForgeSection.ESSENCES -> gear?.let { EssenceBar(game, smithy.essence, it.view, enabled, { code -> code !in essenceRefusals }, vm::applyEssence) }
            }
        }
    }
    if (engraving && target != null && smithy.orb == Orb.ENGRAVERS_ORB.name) {
        EngraveSheet(game, target, enabled, onDismiss = { engraving = false }) { line ->
            vm.applyOrb(target, Orb.ENGRAVERS_ORB.name, line)
            engraving = false
        }
    }
    if (picking) {
        TargetPicker(game, pickFilter, target?.id, onDismiss = { picking = false }) {
            vm.select(it)
            picking = false
        }
    }
}

/** Почему каждая эссенция сумки не ляжет на вещь [gear]: отказ правил словами игрока; эссенции, что ляжет, в карте нет. */
private fun essenceRefusals(index: ContentIndex, hero: HeroView, gear: ForgeTarget.Gear): Map<String, String> {
    val applier = OrbApplier(index)
    val view = gear.view
    return index.essences.essences.values.filter { hero.count(it.code) > 0 && !applier.accepts(it, view.item, view.template) }.associate { essence ->
        essence.code to (ruleRefusal { applier.applyEssence(essence, view.item.copy(), view.template, Dice(0)) } ?: ui("forge.essence_refused"))
    }
}

/** Строки на выбор, что ждут решения над вещью: скрытая строка ([REVEALS] - аффикс или судьбоносная) и знамение выбора. */
@Composable private fun LineChoices(game: GameUi, target: ForgeTarget, enabled: Boolean, vm: SmithyViewModel) {
    when (target) {
        is ForgeTarget.Gear -> {
            val item = target.view.item
            REVEALS.firstOrNull { it.pending(item) }?.Choice(game, item, enabled, vm::unveil)
            LineChoice(game, item, item.offer, "forge.choice_title", "forge.choice_hint", enabled, vm::choose)
        }
    }
}

internal val ForgeSection.title get() = when (this) {
    ForgeSection.ORBS -> "forge.section_orbs"
    ForgeSection.SEALS -> "forge.section_seals"
    ForgeSection.BENCH -> "forge.section_bench"
    ForgeSection.ESSENCES -> "forge.section_essences"
}

/**
 * The anvil's tool socket: the orb or the Atlas seal ([chosen] из лотка своего раздела), the essence or the bench line the section lays
 * on the target, when one is chosen and fits.
 */
internal fun toolSocket(game: GameUi, smithy: Smithy, section: ForgeSection, index: ContentIndex, benchLine: String, chosen: OrbChoice?, essenceRefusals: Map<String, String>): Socket = when (section) {
    ForgeSection.ORBS -> {
        val glyph: (@Composable () -> Unit)? = chosen?.let { { BagIcon(it.code, Modifier.fillMaxSize(), kind = ItemVisualKind.CURRENCY) } }
        Socket(chosen?.let { itemTitle(it.code) } ?: ui("forge.socket_orb"), Gold, glyph)
    }

    ForgeSection.SEALS -> {
        val glyph: (@Composable () -> Unit)? = chosen?.let { { BagIcon(it.code, Modifier.fillMaxSize(), kind = ItemVisualKind.CURRENCY) } }
        Socket(chosen?.let { itemTitle(it.code) } ?: ui("forge.socket_seal"), Gold, glyph)
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

/** The omen's socket beside an orb or a seal: the omen laid with it, a tap taking it off; none for the bench or an essence. */
internal fun omenSocket(game: GameUi, smithy: Smithy, section: ForgeSection, onSelect: (String) -> Unit): Socket? {
    if (!section.omens) return null
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
