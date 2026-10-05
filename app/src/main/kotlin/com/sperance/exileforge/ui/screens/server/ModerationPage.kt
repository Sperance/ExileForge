package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.AccountRole
import com.sperance.exileforge.core.network.DeletionRequest
import com.sperance.exileforge.core.network.DeviceView
import com.sperance.exileforge.core.network.Dossier
import com.sperance.exileforge.core.network.DossierHero
import com.sperance.exileforge.core.network.ModerationEntryView
import com.sperance.exileforge.core.network.ModerationRow
import com.sperance.exileforge.core.network.SanctionCategory
import com.sperance.exileforge.core.network.SanctionKind
import com.sperance.exileforge.core.network.SanctionRequest
import com.sperance.exileforge.core.network.SanctionTarget
import com.sperance.exileforge.core.network.SanctionView
import com.sperance.exileforge.presentation.admin.ModerationTab
import com.sperance.exileforge.presentation.admin.ModerationViewModel
import com.sperance.exileforge.presentation.server.AccountUi
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.auction.listedAt
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Окно модерации (3.88.5, server 1.80.8; вариант B «Досье-карточки» из трёх макетов): поиск, разделы «Все / Баны / Корзина /
 * Журнал», карточки героев с кромкой статуса; тап открывает досье. Бан и удаление - нижними шторками, удаление - удержанием.
 * Что смотрящему нельзя (админ, модератор для модератора), сервер не пропустит, а карточка показывает замком.
 */
