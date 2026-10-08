package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.campaign.RiftArena
import com.sperance.exileforge.core.campaign.TrialPhase
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.RiftLaw
import com.sperance.exileforge.rules.content.RiftNodeKind
import com.sperance.exileforge.rules.content.RiftRarity
import com.sperance.exileforge.rules.content.RiftRule
import com.sperance.exileforge.rules.content.RiftRules
import com.sperance.exileforge.rules.content.TraitLine
import com.sperance.exileforge.rules.content.TrialBoard
import com.sperance.exileforge.rules.rift.RiftAct
import com.sperance.exileforge.rules.rift.RiftBoard
import com.sperance.exileforge.rules.rift.RiftEngine
import com.sperance.exileforge.rules.rift.RiftNode
import com.sperance.exileforge.rules.rift.RiftOffer
import com.sperance.exileforge.rules.rift.RiftRest
import com.sperance.exileforge.rules.rift.RiftRun
import com.sperance.exileforge.rules.rift.RiftStage
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.expedition.arena.ArenaOverlay
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** Палитра Разлома (RULES.md клиента, «Стражи Разлома»): ядовито-зелёный на почти чёрном. */
internal object RiftColors {
    val Ground = Color(0xFF030504)
    val Ground2 = Color(0xFF07100B)
    val Panel = Color(0xFF0A120D)
    val Line = Color(0xFF1F3427)
    val LineHi = Color(0xFF2F5A3F)
    val Rift = Color(0xFF39FF88)
    val Soft = Color(0xFF9DFFB8)
    val Hot = Color(0xFFD4FF6A)
    val Deep = Color(0xFF0B3A21)
    val Text = Color(0xFFDBE8DC)
    val Muted = Color(0xFF7F9686)
    val Dim = Color(0xFF4B5E51)
    val Chaos = Color(0xFFB77CFF)
    val Warn = Color(0xFFFF6B4A)
}

/** Цвет редкости дара. */
internal fun RiftRarity.color(): Color = when (this) {
    RiftRarity.COMMON -> RiftColors.Soft
    RiftRarity.RARE -> RiftColors.Hot
    RiftRarity.LEGENDARY -> RiftColors.Chaos
}

/** Цвет вида узла на схеме. */
private fun RiftNodeKind.color(): Color = when (this) {
    RiftNodeKind.FIGHT -> RiftColors.Soft
    RiftNodeKind.ELITE, RiftNodeKind.CHAMPION -> RiftColors.Hot
    RiftNodeKind.GUARDIAN -> RiftColors.Warn
    RiftNodeKind.ALTAR -> RiftColors.Chaos
    RiftNodeKind.REST, RiftNodeKind.TREASURE, RiftNodeKind.FORGE -> RiftColors.Rift
}

/**
 * Разлом недели (3.96.0) на весь экран: доска недели, схема забега с узлами, предложение узла, бой узла и итог. Пока идёт
 * бой, экран - арена; назад с доски закрывает Разлом, забег ждёт на сервере до конца недели.
 */
@Composable fun RiftScreen() {
    val model = koinViewModel<ExpeditionViewModel>()
    val game by model.game.collectAsStateWithLifecycle()
    val state by model.riftState.collectAsStateWithLifecycle()
    val arena = state.arena
    if (arena != null) return RiftFight(game, model, arena)
    BackHandler { model.closeRift() }
    val index = game.index ?: return
    val rules = index.campaign.trials?.rift ?: return
    Box(Modifier.fillMaxSize().background(RiftColors.Ground)) {
        val board = state.board
        if (board == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = RiftColors.Rift) }
        } else {
            RiftBoardView(game, model, rules, board)
        }
        state.result?.let { RiftResultSheet(game, model, it) }
        state.table?.let { TrialTableSheet(it) { model.trialTable(null) } }
    }
}

