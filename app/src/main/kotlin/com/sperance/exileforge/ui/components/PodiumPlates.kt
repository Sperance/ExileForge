package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.romanNumeral
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Vital

/** Сколько мест у пьедестала: первое и два рядом. */
const val PODIUM_PLACES: Int = 3

/** Редкость плиты каждого места пьедестала: мифическая, уникальная, редкая. */
private val PodiumRarities = listOf(Rarity.MYTHICAL, Rarity.UNIQUE, Rarity.RARE)

/**
 * Плита места пьедестала: [place] - с единицы, [look] - облик редкости места, [big] - первое место во всю ширину. Начертания
 * надписей плиты ([nameStyle], [placeStyle]) растут с [big], цвета - из [look].
 */
@Immutable
class PodiumSlot internal constructor(val place: Int, val look: RelicLook, val big: Boolean) {
    /** Имя на плите. */
    val nameStyle: TextStyle get() = look.titleStyle(if (big) 20 else 15)

    /** Место римской цифрой. */
    internal val placeStyle: TextStyle get() = look.titleStyle(if (big) 28 else 21)
}

/**
 * Пьедестал - до [PODIUM_PLACES] первых строк [top]: первое место - плита во всю ширину, второе и третье - рядом, одной высоты. Плита -
 * подложка редкости места ([relicGround]), строка героя ([own]) - в золотой рамке, касание открывает карточку игрока [player] (null -
 * не открывает). Сверху плиты - место римской цифрой, ниже - [plate] строки по центру.
 */
@Composable fun <T> PodiumPlates(top: List<T>, own: (T) -> Boolean, player: (T) -> String?, modifier: Modifier = Modifier, plate: @Composable ColumnScope.(T, PodiumSlot) -> Unit) {
    val podium = top.take(PODIUM_PLACES)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        podium.firstOrNull()?.let { PodiumPlate(it, 1, own, player, Modifier.fillMaxWidth(), plate) }
        if (podium.size > 1) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                podium.drop(1).forEachIndexed { i, entry -> PodiumPlate(entry, i + 2, own, player, Modifier.weight(1f).fillMaxHeight(), plate) }
                if (podium.size == 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Одна плита места [place] строки [entry]. */
@Composable private fun <T> PodiumPlate(entry: T, place: Int, own: (T) -> Boolean, player: (T) -> String?, modifier: Modifier, plate: @Composable ColumnScope.(T, PodiumSlot) -> Unit) {
    val slot = PodiumSlot(place, relicLook(PodiumRarities[place - 1]), big = place == 1)
    Column(
        modifier.relicGround(slot.look, PlateShape, selected = own(entry)).clip(PlateShape).opensPlayer(player(entry))
            .padding(horizontal = 12.dp, vertical = if (slot.big) 16.dp else 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(romanNumeral(place), style = slot.placeStyle)
        plate(entry, slot)
    }
}

/**
 * Пьедестал строками - до [PODIUM_PLACES] строк [top] «I · имя · мера»: место цветом медали, имя ([name]) - у строки героя ([own])
 * цветом героя, мера ([measure]) золотом; касание строки - карточка игрока [player]. Пусто - ничего: что писать вместо, решает место.
 */
@Composable fun <T> PodiumLines(top: List<T>, name: (T) -> String, measure: (T) -> String, own: (T) -> Boolean, player: (T) -> String?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        top.take(PODIUM_PLACES).forEachIndexed { i, entry ->
            Row(Modifier.fillMaxWidth().opensPlayer(player(entry)).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(romanNumeral(i + 1), color = medalOf(i + 1), style = MaterialTheme.typography.labelLarge, modifier = Modifier.widthIn(min = 24.dp))
                Text(name(entry), color = if (own(entry)) Vital else Parchment, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(measure(entry), color = GoldBright, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Скругление плиты места. */
private val PlateShape = RoundedCornerShape(14.dp)
