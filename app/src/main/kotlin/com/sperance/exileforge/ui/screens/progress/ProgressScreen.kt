package com.sperance.exileforge.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.Feature
import com.sperance.exileforge.presentation.state.unlocked
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.draw.alpha
import com.sperance.exileforge.presentation.state.TAB_CHRONICLE
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.presentation.state.TAB_PETS
import com.sperance.exileforge.presentation.state.TAB_PROGRESS
import com.sperance.exileforge.presentation.state.TAB_TRIALS
import com.sperance.exileforge.rules.content.TrialRules
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.craft.CraftScreen
import com.sperance.exileforge.ui.screens.expedition.TrialsBoard
import com.sperance.exileforge.ui.screens.hero.ChronicleScreen
import com.sperance.exileforge.ui.screens.hero.MenagerieSection
import com.sperance.exileforge.ui.screens.hero.chronicleDone
import com.sperance.exileforge.ui.screens.hero.titleName
import com.sperance.exileforge.ui.theme.*

/** The pets at work at once: one combat pet and one helper. */
private const val PETS_AT_WORK = 2

/**
 * The screens the «Развитие» tab holds: the forge, the menagerie, the trials and the chronicle (3.69.0) open over the
 * hub with a way back to it; the atlas is a sky of its own and covers the whole screen, as it always did.
 */
enum class ProgressPlace(val tab: Int, private val title: String, val icon: ImageVector) {
    FORGE(TAB_CRAFT, "nav.forge", ForgeGlyphs.Anvil), PETS(TAB_PETS, "progress.pets", ForgeGlyphs.Exile),
    TRIALS(TAB_TRIALS, "trials.title", ForgeGlyphs.Skull), CHRONICLE(TAB_CHRONICLE, "chronicle.title", ForgeGlyphs.Scroll);

    val label: String get() = ui(title)

    companion object {
        /** The «Развитие» tab's screen a tab index is, if it is one. */
        fun of(tab: Int): ProgressPlace? = entries.firstOrNull { it.tab == tab }
    }
}

/** What each tile of the hub says: one line of what it holds, one of what waits, and a count when something does. */
@Immutable private data class ProgressTile(val title: String, val icon: ImageVector, val accent: Color,
    val note: String, val news: String?, val badge: Int, val lockedUntil: Int? = null, val onOpen: () -> Unit)

/**
 * «Развитие» (variant A, «Плитки 2×2»): the hero's growth between runs gathered in one tab — the forge, the menagerie,
 * the atlas, the trials and the chronicle (3.69.0), which lay about the Hero tab and the world map before. Each tile says in a line what it
 * holds and, in its colour, what waits; a badge counts what asks to be done.
 */
