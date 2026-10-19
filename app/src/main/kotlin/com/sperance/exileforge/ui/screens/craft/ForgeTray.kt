package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.forge.HiddenOrb
import com.sperance.exileforge.core.forge.OrbChoice
import com.sperance.exileforge.core.forge.Smithy
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.roll.OrbApplier
import com.sperance.exileforge.rules.roll.OrbTarget
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/** A tray cell's side: compact, so two rows of the tray stay short (the owner's word on mockup B). */
private val CELL = 44.dp
private val GAP = 6.dp

/** Past this many stacks the tray lays them in two rows and scrolls sideways; fewer fit one row. */
private const val ONE_ROW = 6

/**
 * The tray of the forge (mockup B): a compact grid of what can be laid on the anvil — only stacks the bag holds and the item
 * takes — each a drawing with its count. Лоток только выбирает: имя выбранного, итог и описание называет рецепт наковальни
 * ([Recipe]); пустой лоток не рисуется - что выбирать нечего, говорит тот же рецепт. [codes] are in their shelf order; [glyph]
 * draws one; [dimmed] - стопка, что цель не примет (тусклая, причину называет рецепт).
 */
@Composable internal fun ForgeTray(
    game: GameUi,
    title: String,
    codes: List<String>,
    chosen: String,
    accent: Color,
    onSelect: (String) -> Unit,
    dimmed: (String) -> Boolean = { false },
    marked: (String) -> Boolean = { false },
    glyph: @Composable (String) -> Unit,
) {
    if (codes.isEmpty()) return
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth().depthPanel(shape).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = accent, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(codes.size.toString(), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        val rows = if (codes.size > ONE_ROW) 2 else 1
        LazyHorizontalGrid(
            GridCells.Fixed(rows),
            Modifier.fillMaxWidth().height(CELL * rows + GAP * (rows - 1)),
            horizontalArrangement = Arrangement.spacedBy(GAP),
            verticalArrangement = Arrangement.spacedBy(GAP),
        ) {
            items(codes, key = { it }) { code ->
                Box(Modifier.alpha(if (dimmed(code)) .4f else 1f)) {
                    TrayCell(itemTitle(code), game.bagAmount(code) ?: 0L, code == chosen, marked(code), accent, { onSelect(code) }) { glyph(code) }
                }
            }
        }
    }
}

/**
 * One stack of the tray: its drawing, the count in the corner, and a gold frame when it lies on the anvil. [marked] -
 * знак знамения в нижнем углу: стопка ляжет только с ним.
 */
@Composable private fun TrayCell(name: String, count: Long, selected: Boolean, marked: Boolean, accent: Color, onClick: () -> Unit, glyph: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier.size(CELL).clip(shape).background(if (selected) accent.copy(alpha = .14f) else PanelRaised, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) accent else Bronze, shape)
            .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) { glyph() }
        Text(
            count.toString(),
            color = GoldBright,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 3.dp),
        )
        if (marked) Icon(ForgeGlyphs.Sigil, null, tint = Rune, modifier = Modifier.align(Alignment.BottomStart).padding(start = 2.dp, bottom = 2.dp).size(11.dp))
    }
}

/**
 * The orbs of the tray: только [choices] - сферы сумки, что лягут на вещь сами или со знамением сумки,
 * в порядке полки; сферы, что не лягут, спрятаны. Сфера, что ждёт знамения, помечена знаком (что его нужно, говорит рецепт).
 * Под лотком - строка «Скрыто сфер: N» ([hidden]): касание открывает список спрятанных с точной причиной каждой.
 */
