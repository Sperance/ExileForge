package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.DeathHit
import com.sperance.exileforge.core.campaign.RunStats
import com.sperance.exileforge.core.campaign.RunSummary
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.locFirst
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.theme.*

/**
 * The run's figures: three numbers at once — damage a second, damage taken, kills a minute. The combat's own events, summed.
 *
 * Разбор всего захода - кнопкой, тот же лист, что у итога боя («Весь заход»).
 */
@Composable internal fun RunFigures(summary: RunSummary) {
    if (summary.seconds <= 0) return
    Caption(ui("run.figures"))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Figure(ui("run.dps"), number(summary.dps))
        Figure(ui("run.taken"), number(summary.totalTaken))
        Figure(ui("run.kpm"), number(summary.killsPerMinute))
    }
    var breakdown by remember { mutableStateOf(false) }
    ForgeButton(onClick = { breakdown = true }, kind = ButtonKind.Text) { Text(ui("breakdown.open")) }
    if (breakdown) FightStatsSheet(null, summary.fights) { breakdown = false }
}

/**
 * What killed the hero: the last blows, oldest first — who struck, how hard, of what — and one line of advice
 * from the type that hurt most.
 */
@Composable internal fun DeathRecap(recap: List<DeathHit>) {
    if (recap.isEmpty()) return
    Caption(ui("run.recap"), LifeRed)
    recap.forEach { hit ->
        val type = hit.type?.let(::typeTitle) ?: ui("run.type_none")
        Text(
            ui(
                if (hit.crit) "run.recap_line_crit" else "run.recap_line",
                monsterTitle(hit.monster.code),
                number(hit.damage),
                type,
                skillTitle(hit.skill),
                number(hit.lifeAfter.coerceAtLeast(0.0)),
            ),
            color = Parchment,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    recap.groupBy { it.type }.maxByOrNull { (_, hits) -> hits.sumOf { it.damage } }?.key?.let { worst ->
        MutedText(ui("run.advice.${worst.name}"))
    }
}

@Composable private fun Figure(label: String, value: String) {
    Column {
        Text(value, color = GoldBright, style = MaterialTheme.typography.titleMedium)
        MutedText(label, style = MaterialTheme.typography.labelSmall)
    }
}

internal fun typeTitle(type: DamageType): String = ui("enum.damage.${type.name}")

internal fun skillTitle(code: String): String = when (code) {
    RunStats.ATTACK, RunStats.TICK, RunStats.REFLECT -> ui("run.skill.$code")
    else -> locFirst(LocaleKey.skillName(code), LocaleKey.traitName(code))
}
