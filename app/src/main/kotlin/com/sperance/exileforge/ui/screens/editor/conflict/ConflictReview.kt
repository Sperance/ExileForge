package com.sperance.exileforge.ui.screens.editor.conflict

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.components.InfoCard
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.*

@Composable fun ConflictReview(s: ForgeState, vm: ForgeViewModel) {
    val review = s.mergeReview
    if(review == null) {
        InfoCard(tr("Запись изменена", "The record changed"), tr("Сравните исходное значение, свой черновик и актуальные данные. Ваш черновик сохранён.", "Compare the original value, your draft and the current data. Your draft is kept."))
        OutlinedButton(enabled = !s.busy, onClick = vm::reviewConflict) { Text(tr("Сравнить изменения", "Compare the changes")) }
        return
    }
    var choices by remember(review) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if(review.conflicts.isEmpty()) Text(tr("Изменения не пересекаются. Их можно объединить без потери данных.", "The changes do not overlap and can be merged without losing data."))
        review.conflicts.forEach { field ->
            ForgePanel(accent = MaterialTheme.colorScheme.error) {
                Text(field.field, style = MaterialTheme.typography.titleMedium)
                Text(tr("Было: ", "Was: ") + readable(field.original), color = Muted)
                Row { RadioButton(selected = choices[field.field] == true, onClick = { choices = choices + (field.field to true) }); Text(tr("Моё: ", "Mine: ") + readable(field.local)) }
                Row { RadioButton(selected = choices[field.field] == false, onClick = { choices = choices + (field.field to false) }); Text(tr("На сервере: ", "On the server: ") + readable(field.remote)) }
            }
        }
        Button(enabled = !s.busy && review.conflicts.all { it.field in choices }, onClick = { vm.resolveConflict(choices) }) { Text(tr("Перенести в черновик", "Move into the draft")) }
        Text(tr("Объединение не отправляет запись на сервер. Проверьте результат и сохраните его отдельно.", "Merging does not send anything to the server. Review the result and save it separately."), color = Muted)
    }
}
private fun readable(value: JsonElement?): String = when(value) {
    null, JsonNull -> tr("Не задано", "Not set")
    is JsonPrimitive -> value.content
    is JsonArray -> if(value.isEmpty()) tr("Пусто", "Empty") else value.joinToString("; ") { readable(it) }
    is JsonObject -> if(value.text("definitionId").isNotBlank()) "${value.text("definitionId")} · " + tr("ревизия", "revision") + " ${value.text("revision")}" else value.entries.joinToString("; ") { "${it.key}: ${readable(it.value)}" }
}
