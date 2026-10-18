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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.run.ObjectAction
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Нижняя пилюля «действие у объекта» высотой [PILL_HEIGHT] - одна для вскрытия трещины тайной комнаты, сбора узла ремесла и запертой
 * двери, той же формы, что лист объекта: круглое гнездо со значком слева, имя объекта и подсказка (у сбора - «Сбор… N с») посередине,
 * процент справа; сделанное - заливка самой пилюли слева направо тоном объекта с яркой кромкой. Заливка читает долю кадра [frame]
 * в рисовании, процент и секунды - долю [live] живого снимка: пилюля не перекомпонуется на каждом шаге. У действия без доли (дверь
 * ждёт рычага) - только подсказка, без заливки и процента.
 */
@Composable internal fun ObjectActionPanel(action: ObjectAction, frame: () -> Float?, live: () -> Float?) {
    val look = ActionLook.of(action.feature)
    val pill = RoundedCornerShape(PILL_HEIGHT / 2)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier.navigationBarsPadding().padding(12.dp).fillMaxWidth().height(PILL_HEIGHT).depthPanel(pill).clip(pill).border(1.dp, look.tone.copy(alpha = .2f), pill)
                .drawBehind {
                    if (action.progress == null) return@drawBehind
                    val reach = size.width * (frame() ?: 0f).coerceIn(0f, 1f)
                    drawRect(Brush.horizontalGradient(listOf(look.tone.copy(alpha = .08f), look.tone.copy(alpha = .25f)), endX = reach.coerceAtLeast(1f)), size = size.copy(width = reach))
                    if (reach > 0f) drawRect(look.tone, topLeft = Offset(reach - EDGE.toPx(), 0f), size = Size(EDGE.toPx(), size.height))
                }
                .padding(start = 6.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(40.dp).background(Abyss, CircleShape).border(1.dp, look.tone.copy(alpha = .45f), CircleShape), contentAlignment = Alignment.Center) { look.icon() }
            Column(Modifier.weight(1f)) {
                Text(look.name, color = GoldBright, style = relicName(14), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val hint by remember(live) { derivedStateOf { look.hint(live()) } }
                Text(hint, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (action.progress != null) {
                val percent by remember(live) { derivedStateOf { ((live() ?: 0f) * 100).roundToInt() } }
                Text(ui("feature.action.percent", percent), color = look.tone, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Облик объекта в пилюле действия: имя, значок, тон заливки и подсказка по сделанной доле. */
private class ActionLook(val name: String, val tone: Color, val hint: (Float?) -> String, val icon: @Composable () -> Unit) {
    companion object {
        /** Узел - его ресурс зелёным, подсказка - «Сбор… N с» до конца; комната (трещина, дверь) - своим именем фиолетовым. */
        fun of(feature: MapFeature): ActionLook = when (feature) {
            is MapFeature.Node -> ActionLook(
                itemTitle(feature.job.output),
                Vital,
                { share -> ui("feature.action.NODE", fineNumber(ceil(feature.seconds * (1 - (share ?: 0f)) * 10) / 10)) },
            ) { BagIcon(feature.job.output, Modifier.size(24.dp)) }

            else -> ActionLook(ui("feature.room.${feature.kind.name}"), CrackGlow, { ui("feature.action.${feature.kind.name}") }) {
                Icon(ForgeGlyphs.Keep, null, tint = CrackGlow, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** Высота пилюли действия у объекта. */
private val PILL_HEIGHT = 52.dp

/** Толщина яркой кромки заливки. */
private val EDGE = 2.dp

/** Тон трещины тайной комнаты и двери. */
private val CrackGlow = Color(0xFFB98CFF)