@Composable private fun RiftBoardView(game: GameUi, model: ExpeditionViewModel, rules: RiftRules, board: RiftBoard) {
    val index = game.index ?: return
    val engine = remember(board.plan) { RiftEngine(rules, index.campaign, board.plan) }
    val run = board.progress.run
    val idle = !game.busy
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ui("rift.title"), color = RiftColors.Rift, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            ForgeTextButton(onClick = model::closeRift) { Text(ui("rift.close"), color = RiftColors.Muted) }
        }
        WeekPlate(game, model, rules, board, idle)
        if (run != null) {
            RunPlate(rules, engine, run)
            when (run.stage) {
                RiftStage.OFFER -> run.offer?.let { OfferPlate(rules, engine, run, it, idle, model::riftAct) }
                else -> Unit
            }
            RiftMap(rules, engine, run, idle) { model.riftAct(RiftAct.Move(it)) }
            var ending by remember { mutableStateOf(false) }
            ForgeOutlinedButton(onClick = { ending = true }, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(ui("rift.end"), color = RiftColors.Warn) }
            if (ending) {
                ConfirmSheet(ui("rift.end"), ui("rift.end"), onDismiss = { ending = false }, note = ui("rift.end_confirm"), danger = true) {
                    ending = false
                    model.riftAct(RiftAct.End)
                }
            }
        } else {
            RiftMap(rules, engine, null, idle = false) {}
        }
    }
}

/** Неделя: лига, сколько осталось, бесплатные забеги, мутаторы, лучший счёт, вход и таблица. */
@Composable private fun WeekPlate(game: GameUi, model: ExpeditionViewModel, rules: RiftRules, board: RiftBoard, idle: Boolean) {
    val hero = game.hero ?: return
    val keys = hero.bag[RiftRules.KEY] ?: 0L
    RiftPlate(RiftColors.LineHi) {
        val left = ((board.endsAt - System.currentTimeMillis()) / 1000.0).coerceAtLeast(0.0)
        Text(ui("rift.ends", clock(left)), color = RiftColors.Muted, style = MaterialTheme.typography.labelMedium)
        val league = board.league
        Text(
            league?.let { ui("rift.league", rules.leagues[it]) } ?: ui("rift.league_none", rules.leagues.first()),
            color = RiftColors.Text,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(ui("rift.free", board.free, rules.free), color = RiftColors.Text, style = MaterialTheme.typography.bodySmall)
        if (board.progress.best > 0) Text(ui("rift.best", board.progress.best), color = RiftColors.Hot, style = MaterialTheme.typography.bodySmall)
        Text(ui("rift.mutators"), color = RiftColors.Muted, style = MaterialTheme.typography.labelMedium)
        board.plan.mutators.mapNotNull(rules.mutatorsByCode::get).forEach { mutator ->
            Text(
                "${ui(if (mutator.good) "rift.mutator_good" else "rift.mutator_evil")}: ${loc("rift.mutator.${mutator.code}.name")} - ${loc("rift.mutator.${mutator.code}.description")}",
                color = if (mutator.good) RiftColors.Soft else RiftColors.Warn,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (board.progress.run == null) {
            // Причина у неактивной кнопки: уровень лиги, ключ сверх бесплатных
            val reason = when {
                league == null -> ui("rift.league_none", rules.leagues.first())
                board.free <= 0 && keys < 1 -> ui("rift.start_locked")
                else -> null
            }
            ForgeButton(onClick = model::startRift, enabled = idle && reason == null, modifier = Modifier.fillMaxWidth()) {
                Text(if (board.free > 0) ui("rift.start") else ui("rift.start_key", itemTitle(RiftRules.KEY)))
            }
            reason?.let { Text(it, color = RiftColors.Warn, style = MaterialTheme.typography.labelSmall) }
        }
        ForgeOutlinedButton(onClick = { model.trialTable(TrialBoard.RIFT, league = league) }, enabled = idle && league != null, modifier = Modifier.fillMaxWidth()) {
            Text(ui("rift.table"))
        }
    }
}

/** Забег: счёт, Эхо, здоровье, дары по ячейкам и проклятия. */
@Composable private fun RunPlate(rules: RiftRules, engine: RiftEngine, run: RiftRun) {
    RiftPlate(RiftColors.Rift) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ui("rift.score", engine.score(run)), color = RiftColors.Hot, style = MaterialTheme.typography.titleSmall)
            Text(ui("rift.echo", run.echo), color = RiftColors.Rift, style = MaterialTheme.typography.titleSmall)
            Text(ui("rift.life", (run.life * 100).toInt()), color = LifeRed, style = MaterialTheme.typography.titleSmall)
        }
        Text(ui("rift.boons", run.boons.size, rules.nodes.slots), color = RiftColors.Muted, style = MaterialTheme.typography.labelMedium)
        run.boons.forEach { held -> BoonLine(rules, held.code, held.grade) }
        Text(ui("rift.curses"), color = RiftColors.Muted, style = MaterialTheme.typography.labelMedium)
        if (run.curses.isEmpty()) Text(ui("rift.no_curses"), color = RiftColors.Dim, style = MaterialTheme.typography.bodySmall)
        run.curses.forEach { curse -> CurseLine(curse.code, curse.altar) }
    }
}

