package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.QualityForecast
import com.sperance.exileforge.core.display.Term
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.forge.Smithy
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.roll.OrbApplier
import com.sperance.exileforge.rules.roll.OrbTarget
import com.sperance.exileforge.ui.components.ExpandableText
import com.sperance.exileforge.ui.components.TermsBlock
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.theme.*

/** A tray cell's side: compact, so two rows of the tray stay short (the owner's word on mockup B). */
private val CELL = 44.dp
private val GAP = 6.dp

/** Past this many stacks the tray lays them in two rows and scrolls sideways; fewer fit one row. */
private const val ONE_ROW = 6

/**
 * The tray of the forge (mockup B): a compact grid of what can be laid on the anvil — only stacks the bag holds and the item
 * takes, nothing greyed — each a drawing with its count; under it the chosen one's name and what it does.
 * [codes] are in their shelf order; [glyph] draws one; [note] adds a line under the chosen one's words.
 */
@Composable internal fun ForgeTray(
    game: GameUi,
    title: String,
    codes: List<String>,
    chosen: String,
    empty: String,
    accent: Color,
    onSelect: (String) -> Unit,
    note: String? = null,
    refusal: (String) -> String? = { null },
    glyph: @Composable (String) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth().depthPanel(shape).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = accent, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            if (codes.isNotEmpty()) Text(codes.size.toString(), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        if (codes.isEmpty()) {
            Text(empty, color = Muted, style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        val rows = if (codes.size > ONE_ROW) 2 else 1
        LazyHorizontalGrid(
            GridCells.Fixed(rows),
            Modifier.fillMaxWidth().height(CELL * rows + GAP * (rows - 1)),
            horizontalArrangement = Arrangement.spacedBy(GAP),
            verticalArrangement = Arrangement.spacedBy(GAP),
        ) {
            items(codes, key = { it }) { code ->
                Box(Modifier.alpha(if (refusal(code) == null) 1f else .4f)) {
                    TrayCell(itemTitle(code), game.bagAmount(code) ?: 0L, code == chosen, accent, { onSelect(code) }) { glyph(code) }
                }
            }
        }
        if (chosen in codes) {
            val refused = refusal(chosen)
            if (refused != null) Text(refused, color = LifeRed, style = MaterialTheme.typography.bodySmall) else TrayTip(itemTitle(chosen), itemDescription(chosen), note)
        }
    }
}

/** One stack of the tray: its drawing, the count in the corner, and a gold frame when it lies on the anvil. */
@Composable private fun TrayCell(name: String, count: Long, selected: Boolean, accent: Color, onClick: () -> Unit, glyph: @Composable () -> Unit) {
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
    }
}

/** The chosen stack's name and what it does, and a [note] of the forge's (an omen it still waits for). */
@Composable private fun TrayTip(title: String, body: String, note: String?) {
    val shape = RoundedCornerShape(10.dp)
    Row(Modifier.fillMaxWidth().background(PanelRaised, shape).padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.Info, null, tint = Gold, modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = GoldBright, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            if (body.isNotBlank()) Text(body, color = Parchment, style = MaterialTheme.typography.bodySmall)
            note?.let { Text(it, color = Rune, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

/**
 * The orbs of the tray: the bag's, Regret left out (it is spent on the tree). Те, что вещь не примет ([refusals] - отказ правил
 * [OrbApplier.refusal] по коду, 4.2.0: сфера удачи без уникалки того же вида и прочие), серыми в конце; выбранная говорит почему.
 */
@Composable internal fun OrbTray(
    game: GameUi,
    smithy: Smithy,
    accepted: (String) -> Boolean,
    refusals: Map<String, String>,
    needsOmen: (String) -> Boolean,
    quality: QualityForecast?,
    onSelect: (String) -> Unit,
) {
    val hero = game.hero ?: return
    val orbs = game.orbs.map { it.code.value }.filter { hero.count(it) > 0 && it != Orb.ORB_OF_REGRET.name && (accepted(it) || it in refusals) }
        .sortedBy { it in refusals }
    val chosen = smithy.orb
    ForgeTray(
        game,
        ui("forge.tray_orbs"),
        orbs,
        chosen,
        ui("forge.no_orbs_fit"),
        Gold,
        onSelect,
        note = if (chosen in orbs && needsOmen(chosen) && smithy.omen.isBlank()) ui("forge.needs_omen") else qualityNote(chosen, quality),
        refusal = refusals::get,
    ) { code ->
        OrbGlyph(Orb.of(code), Modifier.fillMaxSize())
    }
}

/** Подсказка сферы качества (4.3.0): что она поднимет на этой вещи с выбранным знамением и что сбросит. */
private fun qualityNote(orb: String, quality: QualityForecast?): String? = quality?.takeIf { orb == Orb.QUALITY_ORB.name }?.let { forecast ->
    listOfNotNull(forecast.raises, ui("forge.quality_reset", forecast.lost).takeIf { forecast.lost > 0 }).joinToString("\n")
}

/**
 * The essences of the tray (2.78.0): the bag's, the special ones last and the higher tiers first. С 3.94.1 и те, что вещь не
 * примет ([refusals] - отказ правил по коду): серыми в конце, выбранная говорит почему.
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
        ui("forge.no_essences"),
        Elder,
        onSelect,
        note = ui("essence.note"),
        refusal = refusals::get,
    ) { code ->
        BagIcon(code, Modifier.fillMaxSize(), tint = if (index.essence(code)?.special == true) GoldBright else Elder)
    }
}

/**
 * The omens the bag holds for the chosen orb (3.36.0). Описание знамени раскрывается нажатием ([ExpandableText], 4.2.0): one may be laid on the next use, the one that goes on the [target] -
 * an item or, since server 1.65.0, a pet. Chosen again, it is taken off. Each is the orb's pair on the anvil (mockup B).
 */
@Composable fun OmenLedger(game: GameUi, orbCode: String, chosenOmen: String, target: OrbTarget, held: List<Omen>, onSelect: (String) -> Unit) {
    val hero = game.hero ?: return
    val index = game.index ?: return
    val orb = Orb.of(orbCode) ?: return
    val applier = remember(index) { OrbApplier(index) }
    val omens = remember(applier, orb, target, held) { held.filter { it.fits(orb) && applier.accepts(orb, target, it) } }
    if (omens.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        omens.forEach { omen ->
            val chosen = omen.code == chosenOmen
            val shape = RoundedCornerShape(10.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(if (chosen) Rune.copy(alpha = .10f) else Panel, shape)
                    .border(1.dp, if (chosen) Rune else PanelRaised, shape).clickable(role = Role.Button) { onSelect(if (chosen) "" else omen.code) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(ForgeGlyphs.Sigil, null, tint = Rune, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        itemTitle(omen.code),
                        color = if (chosen) GoldBright else Parchment,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ExpandableText(itemDescription(omen.code), style = MaterialTheme.typography.labelSmall)
                }
                Text("×${hero.count(omen.code)}", color = GoldBright, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
        // Общее правило катализаторов (4.2.0) - термином, один раз: описание каждого называет лишь свой вид.
        if (omens.any { it.catalyst != null }) TermsBlock(listOf(Term.CATALYST))
    }
}
