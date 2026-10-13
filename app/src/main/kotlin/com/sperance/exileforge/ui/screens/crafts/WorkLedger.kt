package com.sperance.exileforge.ui.screens.crafts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.rules.roll.WorkTally
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Vital
import com.sperance.exileforge.ui.theme.rarityColor
import kotlinx.coroutines.delay

/*
 * Книга учёта ремесла (4.x, макет «Ремёсла: плашка „С начала работы“», вариант C): итоги с иконками в две колонки - время (идёт
 * вживую), циклы (−впустую), опыт (▲уровни), сделано; затем каждая стопка строкой - иконка, имя, мини-полоса на общей шкале и
 * сколько: добытое зелёным, потраченное красным, под чертой; сделанные вещи - чипами цвета редкости. Касание стопки или вещи
 * открывает её карточку.
 */

/** Итог работы для книги учёта: и с начала работы ([WorkTally] сервера), и за сессию ([WorkGains] клиента). */
internal class WorkSums(
    val cycles: Long,
    val nothing: Long,
    val experience: Double,
    val levels: Int,
    val made: Long,
    val gathered: Map<String, Long>,
    val spent: Map<String, Long>,
) {
    /** Общая шкала мини-полос: самая большая стопка, добытая или потраченная. */
    val scale: Long get() = (gathered.values + spent.values).maxOrNull()?.coerceAtLeast(1) ?: 1

    val isEmpty: Boolean get() = gathered.isEmpty() && spent.isEmpty()

    companion object {
        fun of(tally: WorkTally) = WorkSums(tally.cycles, tally.nothing, tally.experience, tally.levels, tally.made, tally.items, tally.spent)

        fun of(gains: WorkGains) = WorkSums(
            gains.cycles.toLong(),
            gains.nothing.toLong(),
            gains.experience,
            gains.levels,
            gains.equipment.size.toLong(),
            gains.items,
            gains.spent,
        )
    }
}

/**
 * Книга учёта: итоги [sums] с иконками, под чертой [title] строки стопок и чипы сделанного [made]. [startedAt] > 0 - есть время в
 * работе: оно идёт по часам сервера (сдвиг [offset]); ноль - итог сессии, без времени. [makes] - работа делает вещи: есть итог «сделано».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WorkLedger(game: GameUi, title: String, sums: WorkSums, made: List<ItemInstance>, startedAt: Long = 0, offset: Long = 0, makes: Boolean = sums.made > 0) {
    val inspect = rememberInspect()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = 2, verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val cell = Modifier.weight(1f)
            if (startedAt > 0) {
                val now by produceState(System.currentTimeMillis()) {
                    while (true) {
                        value = System.currentTimeMillis()
                        delay(CLOCK_TICK_MS)
                    }
                }
                LedgerFigure(Icons.Outlined.Schedule, ui("crafts.totals_time"), cell) { Text(duration(now + offset - startedAt), color = Parchment) }
            }
            LedgerFigure(Icons.Outlined.Autorenew, ui("crafts.totals_cycles"), cell) {
                val empty = MaterialTheme.typography.labelSmall.toSpanStyle().copy(color = LifeRed)
                Text(
                    buildAnnotatedString {
                        append(sums.cycles.toString())
                        if (sums.nothing > 0) withStyle(empty) { append("  −${sums.nothing}") }
                    },
                    color = Parchment,
                )
            }
            LedgerFigure(Icons.Outlined.StarOutline, ui("crafts.totals_experience"), cell) {
                Text("+" + number(sums.experience) + (if (sums.levels > 0) " ▲${sums.levels}" else ""), color = Vital)
            }
            // «Сделано» - только у работ, что делают вещи (4.10.x, [JobKind.makesItems]): у добычи и стопок оно всегда ноль
            if (makes) LedgerFigure(ForgeGlyphs.Anvil, ui("crafts.totals_made"), cell) { Text(sums.made.toString(), color = Parchment) }
        }
        SectionRule(title)
        if (sums.isEmpty) {
            Text(ui("crafts.totals_empty"), color = Muted, style = MaterialTheme.typography.labelSmall)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                sums.gathered.entries.sortedByDescending { it.value }.forEach { (code, amount) ->
                    LedgerRow(game, code, amount, sums.scale, gathered = true) { inspect(Inspect.Stack(code)) }
                }
                if (sums.gathered.isNotEmpty() && sums.spent.isNotEmpty()) HorizontalDivider(Modifier.padding(vertical = 2.dp), color = Bronze)
                sums.spent.entries.sortedByDescending { it.value }.forEach { (code, amount) ->
                    LedgerRow(game, code, amount, sums.scale, gathered = false) { inspect(Inspect.Stack(code)) }
                }
            }
        }
        if (made.isNotEmpty()) {
            SectionRule(ui("crafts.totals_made"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                made.forEach { item -> TallyChip(equipmentTitle(item.template), null, rarityColor(item.rarity.name)) { inspect(Inspect.Copy(item)) } }
            }
        }
    }
}

/** Итог с иконкой: значок в плашке 28dp, под числом [figure] - подпись [caption]. */
@Composable private fun LedgerFigure(icon: ImageVector, caption: String, modifier: Modifier, figure: @Composable () -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(PanelRaised), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(16.dp))
        }
        Column {
            ProvideTextStyle(MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)) { figure() }
            Text(caption, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

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
