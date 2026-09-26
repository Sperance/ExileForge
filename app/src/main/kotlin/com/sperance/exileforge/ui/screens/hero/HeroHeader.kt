package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import com.sperance.exileforge.ui.components.MutedText
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.ClassPortrait
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * Who the character is, above the Hero tab's sections: the one part every section shares.
 *
 * The name, then class and level as one line, then the purse and the tree as two chips, and the
 * experience as a thin bar under them — the progress a player checks at a glance, which used to be a
 * card of its own. The tree and the forge open from the corner, because it is reached from here but is not a
 * part of the hero.
 */
@Composable fun HeroHeader(s: ForgeState, onTree: () -> Unit, onSkills: () -> Unit = {}, onForge: () -> Unit) {
    val hero = s.play.hero ?: return
    val character = hero.character
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // The class's portrait as the map's token (since 2.31.0).
            ClassPortrait(s.heroClass?.code, s.world.portraits, Modifier.size(64.dp), round = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(character.name, style = MaterialTheme.typography.headlineSmall, color = GoldBright)
                // The class is the base every percentage is counted from; the server owns it.
                Text(ui("hero.class_level", s.heroClass?.title.orEmpty().ifBlank { ui("hero.unknown_class") }, character.level),
                    color = Rune, style = MaterialTheme.typography.labelLarge)
            }
            // The tree left the bar in 2.40.0 and opens from here, beside the forge.
            IconButton(onClick = onTree) {
                Icon(ForgeGlyphs.Constellation, ui("nav.tree"), tint = Gold, modifier = Modifier.size(24.dp))
            }
            // The grimoire (2.78.0): the class's skills and the belt.
            IconButton(onClick = onSkills) {
                Icon(ForgeGlyphs.Grimoire, ui("nav.skills"), tint = Gold, modifier = Modifier.size(24.dp))
            }
            IconButton(enabled = !s.busy, onClick = onForge) {
                Icon(ForgeGlyphs.Tome, ui("nav.craft"), tint = Gold, modifier = Modifier.size(24.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(ForgeGlyphs.Coins, ui("hero.gold_chip", number(character.money.toDouble())))
            Chip(ForgeGlyphs.Constellation, ui("hero.tree_chip", hero.tree.available, hero.tree.total))
        }
        ExperienceLine(s, character.level, character.experience)
    }
}

/** One figure the header carries, framed as a chip: a drawing and a short line. */
@Composable private fun Chip(icon: ImageVector, text: String) {
    val shape = RoundedCornerShape(50)
    Row(Modifier.border(1.dp, Bronze, shape).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(14.dp))
        Text(text, color = GoldBright, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * How far along this level the character is, as a thin bar with what it leads to.
 *
 * The level table says what the next level costs — the client reads it to show what is coming,
 * never to work out a level, which stays the server's to decide. At the last level the bar is whole
 * and says so; without the table there is no scale, and only the total is printed.
 */
@Composable private fun ExperienceLine(s: ForgeState, level: Int, experience: Double) {
    val floor = s.world.levels.firstOrNull { it.level == level }?.experience ?: 0.0
    val next = s.world.levels.firstOrNull { it.level == level + 1 }
    val span = next?.let { it.experience - floor } ?: 0.0
    val fraction = if (span > 0.0) ((experience - floor).coerceAtLeast(0.0) / span).toFloat().coerceIn(0f, 1f) else 1f
    val label = when {
        s.world.levels.isEmpty() -> ui("hero.xp_total", number(experience))
        next == null -> ui("hero.xp_last")
        else -> ui("hero.xp_to_next", Math.round(fraction * 100), level + 1)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(ui("hero.xp_short"), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            MutedText(label, style = MaterialTheme.typography.labelSmall)
        }
        if (s.world.levels.isNotEmpty()) Box(Modifier.fillMaxWidth().height(6.dp).background(PanelRaised, RoundedCornerShape(3.dp))) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(Gold, RoundedCornerShape(3.dp)))
        }
    }
}
