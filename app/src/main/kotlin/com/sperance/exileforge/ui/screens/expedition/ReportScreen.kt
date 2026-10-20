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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.sperance.exileforge.core.campaign.run.PhaseMark
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.campaign.run.RunHud
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.display.groupedNumber
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.phaseTitle
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
import kotlin.math.roundToInt

/** Вкладки отчёта боя: что бой принёс (при гибели - что стоил), ход боя и журнал. */
private enum class ReportTab { SPOILS, FIGHT, LOG }

/**
 * After the fight — вариант «Вкладки»: шапка в строку (жетон монстра, исход, стая и время боя, место первой победы) и вкладки
 * «Добыча · Бой · Журнал»; при гибели первая - «Потери», а экран открывается на «Журнале» рокового боя. У каждой вкладки вся высота:
 * добыча и потери листаются, «Бой» - основные показатели, пройденные пороги фаз и вход в разбор боя, журнал - во всю высоту.
 * В подвале одна кнопка: «Дальше», при гибели - опасная «К итогам захода».
 *
 * The spoils are the server's roll: the screen opens at once, says the loot is on its way, and fills in as the answers arrive — offline,
 * when the connection is back. Until they have, there is no way on: neither the button nor the system back leaves, so nothing the fight
 * brought is walked past unseen. Награды в пути спрашиваются сразу; жест «назад» держит только копилка Бездны при гибели.
 * Гибель ведёт к итогам карты, а не в лагерь. Над отчётом - карточка строки журнала, разбор боя, рецепт с убийства и экран нового уровня.
 * Очки Атласа боя - строкой в экране уровня, если бой поднял уровень, иначе плиткой во вкладке «Добыча».
 */
