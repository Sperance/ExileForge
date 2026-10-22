package com.sperance.exileforge.ui.screens.city

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.Bestiary
import com.sperance.exileforge.core.display.BestiaryTally
import com.sperance.exileforge.core.display.BestiaryZone
import com.sperance.exileforge.core.display.groupedNumber
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Бестиарий - экран над «Историей»: охотничий атлас по зонам. Вкладка «Бестиарий» - поиск по имени открытого вида или зоны, глава
 * чипом («Все», главы кампании, «Иное»), ниже зоны в порядке кампании: в строке зоны полоса силуэтов её монстров и медальон босса,
 * счёт «открыто / всего»; касание раскрывает зону строками видов (неоткрытые - силуэтом и «???»), у открытых - число убийств и
 * ступень 10/100/1000 (бронза, серебро, золото). Вкладка «Кто вас убивал» - рейтинг убийц героя с полосами. Касание открытого вида -
 * лист: убийства, гибели, лестница ступеней, где встречается. Данные - статистика героя (`hero/stats`) и контент кампании; наград нет.
 */
@Composable fun BestiaryScreen() {
    val vm = koinViewModel<HistoryViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val index = game.index
    val stats by produceState<Map<String, Long>?>(null, game.heroId) { value = game.heroId.takeIf { it.isNotBlank() }?.let { vm.stats(it) } }
    val bestiary = remember(index) { index?.let(::Bestiary) }
    BackTrailHost(ui("nav.city")) {
        TrailLevel(ui("history.title"), onBack = vm::city) {
            TrailLevel(ui("history.bestiary"), onBack = vm::back) {
                val known = stats
                if (bestiary == null || known == null || index == null) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Gold) }
                    return@TrailLevel
                }
                val tally = remember(bestiary, known) { BestiaryTally(bestiary, known) }
                val look = remember(index, game.world.portraits) { Look(index, game.world.portraits) }
                BestiaryBody(look, bestiary, tally)
            }
        }
    }
}

@Composable private fun BestiaryBody(look: Look, bestiary: Bestiary, tally: BestiaryTally) {
    TrailData(ui("history.count", tally.opened, bestiary.all.size))
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var opened by remember { mutableStateOf<MonsterCode?>(null) }
    val deaths = tally.deaths
    Column(Modifier.fillMaxSize()) {
        StoneTabs(
            listOf(
                StoneTab(ui("history.bestiary"), ForgeGlyphs.Skull, Brass),
                StoneTab(if (deaths > 0) ui("bestiary.killers_count", deaths) else ui("bestiary.killers"), ForgeGlyphs.Swords, LifeRed),
            ),
            selected = tab,
            onSelect = { tab = it },
            scrollable = false,
        )
        if (tab == 0) Atlas(look, bestiary, tally) { opened = it } else Killers(look, bestiary, tally) { opened = it }
    }
    opened?.let { SpeciesSheet(look, bestiary, tally, it) { opened = null } }
}

/** Контент и портреты экрана: для силуэтов видов. */
private class Look(val index: ContentIndex, val portraits: Int)

/** Выбор главы чипом - число, чтобы пережить пересоздание экрана: [ALL], [OTHER] или номер главы с нуля. */
private object Shelf {
    const val ALL = -1
    const val OTHER = -2
}

/**
 * Атлас зон: поиск, чипы глав и зоны; при «Все» - зоны всех глав под именами глав. Поиск ищет открытые виды по имени и зоны по имени:
 * зона подходит своим именем или открытым видом; неоткрытые по имени не ищутся.
 */
@Composable private fun Atlas(look: Look, bestiary: Bestiary, tally: BestiaryTally, onOpen: (MonsterCode) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var shelf by rememberSaveable { mutableIntStateOf(Shelf.ALL) }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val needle = query.trim()
    val chapters = remember(bestiary, tally, shelf, needle) {
        bestiary.chapters.mapIndexedNotNull { i, chapter ->
            if (shelf != Shelf.ALL && shelf != i) return@mapIndexedNotNull null
            val zones = chapter.zones.filter { zone -> needle.isEmpty() || zone.matches(needle, tally) }
            (i to chapter.region to zones).takeIf { zones.isNotEmpty() }
        }
    }
    val other = remember(bestiary, tally, shelf, needle) {
        bestiary.other.filter { (shelf == Shelf.ALL || shelf == Shelf.OTHER) && (needle.isEmpty() || (tally.open(it) && monsterTitle(it).contains(needle, true))) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "search") { SearchBar(query, { query = it }, ui("bestiary.search")) }
        item(key = "chips") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ForgeChip(ui("common.all"), shelf == Shelf.ALL, { shelf = Shelf.ALL }, mode = ChipMode.Choice)
                bestiary.chapters.indices.forEach { i -> ForgeChip(roman(i + 1), shelf == i, { shelf = i }, mode = ChipMode.Choice) }
                if (bestiary.other.isNotEmpty()) ForgeChip(ui("bestiary.other"), shelf == Shelf.OTHER, { shelf = Shelf.OTHER }, mode = ChipMode.Choice)
            }
        }
        if (chapters.isEmpty() && other.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoCard(ui("bestiary.empty_search"), ui("bestiary.empty_search_hint"))
                    if (needle.isNotEmpty()) ForgeButton(onClick = { query = "" }, kind = ButtonKind.Secondary) { Text(ui("bestiary.clear")) }
                }
            }
        }
        chapters.forEach { (head, zones) ->
            val (at, region) = head
            if (shelf == Shelf.ALL) item(key = "chapter-$region") { ChapterHead(ui("bestiary.chapter", roman(at + 1), regionTitle(region))) }
            items(zones, key = { it.code.value }) { zone ->
                ZonePlate(look, bestiary, zone, tally, open = expanded == zone.code.value, onToggle = { expanded = if (expanded == zone.code.value) null else zone.code.value }, onOpen = onOpen)
            }
        }
        if (other.isNotEmpty()) {
            item(key = "other-head") { ChapterHead(ui("bestiary.other")) }
            items(other, key = { "other-${it.value}" }) { code -> SpeciesRow(look, tally, code, bestiary.boss(code), note = null) { onOpen(code) } }
        }
    }
}