@Composable internal fun OrbTray(
    game: GameUi,
    smithy: Smithy,
    choices: List<OrbChoice>,
    hidden: List<HiddenOrb>,
    onSelect: (String) -> Unit,
) {
    val waiting = remember(choices) { choices.filter { it.needsOmen }.map { it.code }.toSet() }
    var showing by remember { mutableStateOf(false) }
    if (showing) HiddenOrbsSheet(hidden) { showing = false }
    ForgeTray(
        game,
        ui("forge.tray_orbs"),
        choices.map { it.code },
        smithy.orb,
        Gold,
        onSelect,
        marked = { it in waiting },
    ) { code ->
        BagIcon(code, Modifier.fillMaxSize(), kind = ItemVisualKind.CURRENCY)
    }
    if (hidden.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable(role = Role.Button) { showing = true }.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.VisibilityOff, null, tint = Muted, modifier = Modifier.size(16.dp))
            Text(ui("forge.hidden_orbs", hidden.size), color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

/** Лист спрятанных сфер: значок, имя и точная причина отказа правил над вещью под каждой. */
@Composable private fun HiddenOrbsSheet(hidden: List<HiddenOrb>, onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("forge.hidden_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            hidden.forEach { orb ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    BagIcon(orb.code, Modifier.size(28.dp), kind = ItemVisualKind.CURRENCY)
                    Column(Modifier.weight(1f)) {
                        Text(itemTitle(orb.code), color = Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        if (orb.reason.isNotBlank()) Text(orb.reason, color = LifeRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/** Печати Атласа лотка: только [choices] - печати сумки, что лягут на карту сами или со знамением сумки; ждущая знамения помечена. */
@Composable internal fun SealTray(game: GameUi, smithy: Smithy, choices: List<OrbChoice>, onSelect: (String) -> Unit) {
    val waiting = remember(choices) { choices.filter { it.needsOmen }.map { it.code }.toSet() }
    ForgeTray(
        game,
        ui("forge.tray_seals"),
        choices.map { it.code },
        smithy.seal,
        Gold,
        onSelect,
        marked = { it in waiting },
    ) { code ->
        BagIcon(code, Modifier.fillMaxSize(), kind = ItemVisualKind.CURRENCY)
    }
}

/**
 * The essences of the tray: the bag's, the special ones last and the higher tiers first. И те, что вещь не
 * примет ([refusals] - отказ правил по коду): серыми в конце, почему - говорит рецепт.
 */
@Composable internal fun EssenceTray(game: GameUi, chosen: String, refusals: Map<String, String>, onSelect: (String) -> Unit) {
    val hero = game.hero ?: return
    val index = game.index ?: return
    val essences = index.itemsByCategory[com.sperance.exileforge.rules.content.Item.ESSENCE].orEmpty()
        .filter { hero.count(it.code.value) > 0 }
        .sortedWith(compareBy({ it.code.value in refusals }, { index.essence(it.code.value)?.special == true }, { -(index.essence(it.code.value)?.tier ?: 0) })).map { it.code.value }
    ForgeTray(
        game,
        ui("forge.tray_essences"),
        essences,
        chosen,
        Elder,
        onSelect,
        dimmed = refusals::containsKey,
    ) { code ->
        BagIcon(code, Modifier.fillMaxSize(), tint = if (index.essence(code)?.special == true) GoldBright else Elder)
    }
}

/**
 * The omens the bag holds for the chosen orb - ячейками лотка ([OmenTray]): one may be laid on the next use, the one that goes on the
 * [target] - an item. Chosen again, it is taken off. Each is the orb's pair on the anvil (mockup B).
 */
@Composable fun OmenLedger(game: GameUi, orbCode: String, chosenOmen: String, target: OrbTarget, held: List<Omen>, onSelect: (String) -> Unit) {
    val index = game.index ?: return
    val orb = Orb.of(orbCode) ?: return
    val applier = remember(index) { OrbApplier(index) }
    val omens = remember(applier, orb, target, held) { held.filter { it.fits(orb) && applier.accepts(orb, target, it) } }
    OmenTray(game, ui("forge.tray_omens"), omens, chosenOmen, onSelect)
}

/**
 * Знамения [omens], что лягут рядом с валютой на наковальне (сфера или печать Атласа), - лотком под заголовком [title]: выбранное
 * снова - снимается. Имя и описание выбранного называет рецепт наковальни.
 */
@Composable fun OmenTray(game: GameUi, title: String, omens: List<Omen>, chosenOmen: String, onSelect: (String) -> Unit) {
    ForgeTray(game, title, omens.map { it.code }, chosenOmen, Rune, { code -> onSelect(if (code == chosenOmen) "" else code) }) {
        Icon(ForgeGlyphs.Sigil, null, tint = Rune, modifier = Modifier.fillMaxSize())
    }
}

