package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.update.ReleaseKind
import com.sperance.exileforge.core.update.ReleaseNotes
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.RelicLook
import com.sperance.exileforge.ui.components.motionClock
import com.sperance.exileforge.ui.components.nameStyle
import com.sperance.exileforge.ui.components.relicGround
import com.sperance.exileforge.ui.components.relicLook
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.glow
import kotlin.math.sin

/**
 * Карточка релиза «Что нового» (4.0.0, вариант «Печати»): вес релиза задаёт облик. Крупный (`X.0.0`) - звёздное небо
 * мифической вещи с дышащим ореолом, патч (`X.Y.0`) - тёплый уголь уникальной с бегущим отблеском, фикс - простая плита.
 * Касание раскрывает заметки; декор стоит, когда анимации выключены.
 */
@Composable fun ReleaseCard(release: ReleaseNotes, open: Boolean, onToggle: () -> Unit) {
    val kind = release.kind
    val look = kind.look()
    val shape = RoundedCornerShape(12.dp)
    val ground = when (look) {
        null -> Modifier.background(Panel, shape).border(1.dp, Bronze, shape)
        else -> Modifier.kindGlow(kind, look, shape).relicGround(look, shape).sheen(kind, look)
    }
    Column(
        ground.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = if (kind == ReleaseKind.MAJOR) 14.dp else 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val dot = look?.gold ?: Muted.copy(alpha = .6f)
            Box(Modifier.size(8.dp).then(if (look != null) Modifier.glow(dot.copy(alpha = .7f), radius = 6.dp, shape = CircleShape) else Modifier).background(dot, CircleShape))
            Text(ui("app.version", release.version), style = versionStyle(kind, look), modifier = Modifier.weight(1f))
            KindBadge(kind, look)
        }
        release.published?.take(10)?.let { MutedText(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 18.dp)) }
        if (open) {
            release.body.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("## ") }.forEach { line ->
                Text("• " + line.removePrefix("- ").removePrefix("* "), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Облик веса: крупный - мифическая, патч - уникальная, фикс - без облика вещи. */
private fun ReleaseKind.look(): RelicLook? = when (this) {
    ReleaseKind.MAJOR -> relicLook(Rarity.MYTHICAL)
    ReleaseKind.MINOR -> relicLook(Rarity.UNIQUE)
    ReleaseKind.PATCH -> null
}

@Composable private fun versionStyle(kind: ReleaseKind, look: RelicLook?) = when {
    look == null -> MaterialTheme.typography.titleSmall.copy(color = Parchment, fontWeight = FontWeight.SemiBold)
    kind == ReleaseKind.MAJOR -> look.nameStyle(19)
    else -> look.nameStyle(16)
}

/** Печать веса справа: у фикса - контур, у патча и крупного - заливка золотом облика. */
@Composable private fun KindBadge(kind: ReleaseKind, look: RelicLook?) {
    val shape = RoundedCornerShape(10.dp)
    val text = ui("release.kind.${kind.name.lowercase()}").uppercase()
    val style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, fontSize = 9.5.sp)
    if (look == null) {
        Text(text, color = Muted, style = style, modifier = Modifier.border(1.dp, Bronze, shape).padding(horizontal = 7.dp, vertical = 2.dp))
    } else {
        Text(text, color = look.onPrimary, style = style, modifier = Modifier.background(look.primary, shape).padding(horizontal = 7.dp, vertical = 2.dp))
    }
}

/** Дышащий ореол крупного релиза (3,5 с): свечение облика то гаснет, то разгорается. */
@Composable private fun Modifier.kindGlow(kind: ReleaseKind, look: RelicLook, shape: RoundedCornerShape): Modifier {
    if (kind != ReleaseKind.MAJOR) return this
    val breath = (1 - kotlin.math.cos(motionClock(BREATH_MS, "release-breath") * 2 * Math.PI).toFloat()) / 2
    return glow(look.glow.copy(alpha = .25f + .35f * breath), radius = 12.dp, shape = shape)
}

/** Бегущий отблеск патча (4,5 с): косая светлая полоса проходит по карточке и замирает до следующего круга. */
@Composable private fun Modifier.sheen(kind: ReleaseKind, look: RelicLook): Modifier {
    if (kind != ReleaseKind.MINOR) return this
    val t = motionClock(SHEEN_MS, "release-sheen")
    // Первые 60% круга полоса ждёт за левым краем, затем проходит карточку
    val pass = ((t - .6f) / .4f).coerceIn(0f, 1f)
    if (pass <= 0f || pass >= 1f) return this
    return drawWithContent {
        drawContent()
        val x = -size.width * .4f + size.width * 1.8f * pass
        val band = Brush.linearGradient(listOf(Color.Transparent, look.accent.copy(alpha = .14f * sin(pass * Math.PI).toFloat() + .04f), Color.Transparent), Offset(x, 0f), Offset(x + size.width * .4f, size.height))
        drawRect(band)
    }
}

private const val BREATH_MS = 3500
private const val SHEEN_MS = 4500
