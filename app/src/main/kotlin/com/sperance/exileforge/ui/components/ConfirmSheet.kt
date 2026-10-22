package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.trade.GoldBill
import com.sperance.exileforge.ui.theme.*

/** Whether a line of the ledger is taken away, handed over, or only said. */
enum class Tone { PLAIN, SPEND, GAIN }

/**
 * One line of what an action costs and brings: «Спишется — 4 × Сфера хаоса». [full] - значение с полным числом, когда [value] его
 * сократил («150к золота» → «150 340 золота»): касание значения показывает его; null - значение не нажимается.
 */
data class LedgerLine(val label: String, val value: String, val tone: Tone = Tone.PLAIN, val full: String? = null)

/**
 * "Are you sure" for the handful of things that cannot be taken back cheaply.
 *
 * It exists for actions that cost something the player cannot simply earn again in a moment: an
 * orb spent giving a node back, an item sold to a merchant, a purchase that is final. It is a sheet
 * rising from the bottom, where the thumb already is, and it is drawn like an item — the same band
 * down the left edge, gold for an exchange and blood for a loss.
 *
 * The price is the thing being confirmed, so it is a ledger rather than a sentence: what is taken,
 * what is left, what comes back. And the button is held rather than tapped. A tap is what a player
 * does without looking; holding for [Hold.CONFIRM_MS] is what they do on purpose, and the band
 * filling under the thumb shows the moment it becomes real. A screen reader cannot hold, so the
 * same button answers an accessibility click at once — that tap is deliberate by construction.
 *
 * It is deliberately not on everything. A question in front of an ordinary action stops being read.
 *
 * [blocked] - клиент уже знает, что действие не пройдёт: [warning] говорит почему, кнопка погашена; [busy] - запрос уже идёт,
 * кнопка занята (спиннер). [options] - выбор под счётом (оплата
 * атласа), между счётом и заметкой. [gold] - оплата золотом: строки «Есть · Спишется · Останется» под [ledger]; не хватает - кнопка
 * погашена, а [warning] без своего говорит недостачу. [holdMillis] - сколько держать кнопку (сброс всего дерева держат дольше).
 * Тело листается, кнопка удержания и «Отмена» закреплены в подвале листа: поле в [options] с клавиатурой их не прячет. Полоса редкости
 * идёт вдоль всего листа.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmSheet(
    title: String,
    confirm: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    ledger: List<LedgerLine> = emptyList(),
    note: String? = null,
    warning: String? = null,
    danger: Boolean = false,
    blocked: Boolean = false,
    busy: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
    options: (@Composable () -> Unit)? = null,
    gold: GoldBill? = null,
    holdMillis: Int = Hold.CONFIRM_MS,
    onConfirm: () -> Unit,
) {
    val accent = if (danger) LifeRed else Gold
    ForgeSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        body = SheetBody.Scroll(padding = PaddingValues(start = SPINE + 18.dp, end = 18.dp, bottom = 12.dp), spacing = 12.dp),
        header = {
            Box(
                Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 12.dp).size(38.dp, 4.dp)
                    .background(Muted.copy(alpha = .35f), RoundedCornerShape(2.dp)),
            )
        },
        footer = {
            Column(Modifier.weight(1f).padding(start = SPINE + 2.dp, end = 2.dp)) {
                ForgeButton(
                    onClick = {
                        onDismiss()
                        onConfirm()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    kind = if (danger) ButtonKind.Danger else ButtonKind.Primary,
                    state = ButtonState.of(!blocked && gold?.affordable != false, busy),
                    hold = Hold(millis = holdMillis),
                ) { Text(ui("confirm.hold", confirm.lowercase()), textAlign = TextAlign.Center) }
                ForgeButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Text) { Text(ui("common.cancel").uppercase()) }
            }
        },
        contentModifier = Modifier.raritySpine(accent, SPINE),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            icon?.let { Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { it() } }
            Column(Modifier.weight(1f)) {
                Text(title, color = Parchment, style = MaterialTheme.typography.titleLarge)
                subtitle?.let { MutedText(it, style = MaterialTheme.typography.labelMedium) }
            }
        }
        val lines = ledger + gold?.goldLedger().orEmpty()
        if (lines.isNotEmpty()) Ledger(lines)
        options?.invoke()
        note?.let { MutedText(it) }
        (warning ?: gold?.takeUnless { it.affordable }?.shortText())?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = LifeRed, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                Text(it, color = LifeRed, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Ширина полосы редкости подтверждения вдоль левого края листа. */
private val SPINE = 5.dp

/** The account of an action, one line each, with what leaves and what arrives in their colours; сокращённое число - касанием полное. */
@Composable private fun Ledger(lines: List<LedgerLine>) {
    Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .25f)).border(1.dp, Muted.copy(alpha = .20f))) {
        lines.forEachIndexed { index, line ->
            if (index > 0) HorizontalDivider(color = Muted.copy(alpha = .12f))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MutedText(line.label)
                Tipped(line.full?.let { full -> { Tip(body = full) } }, Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        line.value,
                        color = when (line.tone) {
                            Tone.SPEND -> LifeRed
                            Tone.GAIN -> Vital
                            Tone.PLAIN -> Parchment
                        },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}
