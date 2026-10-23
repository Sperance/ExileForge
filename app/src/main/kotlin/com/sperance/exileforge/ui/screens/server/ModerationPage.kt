package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.admin.ModerationFilters
import com.sperance.exileforge.core.admin.ModerationModel
import com.sperance.exileforge.core.admin.ModerationState
import com.sperance.exileforge.core.admin.ModerationTab
import com.sperance.exileforge.core.admin.TrashTerm
import com.sperance.exileforge.core.app.AccountUi
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.dateText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.core.model.hero.ServerClock
import com.sperance.exileforge.core.network.AccountRole
import com.sperance.exileforge.core.network.DeletionRequest
import com.sperance.exileforge.core.network.DeviceView
import com.sperance.exileforge.core.network.Dossier
import com.sperance.exileforge.core.network.ModerationAccount
import com.sperance.exileforge.core.network.ModerationEntryView
import com.sperance.exileforge.core.network.ModerationFate
import com.sperance.exileforge.core.network.ModerationHero
import com.sperance.exileforge.core.network.ModerationRow
import com.sperance.exileforge.core.network.ModerationSort
import com.sperance.exileforge.core.network.SanctionCategory
import com.sperance.exileforge.core.network.SanctionKind
import com.sperance.exileforge.core.network.SanctionNotice
import com.sperance.exileforge.core.network.SanctionRequest
import com.sperance.exileforge.core.network.SanctionTarget
import com.sperance.exileforge.core.network.SanctionView
import com.sperance.exileforge.core.network.TesterAccount
import com.sperance.exileforge.core.network.epochOfStamp
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.rules.text.AppVersion
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.auction.listedAt
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Окно модерации: поиск, разделы «Все / Баны / Корзина / Журнал», карточка на каждого героя ([HeroCard]: герой, его присутствие и логин
 * аккаунта), в корзине - строки удалений героев и аккаунтов ([TrashRow]); тап по карточке открывает досье героя, по логину - аккаунта. Бан и удаление - нижними шторками, удаление - удержанием. Что смотрящему нельзя (админ, модератор для
 * модератора), сервер не пропустит, а карточка показывает замком.
 *
 * Сверху строка поиска ([SearchBar]): порядок и отбор по дару - за кнопкой фильтра с числом сужений, заданные - чипами под
 * полем (у корзины и журнала фильтра нет), справа - «+» тестировщика у администратора. Над списком - сколько найдено. Списки и журнал -
 * страницами сервера ([PagedColumn]): листание до края дочитывает следующую, смена раздела, поиска и отбора читает с первой.
 */
@Composable internal fun ModerationPage(account: AccountUi) {
    val vm = koinViewModel<ModerationViewModel>().model
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        vm.load()
        vm.loadServers()
    }
    if (state.dossierKey != null) {
        TrailLevel(ui("moderation.dossier"), onBack = vm::close) { SettingsScroll { DossierView(account, vm, state.dossier) } }
        return
    }
    var creating by remember { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
    if (creating || state.createdTester != null) {
        AccountSheet(account, state.createdTester, onCreate = vm::createAccount) {
            creating = false
            vm.forgetTester()
        }
    }
    if (filtering) {
        FiltersSheet(account, state.filters, onDismiss = { filtering = false }) {
            filtering = false
            vm.filter(it)
        }
    }
    val journal = state.tab == ModerationTab.JOURNAL
    val loading = Reads.MODERATION in account.activity.loading
    val page = state.page
    PagedColumn(
        count = if (journal) state.journal.items.size else page.items.size,
        more = if (journal) state.journal.more else page.more,
        loading = loading,
        onMore = { if (journal) vm.moreJournal() else vm.more() },
        state.tab,
        state.searched,
        state.filters,
        modifier = Modifier.fillMaxSize(),
        contentPadding = SettingsPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "search") { ModerationSearch(account, state, vm, onFilters = { filtering = true }, onCreate = { creating = true }) }
        item(key = "tabs") {
            PillTabs(
                ModerationTab.entries.map { ui("moderation.tab.${it.name}") },
                state.tab.ordinal,
                { vm.tab(ModerationTab.entries[it]) },
                enabled = !account.busy,
                segmented = true,
            )
        }
        if (journal) {
            val entries = state.journal.items
            if (entries.isEmpty() && !loading) item(key = "none") { MutedText(ui("moderation.journal_empty")) }
            items(entries.size) { JournalRow(entries[it]) }
        } else {
            item(key = "total") { MutedText(ui("moderation.total", page.total ?: 0L), style = MaterialTheme.typography.labelSmall) }
            if (page.items.isEmpty() && !loading) item(key = "none") { MutedText(ui("moderation.none")) }
            val trash = state.tab == ModerationTab.TRASH
            items(page.items, key = { it.key }) { row ->
                val deletion = row.deletion
                val hero = row.hero
                when {
                    trash && deletion != null -> TrashRow(account, row, deletion, vm::open, vm::lift, vm::purge)
                    hero != null -> HeroCard(account, row, hero, vm::serverName, vm::open)
                }
            }
        }
    }
}

