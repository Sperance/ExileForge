package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.core.network.AccountRole
import com.sperance.exileforge.core.network.DeviceView
import com.sperance.exileforge.core.network.Dossier
import com.sperance.exileforge.core.network.DossierHero
import com.sperance.exileforge.core.network.GameServer
import com.sperance.exileforge.core.network.SanctionKind
import com.sperance.exileforge.core.network.SanctionTarget
import com.sperance.exileforge.core.network.SanctionView
import com.sperance.exileforge.presentation.admin.ModerationViewModel
import com.sperance.exileforge.presentation.server.AccountUi
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.auction.listedAt
import com.sperance.exileforge.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Досье (0.15.0, #36, макет «Досье», вариант A «Паспорт»): шапка в две строки, одна панель «ключ — значение» в две колонки,
 * свёрнутые строки Экономика, Связи, История и Предначертание с итогом справа, одна строка управления (роль, сервер, тестировщик) и
 * кнопки санкций внизу - досье помещается в экран. Своё досье администратора (#35) - «Сменить» в Предначертании: лист всех даров.
 */
@Composable internal fun DossierView(account: AccountUi, vm: ModerationViewModel, dossier: Dossier?) {
    var sanctioning by remember { mutableStateOf<SanctionKind?>(null) }
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
    val state by vm.state.collectAsStateWithLifecycle()
    DossierHeader(account, dossier, dossier.hero?.let { vm.serverName(it.server) })
    dossier.sanctions.firstOrNull { it.active }?.let { active ->
        Text(
            sanctionLine(active),
            color = LifeRed,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, LifeRed.copy(alpha = .4f), RoundedCornerShape(10.dp)).padding(10.dp),
        )
    }
    DossierFacts(account, dossier)
    DossierFolds(account, vm, dossier) { item ->
        device = item
        sanctioning = SanctionKind.BAN
    }
    ControlRow(account, vm, dossier, state.servers)
    SanctionButtons(account, dossier, onSanction = { kind ->
        device = null
        sanctioning = kind
    }) { deleting = it }
    if (!dossier.rights.ban) MutedText(ui("moderation.protected"))
    sanctioning?.let { kind ->
        BanSheet(account, dossier, device, kind, onDismiss = { sanctioning = null }) {
            sanctioning = null
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

/** Шапка досье: портрет 40dp, «Имя · класс уровень», ниже «логин · роль · сервер · ✦ дар». */
@Composable private fun DossierHeader(account: AccountUi, dossier: Dossier, server: String?) {
    val hero = dossier.hero
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ClassPortrait(hero?.heroClass, account.world.portraits, Modifier.size(40.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(hero?.name ?: dossier.account.login, color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                hero?.let { MutedText("· ${classTitle(it.heroClass)} ${it.level}", style = MaterialTheme.typography.labelMedium) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                MutedText(dossier.account.login, style = MaterialTheme.typography.labelSmall)
                RoleMark(dossier.account.role, 14.dp)
                server?.let { MutedText("· $it", style = MaterialTheme.typography.labelSmall) }
                dossier.account.fate?.let { FateMark(FateCard(it)) }
            }
        }
    }
}

/** Строка панели фактов: подпись, значение и его цвет. */
private data class Fact(val label: String, val value: String, val color: Color = Parchment)

/**
 * Панель фактов (макет A): «ключ — значение» плотными строками в две колонки - регистрация, вход, в сети (последнее использование
 * сессии аккаунта, коротко), клиент, устройство, у героя - с какого дня, гильдия, золото, бои, уровни в час и часы боя. Версии
 * Android сервер не знает - её строки нет.
 */
@Composable private fun DossierFacts(account: AccountUi, dossier: Dossier) {
    val now = System.currentTimeMillis()
    val owner = dossier.account
    val hero = dossier.hero
    val seen = epochOf(owner.lastSeenAt)
    val online = seen != null && account.index?.rules?.online(seen, now) == true
    val facts = buildList {
        add(Fact(ui("moderation.registered"), epochOf(owner.registeredAt)?.let(::dayOf) ?: DASH))
        add(Fact(ui("moderation.last_login"), epochOf(owner.lastLoginAt)?.let(::momentOf) ?: DASH))
        add(Fact(ui("dossier.online"), seen?.let { (if (online) "● " else "") + seenShort(it, now) } ?: DASH, if (online) Vital else Parchment))
        add(Fact(ui("moderation.client"), owner.clientVersion.ifBlank { DASH }))
        add(Fact(ui("dossier.device"), dossier.devices.firstOrNull()?.let { it.model.ifBlank { it.hardware.take(HARDWARE) } } ?: DASH))
        hero?.let {
            add(Fact(ui("dossier.hero_since"), epochOf(it.createdAt)?.let(::dayOf) ?: DASH))
            add(Fact(ui("moderation.guild"), it.guild ?: DASH))
            add(Fact(ui("moderation.gold"), compactNumber(it.gold.toDouble())))
            add(Fact(ui("moderation.fights"), number(it.fights.toDouble())))
            val flagged = dossier.flags.any { flag -> flag.code == LEVEL_RATE }
            add(Fact(ui("moderation.level_rate"), (it.levelsPerHour?.let(::fineNumber) ?: DASH) + if (flagged) " ⚠" else "", if (flagged) Ember else Parchment))
            add(Fact(ui("dossier.fight_hours"), fineNumber(it.fightHours)))
        }
    }
    ForgePanel {
        facts.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEach { fact ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(fact.label, color = Muted, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp))
                        Spacer(Modifier.weight(1f))
                        Text(fact.value, color = fact.color, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Свёрнутая строка досье (макет A): своё имя и итог справа; касание раскрывает подробности под ней. */
private enum class DossierFold(val title: String) {
    ECONOMY("moderation.economy"),
    LINKS("moderation.links"),
    HISTORY("moderation.history"),
    FATE("moderation.fate.title"),
}

/**
 * Свёрнутые строки досье: экономика (стоимость снаряжения и уникальные), связи (устройства, гильдия, другие герои), история
 * (санкции, апелляции и отчёты) и Предначертание (лента с датой выбора; своё досье администратора - «Сменить»). Итог строки -
 * справа, раскрыта одна. [onBanDevice] - бан устройства из связей.
 */
@Composable private fun DossierFolds(account: AccountUi, vm: ModerationViewModel, dossier: Dossier, onBanDevice: (DeviceView) -> Unit) {
    var open by remember(dossier.account.userId, dossier.hero?.heroId) { mutableStateOf<DossierFold?>(null) }
    var redrawing by remember { mutableStateOf(false) }
    val folds = DossierFold.entries.filter { it != DossierFold.ECONOMY || dossier.economy != null }
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, Bronze, shape)) {
        folds.forEachIndexed { i, fold ->
            if (i > 0) HorizontalDivider(color = Bronze)
            val (summary, tint) = summary(fold, dossier)
            Row(
                Modifier.fillMaxWidth().clickable { open = if (open == fold) null else fold }.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text((if (open == fold) "▾ " else "▸ ") + ui(fold.title), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text(summary, color = tint, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (open == fold) {
                Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (fold) {
                        DossierFold.ECONOMY -> dossier.economy?.let { economy ->
                            Line(ui("moderation.gear_value"), "≈ ${number(economy.gearValue.toDouble())}")
                            Line(ui("moderation.uniques"), "${economy.uniques} / ${economy.mythics}")
                            Line(ui("moderation.trades"), economy.tradesPerDay.toString(), flagged = dossier.flags.any { it.code == TRADES })
                        }

                        DossierFold.LINKS -> Links(account, vm, dossier, onBanDevice)

                        DossierFold.HISTORY -> {
                            if (dossier.sanctions.isEmpty()) MutedText(ui("moderation.clean"))
                            dossier.sanctions.forEach { sanction -> SanctionLine(sanction, account.busy) { vm.lift(sanction) } }
                            MutedText(ui("moderation.reports", dossier.appeals, dossier.reports))
                        }

                        DossierFold.FATE -> {
                            val fate = dossier.account.fate
                            if (fate == null) MutedText(DASH) else FateRibbon(FateCard(fate), ui("moderation.fate.chosen", dossier.account.fateAt?.let(::dayOf) ?: DASH), Modifier.fillMaxWidth())
                            // Смена дара (0.15.0, #35): только администратор и только себе - сервер берёт цель из сессии
                            if (account.isAdmin && dossier.account.userId == account.session.profile?.id) {
                                ForgeOutlinedButton(enabled = !account.busy, onClick = { redrawing = true }, modifier = Modifier.fillMaxWidth()) { Text(ui("moderation.fate.change"), color = Gold) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (redrawing) {
        FateRedrawSheet(account, dossier.account.fate, onDismiss = { redrawing = false }) { code ->
            redrawing = false
            vm.redrawFate(code)
        }
    }
}

/** Итог свёрнутой строки [fold] справа и его цвет: «снаряж. 41K · 5 уник.», «2 устр. · общих нет», «чисто», дата выбора дара. */
private fun summary(fold: DossierFold, dossier: Dossier): Pair<String, Color> = when (fold) {
    DossierFold.ECONOMY -> dossier.economy?.let { ui("dossier.economy_sum", compactNumber(it.gearValue.toDouble()), it.uniques) to Muted } ?: (DASH to Muted)

    DossierFold.LINKS -> {
        val shared = dossier.devices.count { it.sharedWith.isNotEmpty() }
        ui("dossier.devices", dossier.devices.size) + " · " + (if (shared > 0) ui("dossier.shared", shared) else ui("dossier.shared_none")) to if (shared > 0) Ember else Muted
    }

    DossierFold.HISTORY -> when {
        dossier.sanctions.isEmpty() -> ui("dossier.clean") to Vital
        else -> ui("dossier.sanctions", dossier.sanctions.size) to if (dossier.sanctions.any { it.active }) LifeRed else Caution
    }

    DossierFold.FATE -> (dossier.account.fateAt?.let(::dayOf) ?: DASH) to Muted
}

/** Связи досье: устройства (модель, первая дата, последний вход, версия, общее с кем и бан устройства), гильдия, другие герои. */
@Composable private fun Links(account: AccountUi, vm: ModerationViewModel, dossier: Dossier, onBanDevice: (DeviceView) -> Unit) {
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
                ForgeTextButton(enabled = !account.busy, onClick = { onBanDevice(item) }) { Text(ui("moderation.ban_device"), color = LifeRed) }
            }
        }
    }
    dossier.hero?.guild?.let { Line(ui("moderation.guild"), it) }
    dossier.heroes.forEach { other -> OtherHero(other) { vm.open(other.heroId, dossier.account.userId) } }
}

/**
 * Строка управления (макет A): роль аккаунта (администратор; администратора не меняет никто), сервер героя (администратор: перенос
 * с подтверждением - он уводит из гильдии и снимает лоты, «+ новый сервер») и отметка тестировщика (4.12.x, ROLE-02: администратор и
 * модератор) - выпадающими выборами одной строкой. Нечего менять - строки нет.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlRow(account: AccountUi, vm: ModerationViewModel, dossier: Dossier, servers: List<GameServer>) {
    val hero = dossier.hero
    val role = account.isAdmin && dossier.account.role != AccountRole.ADMIN
    val server = account.isAdmin && hero != null
    val tester = account.isModerator && hero != null
    if (!role && !server && !tester) return
    if (server) LaunchedEffect(Unit) { vm.loadServers() }
    var moving by remember(hero?.heroId) { mutableStateOf<GameServer?>(null) }
    var founding by remember { mutableStateOf(false) }
    ForgePanel {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            if (role) {
                MutedText(ui("dossier.role"), style = MaterialTheme.typography.labelSmall)
                Picker(ui("moderation.role.${dossier.account.role.name}"), !account.busy, ASSIGNABLE.map { option -> ui("moderation.role.${option.name}") to { if (option != dossier.account.role) vm.setRole(dossier.account.userId, option) } })
            }
            if (server && hero != null) {
                MutedText(ui("dossier.server"), style = MaterialTheme.typography.labelSmall)
                val known = servers.ifEmpty { listOf(GameServer(hero.server, hero.server)) }
                Picker(
                    vm.serverName(hero.server),
                    !account.busy,
                    known.map { option -> option.name.ifBlank { option.code } to { if (option.code != hero.server) moving = option } } + (ui("dossier.new_server") to { founding = true }),
                )
            }
            if (tester && hero != null) {
                MutedText(ui("dossier.tester"), style = MaterialTheme.typography.labelSmall)
                Switch(
                    checked = hero.tester,
                    enabled = !account.busy,
                    onCheckedChange = { vm.setTester(hero.heroId, it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Elder),
                    modifier = Modifier.height(24.dp),
                )
            }
        }
    }
    val target = moving
    if (target != null && hero != null) {
        ConfirmSheet(
            title = ui("admin.move_q", hero.name),
            confirm = ui("admin.move"),
            danger = true,
            subtitle = "${vm.serverName(hero.server)} → ${target.name.ifBlank { target.code }}",
            note = ui("admin.move_note"),
            onDismiss = { moving = null },
        ) {
            moving = null
            vm.moveHero(hero.heroId, target.code)
        }
    }
    if (founding) {
        ServerSheet(account, onDismiss = { founding = false }) { code, name ->
            founding = false
            vm.createServer(code, name)
        }
    }
}

/** Выпадающий выбор строки управления: значение «Админ ▾» в нити; пункт [options] - подпись и что он делает. */
@Composable private fun Picker(value: String, enabled: Boolean, options: List<Pair<String, () -> Unit>>) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box {
        Text(
            "$value ▾",
            color = if (enabled) Rune else Muted,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.clip(shape).border(1.dp, Bronze, shape).clickable(enabled = enabled) { expanded = true }.padding(horizontal = 8.dp, vertical = 3.dp),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, pick) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    expanded = false
                    pick()
                })
            }
        }
    }
}

/** Кнопки санкций внизу досье (макет A): бан, немота (4.6.3) и удаление - что разрешают права досье, поровну в ширину. */
@Composable private fun SanctionButtons(account: AccountUi, dossier: Dossier, onSanction: (SanctionKind) -> Unit, onDelete: (SanctionTarget) -> Unit) {
    val hero = dossier.hero
    val actions = buildList<Pair<String, () -> Unit>> {
        if (dossier.rights.ban) add(ui("moderation.ban") to { onSanction(SanctionKind.BAN) })
        // Немота (4.6.3): не пишет письма, играет как прежде
        if (dossier.rights.mute) add(ui("moderation.mute") to { onSanction(SanctionKind.MUTE) })
        if (dossier.rights.delete) add(ui("moderation.delete") to { onDelete(if (hero != null && !hero.deleted) SanctionTarget.HERO else SanctionTarget.ACCOUNT) })
    }
    if (actions.isEmpty()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        actions.forEach { (label, act) ->
            ForgeOutlinedButton(enabled = !account.busy, onClick = act, modifier = Modifier.weight(1f)) { Text(label, color = LifeRed, maxLines = 1) }
        }
    }
}

/**
 * Лист смены Предначертания своему аккаунту (0.15.0, #35): сетка всех даров контента по имени в два столбца, описание выбранного и
 * «удержи, чтобы сменить» - нынешний дар выбрать нельзя. Счётчики дара у героев начнутся с нуля, начатый заход доиграет со старым.
 */
@Composable private fun FateRedrawSheet(account: AccountUi, current: String?, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val fates = remember(account.index) { account.index?.fates?.all.orEmpty().map(FateCard::of).sortedBy { it.title } }
    var picked by remember { mutableStateOf(current) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("moderation.fate.change_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            MutedText(ui("moderation.fate.change_note"))
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.fillMaxWidth().heightIn(max = FATE_GRID_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(fates, key = { it.code }) { card -> FateOption(card.title, mark = true, selected = picked == card.code) { picked = card.code } }
            }
            picked?.let { code ->
                Text("«${FateCard(code).text}»", color = Muted, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)
            }
            HoldButton(ui("moderation.fate.hold"), Gold, Modifier.fillMaxWidth(), enabled = !account.busy && picked != null && picked != current) { picked?.let(onPick) }
        }
    }
}

@Composable private fun ServerSheet(account: AccountUi, onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ui("admin.server_new"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                code,
                { value -> code = value.filter { it.isLetterOrDigit() || it == '_' }.uppercase().take(SERVER_CODE) },
                label = { Text(ui("admin.server_code")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                name,
                { name = it.take(SERVER_NAME) },
                label = { Text(ui("admin.server_name")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ForgeButton(onClick = { onCreate(code, name) }, enabled = !account.busy && code.isNotBlank() && name.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(ui("admin.server_create"))
            }
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
            // Причина для игрока (4.12.x) и служебная заметка - модерация видит обе
            if (sanction.reason.isNotBlank()) MutedText(ui("moderation.reason_shown", sanction.reason))
            if (sanction.comment.isNotBlank()) MutedText("«${sanction.comment}»")
        }
        if (sanction.active) {
            ForgeTextButton(enabled = !busy, onClick = onLift) { Text(ui(if (sanction.kind == SanctionKind.DELETION) "moderation.restore" else "moderation.lift"), color = Gold) }
        } else {
            Text(ui(if (sanction.purgedAt != null) "moderation.purged" else "moderation.inactive"), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Давность в сети коротко (макет A): «сейчас», «5 мин», «3 ч», дальше - «14.10 21:03». */
private fun seenShort(at: Long, now: Long): String {
    val minutes = (now - at) / MINUTE_MS
    return when {
        minutes < 1 -> ui("dossier.seen.now")
        minutes < MINUTES_IN_HOUR -> ui("dossier.seen.minutes", minutes)
        minutes < MINUTES_IN_DAY -> ui("dossier.seen.hours", minutes / MINUTES_IN_HOUR)
        else -> momentOf(at)
    }
}

/** День в зоне устройства: «12.03.26». */
private fun dayOf(at: Long): String = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(DAY)

/** Миг в зоне устройства: «14.10 21:03». */
private fun momentOf(at: Long): String = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(MOMENT)

private val DAY = DateTimeFormatter.ofPattern("dd.MM.yy")
private val MOMENT = DateTimeFormatter.ofPattern("dd.MM HH:mm")
private const val DASH = "—"
private const val MINUTE_MS = 60_000L
private const val MINUTES_IN_HOUR = 60
private const val MINUTES_IN_DAY = 24 * 60
private const val SERVER_CODE = 16
private const val SERVER_NAME = 32

/** Подозрения досье, что подсвечивают свою строку. */
private const val LEVEL_RATE = "LEVEL_RATE"
private const val TRADES = "TRADES"
