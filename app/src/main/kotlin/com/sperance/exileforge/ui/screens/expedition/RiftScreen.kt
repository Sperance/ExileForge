package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.Feature
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.lockedText
import com.sperance.exileforge.core.app.unlocked
import com.sperance.exileforge.core.campaign.RiftArena
import com.sperance.exileforge.core.campaign.TrialPhase
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.timeLeft
import com.sperance.exileforge.core.expedition.RiftChart
import com.sperance.exileforge.core.expedition.RiftRecap
import com.sperance.exileforge.core.expedition.RiftSpot
import com.sperance.exileforge.core.expedition.RiftState
import com.sperance.exileforge.core.expedition.RiftStep
import com.sperance.exileforge.core.expedition.RiftToll
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.CallState
import com.sperance.exileforge.rules.content.RiftEntry
import com.sperance.exileforge.rules.content.RiftLaw
import com.sperance.exileforge.rules.content.RiftMetal
import com.sperance.exileforge.rules.content.RiftMutator
import com.sperance.exileforge.rules.content.RiftNodeKind
import com.sperance.exileforge.rules.content.RiftRarity
import com.sperance.exileforge.rules.content.RiftReward
import com.sperance.exileforge.rules.content.RiftRule
import com.sperance.exileforge.rules.content.RiftRules
import com.sperance.exileforge.rules.rift.RiftAct
import com.sperance.exileforge.rules.rift.RiftBoard
import com.sperance.exileforge.rules.rift.RiftEngine
import com.sperance.exileforge.rules.rift.RiftNode
import com.sperance.exileforge.rules.rift.RiftOffer
import com.sperance.exileforge.rules.rift.RiftRest
import com.sperance.exileforge.rules.rift.RiftRun
import com.sperance.exileforge.rules.rift.RiftStage
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.RunMenu
import com.sperance.exileforge.ui.components.RunMenuItem
import com.sperance.exileforge.ui.screens.expedition.arena.ArenaOverlay
import com.sperance.exileforge.ui.screens.expedition.arena.FightLogSheet
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

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

    /** Честь забега - бирюза: полоса, её глубина у начала и тлеющий край потери боя. */
    val Honor = Color(0xFF6FE3F2)
    val HonorDeep = Color(0xFF0E3A44)
    val HonorWell = Color(0xFF0B1A1D)
    val HonorWell2 = Color(0xFF071012)
    val HonorRim = Color(0xFF2A5560)
    val Ember = Color(0xFFFFB4A0)

    /** Металлы ключей и сундуков Владыки; золото - Предначертания. */
    val Bronze = Color(0xFFD0925A)
    val Silver = Color(0xFFC9D6DE)

    /** Выделенный вариант узла - золото Предначертания: рамка и знак выбора. */
    val Pick = FateInk.Gold
    val OnPick = FateInk.OnGold
}

/** Цвет редкости дара. */
internal fun RiftRarity.color(): Color = when (this) {
    RiftRarity.COMMON -> RiftColors.Soft
    RiftRarity.RARE -> RiftColors.Hot
    RiftRarity.LEGENDARY -> RiftColors.Chaos
}

/** Цвет металла ключа и сундука. */
internal fun RiftMetal.color(): Color = when (this) {
    RiftMetal.BRONZE -> RiftColors.Bronze
    RiftMetal.SILVER -> RiftColors.Silver
    RiftMetal.GOLD -> RiftColors.Pick
}

/** Цвет вида узла на схеме. */
internal fun RiftNodeKind.color(): Color = when (this) {
    RiftNodeKind.FIGHT -> RiftColors.Soft
    RiftNodeKind.ELITE, RiftNodeKind.CHAMPION -> RiftColors.Hot
    RiftNodeKind.GUARDIAN -> RiftColors.Warn
    RiftNodeKind.ALTAR -> RiftColors.Chaos
    RiftNodeKind.REST -> RiftColors.Honor
    RiftNodeKind.TREASURE -> RiftColors.Pick
    RiftNodeKind.FORGE -> RiftColors.Bronze
}

/**
 * Разлом недели на весь экран. До забега - входное меню «Врата»: портал, неделя, стражи, мутаторы и вход; во
 * время забега - карта «Восхождение» с тонким HUD, предложение узла всплывает снизу. Пока идёт бой, экран - арена. «Назад» на Вратах
 * закрывает Разлом, в забеге - спрашивает «Покинуть Разлом?» (удержанием): закрыть экран, не кончив забег, нельзя.
 */
@Composable fun RiftScreen() {
    val model = koinViewModel<ExpeditionViewModel>()
    val game by model.game.collectAsStateWithLifecycle()
    val state by model.riftState.collectAsStateWithLifecycle()
    val step by model.riftStep.collectAsStateWithLifecycle()
    val canAct by model.riftCanAct.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalFightReport provides state.lastFight) { RiftBody(game, model, state, step, canAct) }
}

@Composable private fun RiftBody(game: GameUi, model: ExpeditionViewModel, state: RiftState, step: CallState<RiftStep>, canAct: Boolean) {
    val arena = state.arena
    if (arena != null) return RiftFight(game, model, arena, state.deliveryRefused)
    val index = game.index ?: return
    val rules = index.campaign.trials?.rift ?: return
    Box(Modifier.fillMaxSize().background(RiftColors.Ground)) {
        val board = state.board
        val chart = state.chart
        if (board == null || chart == null) {
            BackHandler { model.closeRift() }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = RiftColors.Rift) }
        } else {
            val engine = remember(board.plan) { RiftEngine(rules, index.campaign, board.plan, index.rules.fight) }
            val run = board.progress.run
            if (run == null) RiftGate(game, model, rules, board, engine, chart, step, canAct) else RiftRunView(game, model, rules, board, engine, chart, run, state, step, idle = canAct)
        }
        val recap = state.recap?.takeIf { it.shown }
        if (recap != null) RiftRecapSheet(game, recap, model::closeRiftRecap) else state.result?.let { RiftResultSheet(game, model, it) }
    }
}

/** Краски цепочки «назад» внутри Разлома. */
private val RiftTrail = TrailColors(arrow = RiftColors.Rift, link = RiftColors.Muted, here = RiftColors.Text, data = RiftColors.Soft)

/**
 * Входное меню «Врата» - уровень цепочки «← Испытания › Разлом недели»: срок недели, уровень Разлома (закреплённый первым забегом
 * недели) и бесплатные забеги - данными цепочки ([GateTrail]). Портал и рядом лучший счёт недели, жила мутаторов и стражей
 * ([GateVein]), вход ([GateEnter]) и «Схема недели» - следующий уровень цепочки ([WeekScheme]). Вход и схема закреплены внизу,
 * над ними листается жила; вход в пути - полоса шага ([RequestStrip]) поверх верха Врат.
 */
@Composable private fun RiftGate(game: GameUi, model: ExpeditionViewModel, rules: RiftRules, board: RiftBoard, engine: RiftEngine, chart: RiftChart, step: CallState<RiftStep>, canAct: Boolean) {
    val hero = game.hero ?: return
    var preview by remember { mutableStateOf(false) }
    BackTrailHost(ui("trials.title"), Modifier.statusBarsPadding(), RiftTrail) {
        TrailLevel(ui("rift.title"), onBack = model::closeRift) {
            if (preview) {
                TrailLevel(ui("rift.preview"), onBack = { preview = false }) { WeekScheme(rules, engine, chart) }
            } else {
                GateTrail(rules, board, hero.level)
                Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        GateHead(board.progress.best)
                        GateVein(rules, board.plan.mutators.mapNotNull(rules.mutatorsByCode::get))
                    }
                    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GateEnter(game, rules, board, hero.bag[RiftRules.KEY] ?: 0L, step, canAct, model::startRift)
                        ForgeButton(onClick = { preview = true }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, accent = RiftColors.Soft) { Text(ui("rift.preview")) }
                    }
                }
            }
        }
        val strip = remember(step) { step.strip(ui("call.offline_saved")) }
        RequestStrip(strip, RiftStripTones, onRetry = model::retryRiftStep)
    }
}