/**
 * Строка поиска окна модерации: поиск у сервера с клавиатуры и очисткой поля, порядок и отбор по дару - за кнопкой фильтра
 * ([onFilters]) с чипами сужений (у корзины и журнала фильтра нет), «+» тестировщика у администратора ([onCreate]).
 */
@Composable private fun ModerationSearch(account: AccountUi, state: ModerationState, vm: ModerationModel, onFilters: () -> Unit, onCreate: () -> Unit) {
    val filtered = state.tab != ModerationTab.TRASH && state.tab != ModerationTab.JOURNAL
    SearchBar(
        state.query,
        vm::search,
        ui("moderation.search"),
        onSearch = vm::load,
        filter = SearchFilter(
            rememberChips(state.filters.narrowings(account.lang, account.index), vm::filter),
            onOpen = onFilters,
            onReset = { vm.filter(ModerationFilters.DEFAULT) },
            enabled = !account.busy,
        ).takeIf { filtered },
        action = if (account.isAdmin) {
            { ForgeIconButton(Icons.Outlined.PersonAdd, ui("moderation.new_tester"), onClick = onCreate, kind = ButtonKind.Primary, size = ButtonSize.Compact, state = ButtonState.of(!account.busy)) }
        } else {
            null
        },
    )
}

/**
 * Лист «Фильтры» - общая шторка-«Аккордеон» ([FilterAccordionSheet]): порядок фишками, дар - фишками с поиском по дарам
 * ([SearchChoice]: «Все дары» и «Без дара» первыми, дары контента по имени). Выбор - черновик листа: в список он уходит по «Показать»
 * ([onApply]); «Сбросить» сразу применяет обычный отбор и закрывает лист.
 */
@Composable private fun FiltersSheet(account: AccountUi, current: ModerationFilters, onDismiss: () -> Unit, onApply: (ModerationFilters) -> Unit) {
    var draft by remember(current) { mutableStateOf(current) }
    val fixed = remember(account.lang) { ModerationFilters.fixedFates(account.lang) }
    val fates = remember(account.index, account.lang) { ModerationFilters.fates(account.index) }
    val fateTitle = (fixed + fates).firstOrNull { it.key == draft.fate }?.title ?: ui("moderation.fate.all")
    FilterAccordionSheet(
        groups = listOf(
            AccordionGroup("sort", ui("moderation.filters.sort"), ui("moderation.sort_short.${draft.sort.name}")) {
                ChoiceChips(ModerationSort.entries, { it == draft.sort }, { ui("moderation.sort_short.${it.name}") }) { draft = draft.copy(sort = it) }
            },
            AccordionGroup("fate", ui("moderation.fate.title"), fateTitle) {
                SearchChoice(fates, draft.fate, ui("moderation.filters.fate_search"), fixed) { picked -> picked?.let { draft = draft.copy(fate = it) } }
            },
        ),
        switches = listOf(
            FilterSwitch(ui("moderation.filter.deleted"), draft.deleted) { draft = draft.copy(deleted = it) },
            FilterSwitch(ui("moderation.filter.outdated"), draft.outdated) { draft = draft.copy(outdated = it) },
        ),
        show = ui("common.show"),
        resettable = draft != ModerationFilters.DEFAULT || current != ModerationFilters.DEFAULT,
        onReset = { onApply(ModerationFilters.DEFAULT) },
        onShow = { onApply(draft) },
        onDismiss = onDismiss,
    )
}

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

