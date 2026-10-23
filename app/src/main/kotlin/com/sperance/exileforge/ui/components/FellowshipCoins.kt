package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.groupedNumber
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.LossText
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Rune

/** Как показать сумму монет Содружества: цена ([PRICE]), начисление со знаком «+» ([AWARD]). */
enum class CoinsRole { PRICE, AWARD }

/** Монеты Содружества словами - для чтения с экрана: «56 монет Содружества». */
fun fellowshipCoinsText(coins: Long): String = ui("guild.coins_amount", groupedNumber(coins), plural("guild.coins_noun", coins))

/**
 * Сумма монет Содружества: значок кольца из трёх звеньев ([ForgeGlyphs.Fellowship]) цвета Rune и короткое число ([compactNumber]),
 * сокращённое касанием показывает полное. Начисление ([CoinsRole.AWARD]) - со знаком «+», нулевое - серое; цена, на которую монет
 * не хватает ([affordable] false), - красным числом. Чтение с экрана - полное число с валютой ([fellowshipCoinsText]).
 */
@Composable fun FellowshipCoins(coins: Long, role: CoinsRole, modifier: Modifier = Modifier, affordable: Boolean = true) {
    val award = role == CoinsRole.AWARD
    val idle = award && coins == 0L
    FullNumberTipped(coins, modifier) {
        Row(
            Modifier.semantics(mergeDescendants = true) { contentDescription = fellowshipCoinsText(coins) },
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(ForgeGlyphs.Fellowship, null, tint = if (idle) Muted else Rune, modifier = Modifier.size(13.dp))
            Text(
                (if (award) "+" else "") + compactNumber(coins),
                color = when {
                    idle -> Muted
                    !affordable -> LossText
                    else -> Rune
                },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}