@Composable internal fun ReportScreen(game: GameUi, model: ExpeditionViewModel, hud: RunHud, report: FightReport, onContinue: () -> Unit) {
    val shell: ShellViewModel = koinViewModel()
    val won = report.outcome == Outcome.WIN
    var tab by remember(report) { mutableStateOf(if (won) ReportTab.SPOILS else ReportTab.LOG) }
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
        ReportHead(report, won, hud.rank.takeIf { won })
        ReportTabs(report, hud, won, tab) { tab = it }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                ReportTab.SPOILS -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (won) Spoils(game, hud, onStack = { inspect(Inspect.Stack(it)) }, onRecipe = { recipe = it }) { looked = it } else DeathPrice(game, hud)
                    if (won && hud.levelUp == null && !levelled) AtlasBlock(hud.atlas)
                }

                ReportTab.FIGHT -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FightTab(report) { breakdown = true }
                }

                ReportTab.LOG -> Column(
                    Modifier.fillMaxSize().background(Panel, RoundedCornerShape(8.dp)).border(1.dp, Bronze.copy(alpha = .4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    LogShelves(game.logFilter, shell::logFilter)
                    FightLog(report.pack, Modifier.fillMaxWidth().weight(1f), game.logFilter, toDeath = !won) { event, name -> line = event to name }
                }
            }
        }
        if (won) {
            ForgeButton(state = ButtonState.of(!holding), onClick = onContinue, modifier = Modifier.fillMaxWidth(), size = ButtonSize.Wide) {
                Text(ui("expedition.continue"), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            ForgeButton(
                state = ButtonState.of(!holding),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                kind = ButtonKind.Danger,
                size = ButtonSize.Wide,
                icon = ForgeGlyphs.Skull,
            ) {
                Text(ui("expedition.to_summary"), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    line?.let { (event, name) -> CombatDetailSheet(game, event, name) { line = null } }
    looked?.let { item -> LootSheet(game, model, item, onDismiss = { looked = null }) }
    if (breakdown) FightStatsSheet(report.stats, hud.summary.fights) { breakdown = false }
    recipe?.let { code -> RecipeSheet(game, code, inRun = true) { recipe = null } }
    if (won) hud.levelUp?.let { rise -> LevelUpScreen(rise, game.heroClass, game.index, game.isTester, hud.atlas) { model.runCommand(RunCommand.LevelSeen) } }
}

/**
 * Шапка в строку 44 dp: жетон монстра в кольце редкости, исход и под ним стая и время боя, справа - место первой победы над стражем
 * [rank], его слова - в подсказке по касанию.
 *
 * Дыхание портрета - декор: общие часы декора, при выключенных анимациях - покой.
 */
@Composable private fun ReportHead(report: FightReport, won: Boolean, rank: Long?) {
    val monster = report.monster
    val time = decorTime()
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Color.Black).border(2.dp, rarityTint(monster.rarity), CircleShape),
            contentAlignment = Alignment.TopCenter,
        ) {
            Canvas(Modifier.requiredSize(40.dp, 54.dp).offset(y = 7.dp)) {
                Portraits.monster(this, monster.code.value, monster.form, time.value)
                if (won) drawRect(Ink.copy(alpha = .35f))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                if (won) ui("expedition.report_slain", monsterTitle(monster.code)) else ui("expedition.report_fallen"),
                color = outcomeColour(report.outcome),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val seconds = ui("expedition.log_time", String.format(Locale.ROOT, "%.1f", report.duration))
            Text(
                if (report.packSize > 1) "${ui("expedition.report_pack", report.packSize)} · $seconds" else seconds,
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
        rank?.let { place -> RankMark(place, monsterTitle(monster.code)) }
    }
}

/** Место первой победы над стражем значком «#N»; касание - «Вы N-й герой, победивший …» в подсказке. */
@Composable private fun RankMark(place: Long, guardian: String) {
    val shape = RoundedCornerShape(6.dp)
    Tipped(
        { Tip(ui("expedition.first_win", place, guardian), if (place == 1L) ui("expedition.first_win_world") else "") },
        Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "#$place",
            color = GoldBright,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.background(Gold.copy(alpha = .16f), shape).border(1.dp, GoldBright.copy(alpha = .4f), shape).padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

/** Ряд вкладок долями одной дорожки; у «Добычи» - число находок, у «Журнала» - число строк боя. */
@Composable private fun ReportTabs(report: FightReport, hud: RunHud, won: Boolean, tab: ReportTab, onSelect: (ReportTab) -> Unit) {
    val found = hud.reward?.let { it.equipment.size + it.items.size + (if (it.recipe != null) 1 else 0) } ?: 0
    val labels = ReportTab.entries.map { entry ->
        when (entry) {
            ReportTab.SPOILS -> if (won) counted(ui("expedition.report_tab_spoils"), found) else ui("expedition.report_tab_losses")
            ReportTab.FIGHT -> ui("expedition.report_tab_fight")
            ReportTab.LOG -> counted(ui("expedition.report_tab_log"), report.events.size)
        }
    }
    PillTabs(labels, tab.ordinal, { onSelect(ReportTab.entries[it]) }, segmented = true)
}

private fun counted(label: String, count: Int) = if (count > 0) "$label · $count" else label

/**
 * Вкладка «Бой»: основные показатели плитками по две - нанесено, получено, урон в секунду, криты; пройденные пороги фаз из журнала
 * боя; внизу - вход в лист «Разбор боя».
 */
@Composable private fun FightTab(report: FightReport, onBreakdown: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Kpi(groupedNumber(report.dealt.toLong()), ui("expedition.report_kpi_dealt"), Modifier.weight(1f))
        Kpi(groupedNumber(report.taken.toLong()), ui("expedition.report_kpi_taken"), Modifier.weight(1f), LossText)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Kpi(groupedNumber(report.perSecond.toLong()), ui("breakdown.kpi.per_second"), Modifier.weight(1f))
        Kpi(groupedNumber(report.crits.toLong()), ui("breakdown.kpi.crits"), Modifier.weight(1f))
    }
    if (report.phases.isNotEmpty()) {
        Caption(ui("expedition.report_phases"), Boon)
        report.phases.forEach { mark -> PhaseRow(mark) }
    }
    ForgeButton(onClick = onBreakdown, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, size = ButtonSize.Compact, icon = Icons.Outlined.BarChart) {
        Text(ui("breakdown.title"))
    }
}

@Composable private fun Kpi(value: String, label: String, modifier: Modifier = Modifier, tone: Color = GoldBright) {
    val shape = RoundedCornerShape(8.dp)
    Column(modifier.background(Panel, shape).border(1.dp, PanelRaised, shape).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Text(value, color = tone, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Строка пройденного порога фазы: порог, имя шаблона фаз и «пройден». */
@Composable private fun PhaseRow(mark: PhaseMark) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, PanelRaised, shape).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("${mark.at.roundToInt()}%", color = Boon, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(40.dp))
        Text(phaseTitle(mark.phase), color = Parchment, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(ui("fight.phase_passed"), color = Vital, style = MaterialTheme.typography.labelSmall)
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
 * помечает к продаже. Вещь лучше надетого - с меткой «▲ Лучше надетого», фляга - «Новая фляга · 0 зар.» ([rememberLootMark]); пока
 * набор можно сменить, подсказка под списком: надевают в карточке, до начала следующего боя.
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
            val mark = rememberLootMark(game, piece.item, flasks = hud.gearLock == null)
            SaleMarked(marked) {
                PieceLine(piece, presence, selected = marked, onLongClick = { expedition.toggleSaleMark(piece.id) }.takeIf { presence.actionable && (marked || game.saleMarkable(piece.id, hud.rewardAwaiting > 0)) }, mark = mark) { onItem(piece) }
            }
        }
        if (hud.gearLock == null) MutedText(ui("expedition.report_wear_hint"))
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
 * сгорает копилка, кроме доли атласа. Что сохранилось и что дальше - в подсказке ⓘ, не сноской.
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
        }
        val hint = ui("expedition.report_hint")
        Tipped({ Tip(hint, ui(if (hud.vaal) "vaal.dead_hint" else "expedition.dead_hint"), Muted) }, Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Info, hint, tint = Muted, modifier = Modifier.size(18.dp))
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
