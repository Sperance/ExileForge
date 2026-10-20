package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.fate.TrophyShelfView
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.FateInk
import com.sperance.exileforge.ui.components.FoldPanel
import com.sperance.exileforge.ui.components.RowButton
import com.sperance.exileforge.ui.components.SummaryChip
import com.sperance.exileforge.ui.theme.LossText
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment

/**
 * Трофеи боссов у торговца (Предначертание «Трофейщик») - панель [FoldPanel], свёрнутая при каждом входе: в строке сводка
 * «N боссов» и золотой чип «к обмену N» (готовые обмены, при нуле его нет). Раскрытая - строка на босса: сколько трофеев из цены
 * дара и кнопка обмена на его уникалку; закрытый обмен - с причиной. Полку собирает ядро ([TrophyShelfView]).
 */
@Composable fun TrophyShelf(game: GameUi, busy: Boolean, onTrade: (String) -> Unit) {
    val hero = game.hero ?: return
    val index = game.index ?: return
    val shelf = remember(hero, index, game.lang) { TrophyShelfView.of(index, hero) }
    if (shelf.isEmpty) return
    FoldPanel(
        ui("fate.trophies_title"),
        accent = FateInk.Gold,
        titleColor = FateInk.GoldLight,
        summary = {
            SummaryChip(ui(pluralKey("fate.trophies_bosses", shelf.rows.size), shelf.rows.size))
            if (shelf.tradable > 0) SummaryChip(ui("fate.trophies_tradable", shelf.tradable), FateInk.Gold)
        },
    ) {
        Text(ui("fate.trophies_hint"), color = Muted, style = MaterialTheme.typography.bodySmall)
        shelf.rows.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(row.title, color = Parchment, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(ui("fate.trophies_count", row.have, row.price), color = Muted, style = MaterialTheme.typography.labelSmall)
                    row.block?.let { Text(it, color = LossText, style = MaterialTheme.typography.labelSmall) }
                }
                RowButton(ui("fate.trophies_trade"), onClick = { onTrade(row.boss) }, state = ButtonState.of(!busy && row.tradable))
            }
        }
    }
}
