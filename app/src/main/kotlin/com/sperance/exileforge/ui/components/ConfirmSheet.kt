package com.sperance.exileforge.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.launch

/** How long the confirm button has to be held. Long enough to mean it, short enough not to wait. */
const val HOLD_TO_CONFIRM_MS = 700

/** Whether a line of the ledger is taken away, handed over, or only said. */
enum class Tone { PLAIN, SPEND, GAIN }

/** One line of what an action costs and brings: «Спишется — 4 × Сфера хаоса». */
data class LedgerLine(val label: String, val value: String, val tone: Tone = Tone.PLAIN)

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
 * does without looking; holding for [HOLD_TO_CONFIRM_MS] is what they do on purpose, and the band
 * filling under the thumb shows the moment it becomes real. A screen reader cannot hold, so the
 * same button answers an accessibility click at once — that tap is deliberate by construction.
 *
 * It is deliberately not on everything. A question in front of an ordinary action stops being read.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ConfirmSheet(
    title: String,
    confirm: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    ledger: List<LedgerLine> = emptyList(),
    note: String? = null,
    warning: String? = null,
    danger: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
    onConfirm: () -> Unit,
) {
    val accent = if (danger) LifeRed else Gold
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, shape = RectangleShape,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), dragHandle = null) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            RaritySpine(accent, 5.dp)
            Column(Modifier.weight(1f).padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(38.dp, 4.dp)
                    .background(Muted.copy(alpha = .35f), RoundedCornerShape(2.dp)))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    icon?.let { Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { it() } }
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Parchment, style = MaterialTheme.typography.titleLarge)
                        subtitle?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelMedium) }
                    }
                }
                if (ledger.isNotEmpty()) Ledger(ledger)
                note?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
                warning?.let {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Outlined.ErrorOutline, null, tint = LifeRed, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                        Text(it, color = LifeRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HoldButton(ui("confirm.hold", confirm.lowercase()), accent) { onDismiss(); onConfirm() }
                Text(ui("common.cancel").uppercase(), color = Muted, style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss).padding(vertical = 8.dp))
            }
        }
    }
}

/** The account of an action, one line each, with what leaves and what arrives in their colours. */
@Composable private fun Ledger(lines: List<LedgerLine>) {
    Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .25f)).border(1.dp, Muted.copy(alpha = .20f))) {
        lines.forEachIndexed { index, line ->
            if (index > 0) HorizontalDivider(color = Muted.copy(alpha = .12f))
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(line.label, color = Muted, style = MaterialTheme.typography.bodySmall)
                Text(line.value, color = when (line.tone) { Tone.SPEND -> LifeRed; Tone.GAIN -> Vital; Tone.PLAIN -> Parchment },
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * A button that has to be held.
 *
 * The band fills from the left while the finger is down and drains when it lifts early; reaching
 * the end is the confirmation, with a knock of haptics so the hand knows as well as the eye.
 *
 * In a sheet it fires once, because the sheet goes with it. [rearm] is for a button that stays —
 * the forge's, where the same orb is spent again and again — and empties the band after each hold.
 */
@Composable fun HoldButton(label: String, accent: Color, modifier: Modifier = Modifier, enabled: Boolean = true,
    rearm: Boolean = false, onHeld: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val held by rememberUpdatedState(onHeld)
    val live by rememberUpdatedState(enabled)
    var fired by remember { mutableStateOf(false) }
    val fire = {
        if (!fired && live) {
            fired = true; held()
            if (rearm) { fired = false; scope.launch { progress.animateTo(0f, tween(180)) } }
        }
    }
    val tint = if (enabled) accent else Muted.copy(alpha = .45f)
    Box(modifier.fillMaxWidth().height(52.dp).background(Abyss).border(1.dp, tint)
        .drawBehind {
            drawRect(Brush.horizontalGradient(listOf(accent.copy(alpha = .55f), accent.copy(alpha = .25f))),
                size = Size(size.width * progress.value, size.height))
        }
        .semantics(mergeDescendants = true) { role = Role.Button; if (!enabled) disabled(); onClick(label) { fire(); true } }
        .pointerInput(Unit) {
            detectTapGestures(onPress = {
                if (!live) return@detectTapGestures
                val filling = scope.launch {
                    progress.animateTo(1f, tween(HOLD_TO_CONFIRM_MS, easing = LinearEasing))
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    fire()
                }
                tryAwaitRelease()
                if (progress.value < 1f) {
                    filling.cancel()
                    scope.launch { progress.animateTo(0f, tween(180)) }
                }
            })
        },
        contentAlignment = Alignment.Center) {
        Text(label.uppercase(), color = if (!enabled) Muted else if (accent == LifeRed) Parchment else GoldBright,
            style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp))
    }
}
