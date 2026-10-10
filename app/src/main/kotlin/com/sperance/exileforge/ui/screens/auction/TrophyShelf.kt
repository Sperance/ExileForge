package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.FateKit
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.FateLever
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.ui.components.FateInk
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment

/**
 * Трофеи боссов у торговца (4.6.0, Предначертание «Трофейщик»): строка на босса - сколько трофеев из цены дара и кнопка
 * обмена на его уникалку. Обмен закрыт с причиной: трофеев мало (`FT_003`) или уникалок босса на уровне героя ещё нет (`FT_004`).
 */
@Composable fun TrophyShelf(game: GameUi, busy: Boolean, onTrade: (String) -> Unit) {
    val hero = game.hero ?: return
    val index = game.index ?: return
    val trophies = hero.info.trophies.filterValues { it > 0 }
    if (trophies.isEmpty()) return
    // Цена обмена - на силе дара (4.6.3), как её считает сервер
    val price = FateKit.of(index, hero.info.fate, hero.stats, hero.equipped.values).effects.of(FateLever.TROPHIES)?.value?.toInt()
    ForgePanel(accent = FateInk.Gold) {
        Text(ui("fate.trophies_title"), color = FateInk.GoldLight, style = MaterialTheme.typography.titleSmall)
        Text(ui("fate.trophies_hint"), color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
        trophies.entries.sortedBy { monsterTitle(it.key) }.forEach { (boss, have) ->
            val pool = index.monster(MonsterCode(boss))?.let { index.fates.trophyPool(index, it, hero.level) }.orEmpty()
            // Причина закрытой кнопки - здесь, а не отказом сервера
            val reason = when {
                price == null -> ui("fate.trophies_no_fate")
                have < price -> ui("fate.trophies_few", price - have)
                pool.isEmpty() -> ui("fate.trophies_no_unique")
                else -> null
            }
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(monsterTitle(boss), color = Parchment, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(ui("fate.trophies_count", have, price ?: 0), color = Muted, style = MaterialTheme.typography.labelSmall)
                    reason?.let { Text(it, color = LifeRed, style = MaterialTheme.typography.labelSmall) }
                }
                ForgeButton(onClick = { onTrade(boss) }, enabled = !busy && reason == null) { Text(ui("fate.trophies_trade")) }
            }
        }
    }
}
