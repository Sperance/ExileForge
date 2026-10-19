package com.sperance.exileforge.ui.screens.crafts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.CraftsRules
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeIconButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Parchment

/**
 * Статья справки ремёсел: заголовок `crafts.help.<key>` и текст `crafts.help.<key>_text` из словаря; числа в текст
 * подставляются из правил ремёсел, что пришли с сервера ([args]), - своих балансных чисел у справки нет.
 */
private class GlossaryEntry(val key: String, val args: (CraftsRules) -> List<Any> = { emptyList() })

/** Все показатели работ по порядку чтения карточки: от цикла к профессии и отлучке. */
private val GLOSSARY: List<GlossaryEntry> = listOf(
    GlossaryEntry("cycle") { listOf(number(it.levelSpeed), it.maxLevel) },
    GlossaryEntry("inputs"),
    GlossaryEntry("waste") { listOf(number(it.luckCap)) },
    GlossaryEntry("sure"),
    GlossaryEntry("yield"),
    GlossaryEntry("hour"),
    GlossaryEntry("experience"),
    GlossaryEntry("finds") { listOf(number(it.levelFind), it.maxLevel) },
    GlossaryEntry("additives"),
    GlossaryEntry("tool"),
    GlossaryEntry("bonuses") { listOf(number(it.luckCap)) },
    GlossaryEntry("level") { listOf(it.maxLevel, number(it.levelSpeed), number(it.levelFind)) },
    GlossaryEntry("away") { listOf(number(it.offlineHours)) },
)

/** Кнопка ⓘ шапки ремёсел: открывает справку по показателям работ, над ней - [intro] (описание профессии); пока правила не пришли, её нет. */
@Composable internal fun CraftsGlossaryButton(rules: CraftsRules?, intro: String = "") {
    rules ?: return
    var open by remember { mutableStateOf(false) }
    ForgeIconButton(Icons.Outlined.Info, ui("crafts.help.title"), onClick = { open = true }, compact = true)
    if (open) CraftsGlossary(rules, GLOSSARY, intro) { open = false }
}

/** Одна статья справки [key] - по касанию показателя работы. */
@Composable internal fun CraftsGlossaryEntry(rules: CraftsRules, key: String, onDismiss: () -> Unit) = CraftsGlossary(rules, GLOSSARY.filter { it.key == key }, onDismiss = onDismiss)

/** Справка: что значит каждый показатель [entries] на карточке работы, по правилам ремёсел; над ними - [intro], если он есть. */
@Composable private fun CraftsGlossary(rules: CraftsRules, entries: List<GlossaryEntry>, intro: String = "", onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (intro.isNotEmpty()) Text(intro, color = Parchment, style = MaterialTheme.typography.bodyMedium)
            Engraved(ui("crafts.help.title"))
            entries.forEach { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(ui("crafts.help.${entry.key}"), color = Gold, style = MaterialTheme.typography.titleSmall)
                    Text(ui("crafts.help.${entry.key}_text", *entry.args(rules).toTypedArray()), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
