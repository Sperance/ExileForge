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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.sperance.exileforge.rules.content.RiftRules
import com.sperance.exileforge.rules.content.TraitLine
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
    val LifeLow = Color(0xFFB3263B)
    val LifeHigh = Color(0xFFFF4D62)
}

/** Цвет редкости дара. */
internal fun RiftRarity.color(): Color = when (this) {
    RiftRarity.COMMON -> RiftColors.Soft
    RiftRarity.RARE -> RiftColors.Hot
    RiftRarity.LEGENDARY -> RiftColors.Chaos
}

/** Цвет вида узла на схеме. */
internal fun RiftNodeKind.color(): Color = when (this) {
    RiftNodeKind.FIGHT -> RiftColors.Soft
    RiftNodeKind.ELITE, RiftNodeKind.CHAMPION -> RiftColors.Hot
    RiftNodeKind.GUARDIAN -> RiftColors.Warn
    RiftNodeKind.ALTAR -> RiftColors.Chaos
    RiftNodeKind.REST, RiftNodeKind.TREASURE, RiftNodeKind.FORGE -> RiftColors.Rift
}

/**
 * Разлом недели (3.96.0) на весь экран. До забега - входное меню «Врата» (4.0.1): портал, неделя, стражи, мутаторы и вход; во
 * время забега - карта «Восхождение» с тонким HUD, предложение узла всплывает снизу. Пока идёт бой, экран - арена; назад
 * закрывает Разлом, забег ждёт на сервере до конца недели.
 */
@Composable fun RiftScreen() {
    val model = koinViewModel<ExpeditionViewModel>()
    val game by model.game.collectAsStateWithLifecycle()
    val state by model.riftState.collectAsStateWithLifecycle()
    val arena = state.arena
    if (arena != null) return RiftFight(game, model, arena)
    val index = game.index ?: return
    val rules = index.campaign.trials?.rift ?: return
    Box(Modifier.fillMaxSize().background(RiftColors.Ground)) {
        val board = state.board
        if (board == null) {
            BackHandler { model.closeRift() }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = RiftColors.Rift) }
        } else {
            val engine = remember(board.plan) { RiftEngine(rules, index.campaign, board.plan, index.rules.fight) }
            val run = board.progress.run
            if (run == null) RiftGate(game, model, rules, board, engine) else RiftRunView(model, rules, board, engine, run, idle = !game.busy)
        }
        state.result?.let { RiftResultSheet(game, model, it) }
    }
}

