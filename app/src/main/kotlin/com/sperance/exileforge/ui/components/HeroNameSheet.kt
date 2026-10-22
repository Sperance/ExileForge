package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.InputLimits
import com.sperance.exileforge.rules.text.NameCharset
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted

/**
 * Лист нового имени героя - один на «Лист Угнетения» из сумки и смену имени модерацией в досье: поле имени по правилам имени (алфавит
 * [NameCharset], длина `inputs.heroNameMin..heroName` со счётчиком), прежнее имя [current] под заголовком [title]. С [reasonLabel] - ещё
 * обязательная причина для игрока (модерация: она уйдёт ему письмом). Подтверждение - удержанием; то же имя или пустая причина - кнопка
 * погашена. Свободно ли имя, решает сервер.
 */
@Composable fun HeroNameSheet(
    title: String,
    current: String,
    limits: InputLimits,
    busy: Boolean,
    onDismiss: () -> Unit,
    reasonLabel: String? = null,
    onConfirm: (name: String, reason: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val fresh = name.trim()
    val ready = fresh.length in limits.heroNameMin..limits.heroName && fresh != current && (reasonLabel == null || reason.isNotBlank())
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(spacing = 10.dp),
        footer = {
            ForgeButton(onClick = { onConfirm(fresh, reason.trim()) }, modifier = Modifier.weight(1f), state = ButtonState.of(!busy && ready), hold = Hold()) {
                Text(ui("rename.hold"))
            }
        },
    ) {
        Text(title, color = GoldBright, style = MaterialTheme.typography.titleMedium)
        MutedText(ui("rename.current", current))
        val rules = ui("chars.name_unique")
        OutlinedTextField(
            name,
            { name = NameCharset.filter(it).take(limits.heroName) },
            label = { Text(ui("rename.new_name")) },
            suffix = { Text("${name.length}/${limits.heroName}", color = Muted, style = MaterialTheme.typography.labelMedium) },
            trailingIcon = { InfoMark(rules, tip = { Tip(rules, ui("common.name_charset"), Muted) }) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        MutedText(ui("rename.note"))
        reasonLabel?.let { label ->
            OutlinedTextField(
                reason,
                { reason = it.take(limits.sanctionComment) },
                label = { Text(label) },
                minLines = 2,
                supportingText = { LengthCounter(reason, limits.sanctionComment) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
