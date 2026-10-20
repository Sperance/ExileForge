package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GiveBlock
import com.sperance.exileforge.core.model.guild.GiveForm
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.core.model.guild.nextRank
import com.sperance.exileforge.core.model.guild.rankProgress
import com.sperance.exileforge.rules.content.GuildRules
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/** Ветвь древа гильдии, что поднимает суточный предел вклада: её имя - в подсказке предела. */
private const val LIMIT_BRANCH = "ECONOMY"

/**
 * Вклад в гильдию - только золото: целиком идёт в казну, в опыт гильдии и в личный вклад героя, то есть в его ранг. Три блока,
 * каждое число по разу: казна - в данных цепочки «назад», кошелёк - в строке заголовка формы, предел дня и остаток - одной строкой
 * «Сегодня ещё N из M» с полосой, свой вклад за неделю - только в своей строке таблицы недели. Пояснения - в подсказках ⓘ.
 *
 * Суточный предел и остаток считает сервер (`GuildRules.dailyLimit` с бонусом древа), годность суммы - [GiveForm]. «Макс.» в поле
 * подставляет, сколько влезает в остаток и в кошелёк. Кнопка снова взводится после удержания, поле чистится только принятым вкладом.
 */
@Composable internal fun ContributeTab(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    var amount by remember { mutableStateOf("") }
    val form = GiveForm(amount.toLongOrNull() ?: 0L, me?.dayLeft, game.goldBill(amount.toLongOrNull() ?: 0L))
    TrailData(ui("guild.treasury_trail", compactNumber(guild.treasuryGold.toDouble())))
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item { GivePanel(game, me, form, amount, onAmount = { amount = it }) { vm.contribute(form.amount) { amount = "" } } }
        me?.let { item { RankPanel(game.index?.guilds, it) } }
        item { WeekPanel(guild, me) }
    }
}

/** Форма вклада: заголовок с кошельком, поле с «Макс.», строка предела дня с полосой и удерживаемая «Внести» с причиной отказа. */
@Composable private fun GivePanel(game: GameUi, me: GuildMember?, form: GiveForm, amount: String, onAmount: (String) -> Unit, onGive: () -> Unit) {
    val block = form.block
    val spent = block == GiveBlock.SPENT
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Engraved(ui("guild.give"), modifier = Modifier.weight(1f))
            form.bill?.let { GoldPrice(it.have) }
        }
        OutlinedTextField(
            amount,
            { onAmount(it.filter(Char::isDigit).take(12)) },
            label = { Text(ui("auction.amount")) },
            singleLine = true,
            enabled = !spent,
            isError = form.over,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = form.most?.takeIf { it > 0 }?.let { most ->
                {
                    ForgeButton(state = ButtonState.of(!game.busy), onClick = { onAmount(most.toString()) }, kind = ButtonKind.Text, size = ButtonSize.Small) {
                        Text(ui("guild.give_max"))
                    }
                }
            },
        )
        me?.takeIf { it.dayLimit > 0 }?.let { DayLimit(game.index?.guilds, it, form.over) }
        ForgeButton(
            onClick = onGive,
            modifier = Modifier.fillMaxWidth(),
            state = ButtonState.of(!game.busy && block == null),
            icon = if (spent) Icons.Outlined.Lock else ForgeGlyphs.Coins,
            hold = Hold(),
        ) { Text(giveLabel(form), maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

/** Подпись «Внести»: причина, по которой нельзя, - на самой кнопке. */
private fun giveLabel(form: GiveForm): String = when (form.block) {
    GiveBlock.SPENT -> ui("guild.give_spent")
    GiveBlock.OVER_LIMIT -> ui("guild.give_over")
    GiveBlock.SHORT -> form.bill?.shortText() ?: ui("guild.give_do")
    GiveBlock.EMPTY, null -> ui("guild.give_do")
}

/** «Сегодня ещё N из M» и ⓘ с правилом предела, под ними - полоса внесённого за сутки; сумма больше остатка - строка красная. */
@Composable private fun DayLimit(rules: GuildRules?, me: GuildMember, over: Boolean) {
    val limit = me.dayLimit
    val given = (limit - me.dayLeft).coerceIn(0L, limit)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            ui(if (me.dayLeft == 0L) "guild.give_today_spent" else "guild.give_today_left", number((if (me.dayLeft == 0L) given else me.dayLeft).toDouble()), number(limit.toDouble())),
            color = if (over) LifeRed else Muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        rules?.let {
            InfoMark(ui("guild.give_limit_about"), Gold) {
                TipText(ui("guild.give_limit_tip", number(it.contribution.dailyPerLevel.toDouble()), loc("guild.branch.$LIMIT_BRANCH")))
                TipText(ui("guild.give_where_tip"), Muted)
            }
        }
    }
    Track(if (limit > 0) given.toFloat() / limit else 0f, if (me.dayLeft == 0L) Muted else Gold)
}

/** Ранг героя, вклад за всё время и ⓘ с порогами рангов; ниже - полоса до следующего ранга и сколько осталось. */
@Composable private fun RankPanel(rules: GuildRules?, me: GuildMember) {
    val next = rules?.nextRank(me.contribution)
    ForgePanel(accent = Rune) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(GuildText.rank(me.rank), color = Rune, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            MutedText(ui("guild.rank_total", number(me.contribution.toDouble())))
            rules?.let { InfoMark(ui("guild.rank_about"), Rune) { TipText(rankSteps(it)) } }
        }
        when {
            rules == null -> Unit

            next != null -> {
                Track(rules.rankProgress(me.contribution, next), Rune)
                MutedText(ui("guild.rank_next", GuildText.rank(next.code), number((next.from - me.contribution).toDouble())))
            }

            else -> MutedText(ui("guild.rank_top"))
        }
    }
}