/** Входное меню «Врата»: портал, неделя и лига, стражи силуэтами, мутаторы, счёт, вход и схема недели в тумане. */
@Composable private fun RiftGate(game: GameUi, model: ExpeditionViewModel, rules: RiftRules, board: RiftBoard, engine: RiftEngine) {
    val hero = game.hero ?: return
    val idle = !game.busy
    var preview by remember { mutableStateOf(false) }
    BackHandler { if (preview) preview = false else model.closeRift() }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Text(ui("rift.title").uppercase(), color = RiftColors.Rift, style = relicName(19), modifier = Modifier.align(Alignment.Center))
            ForgeTextButton(onClick = model::closeRift, modifier = Modifier.align(Alignment.CenterEnd)) { Text(ui("rift.close"), color = RiftColors.Muted) }
        }
        val league = board.league
        val left = ((board.endsAt - System.currentTimeMillis()) / 1000.0).coerceAtLeast(0.0)
        Text(
            listOf(ui("rift.ends", clock(left)), league?.let { ui("rift.league", rules.leagues[it]) } ?: ui("rift.league_none", rules.leagues.first())).joinToString(" · "),
            color = RiftColors.Muted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        RiftPortal(Modifier.size(250.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            rules.guardians.monsters.forEachIndexed { act, code -> GuardianSeal(loc("monster.${code.value}.name"), act) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            board.plan.mutators.mapNotNull(rules.mutatorsByCode::get).forEach { mutator ->
                val tint = if (mutator.good) RiftColors.Rift else RiftColors.Warn
                val shape = RoundedCornerShape(10.dp)
                Column(Modifier.weight(1f).background(RiftColors.Panel, shape).border(1.dp, tint.copy(alpha = .35f), shape).padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Text(ui(if (mutator.good) "rift.mutator_good" else "rift.mutator_evil").uppercase(), color = tint, style = relicName(10))
                    Text(loc("rift.mutator.${mutator.code}.name"), color = RiftColors.Text, style = MaterialTheme.typography.bodyMedium)
                    Text(loc("rift.mutator.${mutator.code}.description"), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(ui("rift.free", board.free, rules.free), color = RiftColors.Text, style = MaterialTheme.typography.bodySmall)
            if (board.progress.best > 0) Text(ui("rift.best", board.progress.best), color = RiftColors.Hot, style = MaterialTheme.typography.bodySmall)
        }
        // Причина у неактивной кнопки: уровень лиги, ключ сверх бесплатных
        val keys = hero.bag[RiftRules.KEY] ?: 0L
        val reason = when {
            league == null -> ui("rift.league_none", rules.leagues.first())
            board.free <= 0 && keys < 1 -> ui("rift.start_locked")
            else -> null
        }
        ForgeButton(onClick = model::startRift, enabled = idle && reason == null, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text((if (board.free > 0) ui("rift.start") else ui("rift.start_key", itemTitle(RiftRules.KEY))).uppercase(), style = relicName(15))
        }
        reason?.let { Text(it, color = RiftColors.Warn, style = MaterialTheme.typography.labelSmall) }
        ForgeOutlinedButton(onClick = { preview = true }, modifier = Modifier.fillMaxWidth()) { Text(ui("rift.preview")) }
    }
    if (preview) {
        var looking by remember { mutableStateOf<RiftNode?>(null) }
        Box(Modifier.fillMaxSize().background(RiftColors.Ground)) {
            RiftAscent(rules, engine, null, idle = false, top = 80.dp, bottom = 40.dp) { looking = it }
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(ui("rift.preview").uppercase(), color = RiftColors.Rift, style = relicName(16), modifier = Modifier.weight(1f))
                ForgeTextButton(onClick = { preview = false }) { Text(ui("rift.close"), color = RiftColors.Muted) }
            }
        }
        looking?.let { node -> NodeSheet(rules, engine, null, node, idle = false, onDismiss = { looking = null }) {} }
    }
}

/**
 * Портал Врат: ореол, кольцо рун-засечек (40 с), встречное пунктирное кольцо (18 с), дышащее ядро (5,5 с) и всплывающие искры
 * (6 с). Декор стоит при выключенном `LocalMotion`.
 */
@Composable private fun RiftPortal(modifier: Modifier) {
    val spin = motionClock(40_000, "rift-gate-spin")
    val counter = motionClock(18_000, "rift-gate-counter")
    val breath = (1 - kotlin.math.cos(motionClock(5_500, "rift-gate-breath") * 2 * Math.PI).toFloat()) / 2
    val rise = motionClock(6_000, "rift-gate-sparks")
    Canvas(modifier) {
        val r = size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(RiftColors.Rift.copy(alpha = .4f), Color.Transparent), center, r), r)
        rotate(spin * 360f) {
            repeat(RUNES) { k ->
                val a = k * 2 * Math.PI / RUNES
                val long = if (k % 3 == 0) 12.dp.toPx() else 6.dp.toPx()
                val from = r * .78f
                drawLine(
                    RiftColors.LineHi,
                    Offset(center.x + (kotlin.math.cos(a) * from).toFloat(), center.y + (kotlin.math.sin(a) * from).toFloat()),
                    Offset(center.x + (kotlin.math.cos(a) * (from + long)).toFloat(), center.y + (kotlin.math.sin(a) * (from + long)).toFloat()),
                    1.6.dp.toPx(),
                )
            }
        }
        rotate(-counter * 360f) {
            drawCircle(RiftColors.Rift.copy(alpha = .7f), r * .62f, style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 9.dp.toPx()))))
        }
        val core = r * .48f * (1 + .06f * breath)
        drawCircle(Brush.radialGradient(listOf(RiftColors.Hot, RiftColors.Rift, RiftColors.Deep, Color.Transparent), center, core), core)
        drawCircle(RiftColors.Soft.copy(alpha = .6f), r * .5f, style = Stroke(1.5.dp.toPx()))
        repeat(SPARKS) { k ->
            val t = (rise + k.toFloat() / SPARKS) % 1f
            val x = center.x + ((k * 53 % 100) / 100f - .5f) * r * 1.3f
            drawCircle(RiftColors.Soft, (1 + k % 3 * .6f).dp.toPx(), Offset(x, center.y + r * .9f - t * r * 1.7f), alpha = (1 - t) * .9f)
        }
    }
}

