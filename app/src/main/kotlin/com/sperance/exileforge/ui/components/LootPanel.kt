package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Rune

/** Сумма добычи чипом [LootChip]: текст и его цвет. */
@Immutable
data class LootSum(val text: String, val tone: Color)

/** Часть добычи [LootPanel], над которой место может поставить подпись: вещи, стопки, суммы. */
enum class LootPart { PIECES, STACKS, SUMS }

/** Суммы добычи по порядку: золото [gold] и опыт [experience]; нулевые не печатаются. */
fun lootSums(gold: Long, experience: Double): List<LootSum> = buildList {
    if (gold > 0) add(LootSum(ui("expedition.loot_gold", compactNumber(gold)), GoldBright))
    if (experience > 0) add(LootSum(ui("expedition.loot_experience", number(experience)), Rune))
}

/**
 * Добыча одного места выдачи - один облик везде: итоги боя и карты, сумка захода, награды испытаний, Разлома и Бездны, поражение,
 * сундуки, письма, сделанное ремеслом, награды заданий и сюжета; стопки и их ценность - по контенту [index]. По порядку: вещи [pieces] короткими строками (строку [piece] рисует
 * место - его метки, продажа и сравнение, сама строка - [LootRow]), стопки [stacks] сеткой ячеек [StackCell] размера [LootCellSize]
 * (большие стопки первыми; касание - [onStack]), суммы [sums] чипами. Пустая часть не печатается, её подписи [heading] - тоже.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> LootPanel(
    index: ContentIndex?,
    pieces: List<T>,
    stacks: Map<String, Long>,
    sums: List<LootSum>,
    onStack: (String) -> Unit,
    modifier: Modifier = Modifier,
    heading: (@Composable (LootPart) -> Unit)? = null,
    piece: @Composable (T) -> Unit,
) {
    val sorted = remember(stacks) { stacks.entries.filter { it.value > 0 }.sortedByDescending { it.value } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(LOOT_GAP)) {
        if (pieces.isNotEmpty()) {
            heading?.invoke(LootPart.PIECES)
            pieces.forEach { piece(it) }
        }
        if (sorted.isNotEmpty()) {
            heading?.invoke(LootPart.STACKS)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(CELL_GAP), verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                sorted.forEach { (code, amount) -> StackCell(index, code, amount, size = LootCellSize, onClick = { onStack(code) }) }
            }
        }
        if (sums.isNotEmpty()) {
            heading?.invoke(LootPart.SUMS)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(CELL_GAP), verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                sums.forEach { LootChip(it.text, it.tone) }
            }
        }
    }
}

/** Добыча без вещей - стопки [stacks] ячейками и суммы [sums] чипами в облике [LootPanel] (награда задания, добытое ремеслом). */
@Composable
fun LootPanel(index: ContentIndex?, stacks: Map<String, Long>, sums: List<LootSum>, onStack: (String) -> Unit, modifier: Modifier = Modifier) {
    LootPanel<Nothing>(index, emptyList(), stacks, sums, onStack, modifier) {}
}

/** Чип суммы добычи и прочих чисел итогов (убито, рецепт): текст цветом [tone] в тёмной рамке; с [onClick] - кнопка в рамке его цвета. */
@Composable fun LootChip(text: String, tone: Color = Parchment, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(3.dp)
    Text(
        text,
        color = tone,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.clip(shape).then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .background(Abyss, shape).border(1.dp, if (onClick != null) tone.copy(alpha = .6f) else PanelRaised, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** Зазор между частями добычи и строками вещей. */
private val LOOT_GAP = 8.dp

/** Зазор между ячейками стопок и чипами. */
private val CELL_GAP = 6.dp
