package com.sperance.exileforge.ui.screens.contest

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.contestMetricTitle
import com.sperance.exileforge.core.display.contestPastTitle
import com.sperance.exileforge.core.display.contestValue
import com.sperance.exileforge.core.display.leagueLabel
import com.sperance.exileforge.core.display.prizeLine
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContestKind
import com.sperance.exileforge.rules.content.ContestMetric
import com.sperance.exileforge.rules.content.ContestPast
import com.sperance.exileforge.rules.content.ContestResultRow
import com.sperance.exileforge.rules.content.ContestResults
import com.sperance.exileforge.rules.content.ContestRules
import com.sperance.exileforge.ui.components.BronzePills
import com.sperance.exileforge.ui.components.ClassPortrait
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.SheetBody
import com.sperance.exileforge.ui.components.medalOf
import com.sperance.exileforge.ui.components.opensPlayer
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.screens.expedition.roman
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.Vital
import com.sperance.exileforge.ui.theme.depthPanel

/** Сколько первых мест лиги - римскими цифрами цветом медали. */
private const val MEDAL_PLACES = 3

/**
 * Итоги прошлого периода вида [kind] листом: заголовок «Итоги дня 23.10» / «Итоги недели 13–19.10» (у меры «босс» - с боссом), под ним
 * мера и число героев лиги; лиги плашками ([BronzePills]) - лига героя первой и выбрана; первые места строками ([PastRow]: место, портрет
 * класса, имя, класс и уровень на момент итогов, мера, приз - без приза пусто), свой герой - рамкой и цветом Vital; вне верха - строка
 * «Ваше место: 37». Итогов ещё нет - одна строка словами. [past] - ответ сервера (null - читается, [loading]; не прочитался - словами).
 */
@Composable internal fun ContestPastSheet(game: GameUi, rules: ContestRules, kind: ContestKind, past: ContestPast?, loading: Boolean, onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss, body = SheetBody.Scroll(spacing = 8.dp)) {
        when {
            past == null -> MutedText(ui(if (loading) "common.loading" else "contest.past_failed"))
            past.leagues.isEmpty() -> PastEmpty(kind)
            else -> PastLeagues(game, rules, past)
        }
    }
}

/** Итогов прошлого периода ещё нет: заголовок вида и одна строка словами. */
@Composable private fun PastEmpty(kind: ContestKind) {
    Text(ui(if (kind == ContestKind.DAY) "contest.past_day_none" else "contest.past_week_none"), style = relicName(18).copy(color = GoldBright))
    MutedText(ui("contest.past_none"))
}

/** Итоги по лигам: лига героя первой и выбрана, остальные - по порядку правил. */
@Composable private fun PastLeagues(game: GameUi, rules: ContestRules, past: ContestPast) {
    val order = remember(rules, past) { listOf(past.mine) + rules.leagues.indices.filter { it != past.mine } }
    var league by rememberSaveable(past.kind) { mutableStateOf(past.mine) }
    val shown = past.leagues.firstOrNull { it.league == league }
    val any = past.leagues.first()
    Text(contestPastTitle(rules, past.kind, any.period, any.boss), style = relicName(18).copy(color = GoldBright))
    MutedText(ui("contest.past_about", contestMetricTitle(any.metric), shown?.size ?: 0))
    BronzePills(
        order.map { if (it == past.mine) ui("contest.league_mine", leagueLabel(rules, it)) else leagueLabel(rules, it) },
        order.indexOf(league).coerceAtLeast(0),
        { league = order[it] },
    )
    if (shown == null || shown.top.isEmpty()) {
        MutedText(ui("contest.past_league_empty"))
        return
    }
    shown.top.forEach { row -> PastRow(game, shown.metric, row, row.id == game.heroId) }
    OwnPlace(shown)
}

/**
 * Строка итогов: место (первые три - римской цифрой цветом медали), портрет класса, имя, класс и уровень на момент итогов, мера и приз
 * места ([prizeLine]; без приза - пусто). [own] - свой герой: рамка и цвет Vital, для чтеца экрана - «вы». Касание - карточка игрока;
 * героя больше нет ([ContestResultRow.gone]) - строка не нажимается.
 */
@Composable private fun PastRow(game: GameUi, metric: ContestMetric, row: ContestResultRow, own: Boolean) {
    val shape = RoundedCornerShape(8.dp)
    val you = ui("contest.past_you")
    Row(
        Modifier.fillMaxWidth().depthPanel(shape).then(if (own) Modifier.border(1.dp, Vital.copy(alpha = .6f), shape) else Modifier).clip(shape)
            .opensPlayer(row.id.takeUnless { row.gone })
            .then(if (own) Modifier.semantics { stateDescription = you } else Modifier)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val medal = row.place <= MEDAL_PLACES
        Text(
            if (medal) roman(row.place) else row.place.toString(),
            color = when {
                own -> Vital
                medal -> medalOf(row.place)
                else -> Muted
            },
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.widthIn(min = 28.dp),
        )
        ClassPortrait(row.heroClass.takeIf { it.isNotBlank() }, game.world.portraits, Modifier.size(28.dp), round = true, ring = if (row.honoured) medalOf(row.place) else Bronze)
        Column(Modifier.weight(1f)) {
            Text(row.name, color = if (own) Vital else Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(row.heroClass.takeIf { it.isNotBlank() }?.let(::classTitle), ui("hall.level", row.level)).joinToString(" · "), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(contestValue(metric, row.value), color = GoldBright, style = MaterialTheme.typography.bodyMedium)
            row.prize?.let { Text(prizeLine(it, row.honoured), color = Rune, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        }
    }
}

/** Место героя вне верха итогов [results]: «Ваше место: 37» и его мера; в верхе или не стоял в лиге - ничего. */
@Composable private fun OwnPlace(results: ContestResults) {
    val own = results.own?.takeIf { place -> results.top.none { it.place == place.place } } ?: return
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(ui("contest.past_own", own.place), color = Vital, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(contestValue(results.metric, own.value), color = GoldBright, style = MaterialTheme.typography.bodyMedium)
    }
}