/** Страж недели силуэтом: знак в кольце тревоги дышит (5,5 с), под ним имя заглавными. */
@Composable private fun GuardianSeal(name: String, act: Int) {
    val breath = (1 - kotlin.math.cos((motionClock(5_500, "rift-seal") + act * .15f) * 2 * Math.PI).toFloat()) / 2
    Column(Modifier.width(100.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(52.dp).glow(RiftColors.Warn.copy(alpha = .25f), radius = 8.dp, shape = CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF2A0D08), RiftColors.Ground)), CircleShape).border(1.dp, RiftColors.Warn.copy(alpha = .55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(RiftNodeKind.GUARDIAN.glyph(), null, tint = RiftColors.Warn.copy(alpha = .6f + .4f * breath), modifier = Modifier.size(28.dp))
        }
        Text(name.uppercase(), color = RiftColors.Muted, style = relicName(9), textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** Что открыто поверх карты забега: дары, проклятия или меню забега. */
private enum class RunSheet { BOONS, CURSES, MENU }

/** Забег: карта на весь экран, HUD сверху, предложение узла всплывает снизу. */
@Composable private fun RiftRunView(model: ExpeditionViewModel, rules: RiftRules, board: RiftBoard, engine: RiftEngine, run: RiftRun, idle: Boolean) {
    var looking by remember { mutableStateOf<RiftNode?>(null) }
    var sheet by remember { mutableStateOf<RunSheet?>(null) }
    BackHandler { model.closeRift() }
    val offer = run.offer.takeIf { run.stage == RiftStage.OFFER }
    Box(Modifier.fillMaxSize()) {
        RiftAscent(rules, engine, run, idle, top = 120.dp, bottom = if (offer != null) 360.dp else 48.dp) { looking = it }
        RiftHud(rules, engine, run, Modifier.align(Alignment.TopCenter)) { sheet = it }
        androidx.compose.animation.AnimatedVisibility(
            visible = offer != null,
            enter = androidx.compose.animation.slideInVertically { it },
            exit = androidx.compose.animation.slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp).heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
            ) { offer?.let { OfferPlate(rules, engine, run, it, idle, model::riftAct) } }
        }
    }
    looking?.let { node ->
        NodeSheet(rules, engine, run, node, idle, onDismiss = { looking = null }) {
            looking = null
            model.riftAct(RiftAct.Move(node.id))
        }
    }
    when (sheet) {
        RunSheet.BOONS -> ForgeSheet(onDismissRequest = { sheet = null }) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui("rift.boons", run.boons.size, rules.nodes.slots), color = RiftColors.Soft, style = MaterialTheme.typography.titleMedium)
                run.boons.forEach { held -> BoonLine(rules, held.code, held.grade) }
            }
        }

        RunSheet.CURSES -> ForgeSheet(onDismissRequest = { sheet = null }) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui("rift.curses"), color = RiftColors.Warn, style = MaterialTheme.typography.titleMedium)
                if (run.curses.isEmpty()) Text(ui("rift.no_curses"), color = RiftColors.Dim, style = MaterialTheme.typography.bodySmall)
                run.curses.forEach { curse -> CurseLine(curse.code, curse.altar) }
            }
        }

        RunSheet.MENU -> RunMenu(model, rules, board, idle, onDismiss = { sheet = null })

        null -> Unit
    }
}

