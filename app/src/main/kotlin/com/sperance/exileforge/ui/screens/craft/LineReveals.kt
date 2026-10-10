package com.sperance.exileforge.ui.screens.craft

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.roman
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.boonFigure
import com.sperance.exileforge.presentation.state.boonText
import com.sperance.exileforge.presentation.state.fateTitle
import com.sperance.exileforge.rules.content.FateBoon
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.FateInk
import com.sperance.exileforge.ui.components.HoldToAccept
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.SIGIL
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment
import kotlinx.coroutines.delay

/**
 * Скрытая строка вещи, что ждёт выбора игрока (4.6.2, зеркало `Revelation` правил): сфера кладёт варианты на копию, выбор ставит
 * один. Маршрут выбора у всех один (`hero/unveil`) - его ведёт первая ждущая строка, так что и рисуется только она. Новая скрытая
 * строка - новая реализация в [REVEALS].
 */
internal interface LineReveal {
    /** Копия [item] ждёт выбора этой строки. */
    fun pending(item: ItemInstance): Boolean

    /** Выбор над копией [item]: [onChoose] - id копии и номер варианта. */
    @Composable fun Choice(game: GameUi, item: ItemInstance, enabled: Boolean, onChoose: (String, Int) -> Unit)
}

/** Скрытый аффикс (3.36.0): гибриды сферы раскрытия списком строк. */
internal object VeiledReveal : LineReveal {
    override fun pending(item: ItemInstance): Boolean = item.unveil.isNotEmpty()

    @Composable override fun Choice(game: GameUi, item: ItemInstance, enabled: Boolean, onChoose: (String, Int) -> Unit) = LineChoice(game, item, item.unveil, "forge.unveil_title", "forge.unveil_hint", enabled, onChoose)
}

/** Судьбоносная строка Осквернения (4.6.2): строки дара, что предложила Сфера прозрения, - «Тлеющие письмена». */
internal object FatedReveal : LineReveal {
    override fun pending(item: ItemInstance): Boolean = item.desecration?.offers?.isNotEmpty() == true

    @Composable override fun Choice(game: GameUi, item: ItemInstance, enabled: Boolean, onChoose: (String, Int) -> Unit) = EmberScript(game, item, item.desecration?.offers.orEmpty(), enabled, onChoose)
}

/** Порядок - как у правил (`OrbApplier.revelations`): первая ждущая строка ведёт выбор. */
internal val REVEALS: List<LineReveal> = listOf(VeiledReveal, FatedReveal)

/**
 * Выбор Раскрытия (4.6.2, макет «Тлеющие письмена», утверждён владельцем): заголовок, тлеющая полоса разворачивается, строки
 * дара прожигаются слева направо одна за другой (крупное число и текст словами сервера), выбранная - в золоте; принять -
 * удержанием. Без `LocalMotion` всё сразу на месте.
 */
@Composable private fun EmberScript(game: GameUi, item: ItemInstance, offers: List<FateBoon>, enabled: Boolean, onChoose: (String, Int) -> Unit) {
    val index = game.index ?: return
    val first = offers.firstOrNull() ?: return
    val motion = LocalMotion.current
    var picked by remember(item.id, offers) { mutableStateOf<Int?>(null) }
    val unroll = remember(item.id, offers) { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(unroll) { unroll.animateTo(1f, tween(UNROLL_MS, easing = FastOutSlowInEasing)) }
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(Panel, Abyss))).border(1.dp, Bronze, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            ui("fate.reveal_title"),
            style = relicName(17).copy(color = FateInk.GoldLight, shadow = Shadow(FateInk.Gold.copy(alpha = .4f), blurRadius = 12f)),
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            ui("fate.reveal_subtitle", fateTitle(first.fate), roman(first.tier)),
            color = Muted,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier.fillMaxWidth().padding(vertical = 2.dp).height(6.dp).graphicsLayer { scaleX = unroll.value }
                .background(Brush.horizontalGradient(listOf(Color.Transparent, FateInk.Cinder, FateInk.GoldLight, FateInk.Cinder, Color.Transparent)), RoundedCornerShape(3.dp)),
        )
        offers.forEachIndexed { i, boon ->
            EmberRow(boonFigure(index, boon) ?: SIGIL, boonText(index, boon), selected = picked == i, delayMs = i * BURN_STEP_MS, key = item.id to offers) { picked = i }
        }
        HoldToAccept(
            ui(if (picked == null) "fate.reveal_pick_first" else "fate.reveal_hold"),
            Modifier.fillMaxWidth().padding(top = 4.dp),
            enabled = enabled && picked != null,
        ) { picked?.let { onChoose(item.id, it) } }
    }
}

/**
 * Строка выбора: число крупно серифом, текст и кружок выбора; появляется, прожигаясь слева направо ([BURN_MS] после
 * [delayMs]) с яркой кромкой. Без `LocalMotion` - сразу целиком.
 */
@Composable private fun EmberRow(figure: String, text: String, selected: Boolean, delayMs: Int, key: Any, onClick: () -> Unit) {
    val motion = LocalMotion.current
    val burn = remember(key) { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(burn) {
        delay(delayMs.toLong())
        burn.animateTo(1f, tween(BURN_MS, easing = LinearEasing))
    }
    val shape = RoundedCornerShape(12.dp)
    val wash = if (selected) FateInk.Gold.copy(alpha = .2f) else FateInk.Cinder.copy(alpha = .06f)
    Row(
        Modifier.fillMaxWidth()
            .drawWithContent {
                val edge = size.width * burn.value
                clipRect(right = edge) { this@drawWithContent.drawContent() }
                // Кромка огня и вспышка ещё не остывшей строки
                if (burn.value < 1f) {
                    drawRect(Color.White.copy(alpha = .3f * (1 - burn.value)), size = Size(edge, size.height))
                    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, FateInk.GoldLight), edge - 24f, edge), Offset(edge - 24f, 0f), Size(24f, size.height))
                }
            }
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(wash, Panel)))
            .border(1.dp, if (selected) FateInk.Gold else Bronze, shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(figure, style = relicName(15).copy(color = FateInk.GoldLight), modifier = Modifier.widthIn(min = 52.dp))
        Text(text, color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(16.dp).border(2.dp, if (selected) FateInk.Gold else Muted, CircleShape).padding(3.dp)
                .background(if (selected) FateInk.Gold else Color.Transparent, CircleShape),
        )
    }
}

/** Развёртка полосы, прожиг строки и шаг между строками, мс. */
private const val UNROLL_MS = 800
private const val BURN_MS = 900
private const val BURN_STEP_MS = 500