/**
 * Данные цепочки Врат столбиком: срок недели над уровнем Разлома и бесплатными забегами; касание - те же числа с подписями и правила
 * Разлома. Срок - по часам сервера, тикает живыми часами экрана.
 */
@Composable private fun GateTrail(rules: RiftRules, board: RiftBoard, heroLevel: Int) {
    val left = weekLeft(board.endsAt)
    val level = board.progress.level.takeIf { it > 0 } ?: heroLevel
    TrailData(
        ui("rift.trail", left, level, board.free, rules.free),
        Tip(
            ui("rift.title"),
            ui("rift.hint", rules.acts, rules.layout.rows, rules.free),
            RiftColors.Soft,
            listOf(ui("rift.ends") to left, ui("rift.level") to "$level", ui("rift.free") to ui("rift.free_of", board.free, rules.free)),
            RiftColors.Soft,
        ),
    )
}

/** Сколько осталось до конца недели [endsAt] (мс эпохи сервера): «3 д 14 ч», в последние сутки - «14 ч 05 мин». */
@Composable private fun weekLeft(endsAt: Long): String = timeLeft(rememberServerLeft(endsAt, WEEK_TICK_MS))

/** Такт часов срока недели. */
private const val WEEK_TICK_MS = 15_000L

/** Портал 128 dp и рядом крупно лучший счёт недели; забегов ещё не было - прочерк. */
@Composable private fun GateHead(best: Long) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RiftPortal(Modifier.offset(x = (-8).dp).size(128.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(ui("rift.best"), color = RiftColors.Muted, style = MaterialTheme.typography.bodySmall)
            Text(if (best > 0) number(best.toDouble()) else "—", color = RiftColors.Hot, style = relicName(26).copy(fontFeatureSettings = "tnum"))
        }
    }
}

/**
 * Жила Врат сверху вниз, как путь на схеме: мутаторы недели - узел ▲ (полезный) или ▼ (вредный) знаком и цветом, имя и описание, - затем
 * стражи по актам. Ярлыков «полезный/вредный» нет - их говорит узел.
 */