/** Зона подходит поиску [needle]: своим именем или именем открытого вида. */
private fun BestiaryZone.matches(needle: String, tally: BestiaryTally): Boolean =
    mapTitle(code).contains(needle, true) || species.any { tally.open(it) && monsterTitle(it).contains(needle, true) }

@Composable private fun ChapterHead(text: String) {
    Text(text, color = Brass, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
}

/**
 * Плита зоны: имя, уровень, «открыто / всего» (всё открыто - золотом) и шеврон; свёрнутая - полоса силуэтов монстров и медальон босса
 * за чертой, раскрытая - строки видов.
 */
@Composable private fun ZonePlate(look: Look, bestiary: Bestiary, zone: BestiaryZone, tally: BestiaryTally, open: Boolean, onToggle: () -> Unit, onOpen: (MonsterCode) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val opened = tally.opened(zone.species)
    Column(Modifier.fillMaxWidth().clip(shape).depthPanel(shape).border(1.dp, if (open) Brass else Bronze, shape)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(mapTitle(zone.code), color = Parchment, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            MutedText(ui("bestiary.level", zone.level), style = MaterialTheme.typography.labelSmall)
            Text(ui("history.count", opened, zone.species.size), color = if (opened == zone.species.size) Gold else Muted, style = MaterialTheme.typography.labelMedium)
            Icon(if (open) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight, null, tint = Muted)
        }
        if (!open) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 12.dp, end = 12.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    zone.monsters.forEach { Silhouette(look, it, tally.open(it), boss = false, size = STRIP) }
                }
                Box(Modifier.size(1.dp, STRIP).background(Bronze))
                Silhouette(look, zone.boss, tally.open(zone.boss), boss = true, size = STRIP)
            }
        } else {
            Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                zone.species.forEach { code ->
                    val zones = bestiary.zones(code).size
                    val note = when {
                        code == zone.boss -> ui("bestiary.zone_boss")
                        zones > 1 -> ui("bestiary.more_zones", zones - 1)
                        else -> ui("bestiary.only_here")
                    }
                    SpeciesRow(look, tally, code, code == zone.boss || bestiary.boss(code), note) { onOpen(code) }
                }
            }
        }
    }
}

/**
 * Строка вида: силуэт, имя (неоткрытый - «???») и под ним у открытого заметка [note] («босс зоны», «только здесь», «ещё в N зонах»);
 * у открытого справа - убийства и ступень. Касание открытого - лист вида.
 */
@Composable private fun SpeciesRow(look: Look, tally: BestiaryTally, code: MonsterCode, boss: Boolean, note: String?, onClick: () -> Unit) {
    val open = tally.open(code)
    val kills = tally.kills(code)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).then(if (open) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Silhouette(look, code, open, boss = boss, size = ROW)
        Column(Modifier.weight(1f)) {
            Text(if (open) monsterTitle(code) else ui("bestiary.unknown"), color = if (open) Parchment else Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (open && note != null) MutedText(note, style = MaterialTheme.typography.labelSmall)
        }
        if (open) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(groupedNumber(kills), color = GoldBright, style = MaterialTheme.typography.labelLarge)
                TierPips(Bestiary.tier(kills))
            }
        }
    }
}

/** Цвета ступеней: бронза, серебро, золото. */
private val TierHues = listOf(Color(0xFFB07A4A), Color(0xFFC9D3DA), Gold)

/** Три ромба ступени: взятые - цветом своей ступени. */
@Composable private fun TierPips(tier: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(Bestiary.STEPS.size) { i ->
            Canvas(Modifier.size(8.dp)) {
                val hue = if (i < tier) TierHues[tier - 1] else Bronze
                rotate(45f) { drawRect(hue, topLeft = center.copy(x = center.x - size.width * .32f, y = center.y - size.height * .32f), size = size * .64f) }
            }
        }
    }
}

