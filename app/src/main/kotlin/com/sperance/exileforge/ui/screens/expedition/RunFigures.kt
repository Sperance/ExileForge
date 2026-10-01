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
import com.sperance.exileforge.core.campaign.DamageType
import com.sperance.exileforge.core.campaign.DeathHit
import com.sperance.exileforge.core.campaign.RunStats
import com.sperance.exileforge.core.campaign.RunSummary
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.ui.components.ForgeTextButton
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.theme.*

/**
 * The run's figures (3.47.0): three numbers at once — damage a second, damage taken, kills a minute — and, opened,
 * the split of the dealt damage by type and by skill and of the taken by type. The combat's own events, summed.
 */
@Composable internal fun RunFigures(summary: RunSummary) {
    if (summary.seconds <= 0) return
    var open by remember { mutableStateOf(false) }
    Caption(ui("run.figures"))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Figure(ui("run.dps"), number(summary.dps))
        Figure(ui("run.taken"), number(summary.totalTaken))
        Figure(ui("run.kpm"), number(summary.killsPerMinute))
    }
    if (summary.petTaken >= 1) MutedText(ui("fight.pet_taken", number(summary.petTaken)), style = MaterialTheme.typography.labelSmall)
    ForgeTextButton(onClick = { open = !open }) { Text(ui(if (open) "run.less" else "run.more")) }
    if (open) {
        Split(ui("run.dealt_by_type"), summary.dealt.mapKeys { typeTitle(it.key) }, summary.totalDealt)
        Split(ui("run.dealt_by_skill"), summary.bySkill.mapKeys { skillTitle(it.key) }, summary.totalDealt)
        Split(ui("run.taken_by_type"), summary.taken.mapKeys { typeTitle(it.key) }, summary.totalTaken)
    }
}

/**
 * What killed the hero (3.47.0): the last blows, oldest first — who struck, how hard, of what — and one line of advice
 * from the type that hurt most.
 */
@Composable internal fun DeathRecap(recap: List<DeathHit>) {
    if (recap.isEmpty()) return
    Caption(ui("run.recap"), LifeRed)
    recap.forEach { hit ->
        val type = hit.type?.let(::typeTitle) ?: ui("run.type_none")
        Text(ui(if (hit.crit) "run.recap_line_crit" else "run.recap_line", monsterTitle(hit.monster.code), number(hit.damage), type,
            skillTitle(hit.skill), number(hit.lifeAfter.coerceAtLeast(0.0))), color = Parchment, style = MaterialTheme.typography.bodySmall)
    }
    recap.groupBy { it.type }.maxByOrNull { (_, hits) -> hits.sumOf { it.damage } }?.key?.let { worst ->
        MutedText(ui("run.advice.${worst.name}"))
    }
}

@Composable private fun Figure(label: String, value: String) {
    Column { Text(value, color = GoldBright, style = MaterialTheme.typography.titleMedium); MutedText(label, style = MaterialTheme.typography.labelSmall) }
}

@Composable private fun Split(title: String, parts: Map<String, Double>, total: Double) {
    if (parts.isEmpty() || total <= 0) return
    Text(title, color = Rune, style = MaterialTheme.typography.labelLarge)
    parts.entries.sortedByDescending { it.value }.forEach { (name, amount) ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, color = Parchment, style = MaterialTheme.typography.bodySmall)
            Text(ui("run.share", number(amount), Math.round(amount / total * 100).toInt()), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun typeTitle(type: DamageType): String = ui("enum.damage.${type.name}")

private fun skillTitle(code: String): String = when (code) {
    RunStats.ATTACK, RunStats.TICK, RunStats.REFLECT -> ui("run.skill.$code")
    else -> locOr(LocaleKey.skillName(code), locOr(LocaleKey.traitName(code), code))
}