/** Дар одной строкой: имя цветом редкости, ступень и его строки листа. */
@Composable private fun BoonLine(rules: RiftRules, code: String, grade: Int = 0, modifier: Modifier = Modifier) {
    val boon = rules.boonsByCode[code] ?: return
    val scale = 1 + rules.nodes.gradePower / 100 * grade
    Column(modifier) {
        Text(
            loc("rift.boon.$code.name") + if (grade > 0) " · ${ui("rift.grade", grade)}" else "",
            color = boon.rarity.color(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        boon.lines.forEach { line -> Text(lineText(line, scale), color = RiftColors.Text, style = MaterialTheme.typography.labelSmall) }
    }
}

private fun lineText(line: TraitLine, scale: Double = 1.0): String = SkillText.statLine(line.stat, line.op, line.value * scale)

@Composable private fun CurseLine(code: String, altar: Boolean = false) {
    Text(
        "${loc("rift.curse.$code.name")} - ${loc("rift.curse.$code.description")}" + if (altar) " · ${ui("rift.altar_mark")}" else "",
        color = RiftColors.Warn,
        style = MaterialTheme.typography.bodySmall,
    )
}

/**
 * Схема: акты сверху вниз, ряды узлов, связи линиями. Пройденные узлы тусклые, доступные - светятся и нажимаются; ряды
 * дальше тумана («Туман») скрыты.
 */
@Composable private fun RiftMap(rules: RiftRules, engine: RiftEngine, run: RiftRun?, idle: Boolean, onMove: (Int) -> Unit) {
    val plan = engine.plan
    val reachable = if (run != null && run.stage == RiftStage.MOVE) engine.next(run).toSet() else emptySet()
    val here = run?.at?.let(plan::node)
    val fog = run?.let { engine.rule(it, RiftRule.FOG).toInt() } ?: 0
    var looking by remember { mutableStateOf<RiftNode?>(null) }
    (0 until rules.acts).forEach { act ->
        val nodes = plan.nodes.filter { it.act == act }
        val rows = nodes.maxOf { it.row } + 1
        Text(ui("rift.act", act + 1), color = RiftColors.Muted, style = MaterialTheme.typography.labelLarge)
        BoxWithConstraints(Modifier.fillMaxWidth().height((rows * ROW).dp).background(RiftColors.Ground2, RoundedCornerShape(10.dp)).border(1.dp, RiftColors.Line, RoundedCornerShape(10.dp))) {
            val width = maxWidth.value
            fun at(node: RiftNode) = Offset(((node.lane + .5f) / node.width) * width, node.row * ROW + ROW / 2f)
            val step = rules.layout.rows + 1
            val reach = here?.let { it.act * step + it.row } ?: -1
            val hidden: (RiftNode) -> Boolean = { node -> fog > 0 && run != null && node.act * step + node.row > reach + 1 }
            Canvas(Modifier.matchParentSize()) {
                nodes.forEach { node ->
                    node.links.mapNotNull(plan::node).filter { it.act == act }.forEach { next ->
                        val walked = run != null && node.id in run.path && next.id in run.path
                        drawLine(if (walked) RiftColors.Rift else RiftColors.LineHi, at(node).let { Offset(it.x.dp.toPx(), it.y.dp.toPx()) }, at(next).let { Offset(it.x.dp.toPx(), it.y.dp.toPx()) }, strokeWidth = if (walked) 3.dp.toPx() else 1.5.dp.toPx())
                    }
                }
            }
            nodes.forEach { node ->
                val p = at(node)
                val open = node.id in reachable && idle
                val walked = run != null && node.id in run.path
                val masked = hidden(node)
                val size = if (node.kind == RiftNodeKind.GUARDIAN) 46 else 34
                Box(
                    Modifier.offset((p.x - size / 2f).dp, (p.y - size / 2f).dp).size(size.dp)
                        .background(if (walked) RiftColors.Deep else RiftColors.Panel, CircleShape)
                        .border(
                            if (open) 2.dp else 1.dp,
                            if (masked) {
                                RiftColors.Dim
                            } else if (open) {
                                RiftColors.Rift
                            } else {
                                node.kind.color().copy(alpha = if (walked) .5f else .8f)
                            },
                            CircleShape,
                        )
                        .clickable(enabled = !masked) { looking = node },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (masked) "?" else loc("rift.node.${node.kind.name}.name").take(1), color = if (masked) RiftColors.Dim else node.kind.color(), style = MaterialTheme.typography.labelLarge)
                    if (node.curse != null && !masked) Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(RiftColors.Warn, CircleShape))
                }
            }
        }
    }
    looking?.let { node ->
        NodeSheet(rules, node, open = node.id in reachable && idle, onDismiss = { looking = null }) {
            looking = null
            onMove(node.id)
        }
    }
}