/**
 * Силуэт вида: портрет сервера в рамке; неоткрытый - затемнён до тени со знаком «?»; босс - в рамке латуни. Пока портретов нет -
 * пустой жетон.
 */
@Composable private fun Silhouette(look: Look, code: MonsterCode, open: Boolean, boss: Boolean, size: Dp) {
    val form = look.index.monster(code)?.form.orEmpty()
    val shape = RoundedCornerShape(if (boss) size / 2 else 6.dp)
    Box(Modifier.size(size).clip(shape).border(1.dp, if (boss) Brass else Bronze, shape), contentAlignment = Alignment.Center) {
        MonsterPortrait(code.value, form, look.portraits, Modifier.fillMaxSize(), shadow = !open)
        if (!open) Text("?", color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

private val STRIP = 30.dp
private val ROW = 38.dp

/** «Кто вас убивал»: рейтинг убийц - место, силуэт, имя и зона, полоса гибелей от самого частого; первый - в красной рамке. */
@Composable private fun Killers(look: Look, bestiary: Bestiary, tally: BestiaryTally, onOpen: (MonsterCode) -> Unit) {
    val killers = tally.killers
    if (killers.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) { InfoCard(ui("bestiary.no_killers"), ui("bestiary.no_killers_hint")) }
        return
    }
    val most = killers.first().second.toFloat()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(killers.size, key = { killers[it].first.value }) { i ->
            val (code, deaths) = killers[i]
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).depthPanel(shape).border(1.dp, if (i == 0) LifeRed else Bronze, shape).clickable { onOpen(code) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("${i + 1}", color = if (i == 0) LifeRed else Muted, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(20.dp))
                Silhouette(look, code, true, bestiary.boss(code), ROW)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(monsterTitle(code), color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    bestiary.zones(code).firstOrNull()?.let { MutedText(mapTitle(it), style = MaterialTheme.typography.labelSmall) }
                    LinearProgressIndicator(
                        progress = { deaths / most },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = LifeRed,
                        trackColor = Bronze,
                    )
                }
                Text(ui("bestiary.deaths", deaths), color = LifeRed, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Лист вида: силуэт, имя, глава (или «Иное»), босс или монстр и ступень; факты - убито героем и его гибели; лестница ступеней
 * 10/100/1000 - взята, сколько ещё до следующей, дальше прочерк; где встречается - зоны чипами.
 */
@Composable private fun SpeciesSheet(look: Look, bestiary: Bestiary, tally: BestiaryTally, code: MonsterCode, onDismiss: () -> Unit) {
    val kills = tally.kills(code)
    val tier = Bestiary.tier(kills)
    val boss = bestiary.boss(code)
    val chapter = bestiary.chapter(code)?.let { found -> bestiary.chapters.indexOf(found) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Silhouette(look, code, true, boss, 52.dp)
                Column(Modifier.weight(1f)) {
                    Text(monsterTitle(code), color = GoldBright, style = relicName(18))
                    MutedText(
                        listOfNotNull(
                            chapter?.let { ui("bestiary.chapter_short", roman(it + 1)) } ?: ui("bestiary.other"),
                            ui(if (boss) "bestiary.kind_boss" else "bestiary.kind_monster"),
                            ui("bestiary.tier", roman(tier)).takeIf { tier > 0 },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Fact(groupedNumber(kills), ui(if (boss) "bestiary.wins" else "bestiary.kills"), GoldBright, Modifier.weight(1f))
                val deaths = tally.deaths(code)
                Fact(groupedNumber(deaths), ui("bestiary.your_deaths"), if (deaths > 0) LifeRed else Muted, Modifier.weight(1f))
            }
            Text(ui("bestiary.steps"), color = Brass, style = MaterialTheme.typography.labelMedium)
            Bestiary.STEPS.forEachIndexed { i, step ->
                val got = kills >= step
                val next = !got && (i == 0 || kills >= Bestiary.STEPS[i - 1])
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(roman(i + 1), color = if (got) TierHues[i] else Muted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(28.dp))
                    Text(ui("bestiary.step", groupedNumber(step)), color = if (got) Parchment else Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(
                        when {
                            got -> ui("bestiary.step_got")
                            next -> ui("bestiary.step_left", groupedNumber(step - kills))
                            else -> "—"
                        },
                        color = if (got) Vital else if (next) Caution else Muted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            val zones = bestiary.zones(code)
            if (zones.isNotEmpty()) {
                Text(ui("bestiary.where"), color = Brass, style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    zones.forEach { TagChip(mapTitle(it)) }
                }
            }
        }
    }
}

/** Факт листа: крупное число и подпись под ним. */
@Composable private fun Fact(value: String, label: String, tint: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Column(modifier.clip(shape).depthRaised(shape).padding(10.dp)) {
        Text(value, color = tint, style = relicName(20))
        MutedText(label, style = MaterialTheme.typography.labelSmall)
    }
}

/** Номер римской цифрой: главы и ступени Бестиария немногочисленны. */
private fun roman(n: Int): String = listOf(10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I").fold(n to "") { (rest, out), (value, sign) ->
    rest % value to out + sign.repeat(rest / value)
}.second
