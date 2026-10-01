package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.TAB_HERO
import com.sperance.exileforge.presentation.state.TAB_SKILLS
import com.sperance.exileforge.presentation.state.TAB_TREE
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * The screens the Hero tab holds (3.24.0): the gear, the tree and the grimoire — the forge moved to «Развитие». They were
 * buttons in the header's corner; a strip over them says where the player is and reaches the rest
 * in one tap, and the bottom bar's «Hero» stays lit for each.
 */
enum class HeroTab(val tab: Int, private val title: String, val icon: ImageVector) {
    GEAR(TAB_HERO, "hero.tab_gear", ForgeGlyphs.Helm), TREE(TAB_TREE, "nav.tree", ForgeGlyphs.Constellation),
    GRIMOIRE(TAB_SKILLS, "nav.skills", ForgeGlyphs.Grimoire);

    val label: String get() = ui(title)

    companion object {
        /** The Hero tab's screen a tab index is, if it is one. */
        fun of(tab: Int): HeroTab? = entries.firstOrNull { it.tab == tab }
    }
}

/** The strip over the Hero tab's screens: a glyph over a short label each, the open one underlined in gold. */
@Composable fun HeroTabStrip(selected: HeroTab, onSelect: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().background(Abyss)) {
            HeroTab.entries.forEach { entry ->
                val on = entry == selected
                Column(Modifier.weight(1f).selectable(selected = on, role = Role.Tab, onClick = { if (!on) onSelect(entry.tab) }).padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(entry.icon, null, tint = if (on) Gold else Muted, modifier = Modifier.size(18.dp))
                    Text(entry.label, color = if (on) GoldBright else Muted, style = MaterialTheme.typography.labelSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.fillMaxWidth().height(2.dp).background(if (on) Gold else Color.Transparent))
                }
            }
        }
        HorizontalDivider(color = PanelRaised)
    }
}
