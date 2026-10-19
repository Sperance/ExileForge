package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.PoolReserve
import com.sperance.exileforge.core.campaign.stat
import com.sperance.exileforge.core.character.StatPolarity
import com.sperance.exileforge.core.display.StatLimit
import com.sperance.exileforge.core.display.StatLimits
import com.sperance.exileforge.core.display.contentKeys
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.statValue
import com.sperance.exileforge.core.display.title
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.core.sheet.ConditionalStat
import com.sperance.exileforge.core.sheet.ConditionalStats
import com.sperance.exileforge.core.sheet.RowLink
import com.sperance.exileforge.core.sheet.ShareCard
import com.sperance.exileforge.core.sheet.ShareKind
import com.sperance.exileforge.core.sheet.ShareRow
import com.sperance.exileforge.core.sheet.StatDetail
import com.sperance.exileforge.core.sheet.StatExplainer
import com.sperance.exileforge.core.sheet.StatExplanation
import com.sperance.exileforge.core.sheet.reserve
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.ReservePool
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.rules.sheet.StatReading
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.rules.text.numbered
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonSize
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.*

internal fun ShareKind.color(): Color = when (this) {
    ShareKind.BASE -> Muted
    ShareKind.ATTRIBUTE -> Gold
    ShareKind.CLASS -> GoldBright
    ShareKind.NODE -> Rune
    ShareKind.ITEM -> Color(0xFFFFFF77)
    ShareKind.LAW -> GoldBright
    ShareKind.SKILL -> Rune
    ShareKind.FATE -> Caution
    ShareKind.OTHER -> Parchment
    ShareKind.RESHAPE -> Ember
    ShareKind.AFTER -> Handcrafted
    ShareKind.FIGHT -> ShieldCyan
    ShareKind.LOW_LIFE -> Muted
    ShareKind.APART -> Muted
}

