package com.sperance.exileforge.ui.screens.hall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.hall.HallPick
import com.sperance.exileforge.presentation.hall.HallViewModel
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HallBoard
import com.sperance.exileforge.rules.content.HallEntry
import com.sperance.exileforge.rules.content.HallScope
import com.sperance.exileforge.rules.content.HallTable
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.clock
import com.sperance.exileforge.ui.screens.expedition.roman
import com.sperance.exileforge.ui.screens.guild.GuildEmblem
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/*
 * Доска славы Города (4.2.0, макет «Реликвии»): вкладки - таблицы реестра [HallBoard], под ними выбор раздела (лига, регион
 * и ступень раша, профессия). Первые три места - плиты редкостей (мифическая, уникальная, редкая), дальше список; строка
 * героя (его гильдии) закреплена внизу.
 */

/** Облик призовых мест: первое - мифическая плита, второе - уникальная, третье - редкая. */
private val PODIUM = listOf(Rarity.MYTHICAL, Rarity.UNIQUE, Rarity.RARE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HallScreen() {
    val vm = koinViewModel<HallViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val hall by vm.hall.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    val index = game.index
    LaunchedEffect(game.heroId, game.sessionEpoch, index != null) {
        if (game.heroId.isBlank() || index == null) return@LaunchedEffect
        hall.pick?.let { vm.load() } ?: vm.pick(HallPick(HallBoard.entries.first(), HallBoard.entries.first().defaultScope(index)))
    }
    val pick = hall.pick
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            CollapsibleHeader { ScreenHeader(ui("hall.title"), ui("hall.subtitle"), ForgeGlyphs.Gem) }
        }
        if (index == null || pick == null) {
            MutedText(ui("common.loading"), Modifier.padding(16.dp))
            return
        }
        ScrollableTabRow(selectedTabIndex = pick.board.ordinal, containerColor = Abyss, contentColor = Gold, edgePadding = 12.dp) {
            HallBoard.entries.forEach { board ->
                Tab(
                    selected = board == pick.board,
                    onClick = { if (board != pick.board) vm.pick(HallPick(board, board.defaultScope(index))) },
                    text = { Text(boardTitle(board), maxLines = 1) },
                )
            }
        }
        ScopePicker(index, pick, hall.table, vm::pick)
        PullToRefreshBox(isRefreshing = Reads.HALL in activity.loading, onRefresh = vm::load, modifier = Modifier.weight(1f)) {
            val table = hall.table
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    table == null -> item { MutedText(ui("common.loading")) }

                    table.top.isEmpty() -> item { MutedText(ui("hall.empty")) }

                    else -> {
                        item { Podium(table) }
                        items(table.top.drop(PODIUM.size).withIndex().toList(), key = { it.value.id }) { (i, entry) ->
                            EntryRow(table, entry, PODIUM.size + i + 1L, own = entry.id == table.own?.id)
                        }
                    }
                }
            }
        }
        hall.table?.let { OwnRow(it) }
    }
}

/** Раздел таблицы по умолчанию: раш - первый регион с низшей ступенью, профессия - первая; без разделов - пусто. */
private fun HallBoard.defaultScope(index: ContentIndex): String = when (scope) {
    HallScope.NONE -> ""
    HallScope.RUSH -> index.campaign.regions.firstOrNull()?.let { rushScope(it.code, 0) }.orEmpty()
    HallScope.PROFESSION -> index.professions.professions.firstOrNull()?.code.orEmpty()
}

private fun rushScope(region: String, tier: Int) = "$region:$tier"

/** Название таблицы реестра на вкладке. */
private fun boardTitle(board: HallBoard): String = when (board) {
    HallBoard.RIFT -> ui("hall.board.RIFT")
    HallBoard.TOWER -> ui("hall.board.TOWER")
    HallBoard.RUSH -> ui("hall.board.RUSH")
    HallBoard.GUILD -> ui("hall.board.GUILD")
    HallBoard.PROFESSION -> ui("hall.board.PROFESSION")
}

/** Чем меряется место в таблице: счёт, этаж, время раша, опыт гильдии или профессии. */
private fun HallBoard.measure(value: Long): String = when (this) {
    HallBoard.RIFT -> ui("hall.value.RIFT", number(value.toDouble()))
    HallBoard.TOWER -> ui("hall.value.TOWER", value)
    HallBoard.RUSH -> clock(value.toDouble())
    HallBoard.GUILD, HallBoard.PROFESSION -> ui("hall.value.experience", number(value.toDouble()))
}

/** Кто стоит на месте: герой - класс и уровень, гильдия - знак и уровень. */
private fun about(entry: HallEntry): String = listOfNotNull(
    entry.tag.takeIf { it.isNotBlank() }?.let { "[$it]" },
    entry.heroClass.takeIf { it.isNotBlank() }?.let(::classTitle),
    ui("hall.level", entry.level),
).joinToString(" · ")

