package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.admin.ModerationModel
import com.sperance.exileforge.core.app.AccountUi
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.fateDescription
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.regionTitle
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
import com.sperance.exileforge.rules.run.RunFlag
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.auction.listedAt
import com.sperance.exileforge.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Досье как страница настроек: шапка в две строки, строки «Роль ›», «Сервер ›» и переключатель «Тестировщик» - значение роли и сервера
 * печатается только в своей строке; одна панель «ключ — значение» в две колонки, свёрнутые строки Экономика, Связи, История и
 * Предначертание с итогом справа и красный блок опасных действий внизу - у каждого строка последствий. Своё досье администратора -
 * «Сменить» в Предначертании: лист всех даров. Досье - уровень цепочки «назад» настроек ([TrailLevel] в [ModerationPage]): своей
 * стрелки у него нет.
 */
@Composable internal fun DossierView(account: AccountUi, vm: ModerationModel, dossier: Dossier?) {
    var sanctioning by remember { mutableStateOf<SanctionKind?>(null) }
    var deleting by remember { mutableStateOf<SanctionTarget?>(null) }
    var device by remember { mutableStateOf<DeviceView?>(null) }
    if (dossier == null) {
        CircularProgressIndicator(color = Gold, modifier = Modifier.size(28.dp))
        return
    }
    val state by vm.state.collectAsStateWithLifecycle()
    DossierHeader(account, dossier)
    dossier.sanctions.firstOrNull { it.active }?.let { active ->
        Text(
            sanctionLine(active),
            color = LifeRed,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, LifeRed.copy(alpha = .4f), RoundedCornerShape(10.dp)).padding(10.dp),
        )
    }
    SettingRows(account, vm, dossier, state.servers)
    DossierFacts(account, dossier)
    DossierFolds(account, vm, dossier) { item ->
        device = item
        sanctioning = SanctionKind.BAN
    }
    DangerBlock(account, dossier, vm.owns(dossier.account.userId), onSanction = { kind ->
        device = null
        sanctioning = kind
    }) { deleting = it }
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

/** Шапка досье: портрет 40dp, «Имя · класс уровень», ниже «логин · ✦ дар»; роль и сервер - в своих строках под шапкой. */
@Composable private fun DossierHeader(account: AccountUi, dossier: Dossier) {
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
                dossier.account.fate?.let { FateMark(FateCard(it)) }
            }
        }
    }
}

/** Строка панели фактов: подпись, значение и его цвет. */
private data class Fact(val label: String, val value: String, val color: Color = Parchment)

/**
 * Панель фактов: «ключ — значение» плотными строками в две колонки - регистрация, вход, в сети (последнее использование
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

/** Свёрнутая строка досье: своё имя и итог справа; касание раскрывает подробности под ней. */
private enum class DossierFold(val title: String) {
    ECONOMY("moderation.economy"),
    LINKS("moderation.links"),
    HISTORY("moderation.history"),
    PACE("moderation.run_flags"),
    FATE("moderation.fate.title"),
}

/**
 * Свёрнутые строки досье: экономика (стоимость снаряжения и уникальные), связи (устройства, гильдия, другие герои), история
 * (санкции, апелляции и отчёты), метки темпа героя (только когда они есть) и Предначертание (лента с датой выбора; своё досье
 * администратора - «Сменить»). Итог строки - справа, раскрыта одна. [onBanDevice] - бан устройства из связей.
 *
 * Смена дара - только администратор и только себе: сервер берёт цель из сессии.
 */