private const val ROW = 58

/** Узел: вид и его суть, проклятие, свойства Вождя, условие Чемпиона (скрыто до боя), страж акта. */
@Composable private fun NodeSheet(rules: RiftRules, node: RiftNode, open: Boolean, onDismiss: () -> Unit, onGo: () -> Unit) {
    val lines = buildList {
        add(loc("rift.node.${node.kind.name}.description"))
        if (node.kind == RiftNodeKind.GUARDIAN) rules.guardians.monsters.getOrNull(node.act)?.let { add(loc("monster.${it.value}.name") + ": " + loc("rift.guardian.${it.value}.description")) }
        node.curse?.let { add(ui("rift.node_curse", loc("rift.curse.$it.name") + " - " + loc("rift.curse.$it.description"))) }
        if (node.traits.isNotEmpty()) add(ui("rift.node_traits", node.traits.joinToString(", ") { loc("trait.$it.name") }))
        if (node.trial != null) add(ui("rift.node_trial"))
    }
    ConfirmSheet(loc("rift.node.${node.kind.name}.name"), ui("rift.node_go"), onDismiss = onDismiss, note = lines.joinToString("\n"), blocked = !open, warning = if (open) null else ui("rift.node_closed")) { onGo() }
}

/** Предложение узла: дары, пары алтаря, Отдых, Кузня, контракт Владыки. */
@Composable private fun OfferPlate(rules: RiftRules, engine: RiftEngine, run: RiftRun, offer: RiftOffer, idle: Boolean, onAct: (RiftAct) -> Unit) {
    val full = run.boons.size >= rules.nodes.slots
    var replacing by remember(offer) { mutableStateOf<((Int) -> RiftAct)?>(null) }
    RiftPlate(RiftColors.Hot) {
        when (offer) {
            is RiftOffer.Boons -> {
                Text(ui("rift.boons_title"), color = RiftColors.Hot, style = MaterialTheme.typography.titleSmall)
                offer.options.forEachIndexed { i, code ->
                    Choice(idle, { if (full) replacing = { slot -> RiftAct.Take(i, slot) } else onAct(RiftAct.Take(i)) }) { BoonLine(rules, code) }
                }
                ForgeTextButton(onClick = { onAct(RiftAct.Leave) }, enabled = idle) { Text(ui("rift.skip"), color = RiftColors.Muted) }
            }

            is RiftOffer.Altar -> {
                Text(ui("rift.altar_title"), color = RiftColors.Chaos, style = MaterialTheme.typography.titleSmall)
                Text(ui("rift.altar_hint", rules.score.altarCurse.toInt()), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall)
                offer.pacts.forEachIndexed { i, pact ->
                    Choice(idle, { if (full) replacing = { slot -> RiftAct.Take(i, slot) } else onAct(RiftAct.Take(i)) }) {
                        BoonLine(rules, pact.boon)
                        CurseLine(pact.curse)
                    }
                }
                ForgeTextButton(onClick = { onAct(RiftAct.Leave) }, enabled = idle) { Text(ui("rift.skip"), color = RiftColors.Muted) }
            }

            RiftOffer.Rest -> RestChoices(rules, run, idle, onAct)

            is RiftOffer.Forge -> ForgeChoices(rules, engine, run, offer, idle, full, { replacing = it }, onAct)

            RiftOffer.Contract -> {
                Text(ui("rift.contract_title"), color = RiftColors.Warn, style = MaterialTheme.typography.titleSmall)
                Text(ui("rift.contract_sign"), color = RiftColors.Text, style = MaterialTheme.typography.bodySmall)
                Text(ui("rift.contract_hint"), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall)
                rules.guardians.lord.laws.forEach { law -> LawChoice(law, idle && run.boons.isNotEmpty()) { onAct(RiftAct.Contract(law)) } }
                if (run.boons.isEmpty()) Text(ui("rift.contract_no_boons"), color = RiftColors.Warn, style = MaterialTheme.typography.labelSmall)
                ForgeButton(onClick = { onAct(RiftAct.Contract(null)) }, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(ui("rift.contract_decline", rules.score.contract)) }
            }
        }
    }
    replacing?.let { make ->
        SlotPicker(rules, run, ui("rift.replace_hint"), onDismiss = { replacing = null }) { slot ->
            replacing = null
            onAct(make(slot))
        }
    }
}

