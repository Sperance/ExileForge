package com.sperance.exileforge.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.OrnateDivider
import com.sperance.exileforge.ui.components.PropertyRow
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EquipmentComparisonSheet(s: ForgeState, vm: ForgeViewModel) {
    val result = s.comparison ?: return
    ModalBottomSheet(onDismissRequest = vm::dismissComparison, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr("Сравнение", "Comparison").uppercase() + " · ${s.compareSlot?.title(s.lang).orEmpty()}", style = MaterialTheme.typography.headlineSmall, color = Gold)
            OrnateDivider()
            if(!result.allowed) Text(result.reason ?: tr("Предмет нельзя надеть", "The item cannot be equipped"), color = MaterialTheme.colorScheme.error)
            result.after?.let { after ->
                val changed = (after.values.keys + result.before.values.keys).filter { after.values[it] != result.before.values[it] }
                if(changed.isEmpty() && after.weapons == result.before.weapons) Text(tr("Расчётные характеристики не изменятся", "The calculated stats will not change"))
                changed.forEach { key ->
                    val before = result.before.values[key] ?: 0.0; val next = after.values[key] ?: 0.0
                    PropertyRow(statTitle(key), "%.1f → %.1f (%+.1f)".format(Locale.ROOT, before, next, next - before), key)
                }
                (after.weapons.keys + result.before.weapons.keys).forEach { slot ->
                    val before = result.before.weapons[slot]?.dps ?: 0.0; val next = after.weapons[slot]?.dps ?: 0.0
                    PropertyRow("${slot.title(s.lang)} · DPS", "%.1f → %.1f".format(Locale.ROOT, before, next), "damage")
                }
                if(after.unsupported.isNotEmpty()) Text(tr("Не учтены: ${after.unsupported.joinToString()}", "Not covered: ${after.unsupported.joinToString()}"), color = Muted)
            }
            Button(enabled = result.allowed && !s.busy && result.characterVersion == s.inventoryVersion && s.pending == null, onClick = vm::equipCompared, modifier = Modifier.fillMaxWidth()) { Text(tr("Подтвердить надевание", "Confirm equipping")) }
            Text(tr("Расчёт выполнен сервером. Если экипировка изменилась, потребуется новое сравнение.", "The server did the maths. If the equipment changed, compare again."), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
