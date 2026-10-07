package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.auction.PriceHint
import com.sperance.exileforge.presentation.state.Feature
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.level
import com.sperance.exileforge.presentation.state.unlocked
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.icons.orbArt
import com.sperance.exileforge.ui.theme.*

/** The auction's currencies as a picker's options (3.0.0; server 1.65.0 - the base orbs alone): keyed by item code, named by the dictionary, cheapest first. */
internal fun orbOptions(game: GameUi): Map<String, String> = game.currencies.associate { it.code.value to itemTitle(it.code.value) }

/**
 * Listing something on the auction: one item from its card or the sell tab, or part of a stack.
 *
 * The price is counted in the base orbs alone (`AU_007`), so the orb is picked from the auction's currencies
 * and the amount typed; whether the listing stands is the server's to say. [owned] is given for a
 * stack, and then the sheet also asks how many of them — how many there are is said, not enforced.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingSheet(
    game: GameUi,
    name: String,
    owned: Long? = null,
    onDismiss: () -> Unit,
    hint: (suspend () -> PriceHint?)? = null,
    onList: (orb: String, price: Long, amount: Long) -> Unit,
) {
    var orb by remember { mutableStateOf(game.currencies.firstOrNull()?.code?.value.orEmpty()) }
    var price by remember { mutableStateOf("1") }
    var amount by remember { mutableStateOf("1") }
    val cost = price.toLongOrNull() ?: 0L
    val count = if (owned == null) 1L else amount.toLongOrNull() ?: 0L
    val digits = KeyboardOptions(keyboardType = KeyboardType.Number)
    // What the like sold for lately (3.79.0): the median of recent deals, shown only with enough of them; a tap takes it.
    val recent by produceState<PriceHint?>(null, hint) { value = hint?.invoke() }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Engraved(ui("sell.list"))
            Text(name, color = Parchment, style = MaterialTheme.typography.titleMedium)
            if (owned != null) {
                OutlinedTextField(
                    amount,
                    { value -> amount = value.filter(Char::isDigit).take(game.inputs.number) },
                    label = { Text(ui("sell.amount_owned", owned)) },
                    singleLine = true,
                    keyboardOptions = digits,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (game.currencies.isEmpty()) {
                Text(ui("orb.none"), color = Muted)
            } else {
                Spinner(ui("orb.orb"), orb, orbOptions(game), !game.busy, glyph = Glyph.CURRENCY, optionArt = orbArt(game.currencies)) { orb = it }
            }
            OutlinedTextField(
                price,
                { value -> price = value.filter(Char::isDigit).take(game.inputs.number) },
                label = { Text(ui("sell.price")) },
                singleLine = true,
                keyboardOptions = digits,
                modifier = Modifier.fillMaxWidth(),
            )
            recent?.let { h ->
                TextButton(onClick = {
                    orb = h.priceOrb
                    price = (h.price * count.coerceAtLeast(1)).toString()
                }, contentPadding = PaddingValues(0.dp)) {
                    Text(ui("sell.price_hint", h.price, itemTitle(h.priceOrb), h.sales), color = Gold, style = MaterialTheme.typography.bodySmall)
                }
            }
            MutedText(ui("sell.note"))
            // Аукцион ещё закрыт (3.94.0): кнопка неактивна, и рядом сказано, когда он откроется
            val closed = auctionClosed(game)
            ForgeButton(enabled = closed == null && !game.busy && orb.isNotBlank() && cost > 0 && count > 0, onClick = { onList(orb, cost, count) }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("sell.list"))
            }
            closed?.let { Text(it, color = LifeRed, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

/** Почему выставить лот нельзя (3.94.0): аукцион откроется с уровня из правил; null - можно. */
fun auctionClosed(game: GameUi): String? = Feature.AUCTION.takeUnless { game.unlocked(it) }?.let { ui("auction.closed_until", it.level(game.index?.rules), game.heroLevel) }