@Composable private fun LawChoice(law: RiftLaw, enabled: Boolean, onClick: () -> Unit) {
    Choice(enabled, onClick) {
        Text(loc("rift.law.${law.name}.name"), color = RiftColors.Text, style = MaterialTheme.typography.bodyMedium)
        Text(loc("rift.law.${law.name}.description"), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall)
    }
}

/** Отдых: лечение, усиление дара (выбор дара) или снятие проклятия (выбор проклятия). */
@Composable private fun RestChoices(rules: RiftRules, run: RiftRun, idle: Boolean, onAct: (RiftAct) -> Unit) {
    var target by remember { mutableStateOf<RiftRest?>(null) }
    Text(ui("rift.rest_title"), color = RiftColors.Rift, style = MaterialTheme.typography.titleSmall)
    RiftRest.entries.forEach { choice ->
        val possible = when (choice) {
            RiftRest.HEAL -> true
            RiftRest.UPGRADE -> run.boons.any { it.grade < rules.nodes.maxGrade }
            RiftRest.CLEANSE -> run.curses.isNotEmpty()
        }
        Choice(idle && possible, { if (choice == RiftRest.HEAL) onAct(RiftAct.Rest(choice)) else target = choice }) {
            Text(ui("rift.rest.${choice.name}"), color = RiftColors.Text, style = MaterialTheme.typography.bodyMedium)
        }
    }
    when (target) {
        RiftRest.UPGRADE -> SlotPicker(rules, run, ui("rift.rest_pick_boon"), onDismiss = { target = null }, enabled = { run.boons[it].grade < rules.nodes.maxGrade }) {
            target = null
            onAct(RiftAct.Rest(RiftRest.UPGRADE, it))
        }

        RiftRest.CLEANSE -> ForgeSheet(onDismissRequest = { target = null }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui("rift.rest_pick_curse"), color = RiftColors.Text, style = MaterialTheme.typography.titleSmall)
                run.curses.forEachIndexed { i, curse ->
                    Choice(true, {
                        target = null
                        onAct(RiftAct.Rest(RiftRest.CLEANSE, i))
                    }) { CurseLine(curse.code, curse.altar) }
                }
            }
        }

        else -> Unit
    }
}

