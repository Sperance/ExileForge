package com.sperance.exileforge.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.*
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*

@Composable fun CraftDetails(s: ForgeState) {
    val option = s.craftOptions?.options?.firstOrNull { it.currency == s.selectedCurrency }
    if(option != null) {
        InfoCard(option.name, "В наличии: ${option.amount}\n${currencyDescription(option.currency)}" + if(!option.available) "\nНедоступно: ${option.reason}" else "")
        if(s.craftOptions?.characterVersion != s.inventoryVersion) Text("Обновите сведения о сферах", color = MaterialTheme.colorScheme.error)
    }
    if(s.craftBefore != null && s.craftAfter != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Результат последнего крафта", style = MaterialTheme.typography.titleLarge)
            ItemCard(inventoryDocument(s.craftBefore, s.inventoryBases[s.craftBefore.text("equipmentId")]), enabled = false, detailed = true, definitions = s.inventoryDefinitions, actionLabel = "До")
            ItemCard(inventoryDocument(s.craftAfter, s.inventoryBases[s.craftAfter.text("equipmentId")]), enabled = false, detailed = true, definitions = s.inventoryDefinitions, actionLabel = "После")
        }
    }
}
private fun currencyDescription(id: String): String = when(id) {
    "TRANSMUTATION" -> "Превращает обычный предмет в магический."
    "ALTERATION" -> "Заново выбирает свойства магического предмета."
    "AUGMENTATION" -> "Добавляет свойство магическому предмету."
    "ALCHEMY" -> "Превращает обычный предмет в редкий."
    "CHAOS" -> "Заново выбирает свойства редкого предмета."
    "REGAL" -> "Делает магический предмет редким и добавляет свойство."
    "EXALTED" -> "Добавляет свойство редкому предмету."
    "ANNULMENT" -> "Удаляет случайное изменяемое свойство."
    "SCOURING" -> "Удаляет изменяемые явные свойства."
    "DIVINE" -> "Перебрасывает значения явных свойств в пределах их тиров."
    "BLESSED" -> "Перебрасывает значения встроенных свойств."
    "MIRROR" -> "Создаёт зеркальную копию предмета."
    "FRACTURING" -> "Закрепляет случайное явное свойство."
    else -> "Применяет механику сферы по правилам сервера."
}