@Composable internal fun ModerationPage(account: AccountUi) {
    val vm = koinViewModel<ModerationViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load(0) }
    if (state.dossierKey != null) {
        DossierView(account, vm, state.dossier)
        return
    }
    OutlinedTextField(
        state.query,
        vm::search,
        placeholder = { Text(ui("moderation.search")) },
        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Muted) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { vm.load(0) }),
        modifier = Modifier.fillMaxWidth(),
    )
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
    MutedText(ui("moderation.total", page.total))
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
        Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, if (banned != null) LifeRed.copy(alpha = .45f) else Bronze, shape)
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
                RolePill(row.role)
            }
            MutedText(listOf(classTitle(row.heroClass), ui("pets.level", row.level), row.login).filter { it.isNotBlank() }.joinToString(" · "))
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
        Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, Bronze, shape).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClassPortrait(row.heroClass.takeIf { it.isNotBlank() }, account.world.portraits, Modifier.size(44.dp).alpha(.45f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(row.heroName.ifBlank { sanction.label }, color = Parchment, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            MutedText(listOf(ui("moderation.target.${sanction.target.name}"), sanction.byLogin, categoryTitle(sanction.category)).joinToString(" · "))
            val left = daysLeft(sanction)
            LinearProgressIndicator(progress = { trashShare(sanction) }, color = Muted, trackColor = PanelRaised, modifier = Modifier.fillMaxWidth().height(3.dp))
            Text(ui("moderation.purge_in", left ?: 0), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        if (!row.protected) {
            IconButton(enabled = !account.busy, onClick = onRestore) { Icon(Icons.Outlined.Restore, ui("moderation.restore"), tint = Gold) }
        }
    }
}

@Composable private fun JournalRow(entry: ModerationEntryView) {
    ForgePanel {
        Text(
            ui("moderation.action.${entry.action.name}", entry.actorLogin.ifBlank { ui("moderation.server") }, ui("moderation.target.${entry.target.name}"), entry.label),
            color = Parchment,
            style = MaterialTheme.typography.bodyMedium,
        )
        MutedText(listOfNotNull("#${entry.number}", categoryTitle(entry.category), entry.until?.let { ui("moderation.until", listedAt(it).orEmpty()) }, listedAt(entry.at)).joinToString(" · "))
        if (entry.comment.isNotBlank()) MutedText("«${entry.comment}»")
    }
}

// ==================== Досье ====================

@Composable private fun DossierView(account: AccountUi, vm: ModerationViewModel, dossier: Dossier?) {
    var banning by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<SanctionTarget?>(null) }
    var device by remember { mutableStateOf<DeviceView?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = vm::close) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, ui("common.back"), tint = Parchment) }
        Text(ui("moderation.dossier"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
    }
    if (dossier == null) {
        CircularProgressIndicator(color = Gold, modifier = Modifier.size(28.dp))
        return
    }
    val hero = dossier.hero
    val focus = hero?.name ?: dossier.account.login
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ClassPortrait(hero?.heroClass, account.world.portraits, Modifier.size(52.dp))
        Column(Modifier.weight(1f)) {
            Text(focus, color = GoldBright, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MutedText(dossier.account.login)
                RolePill(dossier.account.role)
                hero?.let { MutedText("${classTitle(it.heroClass)} ${it.level}") }
            }
        }
    }
    dossier.sanctions.firstOrNull { it.active }?.let { active ->
        Text(
            sanctionLine(active),
            color = LifeRed,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().background(LifeRed.copy(alpha = .1f), RoundedCornerShape(10.dp)).padding(10.dp),
        )
    }
    val tiles = buildList {
        hero?.let {
            add(ui("moderation.gold") to number(it.gold.toDouble()))
            add(ui("moderation.fights") to it.fights.toString())
            add(ui("moderation.level_rate") to (it.levelsPerHour?.let { rate -> "%.2f".format(rate) } ?: "—"))
        }
        add(ui("moderation.registered") to (listedAt(dossier.account.registeredAt)?.take(DATE) ?: "—"))
        add(ui("moderation.last_login") to (dossier.account.lastLoginAt?.let(::listedAt) ?: "—"))
        add(ui("moderation.client") to dossier.account.clientVersion.ifBlank { "—" })
    }
    tiles.chunked(TILES).forEach { line ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            line.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f), flagged = dossier.flags.any { label == ui("moderation.level_rate") && it.code == "LEVEL_RATE" }) }
            repeat(TILES - line.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    dossier.economy?.let { economy ->
        Section(ui("moderation.economy")) {
            Line(ui("moderation.gear_value"), "≈ ${number(economy.gearValue.toDouble())}")
            Line(ui("moderation.uniques"), "${economy.uniques} / ${economy.mythics}")
            Line(ui("moderation.trades"), economy.tradesPerDay.toString(), flagged = dossier.flags.any { it.code == "TRADES" })
        }
    }
    Section(ui("moderation.links")) {
        if (dossier.devices.isEmpty()) MutedText(ui("moderation.no_devices"))
        dossier.devices.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Smartphone, null, tint = Muted, modifier = Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.model.ifBlank { item.hardware.take(HARDWARE) }, color = Parchment, style = MaterialTheme.typography.bodySmall)
                    MutedText(listOfNotNull(listedAt(item.firstAt)?.take(DATE), listedAt(item.lastAt), item.clientVersion.ifBlank { null }).joinToString(" · "))
                    if (item.sharedWith.isNotEmpty()) Text(ui("moderation.shared", item.sharedWith.joinToString()), color = Ember, style = MaterialTheme.typography.labelSmall)
                }
                if (dossier.rights.banDevice) {
                    ForgeTextButton(enabled = !account.busy, onClick = {
                        device = item
                        banning = true
                    }) { Text(ui("moderation.ban_device"), color = LifeRed) }
                }
            }
        }
        hero?.guild?.let { Line(ui("moderation.guild"), it) }
        dossier.heroes.forEach { other -> OtherHero(other) { vm.open(other.heroId, dossier.account.userId) } }
    }
    Section(ui("moderation.history")) {
        if (dossier.sanctions.isEmpty()) MutedText(ui("moderation.clean"))
        dossier.sanctions.forEach { sanction -> SanctionLine(sanction, account.busy) { vm.lift(sanction) } }
        MutedText(ui("moderation.reports", dossier.appeals, dossier.reports))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (dossier.rights.ban) {
            ForgeButton(enabled = !account.busy, onClick = {
                device = null
                banning = true
            }, modifier = Modifier.weight(1f)) { Text(ui("moderation.ban")) }
        }
        if (dossier.rights.delete) {
            ForgeOutlinedButton(enabled = !account.busy, onClick = { deleting = if (hero != null && !hero.deleted) SanctionTarget.HERO else SanctionTarget.ACCOUNT }) {
                Text(ui("moderation.delete"), color = LifeRed)
            }
        }
    }
    if (!dossier.rights.ban) MutedText(ui("moderation.protected"))
    if (banning) {
        BanSheet(account, dossier, device, onDismiss = { banning = false }) {
            banning = false
            vm.ban(it)
        }
    }
    deleting?.let { target ->
        DeleteSheet(account, dossier, target, onTarget = { deleting = it }, onDismiss = { deleting = null }) {
            deleting = null
            vm.delete(it)
        }
    }
}

