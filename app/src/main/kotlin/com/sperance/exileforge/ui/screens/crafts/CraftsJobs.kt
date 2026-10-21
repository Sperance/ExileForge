package com.sperance.exileforge.ui.screens.crafts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.crafts.JobBlock
import com.sperance.exileforge.core.crafts.JobGroups
import com.sperance.exileforge.core.crafts.JobLedger
import com.sperance.exileforge.core.crafts.JobRates
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.PickOption
import com.sperance.exileforge.core.display.PickOptions
import com.sperance.exileforge.core.display.clock
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.core.model.crafts.job
import com.sperance.exileforge.core.model.crafts.running
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.CraftsRules
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.Job
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SmithChoice
import com.sperance.exileforge.rules.roll.Work
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.screens.skills.SkillPeek
import com.sperance.exileforge.ui.screens.skills.rememberSkillInspect
import com.sperance.exileforge.ui.theme.*

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
 * Лист работы формулой рецепта: имя и «профессия · с ур. N», выборы листа - строки общего выбора ([ForgePicker]): вариант работы (книга,
 * эссенция, материал, база инструмента, умение ритуала, руна - [jobChoiceLabel]; у ковки - кузнец), вещь у Осквернения, инструмент
 * профессии, добавки работы ([ForgeMultiPicker]); ниже полоса-формула «вход + вход → выход + находки» (касание материала или выхода -
 * карточка), что получится - имя, сведения и описание (касание - карточка), поля работы списком с разбором по источникам ([JobFields]),
 * у нехватки - где материал добывается, и внизу кнопка или одна точная причина, почему работу не начать: уровень, вариант, регион,
 * инструмент или нехватка на цикл.
 *
 * Имя работы и блок под выбором ([JobBody]) держат высоту самого высокого варианта ([TallestOf]): смена варианта не двигает верх
 * листа, что встаёт по низу. Вариант, до которого профессия не доросла, в шторке закрыт; долгое нажатие на вариант - карточка того,
 * что он даст (умение книги и ритуала - карточка умения). Кузнец открывается в последнем режиме этого героя на устройстве, иначе
 * «Случайно»; случайная ковка примесей не берёт - её строки на два тира ниже. Осквернение выбирает вещь тайника, что пропустят правила.
 */
@Composable
internal fun JobSheet(game: GameUi, vm: CraftsViewModel, held: Crafts, profession: ProfessionView, work: JobView, current: Boolean, onDismiss: () -> Unit) {
    val crafts = held.state
    var additives by remember(work.code) { mutableStateOf(emptyList<String>()) }
    val choices = remember(game, work) { choices(game, work) }
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
    val peek = rememberChoicePeek(game, work.kind)
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
            val picksItem = work.job.targetsItem && targets.isNotEmpty()
            val picksTool = profession.tool.isNotBlank()
            if (work.options.isNotEmpty() || picksItem || picksTool) Engraved(ui("crafts.choose"))
            if (work.options.isNotEmpty()) {
                val options = remember(game.lang, game.hero?.bag, game.index, work, choices, profession.level) {
                    if (smith) PickOptions.smiths { bagCount(game, it) } else PickOptions.jobChoices(work, choices, profession, game.index) { bagCount(game, it) }
                }
                ForgePicker(jobChoiceLabel(work.kind), picked, options, hint = ui("picker.peek_hint").takeIf { !smith }, onPeek = peek.takeIf { !smith }) { key -> key?.let { picked = it } }
            }
            if (picksItem) {
                val options = remember(game.lang, targets) { PickOptions.copies(targets) }
                ForgePicker(ui("crafts.pick_target"), item.ifEmpty { null }, options, hint = ui("crafts.pick_item"), onPeek = { id -> targets.firstOrNull { it.id == id }?.let { inspect(Inspect.Copy(it.item)) } }) { target = it.orEmpty() }
            }
            if (picksTool) ToolChoice(game, vm, profession)
            val sheet = JobSheetState(game, crafts, profession, work, choices, targets, item, current)
            val actions = JobSheetActions(
                onInspect = inspect,
                onHelp = { help = it },
                onAdditives = { additives = it },
                onStop = {
                    onDismiss()
                    vm.stop()
                },
            ) { variant, chosen ->
                onDismiss()
                if (smith) SmithChoice.of(variant.choice)?.let { vm.rememberSmithChoice(game.heroId, it) }
                vm.start(variant.code, if (variant.job.targetsItem) item else variant.choice, chosen)
            }
            TallestOf(variants, job) { variant ->
                JobBody(sheet, variant, if (variant.choice == SmithChoice.RANDOM.name) emptyList() else additives, actions)
            }
        }
    }
    help?.let { key -> crafts?.rules?.let { CraftsGlossaryEntry(it, key) { help = null } } }
}

