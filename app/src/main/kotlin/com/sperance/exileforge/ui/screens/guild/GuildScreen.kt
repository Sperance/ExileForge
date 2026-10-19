package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildCard
import com.sperance.exileforge.core.model.guild.GuildInviteView
import com.sperance.exileforge.core.model.guild.GuildMine
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.rules.content.GuildMode
import com.sperance.exileforge.rules.content.GuildRules
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The guild, a building of the City. A hero outside one finds a guild, answers an invitation or
 * founds a guild of their own; a hero inside sees their guild under its arms, in tabs. Every rule is the server's —
 * the screen only says in advance what a button would be refused for, from `guilds.json`.
 */
@Composable fun GuildScreen() {
    val game by koinViewModel<GuildViewModel>().game.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    val vm = koinViewModel<GuildViewModel>()
    val guilds by vm.guilds.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId, game.sessionEpoch) {
        if (game.heroId.isNotBlank()) {
            heroModel.ensure()
            vm.open()
        }
    }
    val mine = guilds.mine
    val guild = mine?.guild
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(12.dp))
        when {
            guild != null -> {
                GuildInside(game, vm, guild, mine?.me)
            }

            mine != null -> {
                GuildOutside(game, vm, mine)
            }

            else -> {
                if (Reads.GUILD in game.loading) {
                    MutedText(ui("guild.loading"))
                } else {
                    ForgeOutlinedButton(enabled = !game.busy, onClick = vm::load, modifier = Modifier.fillMaxWidth()) { Text(ui("auction.check_again")) }
                }
            }
        }
    }
}

/**
 * Outside a guild: the wait after leaving, the invitations, the founding and the list of guilds to knock at.
 *
 * Список первым: пауза после выхода - одна жёлтая строка; приглашения - компактные строки в рамке `Rune`, только когда есть; поиск,
 * «Код» и «Своя» - одна строка инструментов (код и основание - шторками); ниже чипы фракций и гильдии двухстрочными строками, под
 * ними - цена заявки и страницы. Вступление по коду: шесть цифр от главы или офицера - сразу в гильдию, мимо её режима.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.GuildOutside(game: GameUi, vm: GuildViewModel, mine: GuildMine) {
    val guilds by vm.guilds.collectAsStateWithLifecycle()
    var founding by remember { mutableStateOf(false) }
    var coding by remember { mutableStateOf(false) }
    LaunchedEffect(game.heroId) { vm.search(0) }
    val waiting = mine.rejoinAt?.takeIf { it > System.currentTimeMillis() }
    val price = game.index?.guilds?.application?.price?.of(game.heroLevel) ?: 0L
    PullToRefreshBox(
        isRefreshing = Reads.GUILD in game.loading || Reads.GUILD_SEARCH in game.loading,
        onRefresh = {
            vm.load()
            vm.search(guilds.search.page)
        },
        modifier = Modifier.weight(1f),
    ) {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
            waiting?.let { item(key = "wait") { RejoinStrip(it) } }
            if (mine.invites.isNotEmpty()) item(key = "invites") { Invites(game, mine.invites, vm) }
            item(key = "tools") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactSearchField(guilds.query, { vm.query(it.take(game.inputs.search)) }, ui("guild.search"), Modifier.weight(1f), onSearch = { vm.search(0) })
                    ToolButton(Icons.Outlined.Tag, ui("guild.tool_code"), Rune, enabled = waiting == null) { coding = true }
                    ToolButton(Icons.Outlined.Add, ui("guild.tool_found"), Gold, enabled = !game.busy) { founding = true }
                }
            }
            game.index?.guilds?.takeIf { it.factions.isNotEmpty() }?.let { rules -> item(key = "factions") { FactionFilter(guilds.faction, rules, !game.busy, vm::filterFaction) } }
            val page = guilds.search
            if (page.items.isEmpty() && Reads.GUILD_SEARCH !in game.loading) item { InfoCard(ui("guild.none_found"), ui("guild.none_found_hint")) }
            items(page.items, key = { it.id }) { card -> GuildRow(game, card, JoinBlock.of(game, card, waiting), waiting, price) { vm.join(card) } }
            if (price > 0 && page.items.any { it.mode == GuildMode.APPLY }) item(key = "foot") { ApplyFoot(price) }
            if (page.totalPages > 1) item(key = "pager") { Pager(page.page, page.totalPages, vm::search) }
        }
    }
    if (founding) {
        FoundingSheet(game, onDismiss = { founding = false }) { name, tag, faction, emblem, color, mode, minLevel ->
            founding = false
            vm.create(name, tag, faction, emblem, color, mode, minLevel)
        }
    }
    if (coding) {
        CodeSheet(game.busy, onDismiss = { coding = false }) { code ->
            coding = false
            vm.joinByCode(code)
        }
    }
}

/** Пауза после выхода - одна жёлтая строка: с какого часа можно вступить и сколько осталось. */
@Composable private fun RejoinStrip(until: Long) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp).background(Caution.copy(alpha = .07f), shape).border(1.dp, Caution.copy(alpha = .3f), shape).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.HourglassEmpty, null, tint = Caution, modifier = Modifier.size(14.dp))
        Text(ui("guild.rejoin_strip", clockText(until)), color = STRIP_TEXT, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        Text(untilText(until), color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

/** Приглашения: рамка `Rune` с чертой слева, «Приглашения · N» и строки по 48dp - герб, имя, уровень и кто позвал, ✕ и «Принять». */
@Composable private fun Invites(game: GameUi, invites: List<GuildInviteView>, vm: GuildViewModel) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(INVITE_TOP, INVITE_BOTTOM)), shape).border(1.dp, Rune.copy(alpha = .35f), shape)
            .drawBehind { drawRect(Rune, size = Size(3.dp.toPx(), size.height)) }.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.Mail, null, tint = Rune, modifier = Modifier.size(14.dp))
            Engraved(ui("guild.invites", invites.size), Rune)
        }
        invites.forEach { InviteRow(game, it, vm) }
    }
}

