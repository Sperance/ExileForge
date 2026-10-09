package com.sperance.exileforge.ui.screens.city

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.FoundUnique
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.UniqueAlbum
import com.sperance.exileforge.core.display.UniquePool
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.stampText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.UniqueFind
import com.sperance.exileforge.presentation.history.HistoryViewModel
import com.sperance.exileforge.rules.content.ItemTemplate
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** «История» (3.90.2): здание Города со списком разделов прошлого героя; пока раздел один - найденные уникалки. */
@Composable internal fun HistoryScreen() {
    val vm = koinViewModel<HistoryViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val index = game.index
    val finds by produceState<Map<String, UniqueFind>?>(null, game.heroId) { value = game.heroId.takeIf { it.isNotBlank() }?.let { vm.uniques(it) } }
    val total = remember(index) { index?.let { UniqueAlbum(it).all.size } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(ui("history.title"), ui("history.subtitle"), ForgeGlyphs.Tome)
        val shape = RoundedCornerShape(14.dp)
        Row(
            Modifier.fillMaxWidth().clip(shape).depthPanel(shape).border(1.dp, Bronze, shape).clickable(onClick = vm::openUniques).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(ForgeGlyphs.Gem, null, tint = rarityColor("UNIQUE"), modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(ui("history.uniques"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
                val count = finds?.size
                MutedText(if (count != null && total != null) ui("history.found", count, total) else ui("history.uniques_hint"))
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
        }
    }
}

/** Что выбрано чипом: все находки, один пул или боссы. */
private sealed interface AlbumPick {
    data object All : AlbumPick
    data class Pool(val tag: String) : AlbumPick
    data object Bosses : AlbumPick
}

/**
 * Найденные уникалки (3.90.2, макет А2): сверху общий счёт «найдено из всех», ниже чипы пулов со счётом; выбранный пул -
 * плашка с прогрессом и числом ненайденных, под ней найденные с датой первой находки, свежие первыми. Ненайденные не
 * называются - только числом. Чип «Боссы» - список боссов со счётом, раскрытый босс показывает свои находки. Нажатие на
 * находку - полная карточка шаблона со вилками строк.
 */
@Composable fun UniquesScreen() {
    val vm = koinViewModel<HistoryViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val index = game.index
    val finds by produceState<Map<String, UniqueFind>?>(null, game.heroId) { value = game.heroId.takeIf { it.isNotBlank() }?.let { vm.uniques(it) } }
    val album = remember(index, game.lang) { index?.let { UniqueAlbum(it, game.lang) } }
    var pick by remember { mutableStateOf<AlbumPick>(AlbumPick.All) }
    var opened by remember { mutableStateOf<FoundUnique?>(null) }
    var boss by remember { mutableStateOf<String?>(null) }
    // Цепочка «назад» (4.4.x): «← Город › История › Уникалки»
    BackTrailHost(ui("nav.city")) {
        TrailLevel(ui("history.title"), onBack = vm::city) {
            TrailLevel(ui("history.uniques"), onBack = vm::back) {
                Column(Modifier.fillMaxSize()) {
                    val known = finds
                    if (album == null || known == null) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Gold) }
                        return@Column
                    }
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            ScreenHeader(ui("history.uniques"), ui("history.found", album.all.count { it.code in known }, album.all.size), ForgeGlyphs.Gem)
                        }
                        item {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PoolChip(ui("common.all"), pick == AlbumPick.All) { pick = AlbumPick.All }
                                album.pools.forEach { pool ->
                                    PoolChip(ui("history.pool_count", pool.title, pool.count(known), pool.templates.size), pick == AlbumPick.Pool(pool.tag)) { pick = AlbumPick.Pool(pool.tag) }
                                }
                                if (album.bosses.isNotEmpty()) PoolChip(ui("history.bosses"), pick == AlbumPick.Bosses) { pick = AlbumPick.Bosses }
                            }
                        }
                        when (val chosen = pick) {
                            AlbumPick.All -> {
                                val all = album.found(known)
                                if (all.isEmpty()) item { InfoCard(ui("history.empty"), ui("history.empty_hint")) }
                                items(all, key = { it.template.code }) { found -> FoundRow(found) { opened = found } }
                            }

                            is AlbumPick.Pool -> {
                                val pool = album.pools.firstOrNull { it.tag == chosen.tag } ?: return@LazyColumn
                                item { PoolPlate(pool, known) }
                                items(pool.found(known), key = { it.template.code }) { found -> FoundRow(found) { opened = found } }
                            }

                            AlbumPick.Bosses -> items(album.bosses, key = { it.tag }) { pool ->
                                BossRow(pool, known, open = boss == pool.tag, onToggle = { boss = if (boss == pool.tag) null else pool.tag }) { opened = it }
                            }
                        }
                    }
                }
                opened?.let { found ->
                    val view = remember(found.template.code, index) { index?.let { ItemView.showcase(found.template, it) } }
                    ForgeSheet(onDismissRequest = { opened = null }) {
                        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(ui("history.first_find", stampText(found.find.at), found.find.count), color = Muted, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            view?.let { ItemCard(it, enabled = false, detailed = true, action = false) }
                        }
                    }
                }
            }
        }
    }
}