@Composable fun ProgressScreen(s: ForgeState, vm: ForgeViewModel) {
    LaunchedEffect(s.play.heroId, s.account.sessionEpoch) { vm.ensureHero() }
    val hero = s.hero
    val index = s.index
    val tiles = run {
        val orbs = hero?.let { h -> s.orbs.sumOf { h.count(it.code) } } ?: 0L
        val pets = hero?.pets
        // The incubator (server 1.67.0): ripe eggs first, then the ones ripening, then eggs of the bag a free place waits for.
        val incubator = pets?.incubator
        val ready = incubator?.ready ?: 0
        val incubating = incubator?.incubating ?: 0
        val eggs = if (hero != null && index != null && incubator?.free != null) index.pets.eggs.values.toSet().sumOf { hero.count(it) } else 0L
        val atlas = s.atlasState
        val rules = index?.campaign?.trials
        val keys = hero?.count(TrialRules.KEY) ?: 0L
        val chronicle = s.chronicleDone()
        val title = hero?.info?.title?.takeIf { it.isNotBlank() }
        listOf(
            ProgressTile(ui("nav.forge"), ForgeGlyphs.Anvil, Ember, ui("progress.forge_note"),
                ui("progress.forge_orbs", orbs).takeIf { orbs > 0 }, 0, s.lockOf(Feature.FORGE)) { vm.tab(TAB_CRAFT) },
            ProgressTile(ui("progress.pets"), ForgeGlyphs.Exile, Vital, ui("progress.pets_note", pets?.pets?.size ?: 0, pets?.cap ?: 0),
                when {
                    ready > 0 -> ui("progress.pets_ready", ready)
                    incubating > 0 -> ui("progress.pets_incubating", incubating)
                    eggs > 0 -> ui("progress.pets_lay", eggs)
                    else -> ui("progress.pets_work", pets?.active?.size ?: 0, PETS_AT_WORK)
                },
                if (ready > 0) ready else eggs.toInt(), s.lockOf(Feature.PETS)) { vm.tab(TAB_PETS) },
            ProgressTile(ui("atlas.title"), ForgeGlyphs.Atlas, Rune, ui("progress.atlas_note", ((atlas?.allocated?.size ?: 1) - 1).coerceAtLeast(0)),
                atlas?.let { ui("atlas.points", it.available, it.points) }, atlas?.available ?: 0, vm::openAtlas),
            ProgressTile(ui("trials.title"), ForgeGlyphs.Skull, AbyssGlow, ui("progress.trials_note", hero?.campaign?.trials?.towerBest ?: 0),
                when {
                    keys > 0 -> ui("progress.trials_keys", keys)
                    rules != null -> ui("progress.trials_crests", hero?.count(TrialRules.CREST) ?: 0L, rules.rush.key)
                    else -> null
                }, keys.toInt(), s.lockOf(Feature.TRIALS)) { vm.tab(TAB_TRIALS) },
            ProgressTile(ui("chronicle.title"), ForgeGlyphs.Scroll, GoldBright,
                chronicle?.let { (done, all) -> ui("chronicle.done", done, all) } ?: ui("common.loading"),
                title?.let(::titleName), 0) { vm.tab(TAB_CHRONICLE) },
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(ui("progress.title"), ui("progress.subtitle"), ForgeGlyphs.Sigil, guide = Guide.PROGRESS)
        tiles.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { ProgressTileCard(it, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
        InfoCard(ui("progress.title"), ui("progress.hint"))
    }
}

/** One tile: the glyph on its tinted square, the name, and the status at the foot; a badge in the corner when something waits. */
@Composable private fun ProgressTileCard(tile: ProgressTile, modifier: Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val locked = tile.lockedUntil
    val hot = tile.badge > 0 && locked == null
    Box(modifier.heightIn(min = 150.dp).alpha(if (locked != null) LOCKED_TILE_ALPHA else 1f).clip(shape).background(Panel, shape)
        .border(1.dp, if (hot) tile.accent.copy(alpha = .55f) else PanelRaised, shape)
        .clickable(role = Role.Button, onClick = tile.onOpen).padding(12.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(44.dp).background(tile.accent.copy(alpha = .14f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(tile.icon, null, tint = tile.accent, modifier = Modifier.size(26.dp))
            }
            Text(tile.title, color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MutedText(locked?.let { ui("unlock.from", it) } ?: tile.note)
                if (locked == null) tile.news?.let { Text(it, color = tile.accent, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (locked != null) Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
        if (hot) Box(Modifier.align(Alignment.TopEnd).background(tile.accent, CircleShape).padding(horizontal = 6.dp, vertical = 1.dp)) {
            Text(tile.badge.toString(), color = Ink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

/** The menagerie as a screen of its own: it was a section of the Hero tab's list. */
@Composable private fun PetsPlace(s: ForgeState, vm: ForgeViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { MenagerieSection(s, vm) }
}

/**
 * A screen of the hub with its way back: the forge, the menagerie, the trials or the chronicle. The forge is reached from an item's
 * sheet as well, and «back» from it comes here too — the hub is where it lives now.
 */
@Composable fun ProgressPlaceScreen(place: ProgressPlace, s: ForgeState, vm: ForgeViewModel) {
    // The forge reads the hero itself; the menagerie and the trials have only this.
    LaunchedEffect(s.play.heroId, s.account.sessionEpoch) { vm.ensureHero() }
    Column(Modifier.fillMaxSize()) {
        BackRow("${ui("nav.progress")} · ${place.label}") { vm.tab(TAB_PROGRESS) }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (place) {
                ProgressPlace.FORGE -> CraftScreen(s, vm)
                ProgressPlace.PETS -> PetsPlace(s, vm)
                ProgressPlace.TRIALS -> TrialsBoard(s, vm, Modifier.fillMaxSize())
                ProgressPlace.CHRONICLE -> ChronicleScreen(s, vm)
            }
        }
    }
}

/** The level a tile's place opens at (3.76.0), while the hero is below it. */
private fun ForgeState.lockOf(feature: Feature): Int? = feature.takeIf { !unlocked(it) }?.level

private const val LOCKED_TILE_ALPHA = .45f
