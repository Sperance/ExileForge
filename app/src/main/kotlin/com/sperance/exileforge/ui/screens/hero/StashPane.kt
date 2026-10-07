package com.sperance.exileforge.ui.screens.hero

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.character.GearVerdict
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.StashFilter
import com.sperance.exileforge.presentation.state.isWorn
import com.sperance.exileforge.presentation.state.railGroups
import com.sperance.exileforge.presentation.state.sellLots
import com.sperance.exileforge.presentation.state.stashShelf
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/**
 * Тайник (3.90.3, макет «Тайник» В): сверху строка управления, слева рейка мест со счётом, справа список под названием
 * выбранного пункта. «Продать» включает режим выбора (макет «Продажа» А): у строк отметки, сверху быстрые наборы, снизу
 * удерживаемая кнопка с суммой; «Отмена» и системный «назад» выходят из него. Автопродажа - лист правил с «продать сейчас».
 *
 * Фильтры ([filter]) - экрана героя и уходят с ним; порядок и «скрыть надетое» хранятся на устройстве. Список, лоты и счёт рейки
 * запоминаются по копиям, а не по герою: изменившийся кошелёк не перестраивает тысячу строк.
 */
@Composable
internal fun StashPane(game: GameUi, model: HeroViewModel, shell: ShellViewModel, filter: StashFilter, onFilter: (StashFilter) -> Unit, onOpen: (String) -> Unit) {
    val hero = game.hero ?: return
    var filtering by remember { mutableStateOf(false) }
    var autoSelling by remember { mutableStateOf(false) }
    var selling by remember(game.heroId) { mutableStateOf(false) }
    // A copy whose template the content does not hold is left out rather than drawn blank.
    val stash = remember(hero.items, game.index, game.world) { hero.items.mapNotNull { game.view(it) } }
    // «Скрыть надетое» (3.69.0) - правило снаряжения: пункт инструментов показывает всё, что держит.
    val hideWorn = game.stashHideWorn && filter.group?.isTool != true
    val counted = remember(stash, game.stashHideWorn) { stash.filterNot { game.stashHideWorn && it.isWorn && !it.slot.isTool } }
    val rail = remember(counted) { railGroups(counted) }
    val rarities = remember(stash) { stash.map { it.rarity }.distinct().sortedByDescending { it.ordinal } }
    val visible = remember(stash, filter, game.stashSort, hideWorn, hero.level, hero.stats, game.world) { game.stashShelf(stash, filter, hideWorn) }
    val lines = rememberStashLines(game, visible)
    // Лоты - весь тайник, наборы - только видимое в открытом пункте рейки (3.90.4): отмеченное в других пунктах остаётся.
    val lots = remember(stash, hero.info.autoSell, hero.stats, hero.info.heroClass, game.index) { game.sellLots(stash) }
    val shown = remember(visible) { visible.mapTo(HashSet()) { it.id } }
    val pick = rememberSellPick(lots, shown)
    val selected = game.holding.selectedEquipment
    val leave = {
        selling = false
        pick.clear()
    }
    BackHandler(enabled = selling, onBack = leave)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (selling) {
                Text(ui("sell.mode_title"), color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                SellPresetRow(pick)
            } else {
                StashTopBar(
                    fill = { StashFill(game, model) },
                    sort = game.stashSort,
                    onSort = shell::stashSort,
                    autoSellMarks = hero.info.autoSell.marks,
                    onAutoSell = { autoSelling = true },
                    canSell = lots.isNotEmpty(),
                    onSell = { selling = true },
                    tweaks = stashTweaks(filter, showsWorn = filter.group?.isTool != true && !game.stashHideWorn),
                    onFilters = { filtering = true },
                )
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            StashRail(rail, counted.size, filter.group, game.lang, onSelect = { onFilter(filter.copy(group = it)) }, Modifier.fillMaxHeight().padding(start = 8.dp))
            LazyColumn(
                Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "shelf") { ShelfTitle(filter.group?.title(game.lang) ?: ui("stash.all_things"), visible.size) }
                if (hero.overflow.isNotEmpty() && !selling) item(key = "overflow") { StashOverflow(game, model) }
                if (visible.isEmpty()) item { InfoCard(ui("tree.nothing_found"), if (filter.active || hideWorn) ui("stash.filter_empty") else ui("hero.stash_empty_hint")) }
                items(lines, key = { it.piece.id }) { line ->
                    val verdict = rememberGearVerdict(game, line.piece.item)
                    if (selling) {
                        SellLine(pick, line.piece.id) { toggle -> StashTile(line, verdict, selected = pick.chosen(line.piece.id), onClick = toggle) }
                    } else {
                        StashTile(line, verdict, selected = line.piece.id == selected) {
                            onOpen(line.piece.id)
                            model.selectEquipment(line.piece.id)
                        }
                    }
                }
            }
        }
        if (selling) SellDock(pick, enabled = !game.busy, Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), onCancel = leave) { model.sellMany(it) }
    }
    if (filtering) {
        StashFilterSheet(
            filter,
            game.lang,
            rarities,
            onFilter = onFilter,
            hideWorn = game.stashHideWorn.takeUnless { filter.group?.isTool == true },
            onHideWorn = shell::stashHideWorn,
            onDismiss = { filtering = false },
        )
    }
    if (autoSelling) AutoSellSheet(hero.info.autoSell, lots, busy = game.busy, model) { autoSelling = false }
}

/** Название пункта рейки над списком и сколько в нём видно. */
@Composable private fun ShelfTitle(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(count.toString(), color = Muted, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Строка тайника (3.90.3) - плитка вещи [ItemTile] в облике её редкости: все строки мелко, напротив имени цена торговца,
 * стрелки урона и защиты и отметки - надето, заперто, не надеть, ждёт сети; под строками - чего не хватает, чтобы надеть.
 */
@Composable internal fun StashTile(line: StashLine, verdict: GearVerdict?, selected: Boolean, onClick: () -> Unit) {
    ItemTile(
        line.piece,
        selected = selected,
        onClick = onClick,
        trailing = {
            line.price?.let { GoldPrice(it) }
            verdict?.let { GearVerdictBadge(it) }
            StashMarks(line)
        },
    ) {
        line.unwearable.firstOrNull()?.let { Text(requirementReason(it), color = LifeRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) }
    }
}

/** Отметки строки значками: надето или в гнезде, заперто, не надеть, команда ждёт сети. */
@Composable private fun StashMarks(line: StashLine) {
    val locked = line.piece.item.locked
    if (!line.worn && !locked && line.unwearable.isEmpty() && !line.waiting) return
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (line.worn) Icon(Icons.Outlined.CheckCircle, ui("row.worn"), tint = Ink, modifier = Modifier.background(Gold, CircleShape).padding(1.dp).size(13.dp))
        if (locked) Icon(Icons.Outlined.Lock, ui("item.locked"), tint = GoldBright, modifier = Modifier.size(14.dp))
        if (line.unwearable.isNotEmpty()) Icon(Icons.Outlined.Block, ui("hero.inactive"), tint = LifeRed, modifier = Modifier.size(14.dp))
        if (line.waiting) PendingMark()
    }
}