/** Приглашение строкой: герб, имя с тегом, знак фракции, уровень и кто позвал; ✕ отклоняет, «Принять» - вступление. */
@Composable private fun InviteRow(game: GameUi, invite: GuildInviteView, vm: GuildViewModel) {
    val card = invite.guild
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GuildEmblem(card.emblem, card.color, 32.dp)
        Column(Modifier.weight(1f)) {
            NameLine(card)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FactionIcon(card.faction, game.index?.guilds, Modifier.size(12.dp))
                Text(
                    listOfNotNull(ui("guild.card_line", card.level), invite.by.takeIf { it.isNotBlank() }?.let { ui("guild.invite_from", it) }).joinToString(" · "),
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
        IconButton(onClick = { vm.declineInvite(card.id) }, enabled = !game.busy, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Close, ui("guild.decline"), tint = DECLINE_TINT, modifier = Modifier.size(18.dp))
        }
        ForgeButton(enabled = !game.busy, onClick = { vm.acceptInvite(card.id) }) { Text(ui("guild.accept")) }
    }
}

/** The list's faction chips: all, then each faction under its sign and name in its colour. */
@Composable private fun FactionFilter(chosen: String, rules: GuildRules, enabled: Boolean, onChoose: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item { FilterChip(selected = chosen.isBlank(), enabled = enabled, onClick = { onChoose("") }, label = { Text(ui("common.all")) }) }
        items(rules.factions, key = { it.code }) { faction ->
            FilterChip(
                selected = chosen == faction.code,
                enabled = enabled,
                onClick = { onChoose(faction.code) },
                leadingIcon = { FactionIcon(faction.code, rules, Modifier.size(14.dp)) },
                label = { Text(GuildText.faction(faction.code), color = factionColor(faction)) },
            )
        }
    }
}

/** Почему этот герой не войдёт в гильдию сейчас - коротко, со знаком [icon] и строкой словаря [key]; новая причина - запись здесь. */
private enum class JoinBlock(val icon: ImageVector, val key: String) {
    REJOIN(Icons.Outlined.HourglassEmpty, "guild.block_rejoin"),
    FULL(Icons.Outlined.Group, "guild.block_full"),
    LEVEL(Icons.Outlined.Lock, "guild.block_level"),
    INVITE(Icons.Outlined.Mail, "guild.block_invite"),
    ;

    /** Слова причины для гильдии [card]; [until] - конец паузы после выхода. */
    fun text(card: GuildCard, until: Long?): String = when (this) {
        REJOIN -> ui(key, clockText(until ?: 0))
        LEVEL -> ui(key, card.minLevel)
        FULL, INVITE -> ui(key)
    }

    companion object {
        /** Причина отказа героя в гильдию [card]; [until] - пауза после выхода, null - её нет. */
        fun of(game: GameUi, card: GuildCard, until: Long?): JoinBlock? = when {
            until != null -> REJOIN
            card.full -> FULL
            game.heroLevel < card.minLevel -> LEVEL
            card.mode == GuildMode.INVITE -> INVITE
            else -> null
        }
    }
}

/**
 * Гильдия строкой (~62dp): черта цвета фракции слева, герб с уровнем гильдии плашкой в углу; имя [ТЕГ] и «с N ур.»; фракция и состав.
 * Справа - путь внутрь по режиму: «Вступить», «Заявка» с ценой под ней или причина отказа ([block]). Цена заявки ([price]) спишется,
 * когда заявку примут: подать её можно и без золота; [until] - конец паузы после выхода.
 */
