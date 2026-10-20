package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.InfoMark
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.motionPhase
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.glow

/** Режимы испытаний: у каждого свой цвет медальона и знак. */
enum class TrialMode(val accent: Color, val icon: ImageVector, private val title: String) {
    RIFT(Color(0xFF39FF88), ForgeGlyphs.Rift, "rift.title"),
    TOWER(Rune, ForgeGlyphs.Keep, "trials.tower_title"),
    RUSH(Color(0xFFF2A65A), ForgeGlyphs.Skull, "trials.rush_title"),
    ;

    val label: String get() = ui(title)
}

/**
 * Плита режима: медальон в цвете режима с вращающейся пунктирной орбитой, имя серифами со значком ⓘ правил режима [info],
 * строка сути [note] и плашка [chip], если есть; по плите пробегает блик со сдвигом [phase] круга, чтобы плиты не мигали разом.
 * Касание раскрывает [details] - вход и подробности; шеврон поворачивается. Без [details] плита не раскрывается и шеврона нет.
 * Декор стоит, когда анимации выключены.
 */
@Composable fun TrialModeCard(
    mode: TrialMode,
    note: String?,
    chip: String?,
    info: () -> Tip,
    open: Boolean,
    phase: Float,
    onToggle: () -> Unit,
    details: (@Composable ColumnScope.() -> Unit)?,
) {
    val accent = mode.accent
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(SlabTop, SlabBottom)))
            .drawBehind { drawRect(Brush.radialGradient(listOf(accent.copy(alpha = .2f), Color.Transparent), Offset.Zero, size.maxDimension * .7f)) }
            .sweep(phase)
            .border(1.dp, accent.copy(alpha = if (open) .7f else .45f), shape),
    ) {
        Row(
            Modifier.fillMaxWidth().then(if (details != null) Modifier.clickable(onClick = onToggle) else Modifier).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TrialMedallion(mode)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mode.label, color = GoldBright, style = relicName(15), modifier = Modifier.weight(1f, fill = false))
                    InfoMark(mode.label, tip = info)
                }
                note?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
                chip?.let {
                    Text(
                        it,
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 5.dp).background(accent.copy(alpha = .12f), CircleShape).border(1.dp, accent.copy(alpha = .35f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            if (details != null) {
                val turn by animateFloatAsState(if (open) 90f else 0f, label = "trial-chevron")
                Text("›", color = accent, style = relicName(20), modifier = Modifier.rotate(turn))
            }
        }
        if (details != null) {
            AnimatedVisibility(open) {
                Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = details)
            }
        }
    }
}

/**
 * Медальон: знак режима в круге его цвета поперечником [size] dp, вокруг - пунктирная орбита, оборот за 18 с.
 *
 * Фаза читается в рисовании: орбита идёт без перекомпоновки.
 */
@Composable internal fun TrialMedallion(mode: TrialMode, size: Int = MEDALLION) {
    val accent = mode.accent
    val orbit = motionPhase(ORBIT_MS)
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            rotate(orbit.value * 360f) {
                drawCircle(accent.copy(alpha = .5f), radius = size.minDimension / 2 - 1f, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))))
            }
        }
        Box(
            Modifier.size((size - size / 5).dp).glow(accent.copy(alpha = .45f), radius = 10.dp, shape = CircleShape)
                .background(Brush.radialGradient(listOf(accent.copy(alpha = .3f), AbyssInk)), CircleShape)
                .border(1.5.dp, accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(mode.icon, null, tint = accent, modifier = Modifier.size((size * 9 / 20).dp)) }
    }
}

/**
 * Блик плиты: светлая косая полоса раз в 5 с, со сдвигом [phase] круга.
 *
 * Фаза читается в рисовании: блик бежит без перекомпоновки плиты.
 */
@Composable private fun Modifier.sweep(phase: Float): Modifier {
    val clock = motionPhase(SWEEP_MS)
    return drawWithContent {
        drawContent()
        val t = (clock.value + phase) % 1f
        val pass = ((t - .7f) / .3f).coerceIn(0f, 1f)
        if (pass <= 0f || pass >= 1f) return@drawWithContent
        val x = -size.width * .6f + size.width * 1.9f * pass
        drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = .07f), Color.Transparent), Offset(x, 0f), Offset(x + size.width * .4f, size.height)))
    }
}

private val SlabTop = Color(0xFF18232B)
private val SlabBottom = Color(0xFF0E151A)
private val AbyssInk = Color(0xFF0A0E12)
private const val MEDALLION = 58
private const val ORBIT_MS = 18_000
private const val SWEEP_MS = 5_000