/** HUD забега: счёт, Эхо и жизнь; чипы даров и проклятий открывают шторки, обзор - сколько рядов видно; ☰ - меню забега. */
@Composable private fun RiftHud(rules: RiftRules, engine: RiftEngine, run: RiftRun, modifier: Modifier, onSheet: (RunSheet) -> Unit) {
    Column(
        modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(RiftColors.Ground.copy(alpha = .96f), Color.Transparent)))
            .statusBarsPadding().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RiftChip(ui("rift.score", engine.score(run)), RiftColors.Hot)
            RiftChip(ui("rift.echo", run.echo), RiftColors.Rift)
            LifeBar(run.life.toFloat(), Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RiftChip(ui("rift.boons", run.boons.size, rules.nodes.slots), RiftColors.Soft) { onSheet(RunSheet.BOONS) }
            RiftChip(ui("rift.curses_n", run.curses.size), RiftColors.Warn) { onSheet(RunSheet.CURSES) }
            RiftChip(ui("rift.sight", engine.sight(run)), RiftColors.Muted)
            Spacer(Modifier.weight(1f))
            RiftChip("☰", RiftColors.Muted) { onSheet(RunSheet.MENU) }
        }
    }
}

/** Чип HUD: подпись в капсуле Разлома; с [onClick] - нажимается. */
@Composable private fun RiftChip(text: String, tint: Color, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        text,
        color = tint,
        style = relicName(12),
        maxLines = 1,
        modifier = Modifier.background(RiftColors.Panel.copy(alpha = .85f), shape).border(1.dp, RiftColors.LineHi, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

/** Жизнь героя в забеге: багровая полоса с процентом. */
@Composable private fun LifeBar(life: Float, modifier: Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Box(modifier.height(14.dp).background(Color(0xFF1A0B0F), shape).border(1.dp, Color(0xFF3A1820), shape)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(life.coerceIn(0f, 1f)).background(Brush.horizontalGradient(listOf(RiftColors.LifeLow, RiftColors.LifeHigh)), shape))
        Text(ui("rift.life", (life * 100).toInt()), color = RiftColors.Text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Center))
    }
}

/** Меню забега: мутаторы недели, выход из забега. */
@Composable private fun RunMenu(model: ExpeditionViewModel, rules: RiftRules, board: RiftBoard, idle: Boolean, onDismiss: () -> Unit) {
    var ending by remember { mutableStateOf(false) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("rift.title"), color = RiftColors.Rift, style = MaterialTheme.typography.titleMedium)
            val left = ((board.endsAt - System.currentTimeMillis()) / 1000.0).coerceAtLeast(0.0)
            Text(ui("rift.ends", clock(left)), color = RiftColors.Muted, style = MaterialTheme.typography.labelMedium)
            Text(ui("rift.mutators"), color = RiftColors.Muted, style = MaterialTheme.typography.labelMedium)
            board.plan.mutators.mapNotNull(rules.mutatorsByCode::get).forEach { mutator ->
                Text(
                    "${ui(if (mutator.good) "rift.mutator_good" else "rift.mutator_evil")}: ${loc("rift.mutator.${mutator.code}.name")} - ${loc("rift.mutator.${mutator.code}.description")}",
                    color = if (mutator.good) RiftColors.Soft else RiftColors.Warn,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            ForgeOutlinedButton(onClick = { ending = true }, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(ui("rift.end"), color = RiftColors.Warn) }
            ForgeTextButton(onClick = model::closeRift, modifier = Modifier.fillMaxWidth()) { Text(ui("rift.close"), color = RiftColors.Muted) }
        }
    }
    if (ending) {
        ConfirmSheet(ui("rift.end"), ui("rift.end"), onDismiss = { ending = false }, note = ui("rift.end_confirm"), danger = true) {
            ending = false
            onDismiss()
            model.riftAct(RiftAct.End)
        }
    }
}

