package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.Place
import com.sperance.exileforge.core.campaign.PlaceQuery
import com.sperance.exileforge.core.campaign.matching
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.SearchBar
import com.sperance.exileforge.ui.components.SheetBody
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment

/** Слова места в листе поиска: подсказка строки [hint], как искать [help] и что делать, когда ничего не нашлось [noneHelp]; общие - `search.*`. */
@Immutable
internal class PlaceSearchText(val hint: String, val help: String, val noneHelp: String)

/**
 * Лист поиска места на карте - один на «Поход» (зоны) и Атлас (узлы): строка ввода в шапке, ниже ленивым списком - пока запрос пуст,
 * как искать и недавние места [recent]; с запросом - «Найдено: N» и совпадения [places] ([PlaceQuery]: имя, число - уровень ±2,
 * «20-30» - вилка); ничего - что сделать дальше. Строка - имя и сведения места; касание закрывает лист и отдаёт место [onPick].
 */
@Composable internal fun <K> PlaceSearchSheet(places: List<Place<K>>, recent: List<Place<K>>, text: PlaceSearchText, onDismiss: () -> Unit, onPick: (K) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val parsed = remember(query) { PlaceQuery.of(query) }
    val found = remember(places, parsed) { places.matching(parsed) }
    val pick: (Place<K>) -> Unit = { place ->
        onDismiss()
        onPick(place.key)
    }
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Fixed(),
        header = { SearchBar(query, { query = it }, text.hint, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) },
    ) {
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f, fill = false),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when {
                parsed == PlaceQuery.Blank -> {
                    item { MutedText(text.help) }
                    if (recent.isNotEmpty()) rows(ui("search.recent"), recent, pick)
                }

                found.isEmpty() -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ui("search.none"), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                        MutedText(text.noneHelp)
                    }
                }

                else -> rows(ui("search.found", found.size), found, pick)
            }
        }
    }
}

/** Заголовок группы и строки мест [places]. */
private fun <K> LazyListScope.rows(title: String, places: List<Place<K>>, onPick: (Place<K>) -> Unit) {
    item { Text(title.uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp)) }
    items(places, key = { "${it.key}" }) { PlaceRow(it) { onPick(it) } }
}

/** Строка места: имя и сведения; вся строка - касание выбора, не ниже 48 dp. */
@Composable private fun PlaceRow(place: Place<*>, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).background(Panel, shape).border(1.dp, Bronze.copy(alpha = .5f), shape).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(place.title, color = Gold, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        MutedText(place.facts, style = MaterialTheme.typography.labelSmall)
    }
}
