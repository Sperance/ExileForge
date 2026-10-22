package com.sperance.exileforge.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.character.GearVerdict
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.PropertyValue
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.stateTitle
import com.sperance.exileforge.core.display.weaponTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*

/**
 * Вещь в списке - «Плита» (выбор владельца): гнездо с иконкой в цвете редкости и уровнем на углу ([LevelBadge], как у плитки сумки),
 * имя серифами, под ним номер экземпляра уникальной или мифической, строка «что это · база», а ниже - каждая строка вещи со значком тира:
 * модификаторы видны всегда, не открывая карточку. Мифическая плита лежит под звёздами, уникальная - в тёплом угле.
 * [trailing] - напротив имени (цена лота), иначе [price] - что платит торговец; [footer] - под всем, что добавляет список.
 * [compact] - две линии (лут после боя): имя целиком, под ним значки тиров и отметки, без текстов строк.
 *
 * [worn] - надетое или вставленное: строка в золотой рамке, значок на иконке. [unwearable] - причины правил, почему сейчас не надеть.
 * [extra] - факты после слота, [trailing] - напротив имени (цена лота), [price] - цена торговца, когда напротив имени больше ничего,
 * [footer] - строка под свойствами. [locked] - замок, [waiting] - команда ждёт сети. Две линии вместо плиты - лут после боя; «Ценник» -
 * ярлык цены сверху плиты; «лучше или хуже» - только у вещей, что герой может надеть. Удержание строки - пометка к продаже или карточка;
 * свёртка строк - все сразу или под чипом «Свойства · N». Вторая линия лута - значки тиров, качество, состояния и замок; уровень -
 * только на гнезде; каждая строка вещи видна без карточки. [accent] - цвет рамки метки строки ([note]): «лучше надетого» в походе.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ItemRow(
    item: ItemView,
    note: String? = null,
    noteColor: Color = Gold,
    selected: Boolean = false,
    enabled: Boolean = true,
    worn: Boolean = false,
    unwearable: List<String> = emptyList(),
    facts: List<String> = emptyList(),
    trailing: (@Composable () -> Unit)? = null,
    price: Long? = null,
    footer: @Composable (ColumnScope.() -> Unit)? = null,
    locked: Boolean = item.item.locked,
    waiting: Boolean = false,
    compact: Boolean = false,
    tag: (@Composable RowScope.() -> Unit)? = null,
    verdict: GearVerdict? = null,
    onLongClick: (() -> Unit)? = null,
    lines: LineFold = LineFold.OPEN,
    accent: Color? = null,
    onClick: () -> Unit,
) {
    val look = relicLook(item.rarity)
    val legend = look.legend
    val card = RoundedCornerShape(16.dp)
    val base = item.figures
    val sub = (
        listOf(item.weaponType?.let { weaponTitle(it) } ?: slotTitle(item.slot)) + facts +
            base.take(2).map { "${statTitle(it.stat).lowercase()} ${it.text}" }
        ).joinToString(" · ")
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().glow(Gold, on = selected, radius = 10.dp, shape = card).shadow(8.dp, card, clip = false).clip(card)
                .background(
                    when {
                        legend != null -> Brush.verticalGradient(listOf(look.top, look.bottom))
                        worn -> Brush.verticalGradient(listOf(Gold.copy(alpha = .10f).compositeOver(DepthTop), Gold.copy(alpha = .06f).compositeOver(DepthBottom)))
                        else -> Brush.verticalGradient(listOf(DepthTop, DepthBottom))
                    },
                )
                .border(
                    1.dp,
                    when {
                        selected -> Gold
                        accent != null -> accent.copy(alpha = .7f)
                        worn -> Gold.copy(alpha = .5f)
                        legend != null -> look.gold.copy(alpha = .55f)
                        else -> look.rarity.copy(alpha = .2f)
                    },
                    card,
                )
                .combinedClickable(enabled = enabled, onLongClick = onLongClick, onClick = onClick).padding(start = 12.dp, end = 12.dp, top = if (tag != null) 16.dp else 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RowSocket(item, look, worn, locked, unwearable)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        item.title,
                        color = look.name,
                        style = itemName(15),
                        maxLines = if (compact) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.item.serial > 0) SerialLine(item.item.serial, look)
                    if (!compact) Text(sub, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    note?.let { Text(it, color = noteColor, style = MaterialTheme.typography.labelSmall) }
                    trailing?.invoke() ?: price?.let { GoldPrice(it) }
                    verdict?.let { GearVerdictBadge(it) }
                    if (!compact) item.summary.quality?.let { RollPill(it) }
                }
            }
            if (compact) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    item.orderedLines.forEach { TierHex(it.marks, 16.dp) }
                    if (item.orderedLines.isNotEmpty()) Spacer(Modifier.width(3.dp))
                    QualityBadge(item, compact = true)
                    item.summary.quality?.let { RollPill(it) }
                    RowStates(item)
                }
            } else {
                if (item.states.isNotEmpty() || waiting) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        QualityBadge(item, compact = true)
                        RowStates(item)
                        if (waiting) PendingMark()
                    }
                } else {
                    QualityBadge(item, compact = true)
                }
                if (unwearable.isNotEmpty()) Text(requirementReason(unwearable.first()), color = Color(0xFFFF8F88), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                if (item.orderedLines.isNotEmpty()) FoldedLines(item.orderedLines, lines, look)
            }
            footer?.invoke(this)
        }
        tag?.let { PriceTag(Modifier.align(Alignment.TopEnd).padding(end = 12.dp), it) }
    }
}

/** Ярлык цены «Ценника»: свисает с верхнего края плиты, скруглён снизу, в тонкой золотой нити. */
@Composable private fun PriceTag(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    val shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
    Row(
        modifier.height(26.dp).background(Color(0xFF1A242C), shape).border(1.dp, Color(0x40E2C15A), shape).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        content = content,
    )
}

