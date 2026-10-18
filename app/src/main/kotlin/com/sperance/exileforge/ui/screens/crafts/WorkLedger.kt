package com.sperance.exileforge.ui.screens.crafts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.weaponTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.rules.roll.WorkMiss
import com.sperance.exileforge.rules.roll.WorkTally
import com.sperance.exileforge.ui.components.ForgeOutlinedButton
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.components.rememberNow
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.Vital
import com.sperance.exileforge.ui.theme.rarityColor

/**
 * Итог работы для книги учёта: и с начала работы ([WorkTally] сервера), и за сессию ([WorkGains] клиента). [made] - сколько вещей сделано
 * по счёту сервера, [missed] - циклы впустую по причине.
 */
internal class WorkSums(
    val cycles: Long,
    val nothing: Long,
    val experience: Double,
    val levels: Int,
    val made: Long,
    val gathered: Map<String, Long>,
    val spent: Map<String, Long>,
    val missed: Map<WorkMiss, Long>,
) {
    /** Общая шкала мини-полос: самая большая стопка, добытая или потраченная. */
    val scale: Long get() = (gathered.values + spent.values).maxOrNull()?.coerceAtLeast(1) ?: 1

    val isEmpty: Boolean get() = gathered.isEmpty() && spent.isEmpty()

    /** Строки стопок по убыванию, раз на итог. */
    val gatheredSorted: List<Pair<String, Long>> = gathered.entries.sortedByDescending { it.value }.map { it.key to it.value }
    val spentSorted: List<Pair<String, Long>> = spent.entries.sortedByDescending { it.value }.map { it.key to it.value }

    companion object {
        fun of(tally: WorkTally) = WorkSums(tally.cycles, tally.nothing, tally.experience, tally.levels, tally.made, tally.items, tally.spent, tally.missed)

        fun of(gains: WorkGains) = WorkSums(
            gains.cycles.toLong(),
            gains.nothing.toLong(),
            gains.experience,
            gains.levels,
            gains.made.toLong(),
            gains.items,
            gains.spent,
            gains.missed.mapValues { it.value.toLong() },
        )
    }
}

/**
 * Книга учёта ремесла: под чертой [title] итоги плитками в ряд короткими числами без переноса - время в работе (вживую, [startedAt] > 0 по
 * часам сервера со сдвигом [offset]: «1:05»), циклы («130 −2» впустую), опыт («+12.5k ▲1» уровни), сделано (счёт сервера, плитки нет при 0)
 * и уровень профессии [level]; не влезло число - кегль плитки меньше; под плитками - почему циклы прошли впустую («Впустую ×3: нет
 * подходящих основ»); затем каждая
 * стопка строкой - иконка, имя, мини-полоса на общей шкале и сколько (добытое зелёным, потраченное красным); сделанные вещи [made] -
 * строками с иконкой в рамке редкости, именем её цвета, видом и уровнем: первые [MADE_SHOWN], прочие - под «Ещё N». [onShow] - кнопка
 * «Показать в тайнике» под сделанным; null - без неё. Касание строки открывает карточку.
 */
