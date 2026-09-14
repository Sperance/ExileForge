package com.sperance.exileforge.ui.screens.editor.conflict

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.InfoCard
import kotlinx.serialization.json.*

@Composable fun ConflictReview(s: ForgeState, vm: ForgeViewModel) {
    val review = s.mergeReview
    if(review == null) {
        InfoCard("Запись изменена", "Сравните исходное значение, свой черновик и актуальные данные. Ваш черновик сохранён.")
        OutlinedButton(enabled = !s.busy, onClick = vm::reviewConflict) { Text("Сравнить изменения") }
        return
    }
    var choices by remember(review) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if(review.conflicts.isEmpty()) Text("Изменения не пересекаются. Их можно объединить без потери данных.")
        review.conflicts.forEach { field ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text(field.field, style = MaterialTheme.typography.titleMedium)
                Text("Было: ${readable(field.original)}")
                Row { RadioButton(selected = choices[field.field] == true, onClick = { choices = choices + (field.field to true) }); Text("Моё: ${readable(field.local)}") }
                Row { RadioButton(selected = choices[field.field] == false, onClick = { choices = choices + (field.field to false) }); Text("На сервере: ${readable(field.remote)}") }
            } }
        }
        Button(enabled = !s.busy && review.conflicts.all { it.field in choices }, onClick = { vm.resolveConflict(choices) }) { Text("Перенести в черновик") }
        Text("Объединение не отправляет запись на сервер. Проверьте результат и сохраните его отдельно.")
    }
}
private fun readable(value: JsonElement?): String = when(value) {
    null, JsonNull -> "Не задано"
    is JsonPrimitive -> value.content
    is JsonArray -> if(value.isEmpty()) "Пусто" else value.joinToString("; ") { readable(it) }
    is JsonObject -> if(value.text("definitionId").isNotBlank()) "${value.text("definitionId")} · ревизия ${value.text("revision")}" else value.entries.joinToString("; ") { "${it.key}: ${readable(it.value)}" }
}
