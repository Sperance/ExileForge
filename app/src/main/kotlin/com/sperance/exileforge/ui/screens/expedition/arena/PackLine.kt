package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Muted

/**
 * Тонкая строка стаи над журналом боя: «Волна 2/3 · осталось 4» ([packText]); рядом - чип ярости боя ([RageChip], касание - её
 * подсказка). Один враг без волн - строки нет, пока ярость не поднялась на ступень.
 */
@Composable internal fun PackLine(fight: FightHud, modifier: Modifier = Modifier) {
    val text = packText(fight)
    val rage = fight.rage?.takeIf { text.isNotEmpty() || it.stage > 0 }
    if (text.isEmpty() && rage == null) return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        if (text.isNotEmpty()) Text(text, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        rage?.let { RageChip(it) }
    }
}

/** Слова строки стаи: волна, когда их больше одной, и сколько врагов стаи ещё стоит, когда их больше одного; иначе - пусто. */
internal fun packText(fight: FightHud): String = listOfNotNull(
    ui("fight.stage", fight.round, fight.rounds).takeIf { fight.rounds > 1 },
    ui("fight.pack_left", fight.standing).takeIf { fight.foes.size > 1 },
).joinToString(" · ")
