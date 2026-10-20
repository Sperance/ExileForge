package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.combat.RiftGuard
import com.sperance.exileforge.core.campaign.run.BossHud
import com.sperance.exileforge.core.campaign.run.RageView
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.RiftLaw
import com.sperance.exileforge.ui.components.LocalLore
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.Lore
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.Tipped
import com.sperance.exileforge.ui.screens.expedition.scene.BlightTint
import com.sperance.exileforge.ui.theme.Ember
import com.sperance.exileforge.ui.theme.Muted
import kotlin.math.ceil

/** Палитра Разлома (RULES.md, «Стражи Разлома»): черта, ядовитая зелень, тревога. */
private val RiftLineBright = Color(0xFF2F5A3F)
private val RiftGreen = Color(0xFF39FF88)
private val RiftSoft = Color(0xFF9DFFB8)
private val RiftDeep = Color(0xFF0B3A21)
private val RiftAlarm = Color(0xFFFF6B4A)
private val RiftAlarmText = Color(0xFFFFD0C8)

/** Печать Разлома на запертом слоте героя - та же ядовитая зелень. */
internal val RiftSeal = RiftGreen

/** Мигание тревоги знамения Владыки, секунды полупериода. */
private const val ALARM_SECONDS = .5f

/** Вспышка разрыва пуповины, мс. */
private const val BURST_MS = 900

private val ChipShape = RoundedCornerShape(9.dp)

/**
 * Чип строки имени босса: капсула 18dp с рамкой [edge] на заливке [fill], слева - знак [glyph], текст [text] цвета [ink]
 * табулярными цифрами. Касание - выноска [tip] (null - без неё).
 */
@Composable private fun BossChip(
    text: String,
    ink: Color,
    edge: Color,
    fill: Color,
    tip: (() -> Tip)?,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    glyph: (@Composable () -> Unit)? = null,
) {
    Tipped(tip, modifier.height(18.dp).clip(ChipShape).background(fill).border(1.dp, edge, ChipShape).padding(horizontal = 6.dp), Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            glyph?.invoke()
            Text(text, color = ink, fontSize = 10.sp, fontWeight = weight, style = TabularDigits, maxLines = 1, softWrap = false)
        }
    }
}

/** Ромб-знак чипа цвета [tint]; [burst] < 1 - кольцо расходится и гаснет (читается в рисовании). */
@Composable private fun ChipDiamond(tint: Color, burst: () -> Float = { 1f }) {
    Canvas(Modifier.size(9.dp, 11.dp)) {
        val c = center
        val gem = Path().apply {
            moveTo(c.x, 0f)
            lineTo(size.width, c.y)
            lineTo(c.x, size.height)
            lineTo(0f, c.y)
            close()
        }
        drawPath(gem, tint)
        val k = burst()
        if (k < 1f) drawCircle(tint.copy(alpha = 1f - k), size.height / 2 * (1f + 1.6f * k), c, style = Stroke(1.5.dp.toPx()))
    }
}

/** Чип фазы: римский номер текущей фазы; касание - справка фаз босса [name]. */
@Composable internal fun PhaseChip(boss: BossHud, name: String) {
    val lore = LocalLore.current
    val phase = boss.phase
    BossChip(
        phaseRoman(phaseNumber(boss)),
        FrameGoldBright,
        Color(0xFF7A5C26),
        Color(0xFF2A1B10),
        null,
        if (lore != null && phase != null) Modifier.clickable { lore(Lore.Phase(phase, name)) } else Modifier,
        FontWeight.SemiBold,
    )
}

/** Чип ярости: ступень и отсчёт до следующей, до первой - приглушённый отсчёт; касание - строка ярости с прибавкой урона. */
@Composable internal fun RageChip(rage: RageView) {
    val next = rage.next?.let { ceil(it).toInt() }
    val text = when {
        rage.stage <= 0 -> ui("boss.rage_in", next ?: 0)
        next == null -> ui("boss.rage_full", rage.stage)
        else -> ui("boss.rage", rage.stage, next)
    }
    val tip = { Tip(rageText(rage), tint = Ember) }
    if (rage.stage > 0) {
        BossChip(text, Ember, Ember.copy(alpha = .55f), Ember.copy(alpha = .1f), tip)
    } else {
        BossChip(text, Muted, Color(0xFF33404A), Color.Transparent, tip)
    }
}

