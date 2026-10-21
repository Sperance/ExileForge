package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.Feature
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.level
import com.sperance.exileforge.core.app.unlocked
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.PanelRaised

/**
 * Вход в раздел со своим экраном замка ([Feature.lockScreen]): герой дорос - [content], нет - замок ([FeatureLock]) и ни одного запроса
 * раздела.
 */
@Composable fun FeatureGate(game: GameUi, feature: Feature, content: @Composable () -> Unit) {
    if (game.unlocked(feature)) content() else FeatureLock(game, feature)
}

/**
 * Экран раздела [feature], до которого герой не дорос (касание закрытой вкладки панели): замок, «Откроется с N уровня», что это за
 * раздел, полоса уровня героя до уровня открытия и «Ур. 12 · до открытия 4 уровня».
 */
@Composable private fun FeatureLock(game: GameUi, feature: Feature) {
    val needed = feature.level(game.index?.rules)
    val level = game.heroLevel.coerceAtMost(needed)
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(64.dp).background(PanelRaised, CircleShape).border(1.dp, Bronze, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.size(30.dp))
        }
        Text(ui("unlock.from", needed), color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(ui("unlock.about.${feature.name}"), color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        val caption = ui("unlock.progress", level, ui(pluralKey("unlock.levels_left", needed - level), needed - level))
        LinearProgressIndicator(
            progress = { if (needed > 0) level.toFloat() / needed else 1f },
            modifier = Modifier.widthIn(max = 240.dp).fillMaxWidth().height(6.dp).semantics { contentDescription = caption },
            color = Gold,
            trackColor = Abyss,
        )
        Text(caption, color = Muted, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}
