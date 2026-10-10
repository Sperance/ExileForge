package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildMemberCraft
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.GuildRole
import com.sperance.exileforge.rules.content.GuildRules
import com.sperance.exileforge.ui.components.ClassPortrait
import com.sperance.exileforge.ui.components.FateMark
import com.sperance.exileforge.ui.components.GuildRoleMark
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.PresenceDot
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.components.leagueLevel
import com.sperance.exileforge.ui.components.ring
import com.sperance.exileforge.ui.components.seenAgoText
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.hero.titleName
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.ModeHue
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment

/*
 * Состав гильдии (4.6.3, утверждён макет «Плотная таблица», вариант A): поиск по имени, чипы отбора со счётчиками, таблица
 * с сортируемыми «Ур.», «Неделя», «Всего» и итог. Строка - портрет класса в кольце роли с точкой присутствия, титул над
 * именем, знак роли перед ним, под именем значки: лига Разлома, дар, профессия.
 */

/** Чем отбирается состав: правила гильдий (порог неактивности) и момент отбора [now] (мс эпохи). */
internal data class RosterScope(val rules: GuildRules, val now: Long)

/** Правила отбора состава [members]: момент берётся заново, когда пришёл новый состав или правила. */
@Composable internal fun rememberRosterScope(game: GameUi, members: List<GuildMember>): RosterScope {
    val rules = game.index?.guilds ?: GuildRules()
    return remember(rules, members) { RosterScope(rules, System.currentTimeMillis()) }
}

/**
 * Чип отбора состава: подпись [key] (`{0}` - порог неактивности `guilds.json` → `inactiveDays`) и кто проходит [keeps].
 * Новый отбор - новая запись, экран его не знает.
 */
internal enum class RosterFilter(private val key: String, val keeps: (GuildMember, RosterScope) -> Boolean) {
    ALL("guild.filter.ALL", { _, _ -> true }),
    ONLINE("guild.filter.ONLINE", { member, _ -> member.online }),

    /** Глава и офицеры. */
    STAFF("guild.filter.STAFF", { member, _ -> member.role != GuildRole.MEMBER }),

    /** Не в сети и не заходил `inactiveDays` суток и дольше ([GuildRules.inactive]). */
    INACTIVE("guild.filter.INACTIVE", { member, scope -> !member.online && scope.rules.inactive(member.lastSeenAt, scope.now) }),

    /** Ничего не внёс за эту неделю. */
    IDLE("guild.filter.IDLE", { member, _ -> member.weekContribution == 0L }),
    ;

    fun label(scope: RosterScope): String = ui(key, scope.rules.inactiveDays)
}

/** Подходит ли участник под поиск [query] - часть имени без учёта регистра; пустой поиск пропускает всех. */
internal fun GuildMember.named(query: String): Boolean = name.contains(query.trim(), ignoreCase = true)

/** Поиск по имени над составом. */
@Composable internal fun RosterSearch(game: GameUi, query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        query,
        { onQuery(it.take(game.inputs.heroName)) },
        placeholder = { Text(ui("guild.roster_search")) },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Чипы отбора: у каждого - сколько участников [members] под ним проходит. */
@Composable internal fun RosterFilters(members: List<GuildMember>, chosen: RosterFilter, scope: RosterScope, onChoose: (RosterFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(RosterFilter.entries, key = { it.name }) { filter ->
            FilterChip(
                selected = filter == chosen,
                onClick = { onChoose(filter) },
                label = { Text(ui("guild.roster_count", filter.label(scope), members.count { filter.keeps(it, scope) })) },
            )
        }
    }
}

/** Столбец состава, по которому его можно упорядочить. */
internal enum class RosterColumn(val title: String, val value: (GuildMember) -> Long) {
    LEVEL("guild.col_level", { it.level.toLong() }),
    WEEK("guild.col_week", { it.weekContribution }),
    TOTAL("guild.col_total", { it.contribution }),
}

/** Порядок состава: столбец и направление; равные - по имени. */
internal data class RosterOrder(val column: RosterColumn, val descending: Boolean = true) {
    fun sort(members: List<GuildMember>): List<GuildMember> {
        val by = compareBy<GuildMember> { column.value(it) }.let { if (descending) it.reversed() else it }
        return members.sortedWith(by.thenBy { it.name.lowercase() })
    }

    companion object {
        /** Без выбора: глава, офицеры, участники, внутри - по вкладу за всё время. */
        fun byRole(members: List<GuildMember>): List<GuildMember> = members.sortedWith(compareBy<GuildMember> { it.role.ordinal }.thenByDescending { it.contribution })
    }
}

/** Нажатие на заголовок [column]: новый столбец - по убыванию, тот же - обратный порядок. */
internal fun RosterOrder?.next(column: RosterColumn): RosterOrder = if (this?.column == column) copy(descending = !descending) else RosterOrder(column)

@Composable internal fun RosterHeader(order: RosterOrder?, onSort: (RosterColumn) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderCell(ui("guild.col_member"), Modifier.weight(1f), TextAlign.Start)
        RosterColumn.entries.forEach { column ->
            val mark = when {
                order?.column != column -> ""
                order.descending -> " ↓"
                else -> " ↑"
            }
            HeaderCell(ui(column.title) + mark, Modifier.width(column.width).clickable { onSort(column) }, TextAlign.End, active = order?.column == column)
        }
    }
    HorizontalDivider(color = Bronze, thickness = 1.dp)
}

