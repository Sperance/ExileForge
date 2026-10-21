package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.rules.content.GuildRole
import com.sperance.exileforge.rules.content.GuildRules
import com.sperance.exileforge.ui.components.ClassPortrait
import com.sperance.exileforge.ui.components.FateMark
import com.sperance.exileforge.ui.components.GuildRoleMark
import com.sperance.exileforge.ui.components.MightLine
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.PresenceDot
import com.sperance.exileforge.ui.components.SearchBar
import com.sperance.exileforge.ui.components.SortButton
import com.sperance.exileforge.ui.components.SortLabel
import com.sperance.exileforge.ui.components.SortOption
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.components.ring
import com.sperance.exileforge.ui.screens.hero.titleName
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Vital

/**
 * Чем отбирается состав: правила гильдий (порог неактивности) и момент отбора [now] (мс эпохи).
 *
 * Состав гильдии: плотный поиск по имени с кнопкой порядка, чипы отбора со счётчиками, таблица «Участник» / «Неделя / всего» и итог. Строка
 * - две плотные строки ~60dp: портрет класса в кольце роли с точкой присутствия и уровнем, знак роли, имя и титул, «был N назад» и дар; у
 * себя - полоса вклада за сутки.
 */
internal data class RosterScope(val rules: GuildRules, val now: Long)

