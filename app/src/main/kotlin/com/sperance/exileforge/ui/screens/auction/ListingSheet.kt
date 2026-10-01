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
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.orbArt
import com.sperance.exileforge.ui.theme.*

/** The auction's currencies as a picker's options (3.0.0; server 1.65.0 - the base orbs alone): keyed by item code, named by the dictionary, cheapest first. */
internal fun orbOptions(s: ForgeState): Map<String, String> = s.currencies.associate { it.code to itemTitle(it.code) }

/**
 * Listing something on the auction: one item from its card or the sell tab, or part of a stack.
 *
 * The price is counted in the base orbs alone (`AU_007`), so the orb is picked from the auction's currencies
 * and the amount typed; whether the listing stands is the server's to say. [owned] is given for a
 * stack, and then the sheet also asks how many of them — how many there are is said, not enforced.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ListingSheet(s: ForgeState, name: String, owned: Long? = null, onDismiss: () -> Unit,
    onList: (orb: String, price: Long, amount: Long) -> Unit) {
    var orb by remember { mutableStateOf(s.currencies.firstOrNull()?.code.orEmpty()) }
    var price by remember { mutableStateOf("1") }
    var amount by remember { mutableStateOf("1") }
    val cost = price.toLongOrNull() ?: 0L
    val count = if (owned == null) 1L else amount.toLongOrNull() ?: 0L
    val digits = KeyboardOptions(keyboardType = KeyboardType.Number)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Engraved(ui("sell.list"))
            Text(name, color = Parchment, style = MaterialTheme.typography.titleMedium)
            if (owned != null) OutlinedTextField(amount, { value -> amount = value.filter(Char::isDigit).take(s.inputs.number) }, label = { Text(ui("sell.amount_owned", owned)) },
                singleLine = true, keyboardOptions = digits, modifier = Modifier.fillMaxWidth())
            if (s.currencies.isEmpty()) Text(ui("orb.none"), color = Muted)
            else Spinner(ui("orb.orb"), orb, orbOptions(s), !s.busy, glyph = Glyph.CURRENCY, optionArt = orbArt(s.currencies)) { orb = it }
            OutlinedTextField(price, { value -> price = value.filter(Char::isDigit).take(s.inputs.number) }, label = { Text(ui("sell.price")) },
                singleLine = true, keyboardOptions = digits, modifier = Modifier.fillMaxWidth())
            MutedText(ui("sell.note"))
            ForgeButton(enabled = !s.busy && orb.isNotBlank() && cost > 0 && count > 0, onClick = { onList(orb, cost, count) }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("sell.list"))
            }
        }
    }
}
