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
import com.sperance.exileforge.core.campaign.FateKit
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.MonsterCode
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
 * дара и кнопка обмена на его уникалку. Обмен закрыт с причиной: трофеев мало (`FT_003`) или уникалок босса на уровне героя ещё
 * нет (`FT_004`).
 *
 * Цена обмена - на силе дара, как её считает сервер; причина закрытой кнопки - здесь, а не отказом сервера.
 */
@Composable fun TrophyShelf(game: GameUi, busy: Boolean, onTrade: (String) -> Unit) {
    val hero = game.hero ?: return
    val index = game.index ?: return
    val shelf = remember(hero, index, game.lang) {
        val price = FateKit.of(index, hero.info.fate, hero.stats, hero.equipped.values).effects.trophyPrice()
        hero.info.trophies.filterValues { it > 0 }.entries.sortedBy { monsterTitle(it.key) }.map { (boss, have) ->
            val pool = index.monster(MonsterCode(boss))?.let { index.fates.trophyPool(index, it, hero.level) }.orEmpty()
            val reason = when {
                price == null -> ui("fate.trophies_no_fate")
                have < price -> ui("fate.trophies_few", price - have)
                pool.isEmpty() -> ui("fate.trophies_no_unique")
                else -> null
            }
            BossTrophies(boss, have, price ?: 0, reason)
        }
    }
    if (shelf.isEmpty()) return
    val tradable = shelf.count { it.reason == null }
    FoldPanel(
        ui("fate.trophies_title"),
        accent = FateInk.Gold,
        titleColor = FateInk.GoldLight,
        summary = {
            SummaryChip(ui(pluralKey("fate.trophies_bosses", shelf.size), shelf.size))
            if (tradable > 0) SummaryChip(ui("fate.trophies_tradable", tradable), FateInk.Gold)
        },
    ) {
        Text(ui("fate.trophies_hint"), color = Muted, style = MaterialTheme.typography.bodySmall)
        shelf.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(monsterTitle(row.boss), color = Parchment, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(ui("fate.trophies_count", row.have, row.price), color = Muted, style = MaterialTheme.typography.labelSmall)
                    row.reason?.let { Text(it, color = LossText, style = MaterialTheme.typography.labelSmall) }
                }
                RowButton(ui("fate.trophies_trade"), onClick = { onTrade(row.boss) }, state = ButtonState.of(!busy && row.reason == null))
            }
        }
    }
}

/** Трофеи одного босса: [have] из цены обмена [price]; [reason] - почему обмен закрыт, null - обмен готов. */
private data class BossTrophies(val boss: String, val have: Int, val price: Int, val reason: String?)
