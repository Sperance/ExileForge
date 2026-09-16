package com.sperance.exileforge.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.*
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*

@Composable fun CraftDetails(s: ForgeState) {
    val option = s.craftOptions?.options?.firstOrNull { it.currency == s.selectedCurrency }
    if(option != null) {
        InfoCard(option.name, tr("В наличии: ${option.amount}", "In stock: ${option.amount}") + "\n" + currencyDescription(option.currency) +
            if(!option.available) "\n" + tr("Недоступно: ${option.reason}", "Unavailable: ${option.reason}") else "")
        if(s.craftOptions?.characterVersion != s.inventoryVersion) Text(tr("Обновите сведения о сферах", "Refresh the orb information"), color = MaterialTheme.colorScheme.error)
    }
    if(s.craftBefore != null && s.craftAfter != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr("Результат последнего крафта", "Last crafting result").uppercase(), style = MaterialTheme.typography.titleMedium, color = com.sperance.exileforge.ui.theme.Gold)
            OrnateDivider()
            ItemCard(inventoryDocument(s.craftBefore, s.inventoryBases[s.craftBefore.text("equipmentId")]), enabled = false, detailed = true, definitions = s.inventoryDefinitions, actionLabel = tr("До", "Before"))
            ItemCard(inventoryDocument(s.craftAfter, s.inventoryBases[s.craftAfter.text("equipmentId")]), enabled = false, detailed = true, definitions = s.inventoryDefinitions, actionLabel = tr("После", "After"))
        }
    }
}
private fun currencyDescription(id: String): String = when(id) {
    "TRANSMUTATION" -> tr("Превращает обычный предмет в магический.", "Upgrades a normal item to magic.")
    "ALTERATION" -> tr("Заново выбирает свойства магического предмета.", "Rerolls the modifiers of a magic item.")
    "AUGMENTATION" -> tr("Добавляет свойство магическому предмету.", "Adds a modifier to a magic item.")
    "ALCHEMY" -> tr("Превращает обычный предмет в редкий.", "Upgrades a normal item to rare.")
    "CHAOS" -> tr("Заново выбирает свойства редкого предмета.", "Rerolls the modifiers of a rare item.")
    "REGAL" -> tr("Делает магический предмет редким и добавляет свойство.", "Upgrades a magic item to rare and adds a modifier.")
    "EXALTED" -> tr("Добавляет свойство редкому предмету.", "Adds a modifier to a rare item.")
    "ANNULMENT" -> tr("Удаляет случайное изменяемое свойство.", "Removes a random mutable modifier.")
    "SCOURING" -> tr("Удаляет изменяемые явные свойства.", "Removes mutable explicit modifiers.")
    "DIVINE" -> tr("Перебрасывает значения явных свойств в пределах их тиров.", "Rerolls explicit values within their tiers.")
    "BLESSED" -> tr("Перебрасывает значения встроенных свойств.", "Rerolls implicit values.")
    "MIRROR" -> tr("Создаёт зеркальную копию предмета.", "Creates a mirrored copy of the item.")
    "FRACTURING" -> tr("Закрепляет случайное явное свойство.", "Fractures a random explicit modifier.")
    else -> tr("Применяет механику сферы по правилам сервера.", "Applies the orb mechanic by the server's rules.")
}