/**
 * Карточка того, что даст вариант работы вида [kind] по его выбору: умение книги и ритуала - карточка умения, база инструмента -
 * витрина шаблона, стопка - её карточка.
 */
@Composable private fun rememberChoicePeek(game: GameUi, kind: JobKind): (String) -> Unit {
    val inspect = rememberInspect()
    val inspectSkill = rememberSkillInspect()
    val index = game.index
    return remember(index, kind, inspect, inspectSkill) {
        { code: String ->
            val skill = when (kind) {
                JobKind.BOOK -> index?.skills?.byBook(code)?.code
                JobKind.EVOLVE -> code
                else -> null
            }
            when {
                skill != null -> inspectSkill(SkillPeek(skill))
                index?.template(code) != null -> inspect(Inspect.Showcase(code))
                index?.item(ItemCode(code)) != null -> inspect(Inspect.Stack(code))
            }
        }
    }
}

/**
 * Общее у всех вариантов листа работы [work] профессии [profession]: [choices] - варианты, что лист предлагает, [targets] - вещи
 * героя под работу над вещью и выбранная из них [item], [current] - эта работа идёт.
 */
private data class JobSheetState(
    val game: GameUi,
    val crafts: CraftsState?,
    val profession: ProfessionView,
    val work: JobView,
    val choices: List<JobView>,
    val targets: List<ItemView>,
    val item: String,
    val current: Boolean,
)

/**
 * Касания листа работы: [onInspect] - карточка, [onHelp] - статья словаря ремёсел, [onAdditives] - выбор примесей, [onStop] - стоп
 * идущей работы, [onStart] - запуск варианта с примесями.
 */
private class JobSheetActions(
    val onInspect: (Inspect) -> Unit,
    val onHelp: (String) -> Unit,
    val onAdditives: (List<String>) -> Unit,
    val onStop: () -> Unit,
    val onStart: (JobView, List<String>) -> Unit,
)

/**
 * Что меняет вариант [job] листа [sheet]: добавки работы ([additives] - уже без них у случайной ковки; выбор нескольких), формула, что
 * получится, поля работы, короткая заметка и внизу кнопка или причина, почему работу не начать. Идёт в [TallestOf] по всем вариантам:
 * высота блока - по самому высокому, верх листа при смене варианта не двигается; причина и заметка каждого варианта считаются заново лишь
 * при смене того, из чего они сложены.
 */
