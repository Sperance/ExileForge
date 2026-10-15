package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.FightStats
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.PillTabs
import com.sperance.exileforge.ui.screens.expedition.arena.damageTint
import com.sperance.exileforge.ui.theme.*

/**
 * Разбор боя (0.14.4, #30, макет «Разбор боя», вариант A «Три вкладки»): «Нанесено», «Получено» и «Защита» - плитки итога сверху,
 * затем полосы по типам урона, умениям, запасам и защитам. Переключатель «Этот бой / Весь заход» - когда есть оба: [fight] -
 * последний бой (итог боя), [run] - весь заход. Оценки избежанного урона помечены «≈».
 */
@Composable internal fun FightStatsSheet(fight: FightStats?, run: FightStats?, onDismiss: () -> Unit) {
    val scopes = listOfNotNull(fight?.let { ui("breakdown.scope.fight") to it }, run?.let { ui("breakdown.scope.run") to it })
    if (scopes.isEmpty()) return
    var scope by rememberSaveable { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val stats = scopes[scope.coerceIn(scopes.indices)].second
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(ui("breakdown.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            MutedText(ui("breakdown.seconds", fineNumber(stats.seconds)))
            if (scopes.size > 1) PillTabs(scopes.map { it.first }, scope, { scope = it }, segmented = true)
            PillTabs(BreakdownTab.entries.map { ui(it.key) }, tab, { tab = it })
            when (BreakdownTab.entries[tab]) {
                BreakdownTab.DEALT -> DealtPage(stats)
                BreakdownTab.TAKEN -> TakenPage(stats)
                BreakdownTab.GUARD -> GuardPage(stats)
            }
        }
    }
}

/** Вкладки разбора. */
private enum class BreakdownTab(val key: String) {
    DEALT("breakdown.tab.dealt"),
    TAKEN("breakdown.tab.taken"),
    GUARD("breakdown.tab.guard"),
}

/** Нанесено: итог, урон в секунду, доля критов, сильнейший удар; по типам и умениям; что срезали защиты врагов. */
@Composable private fun DealtPage(stats: FightStats) {
    val dealt = stats.dealt
    Kpis(
        number(dealt.total) to ui("breakdown.kpi.total"),
        number(stats.dps) to ui("breakdown.kpi.per_second"),
        ui("breakdown.percent", if (dealt.hits > 0) Math.round(dealt.crits * 100.0 / dealt.hits).toInt() else 0) to ui("breakdown.kpi.crits"),
        number(dealt.strongest) to ui("breakdown.kpi.strongest"),
    )
    if (dealt.total <= 0) return MutedText(ui("breakdown.empty"))
    Bars(ui("breakdown.by_type"), dealt.byType.typed(), dealt.total)
    Bars(ui("breakdown.by_skill"), dealt.bySkill.entries.map { Bar(skillTitle(it.key), it.value, Gold) }, dealt.total)
    val cut = dealt.armour + dealt.resisted.values.sum()
    if (cut > 0) Bars(ui("breakdown.foes_cut"), listOf(Bar(ui("breakdown.armour"), dealt.armour, Brass)) + dealt.resisted.typed(), cut)
    Chips(
        ui("breakdown.chip.hits", dealt.hits),
        ui("breakdown.chip.crits", dealt.crits),
        ui("breakdown.chip.missed", dealt.missed).takeIf { dealt.missed > 0 },
        ui("breakdown.chip.foes_blocked", dealt.blocked).takeIf { dealt.blocked > 0 },
    )
}

/** Получено: итог, в секунду, сильнейший, ударов; по типам; куда легло - барьер, щит, мана, отложенный, здоровье и недуги. */
@Composable private fun TakenPage(stats: FightStats) {
    val taken = stats.taken
    Kpis(
        number(taken.total) to ui("breakdown.kpi.total"),
        number(stats.takenPerSecond) to ui("breakdown.kpi.per_second"),
        number(taken.strongest) to ui("breakdown.kpi.strongest"),
        taken.hits.toString() to ui("breakdown.kpi.hits"),
    )
    if (taken.total <= 0) return MutedText(ui("breakdown.untouched"))
    Bars(ui("breakdown.by_type"), taken.byType.typed(onHero = true), taken.total)
    val pools = listOf(
        Bar(ui("breakdown.pool.barrier"), taken.barrier, GoldBright),
        Bar(ui("breakdown.pool.shield"), taken.shield, ShieldCyan),
        Bar(ui("breakdown.pool.mana"), taken.mana, ManaBlue),
        Bar(ui("breakdown.pool.delayed"), taken.delayed, Elder),
        Bar(ui("breakdown.pool.life"), taken.life, LifeRed),
        Bar(ui("breakdown.pool.ailments"), taken.ailments, Ember),
    )
    Bars(ui("breakdown.landed"), pools, pools.sumOf { it.amount })
}

/** Защита: срезано бронёй и сопротивлениями по типам, избежано уклонением и блоком (≈), восстановлено флягами, вампиризмом и регенерацией. */
@Composable private fun GuardPage(stats: FightStats) {
    val guard = stats.guard
    val resisted = guard.resisted.values.sum()
    Kpis(
        number(guard.armour) to ui("breakdown.kpi.armour"),
        number(resisted) to ui("breakdown.kpi.resists"),
        ui("breakdown.estimate", number(guard.evadedDamage)) to ui("breakdown.kpi.evaded", guard.evaded),
        ui("breakdown.estimate", number(guard.blockedDamage)) to ui("breakdown.kpi.blocked", guard.blocked),
    )
    val cut = guard.armour + resisted
    if (cut > 0) Bars(ui("breakdown.cut"), listOf(Bar(ui("breakdown.armour"), guard.armour, Brass)) + guard.resisted.typed(onHero = true), cut)
    val back = listOf(
        Bar(ui("breakdown.back.flasks"), guard.flasks, Vital),
        Bar(ui("breakdown.back.leech"), guard.leech, LifeRed),
        Bar(ui("breakdown.back.regen"), guard.regen, Gold),
    )
    Bars(ui("breakdown.back"), back, back.sumOf { it.amount })
    MutedText(ui("breakdown.estimate_note"), style = MaterialTheme.typography.labelSmall)
}

/** Полоса разбора: подпись, число и цвет. */
private data class Bar(val name: String, val amount: Double, val tint: Color)

/** Урон по типам - полосами цвета своей стихии; по герою физический - кровью, как числа боя. */
private fun Map<DamageType, Double>.typed(onHero: Boolean = false): List<Bar> = entries.map { (type, amount) -> Bar(typeTitle(type), amount, damageTint(type, onHero)) }

/** Четыре плитки итога: число и подпись. */
@Composable private fun Kpis(vararg tiles: Pair<String, String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tiles.forEach { (value, label) ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Abyss).border(1.dp, Bronze, RoundedCornerShape(8.dp)).padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(value, color = GoldBright, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(label, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

/** Раздел полос [title]: каждая ненулевая часть - подпись, полоса доли от [total], число и процент; большие сверху. */
@Composable private fun Bars(title: String, bars: List<Bar>, total: Double) {
    val shown = bars.filter { it.amount >= 1 }.sortedByDescending { it.amount }
    if (shown.isEmpty() || total <= 0) return
    Caption(title)
    shown.forEach { bar ->
        val share = (bar.amount / total).coerceIn(0.0, 1.0).toFloat()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(bar.name, color = Parchment, style = MaterialTheme.typography.bodySmall, maxLines = 1, modifier = Modifier.width(BAR_LABEL))
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Ink)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(share).background(bar.tint))
            }
            Text(ui("run.share", number(bar.amount), Math.round(share * 100.0).toInt()), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Фишки счётчиков; null - пропуск. */
@Composable private fun Chips(vararg chips: String?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        chips.filterNotNull().forEach { chip ->
            Text(
                chip,
                color = Parchment,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Abyss).border(1.dp, Bronze, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

/** Ширина подписи полосы. */
private val BAR_LABEL = 96.dp
