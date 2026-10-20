package com.sperance.exileforge.ui.screens.crafts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.sperance.exileforge.core.crafts.JobBlock
import com.sperance.exileforge.core.crafts.JobGroups
import com.sperance.exileforge.core.crafts.JobLedger
import com.sperance.exileforge.core.crafts.JobRates
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.additiveTitle
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.choiceTitle
import com.sperance.exileforge.core.display.clock
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.professionDescription
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.core.model.crafts.WorkView
import com.sperance.exileforge.core.model.crafts.job
import com.sperance.exileforge.core.model.crafts.running
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.sheet.sellPrice
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.CraftsRules
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.JobInput
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SmithChoice
import com.sperance.exileforge.rules.roll.Work
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.rules.roll.WorkTally
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay

/** Работы профессии: строка работы, лист запуска с выбором и добавками, выбор инструмента. */
/**
 * Строка работы формулой рецепта: иконка выхода (касание - его карточка), имя, под ним вход мини-иконками
 * «есть/нужно» (нехватка красным, касание - карточка материала) и время цикла; справа крупно выход в час (касание - справка «В час»).
 * Идущая работа ([current]) - короткой строкой: иконка, имя и «Идёт» справа; её вход, цикл и выход в час печатает плашка работы.
 * Работа, до которой профессия не доросла, - тусклая «???» с замком и уровнем открытия. Касание строки открывает лист работы.
 */