/**
 * Гнездо плиты: иконка в цвете редкости, у мифической - круглое; поверх - запрет (слева сверху), замок (справа сверху), отметка
 * надетого (слева снизу) и уровень вещи ([LevelBadge], справа снизу - тот же значок и сдвиг, что у плитки сумки).
 *
 * Отметка на строке одна: чего не хватает, говорит карточка или подсказка отметки.
 */
@Composable private fun RowSocket(item: ItemView, look: RelicLook, worn: Boolean, locked: Boolean, unwearable: List<String>) {
    val shape = if (look.legend == RelicLook.Legend.STARS) CircleShape else RoundedCornerShape(12.dp)
    Box(
        Modifier.size(44.dp).then(if (look.legend != null) Modifier.glow(look.glow.copy(alpha = .4f), radius = 7.dp, shape = shape) else Modifier)
            .background(Brush.radialGradient(listOf(Color(0xFF222B30), Color(0xFF0B1013))), shape)
            .border(1.dp, look.rarity.copy(alpha = .55f), shape),
        contentAlignment = Alignment.Center,
    ) {
        ItemIcon(item, look.rarity, Modifier.size(28.dp))
        if (unwearable.isNotEmpty()) {
            Tipped(
                { Tip(ui("hero.inactive"), unwearable.joinToString("\n") { requirementReason(it) }, LifeRed) },
                Modifier.align(Alignment.TopStart).offset((-4).dp, (-4).dp),
            ) { Icon(Icons.Outlined.Block, ui("hero.inactive"), tint = LifeRed, modifier = Modifier.background(Ink, CircleShape).size(14.dp)) }
        }
        if (locked) {
            Icon(Icons.Outlined.Lock, ui("item.locked"), tint = GoldBright, modifier = Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).background(Ink, CircleShape).padding(1.dp).size(12.dp))
        }
        if (worn) {
            Icon(
                Icons.Outlined.CheckCircle,
                ui("row.worn"),
                tint = Ink,
                modifier = Modifier.align(Alignment.BottomStart).offset((-4).dp, 4.dp).background(Gold, CircleShape).padding(1.dp).size(14.dp),
            )
        }
        LevelBadge(item, look, Modifier.align(Alignment.BottomEnd).offset(x = LevelBadgeOffset.x, y = LevelBadgeOffset.y))
    }
}

/** «◆ № 41 ◆ ──»: номер экземпляра под именем в плите. */
@Composable private fun SerialLine(serial: Long, look: RelicLook) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "◆ № $serial ◆",
            color = UniqueGoldInk,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = RelicSerif, fontWeight = FontWeight.Bold),
        )
        Box(Modifier.width(28.dp).height(1.dp).background(look.gold.copy(alpha = .3f)))
    }
}

/** Золото номера экземпляра. */
private val UniqueGoldInk = Color(0xFFF0C76A)

/** Состояния вещи значками с подсказкой: порча, отражение, влияние, ремесло. */
@Composable private fun RowStates(item: ItemView) {
    item.states.forEach { state ->
        Tipped({ Tip(stateTitle(state), tint = stateColor(state)) }) { Icon(stateGlyph(state), stateTitle(state), tint = stateColor(state), modifier = Modifier.size(13.dp)) }
    }
}

/** One base figure: the number bold, coloured when a local modifier moved it, and what it counts. */
@Composable internal fun BaseChip(value: PropertyValue) {
    Row(
        Modifier.background(Abyss, RoundedCornerShape(4.dp)).padding(horizontal = 7.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(value.text, color = if (value.augmented) Rune else GoldBright, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        if (value.stat.isNotBlank()) {
            Text(
                statTitle(value.stat),
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The drawing of a state, and its colour.
 *
 * A flag the server grows tomorrow gets the neutral sigil rather than nothing, so a row never
 * silently drops a state it has no picture for.
 */
internal fun stateGlyph(state: String) = when (state) {
    "corrupted" -> ForgeGlyphs.Skull
    "mirrored" -> ForgeGlyphs.Chain
    "shaper" -> ForgeGlyphs.Constellation
    "elder" -> ForgeGlyphs.Portal
    "abyss" -> ForgeGlyphs.Rift
    "fractured" -> ForgeGlyphs.Shard
    "crafted" -> ForgeGlyphs.Anvil
    "equipped" -> ForgeGlyphs.Helm
    "socketed" -> ForgeGlyphs.Gem
    else -> ForgeGlyphs.Sigil
}

internal fun stateColor(state: String) = when (state) {
    "corrupted" -> LifeRed
    "equipped" -> Gold
    "mirrored", "socketed" -> Rune
    "shaper" -> Shaper
    "elder" -> Elder
    "abyss" -> AbyssGlow
    "fractured" -> Fractured
    "crafted" -> Crafted
    else -> Muted
}
