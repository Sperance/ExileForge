package com.sperance.exileforge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.theme.*

/** Строка шторки-аккордеона: имя группы, её текущий выбор словами и что раскрывается под ней по нажатию. */
class AccordionGroup(val key: String, val title: String, val value: String, val content: @Composable () -> Unit)

/** Переключатель под группами шторки: «Только сделанное мной», «Скрыть надетое» и подобные. */
class FilterSwitch(val title: String, val checked: Boolean, val onChange: (Boolean) -> Unit)

/**
 * Шторка фильтров «Аккордеон» (4.2.0) - одна на все списки приложения: поиск сверху, каждая группа - строка с текущим выбором,
 * что раскрывается по нажатию (открыта одна), ниже переключатели, внизу «Сбросить» и «Показать N» ([show]). Что выбрано -
 * решает список: шторка лишь рисует [groups] и [switches].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterAccordionSheet(
    query: String,
    onQuery: (String) -> Unit,
    queryHint: String,
    groups: List<AccordionGroup>,
    switches: List<FilterSwitch>,
    show: String,
    resettable: Boolean,
    onReset: () -> Unit,
    onShow: () -> Unit,
    onDismiss: () -> Unit,
) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(ui("filter.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                query,
                onQuery,
                placeholder = { Text(queryHint) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Outlined.Close, ui("common.clear")) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onShow() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Column(Modifier.fillMaxWidth().depthPanel(RoundedCornerShape(12.dp))) {
                groups.forEachIndexed { at, group ->
                    if (at > 0) HorizontalDivider(color = PanelRaised)
                    AccordionRow(group, group.key == open) { open = if (open == group.key) null else group.key }
                }
            }
            switches.forEach { switch ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(switch.title, color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = switch.checked, onCheckedChange = switch.onChange)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForgeOutlinedButton(onClick = onReset, enabled = resettable, modifier = Modifier.weight(1f)) { Text(ui("filter.reset")) }
                ForgeButton(onClick = onShow, modifier = Modifier.weight(1f)) { Text(show, maxLines = 1) }
            }
        }
    }
}

/** Строка группы: имя слева, выбор золотом справа и стрелка; раскрытая - с содержимым под ней. */
@Composable private fun AccordionRow(group: AccordionGroup, open: Boolean, onToggle: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onToggle).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(group.title, color = Parchment, style = MaterialTheme.typography.bodyMedium)
            Text(
                group.value,
                color = GoldBright,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
            Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
        AnimatedVisibility(open, enter = expandVertically(), exit = shrinkVertically()) {
            Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) { group.content() }
        }
    }
}

/** Выбор одного из [options] фишками: выбранная - золотом. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<T>, selected: (T) -> Boolean, label: (T) -> String, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            val on = selected(option)
            val pill = RoundedCornerShape(16.dp)
            Text(
                label(option),
                color = if (on) Ink else Parchment,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                modifier = Modifier.clip(pill).background(if (on) Gold else PanelRaised, pill).clickable(role = Role.RadioButton) { onPick(option) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

/** Редкости цветными плитками (4.2.0): плитка в цвете своей редкости ([rarityColor]), выбранная - залита им. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RarityTiles(rarities: List<Rarity>, selected: (Rarity) -> Boolean, onToggle: (Rarity) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rarities.forEach { rarity ->
            val color = rarityColor(rarity.name)
            val on = selected(rarity)
            val tile = RoundedCornerShape(10.dp)
            Text(
                rarityTitle(rarity),
                color = if (on) Ink else color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.clip(tile).background(if (on) color else color.copy(alpha = .10f), tile)
                    .border(1.dp, color.copy(alpha = if (on) 1f else .55f), tile)
                    .clickable(role = Role.Checkbox) { onToggle(rarity) }.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** Цвет значка фильтра: золото, когда список сужен. */
internal fun filterTint(tweaks: Int): Color = if (tweaks > 0) GoldBright else Muted