/**
 * A figure's own window: its total, then what it is made of — a bar of each kind's share,
 * a card per kind with every source by name, the formula, and for a stat others take from, what it
 * gives them now. A linked stat opens in the same window (the chip on top goes back), a linked item - its card.
 *
 * [shifts] are what lies over the hero's own sheet — a map's and the atlas's effects — by stat. Цифра - лист боя ([CombatSheet]):
 * пассивные умения и шаги Предначертания - доли итога, полосы и формулы.
 *
 * Ключи карточек - по виду и заголовку: переход по цифрам не перекомпонует совпавшие карточки. Пределы и защита процентом - по
 * листу [stats], с которого нарисована сводка (во вкладке «Под картой» - с картой). Под цитатой - свёртка «Подробнее» ([DetailFold]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatBreakdownSheet(game: GameUi, stat: String, stats: Map<String, Double>, shifts: Map<String, List<Shift>>, onDismiss: () -> Unit) {
    val combat = game.hero?.combat ?: run {
        LaunchedEffect(stat) { onDismiss() }
        return
    }
    var trail by remember(stat) { mutableStateOf(listOf(stat)) }
    val current = trail.last()
    val view = remember(current, combat, shifts, game.lang) {
        StatExplainer(game).explain(combat.explain(current, shifts), combat.grants(current), combat::holders, combat.lowLife(current), combat.lootApart(current))
    }
    val cardKeys = remember(view) { contentKeys(view.cards) { "${it.kind}|${it.title}" } }
    val inspect = rememberInspect()
    val follow: (RowLink) -> Unit = { link ->
        when (link) {
            is RowLink.Stat -> trail = trail + link.code
            is RowLink.Item -> game.hero?.item(link.id)?.let { inspect(Inspect.Copy(it)) }
        }
    }
    val accent = game.index?.stats?.section(current)?.accent() ?: Muted
    val detail = remember(current, game.index, game.lang) { StatDetail.paragraphs(current, game.index, game.lang) }
    var folded by rememberSaveable(stat) { mutableStateOf(true) }
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().fillMaxHeight(.9f).navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (trail.size > 1) {
                item {
                    ForgeButton(onClick = { trail = trail.dropLast(1) }, size = ButtonSize.Small, kind = ButtonKind.Secondary, icon = Icons.AutoMirrored.Outlined.ArrowBack) {
                        Text(statTitle(trail[trail.size - 2], game.lang), maxLines = 1)
                    }
                }
            }
            val defence = game.index?.let { index -> game.hero?.level?.let { StatLimits.defence(current, stats, it, index) } }
            item { Header(current, view, defence, stats, accent, game) }
            if (detail.isNotEmpty()) item { DetailFold(detail, folded) { folded = !folded } }
            defence?.let { reading -> item { DefenceCard(current, reading, stats, game) } }
            game.index?.campaign?.let { StatLimits.of(current, stats, it) }?.let { limit -> item { LimitCard(limit, game, follow) } }
            ReservePool.entries.firstOrNull { it.stat == current }?.let(game::reserve)?.takeIf { it.percent > 0 }?.let { reserve -> item { ReserveCard(reserve, game) } }
            if (view.weights.values.sum() > 0) item { ShareBar(view.weights) }
            items(view.cards.size, key = { cardKeys[it] }) { i -> SourceCard(view.cards[i], game, follow) }
            if (view.cards.isEmpty()) item { Text(ui("stat.empty"), color = Muted, style = MaterialTheme.typography.bodySmall) }
            if (view.cards.isNotEmpty()) item { Formula(view.formula) }
            if (view.grants.isNotEmpty()) item { Grants(current, view.grants, game, follow) }
        }
    }
}

/**
 * Разбор строки «С условием» ([ConditionalStat]): имя с условием, вклад, цитата характеристики, карточка «Условие» - когда оно
 * держится (числа правил подстановкой) и что строка действует только в бою, - затем полоса и карточки источников. Формулы нет:
 * в итог своей характеристики строка не входит. Касание вещи-источника - её карточка.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionalBreakdownSheet(game: GameUi, row: ConditionalStat, onDismiss: () -> Unit) {
    val view = remember(row, game.hero, game.lang) { StatExplainer(game).conditional(row) }
    val inspect = rememberInspect()
    val follow: (RowLink) -> Unit = { link -> if (link is RowLink.Item) game.hero?.item(link.id)?.let { inspect(Inspect.Copy(it)) } }
    val section = game.index?.stats?.let { ConditionalStats.section(row, it) }
    val accent = section?.accent() ?: ConditionalAccent
    val quote = uiOr(game.lang, "stat.desc.${row.stat}", "")
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().fillMaxHeight(.9f).navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val shape = RoundedCornerShape(10.dp)
                    Box(Modifier.size(40.dp).clip(shape).background(accent.copy(alpha = .13f)).border(1.dp, accent.copy(alpha = .4f), shape), contentAlignment = Alignment.Center) {
                        StatIcon(row.stat, accent, Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(ui("stat.conditional.group").uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall)
                        Text(conditionalTitle(row, game), color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Text(view.total, color = accent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (quote.isNotBlank()) item { Text("«$quote»", color = Muted, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic) }
            item { ConditionCard(row.condition, game) }
            if (view.weights.values.sum() > 0) item { ShareBar(view.weights) }
            items(view.cards.size, key = { "${view.cards[it].kind}|${view.cards[it].title}" }) { i -> SourceCard(view.cards[i], game, follow) }
        }
    }
}

/** Карточка «Условие: …»: когда условие держится - числа правил подстановкой - и что строка действует только в бою. */
@Composable private fun ConditionCard(condition: Condition, game: GameUi) {
    val shape = RoundedCornerShape(12.dp)
    val about = remember(condition, game.index, game.lang) { game.index.numbered(ui(game.lang, "stat.condition.${condition.name}")) }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(PanelRaised).border(1.dp, ConditionalAccent.copy(alpha = .55f), shape).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.AutoMirrored.Outlined.CallSplit, null, tint = ConditionalAccent, modifier = Modifier.size(16.dp))
            Text(ui("stat.condition.title", loc(LocaleKey.condition(condition))), color = Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        about?.let { Text(it, color = Parchment, style = MaterialTheme.typography.bodySmall) }
        Text(ui(if (condition.target) "stat.condition.fight_target" else "stat.condition.fight"), color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

/** Шапка разбора: имя, раздел и итог; у уклонения и брони итог - процент защиты [defence], рейтинг - мелко под именем. */
@Composable private fun Header(stat: String, view: StatExplanation, defence: StatReading?, stats: Map<String, Double>, accent: Color, game: GameUi) {
    val section = game.index?.stats?.section(stat)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val shape = RoundedCornerShape(10.dp)
            Box(Modifier.size(40.dp).clip(shape).background(accent.copy(alpha = .13f)).border(1.dp, accent.copy(alpha = .4f), shape), contentAlignment = Alignment.Center) {
                StatIcon(stat, accent, Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                section?.let { Text(it.title(game.lang).uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall) }
                Text(statTitle(stat, game.lang), color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (defence != null) Text(ui("stat.defence.rating", statValue(stat, stats[stat] ?: 0.0, game.index)), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Text(defence?.let { ui("stat.defence_percent", number(it.effective)) } ?: view.total, color = accent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        if (section != null) {
            Text(
                "«${ui(game.lang, "stat.desc.$stat")}»",
                color = Muted,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

/**
 * Предел характеристики: базовый лимит правил, каждая прибавка от характеристики, что её даёт (касание открывает её разбор),
 * абсолютный потолок и дно; упёршаяся в предел - строкой «предел; набрано N». Действующего лимита и числа боя отдельной строкой
 * нет: итог уже в шапке разбора.
 */
@Composable private fun LimitCard(limit: StatLimit, game: GameUi, open: (RowLink) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val value = { v: Double -> statValue(limit.stat, v, game.index) }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(PanelRaised).border(1.dp, Bronze, shape).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SourceLine(ShareRow(ui("stat.cap.base"), value(limit.base)), open)
        limit.raises.forEach { (stat, amount) -> SourceLine(ShareRow(statTitle(stat, game.lang), (if (amount >= 0) "+" else "−") + value(kotlin.math.abs(amount)), link = RowLink.Stat(stat), better = StatPolarity.better(game.index, limit.stat, amount)), open) }
        limit.hard?.let { SourceLine(ShareRow(ui("stat.cap.hard"), value(it)), open) }
        limit.floor?.let { SourceLine(ShareRow(ui("stat.cap.floor"), value(it)), open) }
        if (limit.clamped) Text(ui("stat.cap.reached", value(limit.gathered)), color = Caution, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Защита процентом: рейтинг [stat], что он даёт против эталонного врага уровня героя (шанс уклонения или снижение физического
 * урона - [reading], одна функция со сводкой) и предел; набранное сверх предела - мелко.
 */
@Composable private fun DefenceCard(stat: String, reading: StatReading, stats: Map<String, Double>, game: GameUi) {
    val shape = RoundedCornerShape(12.dp)
    val percent = { v: Double -> ui("stat.defence_percent", number(v)) }
    val kind = if (stat == StatLimits.EVASION) "evasion" else "armour"
    Column(
        Modifier.fillMaxWidth().clip(shape).background(PanelRaised).border(1.dp, Bronze, shape).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(ui("stat.defence.title"), color = Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        SourceLine(ShareRow(statTitle(stat, game.lang), statValue(stat, stats[stat] ?: 0.0, game.index))) {}
        SourceLine(ShareRow(ui("stat.defence_title.$kind"), percent(reading.effective))) {}
        SourceLine(ShareRow(ui("stat.cap.title"), percent(reading.cap))) {}
        Text(
            ui("stat.defence.basis.$kind", game.hero?.level ?: 0) + if (reading.value > reading.cap) " · " + ui("stat.cap.gathered", percent(reading.value)) else "",
            color = Muted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/** What the passive auras hold of a pool (mana or life): their share and amount, and the pool left free. */
@Composable private fun ReserveCard(reserve: PoolReserve, game: GameUi) {
    val shape = RoundedCornerShape(12.dp)
    val value = { v: Double -> statValue(reserve.pool.stat, v, game.index) }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(PanelRaised).border(1.dp, reserve.pool.tint().copy(alpha = .45f), shape).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(ui("stat.reserve.title"), color = Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        SourceLine(ShareRow(ui("stat.reserve.held", number(reserve.percent)), "−" + value(reserve.held), better = false)) {}
        SourceLine(ShareRow(ui("stat.reserve.free.${reserve.pool}"), value(reserve.free))) {}
    }
}

/** Each kind's share of the total as one bar and its legend, in percent of the positive part. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareBar(weights: Map<ShareKind, Double>) {
    val sum = weights.values.sum()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val shape = RoundedCornerShape(7.dp)
        Row(Modifier.fillMaxWidth().height(14.dp).clip(shape).border(1.dp, Bronze, shape)) {
            weights.forEach { (kind, value) -> Box(Modifier.weight((value / sum).toFloat().coerceAtLeast(.001f)).fillMaxHeight().background(kind.color())) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            weights.forEach { (kind, value) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(kind.color()))
                    Text("${ui("stat.kind.${kind.name}")} ${Math.round(value / sum * 100)}%", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable private fun KindTag(kind: ShareKind) {
    val color = kind.color()
    Text(
        ui("stat.tag.${kind.name}"),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = .16f)).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** A kind's card: its tag, its name and what it gives together, then a line per source, a grey note under one that has it. */
@Composable private fun SourceCard(card: ShareCard, game: GameUi, open: (RowLink) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(PanelRaised).border(1.dp, Bronze, shape).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KindTag(card.kind)
            Text(card.title ?: ui("stat.kind.${card.kind.name}"), color = Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(card.summary, color = shiftColor(card.better, Gold), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        card.rows.forEach { SourceLine(it, open) }
    }
}

/** Строка источника: со ссылкой ([ShareRow.link] - характеристика или вещь) - подчёркнута и нажимается. */
@Composable private fun SourceLine(row: ShareRow, open: (RowLink) -> Unit) {
    Column(Modifier.fillMaxWidth().then(row.link?.let { link -> Modifier.clickable { open(link) } } ?: Modifier).padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.title,
                color = if (row.link != null) ModBlue else Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                textDecoration = if (row.link != null) TextDecoration.Underline else null,
            )
            Text(row.value, color = shiftColor(row.better, Parchment), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
        row.note?.let { Text(it, color = Muted.copy(alpha = .8f), style = MaterialTheme.typography.labelSmall) }
    }
}

/** Цвет числа строки разбора: к худшему по полярности - кровью, иначе - [plain]; знак числа цвета не решает. */
private fun shiftColor(better: Boolean?, plain: Color): Color = if (better == false) LifeRed else plain

@Composable private fun Formula(text: String) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        text,
        color = Parchment,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(shape).background(Ink).border(1.dp, Bronze, shape).padding(10.dp),
    )
}

/** What the stat gives others now; a tap opens the one given to. */
@Composable private fun Grants(stat: String, rows: List<ShareRow>, game: GameUi, open: (RowLink) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(ui("stat.gives", statTitle(stat, game.lang)).uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall)
        rows.forEach { row ->
            val shape = RoundedCornerShape(8.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(PanelRaised).clickable { row.link?.let(open) }.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.title, color = ModBlue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text(row.value, color = shiftColor(row.better, Gold), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("  ›", color = Muted)
            }
        }
    }
}

/** Свёртка «Подробнее» под цитатой разбора: «Что даёт» - абзацы подробного описания ([StatDetail]). */
@Composable private fun DetailFold(detail: List<String>, folded: Boolean, onToggle: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onToggle).padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(if (folded) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess, null, tint = ModBlue, modifier = Modifier.size(18.dp))
            Text(ui("stat.fold.more"), color = ModBlue, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        if (folded) return@Column
        val shape = RoundedCornerShape(12.dp)
        Column(
            Modifier.fillMaxWidth().clip(shape).background(PanelRaised).border(1.dp, Bronze, shape).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FoldTitle(ui("stat.fold.what"))
            detail.forEach { paragraph ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("•", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text(paragraph, color = Parchment, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable private fun FoldTitle(text: String) {
    Text(text.uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
}
