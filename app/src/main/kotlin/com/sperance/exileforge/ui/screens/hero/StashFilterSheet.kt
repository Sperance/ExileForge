package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.StashFilter
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.theme.*

/**
 * Сколько настроек листа фильтров отличается от обычных (3.90.3): каждая редкость, «могу надеть», поиск и показанное надетое
 * ([showsWorn]: скрытое - обычное с 3.77.0). Группа - рейка, порядок - строка управления: их видно и без счёта.
 */
internal fun stashTweaks(filter: StashFilter, showsWorn: Boolean = false): Int = filter.rarities.size + (if (filter.wearable) 1 else 0) + (if (filter.query.isNotBlank()) 1 else 0) +
    (if (showsWorn) 1 else 0)

/** «Hide equipped» (3.69.0): the gear shelf without what the hero wears or has socketed; the same chip sits in the sheet. */
@Composable internal fun HideWornChip(on: Boolean, onToggle: (Boolean) -> Unit) {
    FilterChip(selected = on, onClick = { onToggle(!on) }, label = { Text(ui("stash.hide_worn"), maxLines = 1) })
}

/**
 * The stash's one filter glyph (3.91.0: a small circle, not a full touch button): lit and badged with the count of what narrows the
 * shelf; its size is required, so no row squeezes it.
 */
@Composable internal fun StashFilterButton(tweaks: Int, onClick: () -> Unit) {
    val tint = if (tweaks > 0) GoldBright else Muted
    BadgedBox(badge = { if (tweaks > 0) Badge(containerColor = Vital, contentColor = Ink) { Text(tweaks.toString(), fontSize = 9.sp, maxLines = 1) } }) {
        Box(
            Modifier.requiredSize(30.dp).clip(CircleShape).border(1.dp, tint.copy(alpha = .6f), CircleShape).clickable(role = Role.Button, onClickLabel = ui("stash.filters"), onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.FilterList, ui("stash.filters"), tint = tint, modifier = Modifier.size(16.dp)) }
    }
}

/**
 * Что сужает список, одним листом (3.90.3): поиск, «могу надеть», «скрыть надетое» и редкости, и сброс их всех. Группа -
 * рейка, порядок - строка управления. [hideWorn] - null там, где правило не действует (инструменты), и его фишки нет.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun StashFilterSheet(
    filter: StashFilter,
    lang: Lang,
    rarities: List<Rarity>,
    onFilter: (StashFilter) -> Unit,
    hideWorn: Boolean?,
    onHideWorn: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(ui("stash.filters"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                filter.query, { onFilter(filter.copy(query = it.take(DefaultInputs.search))) }, placeholder = { Text(ui("hero.search_hint")) },
                label = { Text(ui("hero.find_item")) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (filter.query.isNotEmpty()) IconButton(onClick = { onFilter(filter.copy(query = "")) }) { Icon(Icons.Outlined.Close, null) } },
                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onDismiss() }),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter.wearable, onClick = { onFilter(filter.copy(wearable = !filter.wearable)) }, label = { Text(ui("stash.can_wear")) })
                hideWorn?.let { HideWornChip(it, onHideWorn) }
            }
            if (rarities.isNotEmpty()) {
                Engraved(ui("stash.filter_rarity"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rarities.forEach { rarity ->
                        FilterChip(
                            selected = rarity in filter.rarities,
                            onClick = { onFilter(filter.toggle(rarity)) },
                            label = { Text(rarityTitle(rarity, lang), color = rarityColor(rarity.name)) },
                        )
                    }
                }
            }
            ForgeOutlinedButton(
                onClick = {
                    onFilter(StashFilter(group = filter.group))
                    if (hideWorn == false) onHideWorn(true)
                },
                enabled = stashTweaks(filter, showsWorn = hideWorn == false) > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(ui("stash.reset")) }
        }
    }
}
