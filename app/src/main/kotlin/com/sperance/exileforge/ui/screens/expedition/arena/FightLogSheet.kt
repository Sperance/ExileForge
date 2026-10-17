package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.campaign.combat.CombatEvent
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.campaign.run.FightReport
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.screens.expedition.CombatDetailSheet
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Vital
import org.koin.compose.viewmodel.koinViewModel

/**
 * Журнал законченного боя листом: «Журнал боя» меню забега и плашек итога Разлома и испытаний. Все вступившие враги
 * [report] - лентой по времени или по противникам, полки журнала - те же, что у экрана боя; строка открывает свой расчёт.
 */
@Composable internal fun FightLogSheet(report: FightReport, onDismiss: () -> Unit) {
    val shell: ShellViewModel = koinViewModel()
    val game by shell.game.collectAsStateWithLifecycle()
    var line by remember { mutableStateOf<Pair<CombatEvent, String>?>(null) }
    val won = report.outcome == Outcome.WIN
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.85f).navigationBarsPadding().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(ui("fight.log_title", monsterTitle(report.monster.code)), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            Text(
                ui(if (won) "fight.log_won" else "fight.log_lost", report.packSize, report.dealt, report.taken, String.format(java.util.Locale.ROOT, "%.1f", report.duration)),
                color = if (won) Vital else LifeRed,
                style = MaterialTheme.typography.labelMedium,
            )
            LogShelves(game.logFilter, shell::logFilter)
            FightLog(report.pack, Modifier.fillMaxWidth().weight(1f), game.logFilter, toDeath = !won) { event, name -> line = event to name }
        }
    }
    line?.let { (event, name) -> CombatDetailSheet(game, event, name) { line = null } }
}
