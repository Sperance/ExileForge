package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.run.BlightNews
import com.sperance.exileforge.core.campaign.run.BlightView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.screens.expedition.scene.BlightTint
import com.sperance.exileforge.ui.theme.rarityColor

/**
 * Очаг Скверны на полосе карты (4.0.0): какая точка («Очаг 2 из 4» или Матерь), сколько монстров Скверны ещё стоит вокруг
 * неё, а у зачищенной - сундук и его качество. Пока очаг не найден или уже иссяк - ничего.
 */
@Composable internal fun BlightLine(view: BlightView?) {
    val blight = view?.takeIf { it.seen && !it.over } ?: return
    val mother = blight.point == blight.points
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (mother) ui("map.legend_mother") else ui("feature.blight.point", blight.point + 1, blight.limit),
            color = BlightTint.hot,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
        val chest = blight.chest
        when {
            chest != null -> {
                Text(ui("feature.blight.chest"), color = BlightTint.bile, style = MaterialTheme.typography.labelSmall)
                Text(ui("feature.blight.quality.${chest.name}"), color = rarityColor(chest.name), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            blight.raised -> Text(ui("feature.blight.left", blight.left), color = BlightTint.bone, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Новости Скверны строкой над картой (4.0.0): пробудилась, расползается, иссякла, пробуждается Матерь. Объявляется только
 * новая новость - вошедший снова в заход старых не слышит.
 */
@Composable internal fun BlightWatch(view: BlightView?, announce: (String) -> Unit) {
    var told by remember { mutableIntStateOf(view?.told ?: 0) }
    LaunchedEffect(view?.told) {
        val blight = view ?: return@LaunchedEffect
        if (blight.told <= told) return@LaunchedEffect
        told = blight.told
        blight.news?.let { announce(ui(newsKey(it))) }
    }
}

private fun newsKey(news: BlightNews): String = when (news) {
    BlightNews.AWAKE -> "feature.blight.awake"
    BlightNews.SPREAD -> "feature.blight.spread"
    BlightNews.END -> "feature.blight.end"
    BlightNews.MOTHER -> "feature.blight.mother"
}