@Composable internal fun JobRow(game: GameUi, profession: ProfessionView, job: JobView, rules: CraftsRules?, locked: Boolean, current: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val inspect = rememberInspect()
    var help by remember { mutableStateOf(false) }
    val short = !locked && game.shortfall(job.cycleCost()) != null
    Row(
        Modifier.fillMaxWidth().heightIn(min = if (current) 48.dp else 62.dp).alpha(if (locked) .5f else 1f).depthPanel(shape).border(if (current) 2.dp else 1.dp, if (current) GoldBright else PanelRaised, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val result = if (locked) null else resultCard(game, job)
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Abyss).clickable(enabled = result != null, role = Role.Button) { result?.let(inspect) },
            contentAlignment = Alignment.Center,
        ) {
            if (locked) Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.size(20.dp)) else JobIcon(job, Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (locked) ui("expedition.hidden") else jobTitle(job.code),
                color = if (locked) Muted else GoldBright,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            when {
                locked -> MutedText(ui("crafts.opens_at", job.level), style = MaterialTheme.typography.labelSmall)
                !current -> RecipeLine(game, job) { inspect(Inspect.Stack(it)) }
            }
        }
        if (current) {
            RunningPill()
        } else if (!locked) {
            Column(Modifier.clip(RoundedCornerShape(6.dp)).clickable(enabled = rules != null, role = Role.Button) { help = true }.padding(4.dp), horizontalAlignment = Alignment.End) {
                val hour = rules?.let { remember(it, profession.level, profession.layers, job) { JobRates.perHour(it, profession, job) } }
                Text(hour?.let { fineNumber(it) } ?: "—", color = if (short) Muted else Vital, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(ui("crafts.metric_hour"), color = Muted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (help) rules?.let { CraftsGlossaryEntry(it, "hour") { help = false } }
}

/** Пилюля «Идёт» у идущей работы. */
@Composable internal fun RunningPill() {
    Text(
        ui("crafts.running"),
        color = Ink,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.background(Gold, RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/**
 * Рецепт строкой: вход мини-иконками «есть/нужно» (нехватка красным, касание - [onInput] с кодом материала) и время цикла; работа без
 * входа - «бесплатно», с долей впустую, если она есть.
 */
@Composable private fun RecipeLine(game: GameUi, job: JobView, onInput: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        job.inputs.forEach { input ->
            val have = bagCount(game, input.item)
            Row(
                Modifier.clip(RoundedCornerShape(4.dp)).clickable(role = Role.Button) { onInput(input.item) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                BagIcon(input.item, Modifier.size(16.dp))
                Text(ui("crafts.ratio", compactNumber(have.toDouble()), input.amount), color = if (have >= input.amount) Parchment else LifeRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        val tail = listOfNotNull(ui("crafts.free").takeIf { job.inputs.isEmpty() }, clock(job.cycleMillis), job.nothing.takeIf { it > 0 && job.inputs.isEmpty() }?.let { ui("crafts.waste_short", number(it)) })
        Text(tail.joinToString(" · ", prefix = if (job.inputs.isEmpty()) "" else "· "), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** Отбор работ по группе выхода: «Все N» и каждая группа с числом открытых работ; [selected] null - все. */
@Composable internal fun JobFilters(groups: JobGroups, selected: String?, onSelect: (String?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ForgeChip(ui("crafts.group_all", groups.total), selected == null, { onSelect(null) }, mode = ChipMode.Choice)
        groups.counts.forEach { (group, count) ->
            ForgeChip(ui("crafts.group_count", ui("crafts.group.$group"), count), selected == group, { onSelect(group) }, mode = ChipMode.Choice)
        }
    }
}

/** Карточка того, что делает работа: стопка - её карточка, фляга и инструмент - витрина шаблона; прочее (выбор, карта, ковка) - null. */
internal fun resultCard(game: GameUi, job: JobView): Inspect? {
    val output = job.output.takeIf { it.isNotBlank() } ?: return null
    return when {
        game.index?.item(ItemCode(output)) != null -> Inspect.Stack(output)
        game.index?.template(output) != null -> Inspect.Showcase(output)
        else -> null
    }
}

/** What a work makes, drawn: a stack's glass, an equipment's sprite, or the glyph of the kind when it makes something unnamed. */
@Composable internal fun JobIcon(job: JobView, modifier: Modifier) {
    when (job.kind) {
        JobKind.ITEM, JobKind.CONDENSE, JobKind.REFINE -> if (job.output.isNotBlank()) BagIcon(job.output, modifier) else GlyphIcon(Glyph.CRAFT, Gold, modifier)
        JobKind.FLASK, JobKind.EQUIPMENT, JobKind.JEWEL, JobKind.TOOL -> if (!SpriteIcon(equipmentIcon(job.output), Gold, modifier, halo = false)) GlyphIcon(Glyph.ITEM, Gold, modifier)
        JobKind.MAP -> GlyphIcon(Glyph.MAP, Gold, modifier)
        JobKind.BOOK -> GlyphIcon(Glyph.TEXT, Gold, modifier)
        JobKind.EVOLVE -> GlyphIcon(Glyph.LEVEL, Gold, modifier)
        JobKind.CARVE -> if (job.output.isNotBlank()) BagIcon(job.output, modifier) else GlyphIcon(Glyph.GEM, Gold, modifier)
        JobKind.DESECRATE -> GlyphIcon(Glyph.RARITY, Gold, modifier)
    }
}

/**
 * Лист работы формулой рецепта: имя и «профессия · с ур. N», выбор варианта (и вещи у Осквернения), полоса-формула «вход + вход → выход
 * + находки» (касание материала или выхода - карточка), что получится - имя, сведения и описание (касание - карточка), поля работы списком
 * с разбором по источникам ([JobFields]), добавки работы с их эффектом, у нехватки - где материал добывается,
 * и внизу кнопка или одна точная причина, почему работу не начать: уровень, вариант, регион, инструмент или нехватка на цикл.
 *
 * Имя работы и блок под выбором ([JobBody]) держат высоту самого высокого варианта ([TallestOf]): смена варианта не двигает верх
 * листа, что встаёт по низу. Работа с выбором запускается одним из вариантов; недоступный виден с замком. Кузнец открывается в
 * последнем режиме этого героя на устройстве, иначе «Случайно»; случайная ковка примесей не берёт - её строки на два тира ниже.
 * Осквернение выбирает вещь тайника, что пропустят правила.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun JobSheet(game: GameUi, vm: CraftsViewModel, held: Crafts, profession: ProfessionView, work: JobView, current: Boolean, onDismiss: () -> Unit) {
    val crafts = held.state
    var additives by remember(work.code) { mutableStateOf(emptyList<String>()) }
    val choices = choices(game, work)
    val smith = work.kind == JobKind.EQUIPMENT
    var picked by remember(work.code) {
        mutableStateOf(if (smith) SmithChoice.RANDOM.name else (choices.firstOrNull { it.level <= profession.level } ?: choices.firstOrNull())?.choice.orEmpty())
    }
    LaunchedEffect(work.code, game.heroId) { if (smith) vm.smithChoice(game.heroId)?.let { picked = it.name } }
    val job = work.options.firstOrNull { it.choice == picked } ?: work
    val variants = work.options.ifEmpty { listOf(work) }
    val targets = remember(work.code, game.hero?.items, game.index) { itemTargets(game, work.job) }
    var target by remember(work.code) { mutableStateOf("") }
    val item = target.takeIf { id -> targets.any { it.id == id } }.orEmpty()
    val inspect = rememberInspect()
    var help by remember { mutableStateOf<String?>(null) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    TallestOf(variants, job) { Text(jobTitle(it.code), color = GoldBright, style = MaterialTheme.typography.titleLarge) }
                    Text(ui("crafts.sheet_from", professionTitle(profession.code), job.level), color = Rune, style = MaterialTheme.typography.labelMedium)
                }
                CraftsGlossaryButton(crafts?.rules)
            }
            if (work.options.isNotEmpty()) {
                Engraved(ui("crafts.choose"))
                if (smith) {
                    SmithPicker(SmithChoice.of(picked) ?: SmithChoice.RANDOM, { bagCount(game, it) }) { picked = it.name }
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        choices.forEach { option ->
                            ForgeChip(
                                choiceTitle(option.choice),
                                option.choice == picked,
                                { picked = option.choice },
                                leading = if (!profession.opens(option)) ({ Icon(Icons.Outlined.Lock, null, Modifier.size(14.dp)) }) else null,
                                mode = ChipMode.Choice,
                            )
                        }
                    }
                }
            }
            if (work.job.targetsItem && targets.isNotEmpty()) {
                Engraved(ui("crafts.pick_item"))
                HoldHint(HoldSubject.ITEM)
                targets.forEach { view ->
                    ItemRow(view, selected = view.id == item, price = game.sellPrice(view.item), onLongClick = { inspect(Inspect.Copy(view.item)) }) { target = view.id }
                }
            }
            TallestOf(variants, job) { variant ->
                val chosen = if (variant.choice == SmithChoice.RANDOM.name) emptyList() else additives
                JobBody(
                    game, crafts, profession, work, choices, variant, chosen, targets, item, current,
                    onInspect = inspect,
                    onHelp = { help = it },
                    onAdditives = { additives = it },
                    onStop = {
                        onDismiss()
                        vm.stop()
                    },
                ) {
                    onDismiss()
                    if (smith) SmithChoice.of(variant.choice)?.let { vm.rememberSmithChoice(game.heroId, it) }
                    vm.start(variant.code, if (variant.job.targetsItem) item else variant.choice, chosen)
                }
            }
        }
    }
    help?.let { key -> crafts?.rules?.let { CraftsGlossaryEntry(it, key) { help = null } } }
}

/**
 * Что меняет вариант [job] работы [work]: формула, что получится, поля работы, добавки работы ([additives] - уже без них у случайной
 * ковки; [onAdditives] - выбор примесей), короткая заметка и внизу кнопка или причина, почему работу не начать. Идёт в
 * [TallestOf] по всем вариантам: высота блока - по самому высокому, верх листа при смене варианта не двигается.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun JobBody(
    game: GameUi,
    crafts: CraftsState?,
    profession: ProfessionView,
    work: JobView,
    choices: List<JobView>,
    job: JobView,
    additives: List<String>,
    targets: List<ItemView>,
    item: String,
    current: Boolean,
    onInspect: (Inspect) -> Unit,
    onHelp: (String) -> Unit,
    onAdditives: (List<String>) -> Unit,
    onStop: () -> Unit,
    onStart: () -> Unit,
) {
    val random = job.choice == SmithChoice.RANDOM.name
    val block = JobBlock.of(game, profession, work, choices, job, additives, targets, item)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RecipeFormula(game, profession, job, additives) { it?.let(onInspect) }
        ResultLine(game, job) { it?.let(onInspect) }
        crafts?.rules?.let { rules -> JobFields(remember(game.lang, rules, profession.level, profession.layers, profession.equipped, job) { JobLedger.of(rules, profession, job) }, job.code, onHelp) }
        val offered = crafts?.additives(job).orEmpty()
        if (offered.isNotEmpty() && !random && crafts != null && crafts.maxAdditives > 0) {
            Engraved(ui("crafts.additives", crafts.maxAdditives))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                offered.forEach { (code, effect) ->
                    val on = code in additives
                    ForgeChip(
                        ui("crafts.additive_chip", bagCount(game, code), itemTitle(code), additiveTitle(effect, game.index)),
                        on,
                        { onAdditives(if (on) additives - code else additives + code) },
                        state = ButtonState.of(on || (bagCount(game, code) > 0 && additives.size < crafts.maxAdditives)),
                    )
                }
            }
        }
        shortNote(game, job, additives)?.let { MutedText(it, style = MaterialTheme.typography.labelMedium) }
        Spacer(Modifier.height(4.dp))
        when {
            current -> ForgeButton(state = ButtonState.of(!game.busy), onClick = onStop, modifier = Modifier.fillMaxWidth(), size = ButtonSize.Wide, kind = ButtonKind.Secondary) { Text(ui("crafts.stop")) }

            block != null -> ForgeButton(state = ButtonState.of(false), onClick = {}, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(block.text, textAlign = TextAlign.Center) }

            else -> ForgeButton(state = ButtonState.of(!game.busy), onClick = onStart, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(ui("crafts.start")) }
        }
    }
}

/**
 * Полоса-формула: вход цикла (с примесями [additives]) ячейками «есть/нужно» через «+» - у работы без входа инструмент профессии, -
 * стрелка, выход «×1» (с лишней добычей - «×1–N») и находки с шансом. Касание ячейки - [onOpen] с карточкой материала или выхода.
 */
@Composable private fun RecipeFormula(game: GameUi, profession: ProfessionView, job: JobView, additives: List<String>, onOpen: (Inspect?) -> Unit) {
    val inputs = Work.perCycle(job.job, additives)
    Row(
        Modifier.fillMaxWidth().depthPanel(RoundedCornerShape(12.dp)).horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (inputs.isEmpty()) {
            FormulaSlot(ui("crafts.formula_tool"), Muted, frame = Bronze) { ToolIcon(game, profession, 28) }
        } else {
            inputs.entries.forEachIndexed { i, (code, amount) ->
                if (i > 0) FormulaPlus()
                val have = bagCount(game, code)
                val enough = have >= amount
                FormulaSlot(ui("crafts.ratio", compactNumber(have.toDouble()), amount), if (enough) Parchment else LifeRed, frame = if (enough) Bronze else LifeRed, onClick = { onOpen(Inspect.Stack(code)) }) {
                    BagIcon(code, Modifier.size(28.dp))
                }
            }
        }
        FormulaArrow()
        FormulaSlot(JobRates.yieldRange(profession.bonus), Parchment, frame = Bronze, size = 56, onClick = resultCard(game, job)?.let { card -> { onOpen(card) } }) { JobIcon(job, Modifier.size(36.dp)) }
        job.extra.forEach { find ->
            FormulaPlus()
            FormulaSlot(ui("crafts.percent", number(find.chance)), Brass, frame = Bronze, onClick = { onOpen(Inspect.Stack(find.item)) }) { BagIcon(find.item, Modifier.size(28.dp)) }
        }
    }
}

/** Ячейка формулы: значок в рамке [frame] размером [size] dp и подпись [label] цветом [tone]; касание - [onClick]. */
@Composable private fun FormulaSlot(label: String, tone: Color, frame: Color, size: Int = 46, onClick: (() -> Unit)? = null, icon: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(size.dp).clip(RoundedCornerShape(8.dp)).background(Abyss).border(1.dp, frame, RoundedCornerShape(8.dp))
                .clickable(enabled = onClick != null, role = Role.Button) { onClick?.invoke() },
            contentAlignment = Alignment.Center,
        ) { icon() }
        Text(label, color = tone, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable private fun FormulaPlus() = Text("+", color = Muted, style = MaterialTheme.typography.titleMedium)

/** Стрелка формулы от входа к выходу: черта от бронзы к золоту с наконечником. */
@Composable private fun FormulaArrow() {
    Canvas(Modifier.width(28.dp).height(12.dp)) {
        val y = size.height / 2
        drawLine(Brush.horizontalGradient(listOf(Bronze, Gold)), Offset(0f, y), Offset(size.width - 4.dp.toPx(), y), strokeWidth = 2.dp.toPx())
        val tip = Path().apply {
            moveTo(size.width, y)
            lineTo(size.width - 7.dp.toPx(), y - 5.dp.toPx())
            lineTo(size.width - 7.dp.toPx(), y + 5.dp.toPx())
            close()
        }
        drawPath(tip, Gold)
    }
}

/** Что получится: имя выхода, сведения (в сумке или уровень) и описание стопки; касание - [onOpen] с его карточкой, если она есть. */
@Composable private fun ResultLine(game: GameUi, job: JobView, onOpen: (Inspect?) -> Unit) {
    val card = resultCard(game, job)
    val output = job.output
    val meta = when (card) {
        is Inspect.Stack -> ui("crafts.result_bag", compactNumber(bagCount(game, output).toDouble()))
        is Inspect.Showcase -> game.index?.template(output)?.let { ui("row.level", it.level) }.orEmpty()
        else -> ""
    }
    val description = if (card is Inspect.Stack) itemDescription(output, game.index) else ""
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(enabled = card != null, role = Role.Button) { onOpen(card) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(jobProduct(job), color = Parchment, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (meta.isNotEmpty()) Text(meta, color = Muted, style = MaterialTheme.typography.labelSmall)
            if (description.isNotEmpty()) Text(description, color = Parchment, style = MaterialTheme.typography.bodySmall)
        }
        if (card != null) Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

/** The stash's tools of this profession — the copies whose template sits in the profession's tool slot; касание открывает карточку, её кнопка кладёт инструмент в слот. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolPicker(game: GameUi, profession: ProfessionView, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val slot = Slot.of(profession.tool)
    val tools = game.hero?.stash.orEmpty().filter { !it.socketed }
        .mapNotNull { instance -> game.view(instance)?.takeIf { slot != null && it.slot == slot }?.let { instance to it } }
    val inspect = rememberInspect()
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.7f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("crafts.pick_tool")) }
            if (tools.isEmpty()) item { InfoCard(ui("crafts.no_tools"), ui("crafts.no_tools_hint")) }
            items(tools, key = { it.first.id }) { (instance, view) ->
                ItemRow(view, unwearable = game.unmetFor(instance.template), price = game.sellPrice(instance)) {
                    inspect(Inspect.Copy(instance, InspectAction(ui("hero.equip"), enabled = !game.busy) { onPick(instance.id) }))
                }
            }
        }
    }
}
