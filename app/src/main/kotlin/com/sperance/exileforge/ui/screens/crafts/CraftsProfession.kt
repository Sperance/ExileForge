package com.sperance.exileforge.ui.screens.crafts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.crafts.JobGroups
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.choiceTitle
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.professionDescription
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.core.model.crafts.WorkView
import com.sperance.exileforge.core.model.crafts.job
import com.sperance.exileforge.core.model.crafts.running
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.sheet.sellPrice
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.JobInput
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.rules.roll.WorkTally
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.ceil

/** Окно профессии: плитка, уровень, инструмент и его бонусы. */
/** A profession as a tile, three to a row: its tool in a medallion, name, level, how far to the next one, and what stands out. */
@Composable internal fun ProfessionTile(game: GameUi, profession: ProfessionView, working: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    val toolless = profession.equipped == null
    Column(
        modifier.depthPanel(shape).border(if (working) 2.dp else 1.dp, if (working) GoldBright else PanelRaised, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 6.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Medallion(if (toolless) LifeRed else Bronze) { ToolIcon(game, profession, 26) }
        Box(Modifier.heightIn(min = 32.dp), contentAlignment = Alignment.Center) {
            Text(
                professionTitle(profession.code),
                color = GoldBright,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(ui("crafts.level", profession.level), color = Rune, style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(progress = { share(profession) }, modifier = Modifier.fillMaxWidth().height(3.dp), color = Vital, trackColor = PanelRaised)
        when {
            working -> Text(ui("crafts.working"), color = Gold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            toolless -> Text(ui("crafts.no_tool_short"), color = LifeRed, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

/** A square stood on its corner, as the tree's nodes are, holding a drawing upright. */
@Composable internal fun Medallion(frame: Color, content: @Composable () -> Unit) {
    Box(
        Modifier.padding(6.dp).size(40.dp).rotate(45f)
            .background(Brush.radialGradient(listOf(PanelRaised, Ink)), RoundedCornerShape(2.dp)).border(1.dp, frame, RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center,
    ) { Box(Modifier.rotate(-45f)) { content() } }
}

internal fun share(profession: ProfessionView): Float = profession.next?.takeIf { it > 0 }?.let { (profession.experience / it).toFloat().coerceIn(0f, 1f) } ?: 1f

/** The tool in the slot, drawn as the cards draw it — the server's outline tinted by rarity, the bundled emblem behind it — or the anvil for an empty slot. */
@Composable internal fun ToolIcon(game: GameUi, profession: ProfessionView, size: Int) {
    val modifier = Modifier.size(size.dp)
    val tool = profession.equipped?.let { game.view(it) }
    if (tool == null) {
        Icon(ForgeGlyphs.Anvil, null, tint = Muted, modifier = modifier)
        return
    }
    val paint = rarityColor(tool.rarity.name)
    if (!SpriteIcon(equipmentIcon(tool.code), paint, modifier)) ItemEmblem(tool.visualKind, paint, modifier)
}

/**
 * Окно профессии плотной колонкой: в шапке имя, уровень тонкой полосой (опыт - в подсказке по касанию), справка ⓘ (с описанием
 * профессии и тем, как идёт работа) и инструмент; под ней идущая здесь работа - та же плашка, что на главном экране вкладки
 * ([WorkPlaque]); ниже без заголовка работы профессии строками ([JobRow], идущая - короткой строкой) с отбором по группе выхода
 * ([JobGroups]), когда групп больше одной.
 *
 * Открытые работы и следующая за ними - «???» со своим уровнем.
 */
@Composable internal fun ProfessionWindow(game: GameUi, vm: CraftsViewModel, crafts: Crafts, profession: ProfessionView, offset: Long) {
    BackHandler { vm.openProfession("") }
    var picking by remember { mutableStateOf(false) }
    var toolOpen by remember(profession.equipped?.id) { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<JobView?>(null) }
    var group by remember(profession.code) { mutableStateOf<String?>(null) }
    val work = crafts.state?.work?.takeIf { it.profession == profession.code }
    val groups = remember(profession, game.index) { JobGroups.of(profession, game.index) }
    val picked = group?.takeIf { it in groups }
    val locked = { job: JobView -> !profession.opens(job) }
    val next = profession.jobs.firstOrNull(locked)
    val shown = profession.jobs.filter { (!locked(it) || it == next) && (picked == null || groups.groupOf(it) == picked) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ProfessionHeader(game, crafts, profession, onBack = { vm.openProfession("") }) { if (profession.equipped == null) picking = true else toolOpen = true } }
        if (work != null) item { WorkPlaque(game, vm, crafts, offset) }
        if (groups.size > 1) item { JobFilters(groups, picked) { group = it } }
        items(shown, key = { it.code }) { job ->
            JobRow(game, profession, job, crafts.state?.rules, locked = locked(job), current = work?.job == job.code) { if (!locked(job)) chosen = job }
        }
    }
    chosen?.let { job -> JobSheet(game, vm, crafts, profession, job, current = work?.job == job.code) { chosen = null } }
    if (toolOpen) {
        ToolSheet(game, profession, onDismiss = { toolOpen = false }) {
            toolOpen = false
            picking = true
        }
    }
    if (picking) {
        ToolPicker(game, profession, onDismiss = { picking = false }) { id ->
            picking = false
            vm.equipTool(id)
        }
    }
}

/**
 * Шапка окна профессии: назад, имя и уровень тонкой полосой (на последнем - «макс.»; касание - подсказка с опытом «набрано / нужно»),
 * справка с описанием профессии и инструмент. Срок до уровня печатает плашка идущей работы.
 */
@Composable private fun ProfessionHeader(game: GameUi, crafts: Crafts, profession: ProfessionView, onBack: () -> Unit, onTool: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ForgeIconButton(Icons.AutoMirrored.Outlined.ArrowBack, ui("common.back"), onClick = onBack, accent = Gold)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(professionTitle(profession.code), color = GoldBright, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val tip = profession.next?.let { next ->
                { Tip(ui("crafts.level", profession.level), facts = listOf(ui("crafts.totals_experience") to ui("crafts.level_experience", compactNumber(profession.experience), compactNumber(next)))) }
            }
            Tipped(tip, Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ui("crafts.level", profession.level), color = Rune, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(progress = { share(profession) }, modifier = Modifier.weight(1f).height(4.dp), color = Vital, trackColor = PanelRaised)
                    if (profession.next == null) Text(ui("crafts.level_top_short"), color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        CraftsGlossaryButton(crafts.state?.rules, intro = professionDescription(profession.code), note = true)
        ToolButton(game, profession, onTool)
    }
}

/** Вариант и примеси идущей работы строкой: «Оружие · Сила · + Кровавый флюс»; без них - null. */
internal fun workDetail(work: WorkView): String? = (listOfNotNull(work.choice.takeIf { it.isNotEmpty() }?.let(::choiceTitle)) + work.additives.map { "+ ${itemTitle(it)}" })
    .takeIf { it.isNotEmpty() }?.joinToString(" · ")

/** «Остановить»: опасная компактная иконка-кнопка. */
@Composable internal fun StopButton(enabled: Boolean, onClick: () -> Unit) {
    ForgeIconButton(Icons.Outlined.Stop, ui("crafts.stop"), onClick = onClick, kind = ButtonKind.Danger, size = ButtonSize.Compact, state = ButtonState.of(enabled))
}

/** The tool in the window's header: its icon in a frame of its rarity, or an empty «+» cell for none. */
@Composable internal fun ToolButton(game: GameUi, profession: ProfessionView, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val view = profession.equipped?.let { game.view(it) }
    val frame = when {
        profession.equipped == null -> LifeRed
        view != null -> rarityColor(view.rarity.name)
        else -> Bronze
    }
    Box(
        Modifier.size(44.dp).background(Abyss, shape).border(1.dp, frame, shape)
            .clickable(role = Role.Button, onClickLabel = ui("crafts.tool"), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (profession.equipped == null) {
            Icon(Icons.Outlined.Add, ui("crafts.tool"), tint = LifeRed, modifier = Modifier.size(22.dp))
        } else {
            ToolIcon(game, profession, 30)
        }
    }
}

/**
 * Лист надетого инструмента профессии [profession] за кнопкой шапки: полная карточка и что дают он и древо прокручиваются над
 * закреплённым подвалом ([SheetFooter]) - «Сменить инструмент» и под ней общий ряд действий своей вещи ([OwnItemActions]: «В кузницу»,
 * «Снять и продать» удержанием); действие ряда закрывает лист. Без инструмента листа нет: кнопка шапки открывает выбор инструмента.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolSheet(game: GameUi, profession: ProfessionView, onDismiss: () -> Unit, onChange: () -> Unit) {
    val tool = profession.equipped ?: return
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Engraved(ui("crafts.tool"))
                val view = game.view(tool)
                if (view != null) {
                    ItemCard(view, detailed = true, price = game.sellPrice(view.item))
                } else {
                    Text(equipmentTitle(tool.template), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                }
                BonusChips(profession)
            }
            SheetFooter {
                ForgeButton(state = ButtonState.of(!game.busy), onClick = onChange, modifier = Modifier.fillMaxWidth()) { Text(ui("crafts.change_tool")) }
                OwnItemActions(game, tool, onDone = onDismiss)
            }
        }
    }
}

/** What the tool and the tree give this profession, a chip per figure that is not zero. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun BonusChips(profession: ProfessionView) {
    val bonus = profession.bonus
    val figures = listOf(
        "crafts.bonus_speed" to bonus.speed,
        "crafts.bonus_yield" to bonus.yield,
        "crafts.bonus_luck" to bonus.luck,
        "crafts.bonus_experience" to bonus.experience,
        "crafts.bonus_find" to bonus.find,
    ).filter { it.second != 0.0 }
    if (figures.isEmpty()) {
        MutedText(ui("crafts.bonus_none"))
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        figures.forEach { (key, value) ->
            Text(
                ui(key, number(value)),
                color = Parchment,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.background(PanelRaised, RoundedCornerShape(2.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
