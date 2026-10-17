package com.sperance.exileforge.ui.screens.crafts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.crafts.Absence
import com.sperance.exileforge.core.display.workTitle
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeOutlinedButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Rune
import org.koin.compose.viewmodel.koinViewModel

/**
 * Лист «Пока вас не было» - первый досчёт после отлучки ([Absence]): луна и сколько длилась отлучка, какая работа и идёт ли она дальше;
 * учёт отлучки ([WorkLedger]) - циклы, опыт, сделано, уровень профессии, стопки и сделанные вещи; внизу «В тайник» (когда вещи
 * сделаны - тайник с фильтром «Сделано ремеслом») и «Понятно». Любое закрытие - [onSeen]: отлучка забыта, метка вкладки гаснет.
 */
@Composable internal fun AwaySheet(game: GameUi, away: Absence, onSeen: () -> Unit) {
    val heroModel = koinViewModel<HeroViewModel>()
    val made = away.gains.equipment
    val sums = remember(away) { WorkSums.of(away.gains) }
    ForgeSheet(onDismissRequest = onSeen) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(Brush.radialGradient(listOf(PanelRaised, Abyss)), CircleShape).border(1.dp, Muted.copy(alpha = .4f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.DarkMode, null, tint = Rune, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(ui("crafts.away_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOf(duration(away.millis), workTitle(away.job, away.choice), ui(if (away.running) "crafts.away_running" else "crafts.away_stopped")).joinToString(" · "),
                        color = Muted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            WorkLedger(game, sums, made, makes = made.isNotEmpty(), level = away.level)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (made.isNotEmpty()) {
                    ForgeOutlinedButton(
                        onClick = {
                            onSeen()
                            heroModel.showStash(ItemShelf.STASH.defaults.copy(crafted = true))
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                    ) {
                        Icon(ForgeGlyphs.Stash, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(ui("crafts.away_stash"))
                    }
                }
                ForgeButton(onClick = onSeen, modifier = Modifier.weight(1f).height(44.dp)) { Text(ui("crafts.away_ok")) }
            }
        }
    }
}
