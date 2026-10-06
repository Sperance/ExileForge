package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.ClassesFile
import com.sperance.exileforge.ui.components.ClassPortrait
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.RoleMark
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * Who the hero is, above the Hero tab's sections: the one part every section shares.
 *
 * The name large, then class and level as one smaller line, then the purse and the tree as two chips,
 * and the experience as a thin bar under them. The tree, the grimoire and the forge left the corner
 * for the strip above the tab ([HeroTabStrip]); the title the chronicle opened is chosen on its page under «Развитие».
 * A long name is cut rather than pushed into the portrait. A section may add its own chips after them ([extra]).
 */
@Composable fun HeroHeader(game: GameUi, extra: @Composable RowScope.() -> Unit = {}) {
    rememberHeroHeader(game)?.let { HeroHeader(it, extra) }
}

/** The header over its own cut of the state (3.66.0): it alone is redrawn when the purse or the experience moves. */
@Composable fun HeroHeader(hero: HeroHeaderState, extra: @Composable RowScope.() -> Unit = {}) {
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // The class's portrait as the map's token (since 2.31.0).
            ClassPortrait(hero.heroClass, hero.portraits, Modifier.size(64.dp), round = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(hero.name, style = MaterialTheme.typography.headlineSmall, color = GoldBright, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    RoleMark(hero.role, 18.dp)
                }
                // The class is the base every percentage is counted from; the server owns it.
                Text(
                    ui("hero.class_level", hero.heroClass.takeIf { it.isNotBlank() }?.let(::classTitle) ?: ui("hero.unknown_class"), hero.level),
                    color = Rune,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(ForgeGlyphs.Coins, ui("hero.gold_chip", number(hero.money.toDouble())))
            // The tree's balance is counted here by the rules (3.0.0), so it waits for the content.
            hero.tree?.let { Chip(ForgeGlyphs.Constellation, ui("hero.tree_chip", it.available, it.total)) }
            extra()
        }
        ExperienceLine(hero.classes, hero.level, hero.experience)
    }
}

/** One figure the header carries, framed as a chip: a drawing and a short line. */
@Composable private fun Chip(icon: ImageVector, text: String) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.border(1.dp, Bronze, shape).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(14.dp))
        Text(text, color = GoldBright, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * How far along this level the hero is, as a thin bar with what it leads to.
 *
 * The classes' experience table says what the next level costs — the client reads it to show what
 * is coming, never to work out a level, which stays the server's to decide. At the last level the
 * bar is whole and says so; without the content there is no scale, and only the total is printed.
 */
@Composable private fun ExperienceLine(classes: ClassesFile?, level: Int, experience: Double) {
    val floor = classes?.threshold(level) ?: 0.0
    val next = classes?.nextThreshold(level)
    val span = next?.let { it - floor } ?: 0.0
    val fraction = if (span > 0.0) ((experience - floor).coerceAtLeast(0.0) / span).toFloat().coerceIn(0f, 1f) else 1f
    val label = when {
        classes == null -> ui("hero.xp_total", number(experience))
        next == null -> ui("hero.xp_last")
        else -> ui("hero.xp_to_next", Math.round(fraction * 100), level + 1)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(ui("hero.xp_short"), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            MutedText(label, style = MaterialTheme.typography.labelSmall)
        }
        if (classes != null) {
            Box(Modifier.fillMaxWidth().height(6.dp).background(PanelRaised, RoundedCornerShape(3.dp))) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(Gold, RoundedCornerShape(3.dp)))
            }
        }
    }
}
