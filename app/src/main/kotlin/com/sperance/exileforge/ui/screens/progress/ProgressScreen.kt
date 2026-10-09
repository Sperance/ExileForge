package com.sperance.exileforge.ui.screens.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.progress.ProgressViewModel
import com.sperance.exileforge.presentation.state.Feature
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.presentation.state.TAB_PETS
import com.sperance.exileforge.presentation.state.TAB_PROGRESS
import com.sperance.exileforge.presentation.state.TAB_SKILLS
import com.sperance.exileforge.presentation.state.TAB_TREE
import com.sperance.exileforge.presentation.state.level
import com.sperance.exileforge.presentation.state.unlocked
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.craft.CraftScreen
import com.sperance.exileforge.ui.screens.hero.MenagerieSection
import com.sperance.exileforge.ui.screens.skills.GrimoireScreen
import com.sperance.exileforge.ui.screens.tree.SkillTreeScreen
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** The pets at work at once: one combat pet and one helper. */
private const val PETS_AT_WORK = 2

/**
 * The screens the «Развитие» tab holds: the forge, the menagerie, the tree and the grimoire (3.90.5) open over the hub with a
 * way back to it. The trials and the atlas are pages of «Поход», the chronicle a building of the City (4.0.0).
 */
enum class ProgressPlace(val tab: Int, private val title: String, val icon: ImageVector) {
    FORGE(TAB_CRAFT, "nav.forge", ForgeGlyphs.Anvil),
    PETS(TAB_PETS, "progress.pets", ForgeGlyphs.Exile),
    TREE(TAB_TREE, "nav.tree", ForgeGlyphs.Constellation),
    GRIMOIRE(TAB_SKILLS, "nav.skills", ForgeGlyphs.Grimoire),
    ;

    val label: String get() = ui(title)
}

/**
 * «Развитие» (variant A, «Плитки 2×2»): the hero's growth between runs gathered in one tab — the forge, the menagerie,
 * the tree and the grimoire. Each tile says in a line what it
 * holds and, in its colour, what waits; a badge counts what asks to be done.
 */
@Composable fun ProgressScreen() {
    val game by koinViewModel<ProgressViewModel>().game.collectAsStateWithLifecycle()
    val shell: ShellViewModel = koinViewModel()
    val progress: ProgressViewModel = koinViewModel()
    LaunchedEffect(game.heroId, game.sessionEpoch) { progress.ensure() }
    val hero = game.hero
    val index = game.index
    val tiles = run {
        val orbs = hero?.let { h -> game.orbs.sumOf { h.count(it.code.value) } } ?: 0L
        val pets = hero?.pets
        // The incubator (server 1.67.0): ripe eggs first, then the ones ripening, then eggs of the bag a free place waits for.
        val incubator = pets?.incubator
        val ready = incubator?.ready ?: 0
        val incubating = incubator?.incubating ?: 0
        val eggs = if (hero != null && index != null && incubator?.free != null) index.pets.eggs.values.flatMap { it.values }.toSet().sumOf { hero.count(it) } else 0L
        val tree = game.treeState
        listOf(
            HubTile(
                ui("nav.forge"),
                ForgeGlyphs.Anvil,
                Ember,
                ui("progress.forge_note"),
                ui("progress.forge_orbs", orbs).takeIf { orbs > 0 },
                0,
                game.lockOf(Feature.FORGE),
            ) { shell.tab(TAB_CRAFT) },
            HubTile(
                ui("progress.pets"),
                ForgeGlyphs.Exile,
                Vital,
                ui("progress.pets_note", pets?.pets?.size ?: 0, pets?.cap ?: 0),
                when {
                    ready > 0 -> ui("progress.pets_ready", ready)
                    incubating > 0 -> ui("progress.pets_incubating", incubating)
                    eggs > 0 -> ui("progress.pets_lay", eggs)
                    else -> ui("progress.pets_work", pets?.active?.size ?: 0, PETS_AT_WORK)
                },
                if (ready > 0) ready else eggs.toInt(),
                game.lockOf(Feature.PETS),
            ) { shell.tab(TAB_PETS) },
            // Дерево и гримуар (3.90.5) - из полосы «Героя»: там остались только вещи.
            HubTile(
                ui("nav.tree"),
                ForgeGlyphs.Constellation,
                Gold,
                ui("progress.tree_note"),
                tree?.let { ui("tree.points", it.available, it.total) },
                tree?.available ?: 0,
            ) { shell.tab(TAB_TREE) },
            HubTile(
                ui("nav.skills"),
                ForgeGlyphs.Grimoire,
                ManaBlue,
                ui("progress.grimoire_note"),
                null,
                0,
                game.lockOf(Feature.GRIMOIRE),
            ) { shell.tab(TAB_SKILLS) },
        )
    }
    val scroll = rememberScrollState()
    // Вкладка нажата снова (3.95.0): развитие - наверх
    OnReselect(TAB_PROGRESS) { scroll.animateScrollTo(0) }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(ui("progress.title"), ui("progress.subtitle"), ForgeGlyphs.Sigil)
        HubGrid(tiles)
        InfoCard(ui("progress.title"), ui("progress.hint"))
    }
}

/** The menagerie as a screen of its own: it was a section of the Hero tab's list. */
@Composable private fun PetsPlace(game: GameUi) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { MenagerieSection(game, koinViewModel()) }
}

/**
 * A screen of the hub with its way back: the forge, the menagerie, the tree or the grimoire. The forge is reached from an item's
 * sheet as well, and «back» from it comes here too — the hub is where it lives now.
 */
@Composable fun ProgressPlaceScreen(place: ProgressPlace) {
    val game by koinViewModel<ProgressViewModel>().game.collectAsStateWithLifecycle()
    val shell: ShellViewModel = koinViewModel()
    val heroModel: HeroViewModel = koinViewModel()
    // The forge reads the hero itself; the menagerie has only this.
    LaunchedEffect(game.heroId, game.sessionEpoch) { heroModel.ensure() }
    Column(Modifier.fillMaxSize()) {
        BackRow("${ui("nav.progress")} · ${place.label}") { shell.tab(TAB_PROGRESS) }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (place) {
                ProgressPlace.FORGE -> CraftScreen()
                ProgressPlace.PETS -> PetsPlace(game)
                ProgressPlace.TREE -> SkillTreeScreen()
                ProgressPlace.GRIMOIRE -> GrimoireScreen()
            }
        }
    }
}

/** The level a tile's place opens at (3.76.0), while the hero is below it. */
private fun GameUi.lockOf(feature: Feature): Int? = feature.takeIf { !unlocked(it) }?.level(index?.rules)