@Composable
private fun JobBody(sheet: JobSheetState, job: JobView, additives: List<String>, actions: JobSheetActions) {
    val (game, crafts, profession, work, choices, targets, item, current) = sheet
    val onInspect = actions.onInspect
    val random = job.choice == SmithChoice.RANDOM.name
    val block = remember(game, profession, work, choices, job, additives, targets, item) { JobBlock.of(game, profession, work, choices, job, additives, targets, item) }
    val note = remember(game, job, additives) { shortNote(game, job, additives) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val offered = crafts?.crafting?.additives(job.job).orEmpty()
        val most = crafts?.crafting?.maxAdditives ?: 0
        if (offered.isNotEmpty() && !random && most > 0) {
            val options = remember(game.lang, game.hero?.bag, game.index, offered) { PickOptions.additives(offered, game.index) { bagCount(game, it) } }
            ForgeMultiPicker(
                ui("crafts.additives", most),
                additives,
                options,
                most,
                counted = { ui(pluralKey("crafts.additive_count", it), it) },
                onPeek = { onInspect(Inspect.Stack(it)) },
                onPick = actions.onAdditives,
            )
        }
        RecipeFormula(game, profession, job, additives) { it?.let(onInspect) }
        ResultLine(game, job) { it?.let(onInspect) }
        crafts?.rules?.let { rules -> JobFields(remember(game.lang, rules, profession.level, profession.layers, profession.equipped, job) { JobLedger.of(rules, profession, job) }, job.code, actions.onHelp) }
        note?.let { MutedText(it, style = MaterialTheme.typography.labelMedium) }
        Spacer(Modifier.height(4.dp))
        when {
            current -> ForgeButton(state = ButtonState.of(!game.busy), onClick = actions.onStop, modifier = Modifier.fillMaxWidth(), size = ButtonSize.Wide, kind = ButtonKind.Secondary) { Text(ui("crafts.stop")) }
            block != null -> ForgeButton(state = ButtonState.of(false), onClick = {}, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(block.text, textAlign = TextAlign.Center) }
            else -> ForgeButton(state = ButtonState.of(!game.busy), onClick = { actions.onStart(job, additives) }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(ui("crafts.start")) }
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

/**
 * Что получится: имя выхода, сведения (в сумке или уровень) и описание стопки; касание - [onOpen] с его карточкой, если она есть, у
 * ритуала тира - карточка умения.
 */
@Composable private fun ResultLine(game: GameUi, job: JobView, onOpen: (Inspect?) -> Unit) {
    val card = resultCard(game, job)
    val skill = (job.job as? Job.Evolve)?.skill?.takeIf { it.isNotEmpty() }
    val inspectSkill = rememberSkillInspect()
    val open: (() -> Unit)? = card?.let { { onOpen(it) } } ?: skill?.let { { inspectSkill(SkillPeek(it)) } }
    val output = job.output
    val meta = when (card) {
        is Inspect.Stack -> ui("crafts.result_bag", compactNumber(bagCount(game, output).toDouble()))
        is Inspect.Showcase -> game.index?.template(output)?.let { ui("row.level", it.level) }.orEmpty()
        else -> ""
    }
    val description = if (card is Inspect.Stack) itemDescription(output, game.index) else ""
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(enabled = open != null, role = Role.Button) { open?.invoke() }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(jobProduct(job), color = Parchment, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (meta.isNotEmpty()) Text(meta, color = Muted, style = MaterialTheme.typography.labelSmall)
            if (description.isNotEmpty()) Text(description, color = Parchment, style = MaterialTheme.typography.bodySmall)
        }
        if (open != null) Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

/**
 * Инструменты профессии [profession]: надетый первым, затем копии тайника, чей шаблон встаёт в слот её инструмента (не в гнезде).
 */
internal fun toolViews(game: GameUi, profession: ProfessionView): List<ItemView> {
    val slot = Slot.of(profession.tool) ?: return emptyList()
    val stash = game.hero?.stash.orEmpty().filter { !it.socketed }.mapNotNull { instance -> game.view(instance)?.takeIf { it.slot == slot } }
    return listOfNotNull(profession.equipped?.let { game.view(it) }) + stash
}

/** Выбор инструмента профессии: варианты [options], надетый [worn] и его карточка по долгому нажатию [peek]. */
private class ToolPick(val options: List<PickOption<String>>, val worn: String?, val peek: (String) -> Unit) {
    /** Что сказать, когда инструментов нет: где их взять. */
    val empty: PickEmpty get() = PickEmpty(ui("crafts.no_tools"), ui("crafts.no_tools_hint"))
}

/** Выбор инструмента профессии [profession] из её инструментов ([toolViews]); пересобирается, только когда сдвинулись вещи героя. */
@Composable private fun rememberToolPick(game: GameUi, profession: ProfessionView): ToolPick {
    val tools = remember(game.hero?.items, game.index, profession.tool, profession.equipped) { toolViews(game, profession) }
    val inspect = rememberInspect()
    return remember(game.lang, tools, inspect) {
        ToolPick(PickOptions.copies(tools), profession.equipped?.id) { id -> tools.firstOrNull { it.id == id }?.let { inspect(Inspect.Copy(it.item)) } }
    }
}

/** Строка выбора инструмента профессии в листе работы: выбранный - надетый, выбор другого надевает его. */
@Composable private fun ToolChoice(game: GameUi, vm: CraftsViewModel, profession: ProfessionView) {
    val pick = rememberToolPick(game, profession)
    ForgePicker(ui("crafts.tool"), pick.worn, pick.options, enabled = !game.busy, empty = pick.empty, onPeek = pick.peek) { id -> id?.takeIf { it != pick.worn }?.let(vm::equipTool) }
}

/**
 * Смена инструмента профессии сама по себе (кнопка шапки окна профессии и «Сменить инструмент»): шторка общего выбора ([ForgePickSheet])
 * со всеми её инструментами; выбор другого - [onPick] с id копии, долгое нажатие - карточка копии.
 */
@Composable
internal fun ToolPickSheet(game: GameUi, profession: ProfessionView, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val pick = rememberToolPick(game, profession)
    ForgePickSheet(ui("crafts.tool"), pick.worn, pick.options, empty = pick.empty, onDismiss = onDismiss, onPeek = pick.peek) { id ->
        if (id != null && id != pick.worn) onPick(id) else onDismiss()
    }
}