/** Фишка выбора раздела: подпись, выбрана ли, что делает нажатие. */
private class ScopeChip(val label: String, val on: Boolean, val onClick: () -> Unit)

/**
 * Выбор раздела под вкладками - ряд фишек на каждое деление таблицы: лига у таблиц с лигами ([HallBoard.leagues]), регион и
 * ступень у раша, профессия у профессий. Лига по умолчанию - та, что назвал ответ (лига героя).
 */
@Composable private fun ScopePicker(index: ContentIndex, pick: HallPick, table: HallTable?, onPick: (HallPick) -> Unit) {
    val trials = index.campaign.trials
    val shown = pick.league ?: table?.league
    val leagues = trials?.rift?.leagues.orEmpty().takeIf { pick.board.leagues }.orEmpty()
        .mapIndexed { i, level -> ScopeChip(ui("rift.league", level), shown == i) { onPick(pick.copy(league = i)) } }
    val scopes: List<List<ScopeChip>> = when (pick.board.scope) {
        HallScope.NONE -> emptyList()

        HallScope.PROFESSION -> listOf(index.professions.professions.map { ScopeChip(professionTitle(it.code), it.code == pick.scope) { onPick(pick.copy(scope = it.code)) } })

        HallScope.RUSH -> {
            val region = pick.scope.substringBefore(':')
            val tier = pick.scope.substringAfter(':', "").toIntOrNull() ?: 0
            listOf(
                index.campaign.regions.map { ScopeChip(regionTitle(it.code), it.code == region) { onPick(pick.copy(scope = rushScope(it.code, tier))) } },
                (0 until (trials?.rush?.tierCount ?: 1)).map { t -> ScopeChip(ui("hall.tier", roman(t + 1)), t == tier) { onPick(pick.copy(scope = rushScope(region, t))) } },
            )
        }
    }
    (listOf(leagues) + scopes).filter { it.isNotEmpty() }.forEach { row ->
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { chip -> FilterChip(selected = chip.on, onClick = chip.onClick, label = { Text(chip.label, maxLines = 1) }) }
        }
    }
}

/** Три призовых места: первое - мифическая плита во всю ширину, второе и третье - уникальная и редкая рядом. */
@Composable private fun Podium(table: HallTable) {
    val top = table.top.take(PODIUM.size)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        top.firstOrNull()?.let { Plate(table, it, 0, Modifier.fillMaxWidth(), big = true) }
        if (top.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                top.drop(1).forEachIndexed { i, entry -> Plate(table, entry, i + 1, Modifier.weight(1f), big = false) }
                if (top.size == 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Плита призового места [place] (с нуля): подложка и цвета реликвии своей редкости, своё место отмечено золотой рамкой. */
@Composable private fun Plate(table: HallTable, entry: HallEntry, place: Int, modifier: Modifier, big: Boolean) {
    val look = relicLook(PODIUM[place])
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.relicGround(look, shape, selected = entry.id == table.own?.id).padding(horizontal = 14.dp, vertical = if (big) 18.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(roman(place + 1), style = look.nameStyle(if (big) 30 else 22))
        if (entry.emblem.isNotBlank()) GuildEmblem(entry.emblem, entry.color, if (big) 40.dp else 30.dp)
        Text(entry.name, style = look.nameStyle(if (big) 20 else 15), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(about(entry), color = look.muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(table.board.measure(entry.value), color = look.gold, style = if (big) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall)
    }
}

/** Строка списка после призовых мест; [own] - строка героя, выделена. */
@Composable private fun EntryRow(table: HallTable, entry: HallEntry, place: Long, own: Boolean) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().depthPanel(shape).then(if (own) Modifier.border(1.dp, Vital.copy(alpha = .6f), shape) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(place.toString(), color = if (own) Vital else Muted, style = MaterialTheme.typography.titleSmall, modifier = Modifier.widthIn(min = 28.dp))
        if (entry.emblem.isNotBlank()) GuildEmblem(entry.emblem, entry.color, 26.dp)
        Column(Modifier.weight(1f)) {
            Text(entry.name, color = if (own) Vital else Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(about(entry), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(table.board.measure(entry.value), color = GoldBright, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Строка героя (его гильдии в таблице гильдий), закреплённая под списком: место из скольких или что строки нет. */
@Composable private fun OwnRow(table: HallTable) {
    val own = table.own
    Column(Modifier.fillMaxWidth().background(Abyss).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val guild = table.board == HallBoard.GUILD
        if (own == null || table.place <= 0) {
            MutedText(ui(if (guild) "hall.own_none_guild" else "hall.own_none"))
            return@Column
        }
        MutedText(ui(if (guild) "hall.own_guild" else "hall.own", table.place, table.size), style = MaterialTheme.typography.labelMedium)
        EntryRow(table, own, table.place, own = true)
    }
}