/**
 * Сетка даров в два столбца - неленивая: она живёт в листающемся теле листа ([SheetBody.Scroll]) и своей прокрутки не держит.
 */
@Composable internal fun <T> FateGrid(items: List<T>, cell: @Composable (T) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(FATE_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { Box(Modifier.weight(1f)) { cell(it) } }
                repeat(FATE_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val FATE_COLUMNS = 2

/**
 * Карточка героя списка модерации - «плотная строка» в три строки без подписей: портрет, имя и знак тестировщика, класс и уровень, Мощь
 * справа; ниже присутствие героя (живое, «в сети» - по окну `rules.online`) и логин аккаунта с ролью и замком (касание логина - досье
 * аккаунта); третья - гильдия, золото, когда создан, сервер и версия клиента ([VersionMark]). Чипы - санкции героя и аккаунта и дар.
 * Касание карточки - досье героя ([onOpen]: id героя и аккаунта).
 */
@Composable private fun HeroCard(account: AccountUi, row: ModerationRow, hero: ModerationHero, serverName: (String) -> String, onOpen: (String, String) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val owner = row.account
    val heroSanction = hero.sanction?.takeIf { it.active }
    val accountSanction = owner.sanction?.takeIf { it.active }
    val edge = sanctionEdge(heroSanction ?: accountSanction)
    val presence = rememberPresence(hero.lastSeenAt) { now -> account.index?.rules?.online(hero.lastSeenAt, now) == true }
    Column(
        Modifier.fillMaxWidth().depthPanel(shape).border(1.dp, edge, shape).clip(shape).clickable(enabled = !account.busy) { onOpen(hero.heroId, owner.userId) }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HeroHead(account, hero)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(presence.text, color = if (presence.online) Vital else Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
            MutedText("·", style = MaterialTheme.typography.labelSmall)
            LoginLink(account, owner, Modifier.weight(1f, fill = false)) { onOpen("", owner.userId) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                listOfNotNull(
                    hero.guild ?: ui("moderation.row.no_guild"),
                    ui("moderation.row.gold", compactNumber(hero.gold.toDouble())),
                    epochOfStamp(hero.createdAt)?.let { ui("moderation.row.created", dateText(it)) },
                    hero.server.takeIf { it.isNotBlank() }?.let(serverName),
                ).joinToString(" · "),
                color = Quiet,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            VersionMark(owner.clientVersion)
        }
        if (heroSanction != null || accountSanction != null || owner.fate != null) {
            Chips {
                heroSanction?.let { SanctionTag(it) }
                accountSanction?.let { SanctionTag(it) }
                owner.fate?.let { FateMark(FateCard(it)) }
            }
        }
    }
}

/** Верхняя строка героя: портрет, имя и знак тестировщика, класс и уровень (без данных героя - «данных героя нет»), Мощь справа; удалённый - приглушён. */
@Composable private fun HeroHead(account: AccountUi, hero: ModerationHero) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ClassPortrait(hero.heroClass.takeIf { it.isNotBlank() }, account.world.portraits, Modifier.size(30.dp).alpha(if (hero.deleted) GONE else 1f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(hero.name, color = if (hero.deleted) Muted else Parchment, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                TesterMark(hero.tester, 14.dp)
            }
            MutedText(
                if (hero.heroClass.isBlank()) ui("moderation.row.no_data") else listOf(classTitle(hero.heroClass), ui("common.level_short", hero.level)).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        hero.might?.let { might ->
            Column(horizontalAlignment = Alignment.End) {
                Text(number(might), color = if (hero.deleted) Muted else Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                MutedText(ui("moderation.might"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Логин аккаунта ссылкой на его досье ([onOpen]): значок, логин, роль и замок, если роль выше вашей. */
@Composable private fun LoginLink(account: AccountUi, owner: ModerationAccount, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(6.dp)).clickable(enabled = !account.busy, role = Role.Button, onClick = onOpen).heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Outlined.Person, null, tint = Muted, modifier = Modifier.size(13.dp))
        Text(owner.login, color = Quiet, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        RoleMark(owner.role, 13.dp)
        if (owner.protected) Icon(Icons.Outlined.Lock, ui("moderation.protected"), tint = Muted, modifier = Modifier.size(13.dp))
    }
}

/** Кромка карточки по санкции: бан - красная, немота - жёлтая, иначе обычная. */
private fun sanctionEdge(sanction: SanctionView?): Color = when (sanction?.kind) {
    SanctionKind.BAN -> LifeRed.copy(alpha = .45f)
    SanctionKind.MUTE -> Caution.copy(alpha = .45f)
    else -> Bronze
}

/**
 * Строка корзины: удалённый герой (шапка героя и логин аккаунта) или удалённый аккаунт целиком (логин и сколько героев сотрёт стирание);
 * ниже - кто, когда и за что удалил, живой срок «через 29 дн. 22 ч · 24 нояб.» и полоса «осталось» (серая, красная в последние
 * [TrashTerm.URGENT_DAYS] дня), справа круглые «Восстановить» и «Стереть сейчас» с кольцом удержания - только администратору; пока
 * палец на «Стереть», под полосой - что сотрётся. Касание строки героя - досье героя, аккаунта - досье аккаунта.
 */
@Composable private fun TrashRow(account: AccountUi, row: ModerationRow, deletion: SanctionView, onOpen: (String, String) -> Unit, onRestore: (SanctionView) -> Unit, onPurge: (SanctionView) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val hero = row.hero
    val owner = row.account
    Column(
        Modifier.fillMaxWidth().depthPanel(shape).border(1.dp, Bronze, shape).clip(shape).clickable(enabled = !account.busy) { onOpen(hero?.heroId.orEmpty(), owner.userId) }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (hero != null) {
            HeroHead(account, hero)
            LoginLink(account, owner) { onOpen("", owner.userId) }
        } else {
            Text(owner.login, color = Muted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MutedText(ui("moderation.row.whole_account", heroCount(owner.heroCount)), style = MaterialTheme.typography.labelSmall)
        }
        TrashTermLine(account, deletion, consequence = if (hero != null) ui("moderation.purge.hold_hero") else ui("moderation.purge.hold_account", heroCount(owner.heroCount)), actionable = !owner.protected, onRestore, onPurge)
    }
}

/** «N героев» с числом и склонением языка. */
private fun heroCount(count: Int): String = ui(pluralKey("moderation.hero_count", count), count)

/**
 * Срок удаления [deletion] в строке корзины: кто и за что удалил, живой срок и дата очистки, полоса «осталось»; при [actionable]
 * (администратор) справа «Восстановить» ([onRestore]) и «Стереть сейчас» удержанием ([onPurge]), пока его держат - [consequence].
 */
@Composable private fun TrashTermLine(account: AccountUi, deletion: SanctionView, consequence: String, actionable: Boolean, onRestore: (SanctionView) -> Unit, onPurge: (SanctionView) -> Unit) {
    val now = rememberServerNow(TERM_TICK_MS)
    val term = remember(deletion.at, deletion.until, now) { TrashTerm.of(deletion.at, deletion.until, now) }
    var holding by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            MutedText(listOf(deletedText(deletion), deletion.byLogin, categoryTitle(deletion.category)).joinToString(" · "), style = MaterialTheme.typography.labelSmall)
            term?.let {
                val tint = if (it.urgent) LifeRed else Muted
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(it.leftText(), color = if (it.urgent) LossText else Parchment, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                    MutedText("· " + it.dateText(), style = MaterialTheme.typography.labelSmall)
                }
                LinearProgressIndicator(progress = { it.share }, color = tint, trackColor = PanelRaised, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)))
            }
            Text(if (holding) consequence else ui("moderation.purge.left"), color = if (holding) LossText else Muted, style = MaterialTheme.typography.labelSmall, maxLines = 2)
        }
        if (actionable) {
            ForgeIconButton(Icons.Outlined.Restore, ui("moderation.restore"), onClick = { onRestore(deletion) }, kind = ButtonKind.Secondary, size = ButtonSize.Compact, state = ButtonState.of(!account.busy), accent = Gold, shape = IconButtonShape.Circle)
            HoldRingButton(Icons.Outlined.DeleteForever, ui("moderation.purge.now"), enabled = !account.busy, tint = LifeRed, onHolding = { holding = it }) { onPurge(deletion) }
        }
    }
}

/** Метка удаления: кто удалил - автор (самоудаление) или модерация - и когда. */
private fun deletedText(deletion: SanctionView): String = ui(if (deletion.voluntary) "moderation.deleted_by_author" else "moderation.deleted_by_moderation", listedAt(deletion.at)?.take(DATE).orEmpty())

/** Чип санкции аккаунта: вид, цель, срок и причина для игрока (без неё - категория); бан - красный, немота - жёлтая, удаление - серое. */
@Composable private fun SanctionTag(sanction: SanctionView) {
    val tint = when (sanction.kind) {
        SanctionKind.BAN -> LifeRed
        SanctionKind.MUTE -> Caution
        SanctionKind.DELETION -> Muted
    }
    val text = listOfNotNull(ui("moderation.kind.${sanction.kind.name}") + " " + ui("moderation.target.${sanction.target.name}").lowercase(), termText(sanction), sanction.reason.ifBlank { categoryTitle(sanction.category) })
        .joinToString(" · ")
    Text(
        text,
        color = tint,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.background(tint.copy(alpha = .12f), RoundedCornerShape(8.dp)).border(1.dp, tint.copy(alpha = .4f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/**
 * Версия клиента аккаунта: та же, что у этой сборки, или новее - тихо; старее ([AppVersion.older]) - жёлтый чип со стрелкой вниз, касание
 * называет актуальную версию. Неизвестна - ничего.
 */
@Composable private fun VersionMark(version: String) {
    if (version.isBlank()) return
    val current = BuildConfig.VERSION_NAME
    if (!AppVersion.older(version, current)) {
        MutedText(version, style = MaterialTheme.typography.labelSmall)
        return
    }
    var explain by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).background(Caution.copy(alpha = .14f)).border(1.dp, Caution.copy(alpha = .5f), RoundedCornerShape(8.dp))
                .clickable { explain = true }.padding(horizontal = 5.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.ArrowDownward, ui("moderation.version_old"), tint = Caution, modifier = Modifier.size(12.dp))
            Text(version, color = Caution, style = MaterialTheme.typography.labelSmall)
        }
        if (explain) TipCallout(Tip(body = ui("moderation.version_old_tip", current), tint = Caution)) { explain = false }
    }
}

/** Смена дара - без санкции: ни номера, ни категории; дары - именами. Ниже - причина для игрока, если была (смена дара и имени). */
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
        } else {
            entry.comment.takeIf { it.isNotBlank() }?.let { MutedText("«$it»") }
            entry.reason.takeIf { it.isNotBlank() }?.let { MutedText(ui("moderation.reason_shown", it)) }
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
    val term = custom.toIntOrNull() ?: hours
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(),
        footer = {
            ForgeButton(
                state = ButtonState.of(!account.busy && (custom.isBlank() || (custom.toIntOrNull() ?: 0) > 0)),
                onClick = { onBan(SanctionRequest(target, kind, dossier.hero?.heroId.orEmpty(), dossier.account.userId, device?.hardware.orEmpty(), term, category, comment, reason)) },
                modifier = Modifier.weight(1f),
            ) { Text(ui(if (kind == SanctionKind.MUTE) "moderation.mute_for" else "moderation.ban_for", ui("moderation.target.$target"), term?.let(::hoursText) ?: ui("moderation.forever"))) }
        },
    ) {
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
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(),
        footer = {
            ForgeButton(
                onClick = { onDelete(DeletionRequest(target, if (target == SanctionTarget.HERO) hero?.heroId.orEmpty() else dossier.account.userId, category, comment, reason)) },
                modifier = Modifier.weight(1f),
                kind = ButtonKind.Danger,
                state = ButtonState.of(!account.busy && comment.isNotBlank()),
                icon = Icons.Outlined.DeleteOutline,
                hold = Hold(),
            ) { Text(ui("moderation.hold_delete")) }
        },
    ) {
        Text(ui("moderation.delete_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
        Chips {
            hero?.let { Chip(it.name, target == SanctionTarget.HERO) { onTarget(SanctionTarget.HERO) } }
            Chip(ui("moderation.delete_account", dossier.account.login, dossier.account.heroes), target == SanctionTarget.ACCOUNT) { onTarget(SanctionTarget.ACCOUNT) }
        }
        MutedText(ui(if (target == SanctionTarget.HERO) "moderation.delete_hero_note" else "moderation.delete_account_note", days))
        Chips { SanctionCategory.imposed.forEach { Chip(categoryTitle(it), category == it) { category = it } } }
        ModerationText(account, reason, ui("moderation.reason_for_player")) { reason = it }
        ModerationText(account, comment, ui("moderation.delete_reason")) { comment = it }
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
    ForgeChip(label, selected, onClick, accent = LossText, mode = ChipMode.Choice)
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

internal fun categoryTitle(category: SanctionCategory): String = ui("moderation.category.${category.name}")

/** Срок санкции словами: «до 11.10 18:02», «навсегда» или срок корзины «через 29 дн. 22 ч» ([TrashTerm]). */
internal fun termText(sanction: SanctionView): String? = termText(sanction.kind, sanction.at, sanction.until)

/** Срок санкции глазами наказанного - теми же словами. */
internal fun termText(notice: SanctionNotice): String? = termText(notice.kind, notice.at, notice.until)

private fun termText(kind: SanctionKind, at: String, until: String?): String? = when {
    kind == SanctionKind.DELETION -> TrashTerm.of(at, until, System.currentTimeMillis() + ServerClock.offset)?.leftText()
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

private const val PRESET_WEEK = 2
private const val TERM_TICK_MS = 60_000L

/** Прозрачность портрета героя в корзине. */
private const val GONE = .45f
internal const val TRASH_DAYS = 30
private const val COMMENT = 300
private const val DIGITS = 4
private const val HOURS_IN_DAY = 24
internal const val DATE = 10
internal const val HARDWARE = 6

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
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(spacing = 12.dp),
        footer = {
            if (created == null) {
                ForgeButton(onClick = { onCreate(login, role) }, state = ButtonState.of(!account.busy && login.isNotBlank()), modifier = Modifier.weight(1f)) { Text(ui("moderation.tester_create")) }
            } else {
                ForgeButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(ui("tester.ok")) }
            }
        },
    ) {
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
        } else {
            MutedText(ui("moderation.tester_once"))
            listOf(ui("moderation.tester_login") to created.login, ui("moderation.tester_password") to created.password.orEmpty()).forEach { (label, value) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Label(label)
                        Text(value, color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    ForgeButton(onClick = { clipboard.setText(AnnotatedString(value)) }, kind = ButtonKind.Secondary) { Text(ui("tester.copy")) }
                }
            }
        }
    }
}
