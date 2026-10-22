package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.LootBreakdown
import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.MapTally
import com.sperance.exileforge.core.campaign.RunSummary
import com.sperance.exileforge.core.campaign.run.RunHud
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.Rate
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.expedition.LootPresence
import com.sperance.exileforge.core.expedition.presentLoot
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.sellLots
import com.sperance.exileforge.core.quests.QuestTurnIn
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonSize
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.ItemRow
import com.sperance.exileforge.ui.components.LootCellSize
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.SaleMarked
import com.sperance.exileforge.ui.components.SellDock
import com.sperance.exileforge.ui.components.SellLine
import com.sperance.exileforge.ui.components.SellPick
import com.sperance.exileforge.ui.components.SellPresetRow
import com.sperance.exileforge.ui.components.StackCell
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.components.rememberSellPick
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay

/**
 * How a map's summary opens: its title and line, the colour and glyph of the ending, and the button that closes it.
 * Итог - из состояния захода ([MapEnd]): гибель, страж повержен (любым уходом) или карта покинута; зона Ваал - своими словами.
 */
internal data class SummaryHead(val title: String, val hint: String, val accent: Color, val glyph: ImageVector, val done: String) {
    companion object {
        fun of(hud: RunHud): SummaryHead {
            val back = ui(if (hud.returnsToMap) "vaal.back" else "expedition.back_to_camp")
            return when (hud.tally.end) {
                MapEnd.FELL -> SummaryHead(ui("expedition.dead"), ui(if (hud.vaal) "vaal.dead_hint" else "expedition.dead_hint"), LifeRed, ForgeGlyphs.Skull, back)

                MapEnd.CLEARED -> when {
                    hud.vaal -> SummaryHead(ui("vaal.done"), ui("vaal.done_hint"), Vital, ForgeGlyphs.Banner, back)
                    else -> SummaryHead(ui("expedition.map_done"), ui("expedition.map_done_hint"), Vital, ForgeGlyphs.Banner, back)
                }

                MapEnd.LEFT, null -> SummaryHead(ui("summary.left"), ui("summary.left_hint"), Gold, ForgeGlyphs.Portal, back)
            }
        }
    }
}

/**
 * The map is over — by a fall, the exit or the portal out — and before the camp, what it came to, compact: the ending and
 * the map's name, the figures in one line (foes and guardians slain, deaths, the time on the map), the rewards as tiles two
 * to a row — gold, experience, quests turned in and Atlas points, a tap opening one's breakdown under them — and every stack
 * and piece the server granted for it, folded under its count. The run in numbers is one line with the way into the whole
 * run's sheet. The loot is the server's roll: while some of it is on its way the screen says so, asks for it at once and
 * fills in as the answers arrive. A piece opens its comparison with what is worn, a stack its description.
 * Добычу продают здесь же: отметки у вещей, быстрые наборы и одна кнопка «Продать N и вернуться» на месте возврата в лагерь.
 *
 * Продажа добычи: лоты - копии захода в тайнике, помеченное в заходе и отложенная продажа сервера (добыча, что продаёт фильтр героя: в заходе
 * она легла в тайник, её id пришли с ответами журнала) уже отмечены, отметку можно снять - вещь остаётся. Выход из итогов продаёт отмеченное:
 * кнопка «Продать N и вернуться» вместо возврата, «Назад» - так же (с ценным - сперва режим продажи, где оно подтверждается); оставшуюся
 * отложенную добычу, кроме снятой, сервер продаёт сам. «Назад» живёт, как кнопки итогов: до их оживления и под командой оно ничего не делает. Из
 * выигранной зоны Ваал возвращаются на карту без продажи; павший в зоне продаёт и её добычу, и добычу основной карты. Разбор золота и опыта
 * - прибавки контекста захода, как их берёт сервер. Режим продажи включает удержание вещи; страница открывается в обычном режиме. Кнопки
 * итогов оживают не сразу: нажатие, пришедшее в кнопку отчёта боя на том же месте, не закрывает заход. Ниже уровня штрафа гибель даром.
 */
