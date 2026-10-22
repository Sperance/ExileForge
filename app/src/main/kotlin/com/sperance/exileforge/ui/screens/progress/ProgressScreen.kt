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
import com.sperance.exileforge.core.app.Feature
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.TAB_CRAFT
import com.sperance.exileforge.core.app.TAB_CRAFTS
import com.sperance.exileforge.core.app.TAB_PROGRESS
import com.sperance.exileforge.core.app.TAB_SKILLS
import com.sperance.exileforge.core.app.TAB_TREE
import com.sperance.exileforge.core.app.level
import com.sperance.exileforge.core.app.unlocked
import com.sperance.exileforge.core.display.jobTitle
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.craft.CraftScreen
import com.sperance.exileforge.ui.screens.crafts.CraftsScreen
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import com.sperance.exileforge.ui.screens.skills.GrimoireScreen
import com.sperance.exileforge.ui.screens.tree.SkillTreeScreen
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The screens the «Развитие» tab holds: the forge, the tree, the grimoire and the crafts open over the hub with a
 * way back to it. The trials and the atlas are pages of «Поход», the chronicle a building of the City.
 */
enum class ProgressPlace(val tab: Int, private val title: String, val icon: ImageVector) {
    FORGE(TAB_CRAFT, "nav.forge", ForgeGlyphs.Anvil),
    TREE(TAB_TREE, "nav.tree", ForgeGlyphs.Constellation),
    GRIMOIRE(TAB_SKILLS, "nav.skills", ForgeGlyphs.Grimoire),
    CRAFTS(TAB_CRAFTS, "nav.crafts", ForgeGlyphs.Hammer),
    ;

    val label: String get() = ui(title)
}

/**
 * «Развитие»: the hero's growth between runs gathered in one tab — the forge,
 * the tree, the grimoire and the crafts, сеткой 2×2. Each tile says in a line what it
 * holds and, in its colour, what waits; a badge counts what asks to be done.
 *
 * Ремёсла: в строке - идущая работа («Горное дело · Железная жила»), досчёт после отлучки, ещё не показанный листом, - «Пока вас не
 * было» и точка на плитке. Вкладка нажата снова - наверх.
 */
@Composable fun ProgressScreen() {
    val game by koinViewModel<ProgressViewModel>().game.collectAsStateWithLifecycle()
    val shell: ShellViewModel = koinViewModel()
    val progress: ProgressViewModel = koinViewModel()
    val away by shell.craftsAway.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId, game.sessionEpoch) { progress.ensure() }
    val hero = game.hero
    val tiles = run {
        val orbs = hero?.let { h -> game.orbs.sumOf { h.count(it.code.value) } } ?: 0L
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
                ui("nav.tree"),
                ForgeGlyphs.Constellation,
                Gold,
                ui("progress.tree_note"),
                tree?.let { ui("tree.points", it.available, it.total) },
                tree?.available ?: 0,
                badgeTip = tree?.let { ui("progress.tree_badge_tip", it.available) },
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
            HubTile(
                ui("nav.crafts"),
                ForgeGlyphs.Hammer,
                Brass,
                ui("progress.crafts_note"),
                if (away) ui("progress.crafts_away") else hero?.crafts?.work?.let { "${professionTitle(it.profession)} · ${jobTitle(it.job)}" },
                0,
                game.lockOf(Feature.CRAFTS),
                alert = away,
            ) { shell.tab(TAB_CRAFTS) },
        )
    }
    val scroll = rememberScrollState()
    OnReselect(TAB_PROGRESS) { scroll.animateScrollTo(0) }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HubGrid(tiles)
        MutedText(ui("progress.hint"), style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * A screen of the hub with its way back: the forge, the tree, the grimoire or the crafts. The forge is reached from an item's
 * sheet as well, and «back» from it comes here too — the hub is where it lives.
 *
 * Кузня читает героя сама, дерево и гримуар - только это.
 */
@Composable fun ProgressPlaceScreen(place: ProgressPlace) {
    val game by koinViewModel<ProgressViewModel>().game.collectAsStateWithLifecycle()
    val shell: ShellViewModel = koinViewModel()
    val heroModel: HeroViewModel = koinViewModel()
    LaunchedEffect(game.heroId, game.sessionEpoch) { heroModel.ensure() }
    BackTrailHost(ui("nav.progress")) {
        TrailLevel(place.label, onBack = { shell.tab(TAB_PROGRESS) }) {
            when (place) {
                ProgressPlace.FORGE -> CraftScreen()
                ProgressPlace.TREE -> SkillTreeScreen()
                ProgressPlace.GRIMOIRE -> GrimoireScreen()
                ProgressPlace.CRAFTS -> CraftsScreen()
            }
        }
    }
}

/** The level a tile's place opens at, while the hero is below it. */
private fun GameUi.lockOf(feature: Feature): Int? = feature.takeIf { !unlocked(it) }?.level(index?.rules)
