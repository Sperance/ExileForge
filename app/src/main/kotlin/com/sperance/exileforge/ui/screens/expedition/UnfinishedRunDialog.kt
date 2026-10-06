package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.ui.components.DialogShape
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeTextButton
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import org.koin.compose.viewmodel.koinViewModel

/**
 * Незаконченный заход (3.89.0): приложение закрылось посреди похода, а сервер ещё держит его. Игрок выбирает сам - вернуться
 * на ту же карту (павшие лежат, открытые сундуки открыты, герой у входа) или покинуть заход. Без выбора окно не закрывается.
 */
@Composable fun UnfinishedRunHost() {
    val model: ExpeditionViewModel = koinViewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val activity by model.activity.collectAsStateWithLifecycle()
    val offer = state.unfinished ?: return
    AlertDialog(
        onDismissRequest = {},
        containerColor = PanelRaised,
        titleContentColor = Gold,
        shape = DialogShape,
        tonalElevation = 0.dp,
        title = { Text(ui("expedition.unfinished_title").uppercase(), style = MaterialTheme.typography.titleMedium) },
        text = { Text(ui("expedition.unfinished_text", mapTitle(offer.zone)), color = Parchment) },
        confirmButton = { ForgeButton(model::continueUnfinished, enabled = !activity.busy) { Text(ui("expedition.unfinished_continue")) } },
        dismissButton = { ForgeTextButton(model::abandonUnfinished, enabled = !activity.busy) { Text(ui("expedition.unfinished_leave")) } },
    )
}