/** Правила отбора состава [members]: момент берётся заново, когда пришёл новый состав или правила; контента нет - null. */
@Composable internal fun rememberRosterScope(game: GameUi, members: List<GuildMember>): RosterScope? {
    val rules = game.index?.guilds ?: return null
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

/** Поиск по имени над составом - строка поиска ([SearchBar]); справа - кнопка порядка ([RosterSort]) и, если есть, хвост [trailing] в том же ряду. */
@Composable internal fun RosterSearch(
    game: GameUi,
    query: String,
    order: RosterOrder?,
    onQuery: (String) -> Unit,
    onOrder: (RosterOrder?) -> Unit,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    SearchBar(query, onQuery, ui("guild.roster_search"), maxLength = game.inputs.heroName) {
        RosterSort(order, onOrder)
        trailing?.invoke(this)
    }
}

/**
 * Кнопка порядка «⇅» ([SortButton]): меню столбцов ([RosterColumn]) и порядок по ролям; повторный выбор столбца - обратный порядок.
 * Выбранный порядок подсвечивает кнопку.
 */
@Composable private fun RosterSort(order: RosterOrder?, onOrder: (RosterOrder?) -> Unit) {
    val byRole = SortOption(ui("guild.sort_role"), order == null) { onOrder(null) }
    val columns = RosterColumn.entries.map { column ->
        val mark = when {
            order?.column != column -> ""
            order.descending -> " ↓"
            else -> " ↑"
        }
        SortOption(ui(column.title) + mark, order?.column == column) { onOrder(order.next(column)) }
    }
    SortButton(SortLabel.Mark(order != null), listOf(byRole) + columns)
}

/** Плотные чипы отбора: у каждого - сколько участников [members] под ним проходит; выбранный - в нити `Gold`. */
@Composable internal fun RosterFilters(members: List<GuildMember>, chosen: RosterFilter, scope: RosterScope, onChoose: (RosterFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(RosterFilter.entries, key = { it.name }) { filter ->
            val on = filter == chosen
            val shape = RoundedCornerShape(8.dp)
            Text(
                ui("guild.roster_count", filter.label(scope), members.count { filter.keeps(it, scope) }),
                color = if (on) GoldBright else Muted,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                modifier = Modifier.height(28.dp).clip(shape).background(if (on) PanelRaised else Color.Transparent)
                    .border(1.dp, if (on) Gold.copy(alpha = .5f) else Bronze, shape).clickable { onChoose(filter) }
                    .wrapContentHeight(Alignment.CenterVertically).padding(horizontal = 10.dp),
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

/** Заголовок таблицы: «Участник» и «Неделя / всего»; порядок - кнопкой у поиска ([RosterSort]). */
@Composable internal fun RosterHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderCell(ui("guild.col_member"), Modifier.weight(1f), TextAlign.Start)
        HeaderCell(ui("guild.col_week_total"), Modifier, TextAlign.End)
    }
    HorizontalDivider(color = Bronze, thickness = 1.dp)
}

@Composable private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign) {
    Text(
        text.uppercase(),
        color = Muted,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        textAlign = align,
        maxLines = 1,
        modifier = modifier.padding(vertical = 4.dp),
    )
}

/**
 * Строка состава - две плотные строки (~60dp): портрет ([MemberPortrait]) с точкой присутствия; знак роли, имя и титул курсивом;
 * ниже мощь ([MightLine]) и дар ([FateMark]), без «был N назад»; справа вклад за неделю крупно и «Σ всего». Своя строка подсвечена чертой `Gold` слева и
 * несёт полосу вклада за сутки из потолка ([DayContribution]).
 *
 * Мощь - одно число для сравнения, не прогноз боя; не посчитана сервером - «⚜ …».
 */
@Composable internal fun MemberRow(game: GameUi, member: GuildMember, isMe: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).then(if (isMe) Modifier.background(Gold.copy(alpha = .06f)).ownMark() else Modifier)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MemberPortrait(game, member)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    GuildRoleMark(member.role)
                    Text(
                        if (isMe) ui("guild.me", member.name) else member.name,
                        color = GoldBright,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    member.title.takeIf { it.isNotBlank() }?.let {
                        Text("· " + titleName(it), color = Muted, fontSize = 11.sp, fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MightLine(member.might)
                    member.fate?.let { FateMark(it, Modifier.weight(1f, fill = false)) }
                }
                if (isMe && member.dayLimit > 0) DayContribution(member)
            }
            Column(Modifier.width(76.dp), horizontalAlignment = Alignment.End) {
                Text(number(member.weekContribution.toDouble()), color = if (member.weekContribution == 0L) Muted else GoldBright, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("Σ " + number(member.contribution.toDouble()), color = Muted, fontSize = 11.sp, maxLines = 1)
            }
        }
        HorizontalDivider(color = Bronze.copy(alpha = .5f), thickness = 1.dp)
    }
}

/** Черта своей строки - 2dp `Gold` по левому краю. */
internal fun Modifier.ownMark(): Modifier = drawBehind { drawRect(Gold, size = Size(2.dp.toPx(), size.height)) }

/** Портрет класса в кольце цвета роли; в углу - точка присутствия в ободке фона, в другом - уровень плашкой. */
@Composable private fun MemberPortrait(game: GameUi, member: GuildMember) {
    Box(Modifier.size(40.dp)) {
        ClassPortrait(member.heroClass.takeIf { it.isNotBlank() }, game.world.portraits, Modifier.size(36.dp).align(Alignment.Center), round = true, ring = member.role.ring)
        PresenceDot(member.online, member.lastSeenAt, 11.dp, Modifier.align(Alignment.BottomEnd).border(2.dp, Ink, CircleShape))
        Text(
            member.level.toString(),
            color = Parchment,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopStart).background(PanelRaised, LevelShape).border(1.dp, Bronze, LevelShape).padding(horizontal = 3.dp),
        )
    }
}

private val LevelShape = RoundedCornerShape(6.dp)

/** Свой вклад за сутки: тонкая полоса из суточного потолка и «сегодня X/N». */
@Composable private fun DayContribution(member: GuildMember) {
    val done = (member.dayLimit - member.dayLeft).coerceIn(0L, member.dayLimit)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.weight(1f).height(3.dp).background(Bronze, RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(done.toFloat() / member.dayLimit).fillMaxHeight().background(Gold, RoundedCornerShape(2.dp)))
        }
        Text(ui("guild.day_limit", number(done.toDouble()), number(member.dayLimit.toDouble())), color = Vital, fontSize = 10.sp, maxLines = 1)
    }
}

/** Итог состава: «В сети X из N · Неделя: Σ». */
@Composable internal fun RosterFooter(members: List<GuildMember>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp)) {
        MutedText(ui("guild.roster_online", members.count { it.online }, members.size), Modifier.weight(1f))
        MutedText(ui("guild.roster_week", number(members.sumOf { it.weekContribution }.toDouble())))
    }
}