@Composable internal fun MapSummary(
    game: GameUi,
    vm: ExpeditionViewModel,
    hud: RunHud,
    head: SummaryHead = SummaryHead.of(hud),
    onDone: (sell: List<String>, keep: List<String>) -> Unit,
) {
    val tally = hud.tally
    var looked by remember { mutableStateOf<ItemView?>(null) }
    val inspect = rememberInspect()
    LaunchedEffect(tally.receiving) { if (tally.receiving) vm.flushRun() }
    val hero = game.hero
    val dressed = rememberRunGame(game, vm)
    val run by vm.run.collectAsStateWithLifecycle()
    val back = hud.returnsToMap
    val lots = remember(tally.loot.equipment, hero?.items, hero?.info?.autoSell, hero?.stats, hero?.level, game.index, back) {
        val ids = tally.loot.equipment.mapTo(HashSet()) { it.id }
        if (hud.vaal && !back) ids += vm.outerLoot()
        game.sellLots(hero?.items.orEmpty().filter { it.id in ids }.mapNotNull { game.view(it) })
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val marks = state.saleMarks
    val potion = remember(state.potion, game.index) { state.potion?.let { game.index?.rules?.brews?.potion(it) }.orEmpty() }
    val gold = remember(run?.run, run?.pacts, potion, tally.gold) { run?.let { LootBreakdown.gold(it.run, it.pacts, potion, tally.gold.toDouble()) } }
    val experience = remember(run?.run, run?.pacts, potion, tally.experience, hud.vaal) { run?.let { LootBreakdown.experience(it.run, it.pacts, potion, tally.experience, hud.vaal) } }
    val pick = rememberSellPick(lots, initial = marks)
    val offered = lots.isNotEmpty() && !back
    val seen = remember { HashSet(marks) }
    LaunchedEffect(pick, marks) { pick.markNew(marks, seen) }
    var selling by rememberSaveable { mutableStateOf(false) }
    val selecting = offered && selling
    val docked = offered && (selling || pick.picked.isNotEmpty())
    val finish: (List<String>) -> Unit = { sold -> onDone(sold, lots.map { it.id } - sold.toSet()) }
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ARM_DELAY)
        armed = true
    }
    BackHandler(enabled = selecting) {
        pick.clear()
        selling = false
    }
    BackHandler(enabled = !selecting && armed && !game.busy) {
        val picked = pick.picked.takeIf { offered }.orEmpty()
        if (picked.any { it.valuable }) selling = true else finish(picked.map { it.id })
    }
    Column(
        Modifier.fillMaxSize().background(Ink.copy(alpha = .94f)).statusBarsPadding().navigationBarsPadding().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryBanner(head, mapTitle(hud.mapCode))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MapFigures(tally)
            RewardTiles(rewardTiles(tally, gold, experience, state.turnedIn, state.atlasPoints))
            if (tally.end == MapEnd.FELL) {
                val lost = hud.fall?.takeIf { it > 0 }
                if (lost != null) {
                    Text(ui("expedition.fall_lost", number(lost)), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                } else {
                    hud.fallFrom?.let { MutedText(ui("expedition.fall_from", it), style = MaterialTheme.typography.bodySmall) }
                }
            }
            Loot(
                game,
                dressed,
                tally,
                pick.takeIf { offered },
                selecting,
                onItem = { looked = it },
                onStack = { inspect(Inspect.Stack(it)) },
                onSelect = { id ->
                    selling = true
                    if (!pick.chosen(id)) pick.toggle(id)
                },
            )
            if (tally.end == MapEnd.FELL) DeathRecap(hud.recap)
            RunLine(tally.figures)
        }
        if (docked) {
            SellDock(
                pick,
                enabled = armed && !game.busy,
                verb = "sell.do_n_return",
                shards = { lots -> game.shardsFor(lots.map { it.piece.rarity }) },
                onCancel = {
                    pick.clear()
                    selling = false
                },
                onSell = finish,
            )
        } else {
            ForgeButton(
                onClick = { finish(emptyList()) },
                state = ButtonState.of(armed),
                modifier = Modifier.fillMaxWidth(),
                size = ButtonSize.Wide,
                accent = head.accent,
            ) {
                Text(head.done, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    looked?.let { item -> LootSheet(game, vm, item, onDismiss = { looked = null }, markable = false) }
}

/** The ending over the map's name and why: its glyph and title in its colour, on a glow of it. */
@Composable private fun SummaryBanner(head: SummaryHead, map: String) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().heightIn(min = 84.dp).clip(shape)
            .background(Brush.radialGradient(listOf(head.accent.copy(alpha = .3f), Ink)), shape)
            .border(1.dp, head.accent.copy(alpha = .25f), shape).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(head.glyph, null, tint = head.accent, modifier = Modifier.size(18.dp))
            Text(head.title, color = head.accent, style = MaterialTheme.typography.titleMedium)
        }
        Text(listOf(map, head.hint).joinToString(" · "), color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

/** The map's figures in one line: foes and guardians slain, deaths (red once there is one) and the time on the map. */
@Composable private fun MapFigures(tally: MapTally) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Figure(tally.kills.toString(), ui("summary.kills"))
        Figure(tally.bosses.toString(), ui("summary.bosses"))
        Figure(tally.deaths.toString(), ui("summary.deaths"), if (tally.deaths > 0) LifeRed else Parchment)
        Figure(clock(tally.seconds), null)
    }
}

@Composable private fun Figure(value: String, label: String?, tone: Color = Parchment) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = tone, fontWeight = FontWeight.SemiBold)) { append(value) }
            label?.let { append(" $it") }
        },
        color = Muted,
        style = MaterialTheme.typography.labelMedium,
    )
}