/** Кузня: витрина за Эхо и слияние двух даров одной редкости; открыта, пока герой не уйдёт. */
@Composable private fun ForgeChoices(
    rules: RiftRules,
    engine: RiftEngine,
    run: RiftRun,
    offer: RiftOffer.Forge,
    idle: Boolean,
    full: Boolean,
    onReplace: (((Int) -> RiftAct)?) -> Unit,
    onAct: (RiftAct) -> Unit,
) {
    var merging by remember { mutableStateOf(false) }
    Text(ui("rift.forge_title"), color = RiftColors.Rift, style = MaterialTheme.typography.titleSmall)
    offer.shop.forEachIndexed { i, code ->
        val price = rules.boonsByCode[code]?.rarity?.let(rules.nodes.prices::get) ?: 0
        Choice(idle && run.echo >= price, { if (full) onReplace { slot -> RiftAct.Take(i, slot) } else onAct(RiftAct.Take(i)) }) {
            BoonLine(rules, code)
            Text(ui("rift.forge_buy", price), color = if (run.echo >= price) RiftColors.Rift else RiftColors.Warn, style = MaterialTheme.typography.labelSmall)
        }
    }
    val mergeable = run.boons.groupBy { rules.boonsByCode[it.code]?.rarity }.any { (rarity, held) -> rarity?.next != null && held.size >= 2 }
    ForgeOutlinedButton(onClick = { merging = true }, enabled = idle && mergeable && run.echo >= rules.nodes.merge, modifier = Modifier.fillMaxWidth()) {
        Text(ui("rift.forge_merge", rules.nodes.merge))
    }
    ForgeTextButton(onClick = { onAct(RiftAct.Leave) }, enabled = idle) { Text(ui("rift.forge_leave"), color = RiftColors.Muted) }
    if (merging) {
        var first by remember { mutableStateOf<Int?>(null) }
        val rarityOf = { i: Int -> rules.boonsByCode[run.boons[i].code]?.rarity }
        SlotPicker(
            rules,
            run,
            ui("rift.merge_pick"),
            onDismiss = { merging = false },
            enabled = { i -> rarityOf(i)?.next != null && (first == null || (i != first && rarityOf(i) == rarityOf(first!!))) },
        ) { i ->
            val a = first
            if (a == null) {
                first = i
            } else {
                merging = false
                onAct(RiftAct.Merge(a, i))
            }
        }
    }
}

/** Выбор ячейки дара. */
@Composable private fun SlotPicker(rules: RiftRules, run: RiftRun, title: String, onDismiss: () -> Unit, enabled: (Int) -> Boolean = { true }, onPick: (Int) -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = RiftColors.Text, style = MaterialTheme.typography.titleSmall)
            run.boons.forEachIndexed { i, held -> Choice(enabled(i), { onPick(i) }) { BoonLine(rules, held.code, held.grade) } }
        }
    }
}

@Composable private fun Choice(enabled: Boolean, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().background(RiftColors.Panel, shape).border(1.dp, if (enabled) RiftColors.LineHi else RiftColors.Line, shape)
            .clickable(enabled = enabled, onClick = onClick).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}

