package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.settings.SettingsViewModel
import com.sperance.exileforge.rules.content.Achievement
import com.sperance.exileforge.rules.content.Counter
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.RelicSerif
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Медаль титула по разделу летописи, где лежит счётчик его достижения: новый счётчик раздела получает медаль сам,
 * новый раздел - строку здесь; без строки - печать.
 */
private val SectionMedals: Map<String, ImageVector> = mapOf(
    "COMBAT" to ForgeGlyphs.Swords,
    "TRIALS" to ForgeGlyphs.Keep,
    "LOOT" to ForgeGlyphs.Coins,
    "CRAFT" to ForgeGlyphs.Anvil,
    "PROGRESS" to ForgeGlyphs.Atlas,
)

private fun medalOf(counter: String): ImageVector = Counter.SECTIONS.entries.firstOrNull { counter in it.value }?.let { SectionMedals[it.key] } ?: ForgeGlyphs.Sigil

/** Титул галереи: код, за что он (достижение и его последняя ступень) и получен ли. */
@Immutable
private data class TitleTile(val code: String, val achievement: Achievement, val earned: Boolean) {
    val reason: String get() = ui("chronicle.title_for", loc("achievement.${achievement.code}.name"), number(achievement.tiers.last().toDouble()))
}

/** Текущий титул героя крупно: Cinzel цвета `Caution`, без титула - приглушённо. */
@Composable internal fun CurrentTitle(title: String) {
    Column(
        Modifier.fillMaxWidth().depthPanel(RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(ui("chronicle.current_title"), color = Muted, style = MaterialTheme.typography.labelSmall)
        Text(
            title.takeIf { it.isNotBlank() }?.let(::titleName)?.uppercase() ?: ui("chronicle.no_title"),
            color = if (title.isNotBlank()) Caution else Muted,
            style = relicName(20),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * «Галерея медалей»: заголовок «Титулы · X / N ▾» сворачивает таблицу (помнит устройство,
 * [com.sperance.exileforge.presentation.state.GameSettings.titlesFolded]); сетка в два столбца - «Без титула» первой плиткой,
 * затем полученные: медаль, имя, за что; выбранный - в рамке `Gold`. Закрытых титулов галерея не показывает.
 */
@Composable internal fun TitleGallery(achievements: List<Achievement>, values: Map<String, Long>, current: String, enabled: Boolean, onPick: (String) -> Unit) {
    val settings = koinViewModel<SettingsViewModel>()
    val folded = settings.settings.collectAsStateWithLifecycle().value.titlesFolded
    val tiles = remember(achievements, values) {
        achievements.filter { it.title.isNotBlank() }.map { TitleTile(it.title, it, it.complete(values[it.counter] ?: 0L)) }.distinctBy { it.code }
    }
    val earned = tiles.filter { it.earned }
    Row(
        Modifier.fillMaxWidth().clickable { settings.change { copy(titlesFolded = !titlesFolded) } }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Engraved(ui("chronicle.titles_count", earned.size, tiles.size) + if (folded) " ▸" else " ▾", accent = Brass)
        HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = Brass.copy(alpha = .25f))
    }
    if (folded) return
    TileGrid(listOf<TitleTile?>(null) + earned) { tile ->
        if (tile == null) {
            MedalTile(Icons.Outlined.Block, ui("chronicle.no_title"), null, selected = current.isBlank(), enabled = enabled) { onPick("") }
        } else {
            MedalTile(medalOf(tile.achievement.counter), titleName(tile.code), tile.reason, selected = current == tile.code, enabled = enabled) { onPick(tile.code) }
        }
    }
}

/** Сетка в два столбца без ленивого списка: страница «Хроники» и так прокручивается целиком. */
@Composable private fun <T> TileGrid(items: List<T>, tile: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { Box(Modifier.weight(1f).fillMaxHeight()) { tile(it) } }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Плитка титула: медаль в кольце, имя Cinzel и за что; выбранная - в рамке `Gold`. */
@Composable private fun MedalTile(medal: ImageVector, name: String, reason: String?, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val ring = Brass
    Row(
        Modifier.fillMaxSize().clip(shape).background(PanelRaised)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Gold else Bronze, shape)
            .clickable(enabled = enabled && !selected, onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(34.dp).border(1.5.dp, ring, CircleShape).background(Panel, CircleShape), contentAlignment = Alignment.Center) {
            Icon(medal, null, tint = ring, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, color = if (selected) GoldBright else Parchment, fontFamily = RelicSerif, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            reason?.let { MutedText(it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}