/** Плашка пула: название, «найдено N из M», полоса прогресса и сколько ещё не найдено. */
@Composable private fun PoolPlate(pool: UniquePool, finds: Map<String, UniqueFind>) {
    val count = pool.count(finds)
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().depthRaised(shape).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pool.title, color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            MutedText(ui("history.missing", pool.templates.size - count))
        }
        LinearProgressIndicator(
            progress = { if (pool.templates.isEmpty()) 0f else count / pool.templates.size.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Gold,
            trackColor = Bronze,
        )
    }
}

/** Одна находка: иконка шаблона, имя в цвете редкости, место и сколько всего, справа дата первой находки. */
@Composable private fun FoundRow(found: FoundUnique, onClick: () -> Unit) {
    val template = found.template
    val tint = rarityColor(template.rarity.name)
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).depthPanel(shape).clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TemplateIcon(template)
        Column(Modifier.weight(1f)) {
            Text(equipmentTitle(template.code), color = tint, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            MutedText(listOfNotNull(slotTitle(template.slot), ui("history.times", found.find.count).takeIf { found.find.count > 1 }).joinToString(" · "), style = MaterialTheme.typography.labelSmall)
        }
        Text(stampText(found.find.at).replaceFirst(' ', '\n'), color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
    }
}

/** Босс в группе «Боссы»: имя и счёт; раскрытый - его находки. */
@Composable private fun BossRow(pool: UniquePool, finds: Map<String, UniqueFind>, open: Boolean, onToggle: () -> Unit, onOpen: (FoundUnique) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.fillMaxWidth().depthPanel(shape), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(pool.title, color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(ui("history.count", pool.count(finds), pool.templates.size), color = Gold, style = MaterialTheme.typography.labelMedium)
            Text(if (open) "  ▾" else "  ▸", color = Gold)
        }
        if (open) {
            Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val found = pool.found(finds)
                found.forEach { FoundRow(it) { onOpen(it) } }
                val missing = pool.templates.size - found.size
                if (missing > 0) MutedText(ui("history.missing", missing))
            }
        }
    }
}

@Composable private fun PoolChip(label: String, on: Boolean, onClick: () -> Unit) {
    FilterChip(selected = on, onClick = onClick, label = { Text(label, maxLines = 1) })
}

@Composable private fun TemplateIcon(template: ItemTemplate) {
    val tint = rarityColor(template.rarity.name)
    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
        if (!SpriteIcon(equipmentIcon(template.code), tint, Modifier.size(32.dp), halo = false)) Icon(ForgeGlyphs.Gem, null, tint = tint, modifier = Modifier.size(24.dp))
    }
}
