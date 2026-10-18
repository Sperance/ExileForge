package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.admin.ModerationFilters
import com.sperance.exileforge.core.admin.ModerationTab
import com.sperance.exileforge.core.app.AccountUi
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.core.network.AccountRole
import com.sperance.exileforge.core.network.DeletionRequest
import com.sperance.exileforge.core.network.DeviceView
import com.sperance.exileforge.core.network.Dossier
import com.sperance.exileforge.core.network.ModerationEntryView
import com.sperance.exileforge.core.network.ModerationFate
import com.sperance.exileforge.core.network.ModerationRow
import com.sperance.exileforge.core.network.ModerationSort
import com.sperance.exileforge.core.network.SanctionCategory
import com.sperance.exileforge.core.network.SanctionKind
import com.sperance.exileforge.core.network.SanctionNotice
import com.sperance.exileforge.core.network.SanctionRequest
import com.sperance.exileforge.core.network.SanctionTarget
import com.sperance.exileforge.core.network.SanctionView
import com.sperance.exileforge.core.network.TesterAccount
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.auction.listedAt
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Окно модерации («Досье-карточки»): поиск, разделы «Все / Баны / Корзина /
 * Журнал», карточки героев с кромкой статуса; тап открывает досье. Бан и удаление - нижними шторками, удаление - удержанием.
 * Что смотрящему нельзя (админ, модератор для модератора), сервер не пропустит, а карточка показывает замком.
 *
 * Порядок и отбор по дару - за кнопкой с числом активных, у корзины их нет. Над списком - что отобрано и сколько найдено.
 */
@Composable internal fun ModerationPage(account: AccountUi) {
    val vm = koinViewModel<ModerationViewModel>().model
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load(0) }
    if (state.dossierKey != null) {
        TrailLevel(ui("moderation.dossier"), onBack = vm::close) { DossierView(account, vm, state.dossier) }
        return
    }
    var creating by remember { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            state.query,
            vm::search,
            placeholder = { Text(ui("moderation.search")) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Muted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.load(0) }),
            modifier = Modifier.weight(1f),
        )
        if (state.tab != ModerationTab.TRASH && state.tab != ModerationTab.JOURNAL) {
            FilterButton(state.filters.active, enabled = !account.busy) { filtering = true }
        }
        if (account.isAdmin) {
            ForgeButton(onClick = { creating = true }, enabled = !account.busy, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Outlined.PersonAdd, ui("moderation.new_tester"), modifier = Modifier.size(20.dp))
            }
        }
    }
    if (creating || state.createdTester != null) {
        AccountSheet(account, state.createdTester, onCreate = vm::createAccount) {
            creating = false
            vm.forgetTester()
        }
    }
    PillTabs(
        ModerationTab.entries.map { ui("moderation.tab.${it.name}") },
        state.tab.ordinal,
        { vm.tab(ModerationTab.entries[it]) },
        enabled = !account.busy,
        segmented = true,
    )
    if (state.tab == ModerationTab.JOURNAL) {
        if (state.journal.isEmpty()) MutedText(ui("moderation.journal_empty"))
        state.journal.forEach { JournalRow(it) }
        Pager(state.journalPage, if (state.journal.size >= PAGE_GUESS) state.journalPage + 2 else state.journalPage + 1, account.busy, vm::journal)
        return
    }
    val page = state.page
    if (filtering) {
        FiltersSheet(account, state.filters, onDismiss = { filtering = false }) {
            filtering = false
            vm.filter(it)
        }
    }
    val chosen = if (state.tab == ModerationTab.TRASH) emptyList() else filterLabels(account, state.filters)
    MutedText((chosen + ui("moderation.total", page.total)).joinToString(" · "), style = MaterialTheme.typography.labelSmall)
    if (page.rows.isEmpty()) MutedText(ui("moderation.none"))
    page.rows.forEach { row ->
        if (state.tab == ModerationTab.TRASH) {
            TrashCard(account, row) { row.sanction?.let(vm::lift) }
        } else {
            PlayerCard(account, row) { vm.open(row.heroId, row.userId) }
        }
    }
    val pages = if (page.size > 0) ((page.total + page.size - 1) / page.size).toInt() else 1
    Pager(page.page, pages, account.busy, vm::load)
}

