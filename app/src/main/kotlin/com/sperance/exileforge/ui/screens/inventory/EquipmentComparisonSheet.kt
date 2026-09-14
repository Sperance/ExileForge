package com.sperance.exileforge.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.PropertyRow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EquipmentComparisonSheet(s: ForgeState, vm: ForgeViewModel) {
    val result = s.comparison ?: return
    ModalBottomSheet(onDismissRequest = vm::dismissComparison) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Сравнение · ${s.compareSlot?.title.orEmpty()}", style = MaterialTheme.typography.headlineSmall)
            if(!result.allowed) Text(result.reason ?: "Предмет нельзя надеть", color = MaterialTheme.colorScheme.error)
            result.after?.let { after ->
                val changed = (after.values.keys + result.before.values.keys).filter { after.values[it] != result.before.values[it] }
                if(changed.isEmpty() && after.weapons == result.before.weapons) Text("Расчётные характеристики не изменятся")
                changed.forEach { key ->
                    val before = result.before.values[key] ?: 0.0; val next = after.values[key] ?: 0.0
                    PropertyRow(key.replace('_', ' '), "%.1f → %.1f (%+.1f)".format(Locale.ROOT, before, next, next - before), key)
                }
                (after.weapons.keys + result.before.weapons.keys).forEach { slot ->
                    val before = result.before.weapons[slot]?.dps ?: 0.0; val next = after.weapons[slot]?.dps ?: 0.0
                    PropertyRow("${slot.title} · DPS", "%.1f → %.1f".format(Locale.ROOT, before, next), "damage")
                }
                if(after.unsupported.isNotEmpty()) Text("Не учтены: ${after.unsupported.joinToString()}")
            }
            Button(enabled = result.allowed && !s.busy && result.characterVersion == s.inventoryVersion && s.pending == null, onClick = vm::equipCompared, modifier = Modifier.fillMaxWidth()) { Text("Подтвердить надевание") }
            Text("Расчёт выполнен сервером. Если экипировка изменилась, потребуется новое сравнение.")
        }
    }
}