@Composable private fun GuildRow(game: GameUi, card: GuildCard, block: JoinBlock?, until: Long?, price: Long, onJoin: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val faction = factionColor(game.index?.guilds?.faction(card.faction))
    Row(
        Modifier.fillMaxWidth().heightIn(min = 62.dp).clip(shape).depthPanel(shape, elevation = 0.dp).border(1.dp, Bronze, shape)
            .drawBehind { drawRect(faction.copy(alpha = .85f), size = Size(3.dp.toPx(), size.height)) }.padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box {
            GuildEmblem(card.emblem, card.color, 38.dp)
            Text(
                card.level.toString(),
                color = Caution,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 5.dp, y = 3.dp).background(Ink, PLATE).border(1.dp, Caution.copy(alpha = .55f), PLATE).padding(horizontal = 3.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { NameLine(card) }
                if (card.mode != GuildMode.INVITE) Text(ui("guild.min_level", card.minLevel), color = Muted, fontSize = 10.5.sp, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    FactionIcon(card.faction, game.index?.guilds, Modifier.size(12.dp))
                    Text(GuildText.faction(card.faction), color = faction, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Outlined.Group, null, tint = Muted, modifier = Modifier.size(12.dp))
                    Text(ui("guild.roll", card.members, card.capacity), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
        Box(Modifier.widthIn(min = 76.dp), contentAlignment = Alignment.CenterEnd) {
            when {
                block != null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(block.icon, null, tint = Muted, modifier = Modifier.size(12.dp))
                    Text(block.text(card, until), color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.widthIn(max = 84.dp))
                }

                card.mode == GuildMode.OPEN -> ForgeButton(enabled = !game.busy, onClick = onJoin) { Text(ui("guild.join")) }

                else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    ForgeOutlinedButton(enabled = !game.busy, onClick = onJoin) { Text(ui("guild.apply"), color = Gold) }
                    if (price > 0) GoldPrice(price)
                }
            }
        }
    }
}

/** Имя гильдии и тег мелко за ним, в одну строку. */
@Composable private fun NameLine(card: GuildCard) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(card.name, color = GoldBright, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (card.tag.isNotBlank()) Text(ui("guild.tag", card.tag), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** Под списком: цена заявки и когда она спишется. */
@Composable private fun ApplyFoot(price: Long) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Outlined.Info, null, tint = Muted, modifier = Modifier.size(14.dp))
        Text(ui("guild.apply_note", number(price.toDouble())), color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

/** Страницы списка: ‹ «1 из 4» ›. */
@Composable private fun Pager(page: Int, pages: Int, onPage: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)) {
        PageButton(Icons.Outlined.ChevronLeft, ui("guild.back_page"), page > 0) { onPage(page - 1) }
        Text(ui("guild.page", page + 1, pages), color = Parchment, style = MaterialTheme.typography.bodyMedium)
        PageButton(Icons.Outlined.ChevronRight, ui("auction.forward"), page + 1 < pages) { onPage(page + 1) }
    }
}

@Composable private fun PageButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(9.dp)
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp).alpha(if (enabled) 1f else .35f).background(Abyss.copy(alpha = .4f), shape).border(1.dp, Bronze, shape)) {
        Icon(icon, label, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

/** Шторка «Вступить по коду»: шесть ячеек цифр и «Вступить», пока код не полон - погашена. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeSheet(busy: Boolean, onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(),
        footer = { ForgeButton(enabled = !busy && code.length == CODE_LENGTH, onClick = { onJoin(code) }, modifier = Modifier.weight(1f)) { Text(ui("guild.join")) } },
    ) {
        Text(ui("guild.join_code_title"), style = relicName(16).copy(color = GoldBright))
        MutedText(ui("guild.join_code_note"))
        CodeCells(code) { code = it.filter(Char::isDigit).take(CODE_LENGTH) }
    }
}

/** Ввод кода ячейками: по цифре в ячейке, следующая пустая - в рамке `Rune`; клавиатура цифр. */
@Composable private fun CodeCells(code: String, onCode: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        code,
        onCode,
        Modifier.focusRequester(focus),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(CODE_LENGTH) { i ->
                    val shape = RoundedCornerShape(8.dp)
                    val edge = when {
                        i == code.length -> Rune
                        i < code.length -> CELL_FILLED
                        else -> Bronze
                    }
                    Box(Modifier.size(38.dp, 44.dp).background(Abyss, shape).border(1.dp, edge, shape), contentAlignment = Alignment.Center) {
                        Text(code.getOrNull(i)?.toString().orEmpty(), color = GoldBright, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
    )
}

/** Цифр в коде вступления - как в номере героя сервера. */
private const val CODE_LENGTH = 6

/** Плашка уровня гильдии на гербе. */
private val PLATE = RoundedCornerShape(5.dp)

/** Песочный текст строки паузы. */
private val STRIP_TEXT = Color(0xFFE8D59A)

/** Стекло рамки приглашений: сверху светлее. */
private val INVITE_TOP = Color(0xFF18242E)
private val INVITE_BOTTOM = Color(0xFF111A21)

/** Приглушённый красный «Отклонить». */
private val DECLINE_TINT = Color(0xFFB06A66)

/** Кромка заполненной ячейки кода. */
private val CELL_FILLED = Color(0xFF35505E)