/** Кнопка фильтров у поиска: значок отбора, на нём - число активных фильтров [active] (без него - отбор обычный). */
@Composable private fun FilterButton(active: Int, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier.size(48.dp).border(1.dp, if (active > 0) Gold.copy(alpha = .6f) else Bronze, shape).clip(shape)
            .clickable(enabled = enabled, onClickLabel = ui("moderation.filters"), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Tune, ui("moderation.filters"), tint = if (active > 0) GoldBright else Muted, modifier = Modifier.size(22.dp))
        if (active > 0) {
            Text(
                active.toString(),
                color = Ink,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).widthIn(min = 16.dp).background(Gold, RoundedCornerShape(8.dp)).padding(horizontal = 4.dp),
            )
        }
    }
}

/** Подписи активных фильтров над списком: порядок, если не обычный, и дар, если выбран. */
private fun filterLabels(account: AccountUi, filters: ModerationFilters): List<String> = buildList {
    if (filters.sort != ModerationFilters.DEFAULT.sort) add(ui("moderation.sort.${filters.sort.name}"))
    when (val fate = filters.fate) {
        ModerationFate.All -> Unit
        ModerationFate.Fateless -> add(ui("moderation.fate.none"))
        is ModerationFate.Chosen -> add("✦ " + (account.index?.fates?.all?.firstOrNull { it.code == fate.code }?.let(FateCard::of)?.title ?: fate.code))
    }
}

/**
 * Лист «Фильтры»: «Сбросить» в заголовке, порядок сегментами, дары - поиск и
 * сетка в два столбца («Все дары», «Без дара», дары контента по имени). Выбор - черновик листа: в список он уходит по
 * «Показать» ([onApply]); «Сбросить» (и в заголовке, и внизу) сразу применяет обычный отбор и закрывает лист.
 */
@Composable private fun FiltersSheet(account: AccountUi, current: ModerationFilters, onDismiss: () -> Unit, onApply: (ModerationFilters) -> Unit) {
    var draft by remember(current) { mutableStateOf(current) }
    var query by remember { mutableStateOf("") }
    val resettable = draft != ModerationFilters.DEFAULT || current != ModerationFilters.DEFAULT
    val fates = remember(account.index) { account.index?.fates?.all.orEmpty().map(FateCard::of).sortedBy { it.title } }
    val options = remember(fates, query) {
        val found = fates.filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }.map { it.title to ModerationFate.Chosen(it.code) }
        val fixed = listOf(ui("moderation.fate.all") to ModerationFate.All, ui("moderation.fate.none") to ModerationFate.Fateless)
        (if (query.isBlank()) fixed else emptyList()) + found
    }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ui("moderation.filters"), color = GoldBright, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                ForgeTextButton(onClick = { onApply(ModerationFilters.DEFAULT) }, enabled = resettable) {
                    Text(ui("moderation.filters.reset"), color = Gold)
                }
            }
            SheetLabel(ui("moderation.filters.sort"))
            PillTabs(
                ModerationSort.entries.map { ui("moderation.sort_short.${it.name}") },
                draft.sort.ordinal,
                { draft = draft.copy(sort = ModerationSort.entries[it]) },
                segmented = true,
            )
            SheetLabel(ui("moderation.fate.title"))
            OutlinedTextField(
                query,
                { query = it },
                placeholder = { Text(ui("moderation.filters.fate_search")) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Muted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.fillMaxWidth().heightIn(max = FATE_GRID_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(options, key = { it.second.param.ifEmpty { "*" } }) { (title, fate) ->
                    FateOption(title, mark = fate is ModerationFate.Chosen, selected = draft.fate == fate) { draft = draft.copy(fate = fate) }
                }
            }
            if (options.isEmpty()) MutedText(ui("moderation.none"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForgeOutlinedButton(onClick = { onApply(ModerationFilters.DEFAULT) }, enabled = resettable, modifier = Modifier.weight(1f)) { Text(ui("moderation.filters.reset")) }
                ForgeButton(onClick = { onApply(draft) }, modifier = Modifier.weight(2f)) { Text(ui("common.show")) }
            }
        }
    }
}

/** Подпись раздела листа фильтров: мелко, заглавными, приглушённо. */
@Composable private fun SheetLabel(text: String) = Text(text.uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)