/**
 * The map's reward tiles: gold and experience — their breakdowns while the run is still on screen ([gold], [experience]),
 * the quests turned in at once ([turnedIn]) and the Atlas points earned ([atlas]) when there are any.
 */
private fun rewardTiles(tally: MapTally, gold: LootBreakdown?, experience: LootBreakdown?, turnedIn: List<QuestTurnIn>, atlas: List<String>): List<SummaryTile> = listOfNotNull(
    breakdownTile("gold", ForgeGlyphs.Coins, tally.gold.toDouble(), ui("summary.gold"), GoldBright, gold),
    breakdownTile("experience", ForgeGlyphs.Tome, tally.experience, ui("summary.experience"), Rune, experience),
    turnedIn.takeIf { it.isNotEmpty() }?.let { quests ->
        val rewards = quests.map { it.rewards }
        val coins = rewards.sumOf { it.gold }
        val orbs = rewards.sumOf { it.orbs.values.sum() }.toInt()
        val worth = listOfNotNull(
            coins.takeIf { it > 0 }?.let { "+" + compactNumber(it.toDouble()) },
            orbs.takeIf { it > 0 }?.let { ui("summary.counted", it, plural("summary.orb", it)) },
        ).ifEmpty { listOf("+" + compactNumber(rewards.sumOf { it.experience }.toDouble())) }
        SummaryTile("quests", ForgeGlyphs.Scroll, questCount(quests), worth.joinToString(" · "), Vital, Vital) { TurnedInLines(quests) }
    },
    atlas.takeIf { it.isNotEmpty() }?.let { keys ->
        SummaryTile("atlas", ForgeGlyphs.Constellation, "+${keys.size}", plural("summary.atlas_point", keys.size), Rune, Rune) { AtlasLines(keys) }
    },
)

/** Число сданных заданий на плитке: всё продвинул заход - «N заданий», иначе «за карту + заодно» («2 + 3»). */
private fun questCount(quests: List<QuestTurnIn>): String {
    val run = quests.count { it.run }
    val also = quests.size - run
    return if (also == 0) ui("summary.counted", run, plural("summary.quest", run)) else ui("summary.quests_split", run, also)
}

/**
 * Плитка золота или опыта: итог [breakdown] (без захода на экране - итог сервера [total]), множитель в углу и разбор по нажатию. Прибавки
 * героя в разборе - по набору, что поход носит сейчас: набор в походе меняется, а прибавки каждого боя не хранятся.
 */
private fun breakdownTile(id: String, icon: ImageVector, total: Double, label: String, tone: Color, breakdown: LootBreakdown?): SummaryTile {
    val parts = breakdown?.takeIf { it.parts.isNotEmpty() }
    return SummaryTile(
        id,
        icon,
        compactNumber(breakdown?.total ?: total),
        label,
        tone,
        corner = parts?.let { ui("summary.multiplier", number(it.multiplier)) },
        panel = if (parts != null) ({ BreakdownParts(parts, tone, ui("summary.by_current_gear")) }) else null,
    )
}

/** The run in numbers as one line — damage a second and damage taken — and the way into the whole run's sheet. */
@Composable private fun RunLine(summary: RunSummary) {
    if (summary.seconds <= 0) return
    var sheet by remember { mutableStateOf(false) }
    HorizontalDivider(thickness = 1.dp, color = Bronze)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MutedText(ui("summary.run_line", Rate.of(compactNumber(summary.dps)), compactNumber(summary.totalTaken)), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        ForgeButton(onClick = { sheet = true }, kind = ButtonKind.Text) { Text(ui("summary.whole_run")) }
    }
    if (sheet) FightStatsSheet(null, summary.fights) { sheet = false }
}

