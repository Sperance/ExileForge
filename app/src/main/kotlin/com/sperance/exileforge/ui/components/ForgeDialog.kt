package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.PanelRaised

/** Диалог «Мягкого» стиля: по центру, приподнятый фон, скругление 22 dp, без рамки и тени. */
val DialogShape = RoundedCornerShape(22.dp)

/** Окно-диалог «Мягкого» стиля: заголовок [title] заглавными, тело [body] столбцом, внизу - «Закрыть». */
@Composable internal fun ForgeDialog(title: String, onDismiss: () -> Unit, body: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelRaised,
        titleContentColor = Gold,
        shape = DialogShape,
        tonalElevation = 0.dp,
        title = {
            ChordOverWindow()
            Text(title.uppercase(), style = MaterialTheme.typography.titleMedium)
        },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = body) },
        confirmButton = { ForgeButton(onClick = onDismiss, kind = ButtonKind.Text) { Text(ui("common.close")) } },
    )
}
