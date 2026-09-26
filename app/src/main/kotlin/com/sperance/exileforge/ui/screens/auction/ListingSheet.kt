package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/**
 * Listing something on the auction: one item from its card or the sell tab, or part of a stack.
 *
 * The price is counted in orbs alone (`AU_007`), so the orb is picked from the server's catalogue
 * and the amount typed; whether the listing stands is the server's to say. [owned] is given for a
 * stack, and then the sheet also asks how many of them — how many there are is said, not enforced.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ListingSheet(s: ForgeState, name: String, owned: Long? = null, onDismiss: () -> Unit,
    onList: (orb: String, price: Long, amount: Long) -> Unit) {
    var orb by remember { mutableStateOf(s.world.orbs.firstOrNull()?.id.orEmpty()) }
    var price by remember { mutableStateOf("1") }
    var amount by remember { mutableStateOf("1") }
    val cost = price.toLongOrNull() ?: 0L
    val count = if (owned == null) 1L else amount.toLongOrNull() ?: 0L
    val digits = KeyboardOptions(keyboardType = KeyboardType.Number)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Engraved(ui("sell.list"))
            Text(name, color = Parchment, style = MaterialTheme.typography.titleMedium)
            if (owned != null) OutlinedTextField(amount, { value -> amount = value.filter(Char::isDigit) }, label = { Text(ui("sell.amount_owned", owned)) },
                singleLine = true, keyboardOptions = digits, modifier = Modifier.fillMaxWidth())
            if (s.world.orbs.isEmpty()) Text(ui("orb.none"), color = Muted)
            else Spinner(ui("orb.orb"), orb, s.world.orbs.associate { it.id to it.title(s.lang) }, !s.busy, glyph = Glyph.CURRENCY,
                optionArt = com.sperance.exileforge.ui.icons.orbArt(s.world.orbs)) { orb = it }
            OutlinedTextField(price, { value -> price = value.filter(Char::isDigit) }, label = { Text(ui("sell.price")) },
                singleLine = true, keyboardOptions = digits, modifier = Modifier.fillMaxWidth())
            MutedText(ui("sell.price_note"))
            MutedText(ui("sell.note"))
            ForgeButton(enabled = !s.busy && orb.isNotBlank() && cost > 0 && count > 0, onClick = { onList(orb, cost, count) }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("sell.list"))
            }
        }
    }
}