@Composable internal fun RiftPlate(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().background(RiftColors.Panel, shape).border(1.dp, accent.copy(alpha = .5f), shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** Бой узла: арена, как у испытаний, и плашка с волной и условием Чемпиона. */
@Composable private fun RiftFight(game: GameUi, model: ExpeditionViewModel, arena: RiftArena) {
    val shell: ShellViewModel = koinViewModel()
    val hud by arena.hud.collectAsState()
    LaunchedEffect(arena) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) arena.update(((now - last) / 1e9).coerceAtMost(.05))
                last = now
            }
        }
    }
    // Посреди боя уйти нельзя: назад ставит паузу
    BackHandler { model.riftCommand(RunCommand.Pause) }
    Box(Modifier.fillMaxSize().background(Ink)) {
        hud.fight?.takeIf { hud.phase == TrialPhase.FIGHT }?.let { fight ->
            ArenaOverlay(game, hud.run, fight, hud.level, arena.combat, arena.stance, onCommand = model::riftCommand, onLogFilter = shell::logFilter, onBuzz = shell::buzz)
        }
        val shape = RoundedCornerShape(10.dp)
        Column(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 44.dp, end = 8.dp).widthIn(max = 190.dp)
                .background(RiftColors.Panel.copy(alpha = .92f), shape).border(1.dp, RiftColors.LineHi, shape).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(loc("rift.node.${hud.kind.name}.name"), color = RiftColors.Rift, style = MaterialTheme.typography.labelLarge)
            Text(ui("rift.wave", hud.wave, hud.waves), color = RiftColors.Text, style = MaterialTheme.typography.labelSmall)
            Text(clock(hud.seconds), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall)
            hud.trial?.let { trial ->
                Text(ui("rift.champion", loc("rift.trial.${trial.name}.name")), color = RiftColors.Hot, style = MaterialTheme.typography.labelSmall)
                Text(loc("rift.trial.${trial.name}.description"), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall)
                Text(ui(if (hud.met) "rift.champion_met" else "rift.champion_failed"), color = if (hud.met) RiftColors.Rift else RiftColors.Warn, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (hud.phase != TrialPhase.FIGHT) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = RiftColors.Rift) }
    }
}

/** Итог забега: счёт, место, гибель или полная победа, сундук. */
@Composable private fun RiftResultSheet(game: GameUi, model: ExpeditionViewModel, result: com.sperance.exileforge.rules.rift.RiftResult) {
    ForgeSheet(onDismissRequest = model::dismissRiftResult) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("rift.result_title"), color = RiftColors.Rift, style = MaterialTheme.typography.titleLarge)
            Text(
                ui(
                    if (result.fallen) {
                        "rift.result_fallen"
                    } else if (result.cleared) {
                        "rift.result_cleared"
                    } else {
                        "rift.result_left"
                    },
                ),
                color = if (result.fallen) LifeRed else RiftColors.Soft,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(ui("rift.result_score", result.score), color = RiftColors.Hot, style = MaterialTheme.typography.titleMedium)
            if (result.place > 0) Text(ui("rift.result_place", result.place), color = RiftColors.Text, style = MaterialTheme.typography.bodyMedium)
            RewardLines(game, result.reward)
            ForgeButton(onClick = model::dismissRiftResult, modifier = Modifier.fillMaxWidth()) { Text(ui("rift.back_board")) }
        }
    }
}

/** Таблица испытаний лиги: первые места и место героя. */
@Composable internal fun TrialTableSheet(table: com.sperance.exileforge.rules.content.TrialTable, onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(ui("trials.table_title.${table.board.name}"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            if (table.place > 0) Text(ui("rift.table_you", table.place, table.size), color = Vital, style = MaterialTheme.typography.bodySmall)
            if (table.top.isEmpty()) MutedText(ui("rift.table_empty"))
            table.top.forEachIndexed { i, entry ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${i + 1}.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(28.dp))
                    Text("${entry.name} · ${entry.level}", color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(if (table.board == TrialBoard.RUSH) clock(entry.value.toDouble()) else entry.value.toString(), color = GoldBright, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