@Composable private fun OtherHero(hero: DossierHero, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(hero.name, color = if (hero.deleted) Muted else Parchment, style = MaterialTheme.typography.bodySmall)
        MutedText("${classTitle(hero.heroClass)} ${hero.level}${if (hero.deleted) " · " + ui("moderation.deleted_mark") else ""}")
    }
}

@Composable private fun SanctionLine(sanction: SanctionView, busy: Boolean, onLift: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                "#${sanction.number} · ${ui("moderation.kind.${sanction.kind.name}")} · ${ui("moderation.target.${sanction.target.name}")} · ${categoryTitle(sanction.category)}",
                color = Parchment,
                style = MaterialTheme.typography.bodySmall,
            )
            MutedText(
                listOfNotNull(sanction.byLogin, listedAt(sanction.at), termText(sanction), sanction.liftedAt?.let { ui("moderation.lifted_by", sanction.liftedBy) }, if (sanction.appealed) ui("moderation.appealed") else null)
                    .joinToString(" · "),
            )
        }
        if (sanction.active) {
            ForgeTextButton(enabled = !busy, onClick = onLift) { Text(ui(if (sanction.kind == SanctionKind.DELETION) "moderation.restore" else "moderation.lift"), color = Gold) }
        } else {
            Text(ui(if (sanction.purgedAt != null) "moderation.purged" else "moderation.inactive"), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ==================== Шторки ====================

/** Шторка бана: цель (герой, аккаунт или устройство), срок - готовый или свой, категория и комментарий для игрока. */
@Composable private fun BanSheet(account: AccountUi, dossier: Dossier, device: DeviceView?, onDismiss: () -> Unit, onBan: (SanctionRequest) -> Unit) {
    val presets = account.index?.rules?.moderation?.banHours ?: DEFAULT_HOURS
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
    val limit = account.index?.rules?.inputs?.sanctionComment ?: COMMENT
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("moderation.ban_title", dossier.hero?.name ?: dossier.account.login), color = GoldBright, style = MaterialTheme.typography.titleMedium)
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
            Chips { SanctionCategory.entries.forEach { Chip(categoryTitle(it), category == it) { category = it } } }
            OutlinedTextField(comment, { comment = it.take(limit) }, label = { Text(ui("moderation.comment")) }, minLines = 2, supportingText = { LengthCounter(comment, limit) }, modifier = Modifier.fillMaxWidth())
            val term = custom.toIntOrNull() ?: hours
            ForgeButton(
                enabled = !account.busy && (custom.isBlank() || (custom.toIntOrNull() ?: 0) > 0),
                onClick = { onBan(SanctionRequest(target, dossier.hero?.heroId.orEmpty(), dossier.account.userId, device?.hardware.orEmpty(), term, category, comment)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(ui("moderation.ban_for", ui("moderation.target.$target"), term?.let(::hoursText) ?: ui("moderation.forever"))) }
        }
    }
}

/** Шторка удаления (только администратор): герой или аккаунт целиком, причина обязательна, удаление - удержанием кнопки. */
@Composable private fun DeleteSheet(account: AccountUi, dossier: Dossier, target: SanctionTarget, onTarget: (SanctionTarget) -> Unit, onDismiss: () -> Unit, onDelete: (DeletionRequest) -> Unit) {
    var category by remember { mutableStateOf(SanctionCategory.OTHER) }
    var comment by remember { mutableStateOf("") }
    val days = account.index?.rules?.moderation?.trashDays ?: TRASH_DAYS
    val limit = account.index?.rules?.inputs?.sanctionComment ?: COMMENT
    val hero = dossier.hero?.takeIf { !it.deleted }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("moderation.delete_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            Chips {
                hero?.let { Chip(it.name, target == SanctionTarget.HERO) { onTarget(SanctionTarget.HERO) } }
                Chip(ui("moderation.delete_account", dossier.account.login, dossier.account.heroes), target == SanctionTarget.ACCOUNT) { onTarget(SanctionTarget.ACCOUNT) }
            }
            MutedText(ui(if (target == SanctionTarget.HERO) "moderation.delete_hero_note" else "moderation.delete_account_note", days))
            Chips { SanctionCategory.entries.forEach { Chip(categoryTitle(it), category == it) { category = it } } }
            OutlinedTextField(comment, { comment = it.take(limit) }, label = { Text(ui("moderation.delete_reason")) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            HoldButton(
                ui("moderation.hold_delete"),
                LifeRed,
                modifier = Modifier.fillMaxWidth(),
                enabled = !account.busy && comment.isNotBlank(),
            ) { onDelete(DeletionRequest(target, if (target == SanctionTarget.HERO) hero?.heroId.orEmpty() else dossier.account.userId, category, comment)) }
        }
    }
}

// ==================== Мелочи ====================

@Composable private fun StatTile(label: String, value: String, modifier: Modifier, flagged: Boolean = false) {
    Column(modifier.background(Panel, RoundedCornerShape(12.dp)).border(1.dp, Bronze, RoundedCornerShape(12.dp)).padding(8.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Text(value, color = if (flagged) Ember else GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    ForgePanel(accent = Rune) {
        Text(title, color = Rune, style = MaterialTheme.typography.labelLarge)
        content()
    }
}

@Composable private fun Line(label: String, value: String, flagged: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Parchment, style = MaterialTheme.typography.bodySmall)
        Text(if (flagged) "$value ⚠" else value, color = if (flagged) Ember else Parchment, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun Label(text: String) = Text(text, color = Muted, style = MaterialTheme.typography.labelMedium)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(content: @Composable FlowRowScope.() -> Unit) = FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)

@Composable private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
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
        AccountRole.TESTER -> Elder
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

internal fun categoryTitle(category: SanctionCategory): String = ui("moderation.category.${category.name}")

/** Срок санкции словами: «до 11.10 18:02», «навсегда» или «очистка через N дн.». */
internal fun termText(sanction: SanctionView): String? {
    val until = sanction.until
    return when {
        sanction.kind == SanctionKind.DELETION -> daysLeft(sanction)?.let { ui("moderation.purge_in", it) }
        until == null -> ui("moderation.forever")
        else -> listedAt(until)?.let { ui("moderation.until", it) }
    }
}

private fun hoursText(hours: Int): String = when {
    hours % HOURS_IN_DAY == 0 -> ui("moderation.days", hours / HOURS_IN_DAY)
    else -> ui("moderation.hours", hours)
}

/** Мс эпохи по метке сервера (UTC); не читается - null. */
internal fun epochOf(stamp: String?): Long? = stamp?.let { runCatching { java.time.LocalDateTime.parse(it).toInstant(java.time.ZoneOffset.UTC).toEpochMilli() }.getOrNull() }

private fun daysLeft(sanction: SanctionView): Long? = epochOf(sanction.until)?.let { ((it - System.currentTimeMillis()).coerceAtLeast(0) + DAY_MS - 1) / DAY_MS }

private fun trashShare(sanction: SanctionView): Float {
    val from = epochOf(sanction.at) ?: return 0f
    val to = epochOf(sanction.until) ?: return 0f
    return if (to <= from) 1f else ((System.currentTimeMillis() - from).toFloat() / (to - from)).coerceIn(0f, 1f)
}

private val DEFAULT_HOURS = listOf(1, 24, 168, 720)
private const val PRESET_WEEK = 2
private const val TRASH_DAYS = 30
private const val COMMENT = 300
private const val DIGITS = 4
private const val HOURS_IN_DAY = 24
private const val DAY_MS = 86_400_000L
private const val TILES = 3
private const val DATE = 10
private const val HARDWARE = 6

/** Журнал отдаёт страницу без счёта: полная страница - возможно, есть следующая. */
private const val PAGE_GUESS = 30
