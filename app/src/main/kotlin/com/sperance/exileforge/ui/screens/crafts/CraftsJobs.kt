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
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.choiceTitle
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.professionDescription
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.display.text
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.core.model.crafts.WorkView
import com.sperance.exileforge.core.model.crafts.job
import com.sperance.exileforge.core.model.crafts.running
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.crafts.CraftsViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.JobInput
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SmithChoice
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
import kotlin.math.ceil

/** Работы профессии (3.80.14): строка работы, лист запуска с выбором и добавками, выбор инструмента. */
/**
 * A work as a card: what it makes under its name, then three figures to compare works by — about how much an hour
 * brings, how long a cycle runs, and how a cycle may turn out (doubled, in vain, or sure) — and at the foot what a
 * cycle costs against the bag and what it teaches. A work the profession has not reached is a dimmed «???» with its level.
 */
@Composable internal fun JobRow(game: GameUi, profession: ProfessionView, job: JobView, locked: Boolean, current: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        Modifier.fillMaxWidth().alpha(if (locked) .5f else 1f).depthPanel(shape).border(if (current) 2.dp else 1.dp, if (current) GoldBright else PanelRaised, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).background(Abyss, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                if (locked) Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.size(20.dp)) else JobIcon(job, Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(if (locked) ui("expedition.hidden") else jobTitle(job.code), color = if (locked) Muted else GoldBright, style = MaterialTheme.typography.titleSmall)
                MutedText(if (locked) ui("crafts.opens_at", job.level) else "→ ${jobProduct(job)}", style = MaterialTheme.typography.labelMedium)
            }
            if (current) {
                Text(
                    ui("crafts.running"),
                    color = Ink,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.background(Gold, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        if (locked) return@Column
        val double = profession.bonus.yield.coerceAtLeast(0.0)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val cell = Modifier.weight(1f)
            JobMetric(perHour(job, profession)?.let { ui("crafts.about", fineNumber(it)) } ?: "—", ui("crafts.metric_hour"), Parchment, cell)
            JobMetric(ui("crafts.duration_seconds", number(job.cycleMillis / 1000.0)), ui("crafts.metric_cycle"), Parchment, cell)
            when {
                double > 0 -> JobMetric(ui("crafts.percent", number(double)), ui("crafts.metric_double"), Vital, cell)
                job.nothing > 0 -> JobMetric(ui("crafts.percent", number(job.nothing)), ui("crafts.metric_waste"), LifeRed, cell)
                else -> JobMetric("✓", ui("crafts.metric_sure"), Vital, cell)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(cycleCost(game, job), style = MaterialTheme.typography.labelMedium, color = Muted, modifier = Modifier.weight(1f))
            Text(ui("crafts.experience_gain", number(job.experience)), color = Rune, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** A figure of a work's card: the number large, what it is small under it. */
@Composable internal fun JobMetric(value: String, label: String, tint: Color, modifier: Modifier) {
    Column(modifier.background(Abyss, RoundedCornerShape(6.dp)).padding(vertical = 8.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = tint, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        MutedText(label, style = MaterialTheme.typography.labelSmall)
    }
}

/** What a work makes, drawn: a stack's glass, an equipment's sprite, or the glyph of the kind when it makes something unnamed. */
@Composable internal fun JobIcon(job: JobView, modifier: Modifier) {
    when (job.kind) {
        JobKind.ITEM, JobKind.CONDENSE, JobKind.REFINE -> if (job.output.isNotBlank()) BagIcon(job.output, modifier) else GlyphIcon(Glyph.CRAFT, Gold, modifier)
        JobKind.FLASK, JobKind.EQUIPMENT, JobKind.JEWEL -> if (!SpriteIcon(equipmentIcon(job.output), Gold, modifier, halo = false)) GlyphIcon(Glyph.ITEM, Gold, modifier)
        JobKind.MAP -> GlyphIcon(Glyph.MAP, Gold, modifier)
        JobKind.BOOK -> GlyphIcon(Glyph.TEXT, Gold, modifier)
    }
}

/** A work, opened: what it brings, how long, how often in vain, what it teaches and finds on the side — and its button. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun JobSheet(game: GameUi, vm: CraftsViewModel, held: Crafts, profession: ProfessionView, work: JobView, current: Boolean, onDismiss: () -> Unit) {
    val crafts = held.state
    var additives by remember(work.code) { mutableStateOf(emptyList<String>()) }
    // A choosing work (3.45.0) is started as one of its variants: the sheet shows the one picked.
    val choices = choices(game, profession, work)
    // Кузнец (3.89.0) открывается в режиме, последний раз запущенном этим героем на устройстве; ничего не было - «Случайно».
    val smith = work.kind == JobKind.EQUIPMENT
    var picked by remember(work.code) { mutableStateOf(if (smith) SmithChoice.RANDOM.name else choices.firstOrNull()?.choice.orEmpty()) }
    LaunchedEffect(work.code, game.heroId) { if (smith) vm.smithChoice(game.heroId)?.let { picked = it.name } }
    val job = work.options.firstOrNull { it.choice == picked } ?: work
    // The smith's random piece (3.81.0, server 1.76.0) takes no additives: its lines roll two tiers lower instead.
    val random = job.choice == SmithChoice.RANDOM.name
    val chosenAdditives = if (random) emptyList() else additives
    val short = game.shortfall(job.cycleCost(chosenAdditives))?.text()
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(jobTitle(job.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            Text(professionTitle(profession.code), color = Rune, style = MaterialTheme.typography.labelMedium)
            if (work.options.isNotEmpty()) {
                Engraved(ui("crafts.choose"))
                if (choices.isEmpty()) {
                    MutedText(ui("crafts.no_choice"))
                } else if (smith) {
                    SmithPicker(SmithChoice.of(picked) ?: SmithChoice.RANDOM) { picked = it.name }
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        choices.forEach { option ->
                            FilterChip(selected = option.choice == picked, onClick = { picked = option.choice }, label = { Text(choiceTitle(option.choice)) })
                        }
                    }
                }
            }
            PropertyRow(ui("crafts.output"), jobProduct(job), Glyph.ITEM)
            if (job.inputs.isNotEmpty()) {
                Engraved(ui("crafts.inputs"))
                job.inputs.forEach { input ->
                    val have = bagCount(game, input.item)
                    PropertyRow(itemTitle(input.item), ui("crafts.have", have, input.amount), Glyph.CRAFT)
                }
            }
            // The smith's additives (2.42.0): each is spent every smelt and guarantees its handcrafted line.
            if (random) MutedText(ui("crafts.random_hint"))
            if (job.additives && !random && crafts != null && crafts.maxAdditives > 0) {
                Engraved(ui("crafts.additives", crafts.maxAdditives))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    crafts.additives.keys.forEach { code ->
                        val picked = code in additives
                        FilterChip(
                            selected = picked,
                            enabled = picked || (bagCount(game, code) > 0 && additives.size < crafts.maxAdditives),
                            onClick = { additives = if (picked) additives - code else additives + code },
                            label = { Text(ui("crafts.gain", bagCount(game, code), itemTitle(code)).removePrefix("+")) },
                        )
                    }
                }
            }
            PropertyRow(ui("crafts.cycle_label"), ui("crafts.seconds", number(job.cycleMillis / 1000.0), number(job.seconds)), Glyph.SPEED)
            if (job.nothing > 0) PropertyRow(ui("crafts.nothing"), ui("crafts.percent", number(job.nothing)), Glyph.INFO)
            PropertyRow(ui("crafts.experience"), number(job.experience), Glyph.LEVEL)
            job.extra.forEach { PropertyRow(ui("crafts.find", itemTitle(it.item)), ui("crafts.percent", number(it.chance)), Glyph.ITEM) }
            Spacer(Modifier.height(4.dp))
            when {
                job.level > profession.level -> Text(ui("crafts.needs_level", job.level), color = LifeRed, style = MaterialTheme.typography.bodyMedium)

                job.kind.chosen -> Text(ui("crafts.no_choice"), color = LifeRed, style = MaterialTheme.typography.bodyMedium)

                !job.open -> Text(ui("crafts.locked_map"), color = LifeRed, style = MaterialTheme.typography.bodyMedium)

                current -> ForgeOutlinedButton(enabled = !game.busy, onClick = {
                    onDismiss()
                    vm.stop()
                }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(ui("crafts.stop")) }

                // Цикл, который сумке не прокормить, не запускается (2.46.0); с 3.89.0 строка называет, чего и сколько не хватает.
                short != null -> Text(short, color = LifeRed, style = MaterialTheme.typography.bodyMedium)

                else -> ForgeButton(enabled = !game.busy, onClick = {
                    onDismiss()
                    if (smith) SmithChoice.of(job.choice)?.let { vm.rememberSmithChoice(game.heroId, it) }
                    vm.start(job.code, job.choice, chosenAdditives)
                }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(ui("crafts.start")) }
            }
            if (profession.equipped == null && job.level <= profession.level) Text(ui("crafts.no_tool"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** The stash's tools of this profession — the copies whose template sits in the profession's tool slot; tapping one puts it in the slot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolPicker(game: GameUi, profession: ProfessionView, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val slot = Slot.of(profession.tool)
    val tools = game.hero?.stash.orEmpty().filter { !it.socketed }
        .mapNotNull { instance -> game.view(instance)?.takeIf { slot != null && it.slot == slot }?.let { instance to it } }
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.7f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("crafts.pick_tool")) }
            if (tools.isEmpty()) item { InfoCard(ui("crafts.no_tools"), ui("crafts.no_tools_hint")) }
            items(tools, key = { it.first.id }) { (instance, view) ->
                ItemRow(view, enabled = !game.busy, unwearable = game.unmetFor(instance.template), price = game.sellPrice(instance)) { onPick(instance.id) }
            }
        }
    }
}
