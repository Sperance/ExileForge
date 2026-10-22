package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContestHonour
import com.sperance.exileforge.rules.content.ContestKind
import com.sperance.exileforge.rules.content.ContestMetric
import com.sperance.exileforge.rules.content.ContestRules
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.PlacePrize

/** Вид состязания словами игрока: «Дневное», «Недельное». */
fun contestKindTitle(kind: ContestKind): String = ui("contest.kind.${kind.name}")

/** Мера состязания словами игрока: «Уникальные находки», «Быстрейший босс», «Опыт за период». */
fun contestMetricTitle(metric: ContestMetric): String = ui("contest.metric.${metric.name}")

/** Мера с боссом периода: «Быстрейший босс · Король Бездны»; у прочих мер - только мера. */
fun contestMetricLine(metric: ContestMetric, boss: MonsterCode?): String = boss?.let { ui("contest.metric_boss", contestMetricTitle(metric), monsterTitle(it)) } ?: contestMetricTitle(metric)

/** Титул состязания: «Гроза боссов недели». */
fun contestTitle(honour: ContestHonour): String = contestTitle(honour.metric, honour.kind)

/** Титул первых мест меры [metric] в состязании вида [kind]. */
fun contestTitle(metric: ContestMetric, kind: ContestKind): String = ui("contest.title.${metric.name}.${kind.name}")

/** Лига [league] уровнями: «20–39», последняя - «60+». */
fun leagueLabel(rules: ContestRules, league: Int): String {
    val from = rules.leagues.getOrNull(league) ?: return ""
    val next = rules.leagues.getOrNull(league + 1)
    return if (next == null) ui("contest.league_open", from) else ui("contest.league_range", from, next - 1)
}

/** Мера строки: уникальные и опыт - числом, бой с боссом - «1:47,3» (мс). */
fun contestValue(metric: ContestMetric, value: Long): String = when (metric) {
    ContestMetric.BOSS -> bossTime(value)
    ContestMetric.UNIQUES, ContestMetric.EXPERIENCE -> groupedNumber(value)
}

/** Время боя [millis] до десятых: «1:47,3». */
fun bossTime(millis: Long): String {
    val tenths = (millis.coerceAtLeast(0) + 50) / 100
    val seconds = tenths / 10
    return ui("contest.time", seconds / 60, (seconds % 60).toString().padStart(2, '0'), tenths % 10)
}

/** Разница мер [gap] до цели словами: уникальные и опыт - «ещё 5», бой - «на 5,4 с быстрее». */
fun contestGap(metric: ContestMetric, gap: Long): String = when (metric) {
    ContestMetric.BOSS -> ui("contest.gap.BOSS", fineNumber(gap.coerceAtLeast(0) / 1000.0))
    ContestMetric.UNIQUES, ContestMetric.EXPERIENCE -> ui("contest.gap.more", groupedNumber(gap.coerceAtLeast(1)))
}

/** Награда места: «×5» сфер, у первых мест - ещё «титул и рамка». */
fun prizeLine(prize: PlacePrize, honoured: Boolean): String = if (honoured) ui("contest.prize_honoured", prize.orbs) else ui("contest.prize_orbs", prize.orbs)