/** Название Закона по словарю сервера; «Одна стихия» - с её стихией. */
private fun lawName(law: RiftLaw, element: String?): String {
    val name = locOr("rift.law.${law.name}.name", law.name)
    return if (law == RiftLaw.ONE_ELEMENT && element != null) ui("boss.rift_law_element", name, ui("enum.damage.$element")) else name
}

/**
 * Чип стража Разлома [guard]: печати Стража «N/M», счёт украденного Поглотителем, «Закон» Владыки; в знамение Владыки -
 * отсчёт до смены тревогой, мигает ступенями по часам кадра в слое. Касание - механика словами: печати, украденные дары и
 * насыщение Эхом, Закон в силе и следующий.
 */
@Composable internal fun RiftChip(boss: BossHud, guard: RiftGuard, frame: FightFrame) {
    val motion = LocalMotion.current
    val omen = guard == RiftGuard.LORD && boss.lawNext.isNotEmpty()
    val seconds = ceil(boss.lawIn).toInt()
    val text = when (guard) {
        RiftGuard.WARDEN -> "${boss.seals}/${boss.sealsMax}"
        RiftGuard.DEVOURER -> boss.stolen.size.toString()
        RiftGuard.LORD -> if (omen) "$seconds ${ui("boss.seconds_short")}" else ui("boss.rift_law")
    }
    val tip = { riftTip(boss, guard) }
    val glyph: @Composable () -> Unit = { ChipDiamond(if (omen) RiftAlarm else RiftGreen) }
    if (omen) {
        BossChip(
            text,
            RiftAlarmText,
            RiftAlarm,
            RiftAlarm.copy(alpha = .18f),
            tip,
            Modifier.graphicsLayer { alpha = if (motion && (frame.time / ALARM_SECONDS).toInt() % 2 == 1) .55f else 1f },
            glyph = glyph,
        )
    } else {
        BossChip(text, RiftSoft, RiftLineBright, RiftDeep, tip, glyph = glyph)
    }
}

/** Выноска чипа Разлома: что сейчас делает механика стража [guard]. */
private fun riftTip(boss: BossHud, guard: RiftGuard): Tip = when (guard) {
    RiftGuard.WARDEN -> Tip(ui("boss.rift_seals", boss.seals, boss.sealsMax), tint = RiftGreen)

    RiftGuard.DEVOURER -> Tip(
        ui("boss.rift_stolen", boss.stolen.size),
        listOfNotNull(
            boss.stolen.takeIf { it.isNotEmpty() }?.joinToString(" · ") { loc("rift.boon.$it.name") },
            boss.devoured.takeIf { it > 0 }?.let { ui("boss.rift_devoured", number(it)) },
        ).joinToString("\n"),
        RiftGreen,
    )

    RiftGuard.LORD -> {
        val seconds = ceil(boss.lawIn).toInt()
        val now = boss.law.joinToString(" · ") { lawName(it, boss.lawElement) }.ifEmpty { ui("boss.rift_law_none") }
        val next = if (boss.lawNext.isNotEmpty()) {
            ui("boss.rift_law_omen", seconds, boss.lawNext.joinToString(" · ") { lawName(it, boss.lawNextElement) })
        } else {
            ui("boss.rift_law_in", seconds)
        }
        Tip(ui("boss.rift_law"), "$now\n$next", RiftGreen)
    }
}

/**
 * Чип «Сердца Скверны» Матери: целых пуповин из всех [BossHud.cords] (по одной на порог фазы и последняя на смерть); разрыв -
 * кольцо-вспышка над ромбом. Касание - счёт пуповин словами.
 */
@Composable internal fun BlightChip(boss: BossHud, alive: Boolean) {
    val whole = if (alive) boss.cords - boss.torn else 0
    var seen by remember { mutableIntStateOf(whole) }
    val burst = remember { Animatable(1f) }
    LaunchedEffect(whole) {
        if (whole < seen) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(BURST_MS, easing = LinearEasing))
        }
        seen = whole
    }
    BossChip(
        "$whole/${boss.cords}",
        BlightTint.hot,
        BlightTint.flesh,
        BlightTint.flesh.copy(alpha = .45f),
        { Tip(ui("boss.cords", whole, boss.cords), tint = BlightTint.hot) },
    ) { ChipDiamond(BlightTint.hot) { burst.value } }
}

/** Римский номер фазы: до шестой - римскими цифрами, дальше - арабскими. */
internal fun phaseRoman(n: Int): String = listOf("I", "II", "III", "IV", "V", "VI").getOrElse(n - 1) { n.toString() }