/** Плитка дара в сетке листа фильтров и смены дара: выбранная - в нити и на подложке `Gold`, дар контента - со знаком ✦. */
@Composable internal fun FateOption(title: String, mark: Boolean, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        if (mark) "✦ $title" else title,
        color = when {
            selected -> Vital
            mark -> Parchment
            else -> Muted
        },
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().height(32.dp).clip(shape).background(if (selected) Gold.copy(alpha = .14f) else Panel)
            .border(1.dp, if (selected) Gold.copy(alpha = .6f) else Bronze, shape).clickable(onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically).padding(horizontal = 10.dp),
    )
}

/** Наибольшая высота сетки даров в листах фильтров и смены дара: дальше - прокрутка внутри сетки. */
internal val FATE_GRID_HEIGHT = 228.dp

@Composable private fun Pager(page: Int, pages: Int, busy: Boolean, onPage: (Int) -> Unit) {
    if (pages <= 1 && page == 0) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ForgeTextButton(onClick = { onPage(page - 1) }, enabled = !busy && page > 0) { Text("‹") }
        Text(ui("moderation.page", page + 1, pages), color = Muted, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        ForgeTextButton(onClick = { onPage(page + 1) }, enabled = !busy && page + 1 < pages) { Text("›") }
    }
}

/** Карточка игрока: портрет класса, имя и роль, класс, уровень и логин; под баном - красная кромка и строка бана. */
@Composable private fun PlayerCard(account: AccountUi, row: ModerationRow, onOpen: () -> Unit) {
    val banned = row.sanction?.takeIf { it.active }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().depthPanel(shape).border(1.dp, if (banned != null) LifeRed.copy(alpha = .45f) else Bronze, shape)
            .clickable(enabled = !account.busy, onClick = onOpen).padding(12.dp)
            .alpha(if (row.protected) .6f else 1f),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (banned != null) Box(Modifier.width(3.dp).height(44.dp).background(LifeRed, RoundedCornerShape(2.dp)))
        ClassPortrait(row.heroClass.takeIf { it.isNotBlank() }, account.world.portraits, Modifier.size(44.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(row.heroName, color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                RoleMark(row.role)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MutedText(listOf(classTitle(row.heroClass), ui("common.level_short", row.level), row.login).filter { it.isNotBlank() }.joinToString(" · "), Modifier.weight(1f, fill = false))
                row.fate?.let { FateMark(FateCard(it)) }
            }
            MutedText(seenText(row.lastSeenAt), style = MaterialTheme.typography.labelSmall)
            banned?.let { Text(sanctionLine(it), color = LifeRed, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        if (row.protected) Icon(Icons.Outlined.Lock, ui("moderation.protected"), tint = Muted, modifier = Modifier.size(18.dp))
    }
}

/** Удалённое в корзине: серая карточка, кто и за что удалил, полоса до очистки и «Восстановить» администратору. */
@Composable private fun TrashCard(account: AccountUi, row: ModerationRow, onRestore: () -> Unit) {
    val sanction = row.sanction ?: return
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().depthPanel(shape).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClassPortrait(row.heroClass.takeIf { it.isNotBlank() }, account.world.portraits, Modifier.size(44.dp).alpha(.45f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(row.heroName.ifBlank { sanction.label }, color = Parchment, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            MutedText(listOf(ui("moderation.target.${sanction.target.name}"), sanction.byLogin, categoryTitle(sanction.category)).joinToString(" · "))
            val left = sanction.daysLeft()
            LinearProgressIndicator(progress = { trashShare(sanction) }, color = Muted, trackColor = PanelRaised, modifier = Modifier.fillMaxWidth().height(3.dp))
            Text(ui("moderation.purge_in", left ?: 0), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        if (!row.protected) {
            IconButton(enabled = !account.busy, onClick = onRestore) { Icon(Icons.Outlined.Restore, ui("moderation.restore"), tint = Gold) }
        }
    }
}

/** Смена дара - без санкции: ни номера, ни категории; дары - именами, ниже - причина для игрока, если была. */
@Composable private fun JournalRow(entry: ModerationEntryView) {
    ForgePanel {
        Text(
            ui("moderation.action.${entry.action.name}", entry.actorLogin.ifBlank { ui("moderation.server") }, ui("moderation.target.${entry.target.name}"), entry.label),
            color = Parchment,
            style = MaterialTheme.typography.bodyMedium,
        )
        val fate = entry.fateChange
        MutedText(listOfNotNull(entry.number.takeIf { it > 0 }?.let { "#$it" }, categoryTitle(entry.category).takeIf { fate == null }, entry.until?.let { ui("moderation.until", listedAt(it).orEmpty()) }, listedAt(entry.at)).joinToString(" · "))
        if (fate != null) {
            MutedText("${fate.first?.let { FateCard(it).title } ?: "—"} → ${FateCard(fate.second).title}")
            entry.reason.takeIf { it.isNotBlank() }?.let { MutedText("«$it»") }
        } else if (entry.comment.isNotBlank()) {
            MutedText("«${entry.comment}»")
        }
    }
}

/**
 * Шторка санкции на срок - бана или немоты [kind]: цель (герой, аккаунт; у бана - и устройство), срок - готовый из
 * контента или свой, категория, причина для игрока (её видит наказанный) и служебная заметка модерации.
 */
@Composable internal fun BanSheet(account: AccountUi, dossier: Dossier, device: DeviceView?, kind: SanctionKind, onDismiss: () -> Unit, onBan: (SanctionRequest) -> Unit) {
    val presets = account.index?.rules?.moderation?.let { if (kind == SanctionKind.MUTE) it.muteHours else it.banHours }.orEmpty()
    var target by remember {
        mutableStateOf(
            if (device != null) {
                SanctionTarget.DEVICE
            } else if (dossier.hero != null) {
                SanctionTarget.HERO
            } else {
                SanctionTarget.ACCOUNT
            },
        )
    }
    var hours by remember { mutableStateOf<Int?>(presets.getOrNull(PRESET_WEEK) ?: presets.first()) }
    var custom by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(SanctionCategory.OTHER) }
    var comment by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val name = dossier.hero?.name ?: dossier.account.login
            Text(ui(if (kind == SanctionKind.MUTE) "moderation.mute_title" else "moderation.ban_title", name), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            if (kind == SanctionKind.MUTE) MutedText(ui("moderation.mute_note"))
            Label(ui("moderation.target"))
            Chips {
                if (dossier.hero != null) Chip(ui("moderation.target.HERO"), target == SanctionTarget.HERO) { target = SanctionTarget.HERO }
                Chip(ui("moderation.target.ACCOUNT"), target == SanctionTarget.ACCOUNT) { target = SanctionTarget.ACCOUNT }
                device?.let { Chip(it.model.ifBlank { it.hardware.take(HARDWARE) }, target == SanctionTarget.DEVICE) { target = SanctionTarget.DEVICE } }
            }
            Label(ui("moderation.term"))
            Chips {
                presets.forEach { preset ->
                    Chip(hoursText(preset), hours == preset && custom.isBlank()) {
                        hours = preset
                        custom = ""
                    }
                }
                Chip(ui("moderation.forever"), hours == null && custom.isBlank()) {
                    hours = null
                    custom = ""
                }
            }
            OutlinedTextField(
                custom,
                { custom = it.filter(Char::isDigit).take(DIGITS) },
                label = { Text(ui("moderation.custom_hours")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Label(ui("moderation.category"))
            Chips { SanctionCategory.imposed.forEach { Chip(categoryTitle(it), category == it) { category = it } } }
            ModerationText(account, reason, ui("moderation.reason_for_player")) { reason = it }
            ModerationText(account, comment, ui("moderation.comment")) { comment = it }
            val term = custom.toIntOrNull() ?: hours
            ForgeButton(
                enabled = !account.busy && (custom.isBlank() || (custom.toIntOrNull() ?: 0) > 0),
                onClick = { onBan(SanctionRequest(target, kind, dossier.hero?.heroId.orEmpty(), dossier.account.userId, device?.hardware.orEmpty(), term, category, comment, reason)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(ui(if (kind == SanctionKind.MUTE) "moderation.mute_for" else "moderation.ban_for", ui("moderation.target.$target"), term?.let(::hoursText) ?: ui("moderation.forever"))) }
        }
    }
}

/**
 * Шторка удаления (только администратор): герой или аккаунт целиком, служебная заметка обязательна, причина для игрока - нет;
 * удаление - удержанием кнопки.
 */
@Composable internal fun DeleteSheet(account: AccountUi, dossier: Dossier, target: SanctionTarget, onTarget: (SanctionTarget) -> Unit, onDismiss: () -> Unit, onDelete: (DeletionRequest) -> Unit) {
    var category by remember { mutableStateOf(SanctionCategory.OTHER) }
    var comment by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val days = account.index?.rules?.moderation?.trashDays ?: TRASH_DAYS
    val hero = dossier.hero?.takeIf { !it.deleted }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("moderation.delete_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            Chips {
                hero?.let { Chip(it.name, target == SanctionTarget.HERO) { onTarget(SanctionTarget.HERO) } }
                Chip(ui("moderation.delete_account", dossier.account.login, dossier.account.heroes), target == SanctionTarget.ACCOUNT) { onTarget(SanctionTarget.ACCOUNT) }
            }
            MutedText(ui(if (target == SanctionTarget.HERO) "moderation.delete_hero_note" else "moderation.delete_account_note", days))
            Chips { SanctionCategory.imposed.forEach { Chip(categoryTitle(it), category == it) { category = it } } }
            ModerationText(account, reason, ui("moderation.reason_for_player")) { reason = it }
            ModerationText(account, comment, ui("moderation.delete_reason")) { comment = it }
            HoldButton(
                ui("moderation.hold_delete"),
                LifeRed,
                modifier = Modifier.fillMaxWidth(),
                enabled = !account.busy && comment.isNotBlank(),
            ) { onDelete(DeletionRequest(target, if (target == SanctionTarget.HERO) hero?.heroId.orEmpty() else dossier.account.userId, category, comment, reason)) }
        }
    }
}

/**
 * Поле текста модерации - причины для игрока или служебной заметки: несколько строк, предел `inputs.sanctionComment` правил
 * со счётчиком; лишнее не вводится.
 */
@Composable internal fun ModerationText(account: AccountUi, value: String, label: String, onValueChange: (String) -> Unit) {
    val limit = account.index?.rules?.inputs?.sanctionComment ?: COMMENT
    OutlinedTextField(value, { onValueChange(it.take(limit)) }, label = { Text(label) }, minLines = 2, supportingText = { LengthCounter(value, limit) }, modifier = Modifier.fillMaxWidth())
}

@Composable internal fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    ForgePanel(accent = Rune) {
        Text(title, color = Rune, style = MaterialTheme.typography.labelLarge)
        content()
    }
}

@Composable internal fun Line(label: String, value: String, flagged: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Parchment, style = MaterialTheme.typography.bodySmall)
        Text(if (flagged) "$value ⚠" else value, color = if (flagged) Ember else Parchment, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable internal fun Label(text: String) = Text(text, color = Muted, style = MaterialTheme.typography.labelMedium)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Chips(content: @Composable FlowRowScope.() -> Unit) = FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)

@Composable internal fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LifeRed.copy(alpha = .15f), selectedLabelColor = Color(0xFFFF8A83)),
    )
}

@Composable internal fun RolePill(role: AccountRole) {
    val color = when (role) {
        AccountRole.ADMIN -> LifeRed
        AccountRole.MODERATOR -> Rune
        AccountRole.USER -> Muted
    }
    Text(
        ui("moderation.role.${role.name}"),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.background(color.copy(alpha = .15f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Строка санкции одной фразой: что, на сколько, за что и кем. */
internal fun sanctionLine(sanction: SanctionView): String = listOfNotNull(
    ui("moderation.kind.${sanction.kind.name}") + " · " + ui("moderation.target.${sanction.target.name}"),
    termText(sanction),
    categoryTitle(sanction.category),
    sanction.byLogin,
    "#${sanction.number}",
).joinToString(" · ")

/** Последняя активность героя: «был 5 мин назад», «был 07.10 18:24», «нет данных». */
internal fun seenText(at: Long, now: Long = System.currentTimeMillis()): String {
    if (at <= 0) return ui("moderation.seen_unknown")
    val minutes = (now - at) / 60_000
    return when {
        minutes < 1 -> ui("moderation.seen_now")
        minutes < 60 -> ui("moderation.seen_minutes", minutes)
        minutes < 24 * 60 -> ui("moderation.seen_hours", minutes / 60)
        else -> ui("moderation.seen_at", java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.ROOT).format(java.util.Date(at)))
    }
}

internal fun categoryTitle(category: SanctionCategory): String = ui("moderation.category.${category.name}")

/** Срок санкции словами: «до 11.10 18:02», «навсегда» или «очистка через N дн.». */
internal fun termText(sanction: SanctionView): String? = termText(sanction.kind, sanction.until, sanction.daysLeft())

/** Срок санкции глазами наказанного - теми же словами. */
internal fun termText(notice: SanctionNotice): String? = termText(notice.kind, notice.until, notice.daysLeft())

private fun termText(kind: SanctionKind, until: String?, daysLeft: Long?): String? = when {
    kind == SanctionKind.DELETION -> daysLeft?.let { ui("moderation.purge_in", it) }
    until == null -> ui("moderation.forever")
    else -> listedAt(until)?.let { ui("moderation.until", it) }
}

/** Санкция своего героя одной фразой: что, на сколько и за что - без номера и того, кто наказал. */
internal fun noticeLine(notice: SanctionNotice): String = listOfNotNull(
    ui("moderation.kind.${notice.kind.name}") + " · " + ui("moderation.target.${notice.target.name}"),
    termText(notice),
    categoryTitle(notice.category),
).joinToString(" · ")

private fun hoursText(hours: Int): String = when {
    hours % HOURS_IN_DAY == 0 -> ui("moderation.days", hours / HOURS_IN_DAY)
    else -> ui("moderation.hours", hours)
}

/** Мс эпохи по метке сервера (UTC); не читается - null. */
internal fun epochOf(stamp: String?): Long? = stamp?.let { runCatching { java.time.LocalDateTime.parse(it).toInstant(java.time.ZoneOffset.UTC).toEpochMilli() }.getOrNull() }

private fun trashShare(sanction: SanctionView): Float {
    val from = epochOf(sanction.at) ?: return 0f
    val to = epochOf(sanction.until) ?: return 0f
    return if (to <= from) 1f else ((System.currentTimeMillis() - from).toFloat() / (to - from)).coerceIn(0f, 1f)
}

private const val PRESET_WEEK = 2
private const val TRASH_DAYS = 30
private const val COMMENT = 300
private const val DIGITS = 4
private const val HOURS_IN_DAY = 24
internal const val DATE = 10
internal const val HARDWARE = 6

/** Журнал отдаёт страницу без счёта: полная страница - возможно, есть следующая. */
private const val PAGE_GUESS = 30

/** Роли, что администратор создаёт формой «+»; тестировщик - отметка героя в досье, не роль аккаунта. */
private val CREATABLE = listOf(AccountRole.USER, AccountRole.MODERATOR)

/** Роли, что администратор выдаёт в досье. */
internal val ASSIGNABLE = listOf(AccountRole.USER, AccountRole.MODERATOR)

/**
 * Новый аккаунт: логин и роль - игрок или модератор, - и сервер создаёт аккаунт с паролем,
 * который показывается один раз, с кнопками скопировать логин и пароль.
 */
@Composable private fun AccountSheet(account: AccountUi, created: TesterAccount?, onCreate: (String, AccountRole) -> Unit, onDismiss: () -> Unit) {
    var login by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(AccountRole.USER) }
    val clipboard = LocalClipboardManager.current
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ui("moderation.new_tester"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            if (created == null) {
                OutlinedTextField(
                    login,
                    { login = it.filterNot(Char::isWhitespace).take(account.inputs.login) },
                    label = { Text(ui("moderation.tester_login")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Chips {
                    CREATABLE.forEach { option -> Chip(ui("moderation.role.${option.name}"), role == option) { role = option } }
                }
                ForgeButton(onClick = { onCreate(login, role) }, enabled = !account.busy && login.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(ui("moderation.tester_create")) }
            } else {
                MutedText(ui("moderation.tester_once"))
                listOf(ui("moderation.tester_login") to created.login, ui("moderation.tester_password") to created.password.orEmpty()).forEach { (label, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Label(label)
                            Text(value, color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        ForgeOutlinedButton(onClick = { clipboard.setText(AnnotatedString(value)) }) { Text(ui("tester.copy")) }
                    }
                }
                ForgeButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(ui("tester.ok")) }
            }
        }
    }
}