/** Дар одной строкой: имя цветом редкости, ступень и его строки листа; дар правил (4.0.1) - своим описанием. */
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
        if (boon.rules.isNotEmpty()) Text(loc("rift.boon.$code.description"), color = RiftColors.Text, style = MaterialTheme.typography.labelSmall)
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
 * Почему на узел [node] не шагнуть (4.0.1) - ключ словаря; null - можно идти. До забега - сперва войти; пройденный - уже был;
 * пока открыт выбор или идёт бой - сперва его закончить; дальше следующего ряда - дорога приведёт позже; иначе тропа позади.
 */
private fun RiftEngine.blocked(run: RiftRun?, node: RiftNode): String? {
    val here = run?.at?.let(plan::node)
    return when {
        run == null -> "rift.node_why.no_run"
        node.id in run.path -> "rift.node_why.walked"
        run.stage != RiftStage.MOVE -> "rift.node_why.busy"
        node.id in next(run) -> null
        depth(node) > (here?.let(::depth) ?: -1) + 1 -> "rift.node_why.later"
        else -> "rift.node_why.behind"
    }
}

/** Узел: вид и его суть, проклятие, свойства Вождя, условие Чемпиона (скрыто до боя), страж акта; недоступный - с причиной. */
@Composable private fun NodeSheet(rules: RiftRules, engine: RiftEngine, run: RiftRun?, node: RiftNode, idle: Boolean, onDismiss: () -> Unit, onGo: () -> Unit) {
    val seen = engine.seen(run, node)
    val lines = buildList {
        add(loc("rift.node.${node.kind.name}.description"))
        if (node.kind == RiftNodeKind.GUARDIAN) rules.guardians.monsters.getOrNull(node.act)?.let { add(loc("monster.${it.value}.name") + ": " + loc("rift.guardian.${it.value}.description")) }
        if (seen) {
            node.curse?.let { add(ui("rift.node_curse", loc("rift.curse.$it.name") + " - " + loc("rift.curse.$it.description"))) }
            if (node.traits.isNotEmpty()) add(ui("rift.node_traits", node.traits.joinToString(", ") { loc("trait.$it.name") }))
            if (node.trial != null) add(ui("rift.node_trial"))
        }
    }
    val why = engine.blocked(run, node)
    ConfirmSheet(
        loc("rift.node.${node.kind.name}.name"),
        ui("rift.node_go"),
        onDismiss = onDismiss,
        note = lines.joinToString("\n"),
        blocked = why != null || !idle,
        warning = why?.let { ui(it) },
    ) { onGo() }
}

private const val RUNES = 24
private const val SPARKS = 14

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
        // Досье стража перед боем, с живым портретом: «В бой» начинает бой
        val fight = hud.fight?.takeIf { hud.phase == TrialPhase.FIGHT && !it.started }
        val boss = fight?.boss
        var seen by remember(arena) { mutableStateOf(false) }
        if (fight != null && boss != null && !seen && hud.kind == RiftNodeKind.GUARDIAN) {
            fight.foes.firstOrNull { it.index == boss.index }?.let { foe ->
                BossDossier(
                    game, foe.monster, hud.level, foe.maxLife.toDouble(), boss.phase, boss.marks, loc("rift.node.${hud.kind.name}.name"),
                    fight.heroBody, arena.combat, odds = { null }, record = { model.bossRecord(foe.monster.code.value) },
                ) {
                    seen = true
                    model.riftCommand(RunCommand.Begin)
                }
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