@Composable private fun GateVein(rules: RiftRules, mutators: List<RiftMutator>) {
    val guardians = rules.guardians.monsters
    val last = mutators.size + guardians.size - 1
    Column {
        mutators.forEachIndexed { i, mutator ->
            VeinStep(first = i == 0, last = i == last, node = { MutatorNode(mutator.good) }) { MutatorText(mutator, Modifier.padding(top = 2.dp, bottom = 14.dp)) }
        }
        guardians.forEachIndexed { act, code ->
            val k = mutators.size + act
            VeinStep(first = k == 0, last = k == last, node = { GuardianNode(act) }) {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(loc("monster.${code.value}.name"), color = RiftColors.Text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(ui("rift.act", act + 1), color = RiftColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * Шаг жилы: столбец [VEIN_SIDE] с чертой жилы и узлом [node] наверху, справа - [content]. У первого шага черта начинается под узлом,
 * у последнего - кончается на нём.
 */
@Composable private fun VeinStep(first: Boolean, last: Boolean, node: @Composable () -> Unit, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.width(VEIN_SIDE).fillMaxHeight().drawBehind {
                val x = size.width / 2
                val from = if (first) VEIN_START.toPx() else 0f
                val to = if (last) VEIN_END.toPx() else size.height
                drawLine(RiftColors.LineHi, Offset(x, from), Offset(x, to), 2.dp.toPx())
            },
            contentAlignment = Alignment.TopCenter,
        ) { node() }
        Box(Modifier.weight(1f)) { content() }
    }
}

/** Ширина столбца жилы. */
private val VEIN_SIDE = 28.dp

/** Где начинается черта у первого шага жилы. */
private val VEIN_START = 12.dp

/** Где кончается черта у последнего шага жилы. */
private val VEIN_END = 14.dp

/** Узел мутатора на жиле: кружок 24 dp с [MutatorSign] в рамке и свечении его цвета. */
@Composable private fun MutatorNode(good: Boolean) {
    val tint = mutatorTint(good)
    Box(
        Modifier.size(24.dp).glow(tint.copy(alpha = .33f), radius = 4.dp, shape = CircleShape).background(RiftColors.Ground, CircleShape).border(1.5.dp, tint, CircleShape),
        contentAlignment = Alignment.Center,
    ) { MutatorSign(good) }
}

/** Знак мутатора: ▲ полезный, ▼ вредный - знаком и цветом; читалке - словом. */
@Composable private fun MutatorSign(good: Boolean) {
    val word = ui(if (good) "rift.mutator_good" else "rift.mutator_evil")
    Text(if (good) "▲" else "▼", color = mutatorTint(good), style = MaterialTheme.typography.labelSmall, modifier = Modifier.semantics { contentDescription = word })
}

/** Цвет мутатора: полезный - Разлом, вредный - тревога. */
private fun mutatorTint(good: Boolean): Color = if (good) RiftColors.Rift else RiftColors.Warn

/** Имя мутатора его цветом и описание под ним - одни строки для жилы Врат и меню забега. */
@Composable private fun MutatorText(mutator: RiftMutator, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(loc("rift.mutator.${mutator.code}.name"), color = if (mutator.good) RiftColors.Soft else RiftColors.Warn, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(loc("rift.mutator.${mutator.code}.description"), color = RiftColors.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Узел стража акта [act] на жиле: знак стража в кольце тревоги дышит (5,5 с, со сдвигом по акту).
 *
 * Дыхание - прозрачностью слоя знака в рисовании, без перекомпоновки.
 */
@Composable private fun GuardianNode(act: Int) {
    val phase = motionPhase(5_500)
    Box(
        Modifier.size(28.dp).background(Brush.radialGradient(listOf(Color(0xFF2A0D08), RiftColors.Ground)), CircleShape).border(1.dp, RiftColors.Warn.copy(alpha = .6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            RiftNodeKind.GUARDIAN.glyph(),
            null,
            tint = RiftColors.Warn,
            modifier = Modifier.size(16.dp).graphicsLayer {
                val breath = (1 - kotlin.math.cos((phase.value + act * .15f) * 2 * Math.PI).toFloat()) / 2
                alpha = .6f + .4f * breath
            },
        )
    }
}

/**
 * Вход в Разлом с числом в подписи: «Войти · бесплатно 2/3», после бесплатных - «Войти за ключ · есть 4» ([keys] - ключи в сумке),
 * со свободным прогрессом тестировщика - «Войти · без ключа» ([RiftRules.entry] - то же правило, что у сервера). Закрытый уровнем Разлом или ни бесплатных, ни ключа - кнопка погашена, касание говорит причину (у ключа - и откуда он берётся,
 * описанием предмета).
 */
@Composable private fun GateEnter(game: GameUi, rules: RiftRules, board: RiftBoard, keys: Long, step: CallState<RiftStep>, canAct: Boolean, onEnter: () -> Unit) {
    val entry = rules.entry(board.progress.runs, game.freeProgress)
    val why = when {
        !game.unlocked(Feature.RIFT) -> listOf(Feature.RIFT.lockedText(game.index?.rules))
        entry == RiftEntry.KEY && keys < 1 -> listOf(ui("rift.start_locked"), itemDescription(RiftRules.KEY, game.index)).filter(String::isNotBlank)
        else -> null
    }
    var explain by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        ForgeButton(
            onClick = onEnter,
            modifier = Modifier.fillMaxWidth(),
            size = ButtonSize.Wide,
            state = ButtonState.of(canAct && why == null, busy = step.spins(RiftStep.Start)),
            accent = RiftColors.Rift,
            onDisabledClick = why?.let { { explain = true } },
        ) {
            Text(ui(if (entry == RiftEntry.KEY) "rift.start_key" else "rift.start").uppercase(), style = relicName(15), maxLines = 1)
            Text(
                " · " + when (entry) {
                    RiftEntry.FREE -> ui("rift.start_free", board.free, rules.free)
                    RiftEntry.KEY -> ui("rift.start_keys", keys)
                    RiftEntry.UNBOUND -> ui("rift.start_unbound")
                },
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                modifier = Modifier.alpha(.8f),
            )
        }
        if (explain && why != null) {
            TipCallout(RiftColors.Warn, onClose = { explain = false }) {
                Text(why.first(), color = RiftColors.Text, style = MaterialTheme.typography.bodySmall)
                why.drop(1).forEach { Text(it, color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

/** Схема недели в тумане - уровень цепочки над Вратами: касание узла - его лист, без кнопки. */
@Composable private fun WeekScheme(rules: RiftRules, engine: RiftEngine, chart: RiftChart) {
    var looking by remember { mutableStateOf<RiftSpot?>(null) }
    RiftAscent(rules, engine, chart, walk = null, idle = false, top = 24.dp, bottom = 40.dp, hero = false) { looking = it }
    looking?.let { spot -> NodeSheet(rules, engine, spot, idle = false, onDismiss = { looking = null }) }
}

/**
 * Портал Врат: ореол, кольцо рун-засечек (40 с), встречное пунктирное кольцо (18 с), дышащее ядро (5,5 с) и всплывающие искры
 * (6 с). Декор стоит при выключенном `LocalMotion`.
 *
 * Фазы читаются в рисовании: портал перерисовывается без перекомпоновки экрана.
 */
@Composable private fun RiftPortal(modifier: Modifier) {
    val spinning = motionPhase(40_000)
    val countering = motionPhase(18_000)
    val breathing = motionPhase(5_500)
    val rising = motionPhase(6_000)
    Canvas(modifier) {
        val spin = spinning.value
        val counter = countering.value
        val breath = (1 - kotlin.math.cos(breathing.value * 2 * Math.PI).toFloat()) / 2
        val rise = rising.value
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

/** Что открыто поверх карты забега: дары, проклятия или меню забега. */
private enum class RunSheet { BOONS, CURSES }

/**
 * Забег: карта на весь экран, HUD сверху, под ним - полоса шага в пути ([RequestStrip]), предложение узла всплывает снизу; [state] - итог
 * последнего боя и открытые сундуки, [step] - шаг в пути. «Назад» и «Покинуть Разлом» меню - одно подтверждение удержанием.
 */
@Composable private fun RiftRunView(
    game: GameUi,
    model: ExpeditionViewModel,
    rules: RiftRules,
    board: RiftBoard,
    engine: RiftEngine,
    chart: RiftChart,
    run: RiftRun,
    state: RiftState,
    step: CallState<RiftStep>,
    idle: Boolean,
) {
    var looking by remember { mutableStateOf<Int?>(null) }
    var sheet by remember { mutableStateOf<RunSheet?>(null) }
    var leaving by remember { mutableStateOf(false) }
    BackHandler { leaving = true }
    val offer = run.offer.takeIf { run.stage == RiftStage.OFFER }
    Box(Modifier.fillMaxSize()) {
        RiftAscent(rules, engine, chart, state.walk, idle, top = 156.dp, bottom = if (offer != null) 360.dp else 48.dp) { looking = it.node.id }
        Column(Modifier.align(Alignment.TopCenter)) {
            RiftHud(model, rules, board, engine, run, state.toll, state.recap?.takeIf { !it.shown }, idle, onLeave = { leaving = true }) { sheet = it }
            val strip = remember(step) { step.strip(offlineNote(step)) }
            RequestStrip(strip, RiftStripTones, onRetry = model::retryRiftStep)
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = offer != null,
            enter = androidx.compose.animation.slideInVertically { it },
            exit = androidx.compose.animation.slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp).heightIn(max = 440.dp),
            ) { offer?.let { OfferPlate(game, rules, engine, run, it, state.coffers, idle, step, model::riftAct) } }
        }
    }
    if (leaving) {
        ConfirmSheet(ui("rift.end"), ui("rift.end_hold"), onDismiss = { leaving = false }, danger = true) {
            leaving = false
            model.leaveRift()
        }
    }
    looking?.let(chart::spot)?.let { spot ->
        NodeSheet(rules, engine, spot, idle, onDismiss = { looking = null }) {
            looking = null
            model.riftGo(spot.node.id)
        }
    }
    when (sheet) {
        RunSheet.BOONS -> ForgeSheet(onDismissRequest = { sheet = null }, containerColor = RiftColors.Panel) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui("rift.boons", run.boons.size, rules.nodes.slots), color = RiftColors.Soft, style = MaterialTheme.typography.titleMedium)
                run.boons.forEach { held -> BoonLine(rules, held.code, held.grade) }
            }
        }

        RunSheet.CURSES -> ForgeSheet(onDismissRequest = { sheet = null }, containerColor = RiftColors.Panel) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui("rift.curses"), color = RiftColors.Warn, style = MaterialTheme.typography.titleMedium)
                if (run.curses.isEmpty()) Text(ui("rift.no_curses"), color = RiftColors.Dim, style = MaterialTheme.typography.bodySmall)
                run.curses.forEach { curse -> CurseLine(curse.code, curse.altar) }
            }
        }

        null -> Unit
    }
}

/**
 * HUD забега: счёт, Эхо и ключи захода, под ними свиток Чести; итог последнего боя ([toll]) - «−N Чести» и взятый ключ под свитком,
 * сам гаснет; закрытый лист павшего стража ([recap]) - плашкой «Итог боя» до следующего боя. Чипы даров и проклятий открывают шторки,
 * обзор - сколько рядов видно; ☰ - меню забега.
 */
@Composable private fun RiftHud(
    model: ExpeditionViewModel,
    rules: RiftRules,
    board: RiftBoard,
    engine: RiftEngine,
    run: RiftRun,
    toll: RiftToll?,
    recap: RiftRecap?,
    idle: Boolean,
    onLeave: () -> Unit,
    onSheet: (RunSheet) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(RiftColors.Ground.copy(alpha = .96f), Color.Transparent)))
            .statusBarsPadding().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RiftChip(ui("rift.score", engine.score(run)), RiftColors.Hot)
            RiftChip(ui("rift.echo", run.echo), RiftColors.Rift)
            Spacer(Modifier.weight(1f))
            KeyCounts(run.keys)
        }
        HonorScroll(run.honor, engine.maxHonor(run), 0.0, Modifier.fillMaxWidth())
        toll?.let { TollLine(it, model::dismissRiftToll) }
        recap?.let { RiftRecapPlate(it, model::openRiftRecap) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RiftChip(ui("rift.boons", run.boons.size, rules.nodes.slots), RiftColors.Soft) { onSheet(RunSheet.BOONS) }
            RiftChip(ui("rift.curses_n", run.curses.size), RiftColors.Warn) { onSheet(RunSheet.CURSES) }
            RiftChip(ui("rift.sight", engine.sight(run)), RiftColors.Muted)
            Spacer(Modifier.weight(1f))
            RiftMenu(rules, board, onLeave)
        }
    }
}

/** Чип HUD: подпись в капсуле Разлома; с [onClick] - нажимается всей зоной касания 44 dp, рисуется только капсула. */
@Composable private fun RiftChip(text: String, tint: Color, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        text,
        color = tint,
        style = relicName(12),
        maxLines = 1,
        modifier = Modifier.then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick).touchZone() else Modifier)
            .background(RiftColors.Panel.copy(alpha = .85f), shape).border(1.dp, RiftColors.LineHi, shape).padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

/**
 * Свиток Чести: бирюзовая полоса [honor] из [max] с подписью; [loss] - сколько снимет идущий бой: эта доля полосы заштрихована, у её
 * края тлеет огонёк (декор стоит при выключенном `LocalMotion`), справа - «−N за бой». [spent] - потеря уже снята (итог боя): подпись -
 * остаток после неё, справа - «−N Чести».
 *
 * Тление читается в рисовании: свиток перерисовывается без перекомпоновки.
 */
@Composable internal fun HonorScroll(honor: Double, max: Double, loss: Double, modifier: Modifier = Modifier, spent: Boolean = false) {
    val ember = motionPhase(EMBER_MS)
    val top = max.coerceAtLeast(1.0)
    val now = honor.coerceIn(0.0, top)
    val after = (now - loss).coerceAtLeast(0.0)
    val toll = now.roundToInt() - after.roundToInt()
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier.height(22.dp).background(Brush.verticalGradient(listOf(RiftColors.HonorWell, RiftColors.HonorWell2)), shape).border(1.dp, RiftColors.HonorRim, shape)
            .clip(shape).drawBehind {
                val keep = (after / top).toFloat() * size.width
                val was = (now / top).toFloat() * size.width
                drawRect(Brush.horizontalGradient(listOf(RiftColors.HonorDeep, RiftColors.Honor), 0f, size.width), size = size.copy(width = keep))
                if (was > keep) {
                    clipRect(left = keep, right = was) {
                        val step = 6.dp.toPx()
                        var x = keep - size.height
                        while (x < was) {
                            drawLine(RiftColors.Warn.copy(alpha = .55f), Offset(x, size.height), Offset(x + size.height, 0f), 1.5.dp.toPx())
                            x += step
                        }
                    }
                    val glow = .55f + .45f * (1 - kotlin.math.cos(ember.value * 2 * Math.PI).toFloat()) / 2
                    drawLine(RiftColors.Ember.copy(alpha = glow), Offset(keep, 0f), Offset(keep, size.height), 2.dp.toPx())
                }
            }.semantics { contentDescription = ui("rift.honor", (if (spent) after else now).roundToInt(), top.roundToInt()) },
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(ui("rift.honor", (if (spent) after else now).roundToInt(), top.roundToInt()), color = RiftColors.Text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (toll > 0) Text(ui(if (spent) "rift.honor_toll" else "rift.honor_fight", toll), color = RiftColors.Ember, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Такт тления края потери на свитке Чести. */
private const val EMBER_MS = 1_600

/** Сколько держится итог боя под свитком Чести. */
private const val TOLL_MS = 3_000L

/** Итог боя под свитком: «−N Чести» тревогой и взятый ключ его металлом; гаснет сам через [TOLL_MS]. */
@Composable private fun TollLine(toll: RiftToll, onDone: () -> Unit) {
    LaunchedEffect(toll) {
        kotlinx.coroutines.delay(TOLL_MS)
        onDone()
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (toll.lost > 0) RiftChip(ui("rift.honor_toll", toll.lost), RiftColors.Ember)
        toll.key?.let { RiftChip(ui("rift.key", ui("rift.metal.${it.name}")), it.color()) }
    }
}

/** Ключи захода героя: три счётчика - знак металла и число; читалке - металл и число словами. */
@Composable internal fun KeyCounts(keys: Map<RiftMetal, Int>, burned: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        RiftMetal.entries.forEach { metal ->
            val count = keys[metal] ?: 0
            val name = ui("rift.metal.${metal.name}")
            Row(
                Modifier.semantics(mergeDescendants = true) { contentDescription = ui("rift.keys_count", name, count) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                KeyMark(metal, Modifier.size(10.dp).alpha(if (count > 0) 1f else .35f))
                Text(
                    "$count",
                    color = if (count > 0) metal.color() else RiftColors.Dim,
                    style = relicName(12).copy(textDecoration = if (burned) androidx.compose.ui.text.style.TextDecoration.LineThrough else null),
                )
            }
        }
    }
}

/** Знак ключа: кружок металла с тёмной каймой. */
@Composable internal fun KeyMark(metal: RiftMetal, modifier: Modifier = Modifier) {
    Box(modifier.background(metal.color(), CircleShape).border(1.dp, RiftColors.Ground, CircleShape))
}

/** Краски меню забега Разлома. */
private val RiftMenuPalette = MenuPalette(
    sheet = RiftColors.Panel,
    title = RiftColors.Soft,
    accent = RiftColors.Rift,
    onAccent = RiftColors.Ground,
    well = RiftColors.Ground2,
    text = RiftColors.Text,
    muted = RiftColors.Muted,
    danger = RiftColors.LifeHigh,
    line = RiftColors.Line,
)

/**
 * Меню забега Разлома (общее [RunMenu]) в палитре Разлома: срок недели у заголовка, мутаторы недели теми же строками, что на Вратах;
 * свой пункт - «Покинуть Разлом» ([onLeave]: подтверждение удержанием), под именем - что будет с забегом.
 */
@Composable private fun RiftMenu(rules: RiftRules, board: RiftBoard, onLeave: () -> Unit) {
    RunMenu(
        items = listOf(
            RunMenuItem(Icons.Outlined.Flag, ui("rift.end"), ui("rift.end_confirm"), danger = true, onClick = onLeave),
        ),
        hold = {},
        extra = {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                board.plan.mutators.mapNotNull(rules.mutatorsByCode::get).forEach { mutator ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MutatorSign(mutator.good)
                        MutatorText(mutator)
                    }
                }
            }
        },
        badge = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.HourglassEmpty, ui("rift.ends"), tint = RiftColors.Muted, modifier = Modifier.size(12.dp))
                Text(weekLeft(board.endsAt), color = RiftColors.Muted, style = MaterialTheme.typography.labelMedium)
            }
        },
        palette = RiftMenuPalette,
        trigger = { onClick -> RiftChip("☰", RiftColors.Muted, onClick) },
    )
}