@Composable private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign, active: Boolean = false) {
    Text(
        text.uppercase(),
        color = if (active) GoldBright else Muted,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        textAlign = align,
        maxLines = 1,
        modifier = modifier.padding(vertical = 4.dp),
    )
}

private val RosterColumn.width: Dp get() = if (this == RosterColumn.LEVEL) 36.dp else 64.dp

/**
 * Строка состава: портрет класса в кольце роли с точкой присутствия, титул над именем, знак роли перед именем, под ним значки
 * ([MemberMarks]); справа уровень, вклад за неделю и за всё время. Своя строка подсвечена.
 */
@Composable internal fun MemberRow(game: GameUi, member: GuildMember, isMe: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(if (isMe) Gold.copy(alpha = .06f) else Color.Transparent).clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            MemberPortrait(game, member)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                member.title.takeIf { it.isNotBlank() }?.let {
                    Text(titleName(it).uppercase(), color = Caution, style = MaterialTheme.typography.labelSmall, letterSpacing = .8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GuildRoleMark(member.role)
                    Text(
                        if (isMe) ui("guild.me", member.name) else member.name,
                        color = GoldBright,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                MemberMarks(game, member, isMe)
            }
            NumberCell(member.level.toString(), RosterColumn.LEVEL)
            NumberCell(number(member.weekContribution.toDouble()), RosterColumn.WEEK, dim = member.weekContribution == 0L)
            NumberCell(number(member.contribution.toDouble()), RosterColumn.TOTAL)
        }
        HorizontalDivider(color = Bronze.copy(alpha = .5f), thickness = 1.dp)
    }
}

/** Портрет класса в кольце цвета роли; в углу - точка присутствия в ободке фона. */
@Composable private fun MemberPortrait(game: GameUi, member: GuildMember) {
    Box(Modifier.size(36.dp)) {
        ClassPortrait(member.heroClass.takeIf { it.isNotBlank() }, game.world.portraits, Modifier.size(36.dp), round = true, ring = member.role.ring)
        PresenceDot(member.online, member.lastSeenAt, 11.dp, Modifier.align(Alignment.BottomEnd).border(2.dp, Ink, CircleShape))
    }
}

/**
 * Значки под именем: лига Разлома, метка дара, профессия; у себя - сколько вклада внесено из суточного потолка, у прочих не в
 * сети - «был N назад». Нет данных - нет и значка.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberMarks(game: GameUi, member: GuildMember, isMe: Boolean) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        leagueLevel(game, member.league)?.let { LeagueTag(it) }
        member.fate?.let { FateMark(it) }
        member.craft?.let { CraftTag(it) }
        presenceTail(member, isMe)?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
    }
}

/** Плашка лиги Разлома «Р 70+» - зелёным Разлома. */
@Composable private fun LeagueTag(level: Int) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        ui("guild.league_short", level),
        color = ModeHue.Rift,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        modifier = Modifier.border(1.dp, ModeHue.Rift.copy(alpha = .35f), shape).padding(horizontal = 4.dp),
    )
}

/** Профессия с уровнем: молот, имя и уровень ярче; идёт работа - цветом профессий и «сейчас». */
@Composable private fun CraftTag(craft: GuildMemberCraft) {
    val tint = if (craft.working) ModeHue.Professions else Muted
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(ForgeGlyphs.Anvil, null, tint = tint, modifier = Modifier.size(12.dp))
        Text(
            buildAnnotatedString {
                append(professionTitle(craft.profession))
                append(' ')
                withStyle(SpanStyle(color = if (craft.working) ModeHue.Professions else Parchment, fontWeight = FontWeight.Bold)) { append(craft.level.toString()) }
                if (craft.working) {
                    append(" · ")
                    append(ui("guild.craft_now"))
                }
            },
            color = tint,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

/** Хвост строки: у себя - вклад за сутки из потолка, у прочих не в сети - «был N назад», в сети - ничего. */
private fun presenceTail(member: GuildMember, isMe: Boolean): String? = when {
    isMe && member.dayLimit > 0 -> ui("guild.day_limit", number((member.dayLimit - member.dayLeft).coerceAtLeast(0).toDouble()), number(member.dayLimit.toDouble()))
    member.online -> null
    else -> seenAgoText(false, member.lastSeenAt)
}

@Composable private fun NumberCell(text: String, column: RosterColumn, dim: Boolean = false) {
    Text(text, color = if (dim) Muted else Parchment, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(column.width))
}

/** Итог состава: «В сети X из N · Неделя: Σ». */
@Composable internal fun RosterFooter(members: List<GuildMember>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp)) {
        MutedText(ui("guild.roster_online", members.count { it.online }, members.size), Modifier.weight(1f))
        MutedText(ui("guild.roster_week", number(members.sumOf { it.weekContribution }.toDouble())))
    }
}
