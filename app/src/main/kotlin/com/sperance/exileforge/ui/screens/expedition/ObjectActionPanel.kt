package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.run.ObjectAction
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import kotlin.math.roundToInt

/**
 * Нижняя панель «действие у объекта» - одна для вскрытия трещины тайной комнаты, сбора узла ремесла и запертой двери: значок и имя
 * объекта, подсказка, яркая полоса сделанного и процент. Полоса читает долю кадра [frame] в рисовании, процент - долю [live] живого
 * снимка: панель не перекомпонуется на каждом шаге. У действия без доли (дверь ждёт рычага) - только подсказка.
 */
@Composable internal fun ObjectActionPanel(action: ObjectAction, frame: () -> Float?, live: () -> Float?) {
    val look = ActionLook.of(action.feature)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        RunPanel(Modifier, look.tone) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val socket = RoundedCornerShape(10.dp)
                Box(Modifier.size(44.dp).background(Abyss, socket).border(1.dp, look.tone.copy(alpha = .45f), socket), contentAlignment = Alignment.Center) { look.icon() }
                Column(Modifier.weight(1f)) {
                    Text(look.name, color = GoldBright, style = relicName(16))
                    Text(ui("feature.action.${action.feature.kind.name}"), color = Muted, style = MaterialTheme.typography.labelMedium)
                }
                if (action.progress != null) {
                    val percent by remember(live) { derivedStateOf { ((live() ?: 0f) * 100).roundToInt() } }
                    Text(ui("feature.action.percent", percent), color = look.tone, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (action.progress != null) {
                Box(
                    Modifier.fillMaxWidth().height(10.dp).background(Abyss, RoundedCornerShape(5.dp)).drawBehind {
                        val share = (frame() ?: 0f).coerceIn(0f, 1f)
                        drawRoundRect(Brush.horizontalGradient(listOf(look.tone.copy(alpha = .7f), look.tone)), size = size.copy(width = size.width * share), cornerRadius = CornerRadius(size.height / 2))
                    },
                )
            }
        }
    }
}

/** Облик объекта в панели действия: имя, значок и тон полосы. */
private class ActionLook(val name: String, val tone: Color, val icon: @Composable () -> Unit) {
    companion object {
        /** Узел - его ресурс зелёным; комната (трещина, дверь) - своим именем фиолетовым. */
        fun of(feature: MapFeature): ActionLook = when (feature) {
            is MapFeature.Node -> ActionLook(itemTitle(feature.job.output), Vital) { BagIcon(feature.job.output, Modifier.size(28.dp)) }
            else -> ActionLook(ui("feature.room.${feature.kind.name}"), CrackGlow) { Icon(ForgeGlyphs.Keep, null, tint = CrackGlow, modifier = Modifier.size(24.dp)) }
        }
    }
}

/** Тон трещины тайной комнаты и двери. */
private val CrackGlow = Color(0xFFB98CFF)