/** Пояснение «нет связи» полосы шага: выход из Разлома - что он не дошёл, прочий шаг - что выбор сохранён. */
private fun offlineNote(step: CallState<RiftStep>): String = ui(if (step.tag == LEAVING) "rift.leave_offline" else "call.offline_saved")

/** Шаг «Покинуть Разлом». */
private val LEAVING = RiftStep.Act(RiftAct.End)

/** Краски полосы шага в палитре Разлома. */
private val RiftStripTones = StripTones(line = RiftColors.Rift, track = RiftColors.Line, restored = RiftColors.Soft, muted = RiftColors.Muted)

/** Имя дара [code], со ступенью [grade] - если она есть. */
private fun boonName(code: String, grade: Int): String = loc("rift.boon.$code.name") + if (grade > 0) " · ${ui("rift.grade", grade)}" else ""

/** Дар одной строкой: имя цветом редкости, ступень и его строки листа; дар правил - своим описанием. */
@Composable private fun BoonLine(rules: RiftRules, code: String, grade: Int = 0, modifier: Modifier = Modifier) {
    val index = LocalContentIndex.current
    val boon = rules.boonsByCode[code] ?: return
    val scale = 1 + rules.nodes.gradePower / 100 * grade
    Column(modifier) {
        Text(
            boonName(code, grade),
            color = boon.rarity.color(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        boon.lines.forEach { line -> Text(SkillText.statLine(line.stat, line.op, line.value * scale, index), color = RiftColors.Text, style = MaterialTheme.typography.labelSmall) }
        if (boon.rules.isNotEmpty()) Text(loc("rift.boon.$code.description"), color = RiftColors.Text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun CurseLine(code: String, altar: Boolean = false) {
    Text(
        "${loc("rift.curse.$code.name")} - ${loc("rift.curse.$code.description")}" + if (altar) " · ${ui("rift.altar_mark")}" else "",
        color = RiftColors.Warn,
        style = MaterialTheme.typography.bodySmall,
    )
}

/**
 * Лист узла: знак и имя вида, его суть, страж акта; за обзором - награды ([RiftEngine.rewards], ключ - с металлом), проклятие, черты
 * Вождя и условие Чемпиона (скрыто до боя). В забеге
 * ([onGo]) - «Идти» касанием, к стражу - удержанием; недоступный узел ([RiftSpot.bar]) гасит кнопку, касание говорит причину. На схеме недели
 * кнопки нет.
 */
@Composable private fun NodeSheet(rules: RiftRules, engine: RiftEngine, spot: RiftSpot, idle: Boolean, onDismiss: () -> Unit, onGo: (() -> Unit)? = null) {
    val node = spot.node
    val seen = spot.seen
    val facts = buildList {
        if (node.kind == RiftNodeKind.GUARDIAN) rules.guardians.monsters.getOrNull(node.act)?.let { add(loc("monster.${it.value}.name") + ": " + loc("rift.guardian.${it.value}.description")) }
        if (seen) {
            engine.rewards(node).takeIf { it.isNotEmpty() }?.let { rewards -> add(ui("rift.node_rewards", rewards.joinToString(" · ") { rewardName(node, it) })) }
            node.curse?.let { add(ui("rift.node_curse", loc("rift.curse.$it.name") + " - " + loc("rift.curse.$it.description"))) }
            if (node.traits.isNotEmpty()) add(ui("rift.node_traits", node.traits.joinToString(", ") { loc("trait.$it.name") }))
            if (node.trial != null) add(ui("rift.node_trial"))
        }
    }
    ForgeSheet(onDismissRequest = onDismiss, containerColor = RiftColors.Panel) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 18.dp, end = 18.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NodeSeal(node.kind, Modifier.size(34.dp))
                Text(loc("rift.node.${node.kind.name}.name"), color = node.kind.color(), style = MaterialTheme.typography.titleLarge)
            }
            Text(loc("rift.node.${node.kind.name}.description"), color = RiftColors.Text, style = MaterialTheme.typography.bodyMedium)
            facts.forEach { Text(it, color = RiftColors.Muted, style = MaterialTheme.typography.bodySmall) }
            onGo?.let { NodeGo(spot.bar?.key, idle, hold = spot.guardian, it) }
        }
    }
}

/** Имя награды узла [reward]; ключ - с металлом узла. */
private fun rewardName(node: RiftNode, reward: RiftReward): String = node.key?.takeIf { reward == RiftReward.KEY }?.let { ui("rift.key", ui("rift.metal.${it.name}")) } ?: ui("rift.reward.${reward.name}")

/** «Идти» листа узла: касанием, с [hold] - удержанием; с причиной [why] (ключ словаря) погашена, касание её говорит. */
@Composable private fun NodeGo(why: String?, idle: Boolean, hold: Boolean, onGo: () -> Unit) {
    var explain by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        ForgeButton(
            onClick = onGo,
            modifier = Modifier.fillMaxWidth(),
            state = ButtonState.of(idle && why == null),
            accent = RiftColors.Rift,
            hold = if (hold) Hold() else null,
            onDisabledClick = why?.let { { explain = true } },
        ) { Text(ui("rift.node_go")) }
        if (explain && why != null) TipCallout(Tip(body = ui(why), tint = RiftColors.Warn)) { explain = false }
    }
}

private const val RUNES = 24
private const val SPARKS = 14

/**
 * Вариант предложения узла, который выделяет касание: [Take] - дар, пара алтаря или товар Кузни по номеру в предложении,
 * [Merge] - слияние в Кузне, [Chalice] - Чаша Чести Кузни, [Rest] - выбор Отдыха, [Law] - закон контракта Владыки.
 */
private sealed interface OfferOption {
    data class Take(val index: Int) : OfferOption

    data object Merge : OfferOption

    data object Chalice : OfferOption

    data class Rest(val choice: RiftRest) : OfferOption

    data class Law(val law: RiftLaw) : OfferOption
}

/**
 * Выделение на плашке узла, общее для всех узлов: касание варианта выделяет его, касание другого меняет выбор; [targets] - цели
 * второго шага выделенного (ячейка дара на замену или усиление, проклятие, пара даров слияния). Выбор совершает только удержание
 * кнопки подвала; у нового узла выделение новое.
 */
@Stable private class OfferPick {
    var option by mutableStateOf<OfferOption?>(null)
        private set
    val targets = mutableStateListOf<Int>()

    fun select(next: OfferOption) {
        if (next == option) return
        option = next
        targets.clear()
    }

    /** Отметить цель [index] или снять отметку; сверх [limit] отметок уходит самая ранняя. */
    fun toggle(index: Int, limit: Int) {
        if (targets.remove(index)) return
        if (targets.size >= limit) targets.removeAt(0)
        targets += index
    }
}

/** Тон кнопки удержания по сути выбора: сделка - хаос, дар - золото Предначертания, покупка - Разлом, слияние и отдых - зелёный. */
private enum class CommitTone(val accent: Color?, val hold: Hold) {
    PACT(RiftColors.Chaos, Hold()),
    GIFT(null, Hold(tone = HoldTone.Fate)),
    BUY(RiftColors.Rift, Hold()),
    MEND(null, Hold()),
}

/** Уход с узла без выбора: подпись [label] и ход [act]. */
private class OfferLeave(val label: String, val act: RiftAct)

/**
 * Подвал плашки узла: главная кнопка удержания [label] тона [tone] совершает [act]; без хода выбор не полон - кнопка погашена,
 * а [label] говорит, что выбрать. [leave] - вторичная красная: уйти ни с чем или отказаться, тоже удержанием.
 */
private class OfferFooter(val label: String, val tone: CommitTone, val act: RiftAct?, val leave: OfferLeave? = null)

/** Цена дара [code] на витрине Кузни в Эхо. */
private fun RiftRules.price(code: String): Int = boonsByCode[code]?.rarity?.let(nodes.prices::get) ?: 0

/**
 * Подпись главной кнопки подвала: без выделения - [prompt], выделение без хода [act] (цели не отмечены) - [incomplete], иначе
 * действие [ready].
 */
private fun OfferPick.label(act: RiftAct?, ready: String, prompt: String, incomplete: String = ui("rift.pick_replace")): String = when {
    option == null -> prompt
    act == null -> incomplete
    else -> ready
}

/**
 * Подвал предложения [offer] при выделении [pick]: ход есть, только когда выбраны и вариант, и все его цели. Отдых пропускается только
 * при полной Чести ([RiftEngine.canSkipRest]).
 */
private fun offerFooter(rules: RiftRules, engine: RiftEngine, run: RiftRun, offer: RiftOffer, pick: OfferPick): OfferFooter {
    val option = pick.option
    val target = pick.targets.firstOrNull()
    val take = (option as? OfferOption.Take)?.let { if (run.boons.size < rules.nodes.slots) RiftAct.Take(it.index) else target?.let { slot -> RiftAct.Take(it.index, slot) } }
    val skip = OfferLeave(ui("rift.skip"), RiftAct.Leave)
    return when (offer) {
        is RiftOffer.Boons -> OfferFooter(pick.label(take, ui("rift.boon_take"), ui("rift.boons_title")), CommitTone.GIFT, take, skip)

        is RiftOffer.Altar -> OfferFooter(pick.label(take, ui("rift.altar_take"), ui("rift.altar_pick")), CommitTone.PACT, take, skip)

        is RiftOffer.Forge -> {
            val leave = OfferLeave(ui("rift.forge_leave"), RiftAct.Leave)
            if (option == OfferOption.Merge) {
                val merge = pick.targets.takeIf { it.size == 2 }?.let { RiftAct.Merge(it[0], it[1]) }
                OfferFooter(pick.label(merge, ui("rift.merge_take", rules.nodes.merge), ui("rift.forge_pick"), ui("rift.merge_pick_pair")), CommitTone.MEND, merge, leave)
            } else if (option == OfferOption.Chalice) {
                val chalice = RiftAct.Chalice.takeIf { offer.chalice }
                OfferFooter(pick.label(chalice, ui("rift.chalice_buy", rules.honor.chalicePrice), ui("rift.forge_pick")), CommitTone.BUY, chalice, leave)
            } else {
                val price = (option as? OfferOption.Take)?.let { rules.price(offer.shop[it.index]) } ?: 0
                OfferFooter(pick.label(take, ui("rift.forge_buy", price), ui("rift.forge_pick")), CommitTone.BUY, take, leave)
            }
        }

        RiftOffer.Rest -> {
            val choice = (option as? OfferOption.Rest)?.choice
            val act = choice?.let { rest -> if (rest == RiftRest.HEAL) RiftAct.Rest(rest) else target?.let { RiftAct.Rest(rest, it) } }
            val incomplete = if (choice == RiftRest.CLEANSE) ui("rift.rest_pick_curse") else ui("rift.rest_pick_boon")
            OfferFooter(pick.label(act, ui("rift.rest_take"), ui("rift.rest_pick"), incomplete), CommitTone.MEND, act, skip.takeIf { engine.canSkipRest(run) })
        }

        RiftOffer.Contract -> {
            val act = (option as? OfferOption.Law)?.let { RiftAct.Contract(it.law) }
            OfferFooter(pick.label(act, ui("rift.contract_take"), ui("rift.contract_hint")), CommitTone.PACT, act, OfferLeave(ui("rift.contract_refuse"), RiftAct.Contract(null)))
        }

        is RiftOffer.Coffers -> OfferFooter(ui("rift.coffers_done"), CommitTone.MEND, RiftAct.Leave)
    }
}

/**
 * Предложение узла: дары, пары алтаря, Отдых, Кузня, контракт Владыки, сундуки Владыки. Касание варианта выделяет его ([OfferPick]),
 * выбор совершает удержание кнопки подвала, закреплённого под листаемым списком; сундук открывает своя кнопка ([CofferCards]),
 * [coffers] - что лежало в открытых.
 */
@Composable private fun OfferPlate(
    game: GameUi,
    rules: RiftRules,
    engine: RiftEngine,
    run: RiftRun,
    offer: RiftOffer,
    coffers: Map<Int, Reward>,
    idle: Boolean,
    step: CallState<RiftStep>,
    onAct: (RiftAct) -> Unit,
) {
    val pick = remember(run.at, offer) { OfferPick() }
    RiftPlate(RiftColors.Hot) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (offer) {
                is RiftOffer.Boons -> {
                    Text(ui("rift.boons_title"), color = RiftColors.Hot, style = MaterialTheme.typography.titleSmall)
                    offer.options.forEachIndexed { i, code -> TakeCard(rules, run, pick, i, idle) { BoonLine(rules, code) } }
                }

                is RiftOffer.Altar -> {
                    PlateTitle(ui("rift.altar_title"), RiftColors.Chaos, listOf(ui("rift.altar_hint", rules.score.altarCurse.toInt()), ui("rift.altar_pair")))
                    offer.pacts.forEachIndexed { i, pact ->
                        TakeCard(rules, run, pick, i, idle) {
                            BoonLine(rules, pact.boon)
                            CurseLine(pact.curse)
                        }
                    }
                }

                RiftOffer.Rest -> RestCards(rules, engine, run, pick, idle)

                is RiftOffer.Forge -> ForgeCards(rules, engine, run, offer, pick, idle)

                is RiftOffer.Coffers -> CofferCards(game, offer, coffers, idle, onAct)

                RiftOffer.Contract -> {
                    PlateTitle(
                        ui("rift.contract_title"),
                        RiftColors.Warn,
                        listOf(ui("rift.contract_sign"), ui("rift.contract_hint"), ui("rift.contract_decline", rules.score.contract)),
                    )
                    rules.guardians.lord.laws.forEach { law ->
                        PickCard(pick, OfferOption.Law(law), idle && run.boons.isNotEmpty()) {
                            Text(loc("rift.law.${law.name}.name"), color = RiftColors.Text, style = MaterialTheme.typography.bodyMedium)
                            Text(loc("rift.law.${law.name}.description"), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (run.boons.isEmpty()) Text(ui("rift.contract_no_boons"), color = RiftColors.Warn, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        OfferFooterBar(offerFooter(rules, engine, run, offer, pick), idle, step, onAct)
    }
}

/**
 * Подвал плашки узла под списком: главная кнопка удержания во всю ширину и рядом красная «уйти» ([OfferFooter]); кнопка, чей шаг [step]
 * ждёт ответа дольше [CallTimes.SPINNER_MS], крутит спиннер.
 */
@Composable private fun OfferFooterBar(footer: OfferFooter, idle: Boolean, step: CallState<RiftStep>, onAct: (RiftAct) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ForgeButton(
            onClick = { footer.act?.let(onAct) },
            modifier = Modifier.weight(1f),
            size = ButtonSize.Wide,
            state = ButtonState.of(idle && footer.act != null, busy = footer.act?.let { step.spins(RiftStep.Act(it)) } == true),
            accent = footer.tone.accent,
            hold = footer.tone.hold,
        ) { Text(footer.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        footer.leave?.let { leave ->
            ForgeButton(
                onClick = { onAct(leave.act) },
                kind = ButtonKind.Danger,
                size = ButtonSize.Wide,
                state = ButtonState.of(idle, busy = step.spins(RiftStep.Act(leave.act))),
                icon = Icons.Outlined.Close,
                hold = Hold(),
            ) { Text(leave.label, maxLines = 1) }
        }
    }
}

/** Вариант «взять» ([OfferOption.Take]) номер [index]: при полных ячейках у выделенного - выбор дара на замену. */
@Composable private fun TakeCard(rules: RiftRules, run: RiftRun, pick: OfferPick, index: Int, enabled: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val replace: (@Composable ColumnScope.() -> Unit)? = if (run.boons.size >= rules.nodes.slots) {
        { PickTargets(pick, ui("rift.replace_hint"), heldTargets(rules, run) { true }) }
    } else {
        null
    }
    PickCard(pick, OfferOption.Take(index), enabled, replace, content)
}

/** Отдых: Честь и фляги - со сколько Чести вернёт; усиление дара и снятие проклятия - с целью у выделенного варианта. */
@Composable private fun RestCards(rules: RiftRules, engine: RiftEngine, run: RiftRun, pick: OfferPick, idle: Boolean) {
    RiftRest.entries.forEach { choice ->
        val (hint, targets) = when (choice) {
            RiftRest.HEAL -> "" to emptyList<PickTarget>()

            RiftRest.UPGRADE -> ui("rift.rest_pick_boon") to heldTargets(rules, run) { run.boons[it].grade < rules.nodes.maxGrade }

            RiftRest.CLEANSE -> ui("rift.rest_pick_curse") to run.curses.map { curse ->
                PickTarget(loc("rift.curse.${curse.code}.name") + if (curse.altar) " · ${ui("rift.altar_mark")}" else "", RiftColors.Warn, enabled = true)
            }
        }
        val possible = choice == RiftRest.HEAL || targets.any { it.enabled }
        val more: (@Composable ColumnScope.() -> Unit)? = if (targets.isEmpty()) {
            null
        } else {
            { PickTargets(pick, hint, targets) }
        }
        PickCard(pick, OfferOption.Rest(choice), idle && possible, more) {
            Text(ui("rift.rest.${choice.name}"), color = RiftColors.Text, style = MaterialTheme.typography.bodyMedium)
            if (choice == RiftRest.HEAL) {
                val back = rules.honor.rest * (1 + engine.rule(run, RiftRule.REST_HEAL) / 100).coerceAtLeast(0.0)
                Text(ui("rift.rest_honor", back.roundToInt()), color = RiftColors.Honor, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Кузня: витрина за Эхо, Чаша Чести (раз на Кузню) и слияние двух даров одной редкости; открыта, пока герой не уйдёт. */
@Composable private fun ForgeCards(rules: RiftRules, engine: RiftEngine, run: RiftRun, offer: RiftOffer.Forge, pick: OfferPick, idle: Boolean) {
    offer.shop.forEachIndexed { i, code ->
        val price = rules.price(code)
        val affordable = run.echo >= price
        TakeCard(rules, run, pick, i, idle && affordable) {
            BoonLine(rules, code)
            Text(
                if (affordable) ui("rift.forge_price", price) else ui("rift.forge_price_short", price),
                color = if (affordable) RiftColors.Rift else RiftColors.Warn,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
    val price = rules.honor.chalicePrice
    val affordable = run.echo >= price
    PickCard(pick, OfferOption.Chalice, idle && offer.chalice && affordable && run.honor < engine.maxHonor(run)) {
        Text(ui("rift.chalice_title"), color = RiftColors.Honor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(ui("rift.chalice_hint", rules.honor.chalice.roundToInt()), color = RiftColors.Text, style = MaterialTheme.typography.labelSmall)
        Text(
            when {
                !offer.chalice -> ui("rift.chalice_sold")
                affordable -> ui("rift.forge_price", price)
                else -> ui("rift.forge_price_short", price)
            },
            color = if (offer.chalice && affordable) RiftColors.Rift else RiftColors.Warn,
            style = MaterialTheme.typography.labelSmall,
        )
    }
    val rarityOf = { i: Int -> rules.boonsByCode[run.boons[i].code]?.rarity }
    val mergeable = run.boons.groupBy { rules.boonsByCode[it.code]?.rarity }.any { (rarity, held) -> rarity?.next != null && held.size >= 2 }
    val pair: @Composable ColumnScope.() -> Unit = {
        val first = pick.targets.firstOrNull()
        PickTargets(pick, ui("rift.merge_pick"), heldTargets(rules, run) { i -> rarityOf(i)?.next != null && (first == null || rarityOf(first) == rarityOf(i)) }, limit = 2)
    }
    PickCard(pick, OfferOption.Merge, idle && mergeable && run.echo >= rules.nodes.merge, pair) {
        Text(ui("rift.merge_title"), color = RiftColors.Text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(ui("rift.forge_merge", rules.nodes.merge), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Сундуки Владыки: по сундуку на каждый ключ героя ([RiftOffer.Coffers.row]), сгруппированы по металлу ([CofferGroup]). Под рядом -
 * что неоткрытые пропадут; [coffers] - что лежало в открытых, по номеру в ряду.
 */
@Composable private fun CofferCards(game: GameUi, offer: RiftOffer.Coffers, coffers: Map<Int, Reward>, idle: Boolean, onAct: (RiftAct) -> Unit) {
    PlateTitle(ui("rift.coffers_title"), RiftColors.Hot, listOf(ui("rift.coffers_hint"), ui("rift.coffers_note")))
    val groups = remember(offer, coffers) { CofferMetal.of(offer, coffers) }
    groups.forEach { group ->
        key(group.metal) {
            CofferGroup(game, group.metal, group.opened, group.total, group.next, group.found, idle) { onAct(RiftAct.Open(it)) }
        }
    }
    Text(ui("rift.coffers_note"), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall)
}

/** Сундуки одного металла [metal] в ряду Владыки: открыто [opened] из [total], следующий неоткрытый [next], что лежало в открытых [found]. */
private class CofferMetal(val metal: RiftMetal, val opened: Int, val total: Int, val next: Int?, val found: Reward?) {
    companion object {
        /** Ряд [offer] по металлам, по порядку ряда; [coffers] - что лежало в открытых, по номеру в ряду. */
        fun of(offer: RiftOffer.Coffers, coffers: Map<Int, Reward>): List<CofferMetal> = offer.row.withIndex().groupBy({ it.value }, { it.index }).map { (metal, slots) ->
            val opened = slots.filter { it in offer.opened }
            CofferMetal(metal, opened.size, slots.size, slots.firstOrNull { it !in offer.opened }, opened.mapNotNull(coffers::get).reduceOrNull(Reward::plus))
        }
    }
}

/**
 * Сундуки металла [metal] одной карточкой: рамка цвета металла, их число [total] и отметки открытых [opened], суть содержимого и
 * кнопка «Открыть ключом» - открывает следующий неоткрытый [next] (золотой - удержанием); [found] - что лежало в открытых.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CofferGroup(game: GameUi, metal: RiftMetal, opened: Int, total: Int, next: Int?, found: Reward?, idle: Boolean, onOpen: (Int) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val tint = metal.color()
    Column(
        Modifier.fillMaxWidth().background(RiftColors.Panel, shape).border(if (next == null) 1.dp else 1.5.dp, tint.copy(alpha = .9f), shape).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(ui("rift.metal.${metal.name}"), color = tint, style = MaterialTheme.typography.titleSmall)
            Text("×$total", color = tint, style = relicName(12), modifier = Modifier.weight(1f))
            Text(ui("rift.coffer_tally", opened, total), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall)
        }
        Text(ui("rift.coffer.${metal.name}"), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(total) { i ->
                if (i < opened) Box(Modifier.size(12.dp).border(1.dp, tint.copy(alpha = .6f), CircleShape)) else KeyMark(metal, Modifier.size(12.dp))
            }
        }
        if (next == null) {
            Text(ui("rift.coffers_all_opened"), color = RiftColors.Soft, style = MaterialTheme.typography.labelMedium)
        } else {
            ForgeButton(
                onClick = { onOpen(next) },
                modifier = Modifier.fillMaxWidth(),
                state = ButtonState.of(idle),
                accent = tint,
                hold = if (metal == RiftMetal.GOLD) Hold(tone = HoldTone.Fate) else null,
            ) { Text(ui("rift.coffer_open"), maxLines = 1, style = MaterialTheme.typography.labelMedium) }
        }
        found?.let { RewardLines(game, it) }
    }
}

/** Цель второго шага - чип: подпись [label], цвет [accent], можно ли её отметить ([enabled]). */
private class PickTarget(val label: String, val accent: Color, val enabled: Boolean)

/** Дары героя целями второго шага: имя со ступенью цветом редкости; [enabled] - какой можно отметить. */
private fun heldTargets(rules: RiftRules, run: RiftRun, enabled: (Int) -> Boolean): List<PickTarget> = run.boons.mapIndexed { i, held ->
    PickTarget(boonName(held.code, held.grade), rules.boonsByCode[held.code]?.rarity?.color() ?: RiftColors.Text, enabled(i))
}

/** Цели второго шага у выделенного варианта: строка [hint] и чипы [items]; отметить - не больше [limit]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickTargets(pick: OfferPick, hint: String, items: List<PickTarget>, limit: Int = 1) {
    Text(hint, color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { i, item ->
            val marked = i in pick.targets
            ForgeChip(
                item.label,
                marked,
                { pick.toggle(i, limit) },
                mode = if (limit == 1) ChipMode.Choice else ChipMode.Filter,
                state = ButtonState.of(item.enabled || marked),
                accent = item.accent,
            )
        }
    }
}

/**
 * Заголовок плашки или листа предложения с ⓘ: правило предложения [lines] - в окне по касанию (первая строка - главное, прочие
 * приглушённо), не сноской на плашке.
 */
@Composable private fun PlateTitle(title: String, tint: Color, lines: List<String>, style: TextStyle = MaterialTheme.typography.titleSmall) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = tint, style = style, modifier = Modifier.weight(1f, fill = false))
        InfoMark(title, accent = tint, tint = RiftColors.Muted) {
            Text(title, color = tint, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            lines.forEachIndexed { i, line ->
                Text(line, color = if (i == 0) RiftColors.Text else RiftColors.Muted, style = if (i == 0) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Вариант узла: касание выделяет его в [pick] (выбор можно сменить), сам выбор совершает удержание кнопки подвала. Выделенный -
 * золотая рамка и галочка, прочие - пустое кольцо; [targets] - цели второго шага, видны только у выделенного.
 */
@Composable private fun PickCard(
    pick: OfferPick,
    option: OfferOption,
    enabled: Boolean,
    targets: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val picked = pick.option == option
    val shape = RoundedCornerShape(10.dp)
    val rim = when {
        picked -> RiftColors.Pick
        enabled -> RiftColors.LineHi
        else -> RiftColors.Line
    }
    Box(
        Modifier.fillMaxWidth().background(RiftColors.Panel, shape).border(if (picked) 2.dp else 1.dp, rim, shape)
            .selectable(picked, enabled = enabled, role = Role.RadioButton) { pick.select(option) }
            .padding(10.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(end = 28.dp).alpha(if (enabled || picked) 1f else .6f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            content()
            if (picked) targets?.invoke(this)
        }
        PickMark(picked, Modifier.align(Alignment.TopEnd))
    }
}

/** Знак выделения варианта: золотой круг с галочкой или пустое кольцо. */
@Composable private fun PickMark(picked: Boolean, modifier: Modifier) {
    val mark = modifier.size(22.dp)
    if (picked) {
        Box(mark.background(RiftColors.Pick, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, null, tint = RiftColors.OnPick, modifier = Modifier.size(14.dp))
        }
    } else {
        Box(mark.border(1.5.dp, RiftColors.LineHi, CircleShape))
    }
}

@Composable internal fun RiftPlate(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().background(RiftColors.Panel, shape).border(1.dp, accent.copy(alpha = .5f), shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/**
 * Бой узла: арена, как у испытаний, и плашка с волной и условием Чемпиона. Нет связи - та же полоса, что на карте, под свитком Чести
 * (после боя - сверху экрана), без «Повторить»: бой идёт на устройстве, а итог досылается сам ([arenaStrip]).
 *
 * Посреди боя уйти нельзя: назад ставит паузу; досье стража перед боем, «В бой» начинает бой.
 */
@Composable private fun RiftFight(game: GameUi, model: ExpeditionViewModel, arena: RiftArena, refused: String?) {
    val shell: ShellViewModel = koinViewModel()
    val hud by arena.hud.collectAsState()
    val delivery by model.riftDelivery.collectAsStateWithLifecycle()
    val clock = rememberSimClock(arena)
    val offline = game.offline
    val strip = remember(offline, delivery) { arenaStrip(offline, delivery) }
    BackHandler { model.riftCommand(RunCommand.Pause) }
    Box(Modifier.fillMaxSize().background(Ink)) {
        hud.fight?.takeIf { hud.phase == TrialPhase.FIGHT }?.let { fight ->
            ArenaOverlay(
                game, hud.run, fight, hud.level, arena.combat, arena.stance, clock, onCommand = model::riftCommand, onLogFilter = shell::logFilter, pulse = arena.pulse,
                top = {
                    Column {
                        HonorScroll(hud.honor, hud.maxHonor, hud.loss, Modifier.fillMaxWidth())
                        RequestStrip(strip, RiftStripTones)
                    }
                },
            )
        }
        val shape = RoundedCornerShape(10.dp)
        Column(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 74.dp, end = 8.dp).widthIn(max = 190.dp)
                .background(RiftColors.Panel.copy(alpha = .92f), shape).border(1.dp, RiftColors.LineHi, shape).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(loc("rift.node.${hud.kind.name}.name"), color = RiftColors.Rift, style = MaterialTheme.typography.labelLarge)
            Text(ui("rift.wave", hud.wave, hud.waves), color = RiftColors.Text, style = MaterialTheme.typography.labelSmall)
            Text(clock(hud.seconds), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall)
            hud.trial?.let { trial ->
                PlateTitle(
                    ui("rift.champion", loc("rift.trial.${trial.name}.name")),
                    RiftColors.Hot,
                    listOf(loc("rift.trial.${trial.name}.description")),
                    MaterialTheme.typography.labelSmall,
                )
                Text(ui(if (hud.met) "rift.champion_met" else "rift.champion_failed"), color = if (hud.met) RiftColors.Rift else RiftColors.Warn, style = MaterialTheme.typography.labelSmall)
            }
        }
        val fight = hud.fight?.takeIf { hud.phase == TrialPhase.FIGHT && !it.started }
        val boss = fight?.boss
        var seen by remember(arena) { mutableStateOf(false) }
        if (fight != null && boss != null && !seen && hud.kind == RiftNodeKind.GUARDIAN) {
            fight.foes.firstOrNull { it.index == boss.index }?.let { foe ->
                BossDossier(
                    game, foe.monster, hud.level, foe.maxLife.toDouble(), boss.phase, boss.marks, loc("rift.node.${hud.kind.name}.name"),
                    fight.heroBody, arena.combat, record = { model.bossRecord(foe.monster.code.value) },
                ) {
                    seen = true
                    model.riftCommand(RunCommand.Begin)
                }
            }
        }
        if (hud.phase != TrialPhase.FIGHT) {
            DeliveryPlate(hud.phase == TrialPhase.DONE, refused, delivery, onLeave = model::leaveRiftRefused)
            RequestStrip(strip, RiftStripTones, Modifier.align(Alignment.TopCenter).statusBarsPadding())
        }
    }
}

/**
 * Полоса связи арены: нет связи (значок связи игры) - «Нет связи», бой продолжится, когда она вернётся; итог боя в пути - ожидание, как
 * у шага на доске (с 8 с - «Сервер долго отвечает» с номером попытки, их счёт не ограничен). Повтора кнопкой нет - итог досылается сам.
 */
private fun arenaStrip(offline: Boolean, delivery: CallState<RiftAct.Fight>): StripState = if (offline) StripState.Offline(ui("rift.arena_offline"), retry = false) else delivery.strip(ui("rift.arena_offline"), retry = false)

/**
 * Итог кончившегося боя в пути ([delivery]): «Победа · отправка итога» со спиннером, без связи - номер повтора, отказ сервера
 * ([refused]) - его слова и «К карте». Не голый спиннер: потерянный итог держал бы его вечно.
 */
@Composable private fun DeliveryPlate(won: Boolean, refused: String?, delivery: CallState<RiftAct.Fight>, onLeave: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(Modifier.fillMaxSize().background(RiftColors.Ground.copy(alpha = .7f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp).background(RiftColors.Panel, shape).border(1.dp, RiftColors.LineHi, shape).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(ui(if (won) "rift.delivery.won" else "rift.delivery.lost"), color = if (won) RiftColors.Rift else RiftColors.Warn, style = MaterialTheme.typography.titleMedium)
            FightLogButton()
            if (refused != null) {
                Text(ui("rift.delivery.refused", refused), color = RiftColors.Warn, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                ForgeButton(onClick = onLeave, kind = ButtonKind.Secondary, accent = RiftColors.Text) { Text(ui("rift.delivery.leave")) }
            } else {
                androidx.compose.material3.CircularProgressIndicator(color = RiftColors.Rift)
                val attempt = (delivery as? CallState.Waiting<RiftAct.Fight>)?.attempt ?: 1
                Text(if (attempt > 1) ui("rift.delivery.retry", attempt) else ui("rift.delivery.sending"), color = RiftColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Итог забега: проигрыш - «Честь иссякла» с пустым свитком и сгоревшими ключами, иначе - «Забег окончен» с причиной; счёт по долям -
 * очки узлов и стражей, остаток Чести с его очками и счёт недели; место, сундук.
 */
@Composable private fun RiftResultSheet(game: GameUi, model: ExpeditionViewModel, result: com.sperance.exileforge.rules.rift.RiftResult) {
    ForgeSheet(onDismissRequest = model::dismissRiftResult, containerColor = RiftColors.Panel) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui(if (result.fallen) "rift.result_honor_out" else "rift.result_title"), color = if (result.fallen) RiftColors.Warn else RiftColors.Rift, style = MaterialTheme.typography.titleLarge)
            Text(
                ui(
                    if (result.fallen) {
                        "rift.result_title"
                    } else if (result.cleared) {
                        "rift.result_cleared"
                    } else {
                        "rift.result_left"
                    },
                ),
                color = if (result.fallen) RiftColors.Muted else RiftColors.Soft,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (result.fallen) {
                HonorScroll(0.0, game.index?.campaign?.trials?.rift?.honor?.max ?: 1.0, 0.0, Modifier.fillMaxWidth())
                if (result.burned.values.any { it > 0 }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(ui("rift.result_burned"), color = RiftColors.Warn, style = MaterialTheme.typography.bodySmall)
                        KeyCounts(result.burned, burned = true)
                    }
                }
            }
            ResultLine(ui("rift.result_nodes"), number((result.score - result.honorScore).toDouble()), RiftColors.Text)
            ResultLine(ui("rift.result_honor", result.honor), "+" + number(result.honorScore.toDouble()), RiftColors.Honor)
            ResultLine(ui("rift.result_week"), number(result.score.toDouble()), RiftColors.Hot)
            if (result.place > 0) Text(ui("rift.result_place", result.place), color = RiftColors.Text, style = MaterialTheme.typography.bodyMedium)
            RewardLines(game, result.reward)
            FightLogButton()
            ForgeButton(onClick = model::dismissRiftResult, modifier = Modifier.fillMaxWidth()) { Text(ui("rift.back_board")) }
        }
    }
}

/** Строка счёта итога: подпись слева, число справа цветом [tint]. */
@Composable private fun ResultLine(label: String, value: String, tint: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = RiftColors.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, color = tint, style = relicName(15).copy(fontFeatureSettings = "tnum"))
    }
}

/** «Журнал боя» в итоге боя или забега: последний бой узла листом; без боя кнопки нет. */
@Composable private fun FightLogButton() {
    val report = LocalFightReport.current ?: return
    var open by remember { mutableStateOf(false) }
    ForgeButton(onClick = { open = true }, kind = ButtonKind.Secondary, accent = RiftColors.Text) { Text(ui("run.menu.log")) }
    if (open) FightLogSheet(report) { open = false }
}