/** «Ранг растёт от вклада за всё время: Брат - 5 000, …» по порогам правил; начальный ранг с нуля не называется. */
private fun rankSteps(rules: GuildRules): String = ui("guild.rank_tip", rules.ranks.filter { it.from > 0 }.joinToString(", ") { ui("guild.rank_step", GuildText.rank(it.code), number(it.from.toDouble())) })

/**
 * Вкладчики недели, больше всех первым (у первого - корона): таблица гильдии или, где её нет, строки состава. Своя строка - с чертой
 * `Gold` слева, как в составе: это и есть свой вклад за неделю.
 */
@Composable private fun WeekPanel(guild: GuildView, me: GuildMember?) {
    val names = guild.members.associate { it.heroId to it.name }
    val week = guild.weekly.ifEmpty { guild.members.associate { it.heroId to it.weekContribution } }
        .filterValues { it > 0 }.entries.sortedByDescending { it.value }
    ForgePanel {
        Engraved(ui("guild.week_table"))
        if (week.isEmpty()) MutedText(ui("guild.week_empty"))
        Column {
            week.forEachIndexed { place, (heroId, value) ->
                val mine = heroId == me?.heroId
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 36.dp).then(if (mine) Modifier.background(Gold.copy(alpha = .06f)).ownMark() else Modifier).padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        if (place == 0) LEADER_CROWN else "${place + 1}.",
                        color = if (place == 0) Caution else Muted,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.width(24.dp),
                    )
                    Text(
                        names[heroId] ?: "…${heroId.takeLast(6)}",
                        color = if (mine) GoldBright else Parchment,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    GoldPrice(value)
                }
            }
        }
    }
}

/** Корона первого места недели - тот же знак, что у главы (`GuildRoleMark`). */
private const val LEADER_CROWN = "♛"

@Composable private fun TipText(text: String, color: Color = Parchment) {
    Text(text, color = color, style = MaterialTheme.typography.bodySmall)
}

/** Тонкая полоса заполнения [fraction] цвета [color] по бронзовой дорожке. */
@Composable private fun Track(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(4.dp).background(Bronze, RoundedCornerShape(2.dp))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color, RoundedCornerShape(2.dp)))
    }
}
