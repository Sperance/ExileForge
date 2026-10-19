package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sperance.exileforge.core.campaign.run.BlightNews
import com.sperance.exileforge.core.campaign.run.BlightView
import com.sperance.exileforge.core.i18n.ui

/**
 * Новости Скверны строкой над картой: пробудилась, расползается, иссякла, пробуждается Матерь. Объявляется только
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