@Composable private fun DossierFolds(account: AccountUi, vm: ModerationModel, dossier: Dossier, onBanDevice: (DeviceView) -> Unit) {
    var open by remember(dossier.account.userId, dossier.hero?.heroId) { mutableStateOf<DossierFold?>(null) }
    var redrawing by remember { mutableStateOf(false) }
    val runFlags = dossier.hero?.runFlags.orEmpty()
    val refuseWeight = account.index?.rules?.run?.plausibility?.refuseWeight
    val folds = DossierFold.entries.filter {
        when (it) {
            DossierFold.ECONOMY -> dossier.economy != null
            DossierFold.PACE -> runFlags.isNotEmpty()
            else -> true
        }
    }
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, Bronze, shape)) {
        folds.forEachIndexed { i, fold ->
            if (i > 0) HorizontalDivider(color = Bronze)
            val (summary, tint) = summary(fold, dossier, refuseWeight)
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

                        DossierFold.PACE -> runFlags.asReversed().forEach { flag -> RunFlagLine(flag, refuseWeight) }

                        DossierFold.FATE -> {
                            val fate = dossier.account.fate
                            if (fate == null) MutedText(DASH) else FateRibbon(FateCard(fate), ui("moderation.fate.chosen", dossier.account.fateAt?.let(::dayOf) ?: DASH), Modifier.fillMaxWidth())
                            if (dossier.rights.fate) {
                                ForgeButton(state = ButtonState.of(!account.busy), onClick = { redrawing = true }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, accent = Gold) { Text(ui("moderation.fate.change")) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (redrawing) {
        val other = !vm.owns(dossier.account.userId)
        FateRedrawSheet(account, dossier.account.fate, dossier.account.login.takeIf { other }, onDismiss = { redrawing = false }) { code, reason ->
            redrawing = false
            vm.redrawFate(code, dossier.account.userId, reason)
        }
    }
}

/**
 * Итог свёрнутой строки [fold] справа и его цвет: «снаряж. 41K · 5 уник.», «2 устр. · общих нет», «чисто», «меток: 3» (есть метка
 * с весом отказа [refuseWeight] - [Ember]), дата выбора дара.
 */
private fun summary(fold: DossierFold, dossier: Dossier, refuseWeight: Int?): Pair<String, Color> = when (fold) {
    DossierFold.ECONOMY -> dossier.economy?.let { ui("dossier.economy_sum", compactNumber(it.gearValue.toDouble()), it.uniques) to Muted } ?: (DASH to Muted)

    DossierFold.LINKS -> {
        val shared = dossier.devices.count { it.sharedWith.isNotEmpty() }
        ui("dossier.devices", dossier.devices.size) + " · " + (if (shared > 0) ui("dossier.shared", shared) else ui("dossier.shared_none")) to if (shared > 0) Ember else Muted
    }

    DossierFold.HISTORY -> when {
        dossier.sanctions.isEmpty() -> ui("dossier.clean") to Vital
        else -> ui("dossier.sanctions", dossier.sanctions.size) to if (dossier.sanctions.any { it.active }) LifeRed else Caution
    }

    DossierFold.PACE -> {
        val flags = dossier.hero?.runFlags.orEmpty()
        ui("dossier.run_flags", flags.size) to flags.maxOf { it.weight }.let { weightTint(it, refuseWeight) }
    }

    DossierFold.FATE -> (dossier.account.fateAt?.let(::dayOf) ?: DASH) to Muted
}

/**
 * Метка темпа героя: «14.10 21:03 · зона» и вес справа (вес отказа [refuseWeight] и выше - [Ember], [weightTint]), причина и значение
 * строкой [Line], слагаемые стены ([RunFlag.trace]) одной приглушённой строкой - у меток без стены её нет.
 */
@Composable private fun RunFlagLine(flag: RunFlag, refuseWeight: Int?) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MutedText("${momentOf(flag.at)} · ${placeTitle(flag.zone)}", style = MaterialTheme.typography.labelSmall)
            Text(ui("moderation.run_flag.weight", flag.weight), color = weightTint(flag.weight, refuseWeight), style = MaterialTheme.typography.labelSmall)
        }
        Line(ui("moderation.run_flag.reason.${flag.reason}"), fineNumber(flag.value))
        flag.trace?.let { trace ->
            MutedText(
                ui("moderation.run_flag.trace", trace.dps?.let(::fineNumber) ?: DASH, trace.foe?.let(::compactNumber) ?: DASH, fineNumber(trace.wall), fineNumber(trace.paid), trace.batch),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * Цвет веса метки темпа: с веса, с которого сервер отказывает событию ([refuseWeight] - правило `run.plausibility.refuseWeight`
 * контента), - [Ember], легче - [Caution]; правила ещё не пришли - [Caution].
 */
private fun weightTint(weight: Int, refuseWeight: Int?): Color = if (refuseWeight != null && weight >= refuseWeight) Ember else Caution

/** Место метки темпа словарём: зона похода, регион раша, Разлом или башня - как сервер пишет [RunFlag.zone]. */
private fun placeTitle(zone: String): String = when {
    zone == RIFT || zone == TOWER -> ui("moderation.run_flag.zone.$zone")
    zone.startsWith(REGION) -> regionTitle(zone)
    else -> mapTitle(zone)
}

/** Связи досье: устройства (модель, первая дата, последний вход, версия, общее с кем и бан устройства), гильдия, другие герои. */
@Composable private fun Links(account: AccountUi, vm: ModerationModel, dossier: Dossier, onBanDevice: (DeviceView) -> Unit) {
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
                ForgeButton(state = ButtonState.of(!account.busy), onClick = { onBanDevice(item) }, kind = ButtonKind.Danger, icon = Icons.Outlined.Block) { Text(ui("moderation.ban_device")) }
            }
        }
    }
    dossier.hero?.guild?.let { Line(ui("moderation.guild"), it) }
    dossier.heroes.forEach { other -> OtherHero(other) { vm.open(other.heroId, dossier.account.userId) } }
}

/**
 * Строки досье «имя — значение ›»: роль аккаунта (меняет администратор; администратора не меняет никто), сервер героя (администратор:
 * лист серверов с переносом удержанием) и переключатель тестировщика (администратор и модератор). Нельзя менять - строка без «›» и
 * не нажимается; у аккаунта без героя - только роль.
 */
@Composable private fun SettingRows(account: AccountUi, vm: ModerationModel, dossier: Dossier, servers: List<GameServer>) {
    val hero = dossier.hero
    val role = account.isAdmin && dossier.account.role != AccountRole.ADMIN
    val server = account.isAdmin && hero != null
    if (server) LaunchedEffect(Unit) { vm.loadServers() }
    var picking by remember { mutableStateOf(false) }
    var moving by remember(hero?.heroId) { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, Bronze, shape)) {
        Box {
            SettingRow(ui("dossier.role"), ui("moderation.role.${dossier.account.role.name}"), enabled = role && !account.busy) { picking = true }
            ForgeMenu(expanded = picking, onDismissRequest = { picking = false }) {
                ASSIGNABLE.forEach { option ->
                    DropdownMenuItem(text = { Text(ui("moderation.role.${option.name}")) }, onClick = {
                        picking = false
                        if (option != dossier.account.role) vm.setRole(dossier.account.userId, option)
                    })
                }
            }
        }
        if (hero != null) {
            HorizontalDivider(color = Bronze)
            SettingRow(ui("dossier.server"), vm.serverName(hero.server), enabled = server && !account.busy) { moving = true }
            HorizontalDivider(color = Bronze)
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(ui("dossier.tester"), color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = hero.tester,
                    enabled = account.isModerator && !account.busy,
                    onCheckedChange = { vm.setTester(hero.heroId, it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Elder),
                    modifier = Modifier.height(24.dp),
                )
            }
        }
    }
    if (moving && hero != null) {
        MoveSheet(account, hero, servers.ifEmpty { listOf(GameServer(hero.server, hero.server)) }, vm::serverName, onDismiss = { moving = false }, onFound = vm::createServer) { code ->
            moving = false
            vm.moveHero(hero.heroId, code)
        }
    }
}

/** Строка настроек досье: имя слева, значение справа и «›», если её можно менять - касание [onClick]. */
@Composable private fun SettingRow(label: String, value: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick).heightIn(min = 44.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, color = if (enabled) Rune else Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (enabled) Text("›", color = Muted, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Лист «Сервер героя»: серверы списком - нынешний с «✓», выбранный для переноса с «●», последней строкой «+ Новый сервер». Выбран
 * другой - внизу предупреждение переноса (уходит из гильдии, лоты возвращаются письмами) прямо над удерживаемой «перенести на X».
 */
@Composable private fun MoveSheet(
    account: AccountUi,
    hero: DossierHero,
    servers: List<GameServer>,
    nameOf: (String) -> String,
    onDismiss: () -> Unit,
    onFound: (String, String) -> Unit,
    onMove: (String) -> Unit,
) {
    var target by remember { mutableStateOf<GameServer?>(null) }
    var founding by remember { mutableStateOf(false) }
    val picked = target
    val confirm: @Composable RowScope.() -> Unit = {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("⚠ " + ui("admin.move_note"), color = Ember, style = MaterialTheme.typography.bodySmall)
            ForgeButton(
                onClick = { picked?.let { onMove(it.code) } },
                modifier = Modifier.fillMaxWidth(),
                kind = ButtonKind.Danger,
                state = ButtonState.of(!account.busy),
                icon = Icons.Outlined.SwapHoriz,
                hold = Hold(),
            ) { Text(ui("admin.move_hold", picked?.let { nameOf(it.code) }.orEmpty()), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(spacing = 6.dp),
        footer = confirm.takeIf { picked != null },
    ) {
        Text(hero.name, color = GoldBright, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Engraved(ui("admin.hero_server"))
        servers.forEach { server ->
            val current = server.code == hero.server
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(enabled = !current) { target = server }.heightIn(min = 44.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(nameOf(server.code), color = if (current) GoldBright else Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                when {
                    current -> Text("✓", color = Vital, style = MaterialTheme.typography.titleMedium)
                    server == picked -> Text("●", color = Rune, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Text(
            ui("dossier.new_server"),
            color = Rune,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { founding = true }.heightIn(min = 44.dp).wrapContentHeight(Alignment.CenterVertically).padding(horizontal = 8.dp),
        )
    }
    if (founding) {
        ServerSheet(account, onDismiss = { founding = false }) { code, name ->
            founding = false
            onFound(code, name)
        }
    }
}

/**
 * Опасные действия - красный блок внизу досье: бан, немота и удаление строками, под каждым именем - одна строка последствий. Права
 * - из досье ([Dossier.rights]); роль выше вашей - строки модерации погашены, касание называет причину. В своём досье [own] блока нет.
 */
@Composable private fun DangerBlock(account: AccountUi, dossier: Dossier, own: Boolean, onSanction: (SanctionKind) -> Unit, onDelete: (SanctionTarget) -> Unit) {
    val rights = dossier.rights
    if (own || !account.isModerator) return
    val hero = dossier.hero?.takeIf { !it.deleted }
    val protected = !rights.ban
    val days = account.index?.rules?.moderation?.trashDays ?: TRASH_DAYS
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, LifeRed.copy(alpha = .5f), shape)) {
        DangerRow(ui("moderation.ban"), ui("moderation.ban_effect"), rights.ban && !account.busy, protected) { onSanction(SanctionKind.BAN) }
        HorizontalDivider(color = LifeRed.copy(alpha = .25f))
        DangerRow(ui("moderation.mute"), ui("moderation.mute_note"), rights.mute && !account.busy, protected) { onSanction(SanctionKind.MUTE) }
        if (rights.delete || (protected && account.isAdmin)) {
            HorizontalDivider(color = LifeRed.copy(alpha = .25f))
            val target = if (hero != null) SanctionTarget.HERO else SanctionTarget.ACCOUNT
            DangerRow(
                ui(if (target == SanctionTarget.HERO) "moderation.delete_hero_do" else "moderation.delete_account_do"),
                ui(if (target == SanctionTarget.HERO) "moderation.delete_hero_effect" else "moderation.delete_account_effect", days),
                rights.delete && !account.busy,
                protected,
            ) { onDelete(target) }
        }
    }
}

/**
 * Строка опасного действия: имя красным, под ним строка последствий [effect] и «›»; погашенная - приглушена, при [protected] касание
 * открывает причину «роль выше вашей».
 */
@Composable private fun DangerRow(title: String, effect: String, enabled: Boolean, protected: Boolean, onClick: () -> Unit) {
    var explain by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = enabled || protected, role = Role.Button) { if (enabled) onClick() else explain = true }
                .heightIn(min = 52.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.Block, null, tint = if (enabled) LifeRed else Muted, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, color = if (enabled) LifeRed else Muted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                MutedText(effect, style = MaterialTheme.typography.labelSmall)
            }
            Text("›", color = Muted, style = MaterialTheme.typography.titleMedium)
        }
        if (explain) TipCallout(Tip(body = ui("moderation.protected"), tint = Muted)) { explain = false }
    }
}

/**
 * Лист смены Предначертания: сетка всех даров контента по имени в два столбца, описание выбранного и «удержи, чтобы
 * сменить» - нынешний дар выбрать нельзя. Счётчики дара у героев начнутся с нуля, начатый заход доиграет со старым. Чужому
 * аккаунту [other] (логин; null - свой) - ещё обязательная причина для игрока: без неё кнопка неактивна, игрок получит её письмом.
 */
@Composable private fun FateRedrawSheet(account: AccountUi, current: String?, other: String?, onDismiss: () -> Unit, onPick: (String, String) -> Unit) {
    val fates = remember(account.index) { account.index?.fates?.all.orEmpty().map(FateCard::of).sortedBy { it.title } }
    var picked by remember { mutableStateOf(current) }
    var reason by remember { mutableStateOf("") }
    val ready = picked != null && picked != current && (other == null || reason.isNotBlank())
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(spacing = 8.dp),
        footer = {
            ForgeButton(onClick = { picked?.let { onPick(it, reason) } }, modifier = Modifier.weight(1f), state = ButtonState.of(!account.busy && ready), hold = Hold(tone = HoldTone.Fate)) {
                Text(ui("moderation.fate.hold"))
            }
        },
    ) {
        Text(other?.let { ui("moderation.fate.change_for", it) } ?: ui("moderation.fate.change_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
        MutedText(ui("moderation.fate.change_note"))
        if (other != null) MutedText(ui("moderation.fate.letter_note"))
        FateGrid(fates) { card -> FateOption(card.title, mark = true, selected = picked == card.code) { picked = card.code } }
        picked?.let { code ->
            val description = remember(code, account.index) { fateDescription(FateCard(code), account.index) }
            if (description.isNotEmpty()) Text("«$description»", color = Muted, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)
        }
        if (other != null) ModerationText(account, reason, ui("moderation.fate.reason")) { reason = it }
    }
}

@Composable private fun ServerSheet(account: AccountUi, onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(spacing = 12.dp),
        footer = {
            ForgeButton(onClick = { onCreate(code, name) }, state = ButtonState.of(!account.busy && code.isNotBlank() && name.isNotBlank()), modifier = Modifier.weight(1f)) {
                Text(ui("admin.server_create"))
            }
        },
    ) {
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
    }
}

@Composable private fun OtherHero(hero: DossierHero, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(hero.name, color = if (hero.deleted) Muted else Parchment, style = MaterialTheme.typography.bodySmall)
        MutedText("${classTitle(hero.heroClass)} ${hero.level}${if (hero.deleted) " · " + ui("moderation.deleted_mark") else ""}")
    }
}

/** Модерация видит и причину для игрока, и служебную заметку. */
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
            if (sanction.reason.isNotBlank()) MutedText(ui("moderation.reason_shown", sanction.reason))
            if (sanction.comment.isNotBlank()) MutedText("«${sanction.comment}»")
        }
        if (sanction.active) {
            ForgeButton(state = ButtonState.of(!busy), onClick = onLift, kind = ButtonKind.Text, accent = Gold) { Text(ui(if (sanction.kind == SanctionKind.DELETION) "moderation.restore" else "moderation.lift")) }
        } else {
            Text(ui(if (sanction.purgedAt != null) "moderation.purged" else "moderation.inactive"), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Давность в сети коротко: «сейчас», «5 мин», «3 ч», дальше - «14.10 21:03». */
private fun seenShort(at: Long, now: Long): String {
    val minutes = (now - at) / MINUTE_MS
    return when {
        minutes < 1 -> ui("dossier.seen.now")
        minutes < MINUTES_IN_HOUR -> ui("dossier.seen.minutes", minutes)
        minutes < MINUTES_IN_DAY -> ui("dossier.seen.hours", minutes / MINUTES_IN_HOUR)
        else -> momentOf(at)
    }
}

/** День в зоне устройства: `дд.мм.гг`. */
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

/** Места меток темпа вне зон похода, как их пишет сервер: Разлом, башня и префикс кодов регионов раша. */
private const val RIFT = "rift"
private const val TOWER = "tower"
private const val REGION = "REGION_"
