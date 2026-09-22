package com.sperance.exileforge.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel

/**
 * "Are you sure" for the handful of things that cannot be taken back cheaply.
 *
 * It exists for actions that cost something the player cannot simply earn again in a moment: an
 * orb spent giving a node back, an item sold to a merchant, a purchase that is final. The dialog
 * carries the price in its text rather than only in the button, because the price is the thing
 * being confirmed.
 *
 * It is deliberately not on everything. A dialog in front of an ordinary action stops being read.
 */
@Composable fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String = tr("Подтвердить", "Confirm"),
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text(title) },
        text = { Text(text, color = Muted) },
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Отмена", "Cancel")) } },
    )
}
