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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.combat.CombatEvent
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.campaign.run.FightReport
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.campaign.run.RunHud
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.expedition.presentLoot
import com.sperance.exileforge.core.expedition.saleMarkable
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.decorTime
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.screens.expedition.arena.FightLog
import com.sperance.exileforge.ui.screens.expedition.arena.LogShelves
import com.sperance.exileforge.ui.screens.expedition.arena.outcomeColour
import com.sperance.exileforge.ui.screens.expedition.arena.rarityTint
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import java.util.Locale

/**
 * After the fight — «Поле боя», the owner's pick of five mockups. On top the scene: the
 * fallen monster's token in the half-dark with the outcome over it. Under it what the fight brought,
 * in engraved sections — «Снаряжение», a line per piece with its price, a tap opening its card;
 * «Сферы» as chips; «Награда», gold and experience — or, after a defeat, what the death cost and
 * what the run had gathered. The fight itself is a row of figures at the foot, and its log unfolds
 * from there. The spoils are the server's roll: the screen opens at once, says the loot is on its way,
 * and fills in as the answers arrive — offline, when the connection is back. Until they have, there is no way on:
 * neither the button nor the system back leaves, so nothing the fight brought is walked past unseen.
 *
 * Гибель открывается на журнале рокового боя. Награды в пути спрашиваются сразу; жест «назад» держит только копилка Бездны при гибели.
 * Первая победа над стражем - место героя среди всех победивших. Гибель ведёт к итогам карты, а не в лагерь. Над отчётом - карточка строки
 * журнала, разбор боя, рецепт с убийства и экран нового уровня. Очки Атласа боя - строкой в экране уровня, если бой поднял уровень,
 * иначе плиткой отчёта.
 */
@Composable internal fun ReportScreen(game: GameUi, model: ExpeditionViewModel, hud: RunHud, report: FightReport, onContinue: () -> Unit) {
    val shell: ShellViewModel = koinViewModel()
    val expedition: ExpeditionViewModel = koinViewModel()
    val won = report.outcome == Outcome.WIN
    var logOpen by remember { mutableStateOf(!won) }
    var line by remember { mutableStateOf<Pair<CombatEvent, String>?>(null) }
    var looked by remember { mutableStateOf<ItemView?>(null) }
    val inspect = rememberInspect()
    var recipe by remember { mutableStateOf<String?>(null) }
    var breakdown by remember { mutableStateOf(false) }
    val receiving = if (won) hud.rewardAwaiting > 0 else hud.abyss?.let { it.fallen && it.hoardAwaiting } == true
    LaunchedEffect(receiving) { if (receiving) model.flushRun() }
    val holding = receiving && !won
    var levelled by remember(report) { mutableStateOf(false) }
    LaunchedEffect(hud.levelUp) { if (hud.levelUp != null) levelled = true }
    BackHandler(enabled = holding) {}
    Column(
        Modifier.fillMaxSize().background(Ink.copy(alpha = .94f)).statusBarsPadding().navigationBarsPadding().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FieldHead(report, won)
        if (won) hud.rank?.let { place -> FirstWin(place, monsterTitle(report.monster.code)) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (won) Spoils(game, hud, onStack = { inspect(Inspect.Stack(it)) }, onRecipe = { recipe = it }) { looked = it } else DeathPrice(game, hud)
            if (won && hud.levelUp == null && !levelled) AtlasBlock(hud.atlas)
            if (logOpen) {
                Box(
                    Modifier.fillMaxWidth().height(if (won) 260.dp else 420.dp).background(Panel, RoundedCornerShape(8.dp))
                        .border(1.dp, Bronze.copy(alpha = .4f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Column {
                        LogShelves(game.logFilter, shell::logFilter)
                        FightLog(report.pack, Modifier.fillMaxSize(), game.logFilter, toDeath = !won) { event, name -> line = event to name }
                    }
                }
            }
        }
        FightFigures(report, logOpen, onBreakdown = { breakdown = true }) { logOpen = !logOpen }
        ForgeButton(
            enabled = !holding,
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (won) Gold else LifeRed, contentColor = if (won) Ink else Parchment),
        ) {
            Text(ui(if (won) "expedition.continue" else "expedition.to_summary"), style = MaterialTheme.typography.titleMedium)
        }
    }
    line?.let { (event, name) -> CombatDetailSheet(game, event, name) { line = null } }
    looked?.let { item -> LootSheet(game, model, item, onDismiss = { looked = null }) }
    if (breakdown) FightStatsSheet(report.stats, hud.summary.fights) { breakdown = false }
    recipe?.let { code -> RecipeSheet(game, code, inRun = true) { recipe = null } }
    if (won) hud.levelUp?.let { rise -> LevelUpScreen(rise, game.heroClass, game.index, game.isTester, hud.atlas) { model.runCommand(RunCommand.LevelSeen) } }
}

/**
 * The scene: the monster's round token in its rarity's ring, lit warm for a victory and red for a defeat, and the outcome in words.
 *
 * Дыхание портрета - декор: общие часы декора, при выключенных анимациях - покой.
 */
@Composable private fun FieldHead(report: FightReport, won: Boolean) {
    val expedition: ExpeditionViewModel = koinViewModel()
    val monster = report.monster
    val time = decorTime()
    val glow = if (won) Color(0xFF3B2A17) else Color(0xFF3B1717)
    Box(
        Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(8.dp)).background(Brush.radialGradient(listOf(glow, Ink))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.size(68.dp).clip(CircleShape).background(Color.Black).border(3.dp, rarityTint(monster.rarity), CircleShape),
                contentAlignment = Alignment.TopCenter,
            ) {
                Canvas(Modifier.requiredSize(68.dp, 91.dp).offset(y = 12.dp)) {
                    Portraits.monster(this, monster.code.value, monster.form, time.value)
                    if (won) drawRect(Ink.copy(alpha = .35f))
                }
            }
            Text(
                if (won) ui("expedition.report_slain", monsterTitle(monster.code)) else ui("expedition.report_fallen"),
                color = outcomeColour(report.outcome),
                style = MaterialTheme.typography.titleLarge,
            )
            if (report.packSize > 1) MutedText(ui("expedition.report_pack", report.packSize), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Гравированная подпись части итогов с бронзовой чертой за ней. [trailing] (счёт «+N») стоит после черты своим местом и меряется
 * первым: подпись и черта делят только остаток ширины, счёт всегда виден целиком.
 */
@Composable internal fun Caption(text: String, tone: Color = Gold, trailing: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text.uppercase(), color = tone, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Bronze, Color.Transparent))))
        }
        trailing?.invoke()
    }
}