/**
 * Everything the server granted for the map: the recipe, the pieces a line each, the stacks as chips; on its way, or its absence said.
 * Метки «Лучше надетого» - против набора похода ([dressed]). Продажа - как в тайнике: нажатие открывает карточку вещи, удержание лота [pick] включает режим продажи [selecting]
 * и отмечает её ([onSelect]); в режиме нажатие отмечает, над вещами - быстрые наборы.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Loot(game: GameUi, dressed: GameUi, tally: MapTally, pick: SellPick?, selecting: Boolean, onItem: (ItemView) -> Unit, onStack: (String) -> Unit, onSelect: (String) -> Unit) {
    val loot = tally.loot
    var open by rememberSaveable { mutableStateOf(true) }
    LootHeader(loot.equipment.size, pick?.picked?.size ?: 0, open) { open = !open }
    if (!open) return
    loot.recipe?.let { code ->
        val text = game.index?.let { i -> i.recipe(code)?.let { recipeText(i, it) } } ?: displayName(code)
        Text(ui("summary.recipe", text), color = Rune, style = MaterialTheme.typography.bodySmall)
    }
    pick?.let {
        if (selecting) SellPresetRow(it)
        MutedText(ui(if (selecting) "sell.summary_hint" else "sell.summary_hold"), style = MaterialTheme.typography.bodySmall)
    }
    game.presentLoot(loot.equipment, arriving = tally.receiving).forEach { (instance, presence) ->
        game.view(instance)?.let { piece ->
            val lot = pick?.takeIf { presence.actionable && it.sellable(piece.id) }
            val (_, mark) = rememberLootVerdict(dressed, instance, flasks = false)
            when {
                lot != null && selecting -> SellLine(lot, piece.id) { chosen, toggle -> PieceLine(piece, presence, selected = chosen, onLongClick = { onItem(piece) }, mark = mark, onClick = toggle) }

                lot != null -> {
                    val marked = lot.chosen(piece.id)
                    SaleMarked(marked) { PieceLine(piece, presence, selected = marked, onLongClick = { onSelect(piece.id) }, mark = mark) { onItem(piece) } }
                }

                else -> PieceLine(piece, presence, mark = mark) { onItem(piece) }
            }
        }
    }
    if (loot.items.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            loot.items.entries.sortedByDescending { it.value }.forEach { (code, amount) ->
                StackCell(game.index, code, amount, size = LootCellSize) { onStack(code) }
            }
        }
    }
    if (tally.receiving) {
        Receiving()
    } else if (tally.empty) {
        MutedText(ui("summary.loot_none"))
    }
}

/** «Добыча · N» с меткой «M к продаже» и стрелкой: нажатие сворачивает и разворачивает добычу. */
@Composable private fun LootHeader(pieces: Int, marked: Int, open: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onToggle).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            (if (pieces > 0) ui("summary.loot_count", pieces) else ui("summary.loot")).uppercase(),
            color = Gold,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (marked > 0) {
            val shape = RoundedCornerShape(6.dp)
            Text(
                ui("summary.to_sell", marked),
                color = Caution,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.background(Caution.copy(alpha = .12f), shape).border(1.dp, Caution.copy(alpha = .35f), shape).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Text(if (open) "▴" else "▾", color = Muted, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Добытая вещь «Полем боя»: две линии - имя целиком, под ним значки тиров и отметки; всё прочее - в карточке по нажатию.
 * Надетая ([presence]) - строкой с меткой «Надето», без действий. [mark] - метка похода напротив имени и рамка её цвета ([LootMark]).
 */
@Composable internal fun PieceLine(
    item: ItemView,
    presence: LootPresence,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    mark: LootMark? = null,
    onClick: () -> Unit,
) = if (presence.worn) {
    WornLootRow(item)
} else {
    ItemRow(item, note = mark?.text, noteColor = mark?.color ?: Gold, compact = true, selected = selected, trailing = trailing, onLongClick = onLongClick, accent = mark?.color, onClick = onClick)
}

/** Сколько итоги карты глухи к нажатиям после появления, мс. */
private const val ARM_DELAY = 600L