@Composable
internal fun WorkLedger(
    game: GameUi,
    sums: WorkSums,
    made: List<ItemInstance>,
    title: String? = null,
    startedAt: Long = 0,
    offset: Long = 0,
    level: Int? = null,
    onShow: (() -> Unit)? = null,
) {
    val inspect = rememberInspect()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        title?.let { SectionRule(it) }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val cell = Modifier.weight(1f).fillMaxHeight()
            if (startedAt > 0) LedgerTile(ui("crafts.totals_time"), cell) { WorkTime(startedAt, offset) }
            LedgerTile(ui("crafts.totals_cycles"), cell) {
                TileFigure(
                    buildAnnotatedString {
                        append(compactNumber(sums.cycles.toDouble()))
                        if (sums.nothing > 0) withStyle(SpanStyle(color = LifeRed, fontSize = TILE_NOTE)) { append(" −" + compactNumber(sums.nothing.toDouble())) }
                    },
                    Parchment,
                )
            }
            LedgerTile(ui("crafts.totals_experience"), cell) {
                TileFigure(AnnotatedString("+" + compactNumber(sums.experience) + (if (sums.levels > 0) " ▲${sums.levels}" else "")), Vital)
            }
            if (sums.made > 0) LedgerTile(ui("crafts.totals_made"), cell) { TileFigure(AnnotatedString(compactNumber(sums.made.toDouble())), Parchment) }
            level?.let { LedgerTile(ui("crafts.totals_profession"), cell) { TileFigure(AnnotatedString(ui("crafts.level", it)), Rune) } }
        }
        if (sums.missed.isNotEmpty()) {
            Text(
                sums.missed.entries.joinToString(" · ") { (miss, cycles) -> ui("crafts.missed", ui("crafts.miss.${miss.name}"), compactNumber(cycles.toDouble())) },
                color = LifeRed,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (sums.isEmpty) {
            Text(ui("crafts.totals_empty"), color = Muted, style = MaterialTheme.typography.labelSmall)
        } else {
            Column {
                sums.gatheredSorted.forEach { (code, amount) ->
                    LedgerRow(game, code, amount, sums.scale, gathered = true) { inspect(Inspect.Stack(code)) }
                }
                sums.spentSorted.forEach { (code, amount) ->
                    LedgerRow(game, code, amount, sums.scale, gathered = false) { inspect(Inspect.Stack(code)) }
                }
            }
        }
        if (made.isNotEmpty()) {
            SectionRule(ui("crafts.made_title", made.size))
            MadeList(game, made) { inspect(Inspect.Copy(it)) }
            onShow?.let { show ->
                ForgeOutlinedButton(onClick = show, modifier = Modifier.fillMaxWidth()) {
                    Icon(ForgeGlyphs.Stash, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(ui("crafts.show_in_stash"))
                }
            }
        }
    }
}

/** Сделанные вещи строками: первые [MADE_SHOWN], прочие раскрываются касанием «Ещё N ▾». */
@Composable internal fun MadeList(game: GameUi, made: List<ItemInstance>, onOpen: (ItemInstance) -> Unit) {
    var open by remember(made.size) { mutableStateOf(false) }
    Column {
        (if (open) made else made.take(MADE_SHOWN)).forEach { item -> MadeRow(game, item) { onOpen(item) } }
        if (made.size > MADE_SHOWN) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button) { open = !open }.heightIn(min = 40.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (open) ui("crafts.less") else ui("crafts.more", made.size - MADE_SHOWN), color = Gold, style = MaterialTheme.typography.labelLarge)
                Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Gold, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Сделанная вещь строкой 40dp: иконка в рамке редкости, имя её цвета, вид и уровень справа; касание - [onClick]. */
@Composable private fun MadeRow(game: GameUi, item: ItemInstance, onClick: () -> Unit) {
    val view = game.view(item)
    val tone = rarityColor(item.rarity.name)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp).clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(32.dp).background(Abyss, RoundedCornerShape(6.dp)).border(1.dp, tone.copy(alpha = .7f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            view?.let { ItemIcon(it, tone, Modifier.size(22.dp)) }
        }
        Column(Modifier.weight(1f)) {
            Text(view?.title ?: equipmentTitle(item.template), color = tone, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            view?.let { Text(it.weaponType?.let(::weaponTitle) ?: slotTitle(it.slot), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        }
        view?.let { Text(ui("crafts.made_level", it.level), color = Parchment, style = MaterialTheme.typography.labelMedium) }
    }
}

/** Время в работе живьём: свои часы - перерисовывается только это число, не книга учёта вокруг. */
@Composable private fun WorkTime(startedAt: Long, offset: Long) {
    val now by rememberNow(CLOCK_TICK_MS)
    TileFigure(AnnotatedString(workAge(now + offset - startedAt)), Parchment)
}

/** Итог плиткой: число [figure] по центру, под ним подпись [caption]. */
@Composable private fun LedgerTile(caption: String, modifier: Modifier, figure: @Composable () -> Unit) {
    Column(
        modifier.background(Abyss, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        figure()
        Text(caption, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Число плитки одной строкой цифрами одной ширины: не влезает - кегль от [TILE_MAX] вниз до [TILE_MIN], без переноса. */
@Composable private fun TileFigure(text: AnnotatedString, color: Color) {
    BasicText(
        text,
        style = MaterialTheme.typography.titleSmall.copy(color = color, fontWeight = FontWeight.Bold, fontSize = TILE_MAX, fontFeatureSettings = "tnum", textAlign = TextAlign.Center),
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = TILE_MIN, maxFontSize = TILE_MAX, stepSize = TILE_STEP),
    )
}

/** Кегль числа плитки и его наименьший; приписка «−впустую» у циклов - мельче. */
private val TILE_MAX = 15.sp
private val TILE_MIN = 10.sp
private val TILE_STEP = 0.5.sp
private val TILE_NOTE = 11.sp

/**
 * Строка стопки: иконка, имя, мини-полоса доли [amount] от общей шкалы [scale] и число - «+N» зелёным для добытого ([gathered]),
 * «−N» красным для потраченного. Касание - [onClick].
 */
@Composable private fun LedgerRow(game: GameUi, code: String, amount: Long, scale: Long, gathered: Boolean, onClick: () -> Unit) {
    val tone: Color = if (gathered) Vital else LifeRed
    val kind = game.index?.item(ItemCode(code))?.let(::bagVisualKind) ?: ItemVisualKind.ITEM
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BagIcon(code, Modifier.size(18.dp), kind = kind)
        Text(itemTitle(code), color = Parchment, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(NAME_WEIGHT))
        Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(PanelRaised)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((amount.toFloat() / scale).coerceIn(MIN_BAR, 1f)).clip(RoundedCornerShape(3.dp)).background(tone))
        }
        Text(
            (if (gathered) "+" else "−") + amount,
            color = tone,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 44.dp),
        )
    }
}

/** Имя стопки шире мини-полосы. */
private const val NAME_WEIGHT = 1.3f

/** Мельчайшая видимая доля полосы: малая стопка не пропадает. */
private const val MIN_BAR = .03f

/** Шаг живых часов «В работе». */
private const val CLOCK_TICK_MS = 1_000L

/** Сколько сделанных вещей видно до «Ещё N». */
private const val MADE_SHOWN = 5