@Composable private fun Chip(text: String, tone: Color = Parchment, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(3.dp)
    Text(
        text,
        color = tone,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.clip(shape).let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .background(Abyss, shape).border(1.dp, if (onClick != null) tone.copy(alpha = .6f) else PanelRaised, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/**
 * What the kill brought, by section, as the server's answers bring it; on its way, or its absence said plainly.
 *
 * По положению у героя: проданная вещь уходит из списка, надетая - с меткой «Надето». Строка на вещь, карточка - по касанию, удержание
 * помечает к продаже.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Spoils(game: GameUi, hud: RunHud, onStack: (String) -> Unit, onRecipe: (String) -> Unit, onItem: (ItemView) -> Unit) {
    val expedition: ExpeditionViewModel = koinViewModel()
    val reward = hud.reward ?: return
    val index = game.index
    reward.recipe?.let { code ->
        Caption(ui("expedition.report_recipe"))
        Chip(index?.let { i -> i.recipe(code)?.let { recipeText(i, it) } } ?: displayName(code), Rune) { onRecipe(code) }
    }
    val gear = game.presentLoot(reward.equipment, arriving = hud.rewardAwaiting > 0).mapNotNull { (instance, presence) -> game.view(instance)?.let { it to presence } }
    if (gear.isNotEmpty()) {
        Caption(ui("expedition.report_gear"))
        val marks = expedition.state.collectAsStateWithLifecycle().value.saleMarks
        gear.forEach { (piece, presence) ->
            val marked = piece.id in marks
            SaleMarked(marked) {
                PieceLine(piece, presence, selected = marked, onLongClick = { expedition.toggleSaleMark(piece.id) }.takeIf { presence.actionable && (marked || game.saleMarkable(piece.id, hud.rewardAwaiting > 0)) }) { onItem(piece) }
            }
        }
    }
    if (reward.items.isNotEmpty()) {
        Caption(ui("expedition.report_orbs"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            reward.items.forEach { (code, amount) -> StackChip(game, code, amount) { onStack(code) } }
        }
    }
    Caption(ui("expedition.report_reward"))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip(ui("expedition.loot_gold", reward.gold), GoldBright)
        Chip(ui("expedition.loot_experience", number(reward.experience)), Rune)
    }
    if (hud.rewardAwaiting > 0) {
        Receiving()
    } else if (reward.items.isEmpty() && reward.equipment.isEmpty()) {
        MutedText(ui("expedition.loot_nothing"))
    }
}

/**
 * A defeat: what the death cost by the rules' price — the server's answer stands — and what the run had gathered before it.
 *
 * Гибель в зоне Ваал опыта не стоит; гибель, что журнал не записал, оценивается при чтении героя; ниже уровня штрафа - даром; в Бездне
 * сгорает копилка, кроме доли атласа.
 */
@Composable private fun DeathPrice(game: GameUi, hud: RunHud) {
    val expedition: ExpeditionViewModel = koinViewModel()
    Caption(ui("expedition.report_death"), LifeRed)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(40.dp).border(1.dp, LifeRed, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Icon(ForgeGlyphs.Skull, null, tint = LifeRed, modifier = Modifier.size(22.dp))
        }
        val fall = hud.fall
        Column(Modifier.weight(1f)) {
            when {
                fall == null -> if (hud.vaal) {
                    MutedText(ui("expedition.fall_free"), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(ui("expedition.fall_failed"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                }

                fall > 0 -> Text(ui("expedition.fall_lost", number(fall)), color = LifeRed, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

                else -> MutedText(hud.fallFrom?.let { ui("expedition.fall_from", it) } ?: ui("expedition.fall_free"), style = MaterialTheme.typography.bodyMedium)
            }
            MutedText(ui(if (hud.vaal) "vaal.dead_hint" else "expedition.dead_hint"), style = MaterialTheme.typography.labelSmall)
        }
    }
    hud.abyss?.takeIf { it.fallen }?.let { abyss ->
        Caption(ui("abyss.fallen"), AbyssGlow)
        val kept = abyss.hoard
        if (abyss.hoardAwaiting) {
            Receiving()
        } else if (kept == null || kept.items.isEmpty() && kept.equipment.isEmpty() && kept.experience <= 0) {
            MutedText(ui("abyss.burned"))
        } else {
            MutedText(ui("abyss.kept"))
            RewardLines(game, kept)
        }
    }
    Caption(ui("expedition.report_run"))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip(ui("expedition.report_kills", hud.kills))
        Chip(ui("expedition.loot_gold", hud.gold), GoldBright)
        Chip(ui("expedition.loot_experience", number(hud.experience)), Rune)
    }
    if (hud.awaiting > 0) Receiving()
}

/** The fight as a row of figures — dealt, taken, how long, criticals — and the way into its log; «Разбор» - лист разбора боя. */
@Composable private fun FightFigures(report: FightReport, logOpen: Boolean, onBreakdown: () -> Unit, onLog: () -> Unit) {
    val expedition: ExpeditionViewModel = koinViewModel()
    Row(
        Modifier.fillMaxWidth().background(Abyss, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Figure(ForgeGlyphs.Swords, report.dealt.toString())
        Figure(ForgeGlyphs.Helm, report.taken.toString())
        Figure(ForgeGlyphs.Portal, ui("expedition.log_time", String.format(Locale.ROOT, "%.1f", report.duration)))
        Figure(ForgeGlyphs.Sigil, report.crits.toString())
        Text(
            ui("breakdown.open"),
            color = Rune,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clickable(role = Role.Button, onClick = onBreakdown).padding(4.dp),
        )
        Text(
            ui(if (logOpen) "expedition.log_less" else "expedition.log"),
            color = Rune,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clickable(role = Role.Button, onClick = onLog).padding(4.dp),
        )
    }
}

@Composable private fun Figure(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = Muted, modifier = Modifier.size(13.dp))
        Text(value, color = Parchment, style = MaterialTheme.typography.labelMedium)
    }
}

/** «Вы 5-й герой, победивший …»: a gilt band under the scene, the place large. */
@Composable private fun FirstWin(place: Long, guardian: String) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Gold.copy(alpha = .22f), Panel)), shape).border(1.dp, GoldBright.copy(alpha = .6f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("#$place", color = GoldBright, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f)) {
            Text(ui("expedition.first_win", place, guardian), color = Parchment, style = MaterialTheme.typography.bodyMedium)
            if (place == 1L) Text(ui("expedition.first_win_world"), color = GoldBright, style = MaterialTheme.typography.labelMedium)
        }
    }
}
