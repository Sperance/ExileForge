package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.roman
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.BoonState
import com.sperance.exileforge.presentation.state.FateSight
import com.sperance.exileforge.presentation.state.FatedLine
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.boonText
import com.sperance.exileforge.presentation.state.fateTitle
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.coroutines.delay

/*
 * Осквернение Предначертания на карточке вещи (4.6.2, утверждено владельцем): запечатанная строка - «Руническая печать» (вариант
 * A) - тлеющий сургуч и руны, что перебираются шифром; раскрытая - строка дара в одном из состояний (действует, спит, перебита
 * сильнейшей). Декор (пульс печати, шифр) стоит при выключенном `LocalMotion`.
 */

/** Судьбоносные строки глазами героя на экране; null - героя или контента нет, строки не рисуются. */
val LocalFateSight = compositionLocalOf<FateSight?> { null }

/** Хозяин [LocalFateSight]: дар героя и его надетое - всё внутри читает состояние строк вещей отсюда. */
@Composable fun FateSightHost(game: GameUi, content: @Composable () -> Unit) {
    val hero = game.hero
    val sight = remember(game.index, hero?.info?.fate, hero?.items) { FateSight.of(game.index, hero?.info?.fate, hero?.equipped?.values.orEmpty()) }
    CompositionLocalProvider(LocalFateSight provides sight, content = content)
}

/** Судьбоносная строка вещи [item] в её карточке: печать, пока строка не раскрыта, иначе строка со своим состоянием. */
@Composable fun FatedSlot(item: ItemInstance, modifier: Modifier = Modifier) {
    val desecration = item.desecration ?: return
    val sight = LocalFateSight.current ?: return
    when (val line = sight.line(item)) {
        null -> RuneSeal(pending = desecration.offers.isNotEmpty(), modifier)
        else -> FatedRow(sight, line, modifier)
    }
}

/** «Руническая печать»: сургуч с ✦ дышит жаром, рядом строка рун-шифра и подпись - чем раскрыть или что выбор ждёт. */
@Composable private fun RuneSeal(pending: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.radialGradient(listOf(FateInk.Gold.copy(alpha = .14f), Color.Transparent), Offset.Zero, 600f))
            .border(1.dp, FateInk.Gold.copy(alpha = .35f), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WaxSeal()
        Column(Modifier.weight(1f)) {
            CipherRunes()
            Text(ui(if (pending) "fate.seal_pending" else "fate.seal_caption"), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Сургуч печати: жар пульсирует ([SEAL_MS]), без анимаций - ровное свечение. */
@Composable private fun WaxSeal() {
    val time = motionClock(SEAL_MS, "fate-seal")
    val beat = if (LocalMotion.current) (1 - kotlin.math.cos(time * 2 * Math.PI).toFloat()) / 2 else .5f
    Box(
        Modifier.size(26.dp).drawBehind {
            val radius = size.minDimension / 2
            drawCircle(FateInk.Cinder.copy(alpha = .25f + .4f * beat), radius * (1.2f + .25f * beat))
            drawCircle(Brush.radialGradient(FateInk.Wax, Offset(size.width * .35f, size.height * .35f), radius * 1.5f), radius)
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(SIGIL, color = FateInk.WaxInk, fontSize = 13.sp)
    }
}

/** Руны, что перебираются шифром каждые [CIPHER_MS]: каждая четвёртая подменяется; без анимаций - стоят исходные. */
@Composable private fun CipherRunes() {
    var tick by remember { mutableIntStateOf(0) }
    if (LocalMotion.current) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(CIPHER_MS)
                tick++
            }
        }
    }
    Text(
        remember(tick) { cipher(tick) },
        style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 14.sp, letterSpacing = 2.5.sp, color = FateInk.Gold, shadow = Shadow(FateInk.Gold.copy(alpha = .6f), blurRadius = 16f)),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
    )
}

/** Строка шифра на шаге [tick]: исходные руны, часть подменена детерминированным шумом шага. */
private fun cipher(tick: Int): String = buildString {
    CIPHER.forEachIndexed { i, rune ->
        if (rune == 0) {
            append(' ')
        } else {
            val noise = ((i + 1) * 7919 + tick * 104729).let { it xor (it ushr 7) } and Int.MAX_VALUE
            appendCodePoint(if (tick > 0 && noise % 4 == 0) RUNE_FIRST + noise / 4 % RUNE_COUNT else rune)
        }
    }
}

/** Как рисуется раскрытая строка в своём состоянии: чернила, заливка, черта слева (пунктир - перебита). */
private data class BoonLook(val ink: Color, val wash: Color, val edge: Color, val dashed: Boolean = false)

private fun look(state: BoonState): BoonLook = when (state) {
    BoonState.ACTIVE, BoonState.KIN -> BoonLook(FateInk.GoldLight, FateInk.Gold.copy(alpha = .16f), FateInk.Gold)
    BoonState.SLEEPING -> BoonLook(FateInk.AshInk, FateInk.AshInk.copy(alpha = .08f), FateInk.Ash)
    BoonState.OUTDONE -> BoonLook(FateInk.CinderInk, FateInk.Cinder.copy(alpha = .08f), FateInk.Cinder, dashed = true)
}

/** Подпись под строкой: дар и что с ней сейчас. */
private fun caption(line: FatedLine): String = when (line.state) {
    BoonState.ACTIVE -> ui("fate.boon_active", fateTitle(line.boon.fate))
    BoonState.KIN -> fateTitle(line.boon.fate)
    BoonState.SLEEPING -> ui("fate.boon_sleeping", fateTitle(line.boon.fate))
    BoonState.OUTDONE -> ui("fate.boon_outdone", line.rival?.let { slotTitle(it) }.orEmpty())
}

/** Раскрытая строка: ✦, текст словами сервера, плашка тира и подпись состояния. */
@Composable private fun FatedRow(sight: FateSight, line: FatedLine, modifier: Modifier) {
    val look = look(line.state)
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.horizontalGradient(listOf(look.wash, Color.Transparent)))
            .drawBehind {
                val x = 1.5.dp.toPx()
                val dash = if (look.dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null
                drawLine(look.edge, Offset(x, 0f), Offset(x, size.height), 3.dp.toPx(), pathEffect = dash)
            }
            .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$SIGIL ${boonText(sight.index, line.boon)}", color = look.ink, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TierChip(line.boon.tier, look.ink)
        }
        Text(caption(line), color = look.ink.copy(alpha = .85f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
    }
}

/** Плашка тира строки римской цифрой в нити цвета строки. */
@Composable internal fun TierChip(tier: Int, ink: Color) {
    Text(
        roman(tier),
        color = ink,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 6.dp).border(1.dp, ink, CircleShape).padding(horizontal = 6.dp),
    )
}

/** Пульс сургуча и шаг шифра, мс. */
private const val SEAL_MS = 2_400
private const val CIPHER_MS = 140L

/** Руны шифра (блок Unicode «Руны»): первая и сколько их; исходная строка - три слова, 0 - пробел. */
private const val RUNE_FIRST = 0x16A0
private const val RUNE_COUNT = 0x4B
private val CIPHER = intArrayOf(
    0x16A0, 0x16DF, 0x16B1, 0x16CF, 0x16A2, 0x16BE, 0x16A8, 0,
    0x16DE, 0x16D6, 0x16CB, 0x16CF, 0x16C1, 0x16BE, 0x16DF, 0,
    0x16B9, 0x16D6, 0x16B1, 0x16DE,
)
