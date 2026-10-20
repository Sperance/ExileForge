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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.Feature
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.level
import com.sperance.exileforge.core.app.unlocked
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
import com.sperance.exileforge.rules.content.ItemTemplate
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.screens.hero.ChronicleScreen
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import com.sperance.exileforge.ui.screens.hero.chronicleDone
import com.sperance.exileforge.ui.screens.hero.titleName
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * «История»: здание Города со списком разделов прошлого героя плитками - найденные уникалки и Летопись (деяния и титулы); до уровня
 * открытия Летописи её плитка под замком.
 */
@Composable internal fun HistoryScreen() {
    val vm = koinViewModel<HistoryViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    val index = game.index
    LaunchedEffect(game.heroId, game.sessionEpoch) { heroModel.ensure() }
    val finds by produceState<Map<String, UniqueFind>?>(null, game.heroId) { value = game.heroId.takeIf { it.isNotBlank() }?.let { vm.uniques(it) } }
    val album = remember(index) { index?.let { UniqueAlbum(it) } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val known = finds
        HistoryTile(ForgeGlyphs.Gem, rarityColor("UNIQUE"), ui("history.uniques"), if (known != null && album != null) ui("history.found", album.count(known), album.all.size) else ui("history.uniques_hint"), onOpen = vm::openUniques)
        val chronicle = Feature.CHRONICLE.takeUnless { game.unlocked(it) }?.level(index?.rules)
        HistoryTile(ForgeGlyphs.Scroll, GoldBright, ui("chronicle.title"), chronicle?.let { ui("unlock.from", it) } ?: chronicleNews(game), onOpen = vm::openChronicle.takeIf { chronicle == null })
    }
}

/** Плитка раздела «Истории»: знак, имя и строка того, что внутри; [onOpen] null - раздел под замком, плитка тусклая и не нажимается. */
@Composable private fun HistoryTile(icon: ImageVector, tint: Color, title: String, line: String, onOpen: (() -> Unit)?) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().alpha(if (onOpen == null) LOCKED_ALPHA else 1f).clip(shape).depthPanel(shape).border(1.dp, Bronze, shape)
            .then(onOpen?.let { Modifier.clickable(onClick = it) } ?: Modifier).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = GoldBright, style = MaterialTheme.typography.titleMedium)
            MutedText(line)
        }
        Icon(if (onOpen == null) Icons.Outlined.Lock else Icons.Outlined.ChevronRight, null, tint = Muted)
    }
}

/** Летопись: сколько деяний свершено и носимый титул. */
private fun chronicleNews(game: GameUi): String {
    val done = game.chronicleDone()?.let { (done, all) -> ui("chronicle.done", done, all) } ?: return ui("common.loading")
    val title = game.hero?.info?.title?.takeIf { it.isNotBlank() } ?: return done
    return "$done · ${titleName(title)}"
}

/** Летопись - экран над «Историей»: «назад» ведёт в неё; героя читает сама. */
@Composable fun HistoryChronicleScreen() {
    val vm = koinViewModel<HistoryViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    LaunchedEffect(game.heroId, game.sessionEpoch) { heroModel.ensure() }
    BackTrailHost(ui("nav.city")) {
        TrailLevel(ui("history.title"), onBack = vm::city) {
            TrailLevel(ui("chronicle.title"), onBack = vm::back) { ChronicleScreen(game, heroModel) }
        }
    }
}

/** Плитка закрытого раздела - вполсилы. */
private const val LOCKED_ALPHA = .55f

/** Что выбрано чипом: все находки, один пул или боссы. */
private sealed interface AlbumPick {
    data object All : AlbumPick
    data class Pool(val tag: String) : AlbumPick
    data object Bosses : AlbumPick
}

/**
 * Найденные уникалки: сверху общий счёт «найдено из всех», ниже чипы пулов со счётом - только пулов с находками; выбранный пул -
 * плашка с прогрессом и числом ненайденных, под ней найденные с датой первой находки, свежие первыми. Ненайденные не
 * называются - только числом. Чип «Боссы» - список боссов со счётом, только с находками, раскрытый босс показывает свои находки.
 * Выбранный пул или боссы без находок - выбор «Все». Нажатие на находку - полная карточка шаблона со вилками строк.
 *
 * Печать - наименьший номер копии в мире героя; номера не было - печати нет.
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
    BackTrailHost(ui("nav.city")) {
        TrailLevel(ui("history.title"), onBack = vm::city) {
            TrailLevel(ui("history.uniques"), onBack = vm::back) {
                Column(Modifier.fillMaxSize()) {
                    val known = finds
                    if (album != null && known != null) TrailData(ui("history.count", album.count(known), album.all.size))
                    if (album == null || known == null) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Gold) }
                        return@Column
                    }
                    val bosses = album.bossesFound(known)
                    val pools = album.pools.filter { it.count(known) > 0 }
                    val shown = when (val chosen = pick) {
                        AlbumPick.All -> chosen
                        is AlbumPick.Pool -> chosen.takeIf { pools.any { it.tag == chosen.tag } } ?: AlbumPick.All
                        AlbumPick.Bosses -> chosen.takeIf { bosses.isNotEmpty() } ?: AlbumPick.All
                    }
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PoolChip(ui("common.all"), shown == AlbumPick.All) { pick = AlbumPick.All }
                                pools.forEach { pool ->
                                    PoolChip(ui("history.pool_count", pool.title, pool.count(known), pool.templates.size), shown == AlbumPick.Pool(pool.tag)) { pick = AlbumPick.Pool(pool.tag) }
                                }
                                if (bosses.isNotEmpty()) PoolChip(ui("history.bosses"), shown == AlbumPick.Bosses) { pick = AlbumPick.Bosses }
                            }
                        }
                        when (val chosen = shown) {
                            AlbumPick.All -> {
                                val all = album.found(known)
                                if (all.isEmpty()) item { InfoCard(ui("history.empty"), ui("history.empty_hint")) }
                                items(all, key = { it.template.code }) { found -> FoundRow(found) { opened = found } }
                            }

                            is AlbumPick.Pool -> {
                                val pool = pools.first { it.tag == chosen.tag }
                                item { PoolPlate(pool, known) }
                                items(pool.found(known), key = { it.template.code }) { found -> FoundRow(found) { opened = found } }
                            }

                            AlbumPick.Bosses -> items(bosses, key = { it.tag }) { pool ->
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
                            found.find.lowest.takeIf { it > 0 }?.let { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SerialSeal(it, relicLook(found.template.rarity)) } }
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

/** Одна находка: иконка шаблона, имя в цвете редкости, место, сколько всего и наименьший номер, справа дата первой находки. */
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
            MutedText(
                listOfNotNull(slotTitle(template.slot), ui("history.times", found.find.count).takeIf { found.find.count > 1 }, ui("history.serial", found.find.lowest).takeIf { found.find.lowest > 0 }).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
            )
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
    ForgeChip(label, on, onClick, mode = ChipMode.Choice)
}

@Composable private fun TemplateIcon(template: ItemTemplate) {
    val tint = rarityColor(template.rarity.name)
    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
        if (!SpriteIcon(equipmentIcon(template.code), tint, Modifier.size(32.dp), halo = false)) Icon(ForgeGlyphs.Gem, null, tint = tint, modifier = Modifier.size(24.dp))
    }
}
