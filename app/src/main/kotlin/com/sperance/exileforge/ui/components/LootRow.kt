package com.sperance.exileforge.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.character.GearVerdict
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.weaponTitle
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment

/**
 * Метка короткой строки [text] цветом [color] (`▲ Лучше надетого`, «Новая фляга»): после имени, рамка строки - её цветом.
 */
data class RowNote(val text: String, val color: Color)

/**
 * Короткая строка вещи - «Колонка цифр» (выбор владельца), одна для всех мест коротких вещей: лист добычи боя, итоги захода и сумка
 * похода, награды механик, лист «Из сундуков», итог ремесла, вложения письма. Первая линия - гнездо 44 dp с уровнем ([RowSocket]), имя
 * в одну строку с многоточием и метка [note]; вторая - только тип (оружие или место) и номер экземпляра легенды ([SerialMark]); справа
 * узкая колонка цифр: вердикт Мощи к надетому ([GearVerdictArrow]) над ценой торговца [price]. Строк вещи нет - они в карточке.
 *
 * Касание - [onClick] (полная карточка вещи), удержание - [onLongClick] (метка продажи). [selected] - выбрана, [worn] - надета (отметка
 * на гнезде, золотая подложка), [trailing] - знак после имени (монета метки продажи).
 */
@Composable fun LootRow(
    item: ItemView,
    verdict: GearVerdict? = null,
    price: Long? = null,
    note: RowNote? = null,
    selected: Boolean = false,
    worn: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val look = relicLook(item.rarity)
    LootRowFrame(
        modifier.itemPlate(look, selected, worn, note?.color),
        socket = { RowSocket(item, look, worn, item.item.locked, emptyList()) },
        title = {
            LootName(item.title, look.name, Modifier.weight(1f, fill = false))
            note?.let { Text(it.text, color = it.color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1) }
            trailing?.invoke()
        },
        kind = {
            LootKind(item.weaponType?.let { weaponTitle(it) } ?: slotTitle(item.slot), Modifier.weight(1f, fill = false))
            SerialMark(item.item.serial, look.accent, MaterialTheme.typography.labelSmall)
        },
        figures = {
            verdict?.let { GearVerdictArrow(it) }
            price?.let { GoldPrice(it) }
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

/**
 * Короткая строка стопки [code] ×[amount] - та же «Колонка цифр», что [LootRow]: гнездо со значком стопки и числом, имя, ниже - короткое
 * описание стопки; касание - [onClick] (карточка стопки).
 */
@Composable fun LootStackRow(index: ContentIndex?, code: String, amount: Long, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val known = index?.item(ItemCode(code))
    val description = remember(code, index) { itemDescription(code, index) }
    LootRowFrame(
        modifier.itemPlate(relicLook(Rarity.COMMON), selected = false, worn = false, accent = null),
        socket = {
            Box(Modifier.size(LootSocket).background(Ink, StackSocketShape).border(1.dp, Gold.copy(alpha = .35f), StackSocketShape), contentAlignment = Alignment.Center) {
                BagIcon(code, Modifier.size(28.dp), tint = if (known != null) Gold else Muted, kind = known?.let(::bagVisualKind) ?: ItemVisualKind.ITEM)
                if (amount > 1) {
                    Text(
                        "×$amount",
                        color = GoldBright,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp),
                    )
                }
            }
        },
        title = { LootName(itemTitle(code), Parchment, Modifier.weight(1f, fill = false)) },
        kind = { if (description.isNotBlank()) LootKind(description, Modifier.weight(1f, fill = false)) },
        figures = {},
        onLongClick = null,
        onClick = onClick,
    )
}

/** Каркас короткой строки: подложка [plate], гнездо [socket], линия имени [title], линия типа [kind] и колонка цифр [figures]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LootRowFrame(
    plate: Modifier,
    socket: @Composable BoxScope.() -> Unit,
    title: @Composable RowScope.() -> Unit,
    kind: @Composable RowScope.() -> Unit,
    figures: @Composable ColumnScope.() -> Unit,
    onLongClick: (() -> Unit)?,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().then(plate).combinedClickable(onLongClick = onLongClick, onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(content = socket)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), content = title)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), content = kind)
        }
        Column(Modifier.width(FiguresWidth), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp), content = figures)
    }
}

/** Имя короткой строки: одна линия с многоточием, полное - в карточке. */
@Composable private fun LootName(text: String, color: Color, modifier: Modifier) {
    Text(text, color = color, style = itemName(15), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

/** Вторая линия короткой строки: тип вещи или описание стопки, одна линия с многоточием. */
@Composable private fun LootKind(text: String, modifier: Modifier) {
    Text(text, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

/** Ширина колонки цифр короткой строки: вердикт над ценой ровно по краю во всех строках. */
private val FiguresWidth = 66.dp

/** Сторона гнезда стопки - как у гнезда вещи [RowSocket]. */
private val LootSocket = 44.dp

/** Скругление гнезда стопки. */
private val StackSocketShape = RoundedCornerShape(12.dp)
